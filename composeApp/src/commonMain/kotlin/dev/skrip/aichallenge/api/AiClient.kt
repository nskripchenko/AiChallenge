package dev.skrip.aichallenge.api

import dev.skrip.aichallenge.model.AiCompletionResult
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Клиент для работы с Anthropic Messages API
 */
class AiClient(private val apiKey: String) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    /**
     * Отправляет запрос к модели и получает ответ
     */
    suspend fun complete(
        modelId: String,
        prompt: String,
        temperature: Double = 0.2,
        maxTokens: Int = 1024
    ): AiCompletionResult {
        val request = MessagesRequest(
            model = modelId,
            maxTokens = maxTokens,
            messages = listOf(
                Message(role = "user", content = prompt)
            ),
            temperature = temperature
        )

        val httpResponse = client.post("https://api.anthropic.com/v1/messages") {
            contentType(ContentType.Application.Json)
            header("x-api-key", apiKey)
            header("anthropic-version", "2023-06-01")
            setBody(request)
        }

        val responseText = httpResponse.bodyAsText()

        // Проверяем на ошибку API
        if (!httpResponse.status.isSuccess()) {
            val errorResponse = try {
                json.decodeFromString<ErrorResponse>(responseText)
            } catch (e: Exception) {
                null
            }
            val errorMsg = errorResponse?.error?.message ?: responseText
            throw Exception("API ошибка (${httpResponse.status}): $errorMsg")
        }

        val response = json.decodeFromString<MessagesResponse>(responseText)
        val text = response.content.firstOrNull()?.text ?: ""

        return AiCompletionResult(
            text = text,
            inputTokens = response.usage?.inputTokens,
            outputTokens = response.usage?.outputTokens
        )
    }

    fun close() {
        client.close()
    }
}

@Serializable
private data class MessagesRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val messages: List<Message>,
    val temperature: Double = 0.2
)

@Serializable
private data class Message(
    val role: String,
    val content: String
)

@Serializable
private data class MessagesResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ContentBlock>,
    val model: String,
    @SerialName("stop_reason") val stopReason: String? = null,
    val usage: Usage? = null
)

@Serializable
private data class ContentBlock(
    val type: String,
    val text: String
)

@Serializable
private data class Usage(
    @SerialName("input_tokens") val inputTokens: Int,
    @SerialName("output_tokens") val outputTokens: Int
)

@Serializable
private data class ErrorResponse(
    val type: String,
    val error: ErrorDetail
)

@Serializable
private data class ErrorDetail(
    val type: String,
    val message: String
)
