

```python
#!/usr/bin/env python3
# typing_speed.py
import argparse
import json
import csv
import random
import sys
import time
import os
from datetime import datetime
from colorama import init, Fore, Style

init(autoreset=True)

# Словари (упрощённые, для демонстрации)
WORDS_EN = [
    "apple", "orange", "banana", "grape", "peach", "mango", "lemon", "melon", "berry", "plum",
    "cloud", "storm", "rain", "snow", "wind", "sun", "moon", "star", "sky", "tree",
    "book", "table", "chair", "window", "door", "floor", "wall", "roof", "garden", "flower",
    "happy", "sad", "angry", "calm", "brave", "smart", "kind", "funny", "serious", "wild",
    "quick", "brown", "fox", "jumps", "lazy", "dog", "hello", "world", "python", "code"
]

WORDS_RU = [
    "яблоко", "апельсин", "банан", "виноград", "персик", "манго", "лимон", "дыня", "ягода", "слива",
    "облако", "буря", "дождь", "снег", "ветер", "солнце", "луна", "звезда", "небо", "дерево",
    "книга", "стол", "стул", "окно", "дверь", "пол", "стена", "крыша", "сад", "цветок",
    "счастливый", "грустный", "злой", "спокойный", "храбрый", "умный", "добрый", "смешной", "серьёзный", "дикий",
    "быстрый", "коричневый", "лиса", "прыгает", "ленивый", "собака", "привет", "мир", "питон", "код"
]

class TypingSpeed:
    def __init__(self, language='en', mode='words', length=30, export_stats=None):
        self.language = language
        self.mode = mode
        self.length = length
        self.export_stats = export_stats
        self.words = WORDS_EN if language == 'en' else WORDS_RU
        self.text = self._generate_text()
        self.start_time = None
        self.end_time = None
        self.errors = 0
        self.total_chars = 0
        self.history_file = "typing_history.json"
        self.history = self._load_history()

    def _generate_text(self):
        if self.mode == 'chars':
            # случайные символы (буквы + пробелы)
            chars = ''.join(self.words)[:self.length]
            return ' '.join(chars[i:i+5] for i in range(0, len(chars), 5))[:self.length]
        elif self.mode == 'sentences':
            words = random.sample(self.words, min(self.length, len(self.words)))
            sentence = ' '.join(words)
            return sentence.capitalize() + '.'
        else:  # words
            words = random.sample(self.words, min(self.length, len(self.words)))
            return ' '.join(words)

    def _load_history(self):
        if os.path.exists(self.history_file):
            try:
                with open(self.history_file, 'r') as f:
                    return json.load(f)
            except:
                return []
        return []

    def _save_history(self, stats):
        self.history.append(stats)
        with open(self.history_file, 'w') as f:
            json.dump(self.history, f, indent=2)

    def _get_input(self):
        print(Fore.CYAN + "Введите следующий текст как можно быстрее и точнее:")
        print(Fore.WHITE + self.text + Style.RESET_ALL)
        print("\nНажмите Enter, чтобы начать...")
        input()
        self.start_time = time.time()
        print(Fore.GREEN + "> ", end='', flush=True)
        user_input = input()
        self.end_time = time.time()
        return user_input

    def _calculate_stats(self, user_input):
        self.total_chars = len(user_input)
        min_len = min(len(self.text), len(user_input))
        self.errors = sum(1 for i in range(min_len) if self.text[i] != user_input[i])
        if len(user_input) > len(self.text):
            self.errors += len(user_input) - len(self.text)
        elapsed = self.end_time - self.start_time
        cpm = (self.total_chars / elapsed) * 60 if elapsed > 0 else 0
        wpm = (self.total_chars / 5 / elapsed) * 60 if elapsed > 0 else 0
        accuracy = ((self.total_chars - self.errors) / self.total_chars * 100) if self.total_chars > 0 else 0
        return {
            "timestamp": datetime.now().isoformat(),
            "language": self.language,
            "mode": self.mode,
            "length": self.length,
            "text_len": len(self.text),
            "typed_len": self.total_chars,
            "errors": self.errors,
            "cpm": round(cpm, 2),
            "wpm": round(wpm, 2),
            "accuracy": round(accuracy, 2),
            "time_sec": round(elapsed, 2)
        }

    def _display_stats(self, stats):
        print(Fore.CYAN + "\n--- Результаты ---")
        print(Fore.GREEN + f"Скорость: {stats['cpm']:.1f} симв/мин ({stats['wpm']:.1f} слов/мин)")
        print(Fore.YELLOW + f"Точность: {stats['accuracy']:.1f}%")
        print(Fore.MAGENTA + f"Время: {stats['time_sec']:.2f} сек")
        print(Fore.RED + f"Ошибок: {stats['errors']} (из {stats['typed_len']})")
        if stats['errors'] > 0:
            # Показать ошибочные позиции
            print(Fore.RED + "Ошибки в позициях:")
            for i, (expected, got) in enumerate(zip(self.text, stats.get('user_input', ''))):
                if expected != got:
                    print(f"  {i+1}: ожидалось '{expected}', введено '{got}'")

    def run(self):
        user_input = self._get_input()
        stats = self._calculate_stats(user_input)
        stats['user_input'] = user_input  # для отладки
        self._display_stats(stats)
        self._save_history(stats)
        if self.export_stats:
            self._export_stats(stats)
        return stats

    def _export_stats(self, stats):
        filename = self.export_stats
        ext = filename.split('.')[-1].lower()
        if ext == 'json':
            with open(filename, 'w') as f:
                json.dump(stats, f, indent=2)
        elif ext == 'csv':
            with open(filename, 'w', newline='') as f:
                writer = csv.DictWriter(f, fieldnames=stats.keys())
                writer.writeheader()
                writer.writerow(stats)
        print(Fore.GREEN + f"Статистика сохранена в {filename}")

    def show_history(self):
        if not self.history:
            print(Fore.YELLOW + "История пуста.")
            return
        print(Fore.CYAN + "📊 История тренировок:")
        for i, entry in enumerate(self.history[-10:], 1):
            print(f"  {i}. {entry['timestamp'][:19]} | {entry['language']} | {entry['mode']} | "
                  f"CPM: {entry['cpm']:.0f} | WPM: {entry['wpm']:.0f} | Точность: {entry['accuracy']:.1f}%")

def main():
    parser = argparse.ArgumentParser(description="Тренажер печати (скорость)")
    parser.add_argument("--language", choices=['en', 'ru'], default='en', help="Язык словаря")
    parser.add_argument("--mode", choices=['words', 'sentences', 'chars'], default='words', help="Режим")
    parser.add_argument("--length", type=int, default=30, help="Количество слов или символов")
    parser.add_argument("--export-stats", help="Экспорт статистики в файл")
    parser.add_argument("--history", action="store_true", help="Показать историю")
    args = parser.parse_args()

    trainer = TypingSpeed(
        language=args.language,
        mode=args.mode,
        length=args.length,
        export_stats=args.export_stats
    )
    if args.history:
        trainer.show_history()
    else:
        trainer.run()

if __name__ == "__main__":
    main()
