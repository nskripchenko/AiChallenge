package dev.skrip.aichallenge.model

/**
 * Результат бенчмарка для одной модели
 */
data class ModelBenchResult(
    val tier: ModelTier,
    val profile: ModelProfile,
    val text: String,
    val durationMs: Long,
    val inputTokens: Int,
    val outputTokens: Int,
    val cost: Double,
    val error: String? = null
)
