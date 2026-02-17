package dev.skrip.aichallenge.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.skrip.aichallenge.domain.model.LlmResult
import dev.skrip.aichallenge.domain.usecase.ComparisonResult
import dev.skrip.aichallenge.domain.usecase.DiffType

@Composable
fun ExperimentsPanel(
    repeatResults: List<LlmResult>,
    isRepeatRunning: Boolean,
    repeatProgress: Int,
    comparisonResult: ComparisonResult?,
    isComparing: Boolean,
    onRepeatFiveTimes: () -> Unit,
    onCompare: () -> Unit,
    onClearRepeatResults: () -> Unit,
    onClearComparison: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRepeatDialog by remember { mutableStateOf(false) }
    var showCompareDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Эксперименты",
                style = MaterialTheme.typography.titleSmall
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExperimentButton(
                    text = if (isRepeatRunning) "Прогон $repeatProgress/5" else "Повторить 5 раз",
                    isLoading = isRepeatRunning,
                    onClick = onRepeatFiveTimes,
                    enabled = !isRepeatRunning && !isComparing
                )

                ExperimentButton(
                    text = "Сравнить ответы",
                    isLoading = isComparing,
                    onClick = onCompare,
                    enabled = !isRepeatRunning && !isComparing
                )
            }

            if (repeatResults.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showRepeatDialog = true }) {
                        Text("Результаты повторов (${repeatResults.size})")
                    }
                    TextButton(onClick = onClearRepeatResults) {
                        Text("Очистить")
                    }
                }
            }

            if (comparisonResult != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showCompareDialog = true }) {
                        val diffText = if (comparisonResult.diff.isSame) "совпадают"
                        else "отличаются на ${comparisonResult.diff.changedLinesCount} строк"
                        Text("Сравнение: $diffText")
                    }
                    TextButton(onClick = onClearComparison) {
                        Text("Очистить")
                    }
                }
            }
        }
    }

    if (showRepeatDialog) {
        RepeatResultsDialog(
            results = repeatResults,
            onDismiss = { showRepeatDialog = false }
        )
    }

    if (showCompareDialog && comparisonResult != null) {
        ComparisonDialog(
            result = comparisonResult,
            onDismiss = { showCompareDialog = false }
        )
    }
}

@Composable
private fun ExperimentButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    enabled: Boolean
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(text)
    }
}

@Composable
private fun RepeatResultsDialog(
    results: List<LlmResult>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Результаты повторных запросов") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                itemsIndexed(results) { index, result ->
                    RepeatResultItem(index = index + 1, result = result)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
private fun RepeatResultItem(index: Int, result: LlmResult) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        onClick = { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = if (result.error != null) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Прогон #$index",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "${result.latencyMs}мс",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (result.error != null) {
                Text(
                    text = result.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    result.inputTokens?.let {
                        Text("In: $it", style = MaterialTheme.typography.labelSmall)
                    }
                    result.outputTokens?.let {
                        Text("Out: $it", style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (expanded) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = result.text.take(500) + if (result.text.length > 500) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonDialog(
    result: ComparisonResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Сравнение ответов") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ComparisonSummary(result)
                HorizontalDivider()
                DiffView(result)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
private fun ComparisonSummary(result: ComparisonResult) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (result.diff.isSame) "Ответы совпадают" else "Ответы отличаются",
            style = MaterialTheme.typography.titleSmall,
            color = if (result.diff.isSame) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error
        )

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column {
                Text("Без ограничений", style = MaterialTheme.typography.labelMedium)
                Text("${result.rawResult.latencyMs}мс", style = MaterialTheme.typography.bodySmall)
                result.rawResult.outputTokens?.let {
                    Text("$it токенов", style = MaterialTheme.typography.bodySmall)
                }
            }
            Column {
                Text("С контролем", style = MaterialTheme.typography.labelMedium)
                Text("${result.controlledResult.latencyMs}мс", style = MaterialTheme.typography.bodySmall)
                result.controlledResult.outputTokens?.let {
                    Text("$it токенов", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (!result.diff.isSame) {
            Text(
                text = "Изменено строк: ${result.diff.changedLinesCount} из ${result.diff.totalLines}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun DiffView(result: ComparisonResult) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Различия:", style = MaterialTheme.typography.labelMedium)

        if (result.diff.isSame) {
            Text(
                text = "Ответы идентичны",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            result.diff.differentLines.take(20).forEach { diffLine ->
                DiffLineView(diffLine)
            }

            if (result.diff.differentLines.size > 20) {
                Text(
                    text = "... и ещё ${result.diff.differentLines.size - 20} отличий",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DiffLineView(diffLine: dev.skrip.aichallenge.domain.usecase.DiffLine) {
    val bgColor = when (diffLine.type) {
        DiffType.ADDED -> Color(0xFF1B5E20).copy(alpha = 0.2f)
        DiffType.REMOVED -> Color(0xFFB71C1C).copy(alpha = 0.2f)
        DiffType.CHANGED -> Color(0xFFF57F17).copy(alpha = 0.2f)
    }

    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = bgColor
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            Text(
                text = "Строка ${diffLine.lineNumber}:",
                style = MaterialTheme.typography.labelSmall
            )
            diffLine.rawLine?.let {
                Text(
                    text = "- $it",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color(0xFFB71C1C)
                )
            }
            diffLine.controlledLine?.let {
                Text(
                    text = "+ $it",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color(0xFF1B5E20)
                )
            }
        }
    }
}
