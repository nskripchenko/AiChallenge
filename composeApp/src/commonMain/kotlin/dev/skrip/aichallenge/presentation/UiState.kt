package dev.skrip.aichallenge.presentation

import dev.skrip.aichallenge.domain.model.*

/**
 * Статистика сессии
 */
data class SessionStats(
    val totalInputTokens: Int = 0,
    val totalOutputTokens: Int = 0,
    val totalRequests: Int = 0,
    val totalCost: Double = 0.0
)

/**
 * Состояние UI приложения
 */
data class UiState(
    // Промпт
    val promptText: String = "",
    val systemPrompt: String = "",
    val sendToBoth: Boolean = true,

    // Multi-turn диалог
    val conversationHistory: List<ConversationMessage> = emptyList(),
    val isMultiTurnEnabled: Boolean = false,

    // История промптов
    val promptHistory: List<String> = emptyList(),

    // Результаты
    val rawResult: LlmResult = LlmResult(),
    val controlledResult: LlmResult = LlmResult(),

    // Статусы загрузки
    val rawStatus: RequestStatus = RequestStatus.IDLE,
    val controlledStatus: RequestStatus = RequestStatus.IDLE,

    // Настройки "Без ограничений" (RAW)
    val rawSettings: RawSettings = RawSettings(),

    // Настройки "С контролем"
    val controlledSettings: ControlledSettings = ControlledSettings(),

    // Логи
    val logs: List<LogEntry> = emptyList(),
    val selectedLogTab: LogTab = LogTab.REQUESTS,
    val selectedLogEntry: LogEntry? = null,

    // Статистика сессии
    val sessionStats: SessionStats = SessionStats()
)

/**
 * Настройки панели "Без ограничений" (RAW)
 * model и systemPrompt берутся из ControlledSettings для синхронизации
 */
data class RawSettings(
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1024,
    val streaming: Boolean = false
) {
    fun toConfig(model: String, systemPrompt: String): LlmRequestConfig = LlmRequestConfig(
        model = model,
        systemPrompt = systemPrompt,
        temperature = temperature,
        maxTokens = maxTokens,
        stopSequences = emptyList(),
        streaming = streaming
    )
}

/**
 * Настройки панели "С контролем"
 */
data class ControlledSettings(
    val model: String = AvailableModels.default,
    val temperature: Float = 0.7f,
    val topP: Float? = null,
    val maxTokens: Int = 1024,
    val stopSequences: List<String> = emptyList(),
    val streaming: Boolean = false
) {
    fun toConfig(systemPrompt: String): LlmRequestConfig = LlmRequestConfig(
        model = model,
        systemPrompt = systemPrompt,
        temperature = temperature,
        topP = topP,
        maxTokens = maxTokens,
        stopSequences = stopSequences,
        streaming = streaming
    )
}

/**
 * Вкладки логов
 */
enum class LogTab(val displayName: String) {
    REQUESTS("Запросы"),
    RESPONSES("Ответы"),
    ERRORS("Ошибки")
}
