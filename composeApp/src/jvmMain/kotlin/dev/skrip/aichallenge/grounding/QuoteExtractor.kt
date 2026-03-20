package dev.skrip.aichallenge.grounding

import dev.skrip.aichallenge.model.AnswerQuote
import dev.skrip.aichallenge.model.EnhancedSearchResult
import dev.skrip.aichallenge.model.GroundingConfig

/**
 * Извлекает цитаты из retrieved chunks.
 *
 * Цитаты извлекаются программно на основе keyword overlap,
 * а не генерируются LLM - это гарантирует точность.
 */
class QuoteExtractor(
    private val config: GroundingConfig = GroundingConfig()
) {
    /**
     * Извлекает релевантные цитаты из списка chunks.
     */
    fun extractQuotes(
        query: String,
        results: List<EnhancedSearchResult>
    ): List<AnswerQuote> {
        val queryKeywords = extractKeywords(query)
        if (queryKeywords.isEmpty()) return emptyList()

        val allQuotes = mutableListOf<AnswerQuote>()

        for (result in results) {
            val sentences = splitIntoSentences(result.chunk.text)

            for (sentence in sentences) {
                // Пропускаем слишком короткие или длинные
                if (sentence.length < config.minQuoteLength) continue
                if (sentence.length > config.maxQuoteLength) continue

                // Вычисляем релевантность
                val relevance = calculateRelevance(queryKeywords, sentence)

                if (relevance >= config.minQuoteRelevance) {
                    allQuotes.add(
                        AnswerQuote(
                            text = sentence.trim(),
                            file = result.chunk.metadata.file,
                            section = result.chunk.metadata.section,
                            relevance = relevance
                        )
                    )
                }
            }
        }

        // Сортируем по релевантности и берём top-N
        return allQuotes
            .sortedByDescending { it.relevance }
            .distinctBy { it.text } // Убираем дубликаты
            .take(config.maxQuotes)
    }

    /**
     * Извлекает одну лучшую цитату из конкретного chunk.
     */
    fun extractBestQuote(
        query: String,
        result: EnhancedSearchResult
    ): AnswerQuote? {
        val queryKeywords = extractKeywords(query)
        if (queryKeywords.isEmpty()) return null

        val sentences = splitIntoSentences(result.chunk.text)

        var bestSentence: String? = null
        var bestRelevance = 0f

        for (sentence in sentences) {
            if (sentence.length < config.minQuoteLength) continue
            if (sentence.length > config.maxQuoteLength) continue

            val relevance = calculateRelevance(queryKeywords, sentence)

            if (relevance > bestRelevance) {
                bestRelevance = relevance
                bestSentence = sentence
            }
        }

        return if (bestSentence != null && bestRelevance >= config.minQuoteRelevance) {
            AnswerQuote(
                text = bestSentence.trim(),
                file = result.chunk.metadata.file,
                section = result.chunk.metadata.section,
                relevance = bestRelevance
            )
        } else {
            null
        }
    }

    /**
     * Разбивает текст на предложения.
     */
    private fun splitIntoSentences(text: String): List<String> {
        // Разбиваем по точкам, восклицательным, вопросительным знакам
        // Также учитываем переводы строк как разделители
        return text
            .replace("\n\n", ". ")
            .replace("\n", " ")
            .split(Regex("""[.!?]+\s*"""))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    /**
     * Вычисляет релевантность предложения к запросу.
     * Использует keyword overlap.
     */
    private fun calculateRelevance(queryKeywords: Set<String>, sentence: String): Float {
        if (queryKeywords.isEmpty()) return 0f

        val sentenceWords = tokenize(sentence)

        // Считаем сколько ключевых слов найдено
        val matchedCount = queryKeywords.count { keyword ->
            sentenceWords.any { word ->
                word.contains(keyword) || keyword.contains(word)
            }
        }

        return matchedCount.toFloat() / queryKeywords.size.toFloat()
    }

    /**
     * Извлекает ключевые слова из запроса (без стоп-слов).
     */
    private fun extractKeywords(query: String): Set<String> {
        return tokenize(query)
            .filter { it !in STOP_WORDS }
            .filter { it.length > 2 }
            .toSet()
    }

    /**
     * Токенизация текста.
     */
    private fun tokenize(text: String): Set<String> {
        return text.lowercase()
            .split(Regex("\\W+"))
            .filter { it.isNotBlank() }
            .toSet()
    }

    companion object {
        private val STOP_WORDS = setOf(
            "a", "an", "the", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could",
            "should", "may", "might", "must", "shall", "can", "need", "dare",
            "to", "of", "in", "for", "on", "with", "at", "by", "from", "as",
            "into", "through", "during", "before", "after", "above", "below",
            "between", "under", "again", "further", "then", "once", "here",
            "there", "when", "where", "why", "how", "all", "each", "few",
            "more", "most", "other", "some", "such", "no", "nor", "not",
            "only", "own", "same", "so", "than", "too", "very", "just",
            "and", "but", "if", "or", "because", "until", "while", "what",
            "which", "who", "whom", "this", "that", "these", "those", "it"
        )
    }
}
