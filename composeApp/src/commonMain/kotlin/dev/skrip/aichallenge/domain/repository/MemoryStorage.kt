package dev.skrip.aichallenge.domain.repository

import dev.skrip.aichallenge.domain.model.LongTermMemory
import dev.skrip.aichallenge.domain.model.WorkingMemory

/**
 * Storage interface for persisting memory layers
 */
interface MemoryStorage {
    suspend fun saveWorkingMemory(memory: WorkingMemory)
    suspend fun loadWorkingMemory(): WorkingMemory

    suspend fun saveLongTermMemory(memory: LongTermMemory)
    suspend fun loadLongTermMemory(): LongTermMemory

    suspend fun clearAll()
}
