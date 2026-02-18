package dev.skrip.aichallenge.model

data class ModeResult(
    val mode: ReasoningMode,
    val prompt: String,
    val response: String,
    val durationMs: Long,
    val error: String? = null
) {
    val isSuccess: Boolean get() = error == null
}
