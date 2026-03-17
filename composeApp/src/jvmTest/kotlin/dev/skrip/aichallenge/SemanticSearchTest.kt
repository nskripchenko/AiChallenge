package dev.skrip.aichallenge

import dev.skrip.aichallenge.index.IndexBuilder
import dev.skrip.aichallenge.model.ChunkingStrategy
import dev.skrip.aichallenge.search.SemanticSearch
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SemanticSearchTest {

    @Test
    fun testCosineSimilarity() {
        println("=" .repeat(60))
        println("COSINE SIMILARITY TEST")
        println("=" .repeat(60))

        // Identical vectors should have similarity 1.0
        val v1 = listOf(1f, 2f, 3f)
        val sim1 = SemanticSearch.cosineSimilarity(v1, v1)
        println("Identical vectors: $sim1")
        assertTrue(abs(sim1 - 1.0f) < 0.001f, "Identical vectors should have similarity ~1.0")

        // Orthogonal vectors should have similarity 0.0
        val v2 = listOf(1f, 0f, 0f)
        val v3 = listOf(0f, 1f, 0f)
        val sim2 = SemanticSearch.cosineSimilarity(v2, v3)
        println("Orthogonal vectors: $sim2")
        assertTrue(abs(sim2) < 0.001f, "Orthogonal vectors should have similarity ~0.0")

        // Opposite vectors should have similarity -1.0
        val v4 = listOf(1f, 2f, 3f)
        val v5 = listOf(-1f, -2f, -3f)
        val sim3 = SemanticSearch.cosineSimilarity(v4, v5)
        println("Opposite vectors: $sim3")
        assertTrue(abs(sim3 + 1.0f) < 0.001f, "Opposite vectors should have similarity ~-1.0")

        // Similar vectors should have high similarity
        val v6 = listOf(1f, 2f, 3f)
        val v7 = listOf(1.1f, 2.1f, 3.1f)
        val sim4 = SemanticSearch.cosineSimilarity(v6, v7)
        println("Similar vectors: $sim4")
        assertTrue(sim4 > 0.99f, "Similar vectors should have high similarity")

        println("=" .repeat(60))
    }

    @Test
    fun testSemanticSearch() {
        val documents = DocumentLoader.loadDocuments()
        val builder = IndexBuilder()

        // Build index with mock embeddings
        val index = builder.buildIndexWithMockEmbeddings(documents, ChunkingStrategy.STRUCTURED)

        val search = SemanticSearch(topK = 3)

        println("=" .repeat(60))
        println("SEMANTIC SEARCH TEST")
        println("=" .repeat(60))
        println("Index has ${index.entries.size} chunks")
        println()

        // Use the embedding of one of the chunks as query
        // This should return that chunk as the best match
        val targetChunk = index.entries.find { it.chunk.metadata.section == "class:UserRepository" }!!
        val queryEmbedding = targetChunk.embedding

        println("Query: Using embedding of '${targetChunk.chunk.metadata.section}'")
        println()

        val results = search.search(queryEmbedding, index)

        println("Top-${results.size} results:")
        results.forEachIndexed { i, result ->
            println("  ${i + 1}. [${result.similarity.format(4)}] ${result.chunk.metadata.file} - ${result.chunk.metadata.section}")
        }

        // First result should be the chunk we used as query
        assertEquals(targetChunk.chunk.id, results.first().chunk.id, "Best match should be the query chunk itself")
        assertTrue(abs(results.first().similarity - 1.0f) < 0.001f, "Self-similarity should be ~1.0")

        println()
        println("SUCCESS: Search returns correct results!")
        println("=" .repeat(60))
    }

    @Test
    fun testSearchWithDifferentStrategies() {
        val documents = DocumentLoader.loadDocuments()
        val builder = IndexBuilder()

        val fixedIndex = builder.buildIndexWithMockEmbeddings(documents, ChunkingStrategy.FIXED)
        val structuredIndex = builder.buildIndexWithMockEmbeddings(documents, ChunkingStrategy.STRUCTURED)

        val search = SemanticSearch(topK = 5)

        println("=" .repeat(60))
        println("SEARCH COMPARISON: FIXED vs STRUCTURED")
        println("=" .repeat(60))

        // Use embedding from a coroutines chunk
        val queryChunk = fixedIndex.entries.find { it.chunk.text.contains("coroutine", ignoreCase = true) }!!
        val queryEmbedding = queryChunk.embedding

        println("Query chunk: ${queryChunk.chunk.metadata.file} (${queryChunk.chunk.metadata.section})")
        println()

        val fixedResults = search.search(queryEmbedding, fixedIndex)
        val structuredResults = search.search(queryEmbedding, structuredIndex)

        println("FIXED strategy results:")
        fixedResults.forEach { result ->
            println("  [${result.similarity.format(3)}] ${result.chunk.metadata.file} - ${result.chunk.metadata.section}")
        }

        println()
        println("STRUCTURED strategy results:")
        structuredResults.forEach { result ->
            println("  [${result.similarity.format(3)}] ${result.chunk.metadata.file} - ${result.chunk.metadata.section}")
        }

        println("=" .repeat(60))

        assertTrue(fixedResults.isNotEmpty())
        assertTrue(structuredResults.isNotEmpty())
    }

    private fun Float.format(decimals: Int): String = "%.${decimals}f".format(this)
}
