package dev.skrip.aichallenge.domain.repository

import dev.skrip.aichallenge.domain.model.Message

interface ChatHistoryStorage {
    suspend fun saveHistory(messages: List<Message>)
    suspend fun loadHistory(): List<Message>
    suspend fun clearHistory()
}
