package dev.skrip.aichallenge.retrieval

import dev.skrip.aichallenge.ollama.OllamaClient

/**
 * Переписывает user question в оптимизированный retrieval query.
 *
 * Идея: пользователь спрашивает "How does the app handle errors?",
 * а для поиска лучше использовать "error handling exception catch try".
 *
 * Rewritten query используется только для semantic search,
 * оригинальный вопрос идёт в финальный LLM prompt.
 */
class QueryRewriter(
    private val ollamaClient: OllamaClient
) {
    companion object {
        private val REWRITE_PROMPT = """
            |You are a search query optimizer. Your task is to rewrite the user's question
            |into a short, keyword-focused search query that will work better for semantic search.
            |
            |Rules:
            |1. Extract key concepts and terms
            |2. Remove filler words (how, what, does, the, etc.)
            |3. Add synonyms or related technical terms if relevant
            |4. Keep it SHORT (3-8 words max)
            |5. Output ONLY the rewritten query, nothing else
            |
            |Examples:
            |Q: "What is a coroutine in Kotlin?"
            |A: coroutine kotlin lightweight thread async
            |
            |Q: "How does the UserRepository fetch a user?"
            |A: UserRepository fetch user getUser database cache
            |
            |Q: "What are the main dispatchers in Kotlin coroutines?"
            |A: kotlin coroutine dispatchers Main IO Default
            |
            |Now rewrite this question:
        """.trimMargin()
    }

    /**
     * Переписывает вопрос в retrieval query.
     * При ошибке возвращает оригинальный вопрос.
     */
    suspend fun rewrite(question: String): RewriteResult {
        return try {
            val prompt = "$REWRITE_PROMPT\nQ: \"$question\"\nA:"
            val rewritten = ollamaClient.chatPlain(prompt)
                .trim()
                .lines()
                .first() // Берём только первую строку
                .trim()

            // Sanity check: если результат пустой или слишком длинный, используем оригинал
            if (rewritten.isBlank() || rewritten.length > 200) {
                RewriteResult(
                    original = question,
                    rewritten = question,
                    wasRewritten = false
                )
            } else {
                RewriteResult(
                    original = question,
                    rewritten = rewritten,
                    wasRewritten = true
                )
            }
        } catch (e: Exception) {
            // При любой ошибке возвращаем оригинал
            RewriteResult(
                original = question,
                rewritten = question,
                wasRewritten = false,
                error = e.message
            )
        }
    }
}

/**
 * Результат переписывания запроса
 */
data class RewriteResult(
    val original: String,
    val rewritten: String,
    val wasRewritten: Boolean,
    val error: String? = null
)
