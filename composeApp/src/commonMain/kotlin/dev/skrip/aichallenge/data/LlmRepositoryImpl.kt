package dev.skrip.aichallenge.data

import dev.skrip.aichallenge.domain.model.LlmRequestConfig
import dev.skrip.aichallenge.domain.model.LlmResult
import dev.skrip.aichallenge.domain.repository.LlmRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Clock

class LlmRepositoryImpl(
    private val client: AnthropicClient
) : LlmRepository {

    override suspend fun sendPrompt(prompt: String, config: LlmRequestConfig): LlmResult {
        val startTime = Clock.System.now().toEpochMilliseconds()

        val request = AnthropicRequest(
            model = config.model,
            messages = listOf(AnthropicMessage(role = "user", content = prompt)),
            maxTokens = config.maxTokens,
            system = config.systemPrompt.takeIf { it.isNotBlank() }?.let {
                listOf(ContentBlock(type = "text", text = it))
            },
            temperature = config.temperature.toDouble(),
            topP = config.topP?.toDouble(),
            stopSequences = config.stopSequences.takeIf { it.isNotEmpty() },
            stream = false
        )

        val result = client.sendMessage(request)
        val latency = Clock.System.now().toEpochMilliseconds() - startTime

        return result.fold(
            onSuccess = { response ->
                val text = response.content
                    ?.filter { it.type == "text" }
                    ?.mapNotNull { it.text }
                    ?.joinToString("") ?: ""

                LlmResult(
                    text = text,
                    model = response.model ?: config.model,
                    inputTokens = response.usage?.inputTokens,
                    outputTokens = response.usage?.outputTokens,
                    latencyMs = latency,
                    isStreaming = false,
                    isComplete = true
                )
            },
            onFailure = { e ->
                val anthropicException = e as? AnthropicException
                LlmResult(
                    error = anthropicException?.message ?: e.message ?: "Неизвестная ошибка",
                    latencyMs = latency,
                    isComplete = true
                )
            }
        )
    }

    override fun sendPromptStream(prompt: String, config: LlmRequestConfig): Flow<LlmResult> = flow {
        val startTime = Clock.System.now().toEpochMilliseconds()
        var model = config.model
        var inputTokens: Int? = null
        var outputTokens: Int? = null

        val request = AnthropicRequest(
            model = config.model,
            messages = listOf(AnthropicMessage(role = "user", content = prompt)),
            maxTokens = config.maxTokens,
            system = config.systemPrompt.takeIf { it.isNotBlank() }?.let {
                listOf(ContentBlock(type = "text", text = it))
            },
            temperature = config.temperature.toDouble(),
            topP = config.topP?.toDouble(),
            stopSequences = config.stopSequences.takeIf { it.isNotEmpty() },
            stream = true
        )

        client.sendMessageStream(request).collect { streamResult ->
            val latency = Clock.System.now().toEpochMilliseconds() - startTime

            when (streamResult) {
                is StreamResult.Started -> {
                    model = streamResult.model.ifEmpty { config.model }
                    emit(LlmResult(
                        text = "",
                        model = model,
                        latencyMs = latency,
                        isStreaming = true,
                        isComplete = false
                    ))
                }
                is StreamResult.Delta -> {
                    emit(LlmResult(
                        text = streamResult.accumulated,
                        model = model,
                        inputTokens = inputTokens,
                        outputTokens = outputTokens,
                        latencyMs = latency,
                        isStreaming = true,
                        isComplete = false
                    ))
                }
                is StreamResult.UsageUpdate -> {
                    inputTokens = streamResult.inputTokens ?: inputTokens
                    outputTokens = streamResult.outputTokens ?: outputTokens
                }
                is StreamResult.Complete -> {
                    emit(LlmResult(
                        text = streamResult.fullText,
                        model = model,
                        inputTokens = inputTokens,
                        outputTokens = outputTokens,
                        latencyMs = latency,
                        isStreaming = false,
                        isComplete = true
                    ))
                }
                is StreamResult.Error -> {
                    emit(LlmResult(
                        error = streamResult.exception.message,
                        model = model,
                        latencyMs = latency,
                        isStreaming = false,
                        isComplete = true
                    ))
                }
            }
        }
    }
}
