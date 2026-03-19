package dev.skrip.aichallenge.rag

import dev.skrip.aichallenge.model.*
import dev.skrip.aichallenge.ollama.OllamaClient
import dev.skrip.aichallenge.retrieval.ImprovedRetrieval
import dev.skrip.aichallenge.search.SemanticSearch

data class RagResponse(
    val answer: String,
    val sources: List<SearchResult>,
    val query: String
)

class RagService(
    private val ollamaClient: OllamaClient = OllamaClient(),
    private val search: SemanticSearch = SemanticSearch(),
    private val config: RetrievalConfig = RetrievalConfig()
) {
    private val improvedRetrieval = ImprovedRetrieval(ollamaClient, search, config)

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
     * Ответ в режиме RAG с improved retrieval pipeline
     */
    suspend fun askRag(
        query: String,
        index: DocumentIndex,
        retrievalMode: RetrievalMode = RetrievalMode.BASELINE
    ): AnswerResult {
        val startTime = System.currentTimeMillis()

        // 1. Improved retrieval
        val retrievalResult = improvedRetrieval.retrieve(query, index, retrievalMode)

        // 2. Build context from retrieved chunks
        val context = buildContextFromEnhanced(retrievalResult.results)

        // 3. Generate answer
        val answer = ollamaClient.chat(query, context)

        // 4. Convert to AnswerSource
        val sources = retrievalResult.results.map { result ->
            AnswerSource(
                file = result.chunk.metadata.file,
                section = result.chunk.metadata.section,
                similarity = result.combinedScore,
                textPreview = result.chunk.text.take(150) + "..."
            )
        }

        return AnswerResult(
            query = query,
            mode = QuestionMode.RAG,
            answer = answer,
            sources = sources,
            durationMs = System.currentTimeMillis() - startTime,
            retrievalMode = retrievalMode,
            retrievalStats = retrievalResult.stats
        )
    }

    /**
     * Универсальный метод для всех режимов
     */
    suspend fun ask(
        query: String,
        mode: QuestionMode,
        index: DocumentIndex?,
        retrievalMode: RetrievalMode = RetrievalMode.BASELINE
    ): AnswerResult {
        return when (mode) {
            QuestionMode.PLAIN -> askPlain(query)
            QuestionMode.RAG -> {
                requireNotNull(index) { "Index required for RAG mode" }
                askRag(query, index, retrievalMode)
            }
        }
    }

    // Legacy method for compatibility (без retrievalMode)
    suspend fun ask(query: String, mode: QuestionMode, index: DocumentIndex?): AnswerResult {
        return ask(query, mode, index, RetrievalMode.BASELINE)
    }

    private fun buildContextFromEnhanced(results: List<EnhancedSearchResult>): String {
        if (results.isEmpty()) return "No relevant information found."

        return buildString {
            appendLine("Relevant information from documents:")
            appendLine()

            results.forEachIndexed { index, result ->
                val section = result.chunk.metadata.section ?: "unknown section"
                appendLine("--- Source ${index + 1}: ${result.chunk.metadata.file} ($section) ---")
                appendLine(result.chunk.text)
                appendLine()
            }
        }
    }

    // Legacy methods for backwards compatibility
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

    suspend fun ask(
        query: String,
        index: DocumentIndex
    ): RagResponse {
        val queryEmbedding = ollamaClient.getEmbedding(query)
        val searchResults = search.search(queryEmbedding, index, config.topKAfterFiltering)
        val context = buildContext(searchResults)
        val answer = ollamaClient.chat(query, context)

        return RagResponse(
            answer = answer,
            sources = searchResults,
            query = query
        )
    }

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
