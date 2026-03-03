package dev.skrip.aichallenge.data.remote

import dev.skrip.aichallenge.data.remote.dto.CoinMarketData
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * Service for fetching cryptocurrency market data from CoinGecko API
 * Free tier: ~30 requests/minute, no API key required
 */
class CoinGeckoService(private val httpClient: HttpClient) {

    private val baseUrl = "https://api.coingecko.com/api/v3"

    /**
     * Get market data for top coins
     * @param currency Currency for prices (usd, eur, rub)
     * @param limit Number of coins to fetch (max 250)
     */
    suspend fun getMarketData(
        currency: String = "usd",
        limit: Int = 50
    ): Result<List<CoinMarketData>> = runCatching {
        httpClient.get("$baseUrl/coins/markets") {
            parameter("vs_currency", currency)
            parameter("order", "market_cap_desc")
            parameter("per_page", limit)
            parameter("page", 1)
            parameter("sparkline", false)
            parameter("price_change_percentage", "24h,7d")
        }.body()
    }

    /**
     * Format market data as context for AI
     */
    fun formatMarketDataForContext(
        coins: List<CoinMarketData>,
        currencySymbol: String = "$",
        limit: Int = 20
    ): String = buildString {
        appendLine("=== ТЕКУЩИЕ РЫНОЧНЫЕ ДАННЫЕ ===")
        appendLine("Топ-$limit криптовалют по капитализации:")
        appendLine()

        coins.take(limit).forEachIndexed { index, coin ->
            val change24h = coin.priceChangePercent24h?.let {
                val sign = if (it >= 0) "+" else ""
                "$sign%.2f%%".format(it)
            } ?: "N/A"

            val change7d = coin.priceChangePercent7d?.let {
                val sign = if (it >= 0) "+" else ""
                "$sign%.2f%%".format(it)
            } ?: "N/A"

            appendLine("${index + 1}. ${coin.name} (${coin.symbol.uppercase()})")
            appendLine("   Цена: $currencySymbol${formatPrice(coin.currentPrice)}")
            appendLine("   24ч: $change24h | 7д: $change7d")
            appendLine("   Объём 24ч: $currencySymbol${formatVolume(coin.totalVolume)}")
            if (coin.high24h != null && coin.low24h != null) {
                appendLine("   Диапазон 24ч: $currencySymbol${formatPrice(coin.low24h)} - $currencySymbol${formatPrice(coin.high24h)}")
            }
            appendLine()
        }
    }

    private fun formatPrice(price: Double): String {
        return when {
            price >= 1000 -> "%,.0f".format(price)
            price >= 1 -> "%.2f".format(price)
            price >= 0.01 -> "%.4f".format(price)
            else -> "%.6f".format(price)
        }
    }

    private fun formatVolume(volume: Double): String {
        return when {
            volume >= 1_000_000_000 -> "%.2fB".format(volume / 1_000_000_000)
            volume >= 1_000_000 -> "%.2fM".format(volume / 1_000_000)
            volume >= 1_000 -> "%.2fK".format(volume / 1_000)
            else -> "%.0f".format(volume)
        }
    }
}
