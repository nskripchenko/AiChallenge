package dev.skrip.aichallenge

import dev.skrip.aichallenge.index.IndexBuilder
import dev.skrip.aichallenge.index.IndexStorage
import dev.skrip.aichallenge.model.ChunkingStrategy
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IndexStorageTest {

    @Test
    fun testSaveAndLoadIndex() {
        val testDir = "build/test-index"
        val storage = IndexStorage(baseDir = testDir)
        val builder = IndexBuilder()

        // Clean up before test
        File(testDir).deleteRecursively()

        println("=" .repeat(60))
        println("INDEX STORAGE TEST")
        println("=" .repeat(60))

        // Load documents
        val documents = DocumentLoader.loadDocuments()
        println("Loaded ${documents.size} documents")

        // Build indexes with mock embeddings (no Ollama needed)
        println()
        println("Building FIXED index...")
        val fixedIndex = builder.buildIndexWithMockEmbeddings(documents, ChunkingStrategy.FIXED)
        println("  Chunks: ${fixedIndex.entries.size}")
        println("  Embedding size: ${fixedIndex.entries.first().embedding.size}")

        println()
        println("Building STRUCTURED index...")
        val structuredIndex = builder.buildIndexWithMockEmbeddings(documents, ChunkingStrategy.STRUCTURED)
        println("  Chunks: ${structuredIndex.entries.size}")

        // Save indexes
        println()
        println("Saving indexes...")
        val fixedFile = storage.saveIndex(fixedIndex)
        val structuredFile = storage.saveIndex(structuredIndex)

        println("  Fixed index saved to: ${fixedFile.absolutePath}")
        println("  Structured index saved to: ${structuredFile.absolutePath}")
        println("  Fixed file size: ${fixedFile.length()} bytes")
        println("  Structured file size: ${structuredFile.length()} bytes")

        assertTrue(fixedFile.exists(), "Fixed index file should exist")
        assertTrue(structuredFile.exists(), "Structured index file should exist")

        // Load indexes back
        println()
        println("Loading indexes back...")
        val loadedFixed = storage.loadIndex(ChunkingStrategy.FIXED)
        val loadedStructured = storage.loadIndex(ChunkingStrategy.STRUCTURED)

        assertNotNull(loadedFixed, "Should load fixed index")
        assertNotNull(loadedStructured, "Should load structured index")

        assertEquals(fixedIndex.entries.size, loadedFixed.entries.size, "Fixed chunk count should match")
        assertEquals(structuredIndex.entries.size, loadedStructured.entries.size, "Structured chunk count should match")

        // Verify embeddings preserved
        val originalEmbedding = fixedIndex.entries.first().embedding
        val loadedEmbedding = loadedFixed.entries.first().embedding
        assertEquals(originalEmbedding, loadedEmbedding, "Embeddings should be preserved")

        println("  Fixed index loaded: ${loadedFixed.entries.size} chunks")
        println("  Structured index loaded: ${loadedStructured.entries.size} chunks")

        // Test getIndexInfo
        println()
        println("Index info:")
        val fixedInfo = storage.getIndexInfo(ChunkingStrategy.FIXED)
        val structuredInfo = storage.getIndexInfo(ChunkingStrategy.STRUCTURED)

        println("  FIXED: ${fixedInfo?.chunkCount} chunks, ${fixedInfo?.embeddingSize}D embeddings")
        println("  STRUCTURED: ${structuredInfo?.chunkCount} chunks, ${structuredInfo?.embeddingSize}D embeddings")

        println()
        println("SUCCESS: Index save/load working correctly!")
        println("=" .repeat(60))

        // Clean up after test
        File(testDir).deleteRecursively()
    }

    @Test
    fun testIndexJsonFormat() {
        val testDir = "build/test-index-format"
        val storage = IndexStorage(baseDir = testDir)
        val builder = IndexBuilder()

        File(testDir).deleteRecursively()

        val documents = DocumentLoader.loadDocuments()
        val index = builder.buildIndexWithMockEmbeddings(documents, ChunkingStrategy.FIXED, embeddingSize = 8)

        storage.saveIndex(index)

        // Read raw JSON
        val jsonContent = File("$testDir/fixed_index.json").readText()

        println("=" .repeat(60))
        println("INDEX JSON FORMAT (first 1500 chars):")
        println("=" .repeat(60))
        println(jsonContent.take(1500))
        if (jsonContent.length > 1500) println("...")
        println("=" .repeat(60))

        assertTrue(jsonContent.contains("\"strategy\""))
        assertTrue(jsonContent.contains("\"entries\""))
        assertTrue(jsonContent.contains("\"embedding\""))
        assertTrue(jsonContent.contains("\"metadata\""))

        File(testDir).deleteRecursively()
    }
}
