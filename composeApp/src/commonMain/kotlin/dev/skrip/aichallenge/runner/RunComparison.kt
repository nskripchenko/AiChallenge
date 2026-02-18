package dev.skrip.aichallenge.runner

import dev.skrip.aichallenge.api.AiClient
import dev.skrip.aichallenge.api.Message
import dev.skrip.aichallenge.model.AnthropicModel
import dev.skrip.aichallenge.model.ModeResult
import dev.skrip.aichallenge.model.ReasoningMode
import dev.skrip.aichallenge.prompts.PromptFactory
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

class RunComparison(private val client: AiClient) {

    suspend fun run(task: String, model: AnthropicModel): Map<ReasoningMode, ModeResult> {
        return supervisorScope {
            val results = mutableMapOf<ReasoningMode, ModeResult>()

            // Run DIRECT, STEP_BY_STEP, EXPERTS in parallel
            val directDeferred = async { runMode(ReasoningMode.DIRECT, task, model) }
            val stepByStepDeferred = async { runMode(ReasoningMode.STEP_BY_STEP, task, model) }
            val expertsDeferred = async { runMode(ReasoningMode.EXPERTS, task, model) }

            // PROMPT_FIRST runs sequentially (2 API calls)
            val promptFirstDeferred = async { runPromptFirst(task, model) }

            results[ReasoningMode.DIRECT] = directDeferred.await()
            results[ReasoningMode.STEP_BY_STEP] = stepByStepDeferred.await()
            results[ReasoningMode.EXPERTS] = expertsDeferred.await()
            results[ReasoningMode.PROMPT_FIRST] = promptFirstDeferred.await()

            results
        }
    }

    private suspend fun runMode(
        mode: ReasoningMode,
        task: String,
        model: AnthropicModel
    ): ModeResult {
        val prompt = when (mode) {
            ReasoningMode.DIRECT -> PromptFactory.buildDirectPrompt(task)
            ReasoningMode.STEP_BY_STEP -> PromptFactory.buildStepByStepPrompt(task)
            ReasoningMode.EXPERTS -> PromptFactory.buildExpertsPrompt(task)
            ReasoningMode.PROMPT_FIRST -> throw IllegalArgumentException("Use runPromptFirst for PROMPT_FIRST mode")
        }

        return executeWithTiming(mode, prompt, model)
    }

    private suspend fun runPromptFirst(task: String, model: AnthropicModel): ModeResult {
        val mode = ReasoningMode.PROMPT_FIRST
        var totalDuration = 0L
        var generatedPrompt = ""
        var finalResponse = ""
        var error: String? = null

        try {
            // Step 1: Generate optimal prompt
            val promptGenerationRequest = PromptFactory.buildPromptGenerationRequest(task)
            val promptGenMessages = listOf(Message(role = "user", content = promptGenerationRequest))

            val promptGenDuration = measureTimeMillis {
                generatedPrompt = client.chat(model.modelId, promptGenMessages)
            }
            totalDuration += promptGenDuration

            // Step 2: Use generated prompt to solve the task
            val solutionMessages = listOf(Message(role = "user", content = generatedPrompt))
            val solutionDuration = measureTimeMillis {
                finalResponse = client.chat(model.modelId, solutionMessages)
            }
            totalDuration += solutionDuration

        } catch (e: Exception) {
            error = e.message ?: "Unknown error"
        }

        val fullPrompt = """
            |[Шаг 1: Запрос на генерацию промпта]
            |${PromptFactory.buildPromptGenerationRequest(task)}
            |
            |[Шаг 2: Сгенерированный промпт]
            |$generatedPrompt
        """.trimMargin()

        return ModeResult(
            mode = mode,
            prompt = fullPrompt,
            response = finalResponse,
            durationMs = totalDuration,
            error = error
        )
    }

    private suspend fun executeWithTiming(
        mode: ReasoningMode,
        prompt: String,
        model: AnthropicModel
    ): ModeResult {
        var response = ""
        var error: String? = null
        val duration = measureTimeMillis {
            try {
                val messages = listOf(Message(role = "user", content = prompt))
                response = client.chat(model.modelId, messages)
            } catch (e: Exception) {
                error = e.message ?: "Unknown error"
            }
        }

        return ModeResult(
            mode = mode,
            prompt = prompt,
            response = response,
            durationMs = duration,
            error = error
        )
    }
}
