package dev.skrip.aichallenge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import dev.skrip.aichallenge.ui.state.ChatViewEvent
import dev.skrip.aichallenge.ui.viewmodel.ChatViewModel
import java.awt.Cursor

@Composable
fun ChatApp(viewModel: ChatViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val logs by viewModel.logs.collectAsState()

    var totalWidth by remember { mutableStateOf(0f) }
    var logPanelWeight by remember { mutableStateOf(0.20f) }
    var chatPanelWeight by remember { mutableStateOf(0.50f) }
    val settingsPanelWeight = 1f - logPanelWeight - chatPanelWeight

    MaterialTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { totalWidth = it.width.toFloat() },
            color = Color.White
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                LogPanel(
                    logs = logs,
                    modifier = Modifier.weight(logPanelWeight)
                )

                Divider(
                    onDrag = { delta ->
                        val deltaWeight = delta / totalWidth
                        val newLogWeight = (logPanelWeight + deltaWeight).coerceIn(0.15f, 0.35f)
                        val newChatWeight = (chatPanelWeight - deltaWeight).coerceIn(0.35f, 0.55f)
                        if (newLogWeight + newChatWeight + settingsPanelWeight <= 1f) {
                            logPanelWeight = newLogWeight
                            chatPanelWeight = newChatWeight
                        }
                    }
                )

                ChatPanel(
                    messages = uiState.messages,
                    inputText = uiState.inputText,
                    isLoading = uiState.isLoading,
                    isStreaming = uiState.isStreaming,
                    streamingText = uiState.streamingText,
                    errorMessage = uiState.errorMessage,
                    onInputChanged = { viewModel.onEvent(ChatViewEvent.InputChanged(it)) },
                    onSendClicked = { viewModel.onEvent(ChatViewEvent.SendClicked) },
                    onStopClicked = { viewModel.onEvent(ChatViewEvent.StopGeneration) },
                    modifier = Modifier.weight(chatPanelWeight)
                )

                Divider(
                    onDrag = { delta ->
                        val deltaWeight = delta / totalWidth
                        val newChatWeight = (chatPanelWeight + deltaWeight).coerceIn(0.35f, 0.55f)
                        val newSettingsWeight = (settingsPanelWeight - deltaWeight).coerceIn(0.25f, 0.40f)
                        if (logPanelWeight + newChatWeight + newSettingsWeight <= 1f) {
                            chatPanelWeight = newChatWeight
                        }
                    }
                )

                AgentSettingsPanel(
                    systemPrompt = uiState.systemPromptText,
                    selectedModel = uiState.selectedModel,
                    temperatureText = uiState.temperatureText,
                    maxTokensText = uiState.maxTokensText,
                    historyTokenLimitText = uiState.historyTokenLimitText,
                    estimatedHistoryTokens = uiState.estimatedHistoryTokens,
                    historyTokensRemaining = uiState.historyTokensRemaining,
                    totalMessages = uiState.messages.size,
                    sessionStats = uiState.sessionStats,
                    // Memory model
                    memoryState = uiState.memoryState,
                    selectedMemoryLayer = uiState.selectedMemoryLayer,
                    isMemoryPanelExpanded = uiState.isMemoryPanelExpanded,
                    onSelectMemoryLayer = { viewModel.onEvent(ChatViewEvent.SelectMemoryLayer(it)) },
                    onToggleMemoryPanel = { viewModel.onEvent(ChatViewEvent.ToggleMemoryPanel) },
                    onAddToWorkingMemory = { label, content -> viewModel.onEvent(ChatViewEvent.AddToWorkingMemory(label, content)) },
                    onRemoveFromWorkingMemory = { viewModel.onEvent(ChatViewEvent.RemoveFromWorkingMemory(it)) },
                    onClearWorkingMemory = { viewModel.onEvent(ChatViewEvent.ClearWorkingMemory) },
                    onUpdateProfile = { viewModel.onEvent(ChatViewEvent.UpdateProfile(it)) },
                    onAddDecision = { title, desc -> viewModel.onEvent(ChatViewEvent.AddDecision(title, desc)) },
                    onRemoveDecision = { viewModel.onEvent(ChatViewEvent.RemoveDecision(it)) },
                    onAddKnowledge = { cat, title, content -> viewModel.onEvent(ChatViewEvent.AddKnowledge(cat, title, content)) },
                    onRemoveKnowledge = { viewModel.onEvent(ChatViewEvent.RemoveKnowledge(it)) },
                    onClearLongTermMemory = { viewModel.onEvent(ChatViewEvent.ClearLongTermMemory) },
                    onSetShortTermLimit = { viewModel.onEvent(ChatViewEvent.SetShortTermLimit(it)) },
                    // Original callbacks
                    onSystemPromptChanged = { viewModel.onEvent(ChatViewEvent.SystemPromptChanged(it)) },
                    onModelChanged = { viewModel.onEvent(ChatViewEvent.ModelChanged(it)) },
                    onTemperatureChanged = { viewModel.onEvent(ChatViewEvent.TemperatureChanged(it)) },
                    onMaxTokensChanged = { viewModel.onEvent(ChatViewEvent.MaxTokensChanged(it)) },
                    onHistoryTokenLimitChanged = { viewModel.onEvent(ChatViewEvent.HistoryTokenLimitChanged(it)) },
                    onClearHistory = { viewModel.onEvent(ChatViewEvent.ClearHistory) },
                    modifier = Modifier.weight(settingsPanelWeight)
                )
            }
        }
    }
}

@Composable
private fun Divider(onDrag: (Float) -> Unit) {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(Color(0xFFE5E5E5))
            .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x)
                }
            }
    )
}
