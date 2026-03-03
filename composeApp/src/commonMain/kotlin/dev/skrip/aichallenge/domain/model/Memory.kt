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
    val preferences: Map<String, String> = emptyMap(),
    val context: String = "" // General user context/background
)

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

/**
 * Memory layer type for UI selection
 */
enum class MemoryLayer(val label: String, val icon: String) {
    SHORT_TERM("Краткосрочная", "💭"),
    WORKING("Рабочая", "📋"),
    LONG_TERM("Долговременная", "🧠")
}
