package dev.skrip.aichallenge.model

/**
 * Режимы retrieval pipeline
 */
enum class RetrievalMode {
    /** Базовый: semantic search → top-K → LLM */
    BASELINE,

    /** С фильтрацией: semantic search → filter → rerank → top-K → LLM */
    FILTERED,

    /** С rewrite: query rewrite → semantic search → filter → rerank → top-K → LLM */
    REWRITE_FILTERED
}

/**
 * Конфигурация retrieval pipeline
 */
data class RetrievalConfig(
    /** Сколько chunks забирать из semantic search (до фильтрации) */
    val topKBeforeFiltering: Int = 10,

    /** Сколько chunks оставлять после фильтрации/reranking */
    val topKAfterFiltering: Int = 3,

    /** Минимальный порог similarity (0.0 - 1.0) */
    val similarityThreshold: Float = 0.3f,

    /** Минимальная длина текста chunk */
    val minChunkLength: Int = 50,

    /** Порог для определения дубликатов (Jaccard similarity) */
    val deduplicationThreshold: Float = 0.8f,

    /** Вес semantic similarity в итоговом score */
    val semanticWeight: Float = 0.7f,

    /** Вес keyword overlap в итоговом score */
    val keywordWeight: Float = 0.3f
)

/**
 * Статистика retrieval процесса
 */
data class RetrievalStats(
    /** Исходный запрос */
    val originalQuery: String,

    /** Переписанный запрос (null если rewrite отключен) */
    val rewrittenQuery: String? = null,

    /** Сколько chunks найдено semantic search */
    val rawRetrievedCount: Int,

    /** Сколько осталось после фильтрации */
    val afterFilteringCount: Int,

    /** Сколько использовано в финальном контексте */
    val finalUsedCount: Int,

    /** Сколько отфильтровано по similarity threshold */
    val filteredByThreshold: Int = 0,

    /** Сколько отфильтровано по длине */
    val filteredByLength: Int = 0,

    /** Сколько удалено как дубликаты */
    val filteredAsDuplicates: Int = 0
)

/**
 * Результат retrieval с расширенной информацией
 */
data class EnhancedSearchResult(
    /** Исходный chunk */
    val chunk: Chunk,

    /** Semantic similarity score (0.0 - 1.0) */
    val semanticScore: Float,

    /** Keyword overlap score (0.0 - 1.0) */
    val keywordScore: Float = 0f,

    /** Итоговый combined score */
    val combinedScore: Float = semanticScore
)

/**
 * Полный результат improved retrieval
 */
data class ImprovedRetrievalResult(
    /** Финальные результаты для LLM контекста */
    val results: List<EnhancedSearchResult>,

    /** Статистика процесса */
    val stats: RetrievalStats
)
