// typing_speed.go
package main

import (
	"bufio"
	"encoding/csv"
	"encoding/json"
	"flag"
	"fmt"
	"math/rand"
	"os"
	"strings"
	"time"
)

var wordsEn = []string{
	"apple", "orange", "banana", "grape", "peach", "mango", "lemon", "melon", "berry", "plum",
	"cloud", "storm", "rain", "snow", "wind", "sun", "moon", "star", "sky", "tree",
	"book", "table", "chair", "window", "door", "floor", "wall", "roof", "garden", "flower",
	"happy", "sad", "angry", "calm", "brave", "smart", "kind", "funny", "serious", "wild",
	"quick", "brown", "fox", "jumps", "lazy", "dog", "hello", "world", "python", "code",
}

var wordsRu = []string{
	"яблоко", "апельсин", "банан", "виноград", "персик", "манго", "лимон", "дыня", "ягода", "слива",
	"облако", "буря", "дождь", "снег", "ветер", "солнце", "луна", "звезда", "небо", "дерево",
	"книга", "стол", "стул", "окно", "дверь", "пол", "стена", "крыша", "сад", "цветок",
	"счастливый", "грустный", "злой", "спокойный", "храбрый", "умный", "добрый", "смешной", "серьёзный", "дикий",
	"быстрый", "коричневый", "лиса", "прыгает", "ленивый", "собака", "привет", "мир", "питон", "код",
}

type TypingSpeed struct {
	language    string
	mode        string
	length      int
	exportStats string
	words       []string
	text        string
	startTime   time.Time
	endTime     time.Time
	errors      int
	totalChars  int
	historyFile string
	history     []map[string]interface{}
}

func NewTypingSpeed(language, mode string, length int, exportStats string) *TypingSpeed {
	ts := &TypingSpeed{
		language:    language,
		mode:        mode,
		length:      length,
		exportStats: exportStats,
		historyFile: "typing_history.json",
	}
	if language == "ru" {
		ts.words = wordsRu
	} else {
		ts.words = wordsEn
	}
	ts.text = ts.generateText()
	ts.history = ts.loadHistory()
	return ts
}

func (ts *TypingSpeed) generateText() string {
	rand.Seed(time.Now().UnixNano())
	if ts.mode == "chars" {
		chars := strings.Join(ts.words, "")
		if len(chars) > ts.length {
			return chars[:ts.length]
		}
		return chars
	} else if ts.mode == "sentences" {
		selected := ts.selectRandomWords(ts.length)
		sentence := strings.Join(selected, " ")
		if len(sentence) > 0 {
			sentence = strings.ToUpper(sentence[:1]) + sentence[1:] + "."
		}
		return sentence
	} else { // words
		selected := ts.selectRandomWords(ts.length)
		return strings.Join(selected, " ")
	}
}

func (ts *TypingSpeed) selectRandomWords(n int) []string {
	if n > len(ts.words) {
		n = len(ts.words)
	}
	shuffled := make([]string, len(ts.words))
	copy(shuffled, ts.words)
	rand.Shuffle(len(shuffled), func(i, j int) { shuffled[i], shuffled[j] = shuffled[j], shuffled[i] })
	return shuffled[:n]
}

func (ts *TypingSpeed) loadHistory() []map[string]interface{} {
	data, err := os.ReadFile(ts.historyFile)
	if err != nil {
		return []map[string]interface{}{}
	}
	var history []map[string]interface{}
	if err := json.Unmarshal(data, &history); err != nil {
		return []map[string]interface{}{}
	}
	return history
}

func (ts *TypingSpeed) saveHistory(stats map[string]interface{}) {
	ts.history = append(ts.history, stats)
	data, _ := json.MarshalIndent(ts.history, "", "  ")
	os.WriteFile(ts.historyFile, data, 0644)
}

func (ts *TypingSpeed) run() {
	reader := bufio.NewReader(os.Stdin)
	fmt.Println("\033[36mВведите следующий текст как можно быстрее и точнее:\033[0m")
	fmt.Println("\033[37m" + ts.text + "\033[0m")
	fmt.Println("\nНажмите Enter, чтобы начать...")
	reader.ReadString('\n')
	ts.startTime = time.Now()
	fmt.Print("\033[32m> \033[0m")
	input, _ := reader.ReadString('\n')
	input = strings.TrimSuffix(input, "\n")
	ts.endTime = time.Now()
	ts.totalChars = len(input)
	minLen := len(ts.text)
	if len(input) < minLen {
		minLen = len(input)
	}
	ts.errors = 0
	for i := 0; i < minLen; i++ {
		if ts.text[i] != input[i] {
			ts.errors++
		}
	}
	if len(input) > len(ts.text) {
		ts.errors += len(input) - len(ts.text)
	}
	elapsed := ts.endTime.Sub(ts.startTime).Seconds()
	cpm := float64(ts.totalChars) / elapsed * 60
	wpm := cpm / 5
	accuracy := 0.0
	if ts.totalChars > 0 {
		accuracy = float64(ts.totalChars-ts.errors) / float64(ts.totalChars) * 100
	}
	stats := map[string]interface{}{
		"timestamp": time.Now().Format(time.RFC3339),
		"language":  ts.language,
		"mode":      ts.mode,
		"length":    ts.length,
		"text_len":  len(ts.text),
		"typed_len": ts.totalChars,
		"errors":    ts.errors,
		"cpm":       cpm,
		"wpm":       wpm,
		"accuracy":  accuracy,
		"time_sec":  elapsed,
	}
	fmt.Println("\n\033[36m--- Результаты ---\033[0m")
	fmt.Printf("\033[32mСкорость: %.1f симв/мин (%.1f слов/мин)\033[0m\n", cpm, wpm)
	fmt.Printf("\033[33mТочность: %.1f%%\033[0m\n", accuracy)
	fmt.Printf("\033[35mВремя: %.2f сек\033[0m\n", elapsed)
	fmt.Printf("\033[31mОшибок: %d (из %d)\033[0m\n", ts.errors, ts.totalChars)
	ts.saveHistory(stats)
	if ts.exportStats != "" {
		ts.exportStatsFunc(stats)
	}
}

func (ts *TypingSpeed) exportStatsFunc(stats map[string]interface{}) {
	filename := ts.exportStats
	ext := filename[strings.LastIndex(filename, ".")+1:]
	var data []byte
	var err error
	if ext == "json" {
		data, err = json.MarshalIndent(stats, "", "  ")
	} else if ext == "csv" {
		var line string
		for _, v := range stats {
			line += fmt.Sprintf("%v,", v)
		}
		line = line[:len(line)-1] + "\n"
		data = []byte(line)
	}
	if err == nil {
		os.WriteFile(filename, data, 0644)
		fmt.Printf("\033[32mСтатистика сохранена в %s\033[0m\n", filename)
	}
}

func (ts *TypingSpeed) showHistory() {
	if len(ts.history) == 0 {
		fmt.Println("\033[33mИстория пуста.\033[0m")
		return
	}
	fmt.Println("\033[36m📊 История тренировок:\033[0m")
	start := len(ts.history) - 10
	if start < 0 {
		start = 0
	}
	for i := len(ts.history) - 1; i >= start; i-- {
		entry := ts.history[i]
		fmt.Printf("  %d. %s | %s | %s | CPM: %v | WPM: %v | Точность: %.1f%%\n",
			len(ts.history)-i,
			entry["timestamp"].(string)[:19],
			entry["language"],
			entry["mode"],
			entry["cpm"],
			entry["wpm"],
			entry["accuracy"].(float64))
	}
}

func main() {
	var (
		language    string
		mode        string
		length      int
		exportStats string
		history     bool
	)
	flag.StringVar(&language, "language", "en", "Язык: en, ru")
	flag.StringVar(&mode, "mode", "words", "Режим: words, sentences, chars")
	flag.IntVar(&length, "length", 30, "Количество слов или символов")
	flag.StringVar(&exportStats, "export-stats", "", "Экспорт статистики")
	flag.BoolVar(&history, "history", false, "Показать историю")
	flag.Parse()

	ts := NewTypingSpeed(language, mode, length, exportStats)
	if history {
		ts.showHistory()
	} else {
		ts.run()
	}
}
