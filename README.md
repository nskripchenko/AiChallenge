# RAG Demo - Document Indexing

## День 21-22: RAG Pipeline с режимом сравнения

## Что делает приложение

1. Загружает документы из папки `docs/` (.md, .txt, .kt, .java)
2. Разбивает на чанки (Fixed-size или Structure-aware)
3. Генерирует embeddings через локальный Ollama
4. Сохраняет индекс в JSON
5. Выполняет semantic search по вопросу
6. Генерирует ответ через LLM на основе найденного контекста
7. **NEW:** Сравнение режимов Plain vs RAG с тестовыми вопросами

## Режимы работы

| Режим     | Описание                                     |
|-----------|----------------------------------------------|
| **Plain** | Прямой запрос к LLM без контекста (baseline) |
| **RAG**   | Retrieval-Augmented Generation: поиск + LLM  |

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
2. Выбрать режим: **Plain** или **RAG**
3. Выбрать стратегию chunking: **Fixed** или **Structured** (для RAG)
4. Использовать **Eval dropdown** для выбора тестового вопроса
5. Сравнить ответы в разных режимах

## Evaluation Questions

10 контрольных вопросов для сравнения качества:

| #  | Вопрос                                          | Ожидание (RAG)                |
|----|-------------------------------------------------|-------------------------------|
| 1  | What is a coroutine in Kotlin?                  | Lightweight thread            |
| 2  | What are the main Kotlin coroutine dispatchers? | Main, IO, Default             |
| 3  | What is a suspend function?                     | Can be paused/resumed         |
| 4  | What is the difference between val and var?     | val immutable, var mutable    |
| 5  | How does Kotlin handle null safety?             | Nullable types with ?         |
| 6  | What is a data class in Kotlin?                 | Auto equals/hashCode/toString |
| 7  | How does UserRepository fetch a user?           | Cache first, then API         |
| 8  | What validation does createUser perform?        | Email format check            |
| 9  | What are the three main layers in architecture? | Presentation/Domain/Data      |
| 10 | What dependency injection framework is used?    | Koin                          |

## Стратегии chunking

| Стратегия      | Описание                                           |
|----------------|----------------------------------------------------|
| **Fixed**      | Фиксированный размер (500 символов) с overlap (50) |
| **Structured** | По структуре: заголовки в MD, class/fun в коде     |

## Структура проекта

```
composeApp/src/
├── commonMain/kotlin/dev/skrip/aichallenge/
│   ├── model/           # Document, Chunk, IndexEntry, Config, AnswerModels
│   ├── chunking/        # FixedSizeChunker, StructuredChunker
│   └── search/          # SemanticSearch (cosine similarity)
├── jvmMain/kotlin/dev/skrip/aichallenge/
│   ├── DocumentLoader   # Загрузка файлов из docs/
│   ├── ollama/          # OllamaClient (embeddings + chat)
│   ├── index/           # IndexStorage, IndexBuilder
│   ├── rag/             # RagService (Plain + RAG modes)
│   ├── evaluation/      # EvaluationQuestions
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

## Архитектура RAG Pipeline

```
┌─────────────────────────────────────────────────────────────┐
│                        User Question                         │
└─────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              ▼                               ▼
┌─────────────────────────┐     ┌─────────────────────────────┐
│      PLAIN Mode         │     │         RAG Mode            │
│  Direct LLM Query       │     │                             │
└─────────────────────────┘     │  1. Embed query             │
              │                 │  2. Semantic search         │
              │                 │  3. Build context           │
              │                 │  4. LLM + context           │
              │                 └─────────────────────────────┘
              │                               │
              └───────────────┬───────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Answer + Sources (RAG)                    │
└─────────────────────────────────────────────────────────────┘
```
