package dev.skrip.aichallenge.ui.viewmodel

import dev.skrip.aichallenge.data.remote.CoinGeckoService
import dev.skrip.aichallenge.data.remote.dto.CoinMarketData
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
    private val memoryManager: MemoryManager,
    private val coinGeckoService: CoinGeckoService
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val logs: StateFlow<List<LogEntry>> = agentLogger.logs

    private var streamingJob: Job? = null
    private var cachedMarketData: List<CoinMarketData> = emptyList()
    private var cachedCurrency: String = ""
    private var lastMarketDataFetch: Long = 0
    private val marketDataCacheMs = 60_000L // 1 minute cache

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

            // Working memory (notes)
            is ChatViewEvent.AddToWorkingMemory -> viewModelScope.launch {
                memoryManager.addToWorkingMemory(event.label, event.content)
            }
            is ChatViewEvent.RemoveFromWorkingMemory -> viewModelScope.launch {
                memoryManager.removeFromWorkingMemory(event.itemId)
            }

            // Profile
            is ChatViewEvent.UpdateProfile -> viewModelScope.launch {
                memoryManager.updateProfile(event.profile)
            }

            // Memory
            is ChatViewEvent.ClearAllMemory -> clearAllMemory()
        }
    }

    private inline fun updateState(crossinline transform: ChatUiState.() -> ChatUiState) {
        _uiState.update { it.transform() }
    }

    private fun handleSendClicked() {
        val currentState = _uiState.value
        val userMessageText = currentState.inputText.trim()

        if (userMessageText.isBlank() || currentState.isLoading || currentState.isStreaming) return

        // Update state to show loading
        updateState {
            copy(
                inputText = "",
                isLoading = true,
                isStreaming = false,
                streamingText = "",
                errorMessage = null
            )
        }

        viewModelScope.launch {
            // Fetch market data if needed
            val marketData = fetchMarketDataIfNeeded()

            // Build config with market data
            val config = buildAgentConfig(currentState, marketData)

            val assistantMessageId = generateId()

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
                    isStreaming = true
                )
            }

            streamingJob = launch {
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
    }

    private suspend fun fetchMarketDataIfNeeded(): List<CoinMarketData> {
        val now = currentTimeMillis()
        val currency = _uiState.value.memoryState.longTerm.profile.depositCurrency.apiCode

        // Check cache: valid if same currency and not expired
        val cacheValid = cachedMarketData.isNotEmpty() &&
                cachedCurrency == currency &&
                now - lastMarketDataFetch < marketDataCacheMs

        if (cacheValid) {
            return cachedMarketData
        }

        updateState { copy(isLoadingMarketData = true) }

        return coinGeckoService.getMarketData(currency = currency, limit = 30)
            .onSuccess { data ->
                cachedMarketData = data
                cachedCurrency = currency
                lastMarketDataFetch = now
                updateState {
                    copy(
                        isLoadingMarketData = false,
                        lastMarketDataUpdate = now
                    )
                }
            }
            .onFailure {
                updateState { copy(isLoadingMarketData = false) }
            }
            .getOrElse { cachedMarketData }
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

    private fun buildAgentConfig(state: ChatUiState, marketData: List<CoinMarketData>): AgentConfig {
        val temperature = state.temperatureText.toDoubleOrNull() ?: DEFAULT_TEMPERATURE
        val maxTokens = state.maxTokensText.toIntOrNull() ?: DEFAULT_MAX_TOKENS
        val historyTokenLimit = state.historyTokenLimitText.toIntOrNull() ?: DEFAULT_HISTORY_TOKEN_LIMIT

        val profile = state.memoryState.longTerm.profile

        // Build crypto consultant system prompt
        val systemPrompt = buildString {
            // Base role
            appendLine(CRYPTO_CONSULTANT_PROMPT)
            appendLine()

            // User profile as ДАНО
            appendLine("=== ДАННЫЕ КЛИЕНТА ===")
            appendLine("Депозит: ${profile.formattedDeposit}")
            appendLine("Цель: +${profile.targetPercent.toInt()}% за ${profile.targetDays} дн.")
            appendLine("Риск: ${profile.riskLevel.label}")
            appendLine("Горизонт: ${profile.investmentHorizon.label}")
            appendLine()

            // Market data
            if (marketData.isNotEmpty()) {
                append(coinGeckoService.formatMarketDataForContext(
                    coins = marketData,
                    currencySymbol = profile.depositCurrency.symbol,
                    limit = 20
                ))
            }

            // User's additional prompt
            if (state.systemPromptText.isNotBlank()) {
                appendLine()
                appendLine("=== ДОПОЛНИТЕЛЬНЫЕ ИНСТРУКЦИИ ===")
                append(state.systemPromptText)
            }
        }

        return AgentConfig(
            systemPrompt = systemPrompt,
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

    private fun clearAllMemory() {
        // Clear chat history
        updateState {
            copy(
                messages = emptyList(),
                errorMessage = null
            )
        }
        // Clear all memory layers
        viewModelScope.launch {
            historyStorage.clearHistory()
            memoryManager.clearAllMemory()
        }
    }

    companion object {
        private const val DEFAULT_TEMPERATURE = 0.3  // Lower for more precise financial advice
        private const val DEFAULT_MAX_TOKENS = 4096  // Longer responses for detailed analysis
        private const val DEFAULT_HISTORY_TOKEN_LIMIT = 16000  // More context
        private const val MAX_TOKENS_LIMIT = 8192
        private const val MAX_HISTORY_TOKEN_LIMIT = 100000

        private val CRYPTO_CONSULTANT_PROMPT = """
Ты — крипто-аналитик. Анализируй данные из контекста.

ФОРМАТ ОТВЕТА (строго):

ДАНО: [депозит] | цель [%]% за [дней]д | [риск]

РЫНОК: [1-2 предложения о текущей ситуации по данным]

РЕКОМЕНДАЦИЯ 1: [МОНЕТА]
- Купить на: [сумма] ([%]% депозита)
- Цена сейчас: [из данных]
- Вход: [цена] / TP: [цена] (+[%]%) / SL: [цена] (-[%]%)
- Изменение: 24ч [%], 7д [%]
- Объем: [из данных]
- Обоснование: [Почему именно эта монета? Анализ тренда, объема, позиции в рынке. 2-3 предложения с фактами.]

РЕКОМЕНДАЦИЯ 2: [МОНЕТА] (если нужно)
[тот же формат]

РИСКИ: [Конкретные риски и условия выхода]

ПРАВИЛА:
- Бери цены из ДАННЫХ РЫНКА в контексте
- Анализируй: изменение 24ч/7д, объем торгов, капитализация
- Обоснование должно быть конкретным с цифрами
- Размер позиции зависит от риска клиента
- Максимум 2 монеты
- НЕ используй markdown таблицы
- НЕ используй заголовки с #
        """.trimIndent()
    }
}
