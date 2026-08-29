// TypingSpeed.cs
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace TypingSpeed
{
    class Program
    {
        static void Main(string[] args)
        {
            var opts = ParseArgs(args);
            var trainer = new TypingSpeed(opts);
            if (opts.History) trainer.ShowHistory();
            else trainer.Run();
        }

        static Options ParseArgs(string[] args)
        {
            var opts = new Options();
            for (int i = 0; i < args.Length; i++)
            {
                switch (args[i])
                {
                    case "--language": opts.Language = args[++i]; break;
                    case "--mode": opts.Mode = args[++i]; break;
                    case "--length": opts.Length = int.Parse(args[++i]); break;
                    case "--export-stats": opts.ExportStats = args[++i]; break;
                    case "--history": opts.History = true; break;
                }
            }
            return opts;
        }

        class Options
        {
            public string Language { get; set; } = "en";
            public string Mode { get; set; } = "words";
            public int Length { get; set; } = 30;
            public string ExportStats { get; set; }
            public bool History { get; set; }
        }

        class Stats
        {
            public string Timestamp { get; set; }
            public string Language { get; set; }
            public string Mode { get; set; }
            public int Length { get; set; }
            public int TextLen { get; set; }
            public int TypedLen { get; set; }
            public int Errors { get; set; }
            public double Cpm { get; set; }
            public double Wpm { get; set; }
            public double Accuracy { get; set; }
            public double TimeSec { get; set; }
        }

        class TypingSpeed
        {
            private readonly Options opts;
            private readonly string[] wordsEn = {
                "apple","orange","banana","grape","peach","mango","lemon","melon","berry","plum",
                "cloud","storm","rain","snow","wind","sun","moon","star","sky","tree",
                "book","table","chair","window","door","floor","wall","roof","garden","flower",
                "happy","sad","angry","calm","brave","smart","kind","funny","serious","wild",
                "quick","brown","fox","jumps","lazy","dog","hello","world","python","code"
            };
            private readonly string[] wordsRu = {
                "яблоко","апельсин","банан","виноград","персик","манго","лимон","дыня","ягода","слива",
                "облако","буря","дождь","снег","ветер","солнце","луна","звезда","небо","дерево",
                "книга","стол","стул","окно","дверь","пол","стена","крыша","сад","цветок",
                "счастливый","грустный","злой","спокойный","храбрый","умный","добрый","смешной","серьёзный","дикий",
                "быстрый","коричневый","лиса","прыгает","ленивый","собака","привет","мир","питон","код"
            };
            private string text;
            private DateTime startTime;
            private DateTime endTime;
            private int errors;
            private int totalChars;
            private readonly string historyFile = "typing_history.json";
            private List<Stats> history = new List<Stats>();

            public TypingSpeed(Options opts)
            {
                this.opts = opts;
                text = GenerateText();
                LoadHistory();
            }

            private string GenerateText()
            {
                var words = opts.Language == "ru" ? wordsRu : wordsEn;
                var rnd = new Random();
                if (opts.Mode == "chars")
                {
                    var all = string.Join("", words);
                    var chars = all.ToCharArray();
                    var sb = new System.Text.StringBuilder();
                    for (int i = 0; i < Math.Min(opts.Length, chars.Length); i++)
                        sb.Append(chars[rnd.Next(chars.Length)]);
                    return sb.ToString();
                }
                else if (opts.Mode == "sentences")
                {
                    var shuffled = words.OrderBy(_ => rnd.Next()).ToList();
                    var selected = shuffled.Take(Math.Min(opts.Length, shuffled.Count)).ToList();
                    var sentence = string.Join(" ", selected);
                    if (sentence.Length > 0)
                        sentence = char.ToUpper(sentence[0]) + sentence.Substring(1) + ".";
                    return sentence;
                }
                else
                {
                    var shuffled = words.OrderBy(_ => rnd.Next()).ToList();
                    var selected = shuffled.Take(Math.Min(opts.Length, shuffled.Count));
                    return string.Join(" ", selected);
                }
            }

            private void LoadHistory()
            {
                try
                {
                    if (File.Exists(historyFile))
                    {
                        string json = File.ReadAllText(historyFile);
                        history = JsonSerializer.Deserialize<List<Stats>>(json) ?? new List<Stats>();
                    }
                }
                catch { history = new List<Stats>(); }
            }

            private void SaveHistory(Stats stats)
            {
                history.Add(stats);
                string json = JsonSerializer.Serialize(history, new JsonSerializerOptions { WriteIndented = true });
                File.WriteAllText(historyFile, json);
            }

            public void Run()
            {
                Console.ForegroundColor = ConsoleColor.Cyan;
                Console.WriteLine("Введите следующий текст как можно быстрее и точнее:");
                Console.ForegroundColor = ConsoleColor.White;
                Console.WriteLine(text);
                Console.ResetColor();
                Console.WriteLine("\nНажмите Enter, чтобы начать...");
                Console.ReadLine();
                Console.ForegroundColor = ConsoleColor.Green;
                Console.Write("> ");
                Console.ResetColor();
                startTime = DateTime.UtcNow;
                string input = Console.ReadLine() ?? "";
                endTime = DateTime.UtcNow;
                totalChars = input.Length;
                int minLen = Math.Min(text.Length, input.Length);
                errors = 0;
                for (int i = 0; i < minLen; i++)
                    if (text[i] != input[i]) errors++;
                if (input.Length > text.Length) errors += input.Length - text.Length;
                double elapsed = (endTime - startTime).TotalSeconds;
                double cpm = totalChars / elapsed * 60;
                double wpm = cpm / 5;
                double accuracy = totalChars > 0 ? (double)(totalChars - errors) / totalChars * 100 : 0;
                var stats = new Stats
                {
                    Timestamp = DateTime.UtcNow.ToString("o"),
                    Language = opts.Language,
                    Mode = opts.Mode,
                    Length = opts.Length,
                    TextLen = text.Length,
                    TypedLen = totalChars,
                    Errors = errors,
                    Cpm = Math.Round(cpm, 2),
                    Wpm = Math.Round(wpm, 2),
                    Accuracy = Math.Round(accuracy, 2),
                    TimeSec = Math.Round(elapsed, 2)
                };
                Console.ForegroundColor = ConsoleColor.Cyan;
                Console.WriteLine("\n--- Результаты ---");
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine($"Скорость: {stats.Cpm:F1} симв/мин ({stats.Wpm:F1} слов/мин)");
                Console.ForegroundColor = ConsoleColor.Yellow;
                Console.WriteLine($"Точность: {stats.Accuracy:F1}%");
                Console.ForegroundColor = ConsoleColor.Magenta;
                Console.WriteLine($"Время: {stats.TimeSec:F2} сек");
                Console.ForegroundColor = ConsoleColor.Red;
                Console.WriteLine($"Ошибок: {stats.Errors} (из {stats.TypedLen})");
                Console.ResetColor();
                SaveHistory(stats);
                if (!string.IsNullOrEmpty(opts.ExportStats))
                    ExportStats(stats);
            }

            private void ExportStats(Stats stats)
            {
                string ext = Path.GetExtension(opts.ExportStats).ToLower().TrimStart('.');
                string content;
                if (ext == "json")
                {
                    content = JsonSerializer.Serialize(stats, new JsonSerializerOptions { WriteIndented = true });
                }
                else if (ext == "csv")
                {
                    content = "timestamp,language,mode,length,text_len,typed_len,errors,cpm,wpm,accuracy,time_sec\n";
                    content += $"{stats.Timestamp},{stats.Language},{stats.Mode},{stats.Length},{stats.TextLen},{stats.TypedLen},{stats.Errors},{stats.Cpm},{stats.Wpm},{stats.Accuracy},{stats.TimeSec}\n";
                }
                else
                {
                    content = stats.ToString();
                }
                File.WriteAllText(opts.ExportStats, content);
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine($"Статистика сохранена в {opts.ExportStats}");
                Console.ResetColor();
            }

            public void ShowHistory()
            {
                if (history.Count == 0)
                {
                    Console.ForegroundColor = ConsoleColor.Yellow;
                    Console.WriteLine("История пуста.");
                    Console.ResetColor();
                    return;
                }
                Console.ForegroundColor = ConsoleColor.Cyan;
                Console.WriteLine("📊 История тренировок:");
                Console.ResetColor();
                int start = Math.Max(0, history.Count - 10);
                for (int i = history.Count - 1; i >= start; i--)
                {
                    var s = history[i];
                    Console.WriteLine($"  {history.Count - i}. {s.Timestamp.Substring(0,19).Replace('T',' ')} | {s.Language} | {s.Mode} | CPM: {Math.Round(s.Cpm)} | WPM: {Math.Round(s.Wpm)} | Точность: {s.Accuracy:F1}%");
                }
            }
        }
    }
}
