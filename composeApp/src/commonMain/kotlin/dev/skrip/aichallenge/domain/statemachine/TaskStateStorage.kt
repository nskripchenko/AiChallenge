package dev.skrip.aichallenge.domain.statemachine

/**
 * Interface for persisting task state.
 * Allows the state machine to be paused and resumed.
 */
interface TaskStateStorage {
    /**
     * Save the current task state
     */
    fun saveState(state: TaskState)

    /**
     * Load the saved task state, if any
     */
    suspend fun loadState(): TaskState?

    /**
     * Clear the saved state
     */
    suspend fun clearState()

    /**
     * Check if there's a saved state
     */
    suspend fun hasState(): Boolean
}
