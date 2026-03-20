package dev.skrip.aichallenge.model

/**
 * Цитата из найденного chunk, подтверждающая ответ
 */
data class AnswerQuote(
    /** Текст цитаты (предложение или фрагмент) */
    val text: String,

    /** Файл-источник */
    val file: String,

    /** Секция в файле */
    val section: String?,

    /** Релевантность цитаты к запросу (0.0 - 1.0) */
    val relevance: Float
)

/**
 * Grounded ответ с источниками, цитатами и fallback флагом
 */
data class GroundedAnswer(
    /** Исходный запрос пользователя */
    val query: String,

    /** Ответ от LLM */
    val answer: String,

    /** Источники из retrieved chunks */
    val sources: List<GroundedSource>,

    /** Цитаты, подтверждающие ответ */
    val quotes: List<AnswerQuote>,

    /** Сработал ли fallback "не знаю" */
    val isFallback: Boolean,

    /** Средняя релевантность retrieved chunks */
    val averageRelevance: Float,

    /** Время выполнения в мс */
    val durationMs: Long,

    /** Режим retrieval */
    val retrievalMode: RetrievalMode,

    /** Статистика retrieval */
    val retrievalStats: RetrievalStats?
)

/**
 * Структурированный источник ответа
 */
data class GroundedSource(
    /** Файл */
    val file: String,

    /** Секция */
    val section: String?,

    /** ID chunk */
    val chunkId: String,

    /** Similarity score */
    val similarity: Float,

    /** Превью текста chunk */
    val textPreview: String
)

/**
 * Конфигурация grounding
 */
data class GroundingConfig(
    /** Минимальная средняя релевантность для ответа (ниже = fallback) */
    val answerabilityThreshold: Float = 0.35f,

    /** Минимальная релевантность отдельного chunk */
    val minChunkRelevance: Float = 0.25f,

    /** Минимальное количество релевантных chunks для ответа */
    val minRelevantChunks: Int = 1,

    /** Максимальное количество цитат */
    val maxQuotes: Int = 3,

    /** Минимальная длина цитаты (символов) */
    val minQuoteLength: Int = 20,

    /** Максимальная длина цитаты (символов) */
    val maxQuoteLength: Int = 200,

    /** Минимальная релевантность цитаты к запросу */
    val minQuoteRelevance: Float = 0.2f
)

/**
 * Результат проверки answerability
 */
data class AnswerabilityResult(
    /** Можно ли ответить на основе контекста */
    val canAnswer: Boolean,

    /** Причина, если нельзя */
    val reason: String?,

    /** Средняя релевантность chunks */
    val averageRelevance: Float,

    /** Количество релевантных chunks */
    val relevantChunkCount: Int
)
