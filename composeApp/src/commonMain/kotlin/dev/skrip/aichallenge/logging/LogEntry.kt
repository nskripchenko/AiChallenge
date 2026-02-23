package dev.skrip.aichallenge.logging

sealed class LogEntry {
    abstract val timestamp: Long

    data class Request(
        override val timestamp: Long,
        val model: String,
        val temperature: Double,
        val maxTokens: Int,
        val requestJson: String
    ) : LogEntry()

    data class Response(
        override val timestamp: Long,
        val responseJson: String
    ) : LogEntry()

    data class Error(
        override val timestamp: Long,
        val errorMessage: String,
        val details: String?
    ) : LogEntry()
}
