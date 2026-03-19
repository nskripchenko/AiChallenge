package dev.skrip.aichallenge.retrieval

import dev.skrip.aichallenge.model.*
import dev.skrip.aichallenge.ollama.OllamaClient
import dev.skrip.aichallenge.search.SemanticSearch

/**
 * Improved Retrieval Pipeline.
 *
 * Объединяет все этапы:
 * 1. Query rewrite (опционально)
 * 2. Semantic search (top-K before filtering)
 * 3. Filtering (threshold, length, dedup)
 * 4. Reranking (semantic + keyword)
 * 5. Final top-K selection
 */
class ImprovedRetrieval(
    private val ollamaClient: OllamaClient,
    private val semanticSearch: SemanticSearch = SemanticSearch(),
    private val config: RetrievalConfig = RetrievalConfig()
) {
    private val queryRewriter = QueryRewriter(ollamaClient)
    private val chunkFilter = ChunkFilter(config)
    private val reranker = HeuristicReranker(config)

    /**
     * Выполняет retrieval в зависимости от режима.
     */
    suspend fun retrieve(
        query: String,
        index: DocumentIndex,
        mode: RetrievalMode
    ): ImprovedRetrievalResult {
        return when (mode) {
            RetrievalMode.BASELINE -> retrieveBaseline(query, index)
            RetrievalMode.FILTERED -> retrieveFiltered(query, index, rewriteQuery = false)
            RetrievalMode.REWRITE_FILTERED -> retrieveFiltered(query, index, rewriteQuery = true)
        }
    }

    /**
     * Baseline retrieval: semantic search → top-K → done.
     * Без фильтрации и reranking.
     */
    private suspend fun retrieveBaseline(
        query: String,
        index: DocumentIndex
    ): ImprovedRetrievalResult {
        // 1. Get embedding
        val queryEmbedding = ollamaClient.getEmbedding(query)

        // 2. Semantic search with final top-K
        val results = semanticSearch.search(
            queryEmbedding,
            index,
            limit = config.topKAfterFiltering
        )

        // 3. Convert to EnhancedSearchResult (без keyword scoring)
        val enhanced = results.map { result ->
            EnhancedSearchResult(
                chunk = result.chunk,
                semanticScore = result.similarity,
                keywordScore = 0f,
                combinedScore = result.similarity
            )
        }

        return ImprovedRetrievalResult(
            results = enhanced,
            stats = RetrievalStats(
                originalQuery = query,
                rewrittenQuery = null,
                rawRetrievedCount = results.size,
                afterFilteringCount = results.size,
                finalUsedCount = results.size
            )
        )
    }

    /**
     * Improved retrieval: rewrite (опционально) → search → filter → rerank → top-K.
     */
    private suspend fun retrieveFiltered(
        query: String,
        index: DocumentIndex,
        rewriteQuery: Boolean
    ): ImprovedRetrievalResult {
        // 1. Query rewrite (если включено)
        val rewriteResult = if (rewriteQuery) {
            queryRewriter.rewrite(query)
        } else {
            null
        }
        val searchQuery = rewriteResult?.rewritten ?: query

        // 2. Get embedding for search query
        val queryEmbedding = ollamaClient.getEmbedding(searchQuery)

        // 3. Semantic search with larger top-K (before filtering)
        val rawResults = semanticSearch.search(
            queryEmbedding,
            index,
            limit = config.topKBeforeFiltering
        )

        // 4. Filter
        val filterResult = chunkFilter.filter(rawResults)

        // 5. Rerank (используем оригинальный query для keyword matching)
        val reranked = reranker.rerank(query, filterResult.results)

        // 6. Take final top-K
        val finalResults = reranked.take(config.topKAfterFiltering)

        return ImprovedRetrievalResult(
            results = finalResults,
            stats = RetrievalStats(
                originalQuery = query,
                rewrittenQuery = if (rewriteQuery) rewriteResult?.rewritten else null,
                rawRetrievedCount = rawResults.size,
                afterFilteringCount = filterResult.results.size,
                finalUsedCount = finalResults.size,
                filteredByThreshold = filterResult.stats.filteredByThreshold,
                filteredByLength = filterResult.stats.filteredByLength,
                filteredAsDuplicates = filterResult.stats.filteredAsDuplicates
            )
        )
    }
}
