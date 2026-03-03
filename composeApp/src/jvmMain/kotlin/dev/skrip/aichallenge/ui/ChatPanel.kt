package dev.skrip.aichallenge.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.skrip.aichallenge.domain.model.Message
import dev.skrip.aichallenge.domain.model.Role
import dev.skrip.aichallenge.util.estimateTokens
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
fun ChatPanel(
    messages: List<Message>,
    inputText: String,
    isLoading: Boolean,
    isStreaming: Boolean,
    streamingText: String,
    errorMessage: String?,
    onInputChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onStopClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new messages or streaming
    LaunchedEffect(messages.size, streamingText.length) {
        val itemCount = messages.size + if (streamingText.isNotEmpty()) 1 else 0
        if (itemCount > 0) {
            listState.scrollToItem(itemCount - 1, scrollOffset = Int.MAX_VALUE)
        }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(AppTheme.background)
            .padding(24.dp)
    ) {
        // Header
        Text(
            text = "Chat",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.textPrimary,
            letterSpacing = (-0.02).sp,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Messages list
        Box(modifier = Modifier.weight(1f)) {
            if (messages.isEmpty() && streamingText.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Start a conversation",
                        fontSize = 14.sp,
                        color = AppTheme.textMuted
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(
                        count = messages.size,
                        key = { index -> messages[index].id }
                    ) { index ->
                        MessageItem(message = messages[index])
                    }

                    if (streamingText.isNotEmpty()) {
                        item(key = "streaming") {
                            StreamingMessage(text = streamingText)
                        }
                    }
                }
            }
        }

        // Typing indicator
        if (isLoading && !isStreaming) {
            TypingIndicator()
        }

        // Error message
        errorMessage?.let { error ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .background(Color(0xFFFEE2E2), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = error,
                    color = Color(0xFFDC2626),
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Copy",
                    fontSize = 11.sp,
                    color = Color(0xFFDC2626),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                            clipboard.setContents(StringSelection(error), null)
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
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
                modifier = Modifier
                    .weight(1f)
                    .onKeyEvent { event ->
                        if (event.key == Key.Enter && !isLoading && !isStreaming && inputText.isNotBlank()) {
                            onSendClicked()
                            true
                        } else {
                            false
                        }
                    },
                placeholder = {
                    Text(
                        "Type a message...",
                        color = AppTheme.textMuted,
                        fontSize = 14.sp
                    )
                },
                maxLines = 4,
                enabled = !isLoading && !isStreaming,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppTheme.accent,
                    unfocusedBorderColor = AppTheme.border,
                    cursorColor = AppTheme.accent,
                    disabledBorderColor = AppTheme.border.copy(alpha = 0.5f),
                    disabledTextColor = AppTheme.textMuted
                ),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank() && !isLoading && !isStreaming) {
                            onSendClicked()
                        }
                    }
                )
            )

            Button(
                onClick = if (isStreaming) onStopClicked else onSendClicked,
                enabled = if (isStreaming) true else inputText.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isStreaming) Color(0xFFEF4444) else AppTheme.accent,
                    contentColor = Color.White,
                    disabledContainerColor = AppTheme.border,
                    disabledContentColor = AppTheme.textMuted
                ),
                shape = RoundedCornerShape(12.dp),
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
                fontSize = 11.sp,
                color = AppTheme.textMuted,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp)
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
private fun MessageItem(message: Message) {
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

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MessageMetadata(message = message)

            if (!isUser) {
                Text(
                    text = "Copy",
                    fontSize = 11.sp,
                    color = AppTheme.accent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                            clipboard.setContents(StringSelection(message.text), null)
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun UserMessageBubble(text: String) {
    Box(
        modifier = Modifier
            .widthIn(max = 480.dp)
            .background(
                color = AppTheme.accent.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
            )
            .border(
                width = 1.dp,
                color = AppTheme.accent.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
            )
            .padding(14.dp, 10.dp)
    ) {
        Text(
            text = text,
            color = AppTheme.textPrimary,
            fontSize = 14.sp,
            lineHeight = 22.sp
        )
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
