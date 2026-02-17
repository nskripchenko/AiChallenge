package dev.skrip.aichallenge.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.skrip.aichallenge.domain.model.AvailableModels
import dev.skrip.aichallenge.presentation.ControlledSettings
import dev.skrip.aichallenge.presentation.SessionStats

@Composable
fun SettingsPanel(
    settings: ControlledSettings,
    systemPrompt: String,
    sessionStats: SessionStats,
    onSystemPromptChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onTemperatureChange: (Float) -> Unit,
    onTopPChange: (Float?) -> Unit,
    onMaxTokensChange: (Int) -> Unit,
    onAddStopSequence: (String) -> Unit,
    onRemoveStopSequence: (String) -> Unit,
    onStreamingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Статистика
            SessionStatsRow(sessionStats)

            HorizontalDivider()

            // System Prompt
            SystemPromptSection(systemPrompt, onSystemPromptChange)

            HorizontalDivider()

            // Заголовок настроек "С контролем"
            Text(
                text = "Настройки \"С контролем\"",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            // Модель
            ModelSection(settings.model, onModelChange)

            // Температура и Top-P в одном ряду
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TemperatureSlider(
                    value = settings.temperature,
                    onChange = onTemperatureChange,
                    modifier = Modifier.weight(1f)
                )
            }

            TopPSection(settings.topP, onTopPChange)

            // Max tokens
            MaxTokensField(settings.maxTokens, onMaxTokensChange)

            // Stop sequences
            StopSequencesSection(
                sequences = settings.stopSequences,
                onAdd = onAddStopSequence,
                onRemove = onRemoveStopSequence
            )

            // Streaming
            StreamingSwitch(settings.streaming, onStreamingChange)
        }
    }
}

@Composable
private fun SessionStatsRow(stats: SessionStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Запросов: ${stats.totalRequests}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "In: ${stats.totalInputTokens} | Out: ${stats.totalOutputTokens}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$${stats.totalCost.formatCost()}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SystemPromptSection(value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().height(100.dp),
            label = { Text("System prompt (инструкции для модели)") },
            placeholder = { Text("Например: Отвечай кратко, не более 30 слов", style = MaterialTheme.typography.bodySmall) },
            textStyle = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Только для панели \"С контролем\"",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelSection(model: String, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val modelInfo = AvailableModels.getInfo(model)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = modelInfo?.displayName ?: model,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            label = { Text("Модель", style = MaterialTheme.typography.labelSmall) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            textStyle = MaterialTheme.typography.bodySmall
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AvailableModels.modelsInfo.forEach { info ->
                DropdownMenuItem(
                    text = {
                        Text(
                            "${info.displayName} (${info.contextWindow / 1000}K)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    onClick = {
                        onChange(info.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TemperatureSlider(
    value: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Температура", style = MaterialTheme.typography.labelSmall)
            Text(
                value.formatDecimal(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TopPSection(value: Float?, onChange: (Float?) -> Unit) {
    var isEnabled by remember { mutableStateOf(value != null) }
    var sliderValue by remember(value) { mutableStateOf(value ?: 0.9f) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Top-P", style = MaterialTheme.typography.labelSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isEnabled) {
                Text(
                    sliderValue.formatDecimal(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Checkbox(
                checked = isEnabled,
                onCheckedChange = {
                    isEnabled = it
                    onChange(if (it) sliderValue else null)
                },
                modifier = Modifier.size(32.dp)
            )
        }
    }

    if (isEnabled) {
        Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                onChange(it)
            },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MaxTokensField(value: Int, onChange: (Int) -> Unit) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }

    OutlinedTextField(
        value = textValue,
        onValueChange = { newValue ->
            textValue = newValue
            newValue.toIntOrNull()?.let { onChange(it) }
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Max tokens", style = MaterialTheme.typography.labelSmall) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun StopSequencesSection(
    sequences: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var newSequence by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Stop sequences", style = MaterialTheme.typography.labelSmall)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newSequence,
                onValueChange = { newSequence = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("...", style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )
            FilledTonalButton(
                onClick = {
                    if (newSequence.isNotBlank()) {
                        onAdd(newSequence)
                        newSequence = ""
                    }
                },
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text("+")
            }
        }

        if (sequences.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                sequences.forEach { seq ->
                    InputChip(
                        selected = true,
                        onClick = { onRemove(seq) },
                        label = { Text(seq, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamingSwitch(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Стриминг", style = MaterialTheme.typography.labelSmall)
        Switch(checked = enabled, onCheckedChange = onChange)
    }
}

private fun Double.formatCost(): String {
    return if (this < 0.01) "%.4f".format(this) else "%.2f".format(this)
}
