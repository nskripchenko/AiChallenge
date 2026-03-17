package dev.skrip.aichallenge

import dev.skrip.aichallenge.ollama.OllamaClient
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class OllamaClientTest {

    @Test
    fun testOllamaConnection() = runBlocking {
        val client = OllamaClient()

        println("=" .repeat(60))
        println("OLLAMA CONNECTION TEST")
        println("=" .repeat(60))

        val isAvailable = client.isAvailable()
        println("Ollama available: $isAvailable")

        if (!isAvailable) {
            println()
            println("WARNING: Ollama is not running!")
            println("Please start Ollama with: ollama serve")
            println("And pull the embedding model: ollama pull nomic-embed-text")
            println("=" .repeat(60))
            return@runBlocking
        }

        println("Testing embedding generation...")
        println()

        try {
            val testText = "Kotlin is a modern programming language"
            val embedding = client.getEmbedding(testText)

            println("Input text: \"$testText\"")
            println("Embedding size: ${embedding.size}")
            println("First 5 values: ${embedding.take(5)}")
            println("Last 5 values: ${embedding.takeLast(5)}")

            assertTrue(embedding.isNotEmpty(), "Embedding should not be empty")
            println()
            println("SUCCESS: Embedding generated!")

        } catch (e: Exception) {
            println("ERROR: ${e.message}")
            println()
            println("Make sure you have pulled the embedding model:")
            println("  ollama pull nomic-embed-text")
        }

        println("=" .repeat(60))
        client.close()
    }

    @Test
    fun testMultipleEmbeddings() = runBlocking {
        val client = OllamaClient()

        if (!client.isAvailable()) {
            println("Skipping: Ollama not available")
            return@runBlocking
        }

        println("=" .repeat(60))
        println("MULTIPLE EMBEDDINGS TEST")
        println("=" .repeat(60))

        val texts = listOf(
            "Kotlin coroutines for async programming",
            "Java virtual machine and bytecode",
            "Python is dynamically typed"
        )

        try {
            val embeddings = client.getEmbeddings(texts)

            println("Generated ${embeddings.size} embeddings:")
            embeddings.forEachIndexed { index, emb ->
                println("  [$index] size=${emb.size}, first=${emb.firstOrNull()}")
            }

            assertTrue(embeddings.size == texts.size)
            assertTrue(embeddings.all { it.isNotEmpty() })

            println()
            println("SUCCESS!")

        } catch (e: Exception) {
            println("ERROR: ${e.message}")
        }

        println("=" .repeat(60))
        client.close()
    }
}
