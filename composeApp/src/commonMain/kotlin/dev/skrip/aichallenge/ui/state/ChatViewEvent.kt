package dev.skrip.aichallenge.ui.state

import dev.skrip.aichallenge.domain.model.ContextStrategy
import dev.skrip.aichallenge.domain.model.ModelId

sealed class ChatViewEvent {
    // Common events
    data object SendClicked : ChatViewEvent()
    data object StopGeneration : ChatViewEvent()
    data object ClearHistory : ChatViewEvent()
    data class InputChanged(val text: String) : ChatViewEvent()
    data class TemperatureChanged(val text: String) : ChatViewEvent()
    data class MaxTokensChanged(val text: String) : ChatViewEvent()
    data class SystemPromptChanged(val text: String) : ChatViewEvent()
    data class ModelChanged(val model: ModelId) : ChatViewEvent()
    data class HistoryTokenLimitChanged(val text: String) : ChatViewEvent()
    data class KeepRecentMessagesChanged(val text: String) : ChatViewEvent()

    // Strategy events
    data class StrategyChanged(val strategy: ContextStrategy) : ChatViewEvent()

    // Sliding Window events
    data class WindowSizeChanged(val text: String) : ChatViewEvent()

    // Sticky Facts events
    data class AddFact(val key: String, val value: String) : ChatViewEvent()
    data class RemoveFact(val key: String) : ChatViewEvent()
    data class UpdateFact(val key: String, val newValue: String) : ChatViewEvent()

    // Branching events
    data class CreateBranch(val name: String, val fromMessageIndex: Int) : ChatViewEvent()
    data class SwitchBranch(val branchId: String) : ChatViewEvent()
    data class DeleteBranch(val branchId: String) : ChatViewEvent()
}
