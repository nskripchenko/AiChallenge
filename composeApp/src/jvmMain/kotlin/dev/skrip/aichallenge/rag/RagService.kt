package dev.skrip.aichallenge.rag

import dev.skrip.aichallenge.model.Config
import dev.skrip.aichallenge.model.DocumentIndex
import dev.skrip.aichallenge.model.SearchResult
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
