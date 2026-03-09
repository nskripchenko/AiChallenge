package mcp

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered

/**
 * MCP Server for Project Catalog.
 * Provides tools to inspect Kotlin Multiplatform project structure.
 */
fun main() = runBlocking {
    val server = createServer()
    val transport = StdioServerTransport(
        System.`in`.asSource().buffered(),
        System.out.asSink().buffered()
    )

    // Log to stderr only
    System.err.println("[MCP Server] Starting project-catalog-mcp server...")

    // Setup close handler to know when to exit
    val done = CompletableDeferred<Unit>()
    transport.onClose { done.complete(Unit) }

    server.connect(transport)

    // Keep server running until transport closes
    done.await()

    System.err.println("[MCP Server] Server stopped")
}

private fun createServer(): Server {
    val server = Server(
        serverInfo = Implementation(
            name = "project-catalog-mcp",
            version = "1.0.0"
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = true))
        )
    )

    // Register tools
    server.addTool(
        name = "project_info",
        description = "Get basic information about the project: name, type, main directories",
        inputSchema = Tool.Input()
    ) { _ ->
        val info = ProjectScanner.getProjectInfo()
        CallToolResult(content = listOf(TextContent(info)))
    }

    server.addTool(
        name = "list_modules",
        description = "List all modules/directories in the project, excluding build artifacts",
        inputSchema = Tool.Input()
    ) { _ ->
        val modules = ProjectScanner.listModules()
        CallToolResult(content = listOf(TextContent(modules)))
    }

    server.addTool(
        name = "list_screens",
        description = "Find Compose screens (@Composable functions ending with 'Screen') in the project",
        inputSchema = Tool.Input()
    ) { _ ->
        val screens = ProjectScanner.listScreens()
        CallToolResult(content = listOf(TextContent(screens)))
    }

    return server
}
