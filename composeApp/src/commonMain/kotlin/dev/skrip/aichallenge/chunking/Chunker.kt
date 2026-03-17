package dev.skrip.aichallenge.chunking

import dev.skrip.aichallenge.model.Chunk
import dev.skrip.aichallenge.model.Document

interface Chunker {
    fun chunk(document: Document): List<Chunk>
    fun chunkAll(documents: List<Document>): List<Chunk> = documents.flatMap { chunk(it) }
}
