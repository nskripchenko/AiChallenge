package dev.skrip.aichallenge

import dev.skrip.aichallenge.index.IndexBuilder
import dev.skrip.aichallenge.model.ChunkingStrategy
import dev.skrip.aichallenge.ollama.OllamaClient
import dev.skrip.aichallenge.rag.RagService
import dev.skrip.aichallenge.search.SemanticSearch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class RagServiceTest {

    @Test
    fun testRagPipeline() = runBlocking {
        val ollamaClient = OllamaClient()

        println("=" .repeat(60))
        println("RAG PIPELINE TEST")
        println("=" .repeat(60))

        if (!ollamaClient.isAvailable()) {
            println()
            println("WARNING: Ollama is not running!")
            println("To test RAG pipeline, please:")
            println("  1. Start Ollama: ollama serve")
            println("  2. Pull models:")
            println("     ollama pull nomic-embed-text")
            println("     ollama pull llama3.2")
            println()
            println("Skipping test...")
            println("=" .repeat(60))
            return@runBlocking
        }

        println("Ollama is available!")
        println()

        // Load documents and build index with real embeddings
        val documents = DocumentLoader.loadDocuments()
        println("Loaded ${documents.size} documents")

        val builder = IndexBuilder(ollamaClient)
        println("Building index with real embeddings...")

        val index = builder.buildIndex(documents, ChunkingStrategy.STRUCTURED) { current, total ->
            print("\r  Progress: $current/$total chunks")
        }
        println()
        println("Index built: ${index.entries.size} chunks")
        println()

        // Test RAG
        val ragService = RagService(ollamaClient)

        val testQueries = listOf(
            "What is a coroutine in Kotlin?",
            "How do I create a data class?",
            "What does UserRepository do?"
        )

        testQueries.forEach { query ->
            println("-" .repeat(60))
            println("Q: $query")
            println()

            try {
                val response = ragService.ask(query, index)

                println("A: ${response.answer}")
                println()
                println("Sources:")
                response.sources.forEach { source ->
                    println("  - ${source.chunk.metadata.file} / ${source.chunk.metadata.section} (${source.similarity.format(3)})")
                }
            } catch (e: Exception) {
                println("ERROR: ${e.message}")
            }
            println()
        }

        println("=" .repeat(60))
        ragService.close()
    }

    @Test
    fun testRagWithMockEmbeddings() = runBlocking {
        val ollamaClient = OllamaClient()

        println("=" .repeat(60))
        println("RAG MOCK TEST (search only)")
        println("=" .repeat(60))

        // Build index with mock embeddings
        val documents = DocumentLoader.loadDocuments()
        val builder = IndexBuilder()
        val index = builder.buildIndexWithMockEmbeddings(documents, ChunkingStrategy.STRUCTURED)

        println("Index built: ${index.entries.size} chunks (mock embeddings)")
        println()

        // Simulate search results
        val search = SemanticSearch(topK = 3)
        val mockQueryEmbedding = index.entries.first().embedding
        val searchResults = search.search(mockQueryEmbedding, index)

        println("Mock search results:")
        searchResults.forEach { result ->
            println("  - ${result.chunk.metadata.file} / ${result.chunk.metadata.section}")
            println("    Text preview: ${result.chunk.text.take(100)}...")
            println()
        }

        if (!ollamaClient.isAvailable()) {
            println("Ollama not available - skipping LLM generation")
            println("=" .repeat(60))
            return@runBlocking
        }

        // Test chat generation with context
        val ragService = RagService(ollamaClient)
        val query = "What is this code about?"

        println("Testing LLM with context...")
        println("Q: $query")
        println()

        try {
            val response = ragService.askWithProvidedContext(query, searchResults)
            println("A: ${response.answer}")
        } catch (e: Exception) {
            println("ERROR: ${e.message}")
        }

        println("=" .repeat(60))
        ragService.close()
    }

    private fun Float.format(decimals: Int): String = "%.${decimals}f".format(this)
}
