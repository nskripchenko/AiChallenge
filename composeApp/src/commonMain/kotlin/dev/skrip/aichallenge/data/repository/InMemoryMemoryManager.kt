package dev.skrip.aichallenge.data.repository

import dev.skrip.aichallenge.domain.model.*
import dev.skrip.aichallenge.domain.repository.MemoryManager
import dev.skrip.aichallenge.domain.repository.MemoryStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Memory manager with persistent storage support
 */
class InMemoryMemoryManager(
    private val storage: MemoryStorage? = null
) : MemoryManager {

    private val _memoryState = MutableStateFlow(MemoryState())
    override val memoryState: StateFlow<MemoryState> = _memoryState.asStateFlow()

    override suspend fun initialize() {
        if (storage != null) {
            val working = storage.loadWorkingMemory()
            val longTerm = storage.loadLongTermMemory()
            _memoryState.value = _memoryState.value.copy(
                working = working,
                longTerm = longTerm
            )
        }
    }

    // Short-term memory
    override fun updateShortTermMemory(messages: List<Message>) {
        val current = _memoryState.value
        val maxMessages = current.shortTerm.maxMessages
        val recentMessages = messages.takeLast(maxMessages)
        _memoryState.value = current.copy(
            shortTerm = current.shortTerm.copy(recentMessages = recentMessages)
        )
    }

    override fun setShortTermLimit(maxMessages: Int) {
        val current = _memoryState.value
        _memoryState.value = current.copy(
            shortTerm = current.shortTerm.copy(
                maxMessages = maxMessages,
                recentMessages = current.shortTerm.recentMessages.takeLast(maxMessages)
            )
        )
    }

    // Working memory
    override suspend fun addToWorkingMemory(label: String, content: String) {
        val current = _memoryState.value
        val newItem = WorkingMemoryItem(
            id = UUID.randomUUID().toString(),
            label = label,
            content = content
        )
        val newWorking = current.working.copy(items = current.working.items + newItem)
        _memoryState.value = current.copy(working = newWorking)
        storage?.saveWorkingMemory(newWorking)
    }

    override suspend fun removeFromWorkingMemory(itemId: String) {
        val current = _memoryState.value
        val newWorking = current.working.copy(
            items = current.working.items.filter { it.id != itemId }
        )
        _memoryState.value = current.copy(working = newWorking)
        storage?.saveWorkingMemory(newWorking)
    }

    override suspend fun clearWorkingMemory() {
        val current = _memoryState.value
        val newWorking = WorkingMemory()
        _memoryState.value = current.copy(working = newWorking)
        storage?.saveWorkingMemory(newWorking)
    }

    // Long-term memory
    override suspend fun updateProfile(profile: UserProfile) {
        val current = _memoryState.value
        val newLongTerm = current.longTerm.copy(profile = profile)
        _memoryState.value = current.copy(longTerm = newLongTerm)
        storage?.saveLongTermMemory(newLongTerm)
    }

    override suspend fun addDecision(title: String, description: String) {
        val current = _memoryState.value
        val newDecision = Decision(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description
        )
        val newLongTerm = current.longTerm.copy(
            decisions = current.longTerm.decisions + newDecision
        )
        _memoryState.value = current.copy(longTerm = newLongTerm)
        storage?.saveLongTermMemory(newLongTerm)
    }

    override suspend fun removeDecision(decisionId: String) {
        val current = _memoryState.value
        val newLongTerm = current.longTerm.copy(
            decisions = current.longTerm.decisions.filter { it.id != decisionId }
        )
        _memoryState.value = current.copy(longTerm = newLongTerm)
        storage?.saveLongTermMemory(newLongTerm)
    }

    override suspend fun addKnowledge(category: String, title: String, content: String) {
        val current = _memoryState.value
        val newKnowledge = KnowledgeItem(
            id = UUID.randomUUID().toString(),
            category = category,
            title = title,
            content = content
        )
        val newLongTerm = current.longTerm.copy(
            knowledge = current.longTerm.knowledge + newKnowledge
        )
        _memoryState.value = current.copy(longTerm = newLongTerm)
        storage?.saveLongTermMemory(newLongTerm)
    }

    override suspend fun removeKnowledge(knowledgeId: String) {
        val current = _memoryState.value
        val newLongTerm = current.longTerm.copy(
            knowledge = current.longTerm.knowledge.filter { it.id != knowledgeId }
        )
        _memoryState.value = current.copy(longTerm = newLongTerm)
        storage?.saveLongTermMemory(newLongTerm)
    }

    override suspend fun clearLongTermMemory() {
        val current = _memoryState.value
        val newLongTerm = LongTermMemory()
        _memoryState.value = current.copy(longTerm = newLongTerm)
        storage?.saveLongTermMemory(newLongTerm)
    }

    override fun buildMemoryContext(): String {
        val memory = _memoryState.value
        val sections = mutableListOf<String>()

        // Long-term memory (profile and knowledge)
        val profile = memory.longTerm.profile
        if (profile.name.isNotBlank() || profile.context.isNotBlank() || profile.preferences.isNotEmpty()) {
            val profileSection = buildString {
                appendLine("=== ПРОФИЛЬ ПОЛЬЗОВАТЕЛЯ ===")
                if (profile.name.isNotBlank()) appendLine("Имя: ${profile.name}")
                if (profile.context.isNotBlank()) appendLine("Контекст: ${profile.context}")
                if (profile.preferences.isNotEmpty()) {
                    appendLine("Предпочтения:")
                    profile.preferences.forEach { (key, value) ->
                        appendLine("  - $key: $value")
                    }
                }
            }
            sections.add(profileSection)
        }

        // Decisions
        if (memory.longTerm.decisions.isNotEmpty()) {
            val decisionsSection = buildString {
                appendLine("=== ВАЖНЫЕ РЕШЕНИЯ ===")
                memory.longTerm.decisions.forEach { decision ->
                    appendLine("• ${decision.title}: ${decision.description}")
                }
            }
            sections.add(decisionsSection)
        }

        // Knowledge
        if (memory.longTerm.knowledge.isNotEmpty()) {
            val knowledgeSection = buildString {
                appendLine("=== БАЗА ЗНАНИЙ ===")
                memory.longTerm.knowledge.groupBy { it.category }.forEach { (category, items) ->
                    appendLine("[$category]")
                    items.forEach { item ->
                        appendLine("• ${item.title}: ${item.content}")
                    }
                }
            }
            sections.add(knowledgeSection)
        }

        // Working memory (current task)
        if (memory.working.items.isNotEmpty()) {
            val workingSection = buildString {
                appendLine("=== РАБОЧАЯ ПАМЯТЬ (текущая задача) ===")
                memory.working.items.forEach { item ->
                    appendLine("• ${item.label}: ${item.content}")
                }
            }
            sections.add(workingSection)
        }

        return if (sections.isEmpty()) {
            ""
        } else {
            sections.joinToString("\n\n")
        }
    }
}
