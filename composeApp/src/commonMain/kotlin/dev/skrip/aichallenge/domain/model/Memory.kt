package dev.skrip.aichallenge.domain.model

import kotlinx.serialization.Serializable

/**
 * Memory model for the assistant with 3 layers:
 * - Short-term: current dialog messages (managed automatically)
 * - Working: current task data (user explicitly adds/removes)
 * - Long-term: profile, decisions, knowledge (persisted across sessions)
 */

@Serializable
data class MemoryState(
    val shortTerm: ShortTermMemory = ShortTermMemory(),
    val working: WorkingMemory = WorkingMemory(),
    val longTerm: LongTermMemory = LongTermMemory()
)

/**
 * Short-term memory - current dialog context
 * Automatically managed based on recent messages
 */
@Serializable
data class ShortTermMemory(
    val recentMessages: List<Message> = emptyList(),
    val maxMessages: Int = 10
)

/**
 * Working memory - data for current task
 * User explicitly adds items relevant to current work
 */
@Serializable
data class WorkingMemory(
    val items: List<WorkingMemoryItem> = emptyList()
)

@Serializable
data class WorkingMemoryItem(
    val id: String,
    val label: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Long-term memory - persistent knowledge
 * Profile info, important decisions, learned knowledge
 */
@Serializable
data class LongTermMemory(
    val profile: UserProfile = UserProfile(),
    val decisions: List<Decision> = emptyList(),
    val knowledge: List<KnowledgeItem> = emptyList()
)

@Serializable
data class UserProfile(
    val name: String = "",
    val riskLevel: RiskLevel = RiskLevel.MODERATE,
    val depositAmount: Double = 0.0,
    val depositCurrency: Currency = Currency.USD,
    val targetPercent: Double = 5.0,
    val targetDays: Int = 3,
    val investmentHorizon: InvestmentHorizon = InvestmentHorizon.SHORT_TERM
) {
    val formattedDeposit: String
        get() = if (depositAmount > 0) {
            "${depositCurrency.symbol}${formatAmount(depositAmount)}"
        } else ""

    val formattedTarget: String
        get() = "+${targetPercent.toInt()}% / $targetDays days"

    private fun formatAmount(amount: Double): String = when {
        amount >= 1_000_000 -> "%.1fM".format(amount / 1_000_000)
        amount >= 1_000 -> "%.1fK".format(amount / 1_000)
        amount == amount.toLong().toDouble() -> amount.toLong().toString()
        else -> "%.2f".format(amount)
    }
}

@Serializable
enum class Currency(val symbol: String, val label: String, val apiCode: String) {
    USD("$", "USD", "usd"),
    EUR("€", "EUR", "eur"),
    RUB("₽", "RUB", "rub")
}

@Serializable
enum class RiskLevel(val label: String, val description: String) {
    CONSERVATIVE("Консервативный", "Минимальный риск, стабильные монеты (BTC, ETH)"),
    MODERATE("Умеренный", "Баланс риска и доходности, топ-20 монет"),
    AGGRESSIVE("Агрессивный", "Высокий риск, высокая потенциальная доходность, альткоины")
}

@Serializable
enum class InvestmentHorizon(val label: String, val description: String) {
    INTRADAY("Внутри дня", "Сделки в течение дня"),
    SHORT_TERM("Краткосрок", "1-7 дней"),
    MEDIUM_TERM("Среднесрок", "1-4 недели"),
    LONG_TERM("Долгосрок", "Месяцы")
}

object DefaultProfile {
    val value = UserProfile(
        name = "",
        riskLevel = RiskLevel.MODERATE,
        depositAmount = 1000.0,
        depositCurrency = Currency.USD,
        targetPercent = 5.0,
        targetDays = 3,
        investmentHorizon = InvestmentHorizon.SHORT_TERM
    )
}

@Serializable
data class Decision(
    val id: String,
    val title: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class KnowledgeItem(
    val id: String,
    val category: String,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
