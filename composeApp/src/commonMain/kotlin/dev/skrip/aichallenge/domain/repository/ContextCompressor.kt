package dev.skrip.aichallenge.domain.repository

import dev.skrip.aichallenge.domain.model.ConversationState
import dev.skrip.aichallenge.domain.model.Message

interface ContextCompressor {
    suspend fun compressIfNeeded(
        state: ConversationState,
        keepRecent: Int
    ): ConversationState

    fun buildContextMessages(
        state: ConversationState,
        keepRecent: Int
    ): List<Message>
}
