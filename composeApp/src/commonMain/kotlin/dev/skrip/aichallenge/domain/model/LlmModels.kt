package dev.skrip.aichallenge.domain.model

/**
 * Тип панели для отображения результата
 */
enum class LlmPanel {
    RAW,
    CONTROLLED
}

/**
 * Информация о модели Claude
 */
data class ModelInfo(
    val id: String,
    val displayName: String,
    val contextWindow: Int,
    val inputPricePerMillion: Double,  // $ per 1M tokens
    val outputPricePerMillion: Double  // $ per 1M tokens
)

/**
 * Доступные модели Claude с информацией о ценах и лимитах
 */
object AvailableModels {
    val modelsInfo = listOf(
        ModelInfo("claude-sonnet-4-20250514", "Claude Sonnet 4", 200_000, 3.0, 15.0),
        ModelInfo("claude-3-5-sonnet-latest", "Claude 3.5 Sonnet", 200_000, 3.0, 15.0),
        ModelInfo("claude-3-5-haiku-latest", "Claude 3.5 Haiku", 200_000, 0.80, 4.0),
        ModelInfo("claude-3-haiku-20240307", "Claude 3 Haiku", 200_000, 0.25, 1.25)
    )

    val models = modelsInfo.map { it.id }
    val default = models.first()

    fun getInfo(modelId: String): ModelInfo? = modelsInfo.find { it.id == modelId }
}

/**
 * Конфигурация запроса к LLM
 */
data class LlmRequestConfig(
    val model: String = AvailableModels.default,
    val systemPrompt: String = "",
    val temperature: Float = 1.0f,
    val topP: Float? = null,
    val maxTokens: Int = 1024,
    val stopSequences: List<String> = emptyList(),
    val streaming: Boolean = false
)

/**
 * Результат запроса к LLM
 */
data class LlmResult(
    val text: String = "",
    val model: String = "",
    val inputTokens: Int? = null,
    val outputTokens: Int? = null,
    val latencyMs: Long = 0,
    val isStreaming: Boolean = false,
    val isComplete: Boolean = false,
    val error: String? = null
)

/**
 * Статус запроса
 */
enum class RequestStatus(val displayName: String) {
    IDLE("Готово"),
    SENDING("Отправка…"),
    STREAMING("Стриминг…"),
    ERROR("Ошибка")
}

