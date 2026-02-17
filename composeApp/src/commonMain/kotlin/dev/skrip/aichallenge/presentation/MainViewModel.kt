package dev.skrip.aichallenge.presentation

import dev.skrip.aichallenge.domain.model.*
import dev.skrip.aichallenge.domain.repository.LogStore
import dev.skrip.aichallenge.domain.usecase.SendPromptUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class MainViewModel(
    private val sendPromptUseCase: SendPromptUseCase,
    private val logStore: LogStore,
    private val onCleanup: () -> Unit = {}
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Jobs для отмены запросов
    private var currentRawJob: Job? = null
    private var currentControlledJob: Job? = null

    companion object {
        private const val MAX_HISTORY_SIZE = 20
    }

    init {
        scope.launch {
            logStore.logs.collect { logs ->
                _uiState.update { it.copy(logs = logs) }
            }
        }
    }

    fun onEvent(event: UiEvent) {
        when (event) {
            is UiEvent.UpdatePrompt -> updatePrompt(event.text)
            is UiEvent.UpdateSystemPrompt -> updateSystemPrompt(event.text)
            is UiEvent.ToggleSendToBoth -> toggleSendToBoth(event.enabled)
            UiEvent.SendClicked -> send()
            UiEvent.ClearClicked -> clear()
            UiEvent.CancelRequest -> cancelRequest()
            is UiEvent.SelectFromHistory -> selectFromHistory(event.prompt)

            is UiEvent.ToggleMultiTurn -> toggleMultiTurn(event.enabled)
            UiEvent.ClearConversation -> clearConversation()

            is UiEvent.ToggleRawStreaming -> toggleRawStreaming(event.enabled)

            is UiEvent.UpdateModel -> updateModel(event.model)
            is UiEvent.UpdateTemperature -> updateTemperature(event.value)
            is UiEvent.UpdateTopP -> updateTopP(event.value)
            is UiEvent.UpdateMaxTokens -> updateMaxTokens(event.value)
            is UiEvent.AddStopSequence -> addStopSequence(event.sequence)
            is UiEvent.RemoveStopSequence -> removeStopSequence(event.sequence)
            is UiEvent.ToggleStreaming -> toggleStreaming(event.enabled)

            is UiEvent.SelectLogTab -> selectLogTab(event.tab)
            is UiEvent.SelectLogEntry -> selectLogEntry(event.entry)
            UiEvent.ClearLogs -> clearLogs()
        }
    }

    private fun updatePrompt(text: String) {
        _uiState.update { it.copy(promptText = text) }
    }

    private fun updateSystemPrompt(text: String) {
        _uiState.update { it.copy(systemPrompt = text) }
    }

    private fun toggleSendToBoth(enabled: Boolean) {
        _uiState.update { it.copy(sendToBoth = enabled) }
    }

    private fun send() {
        val state = _uiState.value
        if (state.promptText.isBlank()) return

        val prompt = state.promptText
        val systemPrompt = state.systemPrompt

        // Добавляем в историю промптов
        addToHistory(prompt)

        val controlledConfig = state.controlledSettings.toConfig(systemPrompt)
        val rawConfig = state.rawSettings.toConfig(state.controlledSettings.model, systemPrompt)

        if (state.sendToBoth) {
            sendBoth(prompt, rawConfig, controlledConfig)
        } else {
            sendControlledOnly(prompt, controlledConfig)
        }
    }

    private fun addToHistory(prompt: String) {
        _uiState.update { state ->
            val newHistory = (listOf(prompt) + state.promptHistory.filter { it != prompt })
                .take(MAX_HISTORY_SIZE)
            state.copy(promptHistory = newHistory)
        }
    }

    private fun sendBoth(prompt: String, rawConfig: LlmRequestConfig, controlledConfig: LlmRequestConfig) {
        _uiState.update {
            it.copy(
                rawStatus = if (rawConfig.streaming) RequestStatus.STREAMING else RequestStatus.SENDING,
                controlledStatus = if (controlledConfig.streaming) RequestStatus.STREAMING else RequestStatus.SENDING,
                rawResult = LlmResult(),
                controlledResult = LlmResult()
            )
        }

        currentRawJob = scope.launch {
            sendRaw(prompt, rawConfig)
        }

        currentControlledJob = scope.launch {
            sendControlled(prompt, controlledConfig)
        }
    }

    private fun sendControlledOnly(prompt: String, config: LlmRequestConfig) {
        _uiState.update {
            it.copy(
                controlledStatus = if (config.streaming) RequestStatus.STREAMING else RequestStatus.SENDING,
                controlledResult = LlmResult()
            )
        }

        currentControlledJob = scope.launch {
            sendControlled(prompt, config)
        }
    }

    private suspend fun sendRaw(prompt: String, config: LlmRequestConfig) {
        try {
            if (config.streaming) {
                sendRawStream(prompt, config)
            } else {
                val result = sendPromptUseCase.sendRaw(prompt, config)
                updateSessionStats(result)
                _uiState.update {
                    it.copy(
                        rawResult = result,
                        rawStatus = if (result.error != null) RequestStatus.ERROR else RequestStatus.IDLE
                    )
                }
            }
        } catch (e: CancellationException) {
            _uiState.update { it.copy(rawStatus = RequestStatus.IDLE) }
            throw e
        }
    }

    private suspend fun sendRawStream(prompt: String, config: LlmRequestConfig) {
        sendPromptUseCase.sendRawStream(prompt, config).collect { result ->
            if (result.isComplete) updateSessionStats(result)
            _uiState.update {
                it.copy(
                    rawResult = result,
                    rawStatus = when {
                        result.error != null -> RequestStatus.ERROR
                        result.isComplete -> RequestStatus.IDLE
                        else -> RequestStatus.STREAMING
                    }
                )
            }
        }
    }

    private suspend fun sendControlled(prompt: String, config: LlmRequestConfig) {
        try {
            if (config.streaming) {
                sendControlledStream(prompt, config)
            } else {
                val result = sendPromptUseCase.sendControlled(prompt, config)
                updateSessionStats(result)
                addToConversation(prompt, result)
                _uiState.update {
                    it.copy(
                        controlledResult = result,
                        controlledStatus = if (result.error != null) RequestStatus.ERROR else RequestStatus.IDLE
                    )
                }
            }
        } catch (e: CancellationException) {
            _uiState.update { it.copy(controlledStatus = RequestStatus.IDLE) }
            throw e
        }
    }

    private suspend fun sendControlledStream(prompt: String, config: LlmRequestConfig) {
        _uiState.update { it.copy(controlledStatus = RequestStatus.STREAMING) }

        var finalResult: LlmResult? = null
        sendPromptUseCase.sendControlledStream(prompt, config).collect { result ->
            if (result.isComplete) {
                finalResult = result
                updateSessionStats(result)
            }

            _uiState.update {
                it.copy(
                    controlledResult = result,
                    controlledStatus = when {
                        result.error != null -> RequestStatus.ERROR
                        result.isComplete -> RequestStatus.IDLE
                        else -> RequestStatus.STREAMING
                    }
                )
            }
        }

        finalResult?.let { addToConversation(prompt, it) }
    }

    private fun addToConversation(prompt: String, result: LlmResult) {
        if (!_uiState.value.isMultiTurnEnabled || result.error != null) return

        _uiState.update { state ->
            val newHistory = state.conversationHistory +
                    ChatMessage("user", prompt) +
                    ChatMessage("assistant", result.text)
            state.copy(conversationHistory = newHistory)
        }
    }

    private fun updateSessionStats(result: LlmResult) {
        if (result.error != null) return

        val modelInfo = AvailableModels.getInfo(_uiState.value.controlledSettings.model)
        val inputCost = (result.inputTokens ?: 0) * (modelInfo?.inputPricePerMillion ?: 0.0) / 1_000_000
        val outputCost = (result.outputTokens ?: 0) * (modelInfo?.outputPricePerMillion ?: 0.0) / 1_000_000

        _uiState.update { state ->
            state.copy(
                sessionStats = state.sessionStats.copy(
                    totalInputTokens = state.sessionStats.totalInputTokens + (result.inputTokens ?: 0),
                    totalOutputTokens = state.sessionStats.totalOutputTokens + (result.outputTokens ?: 0),
                    totalRequests = state.sessionStats.totalRequests + 1,
                    totalCost = state.sessionStats.totalCost + inputCost + outputCost
                )
            )
        }
    }

    private fun cancelRequest() {
        currentRawJob?.cancel()
        currentControlledJob?.cancel()
        _uiState.update {
            it.copy(
                rawStatus = if (it.rawStatus != RequestStatus.IDLE) RequestStatus.IDLE else it.rawStatus,
                controlledStatus = if (it.controlledStatus != RequestStatus.IDLE) RequestStatus.IDLE else it.controlledStatus
            )
        }
    }

    private fun selectFromHistory(prompt: String) {
        _uiState.update { it.copy(promptText = prompt) }
    }

    private fun toggleMultiTurn(enabled: Boolean) {
        _uiState.update { it.copy(isMultiTurnEnabled = enabled) }
    }

    private fun clearConversation() {
        _uiState.update { it.copy(conversationHistory = emptyList()) }
    }

    private fun clear() {
        cancelRequest()
        _uiState.update {
            it.copy(
                promptText = "",
                rawResult = LlmResult(),
                controlledResult = LlmResult(),
                rawStatus = RequestStatus.IDLE,
                controlledStatus = RequestStatus.IDLE
            )
        }
    }

    private fun toggleRawStreaming(enabled: Boolean) {
        _uiState.update { it.copy(rawSettings = it.rawSettings.copy(streaming = enabled)) }
    }

    private fun updateModel(model: String) {
        updateSettings { it.copy(model = model) }
    }

    private fun updateTemperature(value: Float) {
        updateSettings { it.copy(temperature = value.coerceIn(0f, 1f)) }
    }

    private fun updateTopP(value: Float?) {
        updateSettings { it.copy(topP = value?.coerceIn(0f, 1f)) }
    }

    private fun updateMaxTokens(value: Int) {
        updateSettings { it.copy(maxTokens = value.coerceIn(1, 8192)) }
    }

    private fun addStopSequence(sequence: String) {
        if (sequence.isBlank()) return
        updateSettings { s ->
            if (sequence !in s.stopSequences) {
                s.copy(stopSequences = s.stopSequences + sequence)
            } else s
        }
    }

    private fun removeStopSequence(sequence: String) {
        updateSettings { it.copy(stopSequences = it.stopSequences - sequence) }
    }

    private fun toggleStreaming(enabled: Boolean) {
        updateSettings { it.copy(streaming = enabled) }
        // Sync raw streaming with controlled
        _uiState.update { it.copy(rawSettings = it.rawSettings.copy(streaming = enabled)) }
    }

    private fun updateSettings(transform: (ControlledSettings) -> ControlledSettings) {
        _uiState.update { it.copy(controlledSettings = transform(it.controlledSettings)) }
    }

    private fun selectLogTab(tab: LogTab) {
        _uiState.update { it.copy(selectedLogTab = tab) }
    }

    private fun selectLogEntry(entry: LogEntry?) {
        _uiState.update { it.copy(selectedLogEntry = entry) }
    }

    private fun clearLogs() {
        logStore.clearLogs()
        _uiState.update { it.copy(selectedLogEntry = null) }
    }

    fun onCleared() {
        scope.cancel()
        onCleanup()
    }
}
