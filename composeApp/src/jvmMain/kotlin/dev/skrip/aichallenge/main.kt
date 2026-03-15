package dev.skrip.aichallenge

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.skrip.aichallenge.orchestration.OrchestrationScreen

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Day 20: Multi-Server MCP Orchestration",
        state = rememberWindowState(width = 950.dp, height = 850.dp)
    ) {
        OrchestrationScreen()
    }
}