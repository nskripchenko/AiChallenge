package dev.skrip.aichallenge.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class Role {
    USER,
    ASSISTANT,
    SYSTEM
}
