package dev.skrip.aichallenge.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.skrip.aichallenge.DocumentLoader
import dev.skrip.aichallenge.index.IndexBuilder
import dev.skrip.aichallenge.index.IndexStorage
import dev.skrip.aichallenge.model.*
import dev.skrip.aichallenge.ollama.OllamaClient
import dev.skrip.aichallenge.rag.RagService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val currentStrategy: ChunkingStrategy = ChunkingStrategy.STRUCTURED,
    val currentQuestionMode: QuestionMode = QuestionMode.RAG,
    val currentRetrievalMode: RetrievalMode = RetrievalMode.BASELINE,
    val indexStatus: IndexStatus? = null,
    val ollamaAvailable: Boolean = false,
    val error: String? = null
)

data class IndexStatus(
    val documentCount: Int,
    val chunkCount: Int,
    val strategy: ChunkingStrategy,
    val isIndexing: Boolean = false,
    val indexingProgress: String? = null
)

class ChatViewModel : ViewModel() {
    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private val ollamaClient = OllamaClient()
    private val indexStorage = IndexStorage()
    private val indexBuilder = IndexBuilder(ollamaClient)
    private val ragService = RagService(ollamaClient)

    private var currentIndex: DocumentIndex? = null

    init {
        checkOllamaAndLoadIndex()
    }

    private fun checkOllamaAndLoadIndex() {
        viewModelScope.launch {
            val available = ollamaClient.isAvailable()
            _state.value = _state.value.copy(ollamaAvailable = available)

            // Try to load existing index
            loadIndex(_state.value.currentStrategy)
        }
    }

    private fun loadIndex(strategy: ChunkingStrategy) {
        val index = indexStorage.loadIndex(strategy)
        currentIndex = index

        if (index != null) {
            _state.value = _state.value.copy(
                indexStatus = IndexStatus(
                    documentCount = index.documentCount,
                    chunkCount = index.entries.size,
                    strategy = index.strategy
                )
            )
        } else {
            _state.value = _state.value.copy(indexStatus = null)
        }
    }

    fun switchStrategy(strategy: ChunkingStrategy) {
        _state.value = _state.value.copy(currentStrategy = strategy)
        loadIndex(strategy)
    }

    fun switchQuestionMode(mode: QuestionMode) {
        _state.value = _state.value.copy(currentQuestionMode = mode)
    }

    fun switchRetrievalMode(mode: RetrievalMode) {
        _state.value = _state.value.copy(currentRetrievalMode = mode)
    }

    fun reindex() {
        viewModelScope.launch {
            val strategy = _state.value.currentStrategy

            _state.value = _state.value.copy(
                indexStatus = IndexStatus(
                    documentCount = 0,
                    chunkCount = 0,
                    strategy = strategy,
                    isIndexing = true,
                    indexingProgress = "Loading documents..."
                ),
                error = null
            )

            try {
                val documents = DocumentLoader.loadDocuments()

                _state.value = _state.value.copy(
                    indexStatus = _state.value.indexStatus?.copy(
                        documentCount = documents.size,
                        indexingProgress = "Building index (0/${documents.size} docs)..."
                    )
                )

                val index = indexBuilder.buildIndex(documents, strategy) { current, total ->
                    _state.value = _state.value.copy(
                        indexStatus = _state.value.indexStatus?.copy(
                            indexingProgress = "Embedding chunks ($current/$total)..."
                        )
                    )
                }

                indexStorage.saveIndex(index)
                currentIndex = index

                _state.value = _state.value.copy(
                    indexStatus = IndexStatus(
                        documentCount = index.documentCount,
                        chunkCount = index.entries.size,
                        strategy = index.strategy,
                        isIndexing = false
                    )
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Indexing failed: ${e.message}",
                    indexStatus = _state.value.indexStatus?.copy(isIndexing = false)
                )
            }
        }
    }

    fun ask(question: String) {
        if (question.isBlank()) return

        val mode = _state.value.currentQuestionMode
        val retrievalMode = _state.value.currentRetrievalMode
        val index = currentIndex

        // RAG mode requires index
        if (mode == QuestionMode.RAG && index == null) {
            _state.value = _state.value.copy(error = "No index available. Please reindex first.")
            return
        }

        // Build mode label
        val modeLabel = buildModeLabel(mode, retrievalMode)
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.USER,
            content = "$modeLabel $question"
        )

        _state.value = _state.value.copy(
            messages = _state.value.messages + userMessage,
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            try {
                val result = ragService.ask(question, mode, index, retrievalMode)

                // Convert AnswerSource to SearchResult for compatibility
                val searchResults = result.sources.map { source ->
                    SearchResult(
                        chunk = Chunk(
                            id = "",
                            text = source.textPreview,
                            metadata = ChunkMetadata(
                                source = source.file,
                                file = source.file,
                                title = null,
                                section = source.section,
                                strategy = _state.value.currentStrategy,
                                startOffset = 0,
                                endOffset = 0
                            )
                        ),
                        similarity = source.similarity
                    )
                }

                // Build response info with retrieval stats
                val responseInfo = buildResponseInfo(result, modeLabel)

                val assistantMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = MessageRole.ASSISTANT,
                    content = "${result.answer}\n\n$responseInfo",
                    sources = searchResults
                )

                _state.value = _state.value.copy(
                    messages = _state.value.messages + assistantMessage,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to get answer: ${e.message}"
                )
            }
        }
    }

    private fun buildModeLabel(mode: QuestionMode, retrievalMode: RetrievalMode): String {
        return when (mode) {
            QuestionMode.PLAIN -> "[Plain]"
            QuestionMode.RAG -> {
                val retrievalLabel = when (retrievalMode) {
                    RetrievalMode.BASELINE -> "Baseline"
                    RetrievalMode.FILTERED -> "Filtered"
                    RetrievalMode.REWRITE_FILTERED -> "Rewrite"
                }
                "[RAG:$retrievalLabel]"
            }
        }
    }

    private fun buildResponseInfo(result: AnswerResult, modeLabel: String): String {
        val stats = result.retrievalStats
        return if (stats != null) {
            buildString {
                append("$modeLabel (${result.durationMs}ms)")
                append(" | Retrieved: ${stats.rawRetrievedCount}")
                append(" → Filtered: ${stats.afterFilteringCount}")
                append(" → Used: ${stats.finalUsedCount}")
                if (stats.rewrittenQuery != null) {
                    append("\nQuery rewritten: \"${stats.rewrittenQuery}\"")
                }
            }
        } else {
            "$modeLabel (${result.durationMs}ms)"
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun clearMessages() {
        _state.value = _state.value.copy(messages = emptyList())
    }

    override fun onCleared() {
        super.onCleared()
        ollamaClient.close()
        ragService.close()
    }
}
