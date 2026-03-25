package dev.skrip.aichallenge.quickask

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.skrip.aichallenge.ollama.OllamaClient
import kotlinx.coroutines.launch

/**
 * Day 27: Quick Ask - минималистичное desktop приложение с локальной LLM
 */
fun main() = application {
    val windowState = rememberWindowState(width = 500.dp, height = 400.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "Quick Ask - Local LLM",
        state = windowState
    ) {
        MaterialTheme(
            colorScheme = darkColorScheme()
        ) {
            QuickAskScreen()
        }
    }
}

@Composable
fun QuickAskScreen() {
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val ollamaClient = remember { OllamaClient() }
    val scope = rememberCoroutineScope()

    fun askQuestion() {
        if (question.isBlank() || isLoading) return

        isLoading = true
        error = null
        answer = ""

        scope.launch {
            try {
                answer = ollamaClient.chatPlain(question)
            } catch (e: Exception) {
                error = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ollamaClient.close()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Quick Ask",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Powered by local LLM (llama3.2)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Input row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    modifier = Modifier
                        .weight(1f)
                        .onKeyEvent { event ->
                            if (event.key == Key.Enter && event.type == KeyEventType.KeyUp && !event.isShiftPressed) {
                                askQuestion()
                                true
                            } else {
                                false
                            }
                        },
                    placeholder = { Text("Ask anything...") },
                    enabled = !isLoading,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = { askQuestion() },
                    enabled = question.isNotBlank() && !isLoading,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Ask")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error
            error?.let {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Answer
            if (answer.isNotEmpty() || isLoading) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        if (isLoading && answer.isEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Thinking...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        } else {
                            Text(
                                text = answer,
                                modifier = Modifier.verticalScroll(rememberScrollState()),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ask a question to get started",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            // Footer
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Press Enter to send • Day 27: Local LLM Integration",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}
