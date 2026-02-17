package dev.skrip.aichallenge.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модели для Anthropic Messages API
 * Документация: https://docs.anthropic.com/claude/reference/messages_post
 */

@Serializable
data class AnthropicRequest(
    val model: String,
    val messages: List<AnthropicMessage>,
    @SerialName("max_tokens") val maxTokens: Int,
    val system: List<ContentBlock>? = null,
    val temperature: Double? = null,
    @SerialName("top_p") val topP: Double? = null,
    @SerialName("stop_sequences") val stopSequences: List<String>? = null,
    val stream: Boolean? = null
)

@Serializable
data class AnthropicMessage(
    val role: String,
    val content: String
)

@Serializable
data class AnthropicResponse(
    val id: String? = null,
    val type: String? = null,
    val role: String? = null,
    val content: List<ContentBlock>? = null,
    val model: String? = null,
    @SerialName("stop_reason") val stopReason: String? = null,
    @SerialName("stop_sequence") val stopSequence: String? = null,
    val usage: Usage? = null,
    val error: AnthropicError? = null
)

@Serializable
data class ContentBlock(
    val type: String,
    val text: String? = null
)

@Serializable
data class Usage(
    @SerialName("input_tokens") val inputTokens: Int? = null,
    @SerialName("output_tokens") val outputTokens: Int? = null
)

@Serializable
data class AnthropicError(
    val type: String? = null,
    val message: String? = null
)

@Serializable
data class StreamEvent(
    val type: String,
    val message: AnthropicResponse? = null,
    val index: Int? = null,
    @SerialName("content_block") val contentBlock: ContentBlock? = null,
    val delta: StreamDelta? = null,
    val usage: Usage? = null,
    val error: AnthropicError? = null
)

@Serializable
data class StreamDelta(
    val type: String? = null,
    val text: String? = null,
    @SerialName("stop_reason") val stopReason: String? = null,
    @SerialName("stop_sequence") val stopSequence: String? = null
)
