package dev.skrip.aichallenge

import dev.skrip.aichallenge.chunking.FixedSizeChunker
import dev.skrip.aichallenge.model.ChunkingStrategy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FixedSizeChunkerTest {

    @Test
    fun testFixedSizeChunking() {
        val documents = DocumentLoader.loadDocuments()
        val chunker = FixedSizeChunker(chunkSize = 500, overlap = 50)

        val allChunks = chunker.chunkAll(documents)

        println("=" .repeat(60))
        println("FIXED-SIZE CHUNKING RESULTS")
        println("=" .repeat(60))
        println("Documents: ${documents.size}")
        println("Total chunks: ${allChunks.size}")
        println()

        // Group by file
        val chunksByFile = allChunks.groupBy { it.metadata.file }
        chunksByFile.forEach { (file, chunks) ->
            println("[$file] -> ${chunks.size} chunks")
        }

        println()
        println("-" .repeat(60))
        println("SAMPLE CHUNKS (first 3):")
        println("-" .repeat(60))

        allChunks.take(3).forEachIndexed { index, chunk ->
            println()
            println("Chunk #$index: ${chunk.id}")
            println("  File: ${chunk.metadata.file}")
            println("  Title: ${chunk.metadata.title ?: "N/A"}")
            println("  Section: ${chunk.metadata.section}")
            println("  Strategy: ${chunk.metadata.strategy}")
            println("  Offset: ${chunk.metadata.startOffset}-${chunk.metadata.endOffset}")
            println("  Text (first 100 chars): ${chunk.text.take(100)}...")
        }

        println()
        println("=" .repeat(60))

        // Assertions
        assertTrue(allChunks.isNotEmpty(), "Should produce chunks")
        assertTrue(allChunks.all { it.metadata.strategy == ChunkingStrategy.FIXED })
        assertTrue(allChunks.all { it.text.length <= 500 })
    }

    @Test
    fun testOverlapWorks() {
        val documents = DocumentLoader.loadDocuments()
        val chunkerWithOverlap = FixedSizeChunker(chunkSize = 200, overlap = 50)

        val chunks = chunkerWithOverlap.chunkAll(documents)

        // Check that consecutive chunks from same file have overlapping content
        val kotlinChunks = chunks.filter { it.metadata.file == "UserRepository.kt" }

        if (kotlinChunks.size >= 2) {
            val first = kotlinChunks[0]
            val second = kotlinChunks[1]

            // With overlap, second chunk should start before first chunk ends
            val expectedOverlapStart = first.metadata.startOffset + (200 - 50) // 150
            assertEquals(expectedOverlapStart, second.metadata.startOffset,
                "Second chunk should start with overlap")

            println("Overlap test: First chunk ends at ${first.metadata.endOffset}, " +
                    "Second chunk starts at ${second.metadata.startOffset}")
        }
    }
}
