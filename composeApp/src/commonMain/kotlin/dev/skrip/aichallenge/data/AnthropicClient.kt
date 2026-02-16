package dev.skrip.aichallenge.data

import dev.skrip.aichallenge.domain.ChatMessage
import dev.skrip.aichallenge.domain.Role
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class AnthropicClient(private val apiKey: String) {

    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient {
        install(ContentNegotiation) { json(json) }
    }

    suspend fun sendMessage(messages: List<ChatMessage>): Result<String> = runCatching {
        val apiMessages = messages
            .filter { it.role != Role.ERROR }
            .takeLast(10)
            .map { ApiMessage(role = it.role.apiName, content = it.content) }

        val response = client.post("https://api.anthropic.com/v1/messages") {
            contentType(ContentType.Application.Json)
            header("x-api-key", apiKey)
            header("anthropic-version", "2023-06-01")
            setBody(AnthropicRequest(model = "claude-sonnet-4-20250514", maxTokens = 1024, messages = apiMessages))
        }

        if (response.status.isSuccess()) {
            response.body<AnthropicResponse>().content
                .firstOrNull { it.type == "text" }?.text
                ?: error("Пустой ответ от API")
        } else {
            val errorMessage = runCatching {
                json.decodeFromString<AnthropicError>(response.bodyAsText()).error.message
            }.getOrElse { "HTTP ${response.status.value}" }
            error(errorMessage)
        }
    }
}

private val Role.apiName: String
    get() = when (this) {
        Role.USER -> "user"
        Role.ASSISTANT -> "assistant"
        Role.ERROR -> "user"
    }
