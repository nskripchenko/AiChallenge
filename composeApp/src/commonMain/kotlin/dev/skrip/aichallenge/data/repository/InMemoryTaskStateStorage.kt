package dev.skrip.aichallenge.data.repository

import dev.skrip.aichallenge.domain.statemachine.TaskState
import dev.skrip.aichallenge.domain.statemachine.TaskStateStorage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * In-memory implementation of TaskStateStorage.
 * For production, this should be replaced with persistent storage.
 */
class InMemoryTaskStateStorage : TaskStateStorage {

    private var savedState: TaskState? = null
    private var serializedState: String? = null

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    override fun saveState(state: TaskState) {
        savedState = state
        // Also serialize for potential persistence
        serializedState = try {
            json.encodeToString(state)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun loadState(): TaskState? {
        return savedState
    }

    override suspend fun clearState() {
        savedState = null
        serializedState = null
    }

    override suspend fun hasState(): Boolean {
        return savedState != null
    }

    /**
     * Get serialized state for debugging/export
     */
    fun getSerializedState(): String? = serializedState

    /**
     * Load from serialized state (for import/restore)
     */
    fun loadFromSerialized(serialized: String): TaskState? {
        return try {
            json.decodeFromString<TaskState>(serialized).also {
                savedState = it
                serializedState = serialized
            }
        } catch (e: Exception) {
            null
        }
    }
}
