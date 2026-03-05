package dev.skrip.aichallenge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.skrip.aichallenge.domain.model.MemoryState
import dev.skrip.aichallenge.ui.state.ChatViewEvent
import dev.skrip.aichallenge.ui.viewmodel.ChatViewModel
import java.awt.Cursor

@Composable
fun ChatApp(viewModel: ChatViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val logs by viewModel.logs.collectAsState()

    var totalWidth by remember { mutableStateOf(0f) }
    var logPanelWeight by remember { mutableStateOf(0.18f) }
    var chatPanelWeight by remember { mutableStateOf(0.52f) }
    val profilePanelWeight = 1f - logPanelWeight - chatPanelWeight

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showMemoryDialog by remember { mutableStateOf(false) }

    MaterialTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { totalWidth = it.width.toFloat() },
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // App Header
                AppHeader(
                    onSettingsClick = { showSettingsDialog = true },
                    onMemoryClick = { showMemoryDialog = true }
                )

                // Main Content
                Row(modifier = Modifier.weight(1f)) {
                    // Left: Log Panel
                    LogPanel(
                        logs = logs,
                        modifier = Modifier.weight(logPanelWeight)
                    )

                    Divider(
                        onDrag = { delta ->
                            val deltaWeight = delta / totalWidth
                            val newLogWeight = (logPanelWeight + deltaWeight).coerceIn(0.12f, 0.25f)
                            val newChatWeight = (chatPanelWeight - deltaWeight).coerceIn(0.40f, 0.60f)
                            if (newLogWeight + newChatWeight + profilePanelWeight <= 1f) {
                                logPanelWeight = newLogWeight
                                chatPanelWeight = newChatWeight
                            }
                        }
                    )

                    // Center: Chat Panel
                    ChatPanel(
                        messages = uiState.messages,
                        inputText = uiState.inputText,
                        isLoading = uiState.isLoading,
                        isStreaming = uiState.isStreaming,
                        streamingText = uiState.streamingText,
                        errorMessage = uiState.errorMessage,
                        taskState = uiState.taskState,
                        taskError = uiState.taskError,
                        onInputChanged = { viewModel.onEvent(ChatViewEvent.InputChanged(it)) },
                        onSendClicked = { viewModel.onEvent(ChatViewEvent.SendClicked) },
                        onStopClicked = { viewModel.onEvent(ChatViewEvent.StopGeneration) },
                        onApprovePlan = { viewModel.onEvent(ChatViewEvent.ApprovePlan) },
                        onRejectPlan = { reason -> viewModel.onEvent(ChatViewEvent.RejectPlan(reason)) },
                        onApproveStep = { viewModel.onEvent(ChatViewEvent.ApproveStep) },
                        onApproveValidation = { viewModel.onEvent(ChatViewEvent.ApproveValidation) },
                        onPauseTask = { viewModel.onEvent(ChatViewEvent.PauseTask()) },
                        onResumeTask = { viewModel.onEvent(ChatViewEvent.ResumeTask) },
                        onCancelTask = { viewModel.onEvent(ChatViewEvent.CancelTask) },
                        modifier = Modifier.weight(chatPanelWeight)
                    )

                    Divider(
                        onDrag = { delta ->
                            val deltaWeight = delta / totalWidth
                            val newChatWeight = (chatPanelWeight + deltaWeight).coerceIn(0.40f, 0.60f)
                            val newProfileWeight = (profilePanelWeight - deltaWeight).coerceIn(0.20f, 0.35f)
                            if (logPanelWeight + newChatWeight + newProfileWeight <= 1f) {
                                chatPanelWeight = newChatWeight
                            }
                        }
                    )

                    // Right: Profile Panel
                    ProfilePanel(
                        memoryState = uiState.memoryState,
                        onUpdateProfile = { viewModel.onEvent(ChatViewEvent.UpdateProfile(it)) },
                        onAddToWorkingMemory = { label, content ->
                            viewModel.onEvent(ChatViewEvent.AddToWorkingMemory(label, content))
                        },
                        onRemoveFromWorkingMemory = { viewModel.onEvent(ChatViewEvent.RemoveFromWorkingMemory(it)) },
                        modifier = Modifier.weight(profilePanelWeight)
                    )
                }
            }
        }

        // Settings Dialog
        if (showSettingsDialog) {
            SettingsDialog(
                systemPrompt = uiState.systemPromptText,
                selectedModel = uiState.selectedModel,
                temperatureText = uiState.temperatureText,
                maxTokensText = uiState.maxTokensText,
                historyTokenLimitText = uiState.historyTokenLimitText,
                estimatedHistoryTokens = uiState.estimatedHistoryTokens,
                totalMessages = uiState.messages.size,
                sessionStats = uiState.sessionStats,
                onSystemPromptChanged = { viewModel.onEvent(ChatViewEvent.SystemPromptChanged(it)) },
                onModelChanged = { viewModel.onEvent(ChatViewEvent.ModelChanged(it)) },
                onTemperatureChanged = { viewModel.onEvent(ChatViewEvent.TemperatureChanged(it)) },
                onMaxTokensChanged = { viewModel.onEvent(ChatViewEvent.MaxTokensChanged(it)) },
                onHistoryTokenLimitChanged = { viewModel.onEvent(ChatViewEvent.HistoryTokenLimitChanged(it)) },
                onClearHistory = { viewModel.onEvent(ChatViewEvent.ClearHistory) },
                onClearAllMemory = { viewModel.onEvent(ChatViewEvent.ClearAllMemory) },
                onDismiss = { showSettingsDialog = false }
            )
        }

        // Memory Dialog
        if (showMemoryDialog) {
            MemoryDialog(
                memoryState = uiState.memoryState,
                onDismiss = { showMemoryDialog = false }
            )
        }
    }
}

@Composable
private fun AppHeader(
    onSettingsClick: () -> Unit,
    onMemoryClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.backgroundSecondary)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Crypto Consultant",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.textPrimary
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Memory",
                fontSize = 12.sp,
                color = AppTheme.accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onMemoryClick() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
            Text(
                text = "Settings",
                fontSize = 12.sp,
                color = AppTheme.accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onSettingsClick() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
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
