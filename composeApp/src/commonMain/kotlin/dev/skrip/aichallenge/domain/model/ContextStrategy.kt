package dev.skrip.aichallenge.domain.model

enum class ContextStrategy(val label: String, val icon: String) {
    SLIDING_WINDOW("Sliding Window", "🪟"),
    STICKY_FACTS("Sticky Facts", "📌"),
    BRANCHING("Branching", "🌿")
}
