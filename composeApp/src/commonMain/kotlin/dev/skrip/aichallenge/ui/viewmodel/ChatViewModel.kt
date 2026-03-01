package dev.skrip.aichallenge.ui.viewmodel

import dev.skrip.aichallenge.data.source.StreamingEvent
import dev.skrip.aichallenge.domain.model.AgentConfig
import dev.skrip.aichallenge.domain.model.ContextStrategy
import dev.skrip.aichallenge.domain.model.ConversationState
import dev.skrip.aichallenge.domain.model.Message
import dev.skrip.aichallenge.domain.model.ModelId
import dev.skrip.aichallenge.domain.model.Role
import dev.skrip.aichallenge.domain.model.TokenUsage
import dev.skrip.aichallenge.domain.repository.ChatAgent
import dev.skrip.aichallenge.domain.repository.ChatHistoryStorage
import dev.skrip.aichallenge.domain.repository.ContextCompressor
import dev.skrip.aichallenge.domain.repository.FactsExtractor
import dev.skrip.aichallenge.logging.AgentLogger
import dev.skrip.aichallenge.logging.LogEntry
import dev.skrip.aichallenge.ui.state.Branch
import dev.skrip.aichallenge.ui.state.ChatUiState
import dev.skrip.aichallenge.ui.state.ChatViewEvent
import dev.skrip.aichallenge.ui.state.Fact
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
    private val contextCompressor: ContextCompressor,
    private val factsExtractor: FactsExtractor
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
            // Common events
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

            // Strategy events
            is ChatViewEvent.StrategyChanged -> updateState { copy(currentStrategy = event.strategy) }

            // Sliding Window events
            is ChatViewEvent.WindowSizeChanged -> updateState { copy(windowSizeText = event.text) }

            // Sticky Facts events
            is ChatViewEvent.AddFact -> handleAddFact(event.key, event.value)
            is ChatViewEvent.RemoveFact -> handleRemoveFact(event.key)
            is ChatViewEvent.UpdateFact -> handleUpdateFact(event.key, event.newValue)

            // Branching events
            is ChatViewEvent.CreateBranch -> handleCreateBranch(event.name, event.fromMessageIndex)
            is ChatViewEvent.SwitchBranch -> handleSwitchBranch(event.branchId)
            is ChatViewEvent.DeleteBranch -> handleDeleteBranch(event.branchId)
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
        val keepRecent = currentState.keepRecentMessages
        val currentStrategy = currentState.currentStrategy

        // Build context messages based on current strategy
        // For BRANCHING: use currentState.messages (reflects current branch)
        // For others: use conversationState.messages (main history)
        val contextMessages = when (currentStrategy) {
            ContextStrategy.SLIDING_WINDOW -> {
                conversationState.messages.takeLast(currentState.windowSize)
            }
            ContextStrategy.STICKY_FACTS -> {
                conversationState.messages.takeLast(keepRecent)
            }
            ContextStrategy.BRANCHING -> {
                // Use current branch messages, not main conversationState
                currentState.messages.takeLast(keepRecent)
            }
        }


        // Build facts context for STICKY_FACTS strategy
        val factsContext = if (currentStrategy == ContextStrategy.STICKY_FACTS && currentState.facts.isNotEmpty()) {
            buildFactsSystemPrompt(currentState.facts)
        } else null

        // Rebuild config with facts context
        val configWithFacts = buildAgentConfig(currentState, factsContext)

        // Trim context to fit token limit
        val trimmedHistory = trimHistoryToTokenLimit(
            messages = contextMessages,
            tokenLimit = configWithFacts.historyTokenLimit
        )

        // Calculate estimated input tokens for this request
        val estimatedInputTokens = calculateRequestTokens(
            userMessage = userMessageText,
            history = trimmedHistory,
            systemPrompt = configWithFacts.systemPrompt
        )

        val userMessage = createUserMessage(
            text = userMessageText,
            estimatedInputTokens = estimatedInputTokens,
            model = configWithFacts.model
        )

        // Update state with user message
        // For BRANCHING: only update UI state (branch-specific)
        // For others: update both conversationState and UI state
        if (currentStrategy != ContextStrategy.BRANCHING) {
            conversationState = conversationState.copy(
                messages = conversationState.messages + userMessage
            )
        }

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
                config = configWithFacts
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

                        // Update state with assistant message
                        if (currentStrategy != ContextStrategy.BRANCHING) {
                            conversationState = conversationState.copy(
                                messages = conversationState.messages + assistantMessage
                            )
                        }

                        // Update UI immediately so user sees the message
                        updateState {
                            copy(
                                messages = messages + assistantMessage,
                                isLoading = false,
                                isStreaming = false,
                                streamingText = ""
                            )
                        }

                        // Extract facts for STICKY_FACTS strategy
                        if (currentStrategy == ContextStrategy.STICKY_FACTS) {
                            updateState { copy(isExtractingFacts = true) }

                            try {
                                val existingFacts = _uiState.value.facts
                                val extractedFacts = factsExtractor.extractFacts(
                                    userMessage = userMessageText,
                                    assistantResponse = fullText.toString(),
                                    existingFacts = existingFacts
                                )

                                val factsChanged = extractedFacts != existingFacts
                                val updatedCount = if (factsChanged) {
                                    _uiState.value.factsUpdatedCount + 1
                                } else {
                                    _uiState.value.factsUpdatedCount
                                }

                                updateState {
                                    copy(
                                        facts = extractedFacts,
                                        factsUpdatedCount = updatedCount,
                                        isExtractingFacts = false
                                    )
                                }
                            } catch (e: Exception) {
                                // On error, just stop the extraction indicator
                                updateState { copy(isExtractingFacts = false) }
                            }
                        }

                        // Only run compression for SUMMARIZATION strategy (not SLIDING_WINDOW or STICKY_FACTS)
                        if (currentStrategy == ContextStrategy.BRANCHING) {
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

    private fun buildAgentConfig(state: ChatUiState, factsContext: String? = null): AgentConfig {
        val temperature = state.temperatureText.toDoubleOrNull() ?: DEFAULT_TEMPERATURE
        val maxTokens = state.maxTokensText.toIntOrNull() ?: DEFAULT_MAX_TOKENS
        val historyTokenLimit = state.historyTokenLimitText.toIntOrNull() ?: DEFAULT_HISTORY_TOKEN_LIMIT

        // Combine user system prompt with facts context
        val combinedSystemPrompt = buildString {
            if (factsContext != null) {
                append(factsContext)
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

    private fun buildFactsSystemPrompt(facts: List<Fact>): String = buildString {
        appendLine("Important facts to remember about this conversation:")
        facts.forEach { fact ->
            appendLine("- ${fact.key}: ${fact.value}")
        }
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

    // Sticky Facts handlers
    private fun handleAddFact(key: String, value: String) {
        updateState {
            val newFact = Fact(key = key, value = value, timestamp = currentTimeMillis())
            copy(facts = facts + newFact)
        }
    }

    private fun handleRemoveFact(key: String) {
        updateState {
            copy(facts = facts.filter { it.key != key })
        }
    }

    private fun handleUpdateFact(key: String, newValue: String) {
        updateState {
            copy(facts = facts.map {
                if (it.key == key) it.copy(value = newValue, timestamp = currentTimeMillis()) else it
            })
        }
    }

    // Branching handlers
    private fun handleCreateBranch(name: String, fromMessageIndex: Int) {
        val branchId = generateId()
        val currentState = _uiState.value
        val branchMessages = currentState.messages.take(fromMessageIndex + 1)

        val newBranch = Branch(
            id = branchId,
            name = name,
            parentBranchId = currentState.currentBranchId,
            forkMessageIndex = fromMessageIndex,
            messages = branchMessages
        )

        updateState {
            copy(
                branches = branches + newBranch,
                currentBranchId = branchId,
                messages = branchMessages
            )
        }
    }

    private fun handleSwitchBranch(branchId: String) {
        val currentState = _uiState.value

        // Save current branch messages before switching
        val updatedBranches = if (currentState.currentBranchId != "main") {
            currentState.branches.map { branch ->
                if (branch.id == currentState.currentBranchId) {
                    branch.copy(messages = currentState.messages)
                } else branch
            }
        } else {
            currentState.branches
        }

        // Switch to new branch
        if (branchId == "main") {
            updateState {
                copy(
                    branches = updatedBranches,
                    currentBranchId = "main",
                    messages = conversationState.messages
                )
            }
        } else {
            val targetBranch = updatedBranches.find { it.id == branchId }
            if (targetBranch != null) {
                updateState {
                    copy(
                        branches = updatedBranches,
                        currentBranchId = branchId,
                        messages = targetBranch.messages
                    )
                }
            }
        }
    }

    private fun handleDeleteBranch(branchId: String) {
        if (branchId == "main") return

        val currentState = _uiState.value
        val wasCurrentBranch = currentState.currentBranchId == branchId

        updateState {
            copy(
                branches = branches.filter { it.id != branchId },
                currentBranchId = if (wasCurrentBranch) "main" else currentBranchId,
                messages = if (wasCurrentBranch) conversationState.messages else messages
            )
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
