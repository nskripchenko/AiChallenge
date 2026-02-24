package dev.skrip.aichallenge.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ModelId(
    val apiId: String,
    val label: String,
    val inputPricePerMillion: Double,
    val outputPricePerMillion: Double
) {
    SONNET(
        apiId = "claude-sonnet-4-20250514",
        label = "Claude Sonnet 4",
        inputPricePerMillion = 3.0,
        outputPricePerMillion = 15.0
    ),
    OPUS(
        apiId = "claude-opus-4-20250514",
        label = "Claude Opus 4",
        inputPricePerMillion = 15.0,
        outputPricePerMillion = 75.0
    ),
    HAIKU(
        apiId = "claude-3-5-haiku-20241022",
        label = "Claude 3.5 Haiku",
        inputPricePerMillion = 0.80,
        outputPricePerMillion = 4.0
    );

    val inputPricePerToken: Double get() = inputPricePerMillion / 1_000_000.0
    val outputPricePerToken: Double get() = outputPricePerMillion / 1_000_000.0

    companion object {
        fun fromApiId(apiId: String): ModelId? = entries.find { it.apiId == apiId }
    }
}
