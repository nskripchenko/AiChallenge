package dev.skrip.aichallenge.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.skrip.aichallenge.presentation.MainViewModel
import dev.skrip.aichallenge.presentation.UiEvent
import dev.skrip.aichallenge.ui.components.*

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Row(
        modifier = modifier.fillMaxSize().padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Левая часть: промпт, ответы, логи
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Верхняя панель ввода
            PromptPanel(
                promptText = state.promptText,
                onPromptChange = { viewModel.onEvent(UiEvent.UpdatePrompt(it)) },
                sendToBoth = state.sendToBoth,
                onSendToBothChange = { viewModel.onEvent(UiEvent.ToggleSendToBoth(it)) },
                onSend = { viewModel.onEvent(UiEvent.SendClicked) },
                onClear = { viewModel.onEvent(UiEvent.ClearClicked) },
                onCancel = { viewModel.onEvent(UiEvent.CancelRequest) },
                rawStatus = state.rawStatus,
                controlledStatus = state.controlledStatus,
                promptHistory = state.promptHistory,
                onSelectFromHistory = { viewModel.onEvent(UiEvent.SelectFromHistory(it)) },
                isMultiTurnEnabled = state.isMultiTurnEnabled,
                onMultiTurnToggle = { viewModel.onEvent(UiEvent.ToggleMultiTurn(it)) },
                conversationCount = state.conversationHistory.size / 2
            )

            // Ответы
            ResponseCards(
                state = state,
                modifier = Modifier.weight(1f)
            )

            // Нижняя область: логи
            LogViewer(
                logs = state.logs,
                selectedTab = state.selectedLogTab,
                selectedEntry = state.selectedLogEntry,
                onTabSelect = { viewModel.onEvent(UiEvent.SelectLogTab(it)) },
                onEntrySelect = { viewModel.onEvent(UiEvent.SelectLogEntry(it)) },
                onClearLogs = { viewModel.onEvent(UiEvent.ClearLogs) },
                modifier = Modifier.height(200.dp)
            )
        }

        // Правая колонка: настройки
        SettingsPanel(
            settings = state.controlledSettings,
            systemPrompt = state.systemPrompt,
            sessionStats = state.sessionStats,
            onSystemPromptChange = { viewModel.onEvent(UiEvent.UpdateSystemPrompt(it)) },
            onModelChange = { viewModel.onEvent(UiEvent.UpdateModel(it)) },
            onTemperatureChange = { viewModel.onEvent(UiEvent.UpdateTemperature(it)) },
            onTopPChange = { viewModel.onEvent(UiEvent.UpdateTopP(it)) },
            onMaxTokensChange = { viewModel.onEvent(UiEvent.UpdateMaxTokens(it)) },
            onAddStopSequence = { viewModel.onEvent(UiEvent.AddStopSequence(it)) },
            onRemoveStopSequence = { viewModel.onEvent(UiEvent.RemoveStopSequence(it)) },
            onStreamingChange = { viewModel.onEvent(UiEvent.ToggleStreaming(it)) },
            modifier = Modifier.width(300.dp).fillMaxHeight()
        )
    }
}

@Composable
private fun ResponseCards(
    state: dev.skrip.aichallenge.presentation.UiState,
    modifier: Modifier = Modifier
) {
    // Raw settings info - показываем понятно
    val rawSettingsInfo = "temp=${state.rawSettings.temperature.formatDecimal()}, max=${state.rawSettings.maxTokens}, без stop-seq"

    val controlledSettingsInfo = buildString {
        append("temp=${state.controlledSettings.temperature.formatDecimal()}")
        append(", max=${state.controlledSettings.maxTokens}")
        if (state.controlledSettings.stopSequences.isNotEmpty()) {
            append(", stops: [${state.controlledSettings.stopSequences.joinToString()}]")
        } else {
            append(", stops: нет")
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ResponseCard(
            title = "Без ограничений",
            settingsInfo = rawSettingsInfo,
            result = state.rawResult,
            status = state.rawStatus,
            modifier = Modifier.weight(1f)
        )

        ResponseCard(
            title = "С контролем",
            settingsInfo = controlledSettingsInfo,
            result = state.controlledResult,
            status = state.controlledStatus,
            modifier = Modifier.weight(1f)
        )
    }
}
