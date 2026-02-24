package dev.skrip.aichallenge.domain.repository

import dev.skrip.aichallenge.data.source.StreamingEvent
import dev.skrip.aichallenge.domain.model.AgentConfig
import dev.skrip.aichallenge.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatAgent {
    suspend fun sendMessage(
        userMessage: String,
        history: List<Message>,
        config: AgentConfig
    ): Result<Message>

    fun sendMessageStreaming(
        userMessage: String,
        history: List<Message>,
        config: AgentConfig
    ): Flow<StreamingEvent>
}
