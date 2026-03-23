package dev.skrip.aichallenge.conversation

import dev.skrip.aichallenge.model.ConversationTurn
import dev.skrip.aichallenge.model.TaskState

/**
 * Day 25: Извлечение и обновление Task State из диалога
 *
 * Анализирует сообщения и извлекает:
 * - Цель диалога
 * - Уточнения пользователя
 * - Ограничения и требования
 * - Ключевые факты из ответов
 */
class TaskStateExtractor {

    /**
     * Извлечь начальную цель из первого вопроса
     */
    fun extractGoal(userMessage: String): String? {
        val message = userMessage.lowercase()

        // Паттерны для определения цели
        return when {
            // Вопросы "что такое" / "what is"
            message.contains("what is") || message.contains("что такое") ->
                "Learn about: ${extractTopic(userMessage)}"

            // Вопросы "как" / "how"
            message.contains("how to") || message.contains("how do") || message.contains("как") ->
                "Understand how: ${extractTopic(userMessage)}"

            // Вопросы "почему" / "why"
            message.contains("why") || message.contains("почему") ->
                "Understand why: ${extractTopic(userMessage)}"

            // Сравнения
            message.contains("difference between") || message.contains("vs") || message.contains("разница") ->
                "Compare: ${extractTopic(userMessage)}"

            // Примеры
            message.contains("example") || message.contains("пример") ->
                "Get examples of: ${extractTopic(userMessage)}"

            // По умолчанию - общий вопрос
            else -> "Answer question about: ${extractTopic(userMessage)}"
        }
    }

    /**
     * Извлечь уточнения из сообщения пользователя
     */
    fun extractClarifications(
        userMessage: String,
        currentState: TaskState
    ): List<String> {
        val newClarifications = mutableListOf<String>()
        val message = userMessage.lowercase()

        // Паттерны уточнений
        when {
            // "Я имею в виду..."
            message.contains("i mean") || message.contains("я имею в виду") -> {
                newClarifications.add("Clarified: ${userMessage.take(100)}")
            }

            // "Конкретно..."
            message.contains("specifically") || message.contains("конкретно") -> {
                newClarifications.add("Specified: ${userMessage.take(100)}")
            }

            // "Например..."
            message.contains("for example") || message.contains("например") -> {
                newClarifications.add("Example context: ${userMessage.take(100)}")
            }

            // "В контексте..."
            message.contains("in context of") || message.contains("в контексте") -> {
                newClarifications.add("Context: ${userMessage.take(100)}")
            }

            // Follow-up вопросы (короткие)
            userMessage.length < 50 && currentState.goal != null -> {
                newClarifications.add("Follow-up: ${userMessage.take(100)}")
            }
        }

        return currentState.clarifications + newClarifications
    }

    /**
     * Извлечь ограничения из сообщения
     */
    fun extractConstraints(
        userMessage: String,
        currentState: TaskState
    ): List<String> {
        val newConstraints = mutableListOf<String>()
        val message = userMessage.lowercase()

        // Паттерны ограничений
        when {
            // "Только..."
            message.contains("only") || message.contains("только") -> {
                newConstraints.add("Only: ${extractAfterKeyword(userMessage, listOf("only", "только"))}")
            }

            // "Без..."
            message.contains("without") || message.contains("без") -> {
                newConstraints.add("Without: ${extractAfterKeyword(userMessage, listOf("without", "без"))}")
            }

            // "Должен..."
            message.contains("must") || message.contains("должен") -> {
                newConstraints.add("Must: ${extractAfterKeyword(userMessage, listOf("must", "должен"))}")
            }

            // Технические ограничения
            message.contains("kotlin") -> newConstraints.add("Language: Kotlin")
            message.contains("java") -> newConstraints.add("Language: Java")
            message.contains("android") -> newConstraints.add("Platform: Android")
            message.contains("coroutine") -> newConstraints.add("Topic: Coroutines")
        }

        // Дедупликация
        return (currentState.constraints + newConstraints).distinct()
    }

    /**
     * Извлечь ключевые факты из ответа ассистента
     */
    fun extractFactsFromAnswer(
        answer: String,
        currentState: TaskState
    ): List<String> {
        val newFacts = mutableListOf<String>()

        // Ищем определения и ключевые утверждения
        val sentences = answer.split(". ", ".\n")

        for (sentence in sentences.take(5)) { // Только первые 5 предложений
            val trimmed = sentence.trim()
            if (trimmed.length in 20..150) {
                // Паттерны фактов
                when {
                    trimmed.contains(" is ") && !trimmed.contains("?") -> {
                        newFacts.add(trimmed.take(150))
                    }
                    trimmed.contains(" are ") && !trimmed.contains("?") -> {
                        newFacts.add(trimmed.take(150))
                    }
                    trimmed.startsWith("The ") || trimmed.startsWith("A ") -> {
                        newFacts.add(trimmed.take(150))
                    }
                }
            }
        }

        // Ограничиваем количество фактов
        val allFacts = currentState.discoveredFacts + newFacts.take(2)
        return allFacts.takeLast(5) // Храним только последние 5 фактов
    }

    /**
     * Обновить task state на основе нового сообщения пользователя
     */
    fun updateFromUserMessage(
        userMessage: String,
        currentState: TaskState,
        isFirstMessage: Boolean
    ): TaskState {
        return currentState.copy(
            goal = if (isFirstMessage || currentState.goal == null) {
                extractGoal(userMessage)
            } else {
                currentState.goal
            },
            clarifications = extractClarifications(userMessage, currentState),
            constraints = extractConstraints(userMessage, currentState),
            currentStep = "Answering: ${userMessage.take(50)}..."
        )
    }

    /**
     * Обновить task state на основе ответа ассистента
     */
    fun updateFromAssistantAnswer(
        answer: String,
        currentState: TaskState
    ): TaskState {
        return currentState.copy(
            discoveredFacts = extractFactsFromAnswer(answer, currentState),
            currentStep = "Answered, waiting for follow-up"
        )
    }

    // === Helper methods ===

    private fun extractTopic(message: String): String {
        // Убираем вопросительные слова и берем остаток
        val cleaned = message
            .replace(Regex("^(what is|what are|how to|how do|why|tell me about|explain|describe|что такое|как|почему|расскажи о|объясни)\\s*", RegexOption.IGNORE_CASE), "")
            .replace("?", "")
            .trim()

        return if (cleaned.length > 50) cleaned.take(50) + "..." else cleaned
    }

    private fun extractAfterKeyword(message: String, keywords: List<String>): String {
        for (keyword in keywords) {
            val index = message.lowercase().indexOf(keyword)
            if (index != -1) {
                val after = message.substring(index + keyword.length).trim()
                return if (after.length > 50) after.take(50) + "..." else after
            }
        }
        return message.take(50)
    }
}
