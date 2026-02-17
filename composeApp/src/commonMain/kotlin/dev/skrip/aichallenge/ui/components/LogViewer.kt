package dev.skrip.aichallenge.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.skrip.aichallenge.domain.model.LogEntry
import dev.skrip.aichallenge.domain.model.LogEntryType
import dev.skrip.aichallenge.presentation.LogTab

@Composable
fun LogViewer(
    logs: List<LogEntry>,
    selectedTab: LogTab,
    selectedEntry: LogEntry?,
    onTabSelect: (LogTab) -> Unit,
    onEntrySelect: (LogEntry?) -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LogHeader(
                selectedTab = selectedTab,
                onTabSelect = onTabSelect,
                onClearLogs = onClearLogs
            )

            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LogList(
                    logs = filterLogs(logs, selectedTab),
                    selectedEntry = selectedEntry,
                    onEntrySelect = onEntrySelect,
                    modifier = Modifier.weight(1f)
                )

                if (selectedEntry != null) {
                    VerticalDivider()
                    LogDetails(
                        entry = selectedEntry,
                        onClose = { onEntrySelect(null) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LogHeader(
    selectedTab: LogTab,
    onTabSelect: (LogTab) -> Unit,
    onClearLogs: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PrimaryTabRow(
            selectedTabIndex = LogTab.entries.indexOf(selectedTab),
            modifier = Modifier.weight(1f)
        ) {
            LogTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { onTabSelect(tab) },
                    text = { Text(tab.displayName) }
                )
            }
        }

        TextButton(onClick = onClearLogs) {
            Text("Очистить")
        }
    }
}

@Composable
private fun LogList(
    logs: List<LogEntry>,
    selectedEntry: LogEntry?,
    onEntrySelect: (LogEntry?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (logs.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Нет записей",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(logs, key = { "${it.id}_${it.type}" }) { entry ->
                LogEntryItem(
                    entry = entry,
                    isSelected = selectedEntry?.id == entry.id && selectedEntry.type == entry.type,
                    onClick = { onEntrySelect(entry) }
                )
            }
        }
    }
}

@Composable
private fun LogEntryItem(
    entry: LogEntry,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        }
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.getFormattedTime(),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace
            )

            TypeBadge(type = entry.type)

            Text(
                text = entry.getPanelName(),
                style = MaterialTheme.typography.labelSmall
            )

            Text(
                text = entry.model.take(15),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            entry.latencyMs?.let {
                Text(
                    text = "${it}мс",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun TypeBadge(type: LogEntryType) {
    val color = when (type) {
        LogEntryType.REQUEST -> MaterialTheme.colorScheme.tertiary
        LogEntryType.RESPONSE -> MaterialTheme.colorScheme.primary
        LogEntryType.ERROR -> MaterialTheme.colorScheme.error
    }

    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = when (type) {
                LogEntryType.REQUEST -> "REQ"
                LogEntryType.RESPONSE -> "RES"
                LogEntryType.ERROR -> "ERR"
            },
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun LogDetails(
    entry: LogEntry,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Детали записи",
                style = MaterialTheme.typography.titleSmall
            )
            TextButton(onClick = onClose) {
                Text("×")
            }
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DetailRow("ID", entry.id)
            DetailRow("Время", entry.getFormattedDate())
            DetailRow("Тип", entry.getTypeName())
            DetailRow("Панель", entry.getPanelName())
            DetailRow("Модель", entry.model)

            entry.temperature?.let { DetailRow("Температура", it.formatDecimal()) }
            entry.maxTokens?.let { DetailRow("Max tokens", it.toString()) }

            if (entry.stopSequences.isNotEmpty()) {
                DetailRow("Stop sequences", entry.stopSequences.joinToString(", "))
            }

            DetailRow("Streaming", if (entry.streaming) "Да" else "Нет")
            entry.httpStatus?.let { DetailRow("HTTP статус", it.toString()) }
            entry.latencyMs?.let { DetailRow("Latency", "${it} мс") }
            entry.inputTokens?.let { DetailRow("Input tokens", it.toString()) }
            entry.outputTokens?.let { DetailRow("Output tokens", it.toString()) }

            entry.requestJson?.let {
                DetailSection("Запрос", it) {
                    clipboardManager.setText(AnnotatedString(it))
                }
            }

            entry.responseJson?.let {
                DetailSection("Ответ", it) {
                    clipboardManager.setText(AnnotatedString(it))
                }
            }

            entry.errorDetails?.let {
                DetailSection("Ошибка", it, isError = true) {
                    clipboardManager.setText(AnnotatedString(it))
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun DetailSection(
    label: String,
    content: String,
    isError: Boolean = false,
    onCopy: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onCopy) {
                Text("Копировать")
            }
        }
        Surface(
            shape = MaterialTheme.shapes.small,
            color = if (isError) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

private fun filterLogs(logs: List<LogEntry>, tab: LogTab): List<LogEntry> {
    return when (tab) {
        LogTab.REQUESTS -> logs.filter { it.type == LogEntryType.REQUEST }
        LogTab.RESPONSES -> logs.filter { it.type == LogEntryType.RESPONSE }
        LogTab.ERRORS -> logs.filter { it.type == LogEntryType.ERROR }
    }
}
