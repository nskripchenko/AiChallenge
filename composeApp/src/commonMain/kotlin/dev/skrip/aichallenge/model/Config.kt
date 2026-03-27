package dev.skrip.aichallenge.model

object Config {
    val OLLAMA_BASE_URL: String = System.getenv("OLLAMA_HOST")?.let { "http://$it" }
        ?: "http://localhost:11434"
    const val EMBEDDING_MODEL = "nomic-embed-text"
    const val CHAT_MODEL = "llama3.2"
    const val CHUNK_SIZE = 500
    const val CHUNK_OVERLAP = 50
    const val TOP_K = 3

    // Day 29: LLM Optimization Parameters
    object LLM {
        /** Temperature: 0.0 = deterministic, 1.0 = creative. Lower for RAG (factual answers) */
        const val TEMPERATURE = 0.3f

        /** Max tokens to generate. Limits response length */
        const val MAX_TOKENS = 512

        /** Context window size. More = better memory, slower */
        const val CONTEXT_SIZE = 4096

        /** Top-p sampling. Lower = more focused */
        const val TOP_P = 0.9f

        /** Repeat penalty. Prevents repetition */
        const val REPEAT_PENALTY = 1.1f
    }
}
