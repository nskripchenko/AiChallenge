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
import androidx.compose.foundation.lazy.items
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.highlightedCodeBlock
import com.mikepenz.markdown.compose.elements.highlightedCodeFence
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import androidx.compose.material3.MaterialTheme
import dev.skrip.aichallenge.domain.model.Message
import dev.skrip.aichallenge.domain.model.Role
import dev.skrip.aichallenge.domain.model.TokenUsage
import dev.skrip.aichallenge.util.estimateTokens

// Design System ChatColors
private object ChatColors {
    val background = Color.White
    val backgroundSecondary = Color(0xFFFAFAFA)
    val backgroundHover = Color(0xFFF5F5F5)

    val textPrimary = Color(0xFF0A0A0A)
    val textSecondary = Color(0xFF404040)
    val textTertiary = Color(0xFF737373)
    val textMuted = Color(0xFFA3A3A3)

    val border = Color(0xFFE5E5E5)
    val borderHover = Color(0xFFD4D4D4)

    val accent = Color(0xFF18181B)
    val accentHover = Color(0xFF27272A)
    val onAccent = Color(0xFFFAFAFA)

    val error = Color(0xFFDC2626)
}

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
            .background(ChatColors.background)
            .padding(24.dp)
    ) {
        // Header
        Text(
            text = "Chat",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = ChatColors.textPrimary,
            letterSpacing = (-0.02).sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Messages
        Box(modifier = Modifier.weight(1f)) {
            SelectionContainer {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    messages.forEachIndexed { index, message ->
                        // Show summary indicator right after summarized messages
                        if (hasSummary && index == summarizedCount) {
                            item(key = "summary_indicator") {
                                SummaryIndicator(
                                    count = summarizedCount,
                                    summaryText = summary
                                )
                            }
                        }
                        item(key = message.id) {
                            MessageItem(message)
                        }
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
                color = ChatColors.error,
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
                        color = ChatColors.textMuted,
                        fontSize = 14.sp
                    )
                },
                maxLines = 4,
                enabled = !isLoading && !isStreaming,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ChatColors.accent,
                    unfocusedBorderColor = ChatColors.border,
                    cursorColor = ChatColors.accent
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Button(
                onClick = if (isStreaming) onStopClicked else onSendClicked,
                enabled = if (isStreaming) true else inputText.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChatColors.accent,
                    contentColor = ChatColors.onAccent,
                    disabledContainerColor = ChatColors.border,
                    disabledContentColor = ChatColors.textMuted
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
                color = ChatColors.textMuted,
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
        val baseTextStyle = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 14.sp,
            lineHeight = 22.sp,
            color = ChatColors.textSecondary
        )
        Markdown(
            content = text,
            colors = markdownColor(
                text = ChatColors.textSecondary,
                codeText = ChatColors.textPrimary,
                codeBackground = ChatColors.backgroundSecondary,
                dividerColor = ChatColors.border
            ),
            typography = markdownTypography(
                h1 = baseTextStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                h2 = baseTextStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                h3 = baseTextStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                h4 = baseTextStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                h5 = baseTextStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                h6 = baseTextStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                text = baseTextStyle,
                paragraph = baseTextStyle,
                ordered = baseTextStyle,
                bullet = baseTextStyle,
                list = baseTextStyle,
                code = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp)
            ),
            components = markdownComponents(
                codeBlock = highlightedCodeBlock,
                codeFence = highlightedCodeFence
            )
        )

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
        if (isUser) {
            // User message - subtle bubble
            Box(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .background(
                        color = ChatColors.backgroundSecondary,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = ChatColors.border,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp, 10.dp)
            ) {
                Text(
                    text = message.text,
                    color = ChatColors.textSecondary,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }
        } else {
            // AI message - no bubble, just text
            Column(modifier = Modifier.widthIn(max = 560.dp)) {
                val baseTextStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = ChatColors.textSecondary
                )
                Markdown(
                    content = message.text,
                    colors = markdownColor(
                        text = ChatColors.textSecondary,
                        codeText = ChatColors.textPrimary,
                        codeBackground = ChatColors.backgroundSecondary,
                        dividerColor = ChatColors.border
                    ),
                    typography = markdownTypography(
                        h1 = baseTextStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                        h2 = baseTextStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                        h3 = baseTextStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                        h4 = baseTextStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                        h5 = baseTextStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                        h6 = baseTextStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                        text = baseTextStyle,
                        paragraph = baseTextStyle,
                        ordered = baseTextStyle,
                        bullet = baseTextStyle,
                        list = baseTextStyle,
                        code = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp)
                    ),
                    components = markdownComponents(
                        codeBlock = highlightedCodeBlock,
                        codeFence = highlightedCodeFence
                    )
                )
            }
        }

        // Metadata line
        Row(
            modifier = Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isUser) "You" else "Claude",
                fontSize = 12.sp,
                color = ChatColors.textMuted
            )

            Text(
                text = formatTimestamp(message.timestamp),
                fontSize = 12.sp,
                color = ChatColors.textMuted
            )

            message.usage?.let { usage ->
                Text(
                    text = "${usage.responseTimeSec.format()}s · ${usage.totalTokens} tokens · ${formatCost(usage.costUsd)}",
                    fontSize = 12.sp,
                    color = ChatColors.textMuted
                )
            }
        }
    }
}

private fun Double.format(): String = "%.1f".format(this)

private fun formatCost(cost: Double): String {
    return when {
        cost < 0.001 -> "<$0.001"
        else -> "$${String.format("%.3f", cost)}"
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
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
                    .background(ChatColors.border)
            )

            // Clickable summary badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(ChatColors.backgroundSecondary)
                    .border(1.dp, ChatColors.border, RoundedCornerShape(12.dp))
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
                    color = ChatColors.textTertiary
                )
                if (summaryText != null) {
                    Text(
                        text = if (isExpanded) "▲" else "▼",
                        fontSize = 10.sp,
                        color = ChatColors.textMuted
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(ChatColors.border)
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
                    .background(ChatColors.backgroundSecondary, RoundedCornerShape(8.dp))
                    .border(1.dp, ChatColors.border, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "Context Summary",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ChatColors.textTertiary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = summaryText ?: "",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = ChatColors.textSecondary
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
                color = ChatColors.textTertiary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Animated progress bar
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(2.dp)
                .background(ChatColors.border, RoundedCornerShape(1.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(2.dp)
                    .background(ChatColors.accent, RoundedCornerShape(1.dp))
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
                        color = ChatColors.textMuted,
                        shape = CircleShape
                    )
            )
        }
    }
}
