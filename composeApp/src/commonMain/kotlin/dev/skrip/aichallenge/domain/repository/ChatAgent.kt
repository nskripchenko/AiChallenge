package dev.skrip.aichallenge.domain.repository

import dev.skrip.aichallenge.domain.model.AgentConfig
import dev.skrip.aichallenge.domain.model.Message

interface ChatAgent {
    suspend fun sendMessage(
        userMessage: String,
        history: List<Message>,
        config: AgentConfig
    ): Result<Message>
}
