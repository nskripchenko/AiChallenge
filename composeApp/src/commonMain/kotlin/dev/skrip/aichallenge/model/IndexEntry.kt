package dev.skrip.aichallenge.model

import kotlinx.serialization.Serializable

@Serializable
data class IndexEntry(
    val chunk: Chunk,
    val embedding: List<Float>
)

@Serializable
data class DocumentIndex(
    val strategy: ChunkingStrategy,
    val entries: List<IndexEntry>,
    val documentCount: Int,
    val createdAt: Long
)
