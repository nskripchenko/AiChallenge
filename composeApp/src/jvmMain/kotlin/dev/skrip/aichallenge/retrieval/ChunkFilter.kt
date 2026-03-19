package dev.skrip.aichallenge.retrieval

import dev.skrip.aichallenge.model.RetrievalConfig
import dev.skrip.aichallenge.model.SearchResult

/**
 * Фильтрация retrieved chunks:
 * 1. По similarity threshold
 * 2. По минимальной длине текста
 * 3. Дедупликация похожих chunks
 */
class ChunkFilter(
    private val config: RetrievalConfig = RetrievalConfig()
) {
    /**
     * Применяет все фильтры к списку результатов.
     * Возвращает отфильтрованный список + статистику.
     */
    fun filter(results: List<SearchResult>): FilterResult {
        var filtered = results
        var filteredByThreshold = 0
        var filteredByLength = 0
        var filteredAsDuplicates = 0

        // 1. Filter by similarity threshold
        val afterThreshold = filtered.filter { it.similarity >= config.similarityThreshold }
        filteredByThreshold = filtered.size - afterThreshold.size
        filtered = afterThreshold

        // 2. Filter by minimum text length
        val afterLength = filtered.filter { it.chunk.text.length >= config.minChunkLength }
        filteredByLength = filtered.size - afterLength.size
        filtered = afterLength

        // 3. Deduplicate near-duplicates
        val afterDedup = deduplicate(filtered)
        filteredAsDuplicates = filtered.size - afterDedup.size
        filtered = afterDedup

        return FilterResult(
            results = filtered,
            stats = FilterStats(
                originalCount = results.size,
                afterFilteringCount = filtered.size,
                filteredByThreshold = filteredByThreshold,
                filteredByLength = filteredByLength,
                filteredAsDuplicates = filteredAsDuplicates
            )
        )
    }

    /**
     * Удаляет near-duplicate chunks на основе Jaccard similarity.
     * Сохраняет chunk с наивысшим similarity score.
     */
    private fun deduplicate(results: List<SearchResult>): List<SearchResult> {
        if (results.size <= 1) return results

        val kept = mutableListOf<SearchResult>()

        for (result in results) {
            val isDuplicate = kept.any { existing ->
                jaccardSimilarity(
                    tokenize(existing.chunk.text),
                    tokenize(result.chunk.text)
                ) >= config.deduplicationThreshold
            }

            if (!isDuplicate) {
                kept.add(result)
            }
        }

        return kept
    }

    /**
     * Jaccard similarity между двумя множествами токенов.
     * J(A,B) = |A ∩ B| / |A ∪ B|
     */
    private fun jaccardSimilarity(set1: Set<String>, set2: Set<String>): Float {
        if (set1.isEmpty() && set2.isEmpty()) return 1f
        if (set1.isEmpty() || set2.isEmpty()) return 0f

        val intersection = set1.intersect(set2).size
        val union = set1.union(set2).size

        return intersection.toFloat() / union.toFloat()
    }

    /**
     * Простая токенизация: lowercase, split по non-word characters.
     */
    private fun tokenize(text: String): Set<String> {
        return text.lowercase()
            .split(Regex("\\W+"))
            .filter { it.length > 2 } // Игнорируем очень короткие токены
            .toSet()
    }
}

/**
 * Результат фильтрации
 */
data class FilterResult(
    val results: List<SearchResult>,
    val stats: FilterStats
)

/**
 * Статистика фильтрации
 */
data class FilterStats(
    val originalCount: Int,
    val afterFilteringCount: Int,
    val filteredByThreshold: Int,
    val filteredByLength: Int,
    val filteredAsDuplicates: Int
)
