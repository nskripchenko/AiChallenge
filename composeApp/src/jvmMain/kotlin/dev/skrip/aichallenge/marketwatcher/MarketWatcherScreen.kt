package dev.skrip.aichallenge.marketwatcher

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val SYMBOLS = listOf("BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT")

@Composable
fun MarketWatcherScreen() {
    val scope = rememberCoroutineScope()

    var isConnected by remember { mutableStateOf(false) }
    var isWatcherRunning by remember { mutableStateOf(false) }
    var currentSymbolIndex by remember { mutableStateOf(0) }
    var totalSnapshots by remember { mutableStateOf(0) }

    val events = remember { mutableStateListOf<String>() }
    val recommendations = remember { mutableStateListOf<Pair<String, TradingRecommendation>>() }
    var isLoading by remember { mutableStateOf(false) }

    val mcpClient = remember {
        McpClient { event ->
            events.add(0, "[${timestamp()}] $event")
            if (events.size > 100) events.removeLast()
        }
    }

    val claudeAgent = remember {
        ClaudeAgent { event ->
            events.add(0, "[${timestamp()}] $event")
            if (events.size > 100) events.removeLast()
        }
    }

    // Auto-cycle through symbols and get recommendations
    LaunchedEffect(isConnected, isWatcherRunning) {
        if (!isConnected || !isWatcherRunning) return@LaunchedEffect

        // Wait for initial data collection
        delay(5000)

        while (isConnected && isWatcherRunning) {
            val symbol = SYMBOLS[currentSymbolIndex % SYMBOLS.size]

            // Update total snapshots
            var total = 0
            for (s in SYMBOLS) {
                val status = mcpClient.getWatchStatus(s)
                total += status?.get("snapshotsCount")?.toString()?.toIntOrNull() ?: 0
            }
            totalSnapshots = total

            // Get summary for current symbol
            val status = mcpClient.getWatchStatus(symbol)
            val count = status?.get("snapshotsCount")?.toString()?.toIntOrNull() ?: 0

            if (count >= 2) {
                events.add(0, "[${timestamp()}] Analyzing $symbol...")
                val summary = mcpClient.getMarketSummary(symbol)
                if (summary != null) {
                    val rec = claudeAgent.analyzeMarket(summary)
                    recommendations.add(0, symbol to rec)
                    if (recommendations.size > 10) recommendations.removeLast()
                    events.add(0, "[${timestamp()}] $symbol → ${rec.action.uppercase()}")
                }
            }

            currentSymbolIndex++
            delay(5000)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mcpClient.disconnect()
            claudeAgent.close()
        }
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Control Panel
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Market Watcher Control",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF89B4FA)
                            )
                            Spacer(Modifier.height(8.dp))

                            Text(
                                "Symbols: ${SYMBOLS.joinToString(", ")}",
                                color = Color(0xFFBAC2DE),
                                fontSize = 13.sp
                            )
                            Text(
                                "Interval: 15s per symbol | Summary: every 5s rotating",
                                color = Color(0xFF6C7086),
                                fontSize = 12.sp
                            )

                            Spacer(Modifier.height(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            isLoading = true
                                            if (!isConnected) {
                                                isConnected = mcpClient.connect()
                                            }
                                            if (isConnected) {
                                                // Start watchers for all symbols
                                                for (symbol in SYMBOLS) {
                                                    mcpClient.startWatch(symbol, "1m", 15)
                                                    events.add(0, "[${timestamp()}] Started watcher: $symbol")
                                                    delay(200)
                                                }
                                                isWatcherRunning = true
                                            }
                                            isLoading = false
                                        }
                                    },
                                    enabled = !isLoading && !isWatcherRunning,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF94E2D5))
                                ) {
                                    Text("Start All Watchers", color = Color.Black)
                                }

                                Button(
                                    onClick = {
                                        scope.launch {
                                            isLoading = true
                                            for (symbol in SYMBOLS) {
                                                mcpClient.stopWatch(symbol)
                                            }
                                            events.add(0, "[${timestamp()}] All watchers stopped")
                                            isWatcherRunning = false
                                            isLoading = false
                                        }
                                    },
                                    enabled = !isLoading && isWatcherRunning,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF38BA8))
                                ) {
                                    Text("Stop All", color = Color.Black)
                                }
                            }
                        }
                    }

                    // Status
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Status",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF89B4FA)
                            )
                            Spacer(Modifier.height(8.dp))
                            StatusRow("MCP Server", if (isConnected) "Connected" else "Disconnected", isConnected)
                            StatusRow("Watchers", if (isWatcherRunning) "${SYMBOLS.size} Running" else "Stopped", isWatcherRunning)
                            StatusRow("Total Snapshots", totalSnapshots.toString(), totalSnapshots > 0)
                            StatusRow("Recommendations", recommendations.size.toString(), recommendations.isNotEmpty())
                            StatusRow("Storage", "SQLite", true)
                        }
                    }

                    // Activity Log
                    Card(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Live Activity",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF89B4FA)
                            )
                            Spacer(Modifier.height(8.dp))

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF11111B), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                items(events) { event ->
                                    Text(
                                        event,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = when {
                                            event.contains("ERROR") -> Color(0xFFF38BA8)
                                            event.contains("BUY", ignoreCase = true) -> Color(0xFFA6E3A1)
                                            event.contains("WAIT", ignoreCase = true) -> Color(0xFFF9E2AF)
                                            event.contains("Started") -> Color(0xFF94E2D5)
                                            event.contains("Claude") -> Color(0xFFCBA6F7)
                                            else -> Color(0xFFCDD6F4)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Right column: Recommendations
                Card(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Trading Recommendations",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF89B4FA)
                        )
                        Spacer(Modifier.height(12.dp))

                        if (recommendations.isEmpty()) {
                            Text(
                                "Waiting for data...\nRecommendations will appear automatically.",
                                color = Color(0xFF6C7086)
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(recommendations) { (symbol, rec) ->
                                    RecommendationCard(symbol, rec)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(symbol: String, rec: TradingRecommendation) {
    val actionColor = when (rec.action.lowercase()) {
        "buy" -> Color(0xFFA6E3A1)
        "sell" -> Color(0xFFF38BA8)
        "wait", "no-trade" -> Color(0xFFF9E2AF)
        else -> Color(0xFFCDD6F4)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF313244))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    symbol,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Text(
                    rec.action.uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = actionColor
                )
            }
            Spacer(Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (rec.entry != null && rec.entry != "N/A") {
                    Text("Entry: ${rec.entry}", fontSize = 11.sp, color = Color(0xFFBAC2DE))
                }
                if (rec.stopLoss != null && rec.stopLoss != "N/A") {
                    Text("SL: ${rec.stopLoss}", fontSize = 11.sp, color = Color(0xFFF38BA8))
                }
                if (rec.takeProfit != null && rec.takeProfit != "N/A") {
                    Text("TP: ${rec.takeProfit}", fontSize = 11.sp, color = Color(0xFFA6E3A1))
                }
            }

            Text(
                rec.explanation.take(100) + if (rec.explanation.length > 100) "..." else "",
                fontSize = 11.sp,
                color = Color(0xFF6C7086),
                maxLines = 2
            )
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String, isGood: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFF6C7086), fontSize = 13.sp)
        Text(
            value,
            color = if (isGood) Color(0xFFA6E3A1) else Color(0xFFF38BA8),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun timestamp(): String {
    return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
}
