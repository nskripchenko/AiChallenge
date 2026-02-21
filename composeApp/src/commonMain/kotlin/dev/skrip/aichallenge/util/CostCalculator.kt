package dev.skrip.aichallenge.util

import dev.skrip.aichallenge.model.ModelProfile

/**
 * Утилита для расчёта стоимости запросов
 */
object CostCalculator {

    /**
     * Рассчитывает примерную стоимость запроса
     * @param profile профиль модели с ценами
     * @param inputTokens количество входных токенов
     * @param outputTokens количество выходных токенов
     * @return стоимость в долларах
     */
    fun estimateCost(profile: ModelProfile, inputTokens: Int, outputTokens: Int): Double {
        return (inputTokens * profile.inputPricePer1M / 1_000_000.0) +
                (outputTokens * profile.outputPricePer1M / 1_000_000.0)
    }

    /**
     * Псевдо-расчёт токенов на основе длины текста
     * Используется, если API не вернул реальное количество токенов
     */
    fun estimateTokens(text: String): Int {
        return (text.length / 4).coerceAtLeast(1)
    }
}
