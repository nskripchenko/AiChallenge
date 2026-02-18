package dev.skrip.aichallenge

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.skrip.aichallenge.ui.MainScreen
import dev.skrip.aichallenge.ui.MainViewModel

fun main() = application {
    val viewModel = MainViewModel()

    Window(
        onCloseRequest = ::exitApplication,
        title = "AI Reasoning Lab",
        state = rememberWindowState(width = 1200.dp, height = 900.dp)
    ) {
        MainScreen(viewModel)
    }
}