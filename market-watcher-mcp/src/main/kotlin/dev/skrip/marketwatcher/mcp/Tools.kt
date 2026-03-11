package dev.skrip.marketwatcher.mcp

import dev.skrip.marketwatcher.scheduler.WatcherScheduler
import dev.skrip.marketwatcher.storage.Database
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.serialization.json.*

object Tools {

    fun registerTools(server: Server) {
        server.addTool(
            name = "start_market_watch",
            description = "Start watching a market symbol. Scheduler will periodically fetch and save market data."
        ) { request ->
            val args = request.arguments
            handleStartMarketWatch(args)
        }

        server.addTool(
            name = "stop_market_watch",
            description = "Stop watching a market symbol."
        ) { request ->
            val args = request.arguments
            handleStopMarketWatch(args)
        }

        server.addTool(
            name = "get_market_summary",
            description = "Get aggregated market summary from collected snapshots."
        ) { request ->
            val args = request.arguments
            handleGetMarketSummary(args)
        }

        server.addTool(
            name = "get_watch_status",
            description = "Get current status of market watcher for a symbol."
        ) { request ->
            val args = request.arguments
            handleGetWatchStatus(args)
        }
    }

    private fun handleStartMarketWatch(args: JsonObject?): CallToolResult {
        val symbol = args?.get("symbol")?.jsonPrimitive?.content ?: return errorResult("Missing symbol")
        val timeframe = args?.get("timeframe")?.jsonPrimitive?.content ?: return errorResult("Missing timeframe")
        val intervalSeconds = args?.get("intervalSeconds")?.jsonPrimitive?.int ?: return errorResult("Missing intervalSeconds")

        Database.createOrUpdateJob(symbol, timeframe, intervalSeconds)
        WatcherScheduler.startWatchJob(symbol, timeframe, intervalSeconds)

        return CallToolResult(
            content = listOf(TextContent(buildJsonObject {
                put("status", JsonPrimitive("started"))
                put("symbol", JsonPrimitive(symbol))
                put("timeframe", JsonPrimitive(timeframe))
                put("intervalSeconds", JsonPrimitive(intervalSeconds))
                put("active", JsonPrimitive(true))
                put("message", JsonPrimitive("Market watcher started for $symbol"))
            }.toString()))
        )
    }

    private fun handleStopMarketWatch(args: JsonObject?): CallToolResult {
        val symbol = args?.get("symbol")?.jsonPrimitive?.content ?: return errorResult("Missing symbol")

        Database.deactivateJob(symbol)
        WatcherScheduler.stopWatchJob(symbol)

        return CallToolResult(
            content = listOf(TextContent(buildJsonObject {
                put("status", JsonPrimitive("stopped"))
                put("symbol", JsonPrimitive(symbol))
                put("active", JsonPrimitive(false))
                put("message", JsonPrimitive("Market watcher stopped for $symbol"))
            }.toString()))
        )
    }

    private fun handleGetMarketSummary(args: JsonObject?): CallToolResult {
        val symbol = args?.get("symbol")?.jsonPrimitive?.content ?: return errorResult("Missing symbol")

        val job = Database.getJob(symbol)
        val snapshots = Database.getSnapshots(symbol, 50)
        val count = Database.getSnapshotCount(symbol)

        if (snapshots.isEmpty()) {
            return CallToolResult(
                content = listOf(TextContent(buildJsonObject {
                    put("symbol", JsonPrimitive(symbol))
                    put("snapshotsCount", JsonPrimitive(0))
                    put("message", JsonPrimitive("No snapshots collected yet for $symbol"))
                }.toString()))
            )
        }

        val lastSnapshot = snapshots.last()
        val minPrice = snapshots.minOf { it.lowPrice }
        val maxPrice = snapshots.maxOf { it.highPrice }
        val avgVolume = snapshots.map { it.volume }.average()

        val firstPrice = snapshots.first().closePrice
        val lastPrice = lastSnapshot.closePrice
        val priceChange = lastPrice - firstPrice
        val priceChangePercent = if (firstPrice > 0) (priceChange / firstPrice) * 100 else 0.0

        val trend = when {
            priceChangePercent > 1.0 -> "bullish"
            priceChangePercent < -1.0 -> "bearish"
            else -> "sideways"
        }

        val summaryText = buildString {
            append("$symbol market summary: ")
            append("Current price: ${"%.2f".format(lastPrice)}, ")
            append("Range: ${"%.2f".format(minPrice)} - ${"%.2f".format(maxPrice)}, ")
            append("Change: ${"%.2f".format(priceChangePercent)}%, ")
            append("Trend: $trend, ")
            append("Based on $count snapshots.")
        }

        return CallToolResult(
            content = listOf(TextContent(buildJsonObject {
                put("symbol", JsonPrimitive(symbol))
                put("timeframe", JsonPrimitive(job?.timeframe ?: "unknown"))
                put("snapshotsCount", JsonPrimitive(count))
                put("lastPrice", JsonPrimitive(lastPrice))
                put("minPrice", JsonPrimitive(minPrice))
                put("maxPrice", JsonPrimitive(maxPrice))
                put("avgVolume", JsonPrimitive(avgVolume))
                put("priceChange", JsonPrimitive(priceChange))
                put("priceChangePercent", JsonPrimitive(priceChangePercent))
                put("trend", JsonPrimitive(trend))
                put("summary", JsonPrimitive(summaryText))
            }.toString()))
        )
    }

    private fun handleGetWatchStatus(args: JsonObject?): CallToolResult {
        val symbol = args?.get("symbol")?.jsonPrimitive?.content ?: return errorResult("Missing symbol")

        val job = Database.getJob(symbol)
        val isActive = WatcherScheduler.isJobActive(symbol)
        val snapshotCount = Database.getSnapshotCount(symbol)

        return CallToolResult(
            content = listOf(TextContent(buildJsonObject {
                put("symbol", JsonPrimitive(symbol))
                put("exists", JsonPrimitive(job != null))
                put("isActive", JsonPrimitive(isActive))
                put("timeframe", JsonPrimitive(job?.timeframe ?: ""))
                put("intervalSeconds", JsonPrimitive(job?.intervalSeconds ?: 0))
                put("lastRunAt", JsonPrimitive(job?.lastRunAt ?: ""))
                put("snapshotsCount", JsonPrimitive(snapshotCount))
            }.toString()))
        )
    }

    private fun errorResult(message: String): CallToolResult {
        return CallToolResult(
            content = listOf(TextContent(buildJsonObject {
                put("error", JsonPrimitive(message))
            }.toString())),
            isError = true
        )
    }
}
