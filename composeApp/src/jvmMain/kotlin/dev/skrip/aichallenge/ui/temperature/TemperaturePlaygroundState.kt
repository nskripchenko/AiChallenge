package dev.skrip.aichallenge.ui.temperature

import dev.skrip.aichallenge.model.AnthropicModel
import dev.skrip.aichallenge.model.RequestLogEntry
import dev.skrip.aichallenge.model.ResponseLogEntry
import dev.skrip.aichallenge.model.TemperatureResult

data class TemperaturePlaygroundState(
    val prompt: String = "",
    val selectedModel: AnthropicModel = AnthropicModel.default(),
    val temperatures: List<Double> = listOf(0.0, 0.7, 1.0),
    val results: List<TemperatureResult> = listOf(
        TemperatureResult(temperature = 0.0),
        TemperatureResult(temperature = 0.7),
        TemperatureResult(temperature = 1.0)
    ),
    val requestLog: List<RequestLogEntry> = emptyList(),
    val responseLog: List<ResponseLogEntry> = emptyList(),
    val isRunning: Boolean = false,
    val errorMessage: String? = null
)
