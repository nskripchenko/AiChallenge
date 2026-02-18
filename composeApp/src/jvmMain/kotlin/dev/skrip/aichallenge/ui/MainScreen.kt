package dev.skrip.aichallenge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.skrip.aichallenge.model.AnthropicModel

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header with title and model selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "AI Reasoning Lab",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Model dropdown
                    var modelExpanded by remember { mutableStateOf(false) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Модель: ", style = MaterialTheme.typography.bodyMedium)
                        Box {
                            OutlinedButton(onClick = { modelExpanded = true }) {
                                Text(state.selectedModel.displayName)
                            }
                            DropdownMenu(
                                expanded = modelExpanded,
                                onDismissRequest = { modelExpanded = false }
                            ) {
                                AnthropicModel.entries.forEach { model ->
                                    DropdownMenuItem(
                                        text = { Text(model.displayName) },
                                        onClick = {
                                            viewModel.selectModel(model)
                                            modelExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Error message
                state.errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text("OK")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2x2 grid of prompt cards
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PromptCard(
                            index = 0,
                            prompt = state.prompts[0],
                            result = state.results[0],
                            onPromptChange = { viewModel.updatePrompt(0, it) },
                            onRun = { viewModel.runSingle(0) },
                            modifier = Modifier.weight(1f)
                        )
                        PromptCard(
                            index = 2,
                            prompt = state.prompts[2],
                            result = state.results[2],
                            onPromptChange = { viewModel.updatePrompt(2, it) },
                            onRun = { viewModel.runSingle(2) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PromptCard(
                            index = 1,
                            prompt = state.prompts[1],
                            result = state.results[1],
                            onPromptChange = { viewModel.updatePrompt(1, it) },
                            onRun = { viewModel.runSingle(1) },
                            modifier = Modifier.weight(1f)
                        )
                        PromptCard(
                            index = 3,
                            prompt = state.prompts[3],
                            result = state.results[3],
                            onPromptChange = { viewModel.updatePrompt(3, it) },
                            onRun = { viewModel.runSingle(3) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PromptCard(
    index: Int,
    prompt: String,
    result: PromptResult,
    onPromptChange: (String) -> Unit,
    onRun: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp)
        ) {
            // Header with run button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Промпт ${index + 1}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (result.durationMs > 0) {
                        Text(
                            text = "${result.durationMs}ms",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onRun,
                        enabled = !result.isLoading,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            if (result.isLoading) "..." else "Run",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Prompt input
            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                modifier = Modifier.fillMaxWidth().weight(0.4f),
                placeholder = { Text("Введите промпт...") },
                textStyle = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Result area
            Text(
                text = "Результат:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
                    .background(
                        if (result.error != null) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp)
            ) {
                when {
                    result.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                    result.error != null -> {
                        val scrollState = rememberScrollState()
                        SelectionContainer {
                            Text(
                                text = "Ошибка: ${result.error}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.verticalScroll(scrollState)
                            )
                        }
                    }
                    result.response.isNotEmpty() -> {
                        val scrollState = rememberScrollState()
                        SelectionContainer {
                            Text(
                                text = result.response,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.verticalScroll(scrollState)
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = "Результат появится здесь...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
