package dev.skrip.aichallenge.grounding

import dev.skrip.aichallenge.model.AnswerabilityResult
import dev.skrip.aichallenge.model.EnhancedSearchResult
import dev.skrip.aichallenge.model.GroundingConfig

/**
 * Проверяет, можно ли ответить на вопрос на основе retrieved chunks.
 *
 * Если релевантность слишком низкая, возвращает fallback "не знаю".
 * Это защита от галлюцинаций.
 */
class AnswerabilityChecker(
    private val config: GroundingConfig = GroundingConfig()
) {
    /**
     * Проверяет, достаточно ли информации для ответа.
     */
    fun check(results: List<EnhancedSearchResult>): AnswerabilityResult {
        // Пустой результат = нельзя ответить
        if (results.isEmpty()) {
            return AnswerabilityResult(
                canAnswer = false,
                reason = "No relevant documents found",
                averageRelevance = 0f,
                relevantChunkCount = 0
            )
        }

        // Вычисляем среднюю релевантность
        val averageRelevance = results.map { it.combinedScore }.average().toFloat()

        // Считаем количество достаточно релевантных chunks
        val relevantChunks = results.filter { it.combinedScore >= config.minChunkRelevance }
        val relevantCount = relevantChunks.size

        // Проверка 1: Есть ли хотя бы minRelevantChunks релевантных?
        if (relevantCount < config.minRelevantChunks) {
            return AnswerabilityResult(
                canAnswer = false,
                reason = "Not enough relevant chunks (found $relevantCount, need ${config.minRelevantChunks})",
                averageRelevance = averageRelevance,
                relevantChunkCount = relevantCount
            )
        }

        // Проверка 2: Средняя релевантность выше порога?
        if (averageRelevance < config.answerabilityThreshold) {
            return AnswerabilityResult(
                canAnswer = false,
                reason = "Average relevance too low (${formatPercent(averageRelevance)} < ${formatPercent(config.answerabilityThreshold)})",
                averageRelevance = averageRelevance,
                relevantChunkCount = relevantCount
            )
        }

        // Проверка 3: Лучший chunk достаточно релевантен?
        val bestScore = results.maxOf { it.combinedScore }
        if (bestScore < config.minChunkRelevance) {
            return AnswerabilityResult(
                canAnswer = false,
                reason = "Best chunk relevance too low (${formatPercent(bestScore)})",
                averageRelevance = averageRelevance,
                relevantChunkCount = relevantCount
            )
        }

        // Все проверки пройдены
        return AnswerabilityResult(
            canAnswer = true,
            reason = null,
            averageRelevance = averageRelevance,
            relevantChunkCount = relevantCount
        )
    }

    /**
     * Быстрая проверка: можно ли ответить?
     */
    fun canAnswer(results: List<EnhancedSearchResult>): Boolean {
        return check(results).canAnswer
    }

    /**
     * Возвращает fallback ответ.
     */
    fun getFallbackAnswer(): String {
        return FALLBACK_ANSWER
    }

    private fun formatPercent(value: Float): String {
        return "%.0f%%".format(value * 100)
    }

    companion object {
        const val FALLBACK_ANSWER = "I don't have enough information in the available documents to answer this question. Please try rephrasing or ask about a topic covered in the indexed documents."
    }
}
