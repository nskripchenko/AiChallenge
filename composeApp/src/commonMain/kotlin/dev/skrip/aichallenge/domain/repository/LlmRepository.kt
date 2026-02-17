package dev.skrip.aichallenge.domain.repository

import dev.skrip.aichallenge.domain.model.LlmRequestConfig
import dev.skrip.aichallenge.domain.model.LlmResult
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий для работы с LLM API
 */
interface LlmRepository {
    /**
     * Отправка запроса без стриминга
     */
    suspend fun sendPrompt(
        prompt: String,
        config: LlmRequestConfig
    ): LlmResult

    /**
     * Отправка запроса со стримингом
     */
    fun sendPromptStream(
        prompt: String,
        config: LlmRequestConfig
    ): Flow<LlmResult>
}
