package dev.skrip.aichallenge.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.skrip.aichallenge.domain.model.RequestStatus
import kotlin.math.roundToInt
import kotlin.math.pow
import kotlin.math.abs

/**
 * Форматирование Float с 2 знаками после запятой (кроссплатформенно)
 */
fun Float.formatDecimal(decimals: Int = 2): String {
    val multiplier = 10.0.pow(decimals).toInt()
    val rounded = (this * multiplier).roundToInt()
    val intPart = rounded / multiplier
    val decPart = abs(rounded % multiplier).toString().padStart(decimals, '0')
    return "$intPart.$decPart"
}

@Composable
fun StatusIndicator(
    status: RequestStatus,
    latencyMs: Long? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val color = when (status) {
            RequestStatus.IDLE -> MaterialTheme.colorScheme.primary
            RequestStatus.SENDING -> MaterialTheme.colorScheme.tertiary
            RequestStatus.STREAMING -> MaterialTheme.colorScheme.secondary
            RequestStatus.ERROR -> MaterialTheme.colorScheme.error
        }

        Surface(
            shape = MaterialTheme.shapes.small,
            color = color.copy(alpha = 0.2f)
        ) {
            Text(
                text = status.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        latencyMs?.let {
            Text(
                text = "${it}мс",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}

@Composable
fun HelpText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
fun LabeledSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    helpText: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = value.formatDecimal(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
        helpText?.let {
            HelpText(text = it)
        }
    }
}

@Composable
fun MetricsRow(
    model: String,
    inputTokens: Int?,
    outputTokens: Int?,
    latencyMs: Long,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MetricChip(label = "Модель", value = model.take(20))
        inputTokens?.let { MetricChip(label = "Вход", value = "$it") }
        outputTokens?.let { MetricChip(label = "Выход", value = "$it") }
        MetricChip(label = "Время", value = "${latencyMs}мс")
    }
}

@Composable
private fun MetricChip(
    label: String,
    value: String
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

