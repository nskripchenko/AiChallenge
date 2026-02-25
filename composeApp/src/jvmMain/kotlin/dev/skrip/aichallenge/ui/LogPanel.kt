package dev.skrip.aichallenge.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.skrip.aichallenge.logging.LogEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Design System LogColors
private object LogColors {
    val background = Color.White
    val backgroundSecondary = Color(0xFFFAFAFA)

    val textPrimary = Color(0xFF0A0A0A)
    val textSecondary = Color(0xFF404040)
    val textTertiary = Color(0xFF737373)
    val textMuted = Color(0xFFA3A3A3)

    val border = Color(0xFFE5E5E5)
}

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
            .background(LogColors.backgroundSecondary)
            .padding(24.dp)
    ) {
        Text(
            text = "Log",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = LogColors.textPrimary,
            letterSpacing = (-0.02).sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(logs, key = { it.timestamp }) { entry ->
                LogEntryCard(entry)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun LogEntryCard(entry: LogEntry) {
    var expanded by remember { mutableStateOf(false) }
    val style = entry.toStyle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(LogColors.background)
            .border(1.dp, LogColors.border, RoundedCornerShape(8.dp))
            .clickable { expanded = !expanded }
            .padding(12.dp)
    ) {
        // Header row
        Row {
            Text(
                text = style.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = LogColors.textPrimary
            )
            Text(
                text = " · ",
                fontSize = 12.sp,
                color = LogColors.textMuted
            )
            Text(
                text = formatTimestamp(entry.timestamp),
                fontSize = 12.sp,
                color = LogColors.textMuted
            )
            Text(
                text = if (expanded) " ↑" else " ↓",
                fontSize = 12.sp,
                color = LogColors.textMuted
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Summary
        Text(
            text = style.summary,
            fontSize = 13.sp,
            color = LogColors.textTertiary
        )

        // Expanded content
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    text = style.content,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = LogColors.textSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .background(LogColors.backgroundSecondary, RoundedCornerShape(6.dp))
                        .padding(12.dp)
                )
            }
        }
    }
}

private data class LogEntryStyle(
    val label: String,
    val summary: String,
    val content: String
)

private fun LogEntry.toStyle(): LogEntryStyle = when (this) {
    is LogEntry.Request -> LogEntryStyle(
        label = "REQUEST",
        summary = "$model · temp $temperature · max $maxTokens",
        content = requestJson
    )
    is LogEntry.Response -> LogEntryStyle(
        label = "RESPONSE",
        summary = "Response received",
        content = responseJson
    )
    is LogEntry.Error -> LogEntryStyle(
        label = "ERROR",
        summary = errorMessage,
        content = details ?: "No details"
    )
}

private fun formatTimestamp(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}
