package dev.skrip.aichallenge

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val apiKey = System.getenv("ANTHROPIC_API_KEY") ?: ""

    Window(
        onCloseRequest = ::exitApplication,
        title = "LLM Playground",
        state = rememberWindowState(width = 1400.dp, height = 900.dp)
    ) {
        App(apiKey)
    }
}
