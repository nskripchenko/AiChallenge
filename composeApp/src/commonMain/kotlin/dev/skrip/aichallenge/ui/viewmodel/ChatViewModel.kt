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
import dev.skrip.aichallenge.domain.statemachine.ParsedResponse
import dev.skrip.aichallenge.domain.statemachine.ResponseParser
import dev.skrip.aichallenge.domain.statemachine.TaskPhase
import dev.skrip.aichallenge.domain.statemachine.TaskPhasePrompts
import dev.skrip.aichallenge.domain.statemachine.TaskStateMachine
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
    private val coinGeckoService: CoinGeckoService,
    private val taskStateMachine: TaskStateMachine
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
        initializeTaskStateMachine()
        observeTaskState()
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

    private fun initializeTaskStateMachine() {
        viewModelScope.launch {
            taskStateMachine.initialize()
        }
    }

    private fun observeTaskState() {
        viewModelScope.launch {
            taskStateMachine.state.collect { taskState ->
                updateState { copy(taskState = taskState) }
            }
        }
        viewModelScope.launch {
            taskStateMachine.error.collect { error ->
                updateState { copy(taskError = error?.message) }
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

            // Task State Machine events
            is ChatViewEvent.StartTask -> handleStartTask(event.request)
            is ChatViewEvent.StartClarifying -> handleStartClarifying(event.questions)
            is ChatViewEvent.AnswerQuestion -> handleAnswerQuestion(event.question, event.answer)
            is ChatViewEvent.CompleteClarifying -> handleCompleteClarifying(event.requirements)
            is ChatViewEvent.StartPlanning -> handleStartPlanning()
            is ChatViewEvent.SubmitPlan -> handleSubmitPlan(event.steps, event.complexity, event.risks)
            is ChatViewEvent.ApprovePlan -> handleApprovePlan()
            is ChatViewEvent.RejectPlan -> handleRejectPlan(event.reason, event.goToClarifying)
            is ChatViewEvent.CompleteStep -> handleCompleteStep(event.output, event.success, event.error)
            is ChatViewEvent.ApproveStep -> handleApproveStep()
            is ChatViewEvent.SubmitValidation -> handleSubmitValidation(event.checks, event.summary)
            is ChatViewEvent.ApproveValidation -> handleApproveValidation()
            is ChatViewEvent.PauseTask -> handlePauseTask(event.reason)
            is ChatViewEvent.ResumeTask -> handleResumeTask()
            is ChatViewEvent.CancelTask -> handleCancelTask()
        }
    }

    private inline fun updateState(crossinline transform: ChatUiState.() -> ChatUiState) {
        _uiState.update { it.transform() }
    }

    private fun handleSendClicked() {
        val currentState = _uiState.value
        val userMessageText = currentState.inputText.trim()

        if (userMessageText.isBlank() || currentState.isLoading || currentState.isStreaming) return

        // Auto-start task and move to planning
        val existingTask = taskStateMachine.state.value
        println("[TASK] Existing task phase: ${existingTask?.currentPhase?.name ?: "null"}")
        if (existingTask == null ||
            existingTask.currentPhase is TaskPhase.Idle ||
            existingTask.currentPhase is TaskPhase.Completed) {
            val startResult = taskStateMachine.startTask(userMessageText)
            println("[TASK] startTask result: ${startResult.isSuccess}, phase: ${taskStateMachine.state.value?.currentPhase?.name}")
            val planResult = taskStateMachine.startPlanning()
            println("[TASK] startPlanning result: ${planResult.isSuccess}, phase: ${taskStateMachine.state.value?.currentPhase?.name}")
        }

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

                            // Process AI response for state machine
                            processAIResponse(fullText.toString())
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

        // Build context (profile + market data) that's always included
        val contextSection = buildString {
            appendLine("=== ДАННЫЕ КЛИЕНТА ===")
            appendLine("Депозит: ${profile.formattedDeposit}")
            appendLine("Цель: +${profile.targetPercent.toInt()}% за ${profile.targetDays} дн.")
            appendLine("Риск: ${profile.riskLevel.label}")
            appendLine("Горизонт: ${profile.investmentHorizon.label}")
            appendLine()

            if (marketData.isNotEmpty()) {
                append(coinGeckoService.formatMarketDataForContext(
                    coins = marketData,
                    currencySymbol = profile.depositCurrency.symbol,
                    limit = 20
                ))
            }
        }

        // Get phase-specific prompt (includes base crypto assistant instructions)
        val taskPhasePrompt = buildTaskPhasePrompt()

        val systemPrompt = buildString {
            // Phase-specific prompt (or default crypto consultant prompt)
            appendLine(taskPhasePrompt ?: CRYPTO_CONSULTANT_PROMPT)
            appendLine()

            // Always include context
            append(contextSection)

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

    // Task State Machine handlers

    private fun handleStartTask(request: String) {
        taskStateMachine.startTask(request).onFailure { error ->
            updateState { copy(taskError = error.message) }
        }
    }

    private fun handleStartClarifying(questions: List<String>) {
        taskStateMachine.startClarifying(questions).onFailure { error ->
            updateState { copy(taskError = error.message) }
        }
    }

    private fun handleAnswerQuestion(question: String, answer: String) {
        taskStateMachine.answerQuestion(question, answer).onFailure { error ->
            updateState { copy(taskError = error.message) }
        }
    }

    private fun handleCompleteClarifying(requirements: String) {
        taskStateMachine.completeClarifying(requirements).onFailure { error ->
            updateState { copy(taskError = error.message) }
        }
    }

    private fun handleStartPlanning() {
        taskStateMachine.startPlanning().onFailure { error ->
            updateState { copy(taskError = error.message) }
        }
    }

    private fun handleSubmitPlan(
        steps: List<dev.skrip.aichallenge.domain.statemachine.PlanStep>,
        complexity: String,
        risks: List<String>
    ) {
        taskStateMachine.submitPlan(steps, complexity, risks).onFailure { error ->
            updateState { copy(taskError = error.message) }
        }
    }

    private fun handleApprovePlan() {
        taskStateMachine.approvePlan().onSuccess {
            // Auto-start first step execution
            continueWithNextStep()
        }.onFailure { error ->
            updateState { copy(taskError = error.message) }
        }
    }

    private fun handleRejectPlan(reason: String, goToClarifying: Boolean) {
        taskStateMachine.rejectPlan(reason, goToClarifying).onFailure { error ->
            updateState { copy(taskError = error.message) }
        }
    }

    private fun handleCompleteStep(output: String, success: Boolean, error: String?) {
        taskStateMachine.completeStep(output, success, error).onFailure { err ->
            updateState { copy(taskError = err.message) }
        }
    }

    private fun handleApproveStep() {
        taskStateMachine.approveStep().onFailure { error ->
            updateState { copy(taskError = error.message) }
        }
    }

    private fun handleSubmitValidation(
        checks: List<dev.skrip.aichallenge.domain.statemachine.ValidationCheck>,
        summary: String
    ) {
        taskStateMachine.submitValidation(checks, summary).onFailure { error ->
            updateState { copy(taskError = error.message) }
        }
    }

    private fun handleApproveValidation() {
        taskStateMachine.approveValidation().onFailure { error ->
            updateState { copy(taskError = error.message) }
        }
    }

    private fun handlePauseTask(reason: String) {
        println("[TASK] handlePauseTask called, current phase: ${taskStateMachine.state.value?.currentPhase?.name}")

        // Stop any ongoing streaming
        streamingJob?.cancel()
        streamingJob = null

        taskStateMachine.pause(reason).onSuccess {
            println("[TASK] Pause SUCCESS, new phase: ${it.currentPhase.name}")
            updateState {
                copy(
                    isLoading = false,
                    isStreaming = false,
                    streamingText = ""
                )
            }
        }.onFailure { error ->
            println("[TASK] Pause FAILED: ${error.message}")
            updateState { copy(taskError = error.message) }
        }
    }

    private fun handleResumeTask() {
        println("[TASK] handleResumeTask called")
        taskStateMachine.resume().onSuccess {
            println("[TASK] Resume SUCCESS, new phase: ${it.currentPhase.name}")
            // After resume, continue execution if we were in Executing phase
            checkAndContinueExecution()
        }.onFailure { error ->
            println("[TASK] Resume FAILED: ${error.message}")
            updateState { copy(taskError = error.message) }
        }
    }

    private fun handleCancelTask() {
        println("[TASK] handleCancelTask called, current phase: ${taskStateMachine.state.value?.currentPhase?.name}")

        // Stop any ongoing streaming
        streamingJob?.cancel()
        streamingJob = null

        taskStateMachine.cancel().onSuccess {
            println("[TASK] Cancel SUCCESS, new phase: ${it.currentPhase.name}")
            updateState {
                copy(
                    isLoading = false,
                    isStreaming = false,
                    streamingText = "",
                    taskError = null
                )
            }
        }.onFailure { error ->
            println("[TASK] Cancel FAILED: ${error.message}")
            updateState { copy(taskError = error.message) }
        }
    }

    /**
     * Build system prompt based on current task phase
     * Reads directly from state machine to avoid race conditions
     */
    private fun buildTaskPhasePrompt(): String? {
        val taskState = taskStateMachine.state.value
        println("[TASK] buildTaskPhasePrompt - phase: ${taskState?.currentPhase?.name ?: "null"}")
        if (taskState == null) return null
        val prompt = TaskPhasePrompts.buildPrompt(taskState)
        println("[TASK] Using prompt for phase: ${taskState.currentPhase.name}, length: ${prompt.length}")
        return prompt
    }

    /**
     * Process AI response and update state machine accordingly
     */
    private fun processAIResponse(response: String) {
        val taskState = taskStateMachine.state.value
        println("[TASK] processAIResponse - taskState: ${taskState?.currentPhase?.name ?: "null"}")
        if (taskState == null) return
        val currentPhase = taskState.currentPhase

        // Skip processing for idle/completed/paused tasks
        if (currentPhase is TaskPhase.Idle ||
            currentPhase is TaskPhase.Completed ||
            currentPhase is TaskPhase.Paused) {
            println("[TASK] Skipping response processing - task is ${currentPhase.name}")
            return
        }

        val parsed = ResponseParser.parse(response, currentPhase)
        println("[TASK] Parsed response: $parsed")

        when (parsed) {
            is ParsedResponse.NoAction -> {
                // No structured output detected, continue waiting
                println("[TASK] NoAction - waiting for proper response format")
            }

            is ParsedResponse.MoveToPlanningPhase -> {
                // Already in planning, request a plan
                if (currentPhase is TaskPhase.Planning) {
                    sendInternalMessage("Create a detailed execution plan for this task.")
                }
            }

            is ParsedResponse.StartClarifying -> {
                taskStateMachine.startClarifying(parsed.questions)
            }

            is ParsedResponse.AddQuestions -> {
                // If already clarifying, just update questions
                if (currentPhase is TaskPhase.Clarifying) {
                    // Questions are added through the prompt context
                }
            }

            is ParsedResponse.CompleteRequirements -> {
                if (currentPhase is TaskPhase.Clarifying) {
                    taskStateMachine.completeClarifying(parsed.requirements)
                    // Auto-transition to planning
                    taskStateMachine.startPlanning()
                }
            }

            is ParsedResponse.SubmitPlan -> {
                val plan = parsed.plan
                // Submit the plan (we should already be in Planning phase)
                if (currentPhase is TaskPhase.Planning) {
                    taskStateMachine.submitPlan(
                        steps = plan.steps,
                        complexity = plan.complexity,
                        risks = plan.risks
                    )
                    println("[TASK] Plan submitted with ${plan.steps.size} steps")
                }
            }

            is ParsedResponse.CompleteStep -> {
                if (currentPhase is TaskPhase.Executing) {
                    taskStateMachine.completeStep(
                        output = parsed.output,
                        success = parsed.success
                    )
                    // Auto-approve step and continue
                    taskStateMachine.approveStep()
                }
            }

            is ParsedResponse.SubmitValidation -> {
                if (currentPhase is TaskPhase.Validating) {
                    taskStateMachine.submitValidation(
                        checks = parsed.checks,
                        summary = parsed.summary
                    )
                    // Auto-approve if all passed
                    if (parsed.allPassed) {
                        taskStateMachine.approveValidation()
                    }
                }
            }
        }

        // Check if we need to continue execution automatically
        checkAndContinueExecution()
    }

    /**
     * Check if the current phase requires automatic continuation
     * and trigger the next AI request if needed
     */
    private fun checkAndContinueExecution() {
        val taskState = taskStateMachine.state.value ?: return

        when (val phase = taskState.currentPhase) {
            is TaskPhase.Executing -> {
                // If step is approved and we're not at the end, continue
                if (phase.isStepApproved && phase.currentStepIndex < phase.totalSteps) {
                    continueWithNextStep()
                }
            }
            is TaskPhase.Validating -> {
                // If we just entered validation, trigger validation
                if (phase.validationChecks.isEmpty()) {
                    triggerValidation()
                }
            }
            else -> {
                // No automatic continuation needed
            }
        }
    }

    /**
     * Continue execution with the next step
     */
    private fun continueWithNextStep() {
        val taskState = taskStateMachine.state.value ?: return
        val phase = taskState.currentPhase as? TaskPhase.Executing ?: return
        val currentStep = taskState.planSteps.getOrNull(phase.currentStepIndex) ?: return

        // Send a system message to trigger next step execution
        val stepMessage = "Execute step ${phase.currentStepIndex + 1}: ${currentStep.title}"
        sendInternalMessage(stepMessage)
    }

    /**
     * Trigger validation phase
     */
    private fun triggerValidation() {
        sendInternalMessage("Validate the completed task and provide validation report.")
    }

    /**
     * Send an internal message to continue the task flow
     */
    private fun sendInternalMessage(message: String) {
        viewModelScope.launch {
            // Small delay to avoid rapid-fire requests
            kotlinx.coroutines.delay(500)

            val currentState = _uiState.value
            if (currentState.isLoading || currentState.isStreaming) return@launch

            // Fetch market data
            val marketData = fetchMarketDataIfNeeded()
            val config = buildAgentConfig(currentState, marketData)

            val shortTermLimit = currentState.memoryState.shortTerm.maxMessages
            val contextMessages = currentState.messages.takeLast(shortTermLimit)
            val trimmedHistory = trimHistoryToTokenLimit(contextMessages, config.historyTokenLimit)

            // Add internal message to UI
            val internalMessage = Message(
                id = generateId(),
                role = Role.USER,
                text = "[Auto] $message",
                timestamp = currentTimeMillis()
            )

            updateState {
                copy(
                    messages = messages + internalMessage,
                    isLoading = true,
                    isStreaming = false
                )
            }

            streamingJob = launch {
                val fullText = StringBuilder()

                chatAgent.sendMessageStreaming(
                    userMessage = message,
                    history = trimmedHistory + internalMessage,
                    config = config
                ).collect { event ->
                    when (event) {
                        is StreamingEvent.TextDelta -> {
                            fullText.append(event.text)
                            updateState { copy(streamingText = fullText.toString(), isStreaming = true) }
                        }
                        is StreamingEvent.Complete -> {
                            val assistantMessage = Message(
                                id = generateId(),
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

                            memoryManager.updateShortTermMemory(updatedMessages)
                            saveHistory()
                            processAIResponse(fullText.toString())
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
