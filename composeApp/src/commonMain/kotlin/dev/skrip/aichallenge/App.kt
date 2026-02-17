package dev.skrip.aichallenge

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.skrip.aichallenge.data.AnthropicClient
import dev.skrip.aichallenge.data.LlmRepositoryImpl
import dev.skrip.aichallenge.data.LogStoreImpl
import dev.skrip.aichallenge.domain.usecase.SendPromptUseCase
import dev.skrip.aichallenge.presentation.MainViewModel
import dev.skrip.aichallenge.ui.MainScreen
import dev.skrip.aichallenge.ui.theme.AppTheme

@Composable
fun App(apiKey: String) {
    val viewModel = remember {
        createViewModel(apiKey)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onCleared()
        }
    }

    AppTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (apiKey.isBlank()) {
                ApiKeyMissingScreen()
            } else {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun ApiKeyMissingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "API ключ не найден",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Установите переменную окружения ANTHROPIC_API_KEY",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "export ANTHROPIC_API_KEY=sk-ant-...",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

private fun createViewModel(apiKey: String): MainViewModel {
    val client = AnthropicClient(apiKey)
    val repository = LlmRepositoryImpl(client)
    val logStore = LogStoreImpl()
    val sendPromptUseCase = SendPromptUseCase(repository, logStore)

    return MainViewModel(
        sendPromptUseCase = sendPromptUseCase,
        logStore = logStore,
        onCleanup = { client.close() }
    )
}
