package dev.skrip.aichallenge.domain

data class ChatMessage(
    val id: String,
    val role: Role,
    val content: String
)

enum class Role {
    USER,
    ASSISTANT,
    ERROR
}
