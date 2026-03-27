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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.skrip.aichallenge.evaluation.EvaluationQuestions
import dev.skrip.aichallenge.model.AnswerQuote
import dev.skrip.aichallenge.model.ChatMessage
import dev.skrip.aichallenge.model.ChunkingStrategy
import dev.skrip.aichallenge.model.MessageRole
import dev.skrip.aichallenge.model.QuestionMode
import dev.skrip.aichallenge.model.RetrievalMode
import dev.skrip.aichallenge.model.TaskState
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
            onModeChange = { viewModel.switchQuestionMode(it) },
            onRetrievalModeChange = { viewModel.switchRetrievalMode(it) },
            onGroundedModeChange = { viewModel.toggleGroundedMode(it) },
            onMemoryModeChange = { viewModel.toggleMemoryMode(it) },
            onReindex = { viewModel.reindex() },
            onClear = { viewModel.clearMessages() }
        )

        // Day 25: Task State panel
        if (state.memoryEnabled && !state.taskState.isEmpty()) {
            TaskStatePanel(taskState = state.taskState)
        }

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

        // Input area - PLAIN mode doesn't require index
        val inputEnabled = !state.isLoading && state.ollamaAvailable &&
            (state.currentQuestionMode == QuestionMode.PLAIN || state.indexStatus != null)

        InputArea(
            enabled = inputEnabled,
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
    onModeChange: (QuestionMode) -> Unit,
    onRetrievalModeChange: (RetrievalMode) -> Unit,
    onGroundedModeChange: (Boolean) -> Unit,
    onMemoryModeChange: (Boolean) -> Unit,
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
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RAG Demo - Day 30",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Server: ${dev.skrip.aichallenge.model.Config.OLLAMA_BASE_URL}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(
                        label = if (state.ollamaAvailable) "Ollama OK" else "Ollama Offline",
                        color = if (state.ollamaAvailable) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mode switcher row (Plain vs RAG)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Mode:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    ModeSwitch(
                        currentMode = state.currentQuestionMode,
                        onModeChange = onModeChange
                    )
                }

                // Index info
                if (state.indexStatus?.isIndexing == true) {
                    Text(
                        text = state.indexStatus.indexingProgress ?: "Indexing...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (state.indexStatus != null) {
                    Text(
                        text = "${state.indexStatus.documentCount} docs, ${state.indexStatus.chunkCount} chunks",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = "No index",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // RAG-specific controls
            if (state.currentQuestionMode == QuestionMode.RAG) {
                Spacer(modifier = Modifier.height(8.dp))

                // Retrieval mode row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Retrieval:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        RetrievalModeSwitch(
                            currentMode = state.currentRetrievalMode,
                            onModeChange = onRetrievalModeChange,
                            enabled = state.indexStatus?.isIndexing != true
                        )
                    }

                    // Grounded mode toggle (Day 24)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Grounded:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = state.groundedMode,
                            onCheckedChange = onGroundedModeChange,
                            enabled = state.indexStatus?.isIndexing != true,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF4CAF50),
                                checkedTrackColor = Color(0xFF4CAF50).copy(alpha = 0.5f)
                            )
                        )
                    }

                    // Memory mode toggle (Day 25)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Memory:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = state.memoryEnabled,
                            onCheckedChange = onMemoryModeChange,
                            enabled = state.indexStatus?.isIndexing != true,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF673AB7),
                                checkedTrackColor = Color(0xFF673AB7).copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Strategy and buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Chunking:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        StrategySwitch(
                            currentStrategy = state.currentStrategy,
                            onStrategyChange = onStrategyChange,
                            enabled = state.indexStatus?.isIndexing != true
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onReindex,
                            enabled = state.ollamaAvailable && state.indexStatus?.isIndexing != true,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("Reindex")
                        }

                        OutlinedButton(onClick = onClear) {
                            Text("New Chat")
                        }
                    }
                }
            } else {
                // Plain mode - show memory toggle and buttons
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Direct LLM (no retrieval)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )

                        // Memory toggle for Plain mode (Day 25)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Memory:",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Switch(
                                checked = state.memoryEnabled,
                                onCheckedChange = onMemoryModeChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF673AB7),
                                    checkedTrackColor = Color(0xFF673AB7).copy(alpha = 0.5f)
                                )
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onReindex,
                            enabled = state.ollamaAvailable && state.indexStatus?.isIndexing != true,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("Reindex")
                        }

                        OutlinedButton(onClick = onClear) {
                            Text("New Chat")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeSwitch(
    currentMode: QuestionMode,
    onModeChange: (QuestionMode) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        QuestionMode.entries.forEach { mode ->
            val label = when (mode) {
                QuestionMode.PLAIN -> "Plain"
                QuestionMode.RAG -> "RAG"
            }
            val color = when (mode) {
                QuestionMode.PLAIN -> Color(0xFFFF9800)
                QuestionMode.RAG -> Color(0xFF4CAF50)
            }
            FilterChip(
                selected = currentMode == mode,
                onClick = { onModeChange(mode) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color.copy(alpha = 0.2f),
                    selectedLabelColor = color
                )
            )
        }
    }
}

@Composable
private fun RetrievalModeSwitch(
    currentMode: RetrievalMode,
    onModeChange: (RetrievalMode) -> Unit,
    enabled: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        RetrievalMode.entries.forEach { mode ->
            val label = when (mode) {
                RetrievalMode.BASELINE -> "Baseline"
                RetrievalMode.FILTERED -> "Filtered"
                RetrievalMode.REWRITE_FILTERED -> "Rewrite"
            }
            val color = when (mode) {
                RetrievalMode.BASELINE -> Color(0xFF9E9E9E)
                RetrievalMode.FILTERED -> Color(0xFF2196F3)
                RetrievalMode.REWRITE_FILTERED -> Color(0xFF9C27B0)
            }
            FilterChip(
                selected = currentMode == mode,
                onClick = { onModeChange(mode) },
                label = { Text(label, fontSize = 12.sp) },
                enabled = enabled,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color.copy(alpha = 0.2f),
                    selectedLabelColor = color
                )
            )
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
                label = { Text(strategy.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 12.sp) },
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
                "Ask questions about your documents.\nClick 'Reindex' to build the search index.\n\nDay 25: Multi-turn chat with memory & task state tracking!"
            } else {
                "Please start Ollama to use this demo:\nollama serve"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

/** Day 25: Task State Panel */
@Composable
private fun TaskStatePanel(taskState: TaskState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF673AB7).copy(alpha = 0.05f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Task State",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF673AB7)
                )
                if (taskState.goal != null) {
                    Text(
                        text = "|",
                        color = Color(0xFF673AB7).copy(alpha = 0.3f)
                    )
                    Text(
                        text = taskState.goal,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF673AB7).copy(alpha = 0.8f)
                    )
                }
            }

            if (taskState.clarifications.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Clarifications: ${taskState.clarifications.takeLast(2).joinToString(" | ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            if (taskState.constraints.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Constraints: ${taskState.constraints.joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            if (taskState.discoveredFacts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Facts: ${taskState.discoveredFacts.size} discovered",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    val backgroundColor = when {
        isUser -> MaterialTheme.colorScheme.primaryContainer
        message.isFallback -> Color(0xFFFFF3E0) // Light orange for fallback
        else -> MaterialTheme.colorScheme.surfaceVariant
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
                // Fallback indicator (Day 24)
                if (!isUser && message.isFallback) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFF9800).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "LOW RELEVANCE",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                        }
                        message.averageRelevance?.let { relevance ->
                            Text(
                                text = "%.0f%%".format(relevance * 100),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Main content
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Quotes section (Day 24)
                if (!isUser && message.quotes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Supporting Quotes:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    message.quotes.forEach { quote ->
                        QuoteItem(quote = quote)
                    }
                }

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

                // Relevance indicator (non-fallback)
                if (!isUser && !message.isFallback && message.averageRelevance != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Relevance: %.0f%%".format(message.averageRelevance * 100),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuoteItem(quote: AnswerQuote) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF1976D2).copy(alpha = 0.05f)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "\"${quote.text}\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "%.0f%%".format(quote.relevance * 100),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF1976D2),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${quote.file}${quote.section?.let { " / $it" } ?: ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
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
    var showEvalDropdown by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Evaluation questions dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Eval:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Box {
                    OutlinedButton(
                        onClick = { showEvalDropdown = true },
                        enabled = enabled
                    ) {
                        Text("Pick test question")
                    }

                    DropdownMenu(
                        expanded = showEvalDropdown,
                        onDismissRequest = { showEvalDropdown = false }
                    ) {
                        EvaluationQuestions.questions.forEach { evalQuestion ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = "#${evalQuestion.id}: ${evalQuestion.question}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "Expected: ${evalQuestion.expectation.take(50)}...",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                },
                                onClick = {
                                    text = evalQuestion.question
                                    showEvalDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Day 25: Memory mode tracks conversation & task state",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Input row
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    placeholder = { Text("Ask a question...") },
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
}
