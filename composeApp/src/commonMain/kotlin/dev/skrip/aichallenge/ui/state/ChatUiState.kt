package dev.skrip.aichallenge.ui.state

import dev.skrip.aichallenge.domain.model.Message
import dev.skrip.aichallenge.domain.model.ModelId
import dev.skrip.aichallenge.util.estimateTokens

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val systemPromptText: String = "",
    val selectedModel: ModelId = ModelId.SONNET,
    val temperatureText: String = "0.7",
    val maxTokensText: String = "512",
    val historyTokenLimitText: String = "1000"
) {
    val estimatedHistoryTokens: Int
        get() = messages.sumOf { it.text.estimateTokens() }

    val historyTokenLimit: Int
        get() = historyTokenLimitText.toIntOrNull() ?: DEFAULT_HISTORY_TOKEN_LIMIT

    val historyTokensRemaining: Int
        get() = (historyTokenLimit - estimatedHistoryTokens).coerceAtLeast(0)

    val isHistoryNearLimit: Boolean
        get() = estimatedHistoryTokens > historyTokenLimit * 0.8

    companion object {
        const val DEFAULT_HISTORY_TOKEN_LIMIT = 1000
    }
}
