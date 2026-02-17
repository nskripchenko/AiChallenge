package dev.skrip.aichallenge.domain.usecase

import dev.skrip.aichallenge.domain.model.*
import dev.skrip.aichallenge.domain.repository.LlmRepository
import dev.skrip.aichallenge.domain.repository.LogStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class SendPromptUseCase(
    private val repository: LlmRepository,
    private val logStore: LogStore
) {
    /**
     * Отправка запроса в панель RAW с конфигом
     */
    suspend fun sendRaw(prompt: String, config: LlmRequestConfig): LlmResult {
        return if (config.streaming) {
            // Для стриминга собираем результат
            var finalResult = LlmResult()
            sendRawStream(prompt, config).collect { result ->
                finalResult = result
            }
            finalResult
        } else {
            sendWithLogging(prompt, config, LlmPanel.RAW)
        }
    }

    /**
     * Отправка запроса в панель RAW со стримингом
     */
    fun sendRawStream(prompt: String, config: LlmRequestConfig): Flow<LlmResult> {
        val requestId = Uuid.random().toString()
        val startTime = Clock.System.now().toEpochMilliseconds()

        logRequest(requestId, prompt, config, LlmPanel.RAW)

        return repository.sendPromptStream(prompt, config)
            .onEach { result ->
                if (result.isComplete || result.error != null) {
                    logResponse(requestId, result, config, LlmPanel.RAW, startTime)
                }
            }
    }

    /**
     * Отправка запроса в панель CONTROLLED с настройками
     */
    suspend fun sendControlled(prompt: String, config: LlmRequestConfig): LlmResult {
        return sendWithLogging(prompt, config, LlmPanel.CONTROLLED)
    }

    /**
     * Отправка запроса со стримингом
     */
    fun sendControlledStream(prompt: String, config: LlmRequestConfig): Flow<LlmResult> {
        val requestId = Uuid.random().toString()
        val startTime = Clock.System.now().toEpochMilliseconds()

        logRequest(requestId, prompt, config, LlmPanel.CONTROLLED)

        return repository.sendPromptStream(prompt, config)
            .onEach { result ->
                if (result.isComplete || result.error != null) {
                    logResponse(requestId, result, config, LlmPanel.CONTROLLED, startTime)
                }
            }
    }

    private suspend fun sendWithLogging(
        prompt: String,
        config: LlmRequestConfig,
        panel: LlmPanel
    ): LlmResult {
        val requestId = Uuid.random().toString()
        val startTime = Clock.System.now().toEpochMilliseconds()

        logRequest(requestId, prompt, config, panel)

        val result = try {
            repository.sendPrompt(prompt, config)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LlmResult(error = e.message ?: "Неизвестная ошибка")
        }

        logResponse(requestId, result, config, panel, startTime)
        return result
    }

    private fun logRequest(requestId: String, prompt: String, config: LlmRequestConfig, panel: LlmPanel) {
        logStore.addLog(
            LogEntry(
                id = requestId,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                type = LogEntryType.REQUEST,
                panel = panel,
                model = config.model,
                temperature = config.temperature,
                maxTokens = config.maxTokens,
                stopSequences = config.stopSequences,
                streaming = config.streaming,
                requestJson = buildRequestPreview(prompt, config)
            )
        )
    }

    private fun logResponse(
        requestId: String,
        result: LlmResult,
        config: LlmRequestConfig,
        panel: LlmPanel,
        startTime: Long
    ) {
        val latency = Clock.System.now().toEpochMilliseconds() - startTime
        val type = if (result.error != null) LogEntryType.ERROR else LogEntryType.RESPONSE

        val responsePreview = if (result.error != null) {
            """{"error": "${result.error}"}"""
        } else {
            """{
  "model": "${result.model}",
  "input_tokens": ${result.inputTokens},
  "output_tokens": ${result.outputTokens},
  "content": "${result.text.take(500).replace("\"", "\\\"").replace("\n", "\\n")}${if (result.text.length > 500) "..." else ""}"
}"""
        }

        logStore.addLog(
            LogEntry(
                id = requestId,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                type = type,
                panel = panel,
                model = result.model.ifEmpty { config.model },
                latencyMs = latency,
                responseJson = responsePreview,
                errorDetails = result.error,
                inputTokens = result.inputTokens,
                outputTokens = result.outputTokens
            )
        )
    }

    private fun buildRequestPreview(prompt: String, config: LlmRequestConfig): String {
        val systemPart = if (config.systemPrompt.isNotBlank()) {
            """"system": [{"type": "text", "text": "${config.systemPrompt.take(100).replace("\"", "\\\"")}"}],"""
        } else ""

        val stopsPart = if (config.stopSequences.isNotEmpty()) {
            """"stop_sequences": ${config.stopSequences.map { "\"$it\"" }},"""
        } else ""

        return """{
  "model": "${config.model}",
  "max_tokens": ${config.maxTokens},
  $systemPart
  "temperature": ${config.temperature},
  ${config.topP?.let { "\"top_p\": $it," } ?: ""}
  $stopsPart
  "stream": ${config.streaming},
  "messages": [{"role": "user", "content": "${prompt.take(200).replace("\"", "\\\"").replace("\n", "\\n")}${if (prompt.length > 200) "..." else ""}"}]
}""".lines().filter { it.isNotBlank() }.joinToString("\n")
    }
}
