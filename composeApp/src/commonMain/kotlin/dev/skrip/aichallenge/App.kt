package dev.skrip.aichallenge

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.skrip.aichallenge.data.AnthropicClient
import dev.skrip.aichallenge.state.ChatViewModel
import dev.skrip.aichallenge.ui.ChatScreen

@Composable
fun App(apiKey: String?) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (apiKey.isNullOrBlank()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ошибка: переменная окружения ANTHROPIC_API_KEY не установлена.\n\n" +
                                "Для запуска установите переменную:\n" +
                                "export ANTHROPIC_API_KEY=your_key_here",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                val client = remember { AnthropicClient(apiKey) }
                val viewModel = remember { ChatViewModel(client) }
                ChatScreen(viewModel)
            }
        }
    }
}
