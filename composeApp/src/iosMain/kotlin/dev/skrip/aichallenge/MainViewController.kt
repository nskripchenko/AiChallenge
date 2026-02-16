package dev.skrip.aichallenge

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController { App(getApiKey()) }
