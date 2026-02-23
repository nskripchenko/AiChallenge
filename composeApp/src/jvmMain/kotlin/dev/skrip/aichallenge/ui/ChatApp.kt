package dev.skrip.aichallenge.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.skrip.aichallenge.ui.state.ChatViewEvent
import dev.skrip.aichallenge.ui.viewmodel.ChatViewModel

@Composable
fun ChatApp(viewModel: ChatViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val logs by viewModel.logs.collectAsState()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                LogPanel(
                    logs = logs,
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(
                    modifier = Modifier.fillMaxHeight().width(1.dp),
                    color = MaterialTheme.colorScheme.outline
                )
                ChatPanel(
                    messages = uiState.messages,
                    inputText = uiState.inputText,
                    isLoading = uiState.isLoading,
                    errorMessage = uiState.errorMessage,
                    onInputChanged = { viewModel.onEvent(ChatViewEvent.InputChanged(it)) },
                    onSendClicked = { viewModel.onEvent(ChatViewEvent.SendClicked) },
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(
                    modifier = Modifier.fillMaxHeight().width(1.dp),
                    color = MaterialTheme.colorScheme.outline
                )
                AgentSettingsPanel(
                    systemPrompt = uiState.systemPromptText,
                    selectedModel = uiState.selectedModel,
                    temperatureText = uiState.temperatureText,
                    maxTokensText = uiState.maxTokensText,
                    historyTokenLimitText = uiState.historyTokenLimitText,
                    estimatedHistoryTokens = uiState.estimatedHistoryTokens,
                    historyTokensRemaining = uiState.historyTokensRemaining,
                    onSystemPromptChanged = { viewModel.onEvent(ChatViewEvent.SystemPromptChanged(it)) },
                    onModelChanged = { viewModel.onEvent(ChatViewEvent.ModelChanged(it)) },
                    onTemperatureChanged = { viewModel.onEvent(ChatViewEvent.TemperatureChanged(it)) },
                    onMaxTokensChanged = { viewModel.onEvent(ChatViewEvent.MaxTokensChanged(it)) },
                    onHistoryTokenLimitChanged = { viewModel.onEvent(ChatViewEvent.HistoryTokenLimitChanged(it)) },
                    onClearHistory = { viewModel.onEvent(ChatViewEvent.ClearHistory) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
