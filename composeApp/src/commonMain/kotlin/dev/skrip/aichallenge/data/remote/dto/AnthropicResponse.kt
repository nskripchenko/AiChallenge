package dev.skrip.aichallenge.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnthropicResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ContentBlock>,
    val model: String,
    @SerialName("stop_reason")
    val stopReason: String?,
    @SerialName("stop_sequence")
    val stopSequence: String?,
    val usage: Usage
)

@Serializable
data class ContentBlock(
    val type: String,
    val text: String? = null
)

@Serializable
data class Usage(
    @SerialName("input_tokens")
    val inputTokens: Int,
    @SerialName("output_tokens")
    val outputTokens: Int
)

@Serializable
data class AnthropicErrorResponse(
    val type: String,
    val error: AnthropicError
)

@Serializable
data class AnthropicError(
    val type: String,
    val message: String
)
