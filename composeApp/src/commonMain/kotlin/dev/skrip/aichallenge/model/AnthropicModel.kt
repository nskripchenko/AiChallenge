package dev.skrip.aichallenge.model

enum class AnthropicModel(
    val displayName: String,
    val modelId: String,
    val costRank: Int  // меньше = дешевле
) {
    HAIKU_3("Claude 3 Haiku", "claude-3-haiku-20240307", 1),
    HAIKU_45("Claude 4.5 Haiku", "claude-haiku-4-5-20251001", 2),
    SONNET_46("Claude 4.6 Sonnet", "claude-sonnet-4-6", 3);

    companion object {
        fun defaultCheapest() = entries.minBy { it.costRank }
    }
}
