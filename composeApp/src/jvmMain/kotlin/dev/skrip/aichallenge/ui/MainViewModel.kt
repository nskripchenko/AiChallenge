package dev.skrip.aichallenge.ui

import dev.skrip.aichallenge.api.AnthropicAiClient
import dev.skrip.aichallenge.api.Message
import dev.skrip.aichallenge.api.createHttpClient
import dev.skrip.aichallenge.model.AnthropicModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

class MainViewModel(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val apiKey: String? = System.getenv("ANTHROPIC_API_KEY")

    fun updatePrompt(index: Int, text: String) {
        _state.update { current ->
            val newPrompts = current.prompts.toMutableList()
            newPrompts[index] = text
            current.copy(prompts = newPrompts)
        }
    }

    fun selectModel(model: AnthropicModel) {
        _state.update { it.copy(selectedModel = model) }
    }

    fun runSingle(index: Int) {
        if (apiKey.isNullOrBlank()) {
            _state.update { it.copy(errorMessage = "ANTHROPIC_API_KEY не установлен") }
            return
        }

        val prompt = _state.value.prompts[index]
        if (prompt.isBlank()) {
            _state.update { it.copy(errorMessage = "Промпт ${index + 1} пустой") }
            return
        }

        // Set loading state
        _state.update { current ->
            val newResults = current.results.toMutableList()
            newResults[index] = PromptResult(isLoading = true)
            current.copy(results = newResults, errorMessage = null)
        }

        scope.launch {
            val httpClient = createHttpClient()
            val client = AnthropicAiClient(apiKey, httpClient)
            val model = _state.value.selectedModel

            var response = ""
            var error: String? = null
            val duration = measureTimeMillis {
                try {
                    val messages = listOf(Message(role = "user", content = prompt))
                    response = client.chat(model.modelId, messages)
                } catch (e: Exception) {
                    error = e.message ?: "Unknown error"
                }
            }

            _state.update { current ->
                val newResults = current.results.toMutableList()
                newResults[index] = PromptResult(
                    response = response,
                    durationMs = duration,
                    isLoading = false,
                    error = error
                )
                current.copy(results = newResults)
            }

            httpClient.close()
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
