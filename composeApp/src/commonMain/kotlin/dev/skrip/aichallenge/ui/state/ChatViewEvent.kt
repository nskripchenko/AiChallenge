package dev.skrip.aichallenge.ui.state

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

    // Working memory (notes)
    data class AddToWorkingMemory(val label: String, val content: String) : ChatViewEvent()
    data class RemoveFromWorkingMemory(val itemId: String) : ChatViewEvent()

    // Profile
    data class UpdateProfile(val profile: UserProfile) : ChatViewEvent()

    // Memory
    data object ClearAllMemory : ChatViewEvent()
}
