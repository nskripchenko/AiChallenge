package dev.skrip.aichallenge.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val role: String,
    val content: String
)

@Serializable
data class AnthropicRequest(
    val model: String,
    val messages: List<Message>,
    @SerialName("max_tokens")
    val maxTokens: Int,
    val temperature: Double
)

@Serializable
data class AnthropicResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ContentBlock>,
    val model: String,
    @SerialName("stop_reason")
    val stopReason: String? = null
)

@Serializable
data class ContentBlock(
    val type: String,
    val text: String? = null
)

@Serializable
data class AnthropicError(
    val type: String,
    val error: ErrorDetail
)

@Serializable
data class ErrorDetail(
    val type: String,
    val message: String
)
