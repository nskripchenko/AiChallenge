package dev.skrip.aichallenge.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.skrip.aichallenge.domain.model.ContextStrategy
import dev.skrip.aichallenge.domain.model.Message
import dev.skrip.aichallenge.domain.model.Role
import dev.skrip.aichallenge.ui.state.Branch
import dev.skrip.aichallenge.util.estimateTokens

@Composable
fun ChatPanel(
    messages: List<Message>,
    inputText: String,
    isLoading: Boolean,
    isStreaming: Boolean,
    isCompressing: Boolean,
    streamingText: String,
    errorMessage: String?,
    hasSummary: Boolean,
    summarizedCount: Int,
    summary: String?,
    // Strategy-specific
    currentStrategy: ContextStrategy,
    windowSize: Int,
    messagesOutsideWindow: List<Message>,
    // Branching
    branches: List<Branch>,
    currentBranchId: String,
    onCreateBranch: (name: String, fromMessageIndex: Int) -> Unit,
    onSwitchBranch: (branchId: String) -> Unit,
    onDeleteBranch: (branchId: String) -> Unit,
    onInputChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onStopClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, streamingText) {
        if (messages.isNotEmpty() || streamingText.isNotEmpty()) {
            listState.animateScrollToItem(maxOf(0, messages.size - 1 + if (streamingText.isNotEmpty()) 1 else 0))
        }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(AppTheme.background)
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Chat",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.textPrimary,
                letterSpacing = (-0.02).sp
            )

            // Branch selector for BRANCHING strategy
            if (currentStrategy == ContextStrategy.BRANCHING) {
                BranchSelector(
                    branches = branches,
                    currentBranchId = currentBranchId,
                    onSwitchBranch = onSwitchBranch,
                    onDeleteBranch = onDeleteBranch
                )
            }
        }

        // Messages
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Sliding Window: Show collapsible section for older messages
                if (currentStrategy == ContextStrategy.SLIDING_WINDOW && messagesOutsideWindow.isNotEmpty()) {
                    item(key = "older_messages_section") {
                        OlderMessagesSection(
                            messages = messagesOutsideWindow,
                            windowSize = windowSize
                        )
                    }
                }

                // Summary indicator (only for SUMMARIZATION strategy)
                if (currentStrategy == ContextStrategy.BRANCHING && hasSummary && summarizedCount > 0) {
                    item(key = "summary_indicator") {
                        SummaryIndicator(
                            count = summarizedCount,
                            summaryText = summary
                        )
                    }
                }

                // Messages
                items(
                    count = messages.size,
                    key = { index -> messages[index].id }
                ) { index ->
                    MessageItem(
                        message = messages[index],
                        messageIndex = index,
                        showForkButton = currentStrategy == ContextStrategy.BRANCHING,
                        onFork = onCreateBranch
                    )
                }

                if (streamingText.isNotEmpty()) {
                    item(key = "streaming") {
                        StreamingMessage(text = streamingText)
                    }
                }
            }
        }

        // Typing indicator
        if (isLoading && !isStreaming) {
            TypingIndicator()
        }

        // Compression indicator
        AnimatedVisibility(
            visible = isCompressing,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            CompressionIndicator()
        }

        // Error
        errorMessage?.let { error ->
            Text(
                text = error,
                color = AppTheme.error,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input area
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChanged,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Ask something...",
                        color = AppTheme.textMuted,
                        fontSize = 14.sp
                    )
                },
                maxLines = 4,
                enabled = !isLoading && !isStreaming,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppTheme.accent,
                    unfocusedBorderColor = AppTheme.border,
                    cursorColor = AppTheme.accent
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Button(
                onClick = if (isStreaming) onStopClicked else onSendClicked,
                enabled = if (isStreaming) true else inputText.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.accent,
                    contentColor = AppTheme.onAccent,
                    disabledContainerColor = AppTheme.border,
                    disabledContentColor = AppTheme.textMuted
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(52.dp)
            ) {
                Text(
                    text = if (isStreaming) "Stop" else "Send",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Token estimate
        if (inputText.isNotBlank()) {
            Text(
                text = "~${inputText.estimateTokens()} tokens",
                fontSize = 12.sp,
                color = AppTheme.textMuted,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )
        }
    }
}

@Composable
private fun StreamingMessage(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        MarkdownContent(content = text, modifier = Modifier.widthIn(max = 560.dp))
        Spacer(modifier = Modifier.height(8.dp))
        TypingIndicator()
    }
}

@Composable
private fun MessageItem(
    message: Message,
    messageIndex: Int = 0,
    showForkButton: Boolean = false,
    onFork: ((name: String, fromIndex: Int) -> Unit)? = null
) {
    var showForkDialog by remember { mutableStateOf(false) }
    var branchName by remember { mutableStateOf("") }
    val isUser = message.role == Role.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        SelectionContainer {
            if (isUser) {
                UserMessageBubble(text = message.text)
            } else {
                MarkdownContent(content = message.text, modifier = Modifier.widthIn(max = 560.dp))
            }
        }

        // Metadata with optional fork button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MessageMetadata(message = message)

            if (showForkButton && onFork != null) {
                ForkButton(onClick = { showForkDialog = true })
            }
        }

        // Fork dialog
        if (showForkDialog && onFork != null) {
            ForkDialog(
                messageIndex = messageIndex,
                branchName = branchName,
                onBranchNameChange = { branchName = it },
                onConfirm = {
                    onFork(branchName.ifBlank { "Ветка ${messageIndex + 1}" }, messageIndex)
                    branchName = ""
                    showForkDialog = false
                },
                onDismiss = {
                    branchName = ""
                    showForkDialog = false
                }
            )
        }
    }
}

@Composable
private fun UserMessageBubble(text: String) {
    Box(
        modifier = Modifier
            .widthIn(max = 480.dp)
            .background(
                color = AppTheme.backgroundSecondary,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = AppTheme.border,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp, 10.dp)
    ) {
        Text(
            text = text,
            color = AppTheme.textSecondary,
            fontSize = 14.sp,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun OlderMessagesSection(
    messages: List<Message>,
    windowSize: Int
) {
    var isExpanded by remember { mutableStateOf(false) }
    val olderCount = messages.size

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Header row with collapsible indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(AppTheme.border)
            )

            // Clickable badge showing older messages count
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppTheme.warningBackground)
                    .border(1.dp, AppTheme.warningBorder, RoundedCornerShape(12.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = "🪟", fontSize = 12.sp)
                Text(
                    text = "$olderCount older messages (not sent to AI)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppTheme.warning
                )
                Text(
                    text = if (isExpanded) "▲" else "▼",
                    fontSize = 10.sp,
                    color = AppTheme.warning
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(AppTheme.border)
            )
        }

        // Expandable older messages
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .background(AppTheme.backgroundSecondary, RoundedCornerShape(8.dp))
                    .border(1.dp, AppTheme.border, RoundedCornerShape(8.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Messages outside window (last $windowSize kept)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.textTertiary
                )

                messages.forEach { message ->
                    OlderMessageItem(message)
                }
            }
        }
    }
}

@Composable
private fun OlderMessageItem(message: Message) {
    val isUser = message.role == Role.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .background(
                        color = if (isUser) AppTheme.backgroundHover else AppTheme.background,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(1.dp, AppTheme.border, RoundedCornerShape(8.dp))
                    .padding(10.dp, 8.dp)
            ) {
                Text(
                    text = message.text,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = AppTheme.textTertiary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isUser) "You" else "Claude",
                    fontSize = 10.sp,
                    color = AppTheme.textMuted
                )
                Text(
                    text = formatTimestamp(message.timestamp),
                    fontSize = 10.sp,
                    color = AppTheme.textMuted
                )
            }
        }
    }
}

@Composable
private fun SummaryIndicator(count: Int, summaryText: String?) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Header row with lines and clickable center
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(AppTheme.border)
            )

            // Clickable summary badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppTheme.backgroundSecondary)
                    .border(1.dp, AppTheme.border, RoundedCornerShape(12.dp))
                    .clickable(enabled = summaryText != null) { isExpanded = !isExpanded }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "\uD83D\uDCDD",
                    fontSize = 12.sp
                )
                Text(
                    text = "$count messages → summary",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppTheme.textTertiary
                )
                if (summaryText != null) {
                    Text(
                        text = if (isExpanded) "▲" else "▼",
                        fontSize = 10.sp,
                        color = AppTheme.textMuted
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(AppTheme.border)
            )
        }

        // Expandable summary content
        AnimatedVisibility(
            visible = isExpanded && summaryText != null,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .background(AppTheme.backgroundSecondary, RoundedCornerShape(8.dp))
                    .border(1.dp, AppTheme.border, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "Context Summary",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.textTertiary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = summaryText ?: "",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = AppTheme.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun CompressionIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "compression")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "\uD83D\uDCDD",
                fontSize = 12.sp
            )
            Text(
                text = "Summarizing context...",
                fontSize = 12.sp,
                color = AppTheme.textTertiary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Animated progress bar
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(2.dp)
                .background(AppTheme.border, RoundedCornerShape(1.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(2.dp)
                    .background(AppTheme.accent, RoundedCornerShape(1.dp))
            )
        }
    }
}

@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 400, delayMillis = index * 100),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )

            Box(
                modifier = Modifier
                    .size(6.dp)
                    .offset(y = offsetY.dp)
                    .background(
                        color = AppTheme.textMuted,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun ForkButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(AppTheme.backgroundSecondary)
            .border(1.dp, AppTheme.border, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = "+", fontSize = 12.sp, color = AppTheme.textTertiary)
        Text(text = "ветка", fontSize = 11.sp, color = AppTheme.textTertiary)
    }
}

@Composable
private fun ForkDialog(
    messageIndex: Int,
    branchName: String,
    onBranchNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Создать ветку", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        },
        text = {
            Column {
                Text(
                    "Новая ветка диалога от сообщения #${messageIndex + 1}",
                    fontSize = 13.sp,
                    color = AppTheme.textTertiary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = branchName,
                    onValueChange = onBranchNameChange,
                    label = { Text("Название ветки") },
                    placeholder = { Text("Ветка ${messageIndex + 1}") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Создать")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
private fun BranchSelector(
    branches: List<Branch>,
    currentBranchId: String,
    onSwitchBranch: (String) -> Unit,
    onDeleteBranch: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentBranch = branches.find { it.id == currentBranchId }
    val displayName = currentBranch?.name ?: "main"

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(AppTheme.backgroundSecondary)
                .border(1.dp, AppTheme.border, RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "🌿", fontSize = 14.sp)
            Text(
                text = displayName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = AppTheme.textPrimary
            )
            Text(
                text = if (expanded) "▲" else "▼",
                fontSize = 10.sp,
                color = AppTheme.textMuted
            )
        }

        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // Main branch
            androidx.compose.material3.DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "main",
                            fontSize = 13.sp,
                            fontWeight = if (currentBranchId == "main") FontWeight.SemiBold else FontWeight.Normal,
                            color = AppTheme.textPrimary
                        )
                        if (currentBranchId == "main") {
                            Text("✓", fontSize = 12.sp, color = AppTheme.accent)
                        }
                    }
                },
                onClick = {
                    onSwitchBranch("main")
                    expanded = false
                }
            )

            // Other branches
            branches.forEach { branch ->
                androidx.compose.material3.DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = branch.name,
                                    fontSize = 13.sp,
                                    fontWeight = if (branch.id == currentBranchId) FontWeight.SemiBold else FontWeight.Normal,
                                    color = AppTheme.textPrimary
                                )
                                if (branch.id == currentBranchId) {
                                    Text("✓", fontSize = 12.sp, color = AppTheme.accent)
                                }
                            }
                            // Delete button
                            Text(
                                text = "×",
                                fontSize = 16.sp,
                                color = AppTheme.textMuted,
                                modifier = Modifier
                                    .clickable { onDeleteBranch(branch.id) }
                                    .padding(horizontal = 4.dp)
                            )
                        }
                    },
                    onClick = {
                        onSwitchBranch(branch.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
