package dev.skrip.mcp.summary

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*

fun main() = runBlocking {
    val server = SummaryMcpServer()
    server.run()
}

class SummaryMcpServer {
    private val json = Json { ignoreUnknownKeys = true }

    fun run() {
        log("summary-mcp server starting...")
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
                        put("name", "summary-mcp")
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
                            put("name", "summarize_posts")
                            put("description", "Create a summary from posts JSON. Returns summary text and count.")
                            put("inputSchema", buildJsonObject {
                                put("type", "object")
                                put("properties", buildJsonObject {
                                    put("postsJson", buildJsonObject {
                                        put("type", "string")
                                        put("description", "JSON string with posts to summarize")
                                    })
                                })
                                put("required", buildJsonArray { add("postsJson") })
                            })
                        })
                        add(buildJsonObject {
                            put("name", "extract_keywords")
                            put("description", "Extract keywords from text. Returns array of keywords.")
                            put("inputSchema", buildJsonObject {
                                put("type", "object")
                                put("properties", buildJsonObject {
                                    put("text", buildJsonObject {
                                        put("type", "string")
                                        put("description", "Text to extract keywords from")
                                    })
                                })
                                put("required", buildJsonArray { add("text") })
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
            "summarize_posts" -> summarizePosts(args["postsJson"]?.jsonPrimitive?.content ?: "{}")
            "extract_keywords" -> extractKeywords(args["text"]?.jsonPrimitive?.content ?: "")
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

    private fun summarizePosts(postsJsonStr: String): String {
        log("Summarizing posts...")
        return try {
            val data = json.parseToJsonElement(postsJsonStr).jsonObject
            val posts = data["posts"]?.jsonArray ?: JsonArray(emptyList())
            val count = data["count"]?.jsonPrimitive?.int ?: posts.size
            val query = data["query"]?.jsonPrimitive?.content ?: "unknown"

            val wordCounts = mutableMapOf<String, Int>()
            posts.forEach { p ->
                val post = p.jsonObject
                val text = "${post["title"]?.jsonPrimitive?.content ?: ""} ${post["body"]?.jsonPrimitive?.content ?: ""}"
                text.lowercase().replace(Regex("[^a-z\\s]"), "").split(Regex("\\s+"))
                    .filter { it.length > 3 }
                    .forEach { wordCounts[it] = (wordCounts[it] ?: 0) + 1 }
            }
            val topWords = wordCounts.entries.sortedByDescending { it.value }.take(5).map { it.key }

            val summaryText = buildString {
                appendLine("=== Posts Summary ===")
                appendLine("Query: \"$query\"")
                appendLine("Total posts: $count")
                appendLine()
                appendLine("Main themes: ${topWords.joinToString(", ")}")
                appendLine()
                appendLine("Highlights:")
                posts.take(3).forEachIndexed { i, p ->
                    val title = p.jsonObject["title"]?.jsonPrimitive?.content ?: ""
                    appendLine("${i + 1}. $title")
                }
                if (count > 3) appendLine("... and ${count - 3} more")
            }

            buildJsonObject {
                put("summaryText", summaryText)
                put("count", count)
                put("topThemes", buildJsonArray { topWords.forEach { add(it) } })
            }.toString()
        } catch (e: Exception) {
            log("Summarize error: ${e.message}")
            """{"error": "${e.message}"}"""
        }
    }

    private fun extractKeywords(text: String): String {
        log("Extracting keywords...")
        val words = text.lowercase().replace(Regex("[^a-z\\s]"), "").split(Regex("\\s+"))
        val counts = mutableMapOf<String, Int>()
        words.filter { it.length > 3 }.forEach { counts[it] = (counts[it] ?: 0) + 1 }
        val keywords = counts.entries.sortedByDescending { it.value }.take(8).map { it.key }

        return buildJsonObject {
            put("keywords", buildJsonArray { keywords.forEach { add(it) } })
            put("count", keywords.size)
        }.toString()
    }

    private fun log(msg: String) = System.err.println("[summary-mcp] $msg")
}
