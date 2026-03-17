package dev.skrip.aichallenge.chunking

import dev.skrip.aichallenge.model.Chunk
import dev.skrip.aichallenge.model.ChunkMetadata
import dev.skrip.aichallenge.model.ChunkingStrategy
import dev.skrip.aichallenge.model.Config
import dev.skrip.aichallenge.model.Document

class FixedSizeChunker(
    private val chunkSize: Int = Config.CHUNK_SIZE,
    private val overlap: Int = Config.CHUNK_OVERLAP
) : Chunker {

    override fun chunk(document: Document): List<Chunk> {
        val content = document.content
        if (content.isBlank()) return emptyList()

        val chunks = mutableListOf<Chunk>()
        var startOffset = 0
        var chunkIndex = 0

        while (startOffset < content.length) {
            val endOffset = minOf(startOffset + chunkSize, content.length)
            val text = content.substring(startOffset, endOffset).trim()

            if (text.isNotEmpty()) {
                chunks.add(
                    Chunk(
                        id = "${document.name}_fixed_$chunkIndex",
                        text = text,
                        metadata = ChunkMetadata(
                            source = document.path,
                            file = document.name,
                            title = extractTitle(document),
                            section = "chunk_$chunkIndex",
                            strategy = ChunkingStrategy.FIXED,
                            startOffset = startOffset,
                            endOffset = endOffset
                        )
                    )
                )
                chunkIndex++
            }

            // Move forward by (chunkSize - overlap), but at least 1 character
            val step = maxOf(chunkSize - overlap, 1)
            startOffset += step
        }

        return chunks
    }

    private fun extractTitle(document: Document): String? {
        // Try to extract title from first line for markdown
        val firstLine = document.content.lines().firstOrNull()?.trim() ?: return null
        return if (firstLine.startsWith("#")) {
            firstLine.trimStart('#').trim()
        } else {
            null
        }
    }
}
