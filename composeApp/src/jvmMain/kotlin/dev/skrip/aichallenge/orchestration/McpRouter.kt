package dev.skrip.aichallenge.orchestration

import java.io.File

class McpRouter {
    private val clients = mutableMapOf<String, McpClient>()
    private val toolToServer = mutableMapOf<String, String>()
    private val allTools = mutableListOf<McpTool>()

    suspend fun initialize(projectRoot: File): Boolean {
        val servers = listOf(
            "search-mcp" to "search-mcp/build/libs/search-mcp-1.0.0.jar",
            "summary-mcp" to "summary-mcp/build/libs/summary-mcp-1.0.0.jar",
            "file-mcp" to "file-mcp/build/libs/file-mcp-1.0.0.jar"
        )

        for ((name, jarRelPath) in servers) {
            val jarPath = File(projectRoot, jarRelPath).absolutePath
            val client = McpClient(name, jarPath)
            if (client.start()) {
                clients[name] = client
                val tools = client.listTools()
                allTools.addAll(tools)
                tools.forEach { toolToServer[it.name] = name }
                System.err.println("[Router] $name started with ${tools.size} tools: ${tools.map { it.name }}")
            } else {
                System.err.println("[Router] Failed to start $name: ${client.lastError}")
                return false
            }
        }
        return clients.size == 3
    }

    fun getAllTools(): List<McpTool> = allTools.toList()

    fun getServerForTool(toolName: String): String? = toolToServer[toolName]

    suspend fun callTool(toolName: String, arguments: Map<String, String>): String {
        val serverName = toolToServer[toolName] ?: return """{"error": "Unknown tool: $toolName"}"""
        val client = clients[serverName] ?: return """{"error": "Server not found: $serverName"}"""
        return client.callTool(toolName, arguments)
    }

    fun stop() {
        clients.values.forEach { it.stop() }
        clients.clear()
    }
}
