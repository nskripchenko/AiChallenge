package dev.skrip.aichallenge.ui.modelbench

import dev.skrip.aichallenge.model.ModelTier

/**
 * Состояние карточки одной модели
 */
data class ModelCardState(
    val tier: ModelTier,
    val status: ModelCardStatus = ModelCardStatus.IDLE,
    val responseText: String = "",
    val durationMs: Long = 0,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val cost: Double = 0.0,
    val errorMessage: String? = null
)

/**
 * Статус выполнения запроса
 */
enum class ModelCardStatus(val displayName: String) {
    IDLE("Ожидание ввода"),
    LOADING("Выполняется…"),
    DONE("Готово"),
    ERROR("Ошибка")
}

/**
 * Общее состояние экрана Model Bench
 */
data class ModelBenchScreenState(
    val prompt: String = "",
    val isRunning: Boolean = false,
    val cards: Map<ModelTier, ModelCardState> = ModelTier.entries.associateWith { ModelCardState(it) }
)

/**
 * Результаты сравнения моделей
 */
data class ComparisonResult(
    val fastestModel: String,
    val cheapestModel: String,
    val mostVerboseModel: String
)
