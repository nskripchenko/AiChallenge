package dev.skrip.aichallenge

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.skrip.aichallenge.ui.ChatScreen

fun main() = application {
    // Log documents on startup
    val documents = DocumentLoader.loadDocuments()
    println("=" .repeat(50))
    println("RAG Demo - Document Indexing")
    println("=" .repeat(50))
    println("Found ${documents.size} documents in docs/:")
    documents.forEach { doc ->
        println("  - ${doc.name} (${doc.type}, ${doc.content.length} chars)")
    }
    println("=" .repeat(50))

    Window(
        onCloseRequest = ::exitApplication,
        title = "RAG Demo - Document Indexing",
        state = rememberWindowState(width = 900.dp, height = 700.dp)
    ) {
        MaterialTheme(
            colorScheme = darkColorScheme()
        ) {
            ChatScreen()
        }
    }
}