package dev.skrip.aichallenge.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CoinMarketData(
    val id: String,
    val symbol: String,
    val name: String,
    @SerialName("current_price")
    val currentPrice: Double,
    @SerialName("market_cap")
    val marketCap: Long,
    @SerialName("market_cap_rank")
    val marketCapRank: Int? = null,
    @SerialName("total_volume")
    val totalVolume: Double,
    @SerialName("price_change_percentage_24h")
    val priceChangePercent24h: Double? = null,
    @SerialName("price_change_percentage_7d_in_currency")
    val priceChangePercent7d: Double? = null,
    @SerialName("high_24h")
    val high24h: Double? = null,
    @SerialName("low_24h")
    val low24h: Double? = null,
    @SerialName("ath")
    val allTimeHigh: Double? = null,
    @SerialName("ath_change_percentage")
    val athChangePercent: Double? = null
)
