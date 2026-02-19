package dev.skrip.aichallenge.model

data class TemperatureResult(
    val temperature: Double,
    val text: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val durationMs: Long = 0,
    val length: Int = 0
)
