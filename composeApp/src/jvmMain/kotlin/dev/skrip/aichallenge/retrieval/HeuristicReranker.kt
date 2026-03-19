package dev.skrip.aichallenge.retrieval

import dev.skrip.aichallenge.model.EnhancedSearchResult
import dev.skrip.aichallenge.model.RetrievalConfig
import dev.skrip.aichallenge.model.SearchResult

/**
 * Heuristic reranking: комбинация semantic similarity + keyword overlap.
 *
 * combinedScore = α * semanticScore + β * keywordScore
 *
 * Где:
 * - semanticScore: исходный cosine similarity из embedding search
 * - keywordScore: доля ключевых слов запроса, найденных в chunk
 */
class HeuristicReranker(
    private val config: RetrievalConfig = RetrievalConfig()
) {
    /**
     * Rerank результаты с учётом keyword overlap.
     * Возвращает отсортированный список по combinedScore (desc).
     */
    fun rerank(query: String, results: List<SearchResult>): List<EnhancedSearchResult> {
        val queryKeywords = extractKeywords(query)

        return results.map { result ->
            val semanticScore = result.similarity
            val keywordScore = calculateKeywordOverlap(queryKeywords, result)
            val combinedScore = config.semanticWeight * semanticScore +
                    config.keywordWeight * keywordScore

            EnhancedSearchResult(
                chunk = result.chunk,
                semanticScore = semanticScore,
                keywordScore = keywordScore,
                combinedScore = combinedScore
            )
        }.sortedByDescending { it.combinedScore }
    }

    /**
     * Вычисляет keyword overlap: какая доля ключевых слов запроса
     * найдена в тексте chunk + metadata.
     *
     * @return Score от 0.0 до 1.0
     */
    private fun calculateKeywordOverlap(
        queryKeywords: Set<String>,
        result: SearchResult
    ): Float {
        if (queryKeywords.isEmpty()) return 0f

        // Собираем все слова из chunk: text + metadata
        val chunkWords = buildSet {
            addAll(tokenize(result.chunk.text))
            result.chunk.metadata.section?.let { addAll(tokenize(it)) }
            result.chunk.metadata.title?.let { addAll(tokenize(it)) }
            addAll(tokenize(result.chunk.metadata.file))
        }

        // Считаем сколько ключевых слов найдено
        val matchedKeywords = queryKeywords.count { keyword ->
            chunkWords.any { word ->
                word.contains(keyword) || keyword.contains(word)
            }
        }

        return matchedKeywords.toFloat() / queryKeywords.size.toFloat()
    }

    /**
     * Извлекает ключевые слова из запроса.
     * Убирает стоп-слова и короткие токены.
     */
    private fun extractKeywords(query: String): Set<String> {
        return tokenize(query)
            .filter { it !in STOP_WORDS }
            .filter { it.length > 2 }
            .toSet()
    }

    /**
     * Простая токенизация: lowercase, split по non-word characters.
     */
    private fun tokenize(text: String): Set<String> {
        return text.lowercase()
            .split(Regex("\\W+"))
            .filter { it.isNotBlank() }
            .toSet()
    }

    companion object {
        /**
         * Базовые английские стоп-слова для фильтрации.
         */
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
