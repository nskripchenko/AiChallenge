package dev.skrip.aichallenge.model

/**
 * Результат вызова API для получения ответа модели
 */
data class AiCompletionResult(
    val text: String,
    val inputTokens: Int?,
    val outputTokens: Int?
)
