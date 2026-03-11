package dev.skrip.aichallenge

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.skrip.aichallenge.marketwatcher.MarketWatcherScreen

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Market Watcher - Day 18 Demo",
        state = rememberWindowState(size = DpSize(1200.dp, 800.dp))
    ) {
        MarketWatcherScreen()
    }
}