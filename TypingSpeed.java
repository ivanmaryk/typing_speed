// TypingSpeed.java
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

public class TypingSpeed {
    private static final String[] WORDS_EN = {
        "apple", "orange", "banana", "grape", "peach", "mango", "lemon", "melon", "berry", "plum",
        "cloud", "storm", "rain", "snow", "wind", "sun", "moon", "star", "sky", "tree",
        "book", "table", "chair", "window", "door", "floor", "wall", "roof", "garden", "flower",
        "happy", "sad", "angry", "calm", "brave", "smart", "kind", "funny", "serious", "wild",
        "quick", "brown", "fox", "jumps", "lazy", "dog", "hello", "world", "python", "code"
    };
    private static final String[] WORDS_RU = {
        "яблоко", "апельсин", "банан", "виноград", "персик", "манго", "лимон", "дыня", "ягода", "слива",
        "облако", "буря", "дождь", "снег", "ветер", "солнце", "луна", "звезда", "небо", "дерево",
        "книга", "стол", "стул", "окно", "дверь", "пол", "стена", "крыша", "сад", "цветок",
        "счастливый", "грустный", "злой", "спокойный", "храбрый", "умный", "добрый", "смешной", "серьёзный", "дикий",
        "быстрый", "коричневый", "лиса", "прыгает", "ленивый", "собака", "привет", "мир", "питон", "код"
    };

    @Parameter(names = "--language")
    private String language = "en";
    @Parameter(names = "--mode")
    private String mode = "words";
    @Parameter(names = "--length")
    private int length = 30;
    @Parameter(names = "--export-stats")
    private String exportStats;
    @Parameter(names = "--history")
    private boolean history;

    private String[] words;
    private String text;
    private long startTime;
    private long endTime;
    private int errors;
    private int totalChars;
    private String historyFile = "typing_history.json";
    private List<Stats> historyList = new ArrayList<>();
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Type historyType = new TypeToken<List<Stats>>(){}.getType();
    private Scanner scanner = new Scanner(System.in);

    static class Stats {
        String timestamp;
        String language, mode;
        int length, text_len, typed_len, errors;
        double cpm, wpm, accuracy, time_sec;
    }

    private void loadHistory() {
        try {
            String json = new String(Files.readAllBytes(Paths.get(historyFile)));
            historyList = gson.fromJson(json, historyType);
        } catch (Exception e) {
            historyList = new ArrayList<>();
        }
    }

    private void saveHistory(Stats stats) {
        historyList.add(stats);
        try {
            Files.write(Paths.get(historyFile), gson.toJson(historyList).getBytes());
        } catch (IOException e) {
            System.err.println("Ошибка сохранения истории: " + e.getMessage());
        }
    }

    private String generateText() {
        words = language.equals("ru") ? WORDS_RU : WORDS_EN;
        Random rand = new Random();
        if (mode.equals("chars")) {
            StringBuilder sb = new StringBuilder();
            String all = String.join("", words);
            for (int i = 0; i < Math.min(length, all.length()); i++) {
                sb.append(all.charAt(rand.nextInt(all.length())));
            }
            return sb.toString();
        } else if (mode.equals("sentences")) {
            List<String> shuffled = new ArrayList<>(Arrays.asList(words));
            Collections.shuffle(shuffled);
            int n = Math.min(length, shuffled.size());
            List<String> selected = shuffled.subList(0, n);
            String sentence = String.join(" ", selected);
            if (!sentence.isEmpty()) {
                sentence = sentence.substring(0,1).toUpperCase() + sentence.substring(1) + ".";
            }
            return sentence;
        } else {
            List<String> shuffled = new ArrayList<>(Arrays.asList(words));
            Collections.shuffle(shuffled);
            int n = Math.min(length, shuffled.size());
            List<String> selected = shuffled.subList(0, n);
            return String.join(" ", selected);
        }
    }

    private void run() throws Exception {
        loadHistory();
        if (history) {
            showHistory();
            return;
        }
        text = generateText();
        System.out.println("\u001B[36mВведите следующий текст как можно быстрее и точнее:\u001B[0m");
        System.out.println("\u001B[37m" + text + "\u001B[0m");
        System.out.println("\nНажмите Enter, чтобы начать...");
        scanner.nextLine();
        startTime = System.currentTimeMillis();
        System.out.print("\u001B[32m> \u001B[0m");
        String userInput = scanner.nextLine();
        endTime = System.currentTimeMillis();
        totalChars = userInput.length();
        int minLen = Math.min(text.length(), userInput.length());
        errors = 0;
        for (int i = 0; i < minLen; i++) {
            if (text.charAt(i) != userInput.charAt(i)) errors++;
        }
        if (userInput.length() > text.length()) errors += userInput.length() - text.length();
        double elapsed = (endTime - startTime) / 1000.0;
        double cpm = (totalChars / elapsed) * 60;
        double wpm = cpm / 5;
        double accuracy = totalChars > 0 ? (double)(totalChars - errors) / totalChars * 100 : 0;
        Stats stats = new Stats();
        stats.timestamp = Instant.now().toString();
        stats.language = language;
        stats.mode = mode;
        stats.length = length;
        stats.text_len = text.length();
        stats.typed_len = totalChars;
        stats.errors = errors;
        stats.cpm = cpm;
        stats.wpm = wpm;
        stats.accuracy = accuracy;
        stats.time_sec = elapsed;
        System.out.println("\n\u001B[36m--- Результаты ---\u001B[0m");
        System.out.printf("\u001B[32mСкорость: %.1f симв/мин (%.1f слов/мин)\u001B[0m%n", cpm, wpm);
        System.out.printf("\u001B[33mТочность: %.1f%%\u001B[0m%n", accuracy);
        System.out.printf("\u001B[35mВремя: %.2f сек\u001B[0m%n", elapsed);
        System.out.printf("\u001B[31mОшибок: %d (из %d)\u001B[0m%n", errors, totalChars);
        saveHistory(stats);
        if (exportStats != null) {
            exportStats(stats);
        }
        scanner.close();
    }

    private void exportStats(Stats stats) throws IOException {
        String ext = exportStats.substring(exportStats.lastIndexOf('.') + 1).toLowerCase();
        if (ext.equals("json")) {
            Files.write(Paths.get(exportStats), gson.toJson(stats).getBytes());
        } else if (ext.equals("csv")) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(exportStats))) {
                pw.println("timestamp,language,mode,length,text_len,typed_len,errors,cpm,wpm,accuracy,time_sec");
                pw.printf("%s,%s,%s,%d,%d,%d,%d,%.2f,%.2f,%.2f,%.2f%n",
                    stats.timestamp, stats.language, stats.mode, stats.length,
                    stats.text_len, stats.typed_len, stats.errors,
                    stats.cpm, stats.wpm, stats.accuracy, stats.time_sec);
            }
        }
        System.out.println("\u001B[32mСтатистика сохранена в " + exportStats + "\u001B[0m");
    }

    private void showHistory() {
        if (historyList.isEmpty()) {
            System.out.println("\u001B[33mИстория пуста.\u001B[0m");
            return;
        }
        System.out.println("\u001B[36m📊 История тренировок:\u001B[0m");
        int start = Math.max(0, historyList.size() - 10);
        for (int i = historyList.size() - 1; i >= start; i--) {
            Stats s = historyList.get(i);
            System.out.printf("  %d. %s | %s | %s | CPM: %.0f | WPM: %.0f | Точность: %.1f%%%n",
                historyList.size() - i,
                s.timestamp.substring(0,19).replace('T', ' '),
                s.language, s.mode,
                s.cpm, s.wpm, s.accuracy);
        }
    }

    public static void main(String[] args) throws Exception {
        TypingSpeed ts = new TypingSpeed();
        JCommander.newBuilder().addObject(ts).build().parse(args);
        ts.run();
    }
}
