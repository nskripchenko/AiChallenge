package dev.skrip.aichallenge.data

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

/**
 * HTTP-клиент для Anthropic Messages API
 *
 * Эндпоинт и заголовки можно изменить здесь при необходимости
 */
class AnthropicClient(private val apiKey: String) {

    companion object {
        // Если эндпоинт изменится — обновите здесь
        private const val BASE_URL = "https://api.anthropic.com/v1/messages"
        // Версия API — обновите при необходимости
        private const val ANTHROPIC_VERSION = "2023-06-01"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
        explicitNulls = false
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 120_000
        }
    }

    /**
     * Отправка запроса без стриминга
     */
    suspend fun sendMessage(request: AnthropicRequest): Result<AnthropicResponse> {
        return try {
            val response = client.post(BASE_URL) {
                contentType(ContentType.Application.Json)
                header("x-api-key", apiKey)
                header("anthropic-version", ANTHROPIC_VERSION)
                setBody(request.copy(stream = false))
            }

            val bodyText = response.bodyAsText()

            if (response.status.isSuccess()) {
                val parsed = json.decodeFromString<AnthropicResponse>(bodyText)
                if (parsed.error != null) {
                    Result.failure(AnthropicException(
                        parsed.error.message ?: "API Error",
                        response.status.value,
                        bodyText
                    ))
                } else {
                    Result.success(parsed)
                }
            } else {
                val error = try {
                    json.decodeFromString<AnthropicResponse>(bodyText)
                } catch (_: Exception) { null }

                Result.failure(AnthropicException(
                    error?.error?.message ?: "HTTP ${response.status.value}",
                    response.status.value,
                    bodyText
                ))
            }
        } catch (e: Exception) {
            Result.failure(AnthropicException(
                e.message ?: "Ошибка сети",
                0,
                e::class.simpleName ?: "Unknown"
            ))
        }
    }

    /**
     * Отправка запроса со стримингом (SSE)
     */
    fun sendMessageStream(request: AnthropicRequest): Flow<StreamResult> = flow {
        try {
            client.preparePost(BASE_URL) {
                contentType(ContentType.Application.Json)
                header("x-api-key", apiKey)
                header("anthropic-version", ANTHROPIC_VERSION)
                setBody(request.copy(stream = true))
            }.execute { response ->
                if (!response.status.isSuccess()) {
                    val bodyText = response.bodyAsText()
                    emit(StreamResult.Error(AnthropicException(
                        "HTTP ${response.status.value}",
                        response.status.value,
                        bodyText
                    )))
                    return@execute
                }

                val channel: ByteReadChannel = response.bodyAsChannel()
                val buffer = StringBuilder()

                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break

                    if (line.startsWith("data: ")) {
                        val data = line.removePrefix("data: ").trim()
                        if (data.isNotEmpty() && data != "[DONE]") {
                            try {
                                val event = json.decodeFromString<StreamEvent>(data)
                                val result = processStreamEvent(event, buffer)
                                if (result != null) {
                                    emit(result)
                                }
                            } catch (e: Exception) {
                                // Логируем ошибку парсинга но продолжаем стриминг
                                println("SSE parse error: ${e.message}, data: ${data.take(100)}")
                            }
                        }
                    }
                }

                emit(StreamResult.Complete(buffer.toString()))
            }
        } catch (e: Exception) {
            emit(StreamResult.Error(AnthropicException(
                e.message ?: "Ошибка стриминга",
                0,
                e::class.simpleName ?: "Unknown"
            )))
        }
    }

    private fun processStreamEvent(event: StreamEvent, buffer: StringBuilder): StreamResult? {
        return when (event.type) {
            "message_start" -> {
                StreamResult.Started(event.message?.model ?: "")
            }
            "content_block_delta" -> {
                val text = event.delta?.text ?: ""
                buffer.append(text)
                StreamResult.Delta(text, buffer.toString())
            }
            "message_delta" -> {
                StreamResult.UsageUpdate(
                    event.usage?.inputTokens,
                    event.usage?.outputTokens
                )
            }
            "error" -> {
                StreamResult.Error(AnthropicException(
                    event.error?.message ?: "Stream error",
                    0,
                    event.error?.type ?: ""
                ))
            }
            else -> null
        }
    }

    fun close() {
        client.close()
    }
}

sealed class StreamResult {
    data class Started(val model: String) : StreamResult()
    data class Delta(val text: String, val accumulated: String) : StreamResult()
    data class UsageUpdate(val inputTokens: Int?, val outputTokens: Int?) : StreamResult()
    data class Complete(val fullText: String) : StreamResult()
    data class Error(val exception: AnthropicException) : StreamResult()
}

class AnthropicException(
    override val message: String,
    val httpStatus: Int,
    val responseBody: String
) : Exception(message)
