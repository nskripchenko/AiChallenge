package dev.skrip.marketwatcher

import dev.skrip.marketwatcher.mcp.Tools
import dev.skrip.marketwatcher.scheduler.WatcherScheduler
import dev.skrip.marketwatcher.storage.Database
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered

fun main() {
    System.err.println("[MarketWatcher] Initializing MCP server...")

    Database.init()
    System.err.println("[MarketWatcher] Database initialized")

    WatcherScheduler.start()
    System.err.println("[MarketWatcher] Scheduler started")

    val server = Server(
        serverInfo = Implementation(
            name = "market-watcher-mcp",
            version = "1.0.0"
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true)
            )
        )
    )

    Tools.registerTools(server)

    Runtime.getRuntime().addShutdownHook(Thread {
        System.err.println("[MarketWatcher] Shutting down...")
        WatcherScheduler.shutdown()
    })

    runBlocking {
        val transport = StdioServerTransport(
            inputStream = System.`in`.asSource().buffered(),
            outputStream = System.out.asSink().buffered()
        )
        System.err.println("[MarketWatcher] Starting stdio transport...")
        server.connect(transport)

        val done = Job()
        server.onClose { done.complete() }
        done.join()
    }
}
