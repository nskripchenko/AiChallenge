package dev.skrip.aichallenge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.skrip.aichallenge.model.ChatMessage
import dev.skrip.aichallenge.model.ChunkingStrategy
import dev.skrip.aichallenge.model.MessageRole
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel { ChatViewModel() }
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar with status
        TopBar(
            state = state,
            onStrategyChange = { viewModel.switchStrategy(it) },
            onReindex = { viewModel.reindex() },
            onClear = { viewModel.clearMessages() }
        )

        // Error banner
        state.error?.let { error ->
            ErrorBanner(
                message = error,
                onDismiss = { viewModel.clearError() }
            )
        }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (state.messages.isEmpty()) {
                item {
                    EmptyState(ollamaAvailable = state.ollamaAvailable)
                }
            }

            items(state.messages, key = { it.id }) { message ->
                MessageBubble(message = message)
            }

            if (state.isLoading) {
                item {
                    LoadingIndicator()
                }
            }
        }

        // Input area
        InputArea(
            enabled = !state.isLoading && state.ollamaAvailable && state.indexStatus != null,
            onSend = { question ->
                viewModel.ask(question)
                coroutineScope.launch {
                    listState.animateScrollToItem(state.messages.size)
                }
            }
        )
    }
}

@Composable
private fun TopBar(
    state: ChatState,
    onStrategyChange: (ChunkingStrategy) -> Unit,
    onReindex: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RAG Demo",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Ollama status
                    StatusChip(
                        label = if (state.ollamaAvailable) "Ollama OK" else "Ollama Offline",
                        color = if (state.ollamaAvailable) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Index status and controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Index info
                Column {
                    if (state.indexStatus?.isIndexing == true) {
                        Text(
                            text = state.indexStatus.indexingProgress ?: "Indexing...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (state.indexStatus != null) {
                        Text(
                            text = "${state.indexStatus.documentCount} docs, ${state.indexStatus.chunkCount} chunks",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = "No index loaded",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Strategy switcher
                    StrategySwitch(
                        currentStrategy = state.currentStrategy,
                        onStrategyChange = onStrategyChange,
                        enabled = state.indexStatus?.isIndexing != true
                    )

                    // Reindex button
                    Button(
                        onClick = onReindex,
                        enabled = state.ollamaAvailable && state.indexStatus?.isIndexing != true,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Reindex")
                    }

                    // Clear button
                    OutlinedButton(onClick = onClear) {
                        Text("Clear")
                    }
                }
            }
        }
    }
}

@Composable
private fun StrategySwitch(
    currentStrategy: ChunkingStrategy,
    onStrategyChange: (ChunkingStrategy) -> Unit,
    enabled: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ChunkingStrategy.entries.forEach { strategy ->
            FilterChip(
                selected = currentStrategy == strategy,
                onClick = { onStrategyChange(strategy) },
                label = { Text(strategy.name.lowercase().replaceFirstChar { it.uppercase() }) },
                enabled = enabled
            )
        }
    }
}

@Composable
private fun StatusChip(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}

@Composable
private fun EmptyState(ollamaAvailable: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "RAG Document Search",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (ollamaAvailable) {
                "Ask questions about your documents.\nClick 'Reindex' to build the search index."
            } else {
                "Please start Ollama to use this demo:\nollama serve"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    val backgroundColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = backgroundColor,
            modifier = Modifier.widthIn(max = 600.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Sources for assistant messages
                if (!isUser && message.sources.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sources:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    message.sources.forEach { source ->
                        SourceItem(
                            file = source.chunk.metadata.file,
                            section = source.chunk.metadata.section ?: "unknown",
                            similarity = source.similarity
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceItem(file: String, section: String, similarity: Float) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "%.0f%%".format(similarity * 100),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$file / $section",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun LoadingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Thinking...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun InputArea(
    enabled: Boolean,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .weight(1f)
                    .onKeyEvent { event ->
                        if (event.key == Key.Enter && event.type == KeyEventType.KeyUp && !event.isShiftPressed) {
                            if (text.isNotBlank() && enabled) {
                                onSend(text.trim())
                                text = ""
                            }
                            true
                        } else {
                            false
                        }
                    },
                placeholder = { Text("Ask a question about your documents...") },
                enabled = enabled,
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )

            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onSend(text.trim())
                        text = ""
                    }
                },
                enabled = enabled && text.isNotBlank(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Ask")
            }
        }
    }
}
