package dev.skrip.aichallenge.logging

import kotlinx.coroutines.flow.StateFlow

interface AgentLogger {
    fun log(entry: LogEntry)
    val logs: StateFlow<List<LogEntry>>
}
