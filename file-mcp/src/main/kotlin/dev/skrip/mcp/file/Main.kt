package dev.skrip.mcp.file

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import java.io.File

fun main() = runBlocking {
    val server = FileMcpServer()
    server.run()
}

class FileMcpServer {
    private val json = Json { ignoreUnknownKeys = true }
    private val safeDir = File("pipeline-output")

    fun run() {
        log("file-mcp server starting...")
        safeDir.mkdirs()
        while (true) {
            val line = readlnOrNull() ?: break
            if (line.isBlank()) continue
            try {
                val request = json.parseToJsonElement(line).jsonObject
                val response = handleRequest(request)
                if (response != null) {
                    println(json.encodeToString(JsonElement.serializer(), response))
                    System.out.flush()
                }
            } catch (e: Exception) {
                log("Error: ${e.message}")
            }
        }
    }

    private fun handleRequest(request: JsonObject): JsonObject? {
        val method = request["method"]?.jsonPrimitive?.content ?: ""
        val id = request["id"]
        return when (method) {
            "initialize" -> buildJsonObject {
                put("jsonrpc", "2.0")
                if (id != null) put("id", id)
                put("result", buildJsonObject {
                    put("protocolVersion", "2024-11-05")
                    put("capabilities", buildJsonObject { put("tools", buildJsonObject {}) })
                    put("serverInfo", buildJsonObject {
                        put("name", "file-mcp")
                        put("version", "1.0.0")
                    })
                })
            }
            "notifications/initialized" -> {
                log("Initialized")
                null
            }
            "tools/list" -> buildJsonObject {
                put("jsonrpc", "2.0")
                if (id != null) put("id", id)
                put("result", buildJsonObject {
                    put("tools", buildJsonArray {
                        add(buildJsonObject {
                            put("name", "save_to_file")
                            put("description", "Save content to a file in pipeline-output directory.")
                            put("inputSchema", buildJsonObject {
                                put("type", "object")
                                put("properties", buildJsonObject {
                                    put("filename", buildJsonObject {
                                        put("type", "string")
                                        put("description", "Filename to save")
                                    })
                                    put("content", buildJsonObject {
                                        put("type", "string")
                                        put("description", "Content to save")
                                    })
                                })
                                put("required", buildJsonArray { add("filename"); add("content") })
                            })
                        })
                        add(buildJsonObject {
                            put("name", "read_file")
                            put("description", "Read file content from pipeline-output directory.")
                            put("inputSchema", buildJsonObject {
                                put("type", "object")
                                put("properties", buildJsonObject {
                                    put("filename", buildJsonObject {
                                        put("type", "string")
                                        put("description", "Filename to read")
                                    })
                                })
                                put("required", buildJsonArray { add("filename") })
                            })
                        })
                    })
                })
            }
            "tools/call" -> handleToolCall(id, request["params"]?.jsonObject)
            else -> null
        }
    }

    private fun handleToolCall(id: JsonElement?, params: JsonObject?): JsonObject {
        val toolName = params?.get("name")?.jsonPrimitive?.content ?: ""
        val args = params?.get("arguments")?.jsonObject ?: buildJsonObject {}
        log("Tool call: $toolName")

        val result = when (toolName) {
            "save_to_file" -> saveToFile(
                args["filename"]?.jsonPrimitive?.content ?: "output.txt",
                args["content"]?.jsonPrimitive?.content ?: ""
            )
            "read_file" -> readFile(args["filename"]?.jsonPrimitive?.content ?: "")
            else -> """{"error": "Unknown tool: $toolName"}"""
        }

        return buildJsonObject {
            put("jsonrpc", "2.0")
            if (id != null) put("id", id)
            put("result", buildJsonObject {
                put("content", buildJsonArray {
                    add(buildJsonObject {
                        put("type", "text")
                        put("text", result)
                    })
                })
            })
        }
    }

    private fun saveToFile(filename: String, content: String): String {
        val safeName = filename.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val file = File(safeDir, safeName)
        return try {
            file.writeText(content)
            log("Saved: ${file.absolutePath}")
            buildJsonObject {
                put("success", true)
                put("savedPath", file.absolutePath)
                put("filename", safeName)
                put("bytesWritten", content.length)
            }.toString()
        } catch (e: Exception) {
            log("Save error: ${e.message}")
            """{"success": false, "error": "${e.message}"}"""
        }
    }

    private fun readFile(filename: String): String {
        val safeName = filename.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val file = File(safeDir, safeName)
        return try {
            if (!file.exists()) {
                return """{"success": false, "error": "File not found: $safeName"}"""
            }
            val content = file.readText()
            log("Read: ${file.absolutePath}")
            buildJsonObject {
                put("success", true)
                put("filename", safeName)
                put("content", content)
                put("size", content.length)
            }.toString()
        } catch (e: Exception) {
            log("Read error: ${e.message}")
            """{"success": false, "error": "${e.message}"}"""
        }
    }

    private fun log(msg: String) = System.err.println("[file-mcp] $msg")
}
