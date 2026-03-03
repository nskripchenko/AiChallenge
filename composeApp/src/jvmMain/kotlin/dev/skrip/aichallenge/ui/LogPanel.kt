package dev.skrip.aichallenge.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
            .background(AppTheme.backgroundSecondary)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "API Logs",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.textPrimary
            )
            if (logs.isNotEmpty()) {
                Text(
                    text = "${logs.size}",
                    fontSize = 12.sp,
                    color = AppTheme.textMuted
                )
            }
        }

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No API calls yet",
                    fontSize = 13.sp,
                    color = AppTheme.textMuted
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(logs, key = { it.id }) { entry ->
                    LogEntryItem(entry)
                }
            }
        }
    }
}

@Composable
private fun LogEntryItem(entry: LogEntry) {
    var expanded by remember { mutableStateOf(false) }
    val style = entry.toStyle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(AppTheme.background)
            .border(1.dp, AppTheme.border, RoundedCornerShape(6.dp))
            .clickable { expanded = !expanded }
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(style.indicatorColor)
                )
                Text(
                    text = style.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppTheme.textPrimary
                )
            }
            Text(
                text = formatTime(entry.timestamp),
                fontSize = 11.sp,
                color = AppTheme.textMuted
            )
        }

        // Preview
        Text(
            text = style.preview,
            fontSize = 11.sp,
            color = AppTheme.textTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )

        // Expanded content
        AnimatedVisibility(visible = expanded) {
            Text(
                text = style.content,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                color = AppTheme.textSecondary,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .background(AppTheme.backgroundSecondary, RoundedCornerShape(4.dp))
                    .padding(8.dp)
            )
        }
    }
}

private data class LogStyle(
    val label: String,
    val preview: String,
    val content: String,
    val indicatorColor: Color
)

private fun LogEntry.toStyle(): LogStyle = when (this) {
    is LogEntry.Request -> LogStyle(
        label = "REQ",
        preview = "$model · $maxTokens tokens",
        content = requestJson,
        indicatorColor = Color(0xFF3B82F6) // blue
    )
    is LogEntry.Response -> LogStyle(
        label = "RES",
        preview = "Response received",
        content = responseJson,
        indicatorColor = Color(0xFF22C55E) // green
    )
    is LogEntry.Error -> LogStyle(
        label = "ERR",
        preview = errorMessage,
        content = details ?: errorMessage,
        indicatorColor = Color(0xFFEF4444) // red
    )
}

private fun formatTime(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}
