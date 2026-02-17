package dev.skrip.aichallenge.domain.usecase

import dev.skrip.aichallenge.domain.model.LlmRequestConfig
import dev.skrip.aichallenge.domain.model.LlmResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Повторная отправка промпта несколько раз для экспериментов
 */
class RepeatPromptUseCase(
    private val sendPromptUseCase: SendPromptUseCase
) {
    /**
     * Сравнение ответов RAW vs CONTROLLED
     */
    suspend fun compareRawVsControlled(
        prompt: String,
        rawConfig: LlmRequestConfig,
        controlledConfig: LlmRequestConfig
    ): ComparisonResult = coroutineScope {
        val rawDeferred = async { sendPromptUseCase.sendRaw(prompt, rawConfig.copy(streaming = false)) }
        val controlledDeferred = async { sendPromptUseCase.sendControlled(prompt, controlledConfig.copy(streaming = false)) }

        val rawResult = rawDeferred.await()
        val controlledResult = controlledDeferred.await()

        ComparisonResult(
            rawResult = rawResult,
            controlledResult = controlledResult,
            diff = computeDiff(rawResult.text, controlledResult.text)
        )
    }

    private fun computeDiff(text1: String, text2: String): DiffResult {
        val lines1 = text1.lines()
        val lines2 = text2.lines()

        val differentLines = mutableListOf<DiffLine>()
        val maxLines = maxOf(lines1.size, lines2.size)

        for (i in 0 until maxLines) {
            val line1 = lines1.getOrNull(i)
            val line2 = lines2.getOrNull(i)

            when {
                line1 == null -> differentLines.add(DiffLine(i + 1, null, line2, DiffType.ADDED))
                line2 == null -> differentLines.add(DiffLine(i + 1, line1, null, DiffType.REMOVED))
                line1 != line2 -> differentLines.add(DiffLine(i + 1, line1, line2, DiffType.CHANGED))
            }
        }

        return DiffResult(
            isSame = differentLines.isEmpty(),
            totalLines = maxLines,
            changedLinesCount = differentLines.size,
            differentLines = differentLines
        )
    }
}

data class ComparisonResult(
    val rawResult: LlmResult,
    val controlledResult: LlmResult,
    val diff: DiffResult
)

data class DiffResult(
    val isSame: Boolean,
    val totalLines: Int,
    val changedLinesCount: Int,
    val differentLines: List<DiffLine>
)

data class DiffLine(
    val lineNumber: Int,
    val rawLine: String?,
    val controlledLine: String?,
    val type: DiffType
)

enum class DiffType {
    ADDED,
    REMOVED,
    CHANGED
}
