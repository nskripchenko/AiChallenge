package dev.skrip.aichallenge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.skrip.aichallenge.ui.modelbench.ModelBenchScreen

@Composable
@Preview
fun App() {
    MaterialTheme {
        val envApiKey = remember { getEnvApiKey() }
        var apiKey by remember { mutableStateOf(envApiKey ?: "") }
        var showApiKeyDialog by remember { mutableStateOf(envApiKey.isNullOrBlank()) }

        // Диалог ввода API ключа (только если нет в env)
        if (showApiKeyDialog) {
            ApiKeyDialog(
                onApiKeyEntered = { key ->
                    apiKey = key
                    showApiKeyDialog = false
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (apiKey.isBlank()) {
                // Заглушка если нет API ключа
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Требуется API ключ Anthropic",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showApiKeyDialog = true }) {
                        Text("Ввести API ключ")
                    }
                }
            } else {
                ModelBenchScreen(apiKey = apiKey)
            }
        }
    }
}

@Composable
private fun ApiKeyDialog(onApiKeyEntered: (String) -> Unit) {
    var keyInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { },
        title = { Text("Anthropic API ключ") },
        text = {
            Column {
                Text("Введите ваш API ключ для работы с Anthropic Claude:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("API ключ") },
                    placeholder = { Text("sk-ant-...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onApiKeyEntered(keyInput) },
                enabled = keyInput.isNotBlank()
            ) {
                Text("Сохранить")
            }
        }
    )
}

