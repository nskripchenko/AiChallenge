package dev.skrip.aichallenge

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.skrip.aichallenge.pipeline.PipelineScreen
import java.io.File

fun main() = application {
    val mcpJarPath = findMcpJar()
    println("MCP JAR path: $mcpJarPath")
    println("JAR exists: ${File(mcpJarPath).exists()}")

    Window(
        onCloseRequest = ::exitApplication,
        title = "Day 19: Content Pipeline Demo",
        state = rememberWindowState(width = 900.dp, height = 800.dp)
    ) {
        PipelineScreen(mcpServerJarPath = mcpJarPath)
    }
}

private fun findMcpJar(): String {
    val jarName = "content-pipeline-mcp-1.0.0.jar"
    val relativePath = "content-pipeline-mcp/build/libs/$jarName"

    // Try multiple possible locations
    val candidates = listOf(
        File(System.getProperty("user.dir"), relativePath),
        File(System.getProperty("user.dir")).parentFile?.let { File(it, relativePath) },
        File(System.getProperty("user.dir"), "../$relativePath"),
        // Fallback: search upward from current dir
        findJarUpward(File(System.getProperty("user.dir")), jarName)
    )

    return candidates
        .filterNotNull()
        .firstOrNull { it.exists() }
        ?.absolutePath
        ?: File(System.getProperty("user.dir"), relativePath).absolutePath
}

private fun findJarUpward(startDir: File, jarName: String): File? {
    var current: File? = startDir
    repeat(5) {
        current?.let { dir ->
            val candidate = File(dir, "content-pipeline-mcp/build/libs/$jarName")
            if (candidate.exists()) return candidate
        }
        current = current?.parentFile
    }
    return null
}