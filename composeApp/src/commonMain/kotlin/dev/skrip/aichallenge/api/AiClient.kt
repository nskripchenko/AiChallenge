package dev.skrip.aichallenge.api

import dev.skrip.aichallenge.model.AnthropicModel
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

interface AiClient {
    suspend fun complete(
        model: AnthropicModel,
        prompt: String,
        temperature: Double
    ): String
}

class AnthropicAiClient(
    private val apiKey: String,
    private val httpClient: HttpClient
) : AiClient {

    companion object {
        private const val BASE_URL = "https://api.anthropic.com/v1/messages"
        private const val API_VERSION = "2023-06-01"
        private const val MAX_TOKENS = 1024
    }

    override suspend fun complete(
        model: AnthropicModel,
        prompt: String,
        temperature: Double
    ): String {
        val request = AnthropicRequest(
            model = model.modelId,
            messages = listOf(Message(role = "user", content = prompt)),
            maxTokens = MAX_TOKENS,
            temperature = temperature
        )

        val response: HttpResponse = httpClient.post(BASE_URL) {
            contentType(ContentType.Application.Json)
            header("x-api-key", apiKey)
            header("anthropic-version", API_VERSION)
            setBody(request)
        }

        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            try {
                val error = Json.decodeFromString<AnthropicError>(errorBody)
                throw Exception("API Error: ${error.error.message}")
            } catch (e: Exception) {
                if (e.message?.startsWith("API Error:") == true) throw e
                throw Exception("HTTP ${response.status.value}: $errorBody")
            }
        }

        val anthropicResponse: AnthropicResponse = response.body()
        return anthropicResponse.content
            .filter { it.type == "text" }
            .mapNotNull { it.text }
            .joinToString("\n")
    }
}

expect fun createHttpClient(): HttpClient
