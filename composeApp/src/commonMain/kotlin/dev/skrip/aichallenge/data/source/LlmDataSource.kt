package dev.skrip.aichallenge.data.source

import dev.skrip.aichallenge.domain.model.Message
import dev.skrip.aichallenge.domain.model.TokenUsage
import kotlinx.coroutines.flow.Flow

sealed class StreamingEvent {
    data class TextDelta(val text: String) : StreamingEvent()
    data class Complete(val usage: TokenUsage) : StreamingEvent()
    data class Error(val message: String) : StreamingEvent()
}

interface LlmDataSource {
    suspend fun sendMessage(
        messages: List<Message>,
        model: String,
        temperature: Double,
        maxTokens: Int,
        tag: String? = null
    ): Result<Message>

    fun sendMessageStreaming(
        messages: List<Message>,
        model: String,
        temperature: Double,
        maxTokens: Int,
        tag: String? = null
    ): Flow<StreamingEvent>
}
