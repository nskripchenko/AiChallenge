package dev.skrip.aichallenge

import dev.skrip.aichallenge.chunking.FixedSizeChunker
import dev.skrip.aichallenge.chunking.StructuredChunker
import dev.skrip.aichallenge.model.ChunkingStrategy
import kotlin.test.Test
import kotlin.test.assertTrue

class StructuredChunkerTest {

    @Test
    fun testStructuredChunking() {
        val documents = DocumentLoader.loadDocuments()
        val chunker = StructuredChunker()

        val allChunks = chunker.chunkAll(documents)

        println("=" .repeat(60))
        println("STRUCTURED CHUNKING RESULTS")
        println("=" .repeat(60))
        println("Documents: ${documents.size}")
        println("Total chunks: ${allChunks.size}")
        println()

        val chunksByFile = allChunks.groupBy { it.metadata.file }
        chunksByFile.forEach { (file, chunks) ->
            println("[$file] -> ${chunks.size} chunks")
            chunks.forEach { chunk ->
                println("    - ${chunk.metadata.section}")
            }
        }

        println("=" .repeat(60))

        assertTrue(allChunks.isNotEmpty())
        assertTrue(allChunks.all { it.metadata.strategy == ChunkingStrategy.STRUCTURED })
    }

    @Test
    fun testCompareStrategies() {
        val documents = DocumentLoader.loadDocuments()

        val fixedChunker = FixedSizeChunker(chunkSize = 500, overlap = 50)
        val structuredChunker = StructuredChunker()

        val fixedChunks = fixedChunker.chunkAll(documents)
        val structuredChunks = structuredChunker.chunkAll(documents)

        println()
        println("=" .repeat(60))
        println("STRATEGY COMPARISON")
        println("=" .repeat(60))
        println()
        println("| File                  | Fixed | Structured |")
        println("|" + "-".repeat(23) + "|" + "-".repeat(7) + "|" + "-".repeat(12) + "|")

        val files = documents.map { it.name }
        files.forEach { file ->
            val fixedCount = fixedChunks.count { it.metadata.file == file }
            val structCount = structuredChunks.count { it.metadata.file == file }
            println("| %-21s | %5d | %10d |".format(file, fixedCount, structCount))
        }

        println("|" + "-".repeat(23) + "|" + "-".repeat(7) + "|" + "-".repeat(12) + "|")
        println("| %-21s | %5d | %10d |".format("TOTAL", fixedChunks.size, structuredChunks.size))
        println()

        println("STRUCTURED SECTIONS:")
        structuredChunks.groupBy { it.metadata.file }.forEach { (file, chunks) ->
            println("  $file:")
            chunks.forEach { println("    - ${it.metadata.section}") }
        }

        println("=" .repeat(60))

        // Both should produce chunks
        assertTrue(fixedChunks.isNotEmpty())
        assertTrue(structuredChunks.isNotEmpty())
    }
}
