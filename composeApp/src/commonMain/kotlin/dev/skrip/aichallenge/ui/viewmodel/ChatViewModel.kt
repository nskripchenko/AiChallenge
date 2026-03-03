package dev.skrip.aichallenge.ui.viewmodel

import dev.skrip.aichallenge.data.source.StreamingEvent
import dev.skrip.aichallenge.domain.model.AgentConfig
import dev.skrip.aichallenge.domain.model.Message
import dev.skrip.aichallenge.domain.model.ModelId
import dev.skrip.aichallenge.domain.model.Role
import dev.skrip.aichallenge.domain.model.TokenUsage
import dev.skrip.aichallenge.domain.repository.ChatAgent
import dev.skrip.aichallenge.domain.repository.ChatHistoryStorage
import dev.skrip.aichallenge.domain.repository.MemoryManager
import dev.skrip.aichallenge.logging.AgentLogger
import dev.skrip.aichallenge.logging.LogEntry
import dev.skrip.aichallenge.ui.state.ChatUiState
import dev.skrip.aichallenge.ui.state.ChatViewEvent
import dev.skrip.aichallenge.util.currentTimeMillis
import dev.skrip.aichallenge.util.estimateTokens
import dev.skrip.aichallenge.util.generateId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatAgent: ChatAgent,
    agentLogger: AgentLogger,
    private val historyStorage: ChatHistoryStorage,
    private val memoryManager: MemoryManager
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val logs: StateFlow<List<LogEntry>> = agentLogger.logs

    private var streamingJob: Job? = null

    init {
        initializeMemory()
        loadHistory()
        observeMemoryState()
    }

    private fun initializeMemory() {
        viewModelScope.launch {
            memoryManager.initialize()
        }
    }

    private fun observeMemoryState() {
        viewModelScope.launch {
            memoryManager.memoryState.collect { memoryState ->
                updateState { copy(memoryState = memoryState) }
            }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            val state = historyStorage.loadState()
            if (state.messages.isNotEmpty()) {
                updateState { copy(messages = state.messages) }
                memoryManager.updateShortTermMemory(state.messages)
            }
        }
    }

    private fun saveHistory() {
        viewModelScope.launch {
            val messages = _uiState.value.messages
            historyStorage.saveState(
                dev.skrip.aichallenge.domain.model.ConversationState(messages = messages)
            )
        }
    }

    fun onEvent(event: ChatViewEvent) {
        when (event) {
            // Chat events
            is ChatViewEvent.InputChanged -> updateState { copy(inputText = event.text) }
            is ChatViewEvent.SendClicked -> handleSendClicked()
            is ChatViewEvent.StopGeneration -> stopGeneration()
            is ChatViewEvent.ClearHistory -> clearHistory()

            // Settings events
            is ChatViewEvent.SystemPromptChanged -> updateState { copy(systemPromptText = event.text) }
            is ChatViewEvent.ModelChanged -> updateState { copy(selectedModel = event.model) }
            is ChatViewEvent.TemperatureChanged -> updateState { copy(temperatureText = event.text) }
            is ChatViewEvent.MaxTokensChanged -> updateState { copy(maxTokensText = event.text) }
            is ChatViewEvent.HistoryTokenLimitChanged -> updateState { copy(historyTokenLimitText = event.text) }

            // Memory events
            is ChatViewEvent.SelectMemoryLayer -> updateState { copy(selectedMemoryLayer = event.layer) }
            is ChatViewEvent.ToggleMemoryPanel -> updateState { copy(isMemoryPanelExpanded = !isMemoryPanelExpanded) }

            // Working memory
            is ChatViewEvent.AddToWorkingMemory -> viewModelScope.launch {
                memoryManager.addToWorkingMemory(event.label, event.content)
            }
            is ChatViewEvent.RemoveFromWorkingMemory -> viewModelScope.launch {
                memoryManager.removeFromWorkingMemory(event.itemId)
            }
            is ChatViewEvent.ClearWorkingMemory -> viewModelScope.launch {
                memoryManager.clearWorkingMemory()
            }

            // Long-term memory
            is ChatViewEvent.UpdateProfile -> viewModelScope.launch {
                memoryManager.updateProfile(event.profile)
            }
            is ChatViewEvent.AddDecision -> viewModelScope.launch {
                memoryManager.addDecision(event.title, event.description)
            }
            is ChatViewEvent.RemoveDecision -> viewModelScope.launch {
                memoryManager.removeDecision(event.decisionId)
            }
            is ChatViewEvent.AddKnowledge -> viewModelScope.launch {
                memoryManager.addKnowledge(event.category, event.title, event.content)
            }
            is ChatViewEvent.RemoveKnowledge -> viewModelScope.launch {
                memoryManager.removeKnowledge(event.knowledgeId)
            }
            is ChatViewEvent.ClearLongTermMemory -> viewModelScope.launch {
                memoryManager.clearLongTermMemory()
            }

            // Short-term memory settings
            is ChatViewEvent.SetShortTermLimit -> memoryManager.setShortTermLimit(event.maxMessages)
        }
    }

    private inline fun updateState(crossinline transform: ChatUiState.() -> ChatUiState) {
        _uiState.update { it.transform() }
    }

    private fun handleSendClicked() {
        val currentState = _uiState.value
        val userMessageText = currentState.inputText.trim()

        if (userMessageText.isBlank() || currentState.isLoading || currentState.isStreaming) return

        val assistantMessageId = generateId()
        val config = buildAgentConfig(currentState)

        // Get recent messages based on short-term memory limit
        val shortTermLimit = currentState.memoryState.shortTerm.maxMessages
        val contextMessages = currentState.messages.takeLast(shortTermLimit)

        // Trim to token limit
        val trimmedHistory = trimHistoryToTokenLimit(contextMessages, config.historyTokenLimit)

        // Calculate estimated input tokens
        val estimatedInputTokens = calculateRequestTokens(
            userMessage = userMessageText,
            history = trimmedHistory,
            systemPrompt = config.systemPrompt
        )

        val userMessage = Message(
            id = generateId(),
            role = Role.USER,
            text = userMessageText,
            timestamp = currentTimeMillis(),
            usage = TokenUsage(
                inputTokens = estimatedInputTokens,
                outputTokens = 0,
                responseTimeMs = 0,
                model = config.model
            )
        )

        // Update state with user message
        updateState {
            copy(
                messages = messages + userMessage,
                inputText = "",
                isLoading = true,
                isStreaming = true,
                streamingText = "",
                errorMessage = null
            )
        }

        streamingJob = viewModelScope.launch {
            val fullText = StringBuilder()

            chatAgent.sendMessageStreaming(
                userMessage = userMessageText,
                history = trimmedHistory,
                config = config
            ).collect { event ->
                when (event) {
                    is StreamingEvent.TextDelta -> {
                        fullText.append(event.text)
                        updateState { copy(streamingText = fullText.toString()) }
                    }
                    is StreamingEvent.Complete -> {
                        val assistantMessage = Message(
                            id = assistantMessageId,
                            role = Role.ASSISTANT,
                            text = fullText.toString(),
                            timestamp = currentTimeMillis(),
                            usage = event.usage
                        )

                        val updatedMessages = _uiState.value.messages + assistantMessage
                        updateState {
                            copy(
                                messages = updatedMessages,
                                isLoading = false,
                                isStreaming = false,
                                streamingText = ""
                            )
                        }

                        // Update short-term memory
                        memoryManager.updateShortTermMemory(updatedMessages)
                        saveHistory()
                    }
                    is StreamingEvent.Error -> {
                        updateState {
                            copy(
                                isLoading = false,
                                isStreaming = false,
                                streamingText = "",
                                errorMessage = event.message
                            )
                        }
                    }
                }
            }
        }
    }

    private fun stopGeneration() {
        streamingJob?.cancel()
        streamingJob = null

        val currentState = _uiState.value
        if (currentState.streamingText.isNotEmpty()) {
            val partialMessage = Message(
                id = generateId(),
                role = Role.ASSISTANT,
                text = currentState.streamingText + "\n\n[Остановлено]",
                timestamp = currentTimeMillis(),
                usage = null
            )
            val updatedMessages = currentState.messages + partialMessage
            updateState {
                copy(
                    messages = updatedMessages,
                    isLoading = false,
                    isStreaming = false,
                    streamingText = ""
                )
            }
            memoryManager.updateShortTermMemory(updatedMessages)
            saveHistory()
        } else {
            updateState {
                copy(
                    isLoading = false,
                    isStreaming = false,
                    streamingText = ""
                )
            }
        }
    }

    private fun buildAgentConfig(state: ChatUiState): AgentConfig {
        val temperature = state.temperatureText.toDoubleOrNull() ?: DEFAULT_TEMPERATURE
        val maxTokens = state.maxTokensText.toIntOrNull() ?: DEFAULT_MAX_TOKENS
        val historyTokenLimit = state.historyTokenLimitText.toIntOrNull() ?: DEFAULT_HISTORY_TOKEN_LIMIT

        // Get memory context
        val memoryContext = memoryManager.buildMemoryContext()

        // Combine memory context with user system prompt
        val combinedSystemPrompt = buildString {
            if (memoryContext.isNotBlank()) {
                append(memoryContext)
                if (state.systemPromptText.isNotBlank()) {
                    append("\n\n")
                }
            }
            if (state.systemPromptText.isNotBlank()) {
                append(state.systemPromptText)
            }
        }.takeIf { it.isNotBlank() }

        return AgentConfig(
            systemPrompt = combinedSystemPrompt,
            model = state.selectedModel,
            temperature = temperature.coerceIn(0.0, 1.0),
            maxTokens = maxTokens.coerceIn(1, MAX_TOKENS_LIMIT),
            historyTokenLimit = historyTokenLimit.coerceIn(0, MAX_HISTORY_TOKEN_LIMIT)
        )
    }

    private fun calculateRequestTokens(
        userMessage: String,
        history: List<Message>,
        systemPrompt: String?
    ): Int {
        var tokens = userMessage.estimateTokens()
        tokens += history.sumOf { it.text.estimateTokens() }
        if (!systemPrompt.isNullOrBlank()) {
            tokens += systemPrompt.estimateTokens()
        }
        tokens += (history.size + 1) * 4
        return tokens
    }

    private fun trimHistoryToTokenLimit(messages: List<Message>, tokenLimit: Int): List<Message> {
        if (tokenLimit <= 0) return emptyList()

        var totalTokens = 0
        val trimmedMessages = mutableListOf<Message>()

        for (message in messages.asReversed()) {
            val messageTokens = message.text.estimateTokens()
            if (totalTokens + messageTokens > tokenLimit) break
            trimmedMessages.add(0, message)
            totalTokens += messageTokens
        }

        return trimmedMessages
    }

    private fun clearHistory() {
        updateState {
            copy(
                messages = emptyList(),
                errorMessage = null
            )
        }
        memoryManager.updateShortTermMemory(emptyList())
        viewModelScope.launch {
            historyStorage.clearHistory()
        }
    }

    companion object {
        private const val DEFAULT_TEMPERATURE = 0.7
        private const val DEFAULT_MAX_TOKENS = 512
        private const val DEFAULT_HISTORY_TOKEN_LIMIT = 4000
        private const val MAX_TOKENS_LIMIT = 8192
        private const val MAX_HISTORY_TOKEN_LIMIT = 100000
    }
}
