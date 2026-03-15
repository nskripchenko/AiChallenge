package dev.skrip.aichallenge.orchestration

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File

data class McpTool(
    val name: String,
    val description: String,
    val inputSchema: JsonObject,
    val serverName: String
)

class McpClient(
    private val serverName: String,
    private val jarPath: String
) {
    private var process: Process? = null
    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null
    private var requestId = 0
    private val json = Json { ignoreUnknownKeys = true }
    var lastError: String? = null
        private set

    suspend fun start(): Boolean = withContext(Dispatchers.IO) {
        try {
            val jarFile = File(jarPath)
            if (!jarFile.exists()) {
                lastError = "JAR not found: $jarPath"
                return@withContext false
            }
            log("Starting from $jarPath")

            val pb = ProcessBuilder("java", "-jar", jarPath)
            pb.directory(jarFile.parentFile.parentFile.parentFile)
            process = pb.start()

            Thread {
                process?.errorStream?.bufferedReader()?.forEachLine { log(it) }
            }.start()

            writer = process!!.outputStream.bufferedWriter()
            reader = process!!.inputStream.bufferedReader()

            Thread.sleep(100)
            if (process?.isAlive != true) {
                lastError = "Process died"
                return@withContext false
            }

            val initResult = sendRequest(buildJsonObject {
                put("jsonrpc", "2.0")
                put("id", ++requestId)
                put("method", "initialize")
                put("params", buildJsonObject {
                    put("protocolVersion", "2024-11-05")
                    put("capabilities", buildJsonObject {})
                    put("clientInfo", buildJsonObject {
                        put("name", "orchestration-client")
                        put("version", "1.0.0")
                    })
                })
            })

            // Send initialized notification
            val notif = buildJsonObject {
                put("jsonrpc", "2.0")
                put("method", "notifications/initialized")
            }
            writer?.write(json.encodeToString(JsonObject.serializer(), notif))
            writer?.newLine()
            writer?.flush()

            initResult["result"] != null
        } catch (e: Exception) {
            lastError = e.message
            false
        }
    }

    suspend fun listTools(): List<McpTool> = withContext(Dispatchers.IO) {
        val response = sendRequest(buildJsonObject {
            put("jsonrpc", "2.0")
            put("id", ++requestId)
            put("method", "tools/list")
        })
        val tools = response["result"]?.jsonObject?.get("tools")?.jsonArray ?: return@withContext emptyList()
        tools.map { t ->
            val tool = t.jsonObject
            McpTool(
                name = tool["name"]?.jsonPrimitive?.content ?: "",
                description = tool["description"]?.jsonPrimitive?.content ?: "",
                inputSchema = tool["inputSchema"]?.jsonObject ?: buildJsonObject {},
                serverName = serverName
            )
        }
    }

    suspend fun callTool(name: String, arguments: Map<String, String>): String = withContext(Dispatchers.IO) {
        val response = sendRequest(buildJsonObject {
            put("jsonrpc", "2.0")
            put("id", ++requestId)
            put("method", "tools/call")
            put("params", buildJsonObject {
                put("name", name)
                put("arguments", buildJsonObject {
                    arguments.forEach { (k, v) -> put(k, v) }
                })
            })
        })
        response["result"]?.jsonObject?.get("content")?.jsonArray
            ?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content ?: "No result"
    }

    private suspend fun sendRequest(request: JsonObject): JsonObject = withContext(Dispatchers.IO) {
        try {
            val requestStr = json.encodeToString(JsonObject.serializer(), request)
            writer?.write(requestStr)
            writer?.newLine()
            writer?.flush()

            var line: String?
            do {
                line = reader?.readLine()
            } while (line != null && line.isBlank())

            if (line.isNullOrBlank()) return@withContext buildJsonObject {}
            json.parseToJsonElement(line).jsonObject
        } catch (e: Exception) {
            buildJsonObject { put("error", e.message ?: "Unknown error") }
        }
    }

    fun stop() {
        runCatching {
            writer?.close()
            reader?.close()
            process?.destroy()
        }
    }

    private fun log(msg: String) = System.err.println("[$serverName] $msg")
}
