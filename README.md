# RAG Demo - Document Indexing

## День 21-24: RAG Pipeline с реранкингом, фильтрацией и анти-галлюцинациями

## Что делает приложение

1. Загружает документы из папки `docs/` (.md, .txt, .kt, .java)
2. Разбивает на чанки (Fixed-size или Structure-aware)
3. Генерирует embeddings через локальный Ollama
4. Сохраняет индекс в JSON
5. Выполняет semantic search по вопросу
6. **Day 23:** Фильтрация и реранкинг retrieved chunks
7. **Day 23:** Query rewriting для улучшения поиска
8. **Day 24:** Answerability check с fallback "не знаю"
9. **Day 24:** Извлечение цитат из документов
10. **Day 24:** Grounded prompt для анти-галлюцинаций
11. Генерирует ответ через LLM на основе найденного контекста

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

## Grounded Mode (Day 24)

**Grounded mode** - режим с защитой от галлюцинаций:

| Компонент             | Описание                                              |
|-----------------------|-------------------------------------------------------|
| **Answerability**     | Проверяет, достаточно ли информации для ответа        |
| **Fallback**          | Если релевантность < 35%, возвращает "не знаю"        |
| **Quotes**            | Извлекает цитаты из документов для подтверждения      |
| **Grounded Prompt**   | Строгий системный prompt: "отвечай ТОЛЬКО по контексту"|

### Как это работает:

```
Query → Retrieval → Answerability Check ─┬─→ (low relevance) → Fallback Answer
                                         │
                                         └─→ (ok) → Extract Quotes →
                                                    Grounded LLM → Answer with Citations
```

### UI индикация:

- **Grounded toggle**: Включает/выключает защиту от галлюцинаций
- **LOW RELEVANCE badge**: Показывается при срабатывании fallback
- **Supporting Quotes**: Цитаты из документов с указанием источника
- **Relevance %**: Показывает среднюю релевантность retrieved chunks

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
4. **Day 24:** Включить **Grounded** toggle для защиты от галлюцинаций
5. Выбрать стратегию chunking: **Fixed** или **Structured**
6. Использовать **Eval dropdown** для выбора тестового вопроса
7. Сравнить ответы, цитаты и статистику retrieval

## Retrieval Stats

В режиме RAG показывается статистика:

```
[RAG:Filtered] (1234ms) | Retrieved: 10 → Filtered: 5 → Used: 3
Query rewritten: "coroutine kotlin lightweight async"
```

В режиме **RAG + Grounded** (Day 24):

```
[RAG:Filtered+Grounded] (1500ms) | Relevance: 68% | Retrieved: 10 → Used: 3 | Quotes: 2
```

Или при низкой релевантности:

```
[RAG:Baseline+Grounded] (800ms) | FALLBACK (low relevance: 28%)
```

| Метрика   | Описание                               |
|-----------|----------------------------------------|
| Retrieved | Сколько chunks найдено semantic search |
| Filtered  | Сколько осталось после фильтрации      |
| Used      | Сколько использовано в контексте LLM   |
| Relevance | Средняя релевантность chunks (Day 24)  |
| Quotes    | Количество извлеченных цитат (Day 24)  |
| FALLBACK  | Сработал fallback "не знаю" (Day 24)   |

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
│   ├── model/           # Document, Chunk, IndexEntry, RetrievalModels, GroundedModels
│   ├── chunking/        # FixedSizeChunker, StructuredChunker
│   └── search/          # SemanticSearch (cosine similarity)
├── jvmMain/kotlin/dev/skrip/aichallenge/
│   ├── DocumentLoader   # Загрузка файлов из docs/
│   ├── ollama/          # OllamaClient (embeddings + chat + chatGrounded)
│   ├── index/           # IndexStorage, IndexBuilder
│   ├── rag/             # RagService (Plain + RAG + Grounded modes)
│   ├── retrieval/       # Day 23: QueryRewriter, ChunkFilter,
│   │                    #         HeuristicReranker, ImprovedRetrieval
│   ├── grounding/       # Day 24: QuoteExtractor, AnswerabilityChecker
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

## Архитектура Grounded Mode (Day 24)

```
┌─────────────────────────────────────────────────────────────┐
│                        User Question                         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              Improved Retrieval Pipeline (Day 23)            │
│         (Query Rewrite → Search → Filter → Rerank)          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   AnswerabilityChecker                       │
│  - Average relevance >= 35%?                                │
│  - At least 1 chunk with relevance >= 25%?                  │
└─────────────────────────────────────────────────────────────┘
                              │
            ┌─────────────────┴─────────────────┐
            ▼                                   ▼
     (relevance < 35%)                   (relevance >= 35%)
            │                                   │
            ▼                                   ▼
   ┌─────────────────┐              ┌─────────────────────┐
   │  FALLBACK MODE  │              │   QuoteExtractor    │
   │  "I don't have  │              │  - Find sentences   │
   │   enough info"  │              │  - Keyword overlap  │
   └─────────────────┘              │  - Top-3 quotes     │
                                    └─────────────────────┘
                                              │
                                              ▼
                                    ┌─────────────────────┐
                                    │  Grounded LLM Call  │
                                    │  System prompt:     │
                                    │  "Answer ONLY from  │
                                    │   provided context" │
                                    └─────────────────────┘
                                              │
                                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      GroundedAnswer                          │
│  - answer: String                                           │
│  - sources: List<GroundedSource>                            │
│  - quotes: List<AnswerQuote>                                │
│  - isFallback: Boolean                                      │
│  - averageRelevance: Float                                  │
└─────────────────────────────────────────────────────────────┘
```

## Конфигурация Grounding (Day 24)

```kotlin
data class GroundingConfig(
    val answerabilityThreshold: Float = 0.35f,  // Min avg relevance
    val minChunkRelevance: Float = 0.25f,       // Min chunk score
    val minRelevantChunks: Int = 1,             // Min chunks needed
    val maxQuotes: Int = 3,                     // Max quotes to show
    val minQuoteLength: Int = 20,               // Min quote chars
    val maxQuoteLength: Int = 200,              // Max quote chars
    val minQuoteRelevance: Float = 0.2f         // Min quote overlap
)
```
