package dev.skrip.aichallenge.rag

import dev.skrip.aichallenge.model.*
import dev.skrip.aichallenge.ollama.OllamaClient
import dev.skrip.aichallenge.search.SemanticSearch

data class RagResponse(
    val answer: String,
    val sources: List<SearchResult>,
    val query: String
)

class RagService(
    private val ollamaClient: OllamaClient = OllamaClient(),
    private val search: SemanticSearch = SemanticSearch(),
    private val topK: Int = Config.TOP_K
) {
    /**
     * Ответ в режиме PLAIN - прямой запрос к LLM без retrieval
     */
    suspend fun askPlain(query: String): AnswerResult {
        val startTime = System.currentTimeMillis()

        val answer = ollamaClient.chatPlain(query)

        return AnswerResult(
            query = query,
            mode = QuestionMode.PLAIN,
            answer = answer,
            sources = emptyList(),
            durationMs = System.currentTimeMillis() - startTime
        )
    }

    /**
     * Ответ в режиме RAG - поиск + LLM
     */
    suspend fun askRag(query: String, index: DocumentIndex): AnswerResult {
        val startTime = System.currentTimeMillis()

        // 1. Get embedding for query
        val queryEmbedding = ollamaClient.getEmbedding(query)

        // 2. Search for relevant chunks
        val searchResults = search.search(queryEmbedding, index, topK)

        // 3. Build context from chunks
        val context = buildContext(searchResults)

        // 4. Generate answer
        val answer = ollamaClient.chat(query, context)

        // Convert to AnswerSource
        val sources = searchResults.map { result ->
            AnswerSource(
                file = result.chunk.metadata.file,
                section = result.chunk.metadata.section,
                similarity = result.similarity,
                textPreview = result.chunk.text.take(150) + "..."
            )
        }

        return AnswerResult(
            query = query,
            mode = QuestionMode.RAG,
            answer = answer,
            sources = sources,
            durationMs = System.currentTimeMillis() - startTime
        )
    }

    /**
     * Универсальный метод для обоих режимов
     */
    suspend fun ask(query: String, mode: QuestionMode, index: DocumentIndex?): AnswerResult {
        return when (mode) {
            QuestionMode.PLAIN -> askPlain(query)
            QuestionMode.RAG -> {
                requireNotNull(index) { "Index required for RAG mode" }
                askRag(query, index)
            }
        }
    }

    // Legacy method for compatibility
    suspend fun ask(
        query: String,
        index: DocumentIndex
    ): RagResponse {
        // 1. Get embedding for query
        val queryEmbedding = ollamaClient.getEmbedding(query)

        // 2. Search for relevant chunks
        val searchResults = search.search(queryEmbedding, index, topK)

        // 3. Build context from chunks
        val context = buildContext(searchResults)

        // 4. Generate answer
        val answer = ollamaClient.chat(query, context)

        return RagResponse(
            answer = answer,
            sources = searchResults,
            query = query
        )
    }

    private fun buildContext(results: List<SearchResult>): String {
        if (results.isEmpty()) return "No relevant information found."

        return buildString {
            appendLine("Relevant information from documents:")
            appendLine()

            results.forEachIndexed { index, result ->
                appendLine("--- Source ${index + 1}: ${result.chunk.metadata.file} (${result.chunk.metadata.section ?: "unknown section"}) ---")
                appendLine(result.chunk.text)
                appendLine()
            }
        }
    }

    /**
     * Ask with mock search results (for testing without Ollama embeddings)
     */
    suspend fun askWithProvidedContext(
        query: String,
        searchResults: List<SearchResult>
    ): RagResponse {
        val context = buildContext(searchResults)
        val answer = ollamaClient.chat(query, context)

        return RagResponse(
            answer = answer,
            sources = searchResults,
            query = query
        )
    }

    fun close() {
        ollamaClient.close()
    }
}
