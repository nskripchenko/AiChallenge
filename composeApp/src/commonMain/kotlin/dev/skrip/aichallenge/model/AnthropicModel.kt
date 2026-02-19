package dev.skrip.aichallenge.model

enum class AnthropicModel(
    val displayName: String,
    val modelId: String
) {
    HAIKU_3("Claude 3 Haiku", "claude-3-haiku-20240307"),
    HAIKU_45("Claude 4.5 Haiku", "claude-haiku-4-5-20251001"),
    SONNET_46("Claude 4.6 Sonnet", "claude-sonnet-4-6");

    companion object {
        fun default() = HAIKU_3
    }
}
