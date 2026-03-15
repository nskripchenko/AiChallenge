# AI Challenge - Day 20: Multi-Server MCP Orchestration

Демонстрация автоматической оркестрации инструментов с нескольких MCP серверов.

## Концепция

Claude как агент автоматически выбирает нужный инструмент с нужного сервера и выполняет сложный multi-step workflow по одному пользовательскому запросу.

```
User Request
      ↓
Claude Agent (Anthropic API)
      ↓
┌────────────────────────────────────────────────────┐
│            MCP Server Routing                       │
├────────────────────────────────────────────────────┤
│  search-mcp   →  search_posts                      │
│  summary-mcp  →  summarize_posts, extract_keywords │
│  file-mcp     →  save_to_file, read_file           │
└────────────────────────────────────────────────────┘
      ↓
Final Response → UI
```

## Быстрый старт

```bash
# 1. Собрать все MCP серверы
cd search-mcp && ./gradlew jar && cd ..
cd summary-mcp && ./gradlew jar && cd ..
cd file-mcp && ./gradlew jar && cd ..

# 2. Установить API ключ
export ANTHROPIC_API_KEY=sk-ant-...

# 3. Запустить desktop приложение
./gradlew :composeApp:run
```

## Структура проекта

```
AiChallenge/
├── search-mcp/                    # MCP сервер: поиск постов
│   └── src/main/kotlin/.../Main.kt
│
├── summary-mcp/                   # MCP сервер: суммаризация и keywords
│   └── src/main/kotlin/.../Main.kt
│
├── file-mcp/                      # MCP сервер: работа с файлами
│   └── src/main/kotlin/.../Main.kt
│
├── composeApp/
│   └── src/jvmMain/kotlin/
│       └── orchestration/
│           ├── McpClient.kt       # Клиент для MCP stdio
│           ├── McpRouter.kt       # Маршрутизация tool → server
│           ├── OrchestrationAgent.kt  # Claude agent
│           └── OrchestrationScreen.kt # Compose UI
│
└── pipeline-output/               # Сохраненные файлы
```

## MCP Серверы и инструменты

### search-mcp
- `search_posts(query)` — поиск постов в JSONPlaceholder по запросу

### summary-mcp
- `summarize_posts(postsJson)` — создание summary из JSON постов
- `extract_keywords(text)` — извлечение ключевых слов из текста

### file-mcp
- `save_to_file(filename, content)` — сохранение в pipeline-output/
- `read_file(filename)` — чтение из pipeline-output/

## Пример запроса

```
Find posts about "qui", summarize the key themes, extract keywords,
save the report to report.txt, and confirm what was saved.
```

## Что происходит

1. Пользователь вводит запрос
2. Claude анализирует и выбирает `search-mcp/search_posts`
3. Получает результаты → выбирает `summary-mcp/summarize_posts`
4. Создает summary → выбирает `summary-mcp/extract_keywords`
5. Извлекает keywords → выбирает `file-mcp/save_to_file`
6. Сохраняет файл → выбирает `file-mcp/read_file` для подтверждения
7. Возвращает финальный ответ

## Что показывает UI

- Статус подключения к каждому MCP серверу (зеленые индикаторы)
- Execution trace с именами серверов и инструментов
- Preview результатов поиска
- Preview summary
- Извлеченные keywords
- Путь к сохраненному файлу
- Финальный ответ Claude
