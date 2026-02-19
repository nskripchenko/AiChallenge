package dev.skrip.aichallenge.ui.temperature

import dev.skrip.aichallenge.api.AnthropicAiClient
import dev.skrip.aichallenge.api.createHttpClient
import dev.skrip.aichallenge.model.AnthropicModel
import dev.skrip.aichallenge.model.RequestLogEntry
import dev.skrip.aichallenge.model.ResponseLogEntry
import dev.skrip.aichallenge.model.TemperatureResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.system.measureTimeMillis

class TemperaturePlaygroundViewModel(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _state = MutableStateFlow(TemperaturePlaygroundState())
    val state: StateFlow<TemperaturePlaygroundState> = _state.asStateFlow()

    private val apiKey: String? = System.getenv("ANTHROPIC_API_KEY")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    fun updatePrompt(text: String) {
        _state.update { it.copy(prompt = text) }
    }

    fun updateTemperature(index: Int, temp: Double) {
        val clampedTemp = temp.coerceIn(0.0, 1.5)
        _state.update { current ->
            val newTemps = current.temperatures.toMutableList()
            newTemps[index] = clampedTemp
            val newResults = current.results.toMutableList()
            newResults[index] = newResults[index].copy(temperature = clampedTemp)
            current.copy(temperatures = newTemps, results = newResults)
        }
    }

    fun selectModel(model: AnthropicModel) {
        _state.update { it.copy(selectedModel = model) }
    }

    fun send() {
        if (apiKey.isNullOrBlank()) {
            _state.update { it.copy(errorMessage = "ANTHROPIC_API_KEY не установлен. Установите переменную окружения.") }
            return
        }

        val prompt = _state.value.prompt
        if (prompt.isBlank()) {
            _state.update { it.copy(errorMessage = "Введите промпт") }
            return
        }

        // Set loading state for all results
        _state.update { current ->
            val newResults = current.results.map { it.copy(isLoading = true, error = null, text = "") }
            current.copy(isRunning = true, results = newResults, errorMessage = null)
        }

        scope.launch {
            val httpClient = createHttpClient()
            val client = AnthropicAiClient(apiKey, httpClient)
            val model = _state.value.selectedModel
            val temperatures = _state.value.temperatures

            supervisorScope {
                temperatures.forEachIndexed { index, temperature ->
                    launch {
                        // Add request log entry
                        val requestTime = getCurrentTime()
                        val promptPreview = prompt.take(80) + if (prompt.length > 80) "..." else ""
                        addRequestLog(RequestLogEntry(requestTime, temperature, promptPreview))

                        var responseText = ""
                        var error: String? = null
                        var length = 0

                        val duration = measureTimeMillis {
                            try {
                                responseText = client.complete(model, prompt, temperature)
                                length = responseText.length
                            } catch (e: Exception) {
                                error = e.message ?: "Неизвестная ошибка"
                            }
                        }

                        // Update result
                        _state.update { current ->
                            val newResults = current.results.toMutableList()
                            newResults[index] = TemperatureResult(
                                temperature = temperature,
                                text = responseText,
                                isLoading = false,
                                error = error,
                                durationMs = duration,
                                length = length
                            )
                            current.copy(results = newResults)
                        }

                        // Add response log entry
                        val responseTime = getCurrentTime()
                        addResponseLog(ResponseLogEntry(
                            timestamp = responseTime,
                            temperature = temperature,
                            lengthChars = if (error != null) 0 else length,
                            isError = error != null
                        ))
                    }
                }
            }

            _state.update { it.copy(isRunning = false) }
            httpClient.close()
        }
    }

    private fun addRequestLog(entry: RequestLogEntry) {
        _state.update { current ->
            current.copy(requestLog = listOf(entry) + current.requestLog)
        }
    }

    private fun addResponseLog(entry: ResponseLogEntry) {
        _state.update { current ->
            current.copy(responseLog = listOf(entry) + current.responseLog)
        }
    }

    private fun getCurrentTime(): String {
        return LocalTime.now().format(timeFormatter)
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
