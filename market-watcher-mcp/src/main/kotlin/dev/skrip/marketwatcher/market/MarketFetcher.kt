package dev.skrip.marketwatcher.market

import dev.skrip.marketwatcher.storage.MarketSnapshot
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*
import java.time.Instant

object MarketFetcher {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private const val BINANCE_API = "https://api.binance.com/api/v3"

    suspend fun fetchKline(symbol: String, timeframe: String): MarketSnapshot? {
        return try {
            val response = client.get("$BINANCE_API/klines") {
                parameter("symbol", symbol)
                parameter("interval", timeframe)
                parameter("limit", 1)
            }

            val klines = response.body<JsonArray>()
            if (klines.isEmpty()) return null

            val kline = klines[0].jsonArray
            MarketSnapshot(
                symbol = symbol,
                timeframe = timeframe,
                snapshotTime = Instant.now().toString(),
                openPrice = kline[1].jsonPrimitive.content.toDouble(),
                highPrice = kline[2].jsonPrimitive.content.toDouble(),
                lowPrice = kline[3].jsonPrimitive.content.toDouble(),
                closePrice = kline[4].jsonPrimitive.content.toDouble(),
                volume = kline[5].jsonPrimitive.content.toDouble()
            )
        } catch (e: Exception) {
            System.err.println("Error fetching market data: ${e.message}")
            null
        }
    }

    fun close() {
        client.close()
    }
}
