# RAG Demo - Document Indexing

## День 21-23: RAG Pipeline с реранкингом и фильтрацией

## Что делает приложение

1. Загружает документы из папки `docs/` (.md, .txt, .kt, .java)
2. Разбивает на чанки (Fixed-size или Structure-aware)
3. Генерирует embeddings через локальный Ollama
4. Сохраняет индекс в JSON
5. Выполняет semantic search по вопросу
6. **Day 23:** Фильтрация и реранкинг retrieved chunks
7. **Day 23:** Query rewriting для улучшения поиска
8. Генерирует ответ через LLM на основе найденного контекста

## Режимы работы

| Режим     | Описание                                     |
|-----------|----------------------------------------------|
| **Plain** | Прямой запрос к LLM без контекста (baseline) |
| **RAG**   | Retrieval-Augmented Generation: поиск + LLM  |

## Режимы Retrieval (Day 23)

| Режим        | Pipeline                                               |
|--------------|--------------------------------------------------------|
| **Baseline** | query → search(top-3) → LLM                            |
| **Filtered** | query → search(top-10) → filter → rerank → top-3 → LLM |
| **Rewrite**  | query → **rewrite** → search → filter → rerank → LLM   |

### Что делает каждый этап:

- **Query Rewrite**: Переписывает вопрос в оптимизированный retrieval query
- **Filtering**: Удаляет chunks с низким similarity, короткие тексты, дубликаты
- **Reranking**: Комбинирует semantic similarity + keyword overlap

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
3. Выбрать **Retrieval mode**: Baseline / Filtered / Rewrite
4. Выбрать стратегию chunking: **Fixed** или **Structured**
5. Использовать **Eval dropdown** для выбора тестового вопроса
6. Сравнить ответы и статистику retrieval

## Retrieval Stats

В режиме RAG показывается статистика:

```
[RAG:Filtered] (1234ms) | Retrieved: 10 → Filtered: 5 → Used: 3
Query rewritten: "coroutine kotlin lightweight async"
```

| Метрика   | Описание                               |
|-----------|----------------------------------------|
| Retrieved | Сколько chunks найдено semantic search |
| Filtered  | Сколько осталось после фильтрации      |
| Used      | Сколько использовано в контексте LLM   |

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
│   ├── model/           # Document, Chunk, IndexEntry, RetrievalModels
│   ├── chunking/        # FixedSizeChunker, StructuredChunker
│   └── search/          # SemanticSearch (cosine similarity)
├── jvmMain/kotlin/dev/skrip/aichallenge/
│   ├── DocumentLoader   # Загрузка файлов из docs/
│   ├── ollama/          # OllamaClient (embeddings + chat)
│   ├── index/           # IndexStorage, IndexBuilder
│   ├── rag/             # RagService (Plain + RAG modes)
│   ├── retrieval/       # Day 23: QueryRewriter, ChunkFilter,
│   │                    #         HeuristicReranker, ImprovedRetrieval
│   ├── evaluation/      # EvaluationQuestions
│   └── ui/              # ChatScreen, ChatViewModel
└── docs/                # Тестовые документы
```

## Конфигурация

### model/Config.kt

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

## Архитектура Improved Retrieval Pipeline (Day 23)

```
┌─────────────────────────────────────────────────────────────┐
│                        User Question                         │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
   ┌─────────┐          ┌──────────┐         ┌──────────┐
   │ BASELINE│          │ FILTERED │         │ REWRITE  │
   └─────────┘          └──────────┘         └──────────┘
        │                     │                     │
        │                     │              ┌──────▼──────┐
        │                     │              │QueryRewriter│
        │                     │              │  (Ollama)   │
        │                     │              └──────┬──────┘
        │                     │                     │
        ▼                     ▼                     ▼
   ┌─────────────────────────────────────────────────────┐
   │              Semantic Search (top-K)                 │
   │         Baseline: top-3 | Filtered/Rewrite: top-10  │
   └─────────────────────────────────────────────────────┘
        │                     │                     │
        │              ┌──────▼──────┐       ┌──────▼──────┐
        │              │ ChunkFilter │       │ ChunkFilter │
        │              │ - threshold │       │ - threshold │
        │              │ - minLength │       │ - minLength │
        │              │ - dedup     │       │ - dedup     │
        │              └──────┬──────┘       └──────┬──────┘
        │                     │                     │
        │              ┌──────▼──────┐       ┌──────▼──────┐
        │              │  Reranker   │       │  Reranker   │
        │              │ semantic +  │       │ semantic +  │
        │              │ keywords    │       │ keywords    │
        │              └──────┬──────┘       └──────┬──────┘
        │                     │                     │
        ▼                     ▼                     ▼
   ┌─────────────────────────────────────────────────────┐
   │                 Final top-K chunks                   │
   └─────────────────────────────────────────────────────┘
                              │
                              ▼
   ┌─────────────────────────────────────────────────────┐
   │              Build Context + LLM Answer              │
   └─────────────────────────────────────────────────────┘
```
