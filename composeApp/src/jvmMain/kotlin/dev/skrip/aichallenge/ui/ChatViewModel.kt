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

        val index = currentIndex
        if (index == null) {
            _state.value = _state.value.copy(error = "No index available. Please reindex first.")
            return
        }

        // Add user message
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.USER,
            content = question
        )

        _state.value = _state.value.copy(
            messages = _state.value.messages + userMessage,
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            try {
                val response = ragService.ask(question, index)

                val assistantMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = MessageRole.ASSISTANT,
                    content = response.answer,
                    sources = response.sources
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
