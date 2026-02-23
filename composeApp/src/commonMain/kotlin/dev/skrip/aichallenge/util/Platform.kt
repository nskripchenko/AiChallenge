package dev.skrip.aichallenge.util

expect fun generateId(): String

expect fun currentTimeMillis(): Long

expect fun getEnvVariable(name: String): String?

fun String.truncate(maxLength: Int): String {
    return if (length > maxLength) take(maxLength) + "..." else this
}

fun String.estimateTokens(): Int {
    // Rough estimation: ~4 chars per token for English, ~2-3 for other languages
    // Using 3 as a middle ground
    return (length / 3).coerceAtLeast(1)
}
