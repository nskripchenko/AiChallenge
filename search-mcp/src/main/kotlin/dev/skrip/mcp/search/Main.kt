package dev.skrip.mcp.search

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class Post(val id: Int, val userId: Int, val title: String, val body: String)

fun main() = runBlocking {
    val server = SearchMcpServer()
    server.run()
}

class SearchMcpServer {
    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun run() {
        log("search-mcp server starting...")
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

    private suspend fun handleRequest(request: JsonObject): JsonObject? {
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
                        put("name", "search-mcp")
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
                            put("name", "search_posts")
                            put("description", "Search posts from JSONPlaceholder by query. Returns matching posts with id, title, and body.")
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
                    })
                })
            }
            "tools/call" -> handleToolCall(id, request["params"]?.jsonObject)
            else -> null
        }
    }

    private suspend fun handleToolCall(id: JsonElement?, params: JsonObject?): JsonObject {
        val toolName = params?.get("name")?.jsonPrimitive?.content ?: ""
        val args = params?.get("arguments")?.jsonObject ?: buildJsonObject {}
        log("Tool call: $toolName")

        val result = when (toolName) {
            "search_posts" -> searchPosts(args["query"]?.jsonPrimitive?.content ?: "")
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

    private suspend fun searchPosts(query: String): String {
        log("Searching posts with query: $query")
        val posts: List<Post> = httpClient.get("https://jsonplaceholder.typicode.com/posts").body()
        val filtered = posts.filter {
            it.title.contains(query, ignoreCase = true) || it.body.contains(query, ignoreCase = true)
        }
        return buildJsonObject {
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
        }.toString()
    }

    private fun log(msg: String) = System.err.println("[search-mcp] $msg")
}
