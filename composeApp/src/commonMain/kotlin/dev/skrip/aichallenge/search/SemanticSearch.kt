package dev.skrip.aichallenge.search

import dev.skrip.aichallenge.model.Config
import dev.skrip.aichallenge.model.DocumentIndex
import dev.skrip.aichallenge.model.SearchResult
import kotlin.math.sqrt

class SemanticSearch(
    private val topK: Int = Config.TOP_K
) {
    fun search(
        queryEmbedding: List<Float>,
        index: DocumentIndex,
        limit: Int = topK
    ): List<SearchResult> {
        return index.entries
            .map { entry ->
                val similarity = cosineSimilarity(queryEmbedding, entry.embedding)
                SearchResult(chunk = entry.chunk, similarity = similarity)
            }
            .sortedByDescending { it.similarity }
            .take(limit)
    }

    companion object {
        fun cosineSimilarity(a: List<Float>, b: List<Float>): Float {
            require(a.size == b.size) { "Vectors must have same dimension" }

            var dotProduct = 0f
            var normA = 0f
            var normB = 0f

            for (i in a.indices) {
                dotProduct += a[i] * b[i]
                normA += a[i] * a[i]
                normB += b[i] * b[i]
            }

            val denominator = sqrt(normA) * sqrt(normB)
            return if (denominator == 0f) 0f else dotProduct / denominator
        }
    }
}
