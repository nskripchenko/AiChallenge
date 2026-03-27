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
    /** Включен ли grounded режим (Day 24) */
    val groundedMode: Boolean = true,
    /** Day 25: Включен ли режим памяти */
    val memoryEnabled: Boolean = true,
    /** Day 25: Task state (цель, уточнения, ограничения) */
    val taskState: TaskState = TaskState(),
    /** Day 25: ID текущего диалога */
    val conversationId: String = UUID.randomUUID().toString(),
    val indexStatus: IndexStatus? = null,
    val ollamaAvailable: Boolean = false,
    val error: String? = null,
    /** Day 29: LLM settings */
    val llmSettings: LLMSettings = LLMSettings(),
    /** Day 29: Settings panel expanded */
    val settingsExpanded: Boolean = false
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

    // Day 25: Conversation memory
    private var conversationMemory = ConversationMemory(id = _state.value.conversationId)

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

    fun toggleGroundedMode(enabled: Boolean) {
        _state.value = _state.value.copy(groundedMode = enabled)
    }

    /** Day 25: Включить/выключить режим памяти */
    fun toggleMemoryMode(enabled: Boolean) {
        _state.value = _state.value.copy(memoryEnabled = enabled)
    }

    /** Day 29: Toggle settings panel */
    fun toggleSettings() {
        _state.value = _state.value.copy(settingsExpanded = !_state.value.settingsExpanded)
    }

    /** Day 29: Update LLM settings */
    fun updateLLMSettings(settings: LLMSettings) {
        _state.value = _state.value.copy(llmSettings = settings)
    }

    /** Day 29: Apply preset */
    fun applyPreset(preset: LLMPreset) {
        val newSettings = _state.value.llmSettings.copy(
            temperature = preset.temperature,
            maxTokens = preset.maxTokens,
            preset = preset
        )
        _state.value = _state.value.copy(llmSettings = newSettings)
    }

    /** Day 29: Update temperature */
    fun updateTemperature(temp: Float) {
        _state.value = _state.value.copy(
            llmSettings = _state.value.llmSettings.copy(temperature = temp)
        )
    }

    /** Day 29: Update max tokens */
    fun updateMaxTokens(tokens: Int) {
        _state.value = _state.value.copy(
            llmSettings = _state.value.llmSettings.copy(maxTokens = tokens)
        )
    }

    /** Day 25: Начать новый диалог (сбросить память) */
    fun startNewConversation() {
        val newId = UUID.randomUUID().toString()
        conversationMemory = ConversationMemory(id = newId)
        _state.value = _state.value.copy(
            messages = emptyList(),
            taskState = TaskState(),
            conversationId = newId
        )
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
        val groundedMode = _state.value.groundedMode
        val memoryEnabled = _state.value.memoryEnabled
        val index = currentIndex

        // RAG mode requires index
        if (mode == QuestionMode.RAG && index == null) {
            _state.value = _state.value.copy(error = "No index available. Please reindex first.")
            return
        }

        // Build mode label
        val modeLabel = buildModeLabel(mode, retrievalMode, groundedMode, memoryEnabled)
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
                if (memoryEnabled) {
                    // Day 25: Memory-enabled mode
                    askWithMemory(question, mode, index, retrievalMode, groundedMode, modeLabel)
                } else if (mode == QuestionMode.RAG && groundedMode && index != null) {
                    // Grounded RAG mode (no memory)
                    askGrounded(question, index, retrievalMode, modeLabel)
                } else {
                    // Regular mode (no memory)
                    askRegular(question, mode, index, retrievalMode, modeLabel)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to get answer: ${e.message}"
                )
            }
        }
    }

    private suspend fun askGrounded(
        question: String,
        index: DocumentIndex,
        retrievalMode: RetrievalMode,
        modeLabel: String
    ) {
        val settings = _state.value.llmSettings
        val result = ragService.askGrounded(question, index, retrievalMode, settings)

        // Convert sources to SearchResult for compatibility
        val searchResults = result.sources.map { source ->
            SearchResult(
                chunk = Chunk(
                    id = source.chunkId,
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

        // Build response info
        val responseInfo = buildGroundedResponseInfo(result, modeLabel)

        val assistantMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.ASSISTANT,
            content = "${result.answer}\n\n$responseInfo",
            sources = searchResults,
            quotes = result.quotes,
            isFallback = result.isFallback,
            averageRelevance = result.averageRelevance
        )

        _state.value = _state.value.copy(
            messages = _state.value.messages + assistantMessage,
            isLoading = false
        )
    }

    private suspend fun askRegular(
        question: String,
        mode: QuestionMode,
        index: DocumentIndex?,
        retrievalMode: RetrievalMode,
        modeLabel: String
    ) {
        val settings = _state.value.llmSettings
        val result = ragService.ask(question, mode, index, retrievalMode, settings)

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
    }

    /** Day 25: Memory-enabled answer */
    private suspend fun askWithMemory(
        question: String,
        mode: QuestionMode,
        index: DocumentIndex?,
        retrievalMode: RetrievalMode,
        groundedMode: Boolean,
        modeLabel: String
    ) {
        // Update memory with user message before calling LLM
        conversationMemory = conversationMemory.addUserMessage(question)

        val settings = _state.value.llmSettings
        val result = if (mode == QuestionMode.RAG && index != null) {
            ragService.askWithMemory(question, index, conversationMemory, retrievalMode, groundedMode, settings)
        } else {
            ragService.askPlainWithMemory(question, conversationMemory, settings)
        }

        // Update memory with assistant response
        conversationMemory = conversationMemory
            .addAssistantMessage(result.answer)
            .updateTaskState(result.updatedTaskState)

        // Update task state in UI
        _state.value = _state.value.copy(taskState = result.updatedTaskState)

        // Convert sources to SearchResult for compatibility
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

        // Build response info
        val responseInfo = buildMemoryResponseInfo(result, modeLabel)

        val assistantMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.ASSISTANT,
            content = "${result.answer}\n\n$responseInfo",
            sources = searchResults,
            quotes = result.quotes,
            isFallback = result.isFallback,
            averageRelevance = result.averageRelevance
        )

        _state.value = _state.value.copy(
            messages = _state.value.messages + assistantMessage,
            isLoading = false
        )
    }

    private fun buildMemoryResponseInfo(result: MemoryAnswerResult, modeLabel: String): String {
        val stats = result.retrievalStats
        return buildString {
            append("$modeLabel (${result.durationMs}ms)")
            if (result.isFallback) {
                append(" | FALLBACK (low relevance: ${formatPercent(result.averageRelevance ?: 0f)})")
            } else if (result.averageRelevance != null) {
                append(" | Relevance: ${formatPercent(result.averageRelevance)}")
            }
            if (stats != null) {
                append(" | Retrieved: ${stats.rawRetrievedCount}")
                append(" → Used: ${stats.finalUsedCount}")
                if (stats.rewrittenQuery != null) {
                    append("\nQuery rewritten: \"${stats.rewrittenQuery}\"")
                }
            }
            if (result.quotes.isNotEmpty()) {
                append(" | Quotes: ${result.quotes.size}")
            }
            // Show memory info
            append(" | Memory: ${conversationMemory.messageCount} msgs")
        }
    }

    private fun buildModeLabel(
        mode: QuestionMode,
        retrievalMode: RetrievalMode,
        groundedMode: Boolean,
        memoryEnabled: Boolean = false
    ): String {
        val memoryTag = if (memoryEnabled) "+Memory" else ""
        return when (mode) {
            QuestionMode.PLAIN -> "[Plain$memoryTag]"
            QuestionMode.RAG -> {
                val retrievalLabel = when (retrievalMode) {
                    RetrievalMode.BASELINE -> "Baseline"
                    RetrievalMode.FILTERED -> "Filtered"
                    RetrievalMode.REWRITE_FILTERED -> "Rewrite"
                }
                val groundedTag = if (groundedMode) "+Grounded" else ""
                "[RAG:$retrievalLabel$groundedTag$memoryTag]"
            }
        }
    }

    private fun buildGroundedResponseInfo(result: GroundedAnswer, modeLabel: String): String {
        val stats = result.retrievalStats
        return buildString {
            append("$modeLabel (${result.durationMs}ms)")
            if (result.isFallback) {
                append(" | FALLBACK (low relevance: ${formatPercent(result.averageRelevance)})")
            } else {
                append(" | Relevance: ${formatPercent(result.averageRelevance)}")
            }
            if (stats != null) {
                append(" | Retrieved: ${stats.rawRetrievedCount}")
                append(" → Used: ${stats.finalUsedCount}")
                if (stats.rewrittenQuery != null) {
                    append("\nQuery rewritten: \"${stats.rewrittenQuery}\"")
                }
            }
            if (result.quotes.isNotEmpty()) {
                append(" | Quotes: ${result.quotes.size}")
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

    private fun formatPercent(value: Float): String {
        return "%.0f%%".format(value * 100)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun clearMessages() {
        // Day 25: Also reset conversation memory
        startNewConversation()
    }

    override fun onCleared() {
        super.onCleared()
        ollamaClient.close()
        ragService.close()
    }
}
