package dev.skrip.aichallenge.marketwatcher

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

class ClaudeAgent(
    private val onEvent: (String) -> Unit
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val apiKey: String?
        get() = System.getenv("ANTHROPIC_API_KEY")

    suspend fun analyzeMarket(summary: JsonObject): TradingRecommendation {
        val key = apiKey
        if (key.isNullOrBlank()) {
            onEvent("ERROR: ANTHROPIC_API_KEY not set")
            return TradingRecommendation(
                action = "error",
                entry = null,
                stopLoss = null,
                takeProfit = null,
                confidence = "0%",
                explanation = "ANTHROPIC_API_KEY environment variable is not set. Please set it and restart the application."
            )
        }

        onEvent("Sending market summary to Claude...")

        val summaryText = summary["summary"]?.jsonPrimitive?.content ?: summary.toString()
        val symbol = summary["symbol"]?.jsonPrimitive?.content ?: "unknown"
        val lastPrice = summary["lastPrice"]?.jsonPrimitive?.doubleOrNull ?: 0.0
        val trend = summary["trend"]?.jsonPrimitive?.content ?: "unknown"

        val userMessage = """
            Analyze this market data and provide a trading recommendation:

            Symbol: $symbol
            Current Price: $lastPrice
            Trend: $trend
            Summary: $summaryText

            Full data: ${summary}
        """.trimIndent()

        return try {
            val response = client.post("https://api.anthropic.com/v1/messages") {
                contentType(ContentType.Application.Json)
                header("x-api-key", key)
                header("anthropic-version", "2023-06-01")
                setBody(ClaudeRequest(
                    model = "claude-sonnet-4-20250514",
                    max_tokens = 1024,
                    system = SYSTEM_PROMPT,
                    messages = listOf(Message("user", userMessage))
                ))
            }

            val claudeResponse = response.body<ClaudeResponse>()
            val text = claudeResponse.content.firstOrNull()?.text ?: ""
            onEvent("Claude analyzed the market data")
            parseRecommendation(text)
        } catch (e: Exception) {
            onEvent("ERROR: Claude API call failed: ${e.message}")
            TradingRecommendation(
                action = "error",
                entry = null,
                stopLoss = null,
                takeProfit = null,
                confidence = "0%",
                explanation = "Failed to get recommendation: ${e.message}"
            )
        }
    }

    private fun parseRecommendation(text: String): TradingRecommendation {
        val lines = text.lines()
        var action = "wait"
        var entry: String? = null
        var stopLoss: String? = null
        var takeProfit: String? = null
        var confidence = "50%"
        val explanationLines = mutableListOf<String>()
        var inExplanation = false

        for (line in lines) {
            val lower = line.lowercase().trim()
            when {
                lower.startsWith("action:") -> {
                    action = line.substringAfter(":").trim().lowercase()
                }
                lower.startsWith("entry:") -> {
                    entry = line.substringAfter(":").trim()
                }
                lower.startsWith("stop loss:") || lower.startsWith("stoploss:") -> {
                    stopLoss = line.substringAfter(":").trim()
                }
                lower.startsWith("take profit:") || lower.startsWith("takeprofit:") -> {
                    takeProfit = line.substringAfter(":").trim()
                }
                lower.startsWith("confidence:") -> {
                    confidence = line.substringAfter(":").trim()
                }
                lower.startsWith("explanation:") -> {
                    inExplanation = true
                    val rest = line.substringAfter(":").trim()
                    if (rest.isNotBlank()) explanationLines.add(rest)
                }
                inExplanation -> {
                    explanationLines.add(line)
                }
            }
        }

        return TradingRecommendation(
            action = action,
            entry = entry,
            stopLoss = stopLoss,
            takeProfit = takeProfit,
            confidence = confidence,
            explanation = explanationLines.joinToString(" ").trim().ifBlank { text }
        )
    }

    fun close() {
        client.close()
    }

    companion object {
        private val SYSTEM_PROMPT = """
            You are an experienced market analyst specializing in short-term technical analysis.
            You analyze market summaries based on candle data, short-term trend, recent price range, and momentum context.
            Your task is to produce a clear and practical trading recommendation.

            Your output MUST include these fields in this exact format:
            Action: [buy / wait / no-trade]
            Entry: [price or N/A]
            Stop Loss: [price or N/A]
            Take Profit: [price or N/A]
            Confidence: [percentage]
            Explanation: [your analysis]

            Rules:
            - Be practical and specific
            - Avoid hype and exaggerated certainty
            - If the data is unclear or insufficient, return "wait" or "no-trade"
            - If action is wait or no-trade, set Entry, Stop Loss, and Take Profit to N/A
            - Keep explanation clear and concise (2-4 sentences)
        """.trimIndent()
    }
}

@Serializable
data class ClaudeRequest(
    val model: String,
    val max_tokens: Int,
    val system: String,
    val messages: List<Message>
)

@Serializable
data class Message(
    val role: String,
    val content: String
)

@Serializable
data class ClaudeResponse(
    val content: List<ContentBlock>
)

@Serializable
data class ContentBlock(
    val type: String,
    val text: String? = null
)

data class TradingRecommendation(
    val action: String,
    val entry: String?,
    val stopLoss: String?,
    val takeProfit: String?,
    val confidence: String,
    val explanation: String
)
