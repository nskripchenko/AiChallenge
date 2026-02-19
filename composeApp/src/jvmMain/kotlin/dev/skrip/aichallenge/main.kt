package dev.skrip.aichallenge

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.skrip.aichallenge.ui.temperature.TemperaturePlaygroundScreen
import dev.skrip.aichallenge.ui.temperature.TemperaturePlaygroundViewModel

fun main() = application {
    val viewModel = TemperaturePlaygroundViewModel()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Temperature Playground — Day 4",
        state = rememberWindowState(width = 1400.dp, height = 900.dp)
    ) {
        TemperaturePlaygroundScreen(viewModel)
    }
}
