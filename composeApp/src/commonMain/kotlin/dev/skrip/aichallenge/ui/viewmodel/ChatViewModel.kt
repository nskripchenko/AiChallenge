package dev.skrip.aichallenge.ui.viewmodel

import dev.skrip.aichallenge.domain.model.AgentConfig
import dev.skrip.aichallenge.domain.model.Message
import dev.skrip.aichallenge.domain.model.Role
import dev.skrip.aichallenge.domain.repository.ChatAgent
import dev.skrip.aichallenge.logging.AgentLogger
import dev.skrip.aichallenge.logging.LogEntry
import dev.skrip.aichallenge.ui.state.ChatUiState
import dev.skrip.aichallenge.ui.state.ChatViewEvent
import dev.skrip.aichallenge.util.currentTimeMillis
import dev.skrip.aichallenge.util.estimateTokens
import dev.skrip.aichallenge.util.generateId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatAgent: ChatAgent,
    agentLogger: AgentLogger
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val logs: StateFlow<List<LogEntry>> = agentLogger.logs

    fun onEvent(event: ChatViewEvent) {
        when (event) {
            is ChatViewEvent.InputChanged -> updateState { copy(inputText = event.text) }
            is ChatViewEvent.SystemPromptChanged -> updateState { copy(systemPromptText = event.text) }
            is ChatViewEvent.TemperatureChanged -> updateState { copy(temperatureText = event.text) }
            is ChatViewEvent.MaxTokensChanged -> updateState { copy(maxTokensText = event.text) }
            is ChatViewEvent.ModelChanged -> updateState { copy(selectedModel = event.model) }
            is ChatViewEvent.HistoryTokenLimitChanged -> updateState { copy(historyTokenLimitText = event.text) }
            is ChatViewEvent.ClearHistory -> updateState { copy(messages = emptyList(), errorMessage = null) }
            is ChatViewEvent.SendClicked -> handleSendClicked()
        }
    }

    private inline fun updateState(crossinline transform: ChatUiState.() -> ChatUiState) {
        _uiState.update { it.transform() }
    }

    private fun handleSendClicked() {
        val currentState = _uiState.value
        val userMessageText = currentState.inputText.trim()

        if (userMessageText.isBlank() || currentState.isLoading) return

        val config = buildAgentConfig(currentState)
        val userMessage = createUserMessage(userMessageText)

        // Trim history to fit token limit
        val trimmedHistory = trimHistoryToTokenLimit(
            messages = currentState.messages,
            tokenLimit = config.historyTokenLimit
        )

        updateState {
            copy(
                messages = messages + userMessage,
                inputText = "",
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            chatAgent.sendMessage(
                userMessage = userMessageText,
                history = trimmedHistory,
                config = config
            ).fold(
                onSuccess = { assistantMessage ->
                    updateState {
                        copy(
                            messages = messages + assistantMessage,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    updateState {
                        copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Unknown error occurred"
                        )
                    }
                }
            )
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

    private fun createUserMessage(text: String): Message {
        return Message(
            id = generateId(),
            role = Role.USER,
            text = text,
            timestamp = currentTimeMillis()
        )
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

    companion object {
        private const val DEFAULT_TEMPERATURE = 0.7
        private const val DEFAULT_MAX_TOKENS = 512
        private const val DEFAULT_HISTORY_TOKEN_LIMIT = 1000
        private const val MAX_TOKENS_LIMIT = 8192
        private const val MAX_HISTORY_TOKEN_LIMIT = 100000
    }
}
