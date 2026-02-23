package dev.skrip.aichallenge.logging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class InMemoryAgentLogger : AgentLogger {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    override val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    override fun log(entry: LogEntry) {
        _logs.update { currentLogs -> currentLogs + entry }
    }
}
