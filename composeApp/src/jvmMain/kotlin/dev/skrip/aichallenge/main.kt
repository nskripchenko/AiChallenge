package dev.skrip.aichallenge

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Day 17: MCP + LLM Agent",
        state = rememberWindowState(width = 1100.dp, height = 700.dp)
    ) {
        Day17DemoScreen()
    }
}