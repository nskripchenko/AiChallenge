package dev.skrip.aichallenge.api

interface AiClient {
    suspend fun chat(model: String, messages: List<Message>): String
}
