package dev.skrip.aichallenge.data

import dev.skrip.aichallenge.domain.model.LogEntry
import dev.skrip.aichallenge.domain.repository.LogStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory хранилище логов
 * Лимит: 500 записей (FIFO)
 */
class LogStoreImpl(
    private val maxEntries: Int = 500
) : LogStore {

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    override val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    override fun addLog(entry: LogEntry) {
        _logs.update { currentLogs ->
            val newList = listOf(entry) + currentLogs
            if (newList.size > maxEntries) {
                newList.dropLast(1)
            } else {
                newList
            }
        }
    }

    override fun clearLogs() {
        _logs.value = emptyList()
    }
}
