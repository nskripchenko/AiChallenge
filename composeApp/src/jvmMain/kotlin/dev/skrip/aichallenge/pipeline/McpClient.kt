package dev.skrip.aichallenge.pipeline

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File

data class McpTool(
    val name: String,
    val description: String,
    val inputSchema: JsonObject
)

class McpClient(private val serverJarPath: String) {
    private var process: Process? = null
    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null
    private var requestId = 0
    var lastError: String? = null
        private set

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    suspend fun start(): Boolean = withContext(Dispatchers.IO) {
        try {
            val jarFile = File(serverJarPath)
            if (!jarFile.exists()) {
                lastError = "MCP JAR not found: $serverJarPath"
                System.err.println(lastError)
                return@withContext false
            }

            System.err.println("Starting MCP server from: $serverJarPath")

            val processBuilder = ProcessBuilder("java", "-jar", serverJarPath)
            processBuilder.redirectErrorStream(false)

            // Set working directory to JAR's parent for pipeline-output
            processBuilder.directory(jarFile.parentFile.parentFile.parentFile)

            process = processBuilder.start()

            // Read stderr in background for debugging
            Thread {
                process?.errorStream?.bufferedReader()?.forEachLine { line ->
                    System.err.println("[MCP Server] $line")
                }
            }.start()

            writer = process!!.outputStream.bufferedWriter()
            reader = process!!.inputStream.bufferedReader()

            // Check if process is alive
            Thread.sleep(100)
            if (process?.isAlive != true) {
                lastError = "MCP process died immediately"
                System.err.println(lastError)
                return@withContext false
            }

            val initResult = initialize()
            val success = initResult["result"] != null
            if (!success) {
                lastError = "MCP initialization failed: $initResult"
                System.err.println(lastError)
            }
            success
        } catch (e: Exception) {
            lastError = "Failed to start MCP: ${e.message}"
            System.err.println(lastError)
            e.printStackTrace()
            false
        }
    }

    private suspend fun initialize(): JsonObject = withContext(Dispatchers.IO) {
        val request = buildJsonObject {
            put("jsonrpc", "2.0")
            put("id", ++requestId)
            put("method", "initialize")
            put("params", buildJsonObject {
                put("protocolVersion", "2024-11-05")
                put("capabilities", buildJsonObject {})
                put("clientInfo", buildJsonObject {
                    put("name", "pipeline-desktop-client")
                    put("version", "1.0.0")
                })
            })
        }
        val response = sendRequest(request)

        // Send initialized notification
        val notification = buildJsonObject {
            put("jsonrpc", "2.0")
            put("method", "notifications/initialized")
        }
        val notifStr = json.encodeToString(JsonObject.serializer(), notification)
        writer?.write(notifStr)
        writer?.newLine()
        writer?.flush()

        response
    }

    suspend fun listTools(): List<McpTool> = withContext(Dispatchers.IO) {
        val request = buildJsonObject {
            put("jsonrpc", "2.0")
            put("id", ++requestId)
            put("method", "tools/list")
        }

        val response = sendRequest(request)
        val tools = response["result"]?.jsonObject?.get("tools")?.jsonArray ?: return@withContext emptyList()

        tools.map { toolElement ->
            val tool = toolElement.jsonObject
            McpTool(
                name = tool["name"]?.jsonPrimitive?.content ?: "",
                description = tool["description"]?.jsonPrimitive?.content ?: "",
                inputSchema = tool["inputSchema"]?.jsonObject ?: buildJsonObject {}
            )
        }
    }

    suspend fun callTool(name: String, arguments: Map<String, String>): String = withContext(Dispatchers.IO) {
        val request = buildJsonObject {
            put("jsonrpc", "2.0")
            put("id", ++requestId)
            put("method", "tools/call")
            put("params", buildJsonObject {
                put("name", name)
                put("arguments", buildJsonObject {
                    arguments.forEach { (key, value) ->
                        put(key, value)
                    }
                })
            })
        }

        val response = sendRequest(request)
        val content = response["result"]?.jsonObject?.get("content")?.jsonArray
        content?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content ?: "No result"
    }

    private suspend fun sendRequest(request: JsonObject): JsonObject = withContext(Dispatchers.IO) {
        try {
            val requestStr = json.encodeToString(JsonObject.serializer(), request)
            System.err.println("[McpClient] Sending: $requestStr")
            writer?.write(requestStr)
            writer?.newLine()
            writer?.flush()

            // Read response, skip empty lines
            var responseLine: String?
            do {
                responseLine = reader?.readLine()
                System.err.println("[McpClient] Received: $responseLine")
            } while (responseLine != null && responseLine.isBlank())

            if (responseLine.isNullOrBlank()) {
                System.err.println("[McpClient] Empty response")
                return@withContext buildJsonObject {}
            }

            json.parseToJsonElement(responseLine).jsonObject
        } catch (e: Exception) {
            System.err.println("[McpClient] Error in sendRequest: ${e.message}")
            buildJsonObject {
                put("error", e.message ?: "Unknown error")
            }
        }
    }

    fun stop() {
        try {
            writer?.close()
            reader?.close()
            process?.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
