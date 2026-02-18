package dev.skrip.aichallenge.model

enum class ReasoningMode(val displayName: String) {
    DIRECT("Напрямую"),
    STEP_BY_STEP("Пошагово"),
    PROMPT_FIRST("Сначала промпт"),
    EXPERTS("Эксперты")
}
