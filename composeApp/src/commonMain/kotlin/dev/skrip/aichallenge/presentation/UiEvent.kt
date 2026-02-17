package dev.skrip.aichallenge.presentation

/**
 * События UI
 */
sealed class UiEvent {
    // Промпт
    data class UpdatePrompt(val text: String) : UiEvent()
    data class UpdateSystemPrompt(val text: String) : UiEvent()
    data class ToggleSendToBoth(val enabled: Boolean) : UiEvent()
    object SendClicked : UiEvent()
    object ClearClicked : UiEvent()
    object CancelRequest : UiEvent()
    data class SelectFromHistory(val prompt: String) : UiEvent()

    // Multi-turn
    data class ToggleMultiTurn(val enabled: Boolean) : UiEvent()
    object ClearConversation : UiEvent()

    // Настройки RAW
    data class ToggleRawStreaming(val enabled: Boolean) : UiEvent()

    // Настройки CONTROLLED
    data class UpdateModel(val model: String) : UiEvent()
    data class UpdateTemperature(val value: Float) : UiEvent()
    data class UpdateTopP(val value: Float?) : UiEvent()
    data class UpdateMaxTokens(val value: Int) : UiEvent()
    data class AddStopSequence(val sequence: String) : UiEvent()
    data class RemoveStopSequence(val sequence: String) : UiEvent()
    data class ToggleStreaming(val enabled: Boolean) : UiEvent()

    // Логи
    data class SelectLogTab(val tab: LogTab) : UiEvent()
    data class SelectLogEntry(val entry: dev.skrip.aichallenge.domain.model.LogEntry?) : UiEvent()
    object ClearLogs : UiEvent()
}
