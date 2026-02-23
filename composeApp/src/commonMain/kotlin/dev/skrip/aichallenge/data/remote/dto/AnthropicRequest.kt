package dev.skrip.aichallenge.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnthropicRequest(
    val model: String,
    @SerialName("max_tokens")
    val maxTokens: Int,
    val temperature: Double,
    val system: String? = null,
    val messages: List<AnthropicMessage>
)

@Serializable
data class AnthropicMessage(
    val role: String,
    val content: String
)
