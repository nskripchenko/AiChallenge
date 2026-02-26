package dev.skrip.aichallenge.data.remote

import dev.skrip.aichallenge.data.remote.dto.AnthropicErrorResponse
import dev.skrip.aichallenge.data.remote.dto.AnthropicMessage
import dev.skrip.aichallenge.data.remote.dto.AnthropicRequest
import dev.skrip.aichallenge.data.remote.dto.AnthropicResponse
import dev.skrip.aichallenge.data.remote.dto.StreamEvent
import dev.skrip.aichallenge.data.source.LlmDataSource
import dev.skrip.aichallenge.data.source.StreamingEvent
import dev.skrip.aichallenge.domain.model.Message
import dev.skrip.aichallenge.domain.model.ModelId
import dev.skrip.aichallenge.domain.model.Role
import dev.skrip.aichallenge.domain.model.TokenUsage
import dev.skrip.aichallenge.logging.AgentLogger
import dev.skrip.aichallenge.logging.LogEntry
import dev.skrip.aichallenge.util.currentTimeMillis
import dev.skrip.aichallenge.util.generateId
import dev.skrip.aichallenge.util.getEnvVariable
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AnthropicRemoteDataSource(
    private val httpClient: HttpClient,
    private val logger: AgentLogger
) : LlmDataSource {

    private val apiKey: String by lazy {
        getEnvVariable("ANTHROPIC_API_KEY")
            ?: throw IllegalStateException("ANTHROPIC_API_KEY environment variable not set")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    override suspend fun sendMessage(
        messages: List<Message>,
        model: String,
        temperature: Double,
        maxTokens: Int,
        tag: String?
    ): Result<Message> {
        return runCatching {
            val systemMessage = messages.find { it.role == Role.SYSTEM }
            val conversationMessages = messages.filter { it.role != Role.SYSTEM }

            val request = AnthropicRequest(
                model = model,
                maxTokens = maxTokens,
                temperature = temperature,
                system = systemMessage?.text,
                messages = conversationMessages.map { it.toAnthropicMessage() }
            )

            val requestJson = json.encodeToString(request)
            logRequest(model, temperature, maxTokens, requestJson, tag)

            val startTime = currentTimeMillis()

            val response = httpClient.post(ANTHROPIC_API_URL) {
                contentType(ContentType.Application.Json)
                headers {
                    append("x-api-key", apiKey)
                    append("anthropic-version", ANTHROPIC_VERSION)
                }
                setBody(request)
            }

            val responseTimeMs = currentTimeMillis() - startTime
            val responseBody = response.bodyAsText()

            if (response.status == HttpStatusCode.OK) {
                logResponse(responseBody)

                val anthropicResponse = json.decodeFromString<AnthropicResponse>(responseBody)
                val text = anthropicResponse.content
                    .filter { it.type == "text" }
                    .mapNotNull { it.text }
                    .joinToString("")

                val modelId = ModelId.fromApiId(model) ?: ModelId.SONNET_4_6

                Message(
                    id = generateId(),
                    role = Role.ASSISTANT,
                    text = text,
                    timestamp = currentTimeMillis(),
                    usage = TokenUsage(
                        inputTokens = anthropicResponse.usage.inputTokens,
                        outputTokens = anthropicResponse.usage.outputTokens,
                        responseTimeMs = responseTimeMs,
                        model = modelId
                    )
                )
            } else {
                val errorMessage = runCatching {
                    val errorResponse = json.decodeFromString<AnthropicErrorResponse>(responseBody)
                    "${errorResponse.error.type}: ${errorResponse.error.message}"
                }.getOrElse { "HTTP ${response.status.value}" }

                logError(errorMessage, responseBody)
                throw Exception(errorMessage)
            }
        }.onFailure { error ->
            if (error !is Exception || !error.message.orEmpty().startsWith("HTTP")) {
                logError(error.message ?: "Unknown error", error.stackTraceToString())
            }
        }
    }

    override fun sendMessageStreaming(
        messages: List<Message>,
        model: String,
        temperature: Double,
        maxTokens: Int,
        tag: String?
    ): Flow<StreamingEvent> = flow {
        val systemMessage = messages.find { it.role == Role.SYSTEM }
        val conversationMessages = messages.filter { it.role != Role.SYSTEM }

        val request = AnthropicRequest(
            model = model,
            maxTokens = maxTokens,
            temperature = temperature,
            system = systemMessage?.text,
            messages = conversationMessages.map { it.toAnthropicMessage() },
            stream = true
        )

        val requestJson = json.encodeToString(request)
        logRequest(model, temperature, maxTokens, requestJson, tag)

        val startTime = currentTimeMillis()
        var inputTokens = 0
        var outputTokens = 0

        try {
            httpClient.preparePost(ANTHROPIC_API_URL) {
                contentType(ContentType.Application.Json)
                headers {
                    append("x-api-key", apiKey)
                    append("anthropic-version", ANTHROPIC_VERSION)
                }
                setBody(request)
            }.execute { response ->
                if (response.status != HttpStatusCode.OK) {
                    val errorBody = response.bodyAsText()
                    val errorMessage = runCatching {
                        val errorResponse = json.decodeFromString<AnthropicErrorResponse>(errorBody)
                        "${errorResponse.error.type}: ${errorResponse.error.message}"
                    }.getOrElse { "HTTP ${response.status.value}" }
                    logError(errorMessage, errorBody)
                    emit(StreamingEvent.Error(errorMessage))
                    return@execute
                }

                val channel = response.bodyAsChannel()
                var currentData = StringBuilder()

                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break

                    when {
                        line.startsWith("data: ") -> {
                            currentData.append(line.removePrefix("data: "))
                        }
                        line.isEmpty() && currentData.isNotEmpty() -> {
                            val dataStr = currentData.toString().trim()
                            currentData = StringBuilder()

                            if (dataStr == "[DONE]") continue

                            runCatching {
                                val event = json.decodeFromString<StreamEvent>(dataStr)

                                when (event.type) {
                                    "message_start" -> {
                                        event.message?.usage?.let {
                                            inputTokens = it.inputTokens
                                        }
                                    }
                                    "content_block_delta" -> {
                                        event.delta?.text?.let { text ->
                                            emit(StreamingEvent.TextDelta(text))
                                        }
                                    }
                                    "message_delta" -> {
                                        event.usage?.let {
                                            outputTokens = it.outputTokens
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                val responseTimeMs = currentTimeMillis() - startTime
                val modelId = ModelId.fromApiId(model) ?: ModelId.SONNET_4_6

                logResponse("{\"streaming\": true, \"input_tokens\": $inputTokens, \"output_tokens\": $outputTokens}")

                emit(StreamingEvent.Complete(
                    TokenUsage(
                        inputTokens = inputTokens,
                        outputTokens = outputTokens,
                        responseTimeMs = responseTimeMs,
                        model = modelId
                    )
                ))
            }
        } catch (e: Exception) {
            logError(e.message ?: "Unknown error", e.stackTraceToString())
            emit(StreamingEvent.Error(e.message ?: "Unknown error"))
        }
    }

    private fun logRequest(model: String, temperature: Double, maxTokens: Int, requestJson: String, tag: String? = null) {
        logger.log(LogEntry.Request(
            timestamp = currentTimeMillis(),
            model = model,
            temperature = temperature,
            maxTokens = maxTokens,
            requestJson = requestJson,
            tag = tag
        ))
    }

    private fun logResponse(responseJson: String) {
        val prettyJson = runCatching {
            val parsed = Json.parseToJsonElement(responseJson)
            json.encodeToString(parsed)
        }.getOrElse { responseJson }

        logger.log(LogEntry.Response(
            timestamp = currentTimeMillis(),
            responseJson = prettyJson
        ))
    }

    private fun logError(errorMessage: String, details: String?) {
        logger.log(LogEntry.Error(
            timestamp = currentTimeMillis(),
            errorMessage = errorMessage,
            details = details
        ))
    }

    private fun Message.toAnthropicMessage(): AnthropicMessage {
        return AnthropicMessage(
            role = when (role) {
                Role.USER -> "user"
                Role.ASSISTANT -> "assistant"
                Role.SYSTEM -> "user"
            },
            content = text
        )
    }

    companion object {
        private const val ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages"
        private const val ANTHROPIC_VERSION = "2023-06-01"
    }
}
