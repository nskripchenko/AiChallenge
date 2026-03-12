package dev.skrip.pipeline

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*

fun main() = runBlocking {
    val server = McpServer()
    server.run()
}

class McpServer {
    private val jsonPlaceholderClient = JsonPlaceholderClient()
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false  // Must be single line for stdio transport
    }

    suspend fun run() {
        log("MCP Content Pipeline Server starting...")

        while (true) {
            val line = readlnOrNull() ?: break
            if (line.isBlank()) continue

            try {
                val request = json.parseToJsonElement(line).jsonObject
                val response = handleRequest(request)
                // Don't send response for notifications (no id, returns null)
                if (response != null) {
                    println(json.encodeToString(JsonElement.serializer(), response))
                    System.out.flush()
                }
            } catch (e: Exception) {
                log("Error processing request: ${e.message}")
                val errorResponse = buildJsonObject {
                    put("jsonrpc", "2.0")
                    put("id", null as String?)
                    put("error", buildJsonObject {
                        put("code", -32700)
                        put("message", "Parse error: ${e.message}")
                    })
                }
                println(json.encodeToString(JsonElement.serializer(), errorResponse))
                System.out.flush()
            }
        }
    }

    private suspend fun handleRequest(request: JsonObject): JsonObject? {
        val method = request["method"]?.jsonPrimitive?.content ?: ""
        val id = request["id"]
        val params = request["params"]?.jsonObject

        return when (method) {
            "initialize" -> handleInitialize(id)
            "notifications/initialized" -> {
                log("Received initialized notification")
                null // No response for notifications
            }
            "tools/list" -> handleListTools(id)
            "tools/call" -> handleToolCall(id, params)
            else -> {
                log("Unknown method: $method")
                buildJsonObject {
                    put("jsonrpc", "2.0")
                    if (id != null) put("id", id)
                    put("result", buildJsonObject {})
                }
            }
        }
    }

    private fun handleInitialize(id: JsonElement?): JsonObject {
        log("Initializing server...")
        return buildJsonObject {
            put("jsonrpc", "2.0")
            if (id != null) put("id", id)
            put("result", buildJsonObject {
                put("protocolVersion", "2024-11-05")
                put("capabilities", buildJsonObject {
                    put("tools", buildJsonObject {})
                })
                put("serverInfo", buildJsonObject {
                    put("name", "content-pipeline-mcp")
                    put("version", "1.0.0")
                })
            })
        }
    }

    private fun handleListTools(id: JsonElement?): JsonObject {
        log("Listing tools...")
        return buildJsonObject {
            put("jsonrpc", "2.0")
            if (id != null) put("id", id)
            put("result", buildJsonObject {
                put("tools", buildJsonArray {
                    add(buildJsonObject {
                        put("name", "search_posts")
                        put("description", "Search posts from JSONPlaceholder by query string. Returns matching posts with id, title, and body.")
                        put("inputSchema", buildJsonObject {
                            put("type", "object")
                            put("properties", buildJsonObject {
                                put("query", buildJsonObject {
                                    put("type", "string")
                                    put("description", "Search query to filter posts by title or body")
                                })
                            })
                            put("required", buildJsonArray { add("query") })
                        })
                    })
                    add(buildJsonObject {
                        put("name", "summarize_posts")
                        put("description", "Create a summary from posts JSON. Analyzes the content and returns a structured summary.")
                        put("inputSchema", buildJsonObject {
                            put("type", "object")
                            put("properties", buildJsonObject {
                                put("postsJson", buildJsonObject {
                                    put("type", "string")
                                    put("description", "JSON string containing posts to summarize")
                                })
                            })
                            put("required", buildJsonArray { add("postsJson") })
                        })
                    })
                    add(buildJsonObject {
                        put("name", "save_to_file")
                        put("description", "Save content to a file in the pipeline-output directory.")
                        put("inputSchema", buildJsonObject {
                            put("type", "object")
                            put("properties", buildJsonObject {
                                put("filename", buildJsonObject {
                                    put("type", "string")
                                    put("description", "Filename for the output file (saved in pipeline-output/)")
                                })
                                put("content", buildJsonObject {
                                    put("type", "string")
                                    put("description", "Content to save to the file")
                                })
                            })
                            put("required", buildJsonArray {
                                add("filename")
                                add("content")
                            })
                        })
                    })
                })
            })
        }
    }

    private suspend fun handleToolCall(id: JsonElement?, params: JsonObject?): JsonObject {
        val toolName = params?.get("name")?.jsonPrimitive?.content ?: ""
        val arguments = params?.get("arguments")?.jsonObject ?: buildJsonObject {}

        log("Tool call: $toolName with args: $arguments")

        val result = when (toolName) {
            "search_posts" -> searchPosts(arguments)
            "summarize_posts" -> summarizePosts(arguments)
            "save_to_file" -> saveToFile(arguments)
            else -> "Unknown tool: $toolName"
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

    private suspend fun searchPosts(arguments: JsonObject): String {
        val query = arguments["query"]?.jsonPrimitive?.content ?: ""
        log("Searching posts with query: $query")

        val posts = jsonPlaceholderClient.getPosts()
        val filtered = posts.filter { post ->
            post.title.contains(query, ignoreCase = true) ||
            post.body.contains(query, ignoreCase = true)
        }

        val result = buildJsonObject {
            put("query", query)
            put("count", filtered.size)
            put("posts", buildJsonArray {
                filtered.forEach { post ->
                    add(buildJsonObject {
                        put("id", post.id)
                        put("title", post.title)
                        put("body", post.body)
                    })
                }
            })
        }

        return json.encodeToString(JsonElement.serializer(), result)
    }

    private fun summarizePosts(arguments: JsonObject): String {
        val postsJsonStr = arguments["postsJson"]?.jsonPrimitive?.content ?: "{}"
        log("Summarizing posts...")

        return try {
            val postsData = json.parseToJsonElement(postsJsonStr).jsonObject
            val posts = postsData["posts"]?.jsonArray ?: JsonArray(emptyList())
            val count = postsData["count"]?.jsonPrimitive?.int ?: posts.size
            val query = postsData["query"]?.jsonPrimitive?.content ?: "unknown"

            // Extract words for frequency analysis
            val wordCounts = mutableMapOf<String, Int>()
            posts.forEach { postElement ->
                val post = postElement.jsonObject
                val title = post["title"]?.jsonPrimitive?.content ?: ""
                val body = post["body"]?.jsonPrimitive?.content ?: ""
                val words = (title + " " + body)
                    .lowercase()
                    .replace(Regex("[^a-z\\s]"), "")
                    .split(Regex("\\s+"))
                    .filter { it.length > 3 }

                words.forEach { word ->
                    wordCounts[word] = (wordCounts[word] ?: 0) + 1
                }
            }

            val topWords = wordCounts.entries
                .sortedByDescending { it.value }
                .take(5)
                .map { it.key }

            // Build summary
            val summaryText = buildString {
                appendLine("=== Posts Summary ===")
                appendLine()
                appendLine("Search Query: \"$query\"")
                appendLine("Posts Found: $count")
                appendLine()
                appendLine("Top Themes/Words: ${topWords.joinToString(", ")}")
                appendLine()
                appendLine("--- Post Highlights ---")
                posts.take(3).forEachIndexed { index, postElement ->
                    val post = postElement.jsonObject
                    val id = post["id"]?.jsonPrimitive?.int ?: 0
                    val title = post["title"]?.jsonPrimitive?.content ?: ""
                    appendLine("${index + 1}. [#$id] $title")
                }
                if (count > 3) {
                    appendLine("... and ${count - 3} more posts")
                }
            }

            val result = buildJsonObject {
                put("summaryText", summaryText)
                put("count", count)
                put("query", query)
                put("topThemes", buildJsonArray { topWords.forEach { add(it) } })
            }

            json.encodeToString(JsonElement.serializer(), result)
        } catch (e: Exception) {
            log("Error summarizing: ${e.message}")
            buildJsonObject {
                put("error", "Failed to summarize: ${e.message}")
            }.toString()
        }
    }

    private fun saveToFile(arguments: JsonObject): String {
        val filename = arguments["filename"]?.jsonPrimitive?.content ?: "output.txt"
        val content = arguments["content"]?.jsonPrimitive?.content ?: ""

        // Security: only allow saving in pipeline-output directory
        val safeFilename = filename.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val outputDir = java.io.File("pipeline-output")
        outputDir.mkdirs()

        val outputFile = java.io.File(outputDir, safeFilename)

        return try {
            outputFile.writeText(content)
            log("File saved: ${outputFile.absolutePath}")

            buildJsonObject {
                put("success", true)
                put("savedPath", outputFile.absolutePath)
                put("filename", safeFilename)
                put("bytesWritten", content.length)
            }.toString()
        } catch (e: Exception) {
            log("Error saving file: ${e.message}")
            buildJsonObject {
                put("success", false)
                put("error", e.message)
            }.toString()
        }
    }

    private fun log(message: String) {
        System.err.println("[MCP] $message")
    }
}
