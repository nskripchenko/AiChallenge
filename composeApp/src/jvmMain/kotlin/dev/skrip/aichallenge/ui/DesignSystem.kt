package dev.skrip.aichallenge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.highlightedCodeBlock
import com.mikepenz.markdown.compose.elements.highlightedCodeFence
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import dev.skrip.aichallenge.domain.model.Message
import dev.skrip.aichallenge.domain.model.Role
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Unified design system colors used across the app
 */
object AppTheme {
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
    val success = Color(0xFF166534)
    val successBackground = Color(0xFFDCFCE7)
    val warning = Color(0xFF92400E)
    val warningBackground = Color(0xFFFEF3C7)
    val warningBorder = Color(0xFFFCD34D)
}

/**
 * Renders markdown content with consistent styling
 */
@Composable
fun MarkdownContent(
    content: String,
    modifier: Modifier = Modifier
) {
    val baseTextStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = 14.sp,
        lineHeight = 22.sp,
        color = AppTheme.textSecondary
    )

    Markdown(
        content = content,
        modifier = modifier,
        colors = markdownColor(
            text = AppTheme.textSecondary,
            codeText = AppTheme.textPrimary,
            codeBackground = AppTheme.backgroundSecondary,
            dividerColor = AppTheme.border
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

/**
 * Message metadata row (author, timestamp, usage)
 */
@Composable
fun MessageMetadata(
    message: Message,
    modifier: Modifier = Modifier,
    additionalContent: @Composable (() -> Unit)? = null
) {
    val isUser = message.role == Role.USER

    Row(
        modifier = modifier.padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isUser) "You" else "Claude",
            fontSize = 12.sp,
            color = AppTheme.textMuted
        )

        Text(
            text = formatTimestamp(message.timestamp),
            fontSize = 12.sp,
            color = AppTheme.textMuted
        )

        message.usage?.let { usage ->
            Text(
                text = "${usage.responseTimeSec.format()}s · ${usage.totalTokens} tokens · ${formatCost(usage.costUsd)}",
                fontSize = 12.sp,
                color = AppTheme.textMuted
            )
        }

        additionalContent?.invoke()
    }
}

fun Double.format(): String = "%.1f".format(this)

fun formatCost(cost: Double): String = when {
    cost < 0.001 -> "<$0.001"
    else -> "$${String.format("%.3f", cost)}"
}

fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
