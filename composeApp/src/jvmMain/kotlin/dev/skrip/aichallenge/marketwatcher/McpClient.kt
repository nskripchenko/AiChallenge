package dev.skrip.aichallenge.marketwatcher

import io.modelcontextprotocol.kotlin.sdk.CallToolResultBase
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import kotlinx.coroutines.delay
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.*
import java.io.File

class McpClient(
    private val onEvent: (String) -> Unit
) {
    private var client: Client? = null
    private var process: Process? = null

    val isConnected: Boolean
        get() = client != null && process?.isAlive == true

    suspend fun connect(): Boolean {
        return try {
            onEvent("Starting MCP server subprocess...")

            val jarPath = findMcpJar()
            if (jarPath == null) {
                onEvent("ERROR: MCP server JAR not found. Run './gradlew jar' in market-watcher-mcp")
                return false
            }

            process = ProcessBuilder("java", "-jar", jarPath)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()

            delay(500)

            if (process?.isAlive != true) {
                onEvent("ERROR: MCP server process failed to start")
                return false
            }

            val transport = StdioClientTransport(
                input = process!!.inputStream.asSource().buffered(),
                output = process!!.outputStream.asSink().buffered()
            )

            client = Client(
                clientInfo = Implementation(
                    name = "market-watcher-desktop",
                    version = "1.0.0"
                )
            )

            client!!.connect(transport)
            onEvent("MCP server connected")
            true
        } catch (e: Exception) {
            onEvent("ERROR: Failed to connect: ${e.message}")
            false
        }
    }

    suspend fun startWatch(symbol: String, timeframe: String, intervalSeconds: Int): JsonObject? {
        return callTool("start_market_watch", buildJsonObject {
            put("symbol", symbol)
            put("timeframe", timeframe)
            put("intervalSeconds", intervalSeconds)
        })
    }

    suspend fun stopWatch(symbol: String): JsonObject? {
        return callTool("stop_market_watch", buildJsonObject {
            put("symbol", symbol)
        })
    }

    suspend fun getMarketSummary(symbol: String): JsonObject? {
        return callTool("get_market_summary", buildJsonObject {
            put("symbol", symbol)
        })
    }

    suspend fun getWatchStatus(symbol: String): JsonObject? {
        return callTool("get_watch_status", buildJsonObject {
            put("symbol", symbol)
        })
    }

    private suspend fun callTool(name: String, arguments: JsonObject): JsonObject? {
        val client = this.client ?: return null
        return try {
            val result = client.callTool(name, arguments)
            result?.let { parseToolResult(it) }
        } catch (e: Exception) {
            onEvent("ERROR calling $name: ${e.message}")
            null
        }
    }

    private fun parseToolResult(result: CallToolResultBase): JsonObject? {
        val textContent = result.content.filterIsInstance<TextContent>().firstOrNull()
        return textContent?.text?.let { Json.parseToJsonElement(it).jsonObject }
    }

    fun disconnect() {
        try {
            process?.destroy()
            process = null
            client = null
            onEvent("MCP server disconnected")
        } catch (e: Exception) {
            onEvent("Error disconnecting: ${e.message}")
        }
    }

    private fun findMcpJar(): String? {
        val possiblePaths = listOf(
            "market-watcher-mcp/build/libs/market-watcher-mcp-1.0.0.jar",
            "../market-watcher-mcp/build/libs/market-watcher-mcp-1.0.0.jar",
            "build/libs/market-watcher-mcp-1.0.0.jar"
        )
        for (path in possiblePaths) {
            val file = File(path)
            if (file.exists()) {
                return file.absolutePath
            }
        }
        val currentDir = File(".").absolutePath
        onEvent("Looking for JAR in: $currentDir")
        return null
    }
}
