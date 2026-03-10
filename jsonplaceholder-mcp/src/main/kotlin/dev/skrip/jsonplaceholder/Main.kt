package dev.skrip.jsonplaceholder

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.*

private fun log(message: String) {
    System.err.println("[jsonplaceholder-mcp] $message")
}

fun main() = runBlocking {
    log("Starting JSONPlaceholder MCP Server...")

    val server = Server(
        serverInfo = Implementation(
            name = "jsonplaceholder-mcp",
            version = "1.0.0"
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = true))
        )
    )

    server.addTool(
        name = "get_post",
        description = "Get a post by ID from JSONPlaceholder API. Returns userId, id, title, and body.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                put("id", buildJsonObject {
                    put("type", JsonPrimitive("integer"))
                    put("description", JsonPrimitive("The post ID to fetch"))
                })
            },
            required = listOf("id")
        )
    ) { request ->
        val id = request.arguments["id"]?.jsonPrimitive?.intOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent(text = "Error: 'id' parameter is required"))
            )

        log("Fetching post with id=$id")

        try {
            val post = JsonPlaceholderApi.getPost(id)
            val result = buildString {
                appendLine("Post #${post.id}")
                appendLine("User ID: ${post.userId}")
                appendLine("Title: ${post.title}")
                appendLine("Body: ${post.body}")
            }
            log("Successfully fetched post #$id")
            CallToolResult(content = listOf(TextContent(text = result)))
        } catch (e: Exception) {
            log("Error fetching post: ${e.message}")
            CallToolResult(content = listOf(TextContent(text = "Error fetching post: ${e.message}")))
        }
    }

    server.addTool(
        name = "get_user",
        description = "Get a user by ID from JSONPlaceholder API. Returns id, name, email, and company name.",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                put("id", buildJsonObject {
                    put("type", JsonPrimitive("integer"))
                    put("description", JsonPrimitive("The user ID to fetch"))
                })
            },
            required = listOf("id")
        )
    ) { request ->
        val id = request.arguments["id"]?.jsonPrimitive?.intOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent(text = "Error: 'id' parameter is required"))
            )

        log("Fetching user with id=$id")

        try {
            val user = JsonPlaceholderApi.getUser(id)
            val result = buildString {
                appendLine("User #${user.id}")
                appendLine("Name: ${user.name}")
                appendLine("Username: ${user.username}")
                appendLine("Email: ${user.email}")
                appendLine("Company: ${user.company.name}")
            }
            log("Successfully fetched user #$id")
            CallToolResult(content = listOf(TextContent(text = result)))
        } catch (e: Exception) {
            log("Error fetching user: ${e.message}")
            CallToolResult(content = listOf(TextContent(text = "Error fetching user: ${e.message}")))
        }
    }

    log("Tools registered: get_post, get_user")
    log("Connecting via stdio...")

    val transport = StdioServerTransport(
        inputStream = System.`in`.asSource().buffered(),
        outputStream = System.out.asSink().buffered()
    )
    server.connect(transport)

    log("Server connected and ready")

    // Keep server running until stdin is closed
    val job = Job()
    job.join()
}
