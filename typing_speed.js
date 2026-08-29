#!/usr/bin/env node
// typing_speed.js
const { program } = require('commander');
const fs = require('fs');
const chalk = require('chalk');
const readline = require('readline');

// Словари
const WORDS_EN = [
    "apple", "orange", "banana", "grape", "peach", "mango", "lemon", "melon", "berry", "plum",
    "cloud", "storm", "rain", "snow", "wind", "sun", "moon", "star", "sky", "tree",
    "book", "table", "chair", "window", "door", "floor", "wall", "roof", "garden", "flower",
    "happy", "sad", "angry", "calm", "brave", "smart", "kind", "funny", "serious", "wild",
    "quick", "brown", "fox", "jumps", "lazy", "dog", "hello", "world", "python", "code"
];
const WORDS_RU = [
    "яблоко", "апельсин", "банан", "виноград", "персик", "манго", "лимон", "дыня", "ягода", "слива",
    "облако", "буря", "дождь", "снег", "ветер", "солнце", "луна", "звезда", "небо", "дерево",
    "книга", "стол", "стул", "окно", "дверь", "пол", "стена", "крыша", "сад", "цветок",
    "счастливый", "грустный", "злой", "спокойный", "храбрый", "умный", "добрый", "смешной", "серьёзный", "дикий",
    "быстрый", "коричневый", "лиса", "прыгает", "ленивый", "собака", "привет", "мир", "питон", "код"
];

class TypingSpeed {
    constructor(language = 'en', mode = 'words', length = 30, exportStats = null) {
        this.language = language;
        this.mode = mode;
        this.length = length;
        this.exportStats = exportStats;
        this.words = language === 'en' ? WORDS_EN : WORDS_RU;
        this.text = this._generateText();
        this.startTime = null;
        this.endTime = null;
        this.errors = 0;
        this.totalChars = 0;
        this.historyFile = 'typing_history.json';
        this.history = this._loadHistory();
        this.rl = readline.createInterface({
            input: process.stdin,
            output: process.stdout,
        });
    }

    _generateText() {
        if (this.mode === 'chars') {
            let chars = this.words.join('').slice(0, this.length);
            return chars.split('').slice(0, this.length).join('');
        } else if (this.mode === 'sentences') {
            const shuffled = this.words.sort(() => Math.random() - 0.5);
            const selected = shuffled.slice(0, Math.min(this.length, shuffled.length));
            return selected.join(' ').charAt(0).toUpperCase() + selected.join(' ').slice(1) + '.';
        } else {
            const shuffled = this.words.sort(() => Math.random() - 0.5);
            const selected = shuffled.slice(0, Math.min(this.length, shuffled.length));
            return selected.join(' ');
        }
    }

    _loadHistory() {
        try {
            if (fs.existsSync(this.historyFile)) {
                return JSON.parse(fs.readFileSync(this.historyFile, 'utf8'));
            }
        } catch (e) {}
        return [];
    }

    _saveHistory(stats) {
        this.history.push(stats);
        fs.writeFileSync(this.historyFile, JSON.stringify(this.history, null, 2));
    }

    _question(prompt) {
        return new Promise((resolve) => {
            this.rl.question(prompt, resolve);
        });
    }

    async run() {
        console.log(chalk.cyan('Введите следующий текст как можно быстрее и точнее:'));
        console.log(chalk.white(this.text));
        console.log('\nНажмите Enter, чтобы начать...');
        await this._question('');
        this.startTime = Date.now();
        console.log(chalk.green('> '));
        const userInput = await this._question('');
        this.endTime = Date.now();
        this.totalChars = userInput.length;
        const minLen = Math.min(this.text.length, userInput.length);
        this.errors = 0;
        for (let i = 0; i < minLen; i++) {
            if (this.text[i] !== userInput[i]) this.errors++;
        }
        if (userInput.length > this.text.length) this.errors += userInput.length - this.text.length;
        const elapsed = (this.endTime - this.startTime) / 1000;
        const cpm = this.totalChars / elapsed * 60;
        const wpm = cpm / 5;
        const accuracy = this.totalChars > 0 ? ((this.totalChars - this.errors) / this.totalChars * 100) : 0;
        const stats = {
            timestamp: new Date().toISOString(),
            language: this.language,
            mode: this.mode,
            length: this.length,
            text_len: this.text.length,
            typed_len: this.totalChars,
            errors: this.errors,
            cpm: parseFloat(cpm.toFixed(2)),
            wpm: parseFloat(wpm.toFixed(2)),
            accuracy: parseFloat(accuracy.toFixed(2)),
            time_sec: parseFloat(elapsed.toFixed(2))
        };
        console.log(chalk.cyan('\n--- Результаты ---'));
        console.log(chalk.green(`Скорость: ${stats.cpm.toFixed(1)} симв/мин (${stats.wpm.toFixed(1)} слов/мин)`));
        console.log(chalk.yellow(`Точность: ${stats.accuracy.toFixed(1)}%`));
        console.log(chalk.magenta(`Время: ${stats.time_sec.toFixed(2)} сек`));
        console.log(chalk.red(`Ошибок: ${stats.errors} (из ${stats.typed_len})`));
        this._saveHistory(stats);
        if (this.exportStats) {
            this._exportStats(stats);
        }
        this.rl.close();
        return stats;
    }

    _exportStats(stats) {
        const filename = this.exportStats;
        const ext = filename.split('.').pop().toLowerCase();
        if (ext === 'json') {
            fs.writeFileSync(filename, JSON.stringify(stats, null, 2));
        } else if (ext === 'csv') {
            const header = Object.keys(stats).join(',') + '\n';
            const row = Object.values(stats).join(',') + '\n';
            fs.writeFileSync(filename, header + row);
        }
        console.log(chalk.green(`Статистика сохранена в ${filename}`));
    }

    showHistory() {
        if (this.history.length === 0) {
            console.log(chalk.yellow('История пуста.'));
            return;
        }
        console.log(chalk.cyan('📊 История тренировок:'));
        for (let i = 0; i < Math.min(10, this.history.length); i++) {
            const entry = this.history[this.history.length - 1 - i];
            console.log(`  ${i+1}. ${entry.timestamp.slice(0,19)} | ${entry.language} | ${entry.mode} | CPM: ${Math.round(entry.cpm)} | WPM: ${Math.round(entry.wpm)} | Точность: ${entry.accuracy.toFixed(1)}%`);
        }
    }
}

program
    .option('-l, --language <lang>', 'Язык: en, ru', 'en')
    .option('-m, --mode <mode>', 'Режим: words, sentences, chars', 'words')
    .option('-L, --length <number>', 'Количество слов или символов', parseInt, 30)
    .option('-e, --export-stats <file>', 'Экспорт статистики')
    .option('--history', 'Показать историю')
    .parse(process.argv);

const opts = program.opts();

const trainer = new TypingSpeed(opts.language, opts.mode, opts.length, opts.exportStats);

if (opts.history) {
    trainer.showHistory();
} else {
    trainer.run();
}
