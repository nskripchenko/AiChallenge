# RAG Demo - Document Indexing

## Что делает приложение

1. Загружает документы из папки `docs/` (.md, .txt, .kt, .java)
2. Разбивает на чанки (Fixed-size или Structure-aware)
3. Генерирует embeddings через локальный Ollama
4. Сохраняет индекс в JSON
5. Выполняет semantic search по вопросу
6. Генерирует ответ через LLM на основе найденного контекста

## Быстрый старт

### 1. Установить и запустить Ollama

```bash
brew install ollama
ollama serve
```

### 2. Скачать модели (в другом терминале)

```bash
ollama pull nomic-embed-text   # embeddings
ollama pull llama3.2           # chat
```

### 3. Запустить приложение

```bash
./gradlew composeApp:run
```

### 4. Использование

1. Нажать **Reindex** для построения индекса
2. Выбрать стратегию: **Fixed** или **Structured**
3. Задать вопрос в поле ввода
4. Получить ответ с источниками

## Стратегии chunking

| Стратегия | Описание |
|-----------|----------|
| **Fixed** | Фиксированный размер (500 символов) с overlap (50) |
| **Structured** | По структуре: заголовки в MD, class/fun в коде |

## Структура проекта

```
composeApp/src/
├── commonMain/kotlin/dev/skrip/aichallenge/
│   ├── model/           # Document, Chunk, IndexEntry, Config
│   ├── chunking/        # FixedSizeChunker, StructuredChunker
│   └── search/          # SemanticSearch (cosine similarity)
├── jvmMain/kotlin/dev/skrip/aichallenge/
│   ├── DocumentLoader   # Загрузка файлов из docs/
│   ├── ollama/          # OllamaClient (embeddings + chat)
│   ├── index/           # IndexStorage, IndexBuilder
│   ├── rag/             # RagService
│   └── ui/              # ChatScreen, ChatViewModel
└── docs/                # Тестовые документы
```

## Конфигурация

Файл `model/Config.kt`:

```kotlin
object Config {
    const val OLLAMA_BASE_URL = "http://localhost:11434"
    const val EMBEDDING_MODEL = "nomic-embed-text"
    const val CHAT_MODEL = "llama3.2"
    const val CHUNK_SIZE = 500
    const val CHUNK_OVERLAP = 50
    const val TOP_K = 3
}
```
