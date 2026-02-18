package dev.skrip.aichallenge.ui

import dev.skrip.aichallenge.model.AnthropicModel

data class PromptResult(
    val response: String = "",
    val durationMs: Long = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class UiState(
    val prompts: List<String> = listOf("", "", "", ""),
    val results: List<PromptResult> = listOf(PromptResult(), PromptResult(), PromptResult(), PromptResult()),
    val selectedModel: AnthropicModel = AnthropicModel.defaultCheapest(),
    val isRunning: Boolean = false,
    val errorMessage: String? = null
)
