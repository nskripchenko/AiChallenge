package dev.skrip.aichallenge

import androidx.compose.ui.window.ComposeUIViewController
import platform.Foundation.NSProcessInfo

fun MainViewController() = ComposeUIViewController {
    val apiKey = NSProcessInfo.processInfo.environment["ANTHROPIC_API_KEY"] as? String ?: ""
    App(apiKey)
}
