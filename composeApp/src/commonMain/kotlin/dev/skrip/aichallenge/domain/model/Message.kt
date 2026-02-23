package dev.skrip.aichallenge.domain.model

data class Message(
    val id: String,
    val role: Role,
    val text: String,
    val timestamp: Long,
    val usage: TokenUsage? = null
)

data class TokenUsage(
    val inputTokens: Int,
    val outputTokens: Int,
    val responseTimeMs: Long,
    val model: ModelId
) {
    val totalTokens: Int get() = inputTokens + outputTokens

    val costUsd: Double get() {
        val inputCost = inputTokens * model.inputPricePerToken
        val outputCost = outputTokens * model.outputPricePerToken
        return inputCost + outputCost
    }

    val responseTimeSec: Double get() = responseTimeMs / 1000.0
}
