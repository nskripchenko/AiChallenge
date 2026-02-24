package dev.skrip.aichallenge.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.skrip.aichallenge.logging.LogEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogPanel(
    logs: List<LogEntry>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp)
    ) {
        Text(
            text = "Log",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(logs, key = { it.timestamp }) { entry ->
                LogEntryCard(entry)
            }
        }
    }
}

@Composable
private fun LogEntryCard(entry: LogEntry) {
    var expanded by remember { mutableStateOf(false) }
    val style = entry.toStyle()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = style.backgroundColor)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row {
                Text(
                    text = formatTimestamp(entry.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    text = " | ",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    text = style.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = style.labelColor
                )
                Text(
                    text = if (expanded) " ▼" else " ▶",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
            }

            Text(
                text = style.summary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = style.content,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = style.contentColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .background(Color(0xFFF5F5F5))
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

private data class LogEntryStyle(
    val backgroundColor: Color,
    val label: String,
    val labelColor: Color,
    val summary: String,
    val content: String,
    val contentColor: Color = Color.Black
)

private fun LogEntry.toStyle(): LogEntryStyle = when (this) {
    is LogEntry.Request -> LogEntryStyle(
        backgroundColor = Color(0xFFF5F5F5),
        label = "REQUEST",
        labelColor = Color(0xFF424242),
        summary = "Model: $model | Temp: $temperature | MaxTokens: $maxTokens",
        content = requestJson
    )
    is LogEntry.Response -> LogEntryStyle(
        backgroundColor = Color(0xFFEEEEEE),
        label = "RESPONSE",
        labelColor = Color(0xFF616161),
        summary = "Response received",
        content = responseJson
    )
    is LogEntry.Error -> LogEntryStyle(
        backgroundColor = Color(0xFFE0E0E0),
        label = "ERROR",
        labelColor = Color(0xFF212121),
        summary = errorMessage,
        content = details ?: "No additional details",
        contentColor = Color(0xFF424242)
    )
}

private fun formatTimestamp(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}
