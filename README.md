# RAG Demo - Document Indexing

## День 21-25: RAG Pipeline с памятью диалога и task state

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

| Компонент           | Описание                                                |
|---------------------|---------------------------------------------------------|
| **Answerability**   | Проверяет, достаточно ли информации для ответа          |
| **Fallback**        | Если релевантность < 35%, возвращает "не знаю"          |
| **Quotes**          | Извлекает цитаты из документов для подтверждения        |
| **Grounded Prompt** | Строгий системный prompt: "отвечай ТОЛЬКО по контексту" |

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

## Memory Mode (Day 25)

**Memory mode** - многоходовый чат с памятью:

| Компонент               | Описание                                        |
|-------------------------|-------------------------------------------------|
| **ConversationMemory**  | Хранит историю всех сообщений диалога           |
| **TaskState**           | Отслеживает цель, уточнения, ограничения, факты |
| **SystemPromptBuilder** | Динамически строит prompt с учетом контекста    |
| **TaskStateExtractor**  | Извлекает task state из сообщений               |

### Task State включает:

- **Goal**: Цель диалога (что пользователь хочет узнать)
- **Clarifications**: Уточнения от пользователя
- **Constraints**: Ограничения и требования
- **Discovered Facts**: Ключевые факты из документов

### Как это работает:

```
User Question
      │
      ▼
┌─────────────────────────────────────┐
│     TaskStateExtractor              │
│  - Extract goal from first message  │
│  - Track clarifications             │
│  - Note constraints                 │
└─────────────────────────────────────┘
      │
      ▼
┌─────────────────────────────────────┐
│     SystemPromptBuilder             │
│  - Base instructions                │
│  - Task state summary               │
│  - Retrieved context (RAG)          │
└─────────────────────────────────────┘
      │
      ▼
┌─────────────────────────────────────┐
│   OllamaClient.chatWithHistory()    │
│  [system] + [history] + [user]      │
└─────────────────────────────────────┘
      │
      ▼
Answer + Updated TaskState
```

### UI индикация:

- **Memory toggle**: Включает/выключает режим памяти
- **Task State panel**: Показывает цель, уточнения, ограничения
- **Memory: N msgs**: Количество сообщений в памяти
- **New Chat button**: Сбрасывает память и начинает новый диалог
