package mcp

import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * MCP Client that connects to the server and lists available tools.
 * Used for testing and verification.
 */
fun main() = runBlocking {
    println("=== MCP Client: List Tools ===")
    println()

    val projectDir = File(System.getProperty("user.dir"))

    // Build classpath for running server
    val javaHome = System.getProperty("java.home")
    val java = File(javaHome, "bin/java").absolutePath

    // Use application plugin distribution
    val libsDir = File(projectDir, "build/install/project-catalog-mcp/lib")

    if (!libsDir.exists()) {
        println("Building distribution...")
        val gradlew = File(projectDir, "gradlew")
        ProcessBuilder(gradlew.absolutePath, "installDist", "--quiet")
            .directory(projectDir)
            .inheritIO()
            .start()
            .waitFor(60, TimeUnit.SECONDS)
    }

    val classpath = libsDir.listFiles()?.joinToString(File.pathSeparator) { it.absolutePath }
        ?: error("Could not find libs in $libsDir")

    println("Starting MCP server process...")

    val processBuilder = ProcessBuilder(
        java, "-cp", classpath, "mcp.ServerKt"
    ).apply {
        directory(projectDir)
        redirectError(ProcessBuilder.Redirect.INHERIT)
    }

    val process = processBuilder.start()

    try {
        val transport = StdioClientTransport(
            process.inputStream.asSource().buffered(),
            process.outputStream.asSink().buffered()
        )

        val client = Client(
            clientInfo = Implementation(
                name = "project-catalog-client",
                version = "1.0.0"
            )
        )

        client.connect(transport)

        // List tools
        val toolsResult = client.listTools()

        println()
        println("Available tools:")
        println()
        toolsResult?.tools?.forEach { tool ->
            println("  - ${tool.name}")
            println("    ${tool.description}")
            println()
        } ?: println("  (no tools found)")

        client.close()

    } catch (e: Exception) {
        System.err.println("Error: ${e.message}")
        e.printStackTrace()
    } finally {
        process.destroyForcibly()
        process.waitFor(5, TimeUnit.SECONDS)
    }

    println("Done.")
}
