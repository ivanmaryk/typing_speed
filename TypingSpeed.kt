// TypingSpeed.kt
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.time.Instant
import kotlin.random.Random

class TypingSpeed {
    @Parameter(names = ["--language"])
    private var language: String = "en"

    @Parameter(names = ["--mode"])
    private var mode: String = "words"

    @Parameter(names = ["--length"])
    private var length: Int = 30

    @Parameter(names = ["--export-stats"])
    private var exportStats: String? = null

    @Parameter(names = ["--history"])
    private var history: Boolean = false

    data class Stats(
        val timestamp: String,
        val language: String,
        val mode: String,
        val length: Int,
        val text_len: Int,
        val typed_len: Int,
        val errors: Int,
        val cpm: Double,
        val wpm: Double,
        val accuracy: Double,
        val time_sec: Double
    )

    private val wordsEn = listOf(
        "apple","orange","banana","grape","peach","mango","lemon","melon","berry","plum",
        "cloud","storm","rain","snow","wind","sun","moon","star","sky","tree",
        "book","table","chair","window","door","floor","wall","roof","garden","flower",
        "happy","sad","angry","calm","brave","smart","kind","funny","serious","wild",
        "quick","brown","fox","jumps","lazy","dog","hello","world","python","code"
    )

    private val wordsRu = listOf(
        "яблоко","апельсин","банан","виноград","персик","манго","лимон","дыня","ягода","слива",
        "облако","буря","дождь","снег","ветер","солнце","луна","звезда","небо","дерево",
        "книга","стол","стул","окно","дверь","пол","стена","крыша","сад","цветок",
        "счастливый","грустный","злой","спокойный","храбрый","умный","добрый","смешной","серьёзный","дикий",
        "быстрый","коричневый","лиса","прыгает","ленивый","собака","привет","мир","питон","код"
    )

    private val historyFile = "typing_history.json"
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val type = object : TypeToken<MutableList<Stats>>() {}.type
    private var historyList = mutableListOf<Stats>()
    private lateinit var text: String
    private var startTime: Long = 0
    private var endTime: Long = 0
    private var errors = 0
    private var totalChars = 0
    private val scanner = Scanner(System.`in`)

    private fun loadHistory() {
        val f = File(historyFile)
        if (!f.exists()) return
        try {
            val json = f.readText()
            historyList = gson.fromJson(json, type) ?: mutableListOf()
        } catch (e: Exception) {
            historyList = mutableListOf()
        }
    }

    private fun saveHistory(stats: Stats) {
        historyList.add(stats)
        val json = gson.toJson(historyList)
        File(historyFile).writeText(json)
    }

    private fun generateText(): String {
        val words = if (language == "ru") wordsRu else wordsEn
        return when (mode) {
            "chars" -> {
                val all = words.joinToString("")
                val sb = StringBuilder()
                for (i in 0 until minOf(length, all.length)) {
                    sb.append(all[Random.nextInt(all.length)])
                }
                sb.toString()
            }
            "sentences" -> {
                val shuffled = words.shuffled()
                val selected = shuffled.take(minOf(length, shuffled.size))
                var sentence = selected.joinToString(" ")
                if (sentence.isNotEmpty()) {
                    sentence = sentence.replaceFirstChar { it.uppercase() }
                    sentence += "."
                }
                sentence
            }
            else -> {
                val shuffled = words.shuffled()
                val selected = shuffled.take(minOf(length, shuffled.size))
                selected.joinToString(" ")
            }
        }
    }

    fun run() {
        loadHistory()
        if (history) {
            showHistory()
            return
        }
        text = generateText()
        println("\u001B[36mВведите следующий текст как можно быстрее и точнее:\u001B[0m")
        println("\u001B[37m$text\u001B[0m")
        println("\nНажмите Enter, чтобы начать...")
        readLine()
        print("\u001B[32m> \u001B[0m")
        startTime = System.currentTimeMillis()
        val userInput = readLine() ?: ""
        endTime = System.currentTimeMillis()
        totalChars = userInput.length
        val minLen = minOf(text.length, userInput.length)
        errors = 0
        for (i in 0 until minLen) {
            if (text[i] != userInput[i]) errors++
        }
        if (userInput.length > text.length) errors += userInput.length - text.length
        val elapsed = (endTime - startTime) / 1000.0
        val cpm = if (elapsed > 0) totalChars / elapsed * 60 else 0.0
        val wpm = cpm / 5
        val accuracy = if (totalChars > 0) (totalChars - errors).toDouble() / totalChars * 100 else 0.0
        val stats = Stats(
            timestamp = Instant.now().toString(),
            language = language,
            mode = mode,
            length = length,
            text_len = text.length,
            typed_len = totalChars,
            errors = errors,
            cpm = cpm,
            wpm = wpm,
            accuracy = accuracy,
            time_sec = elapsed
        )
        println("\n\u001B[36m--- Результаты ---\u001B[0m")
        println("\u001B[32mСкорость: ${"%.1f".format(cpm)} симв/мин (${"%.1f".format(wpm)} слов/мин)\u001B[0m")
        println("\u001B[33mТочность: ${"%.1f".format(accuracy)}%\u001B[0m")
        println("\u001B[35mВремя: ${"%.2f".format(elapsed)} сек\u001B[0m")
        println("\u001B[31mОшибок: $errors (из $totalChars)\u001B[0m")
        saveHistory(stats)
        exportStats?.let { exportStatsFunc(stats, it) }
    }

    private fun exportStatsFunc(stats: Stats, filename: String) {
        val ext = filename.substringAfterLast('.')
        val content = when (ext) {
            "json" -> gson.toJson(stats)
            "csv" -> {
                "timestamp,language,mode,length,text_len,typed_len,errors,cpm,wpm,accuracy,time_sec\n" +
                        "${stats.timestamp},${stats.language},${stats.mode},${stats.length}," +
                        "${stats.text_len},${stats.typed_len},${stats.errors},${stats.cpm},${stats.wpm}," +
                        "${stats.accuracy},${stats.time_sec}\n"
            }
            else -> stats.toString()
        }
        File(filename).writeText(content)
        println("\u001B[32mСтатистика сохранена в $filename\u001B[0m")
    }

    private fun showHistory() {
        if (historyList.isEmpty()) {
            println("\u001B[33mИстория пуста.\u001B[0m")
            return
        }
        println("\u001B[36m📊 История тренировок:\u001B[0m")
        val start = maxOf(0, historyList.size - 10)
        for (i in historyList.indices.reversed().take(historyList.size - start)) {
            val s = historyList[i]
            println("  ${historyList.size - i}. ${s.timestamp.substring(0,19).replace('T',' ')} | ${s.language} | ${s.mode} | CPM: ${s.cpm.toInt()} | WPM: ${s.wpm.toInt()} | Точность: ${"%.1f".format(s.accuracy)}%")
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val trainer = TypingSpeed()
            JCommander.newBuilder().addObject(trainer).build().parse(*args)
            trainer.run()
        }
    }
}
