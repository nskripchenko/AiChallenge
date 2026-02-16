package dev.skrip.aichallenge.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnthropicRequest(
    val model: String,
    @SerialName("max_tokens")
    val maxTokens: Int,
    val messages: List<ApiMessage>
)

@Serializable
data class ApiMessage(
    val role: String,
    val content: String
)

@Serializable
data class AnthropicResponse(
    val content: List<ContentBlock>
)

@Serializable
data class ContentBlock(
    val type: String,
    val text: String? = null
)

@Serializable
data class AnthropicError(
    val error: ErrorDetail
)

@Serializable
data class ErrorDetail(
    val message: String
)
