package dev.skrip.aichallenge.domain.repository

import dev.skrip.aichallenge.domain.model.*
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for managing assistant memory across 3 layers
 */
interface MemoryManager {
    val memoryState: StateFlow<MemoryState>

    // Persistence
    suspend fun initialize()

    // Short-term memory (automatic)
    fun updateShortTermMemory(messages: List<Message>)
    fun setShortTermLimit(maxMessages: Int)

    // Working memory (explicit user control)
    suspend fun addToWorkingMemory(label: String, content: String)
    suspend fun removeFromWorkingMemory(itemId: String)
    suspend fun clearWorkingMemory()

    // Long-term memory (persistent)
    suspend fun updateProfile(profile: UserProfile)
    suspend fun addDecision(title: String, description: String)
    suspend fun removeDecision(decisionId: String)
    suspend fun addKnowledge(category: String, title: String, content: String)
    suspend fun removeKnowledge(knowledgeId: String)
    suspend fun clearLongTermMemory()

    // Clear everything
    suspend fun clearAllMemory()
}
