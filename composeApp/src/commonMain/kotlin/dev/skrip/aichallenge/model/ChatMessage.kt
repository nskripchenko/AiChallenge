package dev.skrip.aichallenge.model

data class SearchResult(
    val chunk: Chunk,
    val similarity: Float
)

data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val sources: List<SearchResult> = emptyList(),
    /** Цитаты из документов (Day 24) */
    val quotes: List<AnswerQuote> = emptyList(),
    /** Сработал ли fallback "не знаю" */
    val isFallback: Boolean = false,
    /** Средняя релевантность (для отображения) */
    val averageRelevance: Float? = null
)

enum class MessageRole {
    USER,
    ASSISTANT
}
