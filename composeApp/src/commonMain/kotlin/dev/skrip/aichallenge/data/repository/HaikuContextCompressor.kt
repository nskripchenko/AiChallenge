package dev.skrip.aichallenge.data.repository

import dev.skrip.aichallenge.data.source.LlmDataSource
import dev.skrip.aichallenge.domain.model.ConversationState
import dev.skrip.aichallenge.domain.model.Message
import dev.skrip.aichallenge.domain.model.ModelId
import dev.skrip.aichallenge.domain.model.Role
import dev.skrip.aichallenge.domain.repository.ContextCompressor
import dev.skrip.aichallenge.util.generateId
import dev.skrip.aichallenge.util.currentTimeMillis

class HaikuContextCompressor(
    private val llmDataSource: LlmDataSource
) : ContextCompressor {

    override suspend fun compressIfNeeded(
        state: ConversationState,
        keepRecent: Int
    ): ConversationState {
        if (!state.needsCompression(keepRecent)) {
            return state
        }

        val messagesToSummarize = state.getMessagesToSummarize(keepRecent)
        if (messagesToSummarize.isEmpty()) {
            return state
        }

        val newSummary = summarize(state.summary, messagesToSummarize)
        val newSummarizedCount = state.messages.size - keepRecent

        return state.copy(
            summary = newSummary,
            summarizedCount = newSummarizedCount
        )
    }

    override fun buildContextMessages(
        state: ConversationState,
        keepRecent: Int
    ): List<Message> {
        val recentMessages = state.getRecentMessages(keepRecent)

        return if (state.summary != null && state.summarizedCount > 0) {
            val summaryMessage = Message(
                id = "summary",
                role = Role.USER,
                text = "[Previous conversation summary: ${state.summary}]",
                timestamp = 0
            )
            listOf(summaryMessage) + recentMessages
        } else {
            recentMessages
        }
    }

    private suspend fun summarize(
        existingSummary: String?,
        messages: List<Message>
    ): String {
        val prompt = buildSummarizationPrompt(existingSummary, messages)

        val summaryRequest = listOf(
            Message(
                id = generateId(),
                role = Role.USER,
                text = prompt,
                timestamp = currentTimeMillis()
            )
        )

        val result = llmDataSource.sendMessage(
            messages = summaryRequest,
            model = ModelId.HAIKU_4_5.apiId,
            temperature = 0.3,
            maxTokens = 500,
            tag = "SUMMARIZE"
        )

        return result.map { it.text }.getOrElse {
            // Fallback: simple concatenation if summarization fails
            existingSummary?.let { "$it\n\n" }.orEmpty() +
            messages.joinToString("\n") { "${it.role}: ${it.text.take(100)}..." }
        }
    }

    private fun buildSummarizationPrompt(
        existingSummary: String?,
        messages: List<Message>
    ): String {
        val conversationText = messages.joinToString("\n\n") { msg ->
            val role = if (msg.role == Role.USER) "User" else "Assistant"
            "$role: ${msg.text}"
        }

        return buildString {
            appendLine("Summarize this conversation concisely. Capture:")
            appendLine("- Key topics and questions discussed")
            appendLine("- Important decisions or conclusions")
            appendLine("- Technical details or code mentioned (briefly)")
            appendLine("- User preferences or requirements")
            appendLine()

            if (existingSummary != null) {
                appendLine("Previous conversation summary:")
                appendLine(existingSummary)
                appendLine()
                appendLine("New messages to incorporate:")
            } else {
                appendLine("Conversation to summarize:")
            }
            appendLine()
            appendLine(conversationText)
            appendLine()
            appendLine("Write a concise updated summary (2-3 short paragraphs):")
        }
    }
}
