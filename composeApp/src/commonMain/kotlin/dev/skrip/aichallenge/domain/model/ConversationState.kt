package dev.skrip.aichallenge.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ConversationState(
    val messages: List<Message> = emptyList()
)
