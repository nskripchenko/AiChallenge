# AI Reasoning Lab

Учебное приложение для сравнения 4 стратегий промптинга на одной задаче.

## Описание

AI Reasoning Lab позволяет экспериментировать с различными подходами к формулировке промптов и сравнивать качество ответов AI моделей.

## 4 режима рассуждения

1. **Напрямую (Direct)** - задача передаётся модели как есть, без дополнительных инструкций
2. **Пошагово (Step-by-Step)** - к задаче добавляется инструкция решать пошагово с анализом
3. **Сначала промпт (Prompt First)** - сначала AI генерирует оптимальный промпт, затем решает задачу по этому промпту (2 API-запроса)
4. **Эксперты (Experts)** - задача решается "советом" из трёх экспертов: Аналитика, Стратега и Критика

## Как запустить

### 1. Установите API ключ

Установите переменную окружения `ANTHROPIC_API_KEY`:

```bash
# macOS/Linux
export ANTHROPIC_API_KEY="your-api-key-here"

# Windows (PowerShell)
$env:ANTHROPIC_API_KEY="your-api-key-here"

# Windows (CMD)
set ANTHROPIC_API_KEY=your-api-key-here
```

### 2. Запустите приложение

```bash
# macOS/Linux
./gradlew :composeApp:run

# Windows
.\gradlew.bat :composeApp:run
```

## Использование

1. Введите задачу в текстовое поле или выберите одну из готовых задач
2. Выберите модель Claude (по умолчанию - самая дешёвая)
3. Опционально включите "Показать промпты" для просмотра сгенерированных промптов
4. Нажмите "Запустить сравнение"
5. Сравните ответы 4 режимов и отметьте, какие из них дали правильный ответ

## Доступные модели

- **Claude 3.5 Haiku** - самая быстрая и дешёвая (cost rank: 1)
- **Claude 3.5 Sonnet** - баланс качества и скорости (cost rank: 2)
- **Claude Sonnet 4** - высокое качество (cost rank: 3)

## Архитектура проекта

```
composeApp/src/
├── commonMain/kotlin/dev/skrip/aichallenge/
│   ├── model/           # Модели данных
│   ├── prompts/         # Генерация промптов
│   ├── api/             # HTTP клиент для Anthropic API
│   └── runner/          # Логика запуска сравнения
└── jvmMain/kotlin/dev/skrip/aichallenge/
    ├── main.kt          # Entry point
    └── ui/              # Compose UI компоненты
```

## Технический стек

- Kotlin Multiplatform
- Compose Multiplatform (Desktop)
- Ktor Client
- Kotlinx Serialization

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
