package dev.skrip.aichallenge.domain.model

data class AgentConfig(
    val systemPrompt: String?,
    val model: ModelId,
    val temperature: Double,
    val maxTokens: Int,
    val historyTokenLimit: Int
)
