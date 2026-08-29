## Тренажер печати (скорость)

Многоязычное консольное приложение для тренировки скорости печати на клавиатуре.  
Генерирует случайный текст (слова или предложения), измеряет скорость ввода (символов/мин, слов/мин) и точность, сохраняет историю тренировок.

## Особенности
- Генерация текста из словаря на русском или английском языке.
- Режимы: **слова** (последовательность слов), **предложения** (связный текст), **символы** (случайные символы).
- Подсчёт скорости: CPM (символов в минуту) и WPM (слов в минуту, 1 слово = 5 символов).
- Точность ввода (процент правильно введённых символов).
- Отображение ошибок в реальном времени (цветовая индикация при вводе).
- Настройка длины текста (количество слов или символов).
- Экспорт статистики в JSON и CSV.
- История тренировок с возможностью просмотра лучших результатов.
- Поддержка аргументов командной строки для автоматизации.

## Установка и запуск
Для каждого языка требуются соответствующие инструменты и зависимости (указаны ниже).

### Запуск на разных языках

1. **Python**  
   Установка: `pip install colorama` (опционально).  
   Запуск: `python typing_speed.py --language en --mode words --length 30`

2. **JavaScript (Node.js)**  
   Установка: `npm install commander chalk`  
   Запуск: `node typing_speed.js --language ru --mode sentences --length 20`

3. **Go**  
   Запуск: `go run typing_speed.go --language en --mode words --length 25`

4. **Rust**  
   Сборка: `cargo build --release`  
   Запуск: `cargo run -- --language en --mode words --length 25`

5. **Java**  
   Сборка: `javac -cp gson.jar TypingSpeed.java`  
   Запуск: `java -cp .;gson.jar TypingSpeed --language en --mode words --length 25`

6. **C# (.NET Core)**  
   Установка: `dotnet add package Newtonsoft.Json`  
   Запуск: `dotnet run -- --language en --mode words --length 25`

7. **C++ (Linux)**  
   Сборка: `g++ -std=c++11 -o typing_speed typing_speed.cpp -ljsoncpp`  
   Запуск: `./typing_speed --language en --mode words --length 25`

8. **Kotlin (JVM)**  
   Сборка: `kotlinc -cp gson.jar TypingSpeed.kt`  
   Запуск: `kotlin -cp .;gson.jar TypingSpeedKt --language en --mode words --length 25`

## Использование

Общие аргументы командной строки (везде, где поддерживается):

- `--language <en|ru>` – язык словаря (по умолчанию `en`).
- `--mode <words|sentences|chars>` – режим генерации текста (по умолчанию `words`).
- `--length <число>` – количество слов (в режиме `words` или `sentences`) или символов (в режиме `chars`). По умолчанию 30.
- `--export-stats <файл>` – экспортировать статистику в JSON или CSV (расширение определяет формат).
- `--history` – показать историю тренировок (если сохранялась).
- `--help` – справка.

Пример (Python):
```bash
python typing_speed.py --language ru --mode sentences --length 15 --export-stats result.json
Структура репозитория
text
/
├── README.md
├── typing_speed.py
├── typing_speed.js
├── typing_speed.go
├── typing_speed.rs
├── TypingSpeed.java
├── TypingSpeed.cs
├── typing_speed.cpp
└── TypingSpeed.kt
Лицензия
MIT
