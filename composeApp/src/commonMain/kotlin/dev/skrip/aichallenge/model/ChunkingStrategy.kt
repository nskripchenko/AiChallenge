package dev.skrip.aichallenge.model

import kotlinx.serialization.Serializable

@Serializable
enum class ChunkingStrategy {
    FIXED,
    STRUCTURED
}
