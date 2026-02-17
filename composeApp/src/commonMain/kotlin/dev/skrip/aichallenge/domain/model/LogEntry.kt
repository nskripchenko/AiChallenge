package dev.skrip.aichallenge.domain.model

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Тип записи лога
 */
enum class LogEntryType {
    REQUEST,
    RESPONSE,
    ERROR
}

/**
 * Запись лога
 */
data class LogEntry(
    val id: String,
    val timestamp: Long,
    val type: LogEntryType,
    val panel: LlmPanel,
    val model: String,
    val temperature: Float? = null,
    val maxTokens: Int? = null,
    val stopSequences: List<String> = emptyList(),
    val streaming: Boolean = false,
    val httpStatus: Int? = null,
    val latencyMs: Long? = null,
    val requestJson: String? = null,
    val responseJson: String? = null,
    val errorDetails: String? = null,
    val inputTokens: Int? = null,
    val outputTokens: Int? = null
) {
    fun getFormattedTime(): String {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val millis = (timestamp % 1000).toString().padStart(3, '0')
        return "${localDateTime.hour.toString().padStart(2, '0')}:" +
                "${localDateTime.minute.toString().padStart(2, '0')}:" +
                "${localDateTime.second.toString().padStart(2, '0')}.$millis"
    }

    fun getFormattedDate(): String {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${localDateTime.day.toString().padStart(2, '0')}." +
                "${localDateTime.month.ordinal.plus(1).toString().padStart(2, '0')}." +
                "${localDateTime.year} " +
                "${localDateTime.hour.toString().padStart(2, '0')}:" +
                "${localDateTime.minute.toString().padStart(2, '0')}:" +
                "${localDateTime.second.toString().padStart(2, '0')}"
    }

    fun getPanelName(): String = when (panel) {
        LlmPanel.RAW -> "Без ограничений"
        LlmPanel.CONTROLLED -> "С контролем"
    }

    fun getTypeName(): String = when (type) {
        LogEntryType.REQUEST -> "Запрос"
        LogEntryType.RESPONSE -> "Ответ"
        LogEntryType.ERROR -> "Ошибка"
    }
}
