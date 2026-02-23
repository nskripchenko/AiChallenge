package dev.skrip.aichallenge.data.source

import dev.skrip.aichallenge.domain.model.Message

interface LlmDataSource {
    suspend fun sendMessage(
        messages: List<Message>,
        model: String,
        temperature: Double,
        maxTokens: Int
    ): Result<Message>
}
