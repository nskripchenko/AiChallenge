package dev.skrip.aichallenge.ui.temperature

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.skrip.aichallenge.model.AnthropicModel
import dev.skrip.aichallenge.model.RequestLogEntry
import dev.skrip.aichallenge.model.ResponseLogEntry
import dev.skrip.aichallenge.model.TemperatureResult
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
fun TemperaturePlaygroundScreen(viewModel: TemperaturePlaygroundViewModel) {
    val state by viewModel.state.collectAsState()

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Header
                Text(
                    text = "Temperature Playground",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Error message
                state.errorMessage?.let { error ->
                    ErrorCard(error) { viewModel.clearError() }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Main content: Left panel + Right panel
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left panel: Prompt + Logs
                    LeftPanel(
                        prompt = state.prompt,
                        selectedModel = state.selectedModel,
                        isRunning = state.isRunning,
                        requestLog = state.requestLog,
                        responseLog = state.responseLog,
                        onPromptChange = viewModel::updatePrompt,
                        onModelChange = viewModel::selectModel,
                        onSend = viewModel::send,
                        modifier = Modifier.weight(0.35f)
                    )

                    // Right panel: 3 result cards
                    RightPanel(
                        results = state.results,
                        temperatures = state.temperatures,
                        onTemperatureChange = viewModel::updateTemperature,
                        modifier = Modifier.weight(0.65f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(error: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    }
}

@Composable
private fun LeftPanel(
    prompt: String,
    selectedModel: AnthropicModel,
    isRunning: Boolean,
    requestLog: List<RequestLogEntry>,
    responseLog: List<ResponseLogEntry>,
    onPromptChange: (String) -> Unit,
    onModelChange: (AnthropicModel) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Model selector
        ModelSelector(selectedModel, onModelChange)

        // Prompt input
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier.fillMaxWidth().weight(0.3f),
            label = { Text("Промпт") },
            placeholder = { Text("Введите промпт для сравнения...") }
        )

        // Send button
        Button(
            onClick = onSend,
            enabled = !isRunning,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isRunning) "Отправка..." else "Отправить")
        }

        // Request log
        LogSection(
            title = "Лог запросов",
            modifier = Modifier.weight(0.35f)
        ) {
            requestLog.forEach { entry ->
                Text(
                    text = "[${entry.timestamp}] t=${entry.temperature} → ${entry.promptPreview}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (requestLog.isEmpty()) {
                Text(
                    text = "Пусто",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        // Response log
        LogSection(
            title = "Лог ответов",
            modifier = Modifier.weight(0.35f)
        ) {
            responseLog.forEach { entry ->
                val statusText = if (entry.isError) "ошибка" else "${entry.lengthChars} симв."
                Text(
                    text = "[${entry.timestamp}] t=${entry.temperature} ← $statusText",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (entry.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
            if (responseLog.isEmpty()) {
                Text(
                    text = "Пусто",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun ModelSelector(
    selectedModel: AnthropicModel,
    onModelChange: (AnthropicModel) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Модель: ", style = MaterialTheme.typography.bodyMedium)
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text(selectedModel.displayName)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                AnthropicModel.entries.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model.displayName) },
                        onClick = {
                            onModelChange(model)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LogSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                content = content
            )
        }
    }
}

@Composable
private fun RightPanel(
    results: List<TemperatureResult>,
    temperatures: List<Double>,
    onTemperatureChange: (Int, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxHeight(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        results.forEachIndexed { index, result ->
            ResultCard(
                result = result,
                temperature = temperatures[index],
                onTemperatureChange = { onTemperatureChange(index, it) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ResultCard(
    result: TemperatureResult,
    temperature: Double,
    onTemperatureChange: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            // Temperature input
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Температура",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = "%.1f".format(temperature),
                    onValueChange = { text ->
                        text.toDoubleOrNull()?.let { onTemperatureChange(it) }
                    },
                    modifier = Modifier.width(80.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Slider
            Slider(
                value = temperature.toFloat(),
                onValueChange = { onTemperatureChange(it.toDouble()) },
                valueRange = 0f..1.5f,
                steps = 14,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (result.durationMs > 0) {
                    Text("${result.durationMs} мс", style = MaterialTheme.typography.bodySmall)
                }
                if (result.length > 0) {
                    Text("${result.length} симв.", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Response area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        if (result.error != null) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp)
                    )
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                when {
                    result.isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    }
                    result.error != null -> {
                        SelectionContainer {
                            Text(
                                text = "Ошибка: ${result.error}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            )
                        }
                    }
                    result.text.isNotEmpty() -> {
                        SelectionContainer {
                            Text(
                                text = result.text,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = "Ответ появится здесь...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Copy button
            Button(
                onClick = { copyToClipboard(result.text) },
                enabled = result.text.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Копировать")
            }
        }
    }
}

private fun copyToClipboard(text: String) {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    clipboard.setContents(StringSelection(text), null)
}
