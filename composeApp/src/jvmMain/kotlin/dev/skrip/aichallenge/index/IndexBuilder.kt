package dev.skrip.aichallenge.index

import dev.skrip.aichallenge.chunking.Chunker
import dev.skrip.aichallenge.chunking.FixedSizeChunker
import dev.skrip.aichallenge.chunking.StructuredChunker
import dev.skrip.aichallenge.model.ChunkingStrategy
import dev.skrip.aichallenge.model.Document
import dev.skrip.aichallenge.model.DocumentIndex
import dev.skrip.aichallenge.model.IndexEntry
import dev.skrip.aichallenge.ollama.OllamaClient

class IndexBuilder(
    private val ollamaClient: OllamaClient = OllamaClient()
) {
    private val fixedChunker: Chunker = FixedSizeChunker()
    private val structuredChunker: Chunker = StructuredChunker()

    private fun getChunker(strategy: ChunkingStrategy): Chunker {
        return when (strategy) {
            ChunkingStrategy.FIXED -> fixedChunker
            ChunkingStrategy.STRUCTURED -> structuredChunker
        }
    }

    suspend fun buildIndex(
        documents: List<Document>,
        strategy: ChunkingStrategy,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): DocumentIndex {
        val chunker = getChunker(strategy)
        val chunks = chunker.chunkAll(documents)

        val entries = mutableListOf<IndexEntry>()

        chunks.forEachIndexed { index, chunk ->
            onProgress?.invoke(index + 1, chunks.size)

            val embedding = ollamaClient.getEmbedding(chunk.text)
            entries.add(IndexEntry(chunk = chunk, embedding = embedding))
        }

        return DocumentIndex(
            strategy = strategy,
            entries = entries,
            documentCount = documents.size,
            createdAt = System.currentTimeMillis()
        )
    }

    /**
     * Build index with mock embeddings (for testing without Ollama)
     */
    fun buildIndexWithMockEmbeddings(
        documents: List<Document>,
        strategy: ChunkingStrategy,
        embeddingSize: Int = 768
    ): DocumentIndex {
        val chunker = getChunker(strategy)
        val chunks = chunker.chunkAll(documents)

        val entries = chunks.map { chunk ->
            // Generate deterministic mock embedding based on chunk text hash
            val hash = chunk.text.hashCode()
            val mockEmbedding = List(embeddingSize) { i ->
                ((hash + i) % 1000) / 1000f
            }
            IndexEntry(chunk = chunk, embedding = mockEmbedding)
        }

        return DocumentIndex(
            strategy = strategy,
            entries = entries,
            documentCount = documents.size,
            createdAt = System.currentTimeMillis()
        )
    }
}
