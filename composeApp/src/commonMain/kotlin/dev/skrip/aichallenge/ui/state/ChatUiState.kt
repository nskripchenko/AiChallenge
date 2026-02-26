package dev.skrip.aichallenge.ui.state

import dev.skrip.aichallenge.domain.model.Message
import dev.skrip.aichallenge.domain.model.ModelId
import dev.skrip.aichallenge.util.estimateTokens

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val isCompressing: Boolean = false,
    val streamingText: String = "",
    val errorMessage: String? = null,
    val systemPromptText: String = "",
    val selectedModel: ModelId = ModelId.SONNET_4_6,
    val temperatureText: String = "0.7",
    val maxTokensText: String = "512",
    val historyTokenLimitText: String = "1000",
    val keepRecentMessagesText: String = "10",
    val summary: String? = null,
    val summarizedCount: Int = 0
) {
    val estimatedHistoryTokens: Int
        get() = messages.sumOf { it.text.estimateTokens() }

    val historyTokenLimit: Int
        get() = historyTokenLimitText.toIntOrNull() ?: DEFAULT_HISTORY_TOKEN_LIMIT

    val historyTokensRemaining: Int
        get() = (historyTokenLimit - estimatedHistoryTokens).coerceAtLeast(0)

    val isHistoryNearLimit: Boolean
        get() = estimatedHistoryTokens > historyTokenLimit * 0.8

    val keepRecentMessages: Int
        get() = keepRecentMessagesText.toIntOrNull() ?: DEFAULT_KEEP_RECENT_MESSAGES

    val hasSummary: Boolean
        get() = summary != null && summarizedCount > 0

    // Session statistics
    val sessionStats: SessionStats
        get() {
            val usages = messages.mapNotNull { it.usage }
            return SessionStats(
                totalInputTokens = usages.sumOf { it.inputTokens },
                totalOutputTokens = usages.sumOf { it.outputTokens },
                totalCostUsd = usages.sumOf { it.costUsd },
                avgResponseTimeSec = if (usages.isNotEmpty()) {
                    usages.map { it.responseTimeSec }.average()
                } else 0.0,
                messageCount = messages.size,
                exchangeCount = usages.size
            )
        }

    companion object {
        const val DEFAULT_HISTORY_TOKEN_LIMIT = 1000
        const val DEFAULT_KEEP_RECENT_MESSAGES = 10
    }
}

data class SessionStats(
    val totalInputTokens: Int,
    val totalOutputTokens: Int,
    val totalCostUsd: Double,
    val avgResponseTimeSec: Double,
    val messageCount: Int,
    val exchangeCount: Int
) {
    val totalTokens: Int get() = totalInputTokens + totalOutputTokens
}
