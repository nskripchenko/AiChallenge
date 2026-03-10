package dev.skrip.aichallenge

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File

class McpJsonPlaceholderAgent {

    private var process: Process? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null
    private var requestId = 0

    var lastError: String? = null
        private set

    suspend fun start(): Boolean = withContext(Dispatchers.IO) {
        try {
            lastError = null
            System.err.println("[Agent] Starting...")

            val projectRoot = findProjectRoot()
            val mcpServerDir = File(projectRoot, "jsonplaceholder-mcp")
            val jarPath = File(mcpServerDir, "build/libs/jsonplaceholder-mcp-1.0.0.jar")

            System.err.println("[Agent] JAR path: ${jarPath.absolutePath}")

            if (!jarPath.exists()) {
                lastError = "JAR not found: ${jarPath.absolutePath}"
                System.err.println("[Agent] $lastError")
                return@withContext false
            }

            val processBuilder = ProcessBuilder("java", "-jar", jarPath.absolutePath)
            processBuilder.directory(mcpServerDir)
            processBuilder.redirectErrorStream(false)

            val proc = processBuilder.start()
            process = proc

            // Read stderr in background
            Thread {
                proc.errorStream.bufferedReader().forEachLine {
                    System.err.println("[MCP Server] $it")
                }
            }.start()

            reader = proc.inputStream.bufferedReader()
            writer = proc.outputStream.bufferedWriter()

            // Wait for server to start
            delay(500)

            // Send initialize request
            System.err.println("[Agent] Sending initialize request...")
            val initResponse = sendRequest("initialize", buildJsonObject {
                put("protocolVersion", JsonPrimitive("2024-11-05"))
                put("capabilities", JsonObject(emptyMap()))
                put("clientInfo", buildJsonObject {
                    put("name", JsonPrimitive("desktop-client"))
                    put("version", JsonPrimitive("1.0.0"))
                })
            })

            if (initResponse == null) {
                lastError = "No response to initialize"
                return@withContext false
            }

            System.err.println("[Agent] Initialize response: $initResponse")
            System.err.println("[Agent] Connected to MCP server!")
            true
        } catch (e: Exception) {
            lastError = e.message ?: e.toString()
            System.err.println("[Agent] Failed: $lastError")
            e.printStackTrace(System.err)
            false
        }
    }

    suspend fun getPost(id: Int): String = withContext(Dispatchers.IO) {
        callTool("get_post", buildJsonObject { put("id", JsonPrimitive(id)) })
    }

    suspend fun getUser(id: Int): String = withContext(Dispatchers.IO) {
        callTool("get_user", buildJsonObject { put("id", JsonPrimitive(id)) })
    }

    private fun callTool(name: String, arguments: JsonObject): String {
        val response = sendRequest("tools/call", buildJsonObject {
            put("name", JsonPrimitive(name))
            put("arguments", arguments)
        })

        if (response == null) return "Error: No response"

        val result = response["result"]?.jsonObject
        val content = result?.get("content")?.jsonArray

        return content?.mapNotNull { item ->
            val obj = item.jsonObject
            if (obj["type"]?.jsonPrimitive?.content == "text") {
                obj["text"]?.jsonPrimitive?.content
            } else null
        }?.joinToString("\n") ?: "No content"
    }

    private fun sendRequest(method: String, params: JsonObject): JsonObject? {
        val w = writer ?: return null
        val r = reader ?: return null

        val id = ++requestId
        val request = buildJsonObject {
            put("jsonrpc", JsonPrimitive("2.0"))
            put("id", JsonPrimitive(id))
            put("method", JsonPrimitive(method))
            put("params", params)
        }

        val jsonStr = request.toString()
        System.err.println("[Agent] -> $jsonStr")

        w.write(jsonStr)
        w.newLine()
        w.flush()

        // Read response
        val line = r.readLine() ?: return null
        System.err.println("[Agent] <- $line")

        return Json.parseToJsonElement(line).jsonObject
    }

    private fun sendNotification(method: String, params: JsonObject) {
        val w = writer ?: return

        val notification = buildJsonObject {
            put("jsonrpc", JsonPrimitive("2.0"))
            put("method", JsonPrimitive(method))
            put("params", params)
        }

        val jsonStr = notification.toString()
        System.err.println("[Agent] -> $jsonStr")

        w.write(jsonStr)
        w.newLine()
        w.flush()
    }

    fun stop() {
        try {
            reader?.close()
            writer?.close()
            process?.destroy()
            process = null
            System.err.println("[Agent] MCP server stopped")
        } catch (e: Exception) {
            System.err.println("[Agent] Error stopping: ${e.message}")
        }
    }

    private fun findProjectRoot(): File {
        var current = File(System.getProperty("user.dir"))
        while (current.parentFile != null) {
            if (File(current, "jsonplaceholder-mcp").exists()) {
                return current
            }
            if (File(current, "composeApp").exists() && File(current, "settings.gradle.kts").exists()) {
                return current
            }
            current = current.parentFile
        }
        return File(System.getProperty("user.dir"))
    }
}
