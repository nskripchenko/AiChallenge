package dev.skrip.aichallenge.model

data class RequestLogEntry(
    val timestamp: String,
    val temperature: Double,
    val promptPreview: String
)

data class ResponseLogEntry(
    val timestamp: String,
    val temperature: Double,
    val lengthChars: Int,
    val isError: Boolean = false
)
