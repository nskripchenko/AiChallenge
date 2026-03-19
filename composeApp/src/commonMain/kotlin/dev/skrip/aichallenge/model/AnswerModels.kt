package dev.skrip.aichallenge.model

/**
 * Режим ответа на вопрос
 */
enum class QuestionMode {
    /** Прямой запрос к LLM без retrieval */
    PLAIN,
    /** RAG: поиск релевантных чанков + LLM */
    RAG
}

/**
 * Источник ответа (использованный чанк)
 */
data class AnswerSource(
    val file: String,
    val section: String?,
    val similarity: Float,
    val textPreview: String
)

/**
 * Результат ответа на вопрос
 */
data class AnswerResult(
    val query: String,
    val mode: QuestionMode,
    val answer: String,
    val sources: List<AnswerSource> = emptyList(),
    val durationMs: Long = 0,
    /** Режим retrieval (только для RAG) */
    val retrievalMode: RetrievalMode? = null,
    /** Статистика retrieval (только для RAG) */
    val retrievalStats: RetrievalStats? = null
)

/**
 * Контрольный вопрос для evaluation
 */
data class EvaluationQuestion(
    val id: Int,
    val question: String,
    val expectation: String,
    val expectedSources: List<String>
)

/**
 * Результат сравнения двух режимов
 */
data class ComparisonResult(
    val question: EvaluationQuestion,
    val plainResult: AnswerResult?,
    val ragResult: AnswerResult?
)
