package dev.skrip.aichallenge.ui.state

import dev.skrip.aichallenge.domain.model.ModelId

sealed class ChatViewEvent {
    data object SendClicked : ChatViewEvent()
    data object StopGeneration : ChatViewEvent()
    data object ClearHistory : ChatViewEvent()
    data class InputChanged(val text: String) : ChatViewEvent()
    data class TemperatureChanged(val text: String) : ChatViewEvent()
    data class MaxTokensChanged(val text: String) : ChatViewEvent()
    data class SystemPromptChanged(val text: String) : ChatViewEvent()
    data class ModelChanged(val model: ModelId) : ChatViewEvent()
    data class HistoryTokenLimitChanged(val text: String) : ChatViewEvent()
}
