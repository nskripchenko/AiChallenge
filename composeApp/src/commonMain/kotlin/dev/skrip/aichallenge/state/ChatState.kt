package dev.skrip.aichallenge.state

import dev.skrip.aichallenge.domain.ChatMessage

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false
)
