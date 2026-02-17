package dev.skrip.aichallenge.domain.repository

import dev.skrip.aichallenge.domain.model.LogEntry
import kotlinx.coroutines.flow.StateFlow

/**
 * Хранилище логов (in-memory)
 */
interface LogStore {
    val logs: StateFlow<List<LogEntry>>

    fun addLog(entry: LogEntry)
    fun clearLogs()
}
