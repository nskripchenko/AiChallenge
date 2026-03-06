package dev.skrip.aichallenge.domain.invariants

import kotlinx.coroutines.flow.StateFlow

/**
 * Storage interface for invariants.
 * Invariants are stored separately from dialog history.
 */
interface InvariantStorage {
    val state: StateFlow<InvariantState>

    suspend fun initialize()
    suspend fun addInvariant(invariant: Invariant)
    suspend fun updateInvariant(invariant: Invariant)
    suspend fun removeInvariant(id: String)
    suspend fun toggleInvariant(id: String, isActive: Boolean)
    suspend fun resetToDefaults()
}
