package dev.skrip.aichallenge.model

/**
 * Профиль модели с информацией о ценообразовании
 */
data class ModelProfile(
    val tier: ModelTier,
    val displayName: String,
    val modelId: String,
    val inputPricePer1M: Double,   // $ за 1M входных токенов
    val outputPricePer1M: Double   // $ за 1M выходных токенов
)
