package dev.skrip.aichallenge.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ModelId(
    val apiId: String,
    val label: String,
    val inputPricePerMillion: Double,
    val outputPricePerMillion: Double
) {
    // Latest generation (4.6)
    OPUS_4_6(
        apiId = "claude-opus-4-6",
        label = "Opus 4.6",
        inputPricePerMillion = 5.0,
        outputPricePerMillion = 25.0
    ),
    SONNET_4_6(
        apiId = "claude-sonnet-4-6",
        label = "Sonnet 4.6",
        inputPricePerMillion = 3.0,
        outputPricePerMillion = 15.0
    ),
    HAIKU_4_5(
        apiId = "claude-haiku-4-5-20251001",
        label = "Haiku 4.5",
        inputPricePerMillion = 1.0,
        outputPricePerMillion = 5.0
    ),

    // Previous generation (4.5)
    OPUS_4_5(
        apiId = "claude-opus-4-5-20251101",
        label = "Opus 4.5",
        inputPricePerMillion = 5.0,
        outputPricePerMillion = 25.0
    ),
    SONNET_4_5(
        apiId = "claude-sonnet-4-5-20250929",
        label = "Sonnet 4.5",
        inputPricePerMillion = 3.0,
        outputPricePerMillion = 15.0
    ),

    // Generation 4.0
    OPUS_4(
        apiId = "claude-opus-4-20250514",
        label = "Opus 4",
        inputPricePerMillion = 15.0,
        outputPricePerMillion = 75.0
    ),
    SONNET_4(
        apiId = "claude-sonnet-4-20250514",
        label = "Sonnet 4",
        inputPricePerMillion = 3.0,
        outputPricePerMillion = 15.0
    );

    val inputPricePerToken: Double get() = inputPricePerMillion / 1_000_000.0
    val outputPricePerToken: Double get() = outputPricePerMillion / 1_000_000.0

    companion object {
        fun fromApiId(apiId: String): ModelId? = entries.find { it.apiId == apiId }
    }
}
