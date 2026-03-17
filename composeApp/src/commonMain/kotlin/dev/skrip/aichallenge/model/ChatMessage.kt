package dev.skrip.aichallenge.model

data class SearchResult(
    val chunk: Chunk,
    val similarity: Float
)

data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val sources: List<SearchResult> = emptyList()
)

enum class MessageRole {
    USER,
    ASSISTANT
}
