package dev.skrip.aichallenge

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class ClaudeRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val messages: List<ClaudeMessage>,
    val tools: List<ClaudeTool>? = null
)

@Serializable
data class ClaudeMessage(
    val role: String,
    val content: JsonElement
)

@Serializable
data class ClaudeTool(
    val name: String,
    val description: String,
    @SerialName("input_schema") val inputSchema: JsonObject
)

@Serializable
data class ClaudeResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ClaudeContentBlock>,
    @SerialName("stop_reason") val stopReason: String? = null
)

@Serializable
data class ClaudeContentBlock(
    val type: String,
    val text: String? = null,
    val id: String? = null,
    val name: String? = null,
    val input: JsonObject? = null
)

class ClaudeApiClient {

    private val apiKey: String = System.getenv("ANTHROPIC_API_KEY")
        ?: throw IllegalStateException("ANTHROPIC_API_KEY environment variable is not set")

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
    }

    private val tools = listOf(
        ClaudeTool(
            name = "get_post",
            description = "Get a post by ID from JSONPlaceholder API. Returns userId, id, title, and body.",
            inputSchema = buildJsonObject {
                put("type", JsonPrimitive("object"))
                put("properties", buildJsonObject {
                    put("id", buildJsonObject {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("The post ID to fetch"))
                    })
                })
                put("required", JsonArray(listOf(JsonPrimitive("id"))))
            }
        ),
        ClaudeTool(
            name = "get_user",
            description = "Get a user by ID from JSONPlaceholder API. Returns id, name, email, and company name.",
            inputSchema = buildJsonObject {
                put("type", JsonPrimitive("object"))
                put("properties", buildJsonObject {
                    put("id", buildJsonObject {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("The user ID to fetch"))
                    })
                })
                put("required", JsonArray(listOf(JsonPrimitive("id"))))
            }
        )
    )

    suspend fun sendMessage(messages: List<ClaudeMessage>): ClaudeResponse {
        val request = ClaudeRequest(
            model = "claude-sonnet-4-20250514",
            maxTokens = 1024,
            messages = messages,
            tools = tools
        )

        return client.post("https://api.anthropic.com/v1/messages") {
            contentType(ContentType.Application.Json)
            header("x-api-key", apiKey)
            header("anthropic-version", "2023-06-01")
            setBody(request)
        }.body()
    }

    fun close() {
        client.close()
    }
}
