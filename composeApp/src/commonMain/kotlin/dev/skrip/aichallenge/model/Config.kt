package dev.skrip.aichallenge.model

object Config {
    const val OLLAMA_BASE_URL = "http://localhost:11434"
    const val EMBEDDING_MODEL = "nomic-embed-text"
    const val CHAT_MODEL = "llama3.2"
    const val CHUNK_SIZE = 500
    const val CHUNK_OVERLAP = 50
    const val TOP_K = 3
}
