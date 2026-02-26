package dev.skrip.aichallenge.ui.viewmodel

import dev.skrip.aichallenge.data.source.StreamingEvent
import dev.skrip.aichallenge.domain.model.AgentConfig
import dev.skrip.aichallenge.domain.model.ConversationState
import dev.skrip.aichallenge.domain.model.Message
import dev.skrip.aichallenge.domain.model.ModelId
import dev.skrip.aichallenge.domain.model.Role
import dev.skrip.aichallenge.domain.model.TokenUsage
import dev.skrip.aichallenge.domain.repository.ChatAgent
import dev.skrip.aichallenge.domain.repository.ChatHistoryStorage
import dev.skrip.aichallenge.domain.repository.ContextCompressor
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
    private val contextCompressor: ContextCompressor
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val logs: StateFlow<List<LogEntry>> = agentLogger.logs

    private var streamingJob: Job? = null
    private var conversationState = ConversationState()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            conversationState = historyStorage.loadState()
            if (conversationState.messages.isNotEmpty()) {
                updateState {
                    copy(
                        messages = conversationState.messages,
                        summary = conversationState.summary,
                        summarizedCount = conversationState.summarizedCount
                    )
                }
            }
        }
    }

    private fun saveHistory() {
        viewModelScope.launch {
            historyStorage.saveState(conversationState)
        }
    }

    fun onEvent(event: ChatViewEvent) {
        when (event) {
            is ChatViewEvent.InputChanged -> updateState { copy(inputText = event.text) }
            is ChatViewEvent.SystemPromptChanged -> updateState { copy(systemPromptText = event.text) }
            is ChatViewEvent.TemperatureChanged -> updateState { copy(temperatureText = event.text) }
            is ChatViewEvent.MaxTokensChanged -> updateState { copy(maxTokensText = event.text) }
            is ChatViewEvent.ModelChanged -> updateState { copy(selectedModel = event.model) }
            is ChatViewEvent.HistoryTokenLimitChanged -> updateState { copy(historyTokenLimitText = event.text) }
            is ChatViewEvent.KeepRecentMessagesChanged -> updateState { copy(keepRecentMessagesText = event.text) }
            is ChatViewEvent.ClearHistory -> clearHistory()
            is ChatViewEvent.StopGeneration -> stopGeneration()
            is ChatViewEvent.SendClicked -> handleSendClicked()
        }
    }

    private inline fun updateState(crossinline transform: ChatUiState.() -> ChatUiState) {
        _uiState.update { it.transform() }
    }

    private fun handleSendClicked() {
        val currentState = _uiState.value
        val userMessageText = currentState.inputText.trim()

        if (userMessageText.isBlank() || currentState.isLoading || currentState.isStreaming) return

        val config = buildAgentConfig(currentState)
        val assistantMessageId = generateId()
        val keepRecent = currentState.keepRecentMessages

        // Build context messages using compressor (includes summary if available)
        val contextMessages = contextCompressor.buildContextMessages(conversationState, keepRecent)

        // Trim context to fit token limit
        val trimmedHistory = trimHistoryToTokenLimit(
            messages = contextMessages,
            tokenLimit = config.historyTokenLimit
        )

        // Calculate estimated input tokens for this request
        val estimatedInputTokens = calculateRequestTokens(
            userMessage = userMessageText,
            history = trimmedHistory,
            systemPrompt = config.systemPrompt
        )

        val userMessage = createUserMessage(
            text = userMessageText,
            estimatedInputTokens = estimatedInputTokens,
            model = config.model
        )

        // Update conversation state with user message
        conversationState = conversationState.copy(
            messages = conversationState.messages + userMessage
        )

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
            var fullText = StringBuilder()

            chatAgent.sendMessageStreaming(
                userMessage = userMessageText,
                history = trimmedHistory,
                config = config
            ).collect { event ->
                when (event) {
                    is StreamingEvent.TextDelta -> {
                        fullText.append(event.text)
                        updateState {
                            copy(streamingText = fullText.toString())
                        }
                    }
                    is StreamingEvent.Complete -> {
                        val assistantMessage = Message(
                            id = assistantMessageId,
                            role = Role.ASSISTANT,
                            text = fullText.toString(),
                            timestamp = currentTimeMillis(),
                            usage = event.usage
                        )

                        // Update conversation state with assistant message
                        conversationState = conversationState.copy(
                            messages = conversationState.messages + assistantMessage
                        )

                        // Update UI immediately so user sees the message
                        updateState {
                            copy(
                                messages = conversationState.messages,
                                isLoading = false,
                                isStreaming = false,
                                streamingText = ""
                            )
                        }

                        // Check if compression is needed and compress (may take time)
                        val needsCompression = conversationState.needsCompression(keepRecent)
                        if (needsCompression) {
                            updateState { copy(isCompressing = true) }
                        }

                        conversationState = contextCompressor.compressIfNeeded(
                            conversationState,
                            keepRecent
                        )

                        // Update UI with compression results
                        updateState {
                            copy(
                                summary = conversationState.summary,
                                summarizedCount = conversationState.summarizedCount,
                                isCompressing = false
                            )
                        }
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
            // Save partial response as a message
            val partialMessage = Message(
                id = generateId(),
                role = Role.ASSISTANT,
                text = currentState.streamingText + "\n\n[Generation stopped]",
                timestamp = currentTimeMillis(),
                usage = null
            )
            conversationState = conversationState.copy(
                messages = conversationState.messages + partialMessage
            )
            updateState {
                copy(
                    messages = conversationState.messages,
                    isLoading = false,
                    isStreaming = false,
                    streamingText = ""
                )
            }
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

        return AgentConfig(
            systemPrompt = state.systemPromptText.takeIf { it.isNotBlank() },
            model = state.selectedModel,
            temperature = temperature.coerceIn(0.0, 1.0),
            maxTokens = maxTokens.coerceIn(1, MAX_TOKENS_LIMIT),
            historyTokenLimit = historyTokenLimit.coerceIn(0, MAX_HISTORY_TOKEN_LIMIT)
        )
    }

    private fun createUserMessage(
        text: String,
        estimatedInputTokens: Int,
        model: ModelId
    ): Message {
        return Message(
            id = generateId(),
            role = Role.USER,
            text = text,
            timestamp = currentTimeMillis(),
            usage = TokenUsage(
                inputTokens = estimatedInputTokens,
                outputTokens = 0,
                responseTimeMs = 0,
                model = model
            )
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
        // Add overhead for message formatting (~4 tokens per message)
        tokens += (history.size + 1) * 4
        return tokens
    }

    private fun trimHistoryToTokenLimit(messages: List<Message>, tokenLimit: Int): List<Message> {
        if (tokenLimit <= 0) return emptyList()

        var totalTokens = 0
        val trimmedMessages = mutableListOf<Message>()

        // Iterate from newest to oldest, keep messages until we hit the limit
        for (message in messages.asReversed()) {
            val messageTokens = message.text.estimateTokens()
            if (totalTokens + messageTokens > tokenLimit) break
            trimmedMessages.add(0, message)
            totalTokens += messageTokens
        }

        return trimmedMessages
    }

    private fun clearHistory() {
        conversationState = ConversationState()
        updateState {
            copy(
                messages = emptyList(),
                summary = null,
                summarizedCount = 0,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            historyStorage.clearHistory()
        }
    }

    companion object {
        private const val DEFAULT_TEMPERATURE = 0.7
        private const val DEFAULT_MAX_TOKENS = 512
        private const val DEFAULT_HISTORY_TOKEN_LIMIT = 1000
        private const val MAX_TOKENS_LIMIT = 8192
        private const val MAX_HISTORY_TOKEN_LIMIT = 100000
    }
}
