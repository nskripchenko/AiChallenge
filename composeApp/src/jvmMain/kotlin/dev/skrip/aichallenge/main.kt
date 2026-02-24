package dev.skrip.aichallenge

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import dev.skrip.aichallenge.di.appModules
import dev.skrip.aichallenge.di.jvmModule
import dev.skrip.aichallenge.ui.ChatApp
import dev.skrip.aichallenge.ui.viewmodel.ChatViewModel
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.get

fun main() {
    startKoin {
        modules(appModules + jvmModule)
    }

    application {
        val windowState = rememberWindowState(width = 1200.dp, height = 800.dp)
        val viewModel: ChatViewModel = get(ChatViewModel::class.java)

        Window(
            onCloseRequest = ::exitApplication,
            title = "AI Chat - Claude Desktop",
            state = windowState
        ) {
            ChatApp(viewModel)
        }
    }
}
