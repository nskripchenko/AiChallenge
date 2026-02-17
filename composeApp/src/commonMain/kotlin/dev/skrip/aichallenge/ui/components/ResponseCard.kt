package dev.skrip.aichallenge.ui.components

import androidx.compose.foundation.layout.*
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
import dev.skrip.aichallenge.domain.model.LlmResult
import dev.skrip.aichallenge.domain.model.RequestStatus

@Composable
fun ResponseCard(
    title: String,
    settingsInfo: String,
    result: LlmResult,
    status: RequestStatus,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header with title and settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = settingsInfo,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Latency - показываем только здесь
                    if (result.latencyMs > 0) {
                        Text(
                            text = "${result.latencyMs}ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (result.text.isNotEmpty()) {
                        TextButton(
                            onClick = { clipboardManager.setText(AnnotatedString(result.text)) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Copy", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (status == RequestStatus.STREAMING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            // Content
            if (result.error != null) {
                ErrorContent(error = result.error)
            } else if (result.text.isNotEmpty() || status == RequestStatus.STREAMING) {
                ResponseContent(
                    text = result.text,
                    isStreaming = status == RequestStatus.STREAMING,
                    modifier = Modifier.weight(1f)
                )
            } else {
                EmptyContent(modifier = Modifier.weight(1f))
            }

            // Tokens info (compact)
            if (result.text.isNotEmpty() && result.error == null) {
                TokensInfo(
                    model = result.model,
                    inputTokens = result.inputTokens,
                    outputTokens = result.outputTokens
                )
            }
        }
    }
}

@Composable
private fun TokensInfo(
    model: String,
    inputTokens: Int?,
    outputTokens: Int?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = model.take(15),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        inputTokens?.let {
            Text(
                text = "in:$it",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        outputTokens?.let {
            Text(
                text = "out:$it",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ResponseContent(
    text: String,
    isStreaming: Boolean,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val hasCodeBlocks = text.contains("```")
    val displayText = if (isStreaming && text.isEmpty()) {
        AnnotatedString("...")
    } else if (hasCodeBlocks) {
        CodeHighlighter.parseAndHighlight(text)
    } else {
        AnnotatedString(text)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier
                .padding(8.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Default
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "Ответ появится здесь",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
