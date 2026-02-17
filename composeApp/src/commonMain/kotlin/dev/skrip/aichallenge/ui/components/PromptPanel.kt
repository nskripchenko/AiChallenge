package dev.skrip.aichallenge.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.skrip.aichallenge.domain.model.RequestStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptPanel(
    promptText: String,
    onPromptChange: (String) -> Unit,
    sendToBoth: Boolean,
    onSendToBothChange: (Boolean) -> Unit,
    onSend: () -> Unit,
    onClear: () -> Unit,
    onCancel: () -> Unit,
    rawStatus: RequestStatus,
    controlledStatus: RequestStatus,
    promptHistory: List<String>,
    onSelectFromHistory: (String) -> Unit,
    isMultiTurnEnabled: Boolean,
    onMultiTurnToggle: (Boolean) -> Unit,
    conversationCount: Int,
    modifier: Modifier = Modifier
) {
    val isLoading = rawStatus == RequestStatus.SENDING ||
            rawStatus == RequestStatus.STREAMING ||
            controlledStatus == RequestStatus.SENDING ||
            controlledStatus == RequestStatus.STREAMING

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Поле ввода с историей
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = promptText,
                    onValueChange = onPromptChange,
                    modifier = Modifier.weight(1f).heightIn(min = 60.dp, max = 120.dp),
                    placeholder = { Text("Введите запрос...", style = MaterialTheme.typography.bodySmall) },
                    textStyle = MaterialTheme.typography.bodySmall,
                    minLines = 2,
                    maxLines = 4
                )

                if (promptHistory.isNotEmpty()) {
                    HistoryDropdown(
                        history = promptHistory,
                        onSelect = onSelectFromHistory
                    )
                }
            }

            // Управление
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Левая часть - чекбоксы
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = sendToBoth,
                            onCheckedChange = onSendToBothChange,
                            modifier = Modifier.size(28.dp)
                        )
                        Text("Оба", style = MaterialTheme.typography.labelSmall)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isMultiTurnEnabled,
                            onCheckedChange = onMultiTurnToggle,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = if (conversationCount > 0) "Multi ($conversationCount)" else "Multi",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    // Статусы
                    StatusBadges(rawStatus, controlledStatus)
                }

                // Правая часть - кнопки
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onClear, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text("Очистить", style = MaterialTheme.typography.labelSmall)
                    }

                    if (isLoading) {
                        TextButton(
                            onClick = onCancel,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Отмена", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Button(
                        onClick = onSend,
                        enabled = !isLoading,
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text("Отправить", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryDropdown(
    history: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        FilledTonalButton(
            onClick = { expanded = true },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Text("История", style = MaterialTheme.typography.labelSmall)
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            history.take(10).forEach { prompt ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = prompt.take(40) + if (prompt.length > 40) "..." else "",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    onClick = {
                        onSelect(prompt)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun StatusBadges(
    rawStatus: RequestStatus,
    controlledStatus: RequestStatus
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (rawStatus != RequestStatus.IDLE) {
            StatusBadge("RAW", rawStatus)
        }
        if (controlledStatus != RequestStatus.IDLE) {
            StatusBadge("CTRL", controlledStatus)
        }
    }
}

@Composable
private fun StatusBadge(label: String, status: RequestStatus) {
    val color = when (status) {
        RequestStatus.IDLE -> MaterialTheme.colorScheme.primary
        RequestStatus.SENDING -> MaterialTheme.colorScheme.tertiary
        RequestStatus.STREAMING -> MaterialTheme.colorScheme.secondary
        RequestStatus.ERROR -> MaterialTheme.colorScheme.error
    }

    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = "$label: ${status.displayName}",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
