package dev.skrip.aichallenge.logging

import dev.skrip.aichallenge.util.generateId

sealed class LogEntry {
    abstract val id: String
    abstract val timestamp: Long

    data class Request(
        override val id: String = generateId(),
        override val timestamp: Long,
        val model: String,
        val temperature: Double,
        val maxTokens: Int,
        val requestJson: String,
        val tag: String? = null
    ) : LogEntry()

    data class Response(
        override val id: String = generateId(),
        override val timestamp: Long,
        val responseJson: String
    ) : LogEntry()

    data class Error(
        override val id: String = generateId(),
        override val timestamp: Long,
        val errorMessage: String,
        val details: String?
    ) : LogEntry()
}
