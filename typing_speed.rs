// typing_speed.rs
use chrono::{Local, Utc};
use clap::{App, Arg};
use rand::prelude::*;
use serde::{Deserialize, Serialize};
use serde_json;
use std::fs;
use std::io::{self, Write, BufRead};
use std::time::Instant;
use colored::*;

const WORDS_EN: &[&str] = &[
    "apple", "orange", "banana", "grape", "peach", "mango", "lemon", "melon", "berry", "plum",
    "cloud", "storm", "rain", "snow", "wind", "sun", "moon", "star", "sky", "tree",
    "book", "table", "chair", "window", "door", "floor", "wall", "roof", "garden", "flower",
    "happy", "sad", "angry", "calm", "brave", "smart", "kind", "funny", "serious", "wild",
    "quick", "brown", "fox", "jumps", "lazy", "dog", "hello", "world", "python", "code",
];

const WORDS_RU: &[&str] = &[
    "яблоко", "апельсин", "банан", "виноград", "персик", "манго", "лимон", "дыня", "ягода", "слива",
    "облако", "буря", "дождь", "снег", "ветер", "солнце", "луна", "звезда", "небо", "дерево",
    "книга", "стол", "стул", "окно", "дверь", "пол", "стена", "крыша", "сад", "цветок",
    "счастливый", "грустный", "злой", "спокойный", "храбрый", "умный", "добрый", "смешной", "серьёзный", "дикий",
    "быстрый", "коричневый", "лиса", "прыгает", "ленивый", "собака", "привет", "мир", "питон", "код",
];

#[derive(Serialize, Deserialize)]
struct Stats {
    timestamp: String,
    language: String,
    mode: String,
    length: usize,
    text_len: usize,
    typed_len: usize,
    errors: usize,
    cpm: f64,
    wpm: f64,
    accuracy: f64,
    time_sec: f64,
}

struct TypingSpeed {
    language: String,
    mode: String,
    length: usize,
    export_stats: Option<String>,
    words: Vec<String>,
    text: String,
    history_file: String,
    history: Vec<Stats>,
}

impl TypingSpeed {
    fn new(language: &str, mode: &str, length: usize, export_stats: Option<&str>) -> Self {
        let words = if language == "ru" {
            WORDS_RU.iter().map(|s| s.to_string()).collect()
        } else {
            WORDS_EN.iter().map(|s| s.to_string()).collect()
        };
        let mut ts = TypingSpeed {
            language: language.to_string(),
            mode: mode.to_string(),
            length,
            export_stats: export_stats.map(|s| s.to_string()),
            words,
            text: String::new(),
            history_file: "typing_history.json".to_string(),
            history: Vec::new(),
        };
        ts.text = ts.generate_text();
        ts.history = ts.load_history();
        ts
    }

    fn generate_text(&mut self) -> String {
        let mut rng = thread_rng();
        if self.mode == "chars" {
            let chars: String = self.words.join("");
            if chars.len() > self.length {
                chars[..self.length].to_string()
            } else {
                chars
            }
        } else if self.mode == "sentences" {
            let selected = self.select_random_words(self.length);
            let mut sentence = selected.join(" ");
            if !sentence.is_empty() {
                let first = sentence.chars().next().unwrap().to_uppercase().to_string();
                sentence = first + &sentence[1..];
                sentence.push('.');
            }
            sentence
        } else {
            let selected = self.select_random_words(self.length);
            selected.join(" ")
        }
    }

    fn select_random_words(&mut self, n: usize) -> Vec<String> {
        let mut rng = thread_rng();
        let mut words = self.words.clone();
        words.shuffle(&mut rng);
        let count = n.min(words.len());
        words[..count].to_vec()
    }

    fn load_history(&self) -> Vec<Stats> {
        if let Ok(data) = fs::read_to_string(&self.history_file) {
            if let Ok(history) = serde_json::from_str(&data) {
                return history;
            }
        }
        Vec::new()
    }

    fn save_history(&mut self, stats: Stats) {
        self.history.push(stats);
        let json = serde_json::to_string_pretty(&self.history).unwrap();
        fs::write(&self.history_file, json).unwrap();
    }

    fn run(&mut self) {
        let stdin = io::stdin();
        let mut stdout = io::stdout();
        println!("{}", "Введите следующий текст как можно быстрее и точнее:".cyan());
        println!("{}", self.text.white());
        println!("\nНажмите Enter, чтобы начать...");
        let mut line = String::new();
        stdin.read_line(&mut line).unwrap();
        print!("{}", "> ".green());
        stdout.flush().unwrap();
        let start = Instant::now();
        line.clear();
        stdin.read_line(&mut line).unwrap();
        let user_input = line.trim_end_matches('\n');
        let elapsed = start.elapsed().as_secs_f64();
        let typed_len = user_input.len();
        let text_len = self.text.len();
        let min_len = text_len.min(typed_len);
        let errors = (0..min_len).filter(|i| self.text.chars().nth(*i) != user_input.chars().nth(*i)).count()
            + if typed_len > text_len { typed_len - text_len } else { 0 };
        let cpm = if elapsed > 0.0 { (typed_len as f64 / elapsed) * 60.0 } else { 0.0 };
        let wpm = cpm / 5.0;
        let accuracy = if typed_len > 0 { ((typed_len - errors) as f64 / typed_len as f64) * 100.0 } else { 0.0 };
        let stats = Stats {
            timestamp: Utc::now().to_rfc3339(),
            language: self.language.clone(),
            mode: self.mode.clone(),
            length: self.length,
            text_len,
            typed_len,
            errors,
            cpm,
            wpm,
            accuracy,
            time_sec: elapsed,
        };
        println!("\n{}", "--- Результаты ---".cyan());
        println!("{}", format!("Скорость: {:.1} симв/мин ({:.1} слов/мин)", cpm, wpm).green());
        println!("{}", format!("Точность: {:.1}%", accuracy).yellow());
        println!("{}", format!("Время: {:.2} сек", elapsed).magenta());
        println!("{}", format!("Ошибок: {} (из {})", errors, typed_len).red());
        self.save_history(stats);
        if let Some(ref file) = self.export_stats {
            self.export_stats(&stats, file);
        }
    }

    fn export_stats(&self, stats: &Stats, filename: &str) {
        let ext = filename.split('.').last().unwrap_or("json");
        let content = if ext == "json" {
            serde_json::to_string_pretty(stats).unwrap()
        } else if ext == "csv" {
            let mut csv = String::new();
            for (k, v) in [
                ("timestamp", stats.timestamp.as_str()),
                ("language", stats.language.as_str()),
                ("mode", stats.mode.as_str()),
                ("length", &stats.length.to_string()),
                ("text_len", &stats.text_len.to_string()),
                ("typed_len", &stats.typed_len.to_string()),
                ("errors", &stats.errors.to_string()),
                ("cpm", &stats.cpm.to_string()),
                ("wpm", &stats.wpm.to_string()),
                ("accuracy", &stats.accuracy.to_string()),
                ("time_sec", &stats.time_sec.to_string()),
            ] {
                csv.push_str(&format!("{},{}\n", k, v));
            }
            csv
        } else {
            format!("{:?}", stats)
        };
        fs::write(filename, content).unwrap();
        println!("{}", format!("Статистика сохранена в {}", filename).green());
    }

    fn show_history(&self) {
        if self.history.is_empty() {
            println!("{}", "История пуста.".yellow());
            return;
        }
        println!("{}", "📊 История тренировок:".cyan());
        let start = if self.history.len() > 10 { self.history.len() - 10 } else { 0 };
        for (i, entry) in self.history.iter().skip(start).enumerate() {
            let ts = Local.from_utc_datetime(&entry.timestamp[..19].parse().unwrap());
            println!("  {}. {} | {} | {} | CPM: {:.0} | WPM: {:.0} | Точность: {:.1}%",
                i+1,
                ts.format("%Y-%m-%d %H:%M:%S"),
                entry.language,
                entry.mode,
                entry.cpm,
                entry.wpm,
                entry.accuracy
            );
        }
    }
}

fn main() {
    let matches = App::new("Typing Speed Trainer")
        .arg(Arg::with_name("language").long("language").takes_value(true).default_value("en"))
        .arg(Arg::with_name("mode").long("mode").takes_value(true).default_value("words"))
        .arg(Arg::with_name("length").long("length").takes_value(true).default_value("30"))
        .arg(Arg::with_name("export-stats").long("export-stats").takes_value(true))
        .arg(Arg::with_name("history").long("history"))
        .get_matches();

    let language = matches.value_of("language").unwrap();
    let mode = matches.value_of("mode").unwrap();
    let length: usize = matches.value_of("length").unwrap().parse().unwrap();
    let export_stats = matches.value_of("export-stats");
    let history = matches.is_present("history");

    let mut trainer = TypingSpeed::new(language, mode, length, export_stats);
    if history {
        trainer.show_history();
    } else {
        trainer.run();
    }
}
