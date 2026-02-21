package dev.skrip.aichallenge.model

/**
 * Предустановленные профили моделей Anthropic
 */
object ModelProfiles {

    val weak = ModelProfile(
        tier = ModelTier.WEAK,
        displayName = "Claude Haiku 4.5",
        modelId = "claude-haiku-4-5",
        inputPricePer1M = 1.0,
        outputPricePer1M = 5.0
    )

    val medium = ModelProfile(
        tier = ModelTier.MEDIUM,
        displayName = "Claude Sonnet 4.6",
        modelId = "claude-sonnet-4-6",
        inputPricePer1M = 3.0,
        outputPricePer1M = 15.0
    )

    val strong = ModelProfile(
        tier = ModelTier.STRONG,
        displayName = "Claude Opus 4.6",
        modelId = "claude-opus-4-6",
        inputPricePer1M = 5.0,
        outputPricePer1M = 25.0
    )

    val all = listOf(weak, medium, strong)

    fun byTier(tier: ModelTier): ModelProfile = when (tier) {
        ModelTier.WEAK -> weak
        ModelTier.MEDIUM -> medium
        ModelTier.STRONG -> strong
    }
}
