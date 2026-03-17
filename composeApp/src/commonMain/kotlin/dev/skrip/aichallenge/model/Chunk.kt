package dev.skrip.aichallenge.model

import kotlinx.serialization.Serializable

@Serializable
data class ChunkMetadata(
    val source: String,
    val file: String,
    val title: String? = null,
    val section: String? = null,
    val strategy: ChunkingStrategy,
    val startOffset: Int,
    val endOffset: Int
)

@Serializable
data class Chunk(
    val id: String,
    val text: String,
    val metadata: ChunkMetadata
)
