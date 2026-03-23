package dev.skrip.aichallenge.model

/**
 * Day 25: Модели для памяти диалога и task state
 */

/**
 * Один ход диалога (сообщение в истории)
 */
data class ConversationTurn(
    /** Роль: "user" или "assistant" */
    val role: String,
    /** Текст сообщения */
    val content: String,
    /** Временная метка */
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Task State - состояние задачи в диалоге
 *
 * Хранит:
 * - Цель диалога (что пользователь хочет узнать/сделать)
 * - Уточнения (что пользователь уже уточнил)
 * - Ограничения и термины (зафиксированные требования)
 * - Текущий шаг (где мы находимся в решении)
 */
data class TaskState(
    /** Цель диалога (определяется из первых сообщений) */
    val goal: String? = null,

    /** Список уточнений от пользователя */
    val clarifications: List<String> = emptyList(),

    /** Ограничения и зафиксированные термины */
    val constraints: List<String> = emptyList(),

    /** Текущий шаг в решении задачи */
    val currentStep: String? = null,

    /** Ключевые факты из документов, уже найденные */
    val discoveredFacts: List<String> = emptyList()
) {
    fun isEmpty(): Boolean =
        goal == null &&
        clarifications.isEmpty() &&
        constraints.isEmpty() &&
        currentStep == null &&
        discoveredFacts.isEmpty()

    fun summary(): String = buildString {
        goal?.let { appendLine("Goal: $it") }
        if (clarifications.isNotEmpty()) {
            appendLine("Clarifications: ${clarifications.joinToString("; ")}")
        }
        if (constraints.isNotEmpty()) {
            appendLine("Constraints: ${constraints.joinToString("; ")}")
        }
        currentStep?.let { appendLine("Current step: $it") }
        if (discoveredFacts.isNotEmpty()) {
            appendLine("Known facts: ${discoveredFacts.joinToString("; ")}")
        }
    }
}

/**
 * Полная память диалога
 */
data class ConversationMemory(
    /** Уникальный ID диалога */
    val id: String,

    /** История сообщений */
    val turns: List<ConversationTurn> = emptyList(),

    /** Состояние задачи */
    val taskState: TaskState = TaskState(),

    /** Время создания */
    val createdAt: Long = System.currentTimeMillis(),

    /** Время последнего обновления */
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** Количество сообщений */
    val messageCount: Int get() = turns.size

    /** Последние N сообщений */
    fun lastTurns(n: Int): List<ConversationTurn> =
        turns.takeLast(n)

    /** Добавить сообщение пользователя */
    fun addUserMessage(content: String): ConversationMemory = copy(
        turns = turns + ConversationTurn("user", content),
        updatedAt = System.currentTimeMillis()
    )

    /** Добавить сообщение ассистента */
    fun addAssistantMessage(content: String): ConversationMemory = copy(
        turns = turns + ConversationTurn("assistant", content),
        updatedAt = System.currentTimeMillis()
    )

    /** Обновить task state */
    fun updateTaskState(newState: TaskState): ConversationMemory = copy(
        taskState = newState,
        updatedAt = System.currentTimeMillis()
    )

    /** Сбросить память (новый диалог) */
    fun reset(newId: String): ConversationMemory = ConversationMemory(id = newId)
}

/**
 * Конфигурация памяти
 */
data class MemoryConfig(
    /** Максимальное количество сообщений в контексте LLM */
    val maxHistoryTurns: Int = 10,

    /** Включить извлечение task state */
    val enableTaskStateExtraction: Boolean = true,

    /** Включить суммаризацию длинной истории */
    val enableSummarization: Boolean = false,

    /** Порог для суммаризации (количество сообщений) */
    val summarizationThreshold: Int = 20
)

/**
 * Результат ответа с обновленной памятью
 */
data class MemoryAnswerResult(
    /** Ответ от LLM */
    val answer: String,

    /** Источники */
    val sources: List<AnswerSource>,

    /** Цитаты (если grounded mode) */
    val quotes: List<AnswerQuote>,

    /** Обновленный task state */
    val updatedTaskState: TaskState,

    /** Время выполнения */
    val durationMs: Long,

    /** Режим retrieval */
    val retrievalMode: RetrievalMode,

    /** Статистика retrieval */
    val retrievalStats: RetrievalStats?,

    /** Сработал ли fallback */
    val isFallback: Boolean = false,

    /** Средняя релевантность */
    val averageRelevance: Float? = null
)
