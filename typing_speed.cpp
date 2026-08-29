// typing_speed.cpp
#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include <algorithm>
#include <random>
#include <chrono>
#include <sstream>
#include <json/json.h> // using jsoncpp

using namespace std;

const vector<string> WORDS_EN = {
    "apple", "orange", "banana", "grape", "peach", "mango", "lemon", "melon", "berry", "plum",
    "cloud", "storm", "rain", "snow", "wind", "sun", "moon", "star", "sky", "tree",
    "book", "table", "chair", "window", "door", "floor", "wall", "roof", "garden", "flower",
    "happy", "sad", "angry", "calm", "brave", "smart", "kind", "funny", "serious", "wild",
    "quick", "brown", "fox", "jumps", "lazy", "dog", "hello", "world", "python", "code"
};

const vector<string> WORDS_RU = {
    "яблоко", "апельсин", "банан", "виноград", "персик", "манго", "лимон", "дыня", "ягода", "слива",
    "облако", "буря", "дождь", "снег", "ветер", "солнце", "луна", "звезда", "небо", "дерево",
    "книга", "стол", "стул", "окно", "дверь", "пол", "стена", "крыша", "сад", "цветок",
    "счастливый", "грустный", "злой", "спокойный", "храбрый", "умный", "добрый", "смешной", "серьёзный", "дикий",
    "быстрый", "коричневый", "лиса", "прыгает", "ленивый", "собака", "привет", "мир", "питон", "код"
};

struct Stats {
    string timestamp;
    string language, mode;
    int length, text_len, typed_len, errors;
    double cpm, wpm, accuracy, time_sec;
};

class TypingSpeed {
private:
    string language;
    string mode;
    int length;
    string exportStats;
    bool showHistory;
    vector<string> words;
    string text;
    chrono::steady_clock::time_point startTime;
    chrono::steady_clock::time_point endTime;
    int errors;
    int totalChars;
    string historyFile;
    vector<Stats> history;

    vector<string> selectRandomWords(int n) {
        vector<string> shuffled = words;
        random_device rd;
        mt19937 g(rd());
        shuffle(shuffled.begin(), shuffled.end(), g);
        int count = min(n, (int)shuffled.size());
        return vector<string>(shuffled.begin(), shuffled.begin() + count);
    }

    string generateText() {
        if (mode == "chars") {
            string all;
            for (const auto& w : words) all += w;
            random_device rd;
            mt19937 g(rd());
            uniform_int_distribution<int> dist(0, all.size()-1);
            string result;
            for (int i = 0; i < min(length, (int)all.size()); ++i) {
                result += all[dist(g)];
            }
            return result;
        } else if (mode == "sentences") {
            auto selected = selectRandomWords(length);
            string sentence;
            for (size_t i = 0; i < selected.size(); ++i) {
                if (i > 0) sentence += " ";
                sentence += selected[i];
            }
            if (!sentence.empty()) {
                sentence[0] = toupper(sentence[0]);
                sentence += ".";
            }
            return sentence;
        } else { // words
            auto selected = selectRandomWords(length);
            string result;
            for (size_t i = 0; i < selected.size(); ++i) {
                if (i > 0) result += " ";
                result += selected[i];
            }
            return result;
        }
    }

    void loadHistory() {
        ifstream ifs(historyFile);
        if (!ifs) return;
        Json::Value root;
        ifs >> root;
        for (const auto& item : root) {
            Stats s;
            s.timestamp = item["timestamp"].asString();
            s.language = item["language"].asString();
            s.mode = item["mode"].asString();
            s.length = item["length"].asInt();
            s.text_len = item["text_len"].asInt();
            s.typed_len = item["typed_len"].asInt();
            s.errors = item["errors"].asInt();
            s.cpm = item["cpm"].asDouble();
            s.wpm = item["wpm"].asDouble();
            s.accuracy = item["accuracy"].asDouble();
            s.time_sec = item["time_sec"].asDouble();
            history.push_back(s);
        }
    }

    void saveHistory(const Stats& stats) {
        history.push_back(stats);
        Json::Value root(Json::arrayValue);
        for (const auto& s : history) {
            Json::Value item;
            item["timestamp"] = s.timestamp;
            item["language"] = s.language;
            item["mode"] = s.mode;
            item["length"] = s.length;
            item["text_len"] = s.text_len;
            item["typed_len"] = s.typed_len;
            item["errors"] = s.errors;
            item["cpm"] = s.cpm;
            item["wpm"] = s.wpm;
            item["accuracy"] = s.accuracy;
            item["time_sec"] = s.time_sec;
            root.append(item);
        }
        ofstream ofs(historyFile);
        ofs << root.toStyledString();
    }

    string currentTimeISO() {
        time_t t = time(nullptr);
        char buf[64];
        strftime(buf, sizeof(buf), "%Y-%m-%dT%H:%M:%SZ", gmtime(&t));
        return string(buf);
    }

public:
    TypingSpeed(const string& lang, const string& md, int len, const string& exp, bool hist)
        : language(lang), mode(md), length(len), exportStats(exp), showHistory(hist), historyFile("typing_history.json") {
        words = (lang == "ru") ? WORDS_RU : WORDS_EN;
        text = generateText();
        errors = 0;
        totalChars = 0;
        loadHistory();
    }

    void run() {
        if (showHistory) {
            displayHistory();
            return;
        }
        cout << "\033[36mВведите следующий текст как можно быстрее и точнее:\033[0m" << endl;
        cout << "\033[37m" << text << "\033[0m" << endl;
        cout << "\nНажмите Enter, чтобы начать...";
        cin.ignore();
        startTime = chrono::steady_clock::now();
        cout << "\033[32m> \033[0m";
        string input;
        getline(cin, input);
        endTime = chrono::steady_clock::now();
        totalChars = input.size();
        int minLen = min(text.size(), input.size());
        errors = 0;
        for (int i = 0; i < minLen; ++i) {
            if (text[i] != input[i]) errors++;
        }
        if (input.size() > text.size()) errors += input.size() - text.size();
        double elapsed = chrono::duration<double>(endTime - startTime).count();
        double cpm = (totalChars / elapsed) * 60;
        double wpm = cpm / 5;
        double accuracy = totalChars > 0 ? (double)(totalChars - errors) / totalChars * 100 : 0;
        Stats stats;
        stats.timestamp = currentTimeISO();
        stats.language = language;
        stats.mode = mode;
        stats.length = length;
        stats.text_len = text.size();
        stats.typed_len = totalChars;
        stats.errors = errors;
        stats.cpm = cpm;
        stats.wpm = wpm;
        stats.accuracy = accuracy;
        stats.time_sec = elapsed;

        cout << "\n\033[36m--- Результаты ---\033[0m" << endl;
        cout << "\033[32mСкорость: " << cpm << " симв/мин (" << wpm << " слов/мин)\033[0m" << endl;
        cout << "\033[33mТочность: " << accuracy << "%\033[0m" << endl;
        cout << "\033[35mВремя: " << elapsed << " сек\033[0m" << endl;
        cout << "\033[31mОшибок: " << errors << " (из " << totalChars << ")\033[0m" << endl;

        saveHistory(stats);
        if (!exportStats.empty()) exportStatsFunc(stats);
    }

    void exportStatsFunc(const Stats& stats) {
        string ext = exportStats.substr(exportStats.find_last_of('.') + 1);
        if (ext == "json") {
            Json::Value root;
            root["timestamp"] = stats.timestamp;
            root["language"] = stats.language;
            root["mode"] = stats.mode;
            root["length"] = stats.length;
            root["text_len"] = stats.text_len;
            root["typed_len"] = stats.typed_len;
            root["errors"] = stats.errors;
            root["cpm"] = stats.cpm;
            root["wpm"] = stats.wpm;
            root["accuracy"] = stats.accuracy;
            root["time_sec"] = stats.time_sec;
            ofstream ofs(exportStats);
            ofs << root.toStyledString();
        } else if (ext == "csv") {
            ofstream ofs(exportStats);
            ofs << "timestamp,language,mode,length,text_len,typed_len,errors,cpm,wpm,accuracy,time_sec\n";
            ofs << stats.timestamp << "," << stats.language << "," << stats.mode << ","
                << stats.length << "," << stats.text_len << "," << stats.typed_len << ","
                << stats.errors << "," << stats.cpm << "," << stats.wpm << ","
                << stats.accuracy << "," << stats.time_sec << "\n";
        }
        cout << "\033[32mСтатистика сохранена в " << exportStats << "\033[0m" << endl;
    }

    void displayHistory() {
        if (history.empty()) {
            cout << "\033[33mИстория пуста.\033[0m" << endl;
            return;
        }
        cout << "\033[36m📊 История тренировок:\033[0m" << endl;
        int start = max(0, (int)history.size() - 10);
        for (int i = (int)history.size() - 1; i >= start; --i) {
            auto& s = history[i];
            cout << "  " << (history.size() - i) << ". " << s.timestamp.substr(0,19) << " | "
                 << s.language << " | " << s.mode << " | CPM: " << (int)s.cpm
                 << " | WPM: " << (int)s.wpm << " | Точность: " << s.accuracy << "%" << endl;
        }
    }
};

int main(int argc, char* argv[]) {
    string language = "en", mode = "words", exportStats;
    int length = 30;
    bool history = false;

    for (int i = 1; i < argc; ++i) {
        string arg = argv[i];
        if (arg == "--language" && i+1 < argc) language = argv[++i];
        else if (arg == "--mode" && i+1 < argc) mode = argv[++i];
        else if (arg == "--length" && i+1 < argc) length = stoi(argv[++i]);
        else if (arg == "--export-stats" && i+1 < argc) exportStats = argv[++i];
        else if (arg == "--history") history = true;
    }

    TypingSpeed trainer(language, mode, length, exportStats, history);
    trainer.run();
    return 0;
}
