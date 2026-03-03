package dev.skrip.aichallenge.ui.state

import dev.skrip.aichallenge.domain.model.MemoryLayer
import dev.skrip.aichallenge.domain.model.ModelId
import dev.skrip.aichallenge.domain.model.UserProfile

sealed class ChatViewEvent {
    // Chat events
    data object SendClicked : ChatViewEvent()
    data object StopGeneration : ChatViewEvent()
    data object ClearHistory : ChatViewEvent()
    data class InputChanged(val text: String) : ChatViewEvent()

    // Settings events
    data class SystemPromptChanged(val text: String) : ChatViewEvent()
    data class ModelChanged(val model: ModelId) : ChatViewEvent()
    data class TemperatureChanged(val text: String) : ChatViewEvent()
    data class MaxTokensChanged(val text: String) : ChatViewEvent()
    data class HistoryTokenLimitChanged(val text: String) : ChatViewEvent()

    // Memory events
    data class SelectMemoryLayer(val layer: MemoryLayer) : ChatViewEvent()
    data object ToggleMemoryPanel : ChatViewEvent()

    // Working memory
    data class AddToWorkingMemory(val label: String, val content: String) : ChatViewEvent()
    data class RemoveFromWorkingMemory(val itemId: String) : ChatViewEvent()
    data object ClearWorkingMemory : ChatViewEvent()

    // Long-term memory
    data class UpdateProfile(val profile: UserProfile) : ChatViewEvent()
    data class AddDecision(val title: String, val description: String) : ChatViewEvent()
    data class RemoveDecision(val decisionId: String) : ChatViewEvent()
    data class AddKnowledge(val category: String, val title: String, val content: String) : ChatViewEvent()
    data class RemoveKnowledge(val knowledgeId: String) : ChatViewEvent()
    data object ClearLongTermMemory : ChatViewEvent()

    // Short-term memory settings
    data class SetShortTermLimit(val maxMessages: Int) : ChatViewEvent()
}
