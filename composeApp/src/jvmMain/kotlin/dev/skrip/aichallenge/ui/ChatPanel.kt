package dev.skrip.aichallenge.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.skrip.aichallenge.domain.model.Message
import dev.skrip.aichallenge.domain.model.Role
import dev.skrip.aichallenge.util.estimateTokens

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
        Text(
            text = "Чат",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.textPrimary,
            letterSpacing = (-0.02).sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Messages
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
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

        // Typing indicator
        if (isLoading && !isStreaming) {
            TypingIndicator()
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
                        "Напишите сообщение...",
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
                    text = if (isStreaming) "Стоп" else "Отправить",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Token estimate
        if (inputText.isNotBlank()) {
            Text(
                text = "~${inputText.estimateTokens()} токенов",
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

        MessageMetadata(message = message)
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
