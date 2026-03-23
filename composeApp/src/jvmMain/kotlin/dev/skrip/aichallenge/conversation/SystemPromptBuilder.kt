package dev.skrip.aichallenge.conversation

import dev.skrip.aichallenge.model.ConversationTurn
import dev.skrip.aichallenge.model.QuestionMode
import dev.skrip.aichallenge.model.TaskState

/**
 * Day 25: Динамическое построение system prompt
 *
 * Строит system prompt с учётом:
 * - Режима работы (Plain/RAG/Grounded)
 * - Task state (цель, уточнения, ограничения)
 * - Retrieved context
 * - Истории диалога
 */
class SystemPromptBuilder {

    /**
     * Построить system prompt для RAG режима с памятью
     */
    fun buildRagPrompt(
        context: String,
        taskState: TaskState?,
        isGrounded: Boolean
    ): String = buildString {
        // Base instruction
        if (isGrounded) {
            appendLine(GROUNDED_BASE)
        } else {
            appendLine(RAG_BASE)
        }

        // Task state section
        if (taskState != null && !taskState.isEmpty()) {
            appendLine()
            appendLine("=== CONVERSATION STATE ===")
            append(taskState.summary())
            appendLine()
            appendLine("IMPORTANT: Keep track of the conversation goal and build upon previous clarifications.")
        }

        // Context section
        appendLine()
        appendLine("=== RETRIEVED CONTEXT ===")
        appendLine(context)

        // Final instructions
        appendLine()
        appendLine("=== INSTRUCTIONS ===")
        if (isGrounded) {
            appendLine(GROUNDED_INSTRUCTIONS)
        } else {
            appendLine(RAG_INSTRUCTIONS)
        }

        // Task state extraction hint
        appendLine()
        appendLine(TASK_STATE_HINT)
    }

    /**
     * Построить system prompt для Plain режима с памятью
     */
    fun buildPlainPrompt(
        taskState: TaskState?
    ): String = buildString {
        appendLine(PLAIN_BASE)

        // Task state section
        if (taskState != null && !taskState.isEmpty()) {
            appendLine()
            appendLine("=== CONVERSATION STATE ===")
            append(taskState.summary())
            appendLine()
            appendLine("Continue the conversation with this context in mind.")
        }
    }

    /**
     * Форматировать историю диалога для включения в messages
     */
    fun formatHistory(
        turns: List<ConversationTurn>,
        maxTurns: Int = 10
    ): List<Pair<String, String>> {
        return turns.takeLast(maxTurns).map { turn ->
            turn.role to turn.content
        }
    }

    companion object {
        private const val RAG_BASE = """You are a helpful assistant that answers questions based on the provided context.
You are having a multi-turn conversation with the user. Remember previous exchanges and build upon them."""

        private const val GROUNDED_BASE = """You are a precise assistant that answers questions ONLY based on the provided context.
You are having a multi-turn conversation with the user. Remember previous exchanges and build upon them.

STRICT RULES:
1. Answer ONLY using information from the context below
2. If the context doesn't contain the answer, say "I cannot find this information in the provided documents"
3. Do NOT add information from your general knowledge
4. Be concise and factual
5. Quote or paraphrase directly from the context when possible"""

        private const val PLAIN_BASE = """You are a helpful assistant. Answer questions concisely and directly.
You are having a multi-turn conversation with the user. Remember previous exchanges and build upon them."""

        private const val RAG_INSTRUCTIONS = """- Use the retrieved context to answer the user's question
- If information is not in the context, you may use general knowledge but indicate this
- Reference specific parts of the context when relevant
- Be concise but thorough"""

        private const val GROUNDED_INSTRUCTIONS = """- Answer ONLY from the provided context
- If you cannot find the answer, clearly state this
- Do not hallucinate or add external information
- Quote the source when possible"""

        private const val TASK_STATE_HINT = """After answering, if the user's question reveals:
- A clear GOAL they want to achieve, note it
- Any CLARIFICATIONS or preferences, remember them
- Any CONSTRAINTS or specific requirements, track them
- Any KEY FACTS discovered from documents, retain them

This helps maintain conversation continuity."""
    }
}
