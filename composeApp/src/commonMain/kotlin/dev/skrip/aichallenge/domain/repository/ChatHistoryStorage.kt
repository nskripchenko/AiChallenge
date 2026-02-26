package dev.skrip.aichallenge.domain.repository

import dev.skrip.aichallenge.domain.model.ConversationState

interface ChatHistoryStorage {
    suspend fun saveState(state: ConversationState)
    suspend fun loadState(): ConversationState
    suspend fun clearHistory()
}
