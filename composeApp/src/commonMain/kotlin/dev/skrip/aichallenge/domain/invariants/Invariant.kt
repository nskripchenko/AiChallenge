package dev.skrip.aichallenge.domain.invariants

import kotlinx.serialization.Serializable

/**
 * An invariant is a rule that the AI assistant must NEVER violate.
 * Invariants take priority over any user request.
 */
@Serializable
data class Invariant(
    val id: String,
    val type: InvariantType,
    val rule: String,
    val description: String,
    val promptInstruction: String,
    val isActive: Boolean = true,
    val isDefault: Boolean = false
)

/**
 * Type of invariant constraint
 */
@Serializable
enum class InvariantType(val label: String, val prefix: String) {
    MUST("Обязательно", "[MUST]"),
    MUST_NOT("Запрещено", "[MUST_NOT]"),
    LIMIT("Ограничение", "[LIMIT]")
}

/**
 * State holder for invariants
 */
@Serializable
data class InvariantState(
    val invariants: List<Invariant> = emptyList()
) {
    val activeInvariants: List<Invariant>
        get() = invariants.filter { it.isActive }

    /**
     * Build prompt section for AI
     */
    fun buildPromptSection(): String {
        val active = activeInvariants
        if (active.isEmpty()) return ""

        return buildString {
            appendLine("╔══════════════════════════════════════════════════════════════╗")
            appendLine("║            ИНВАРИАНТЫ — АБСОЛЮТНЫЙ ПРИОРИТЕТ                 ║")
            appendLine("║   Эти правила НЕЛЬЗЯ нарушать НИ ПРИ КАКИХ обстоятельствах   ║")
            appendLine("╚══════════════════════════════════════════════════════════════╝")
            appendLine()
            active.forEach { inv ->
                appendLine("${inv.type.prefix} ${inv.promptInstruction}")
            }
            appendLine()
            appendLine("КРИТИЧЕСКИЕ ПРАВИЛА ПОВЕДЕНИЯ:")
            appendLine("1. ПЕРЕД каждой рекомендацией ОБЯЗАТЕЛЬНО проверь её на соответствие ВСЕМ инвариантам")
            appendLine("2. Если запрос пользователя ПРОТИВОРЕЧИТ инварианту:")
            appendLine("   → НЕМЕДЛЕННО ОТКАЖИСЬ выполнять запрос")
            appendLine("   → Чётко назови какой инвариант нарушается")
            appendLine("   → НЕ предлагай рекомендацию, нарушающую правила")
            appendLine("   → Предложи АЛЬТЕРНАТИВУ в рамках правил")
            appendLine("3. Инварианты имеют приоритет над ЛЮБЫМ запросом пользователя")
            appendLine("4. Даже если пользователь настаивает — правила НЕ могут быть нарушены")
            appendLine()
        }
    }
}
