package dev.skrip.aichallenge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun Day17DemoScreen() {
    val scope = rememberCoroutineScope()
    val mcpClient = remember { McpJsonPlaceholderAgent() }

    var connectionStatus by remember { mutableStateOf("Not connected") }
    var isConnected by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var userQuery by remember { mutableStateOf("Show me post 1") }
    var claudeClient by remember { mutableStateOf<ClaudeApiClient?>(null) }
    var agent by remember { mutableStateOf<JsonPlaceholderAgent?>(null) }
    var apiKeyError by remember { mutableStateOf<String?>(null) }

    // Trace state
    var trace by remember { mutableStateOf(AgentTrace()) }

    DisposableEffect(Unit) {
        onDispose {
            mcpClient.stop()
            claudeClient?.close()
        }
    }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Day 17: MCP + LLM Agent",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Claude calls MCP tools to fetch data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Connection status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) Color(0xFF4CAF50) else Color(0xFFFF5722))
                    )
                    Text(
                        text = connectionStatus,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (!isConnected) {
                        Button(
                            onClick = {
                                scope.launch {
                                    connectionStatus = "Connecting..."
                                    apiKeyError = null
                                    val mcpSuccess = mcpClient.start()
                                    if (!mcpSuccess) {
                                        connectionStatus = "MCP failed"
                                        apiKeyError = mcpClient.lastError ?: "Unknown MCP error"
                                        return@launch
                                    }
                                    try {
                                        val client = ClaudeApiClient()
                                        claudeClient = client
                                        agent = JsonPlaceholderAgent(mcpClient, client)
                                        connectionStatus = "Connected"
                                        isConnected = true
                                    } catch (e: IllegalStateException) {
                                        connectionStatus = "API key missing"
                                        apiKeyError = e.message
                                    }
                                }
                            },
                            enabled = !isLoading,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Connect", fontSize = 12.sp)
                        }
                    }
                }
            }

            if (apiKeyError != null) {
                Text(
                    text = "Set ANTHROPIC_API_KEY environment variable",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Query input row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = userQuery,
                    onValueChange = { userQuery = it },
                    label = { Text("Your query") },
                    modifier = Modifier.weight(1f),
                    enabled = isConnected && !isLoading,
                    singleLine = true
                )
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            trace = AgentTrace()

                            try {
                                agent?.processQuery(userQuery) { updatedTrace ->
                                    trace = updatedTrace
                                }
                            } catch (e: Exception) {
                                trace.addStep(TraceStep("Error", e.message ?: "Unknown error", StepStatus.ERROR))
                            }

                            isLoading = false
                        }
                    },
                    enabled = isConnected && !isLoading && userQuery.isNotBlank()
                ) {
                    Text(if (isLoading) "Processing..." else "Ask Claude")
                }
            }

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main content area
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left column: Execution Trace
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    SectionCard(
                        title = "Execution Trace",
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (trace.steps.isEmpty()) {
                            Text(
                                text = "Steps will appear here when you ask Claude...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                trace.steps.forEachIndexed { index, step ->
                                    TraceStepRow(index + 1, step)
                                }
                            }
                        }
                    }
                }

                // Middle column: Tool Call + Tool Result
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tool Call
                    SectionCard(
                        title = "Tool Call",
                        modifier = Modifier.weight(1f)
                    ) {
                        val toolCall = trace.toolCall
                        if (toolCall != null) {
                            Column {
                                LabelValue("Tool", toolCall.toolName)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Arguments:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF1E1E1E), RoundedCornerShape(4.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = toolCall.argumentsPretty,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF9CDCFE)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "No tool called yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Tool Result
                    SectionCard(
                        title = "Tool Result (from MCP)",
                        modifier = Modifier.weight(1f)
                    ) {
                        val toolResult = trace.toolResult
                        if (toolResult != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = toolResult,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            Text(
                                text = "No result yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Right column: Final Response
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    SectionCard(
                        title = "Final Response (Claude)",
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val finalResponse = trace.finalResponse
                        if (finalResponse != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = finalResponse,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            Text(
                                text = "Waiting for Claude's response...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Flow diagram
            Text(
                text = "Flow: User → Claude API → tool_use → MCP Tool → tool_result → Claude → Final Response",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                    .padding(10.dp)
            ) {
                Column { content() }
            }
        }
    }
}

@Composable
private fun TraceStepRow(index: Int, step: TraceStep) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Status indicator
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(
                    when (step.status) {
                        StepStatus.DONE -> Color(0xFF4CAF50)
                        StepStatus.RUNNING -> Color(0xFF2196F3)
                        StepStatus.ERROR -> Color(0xFFF44336)
                        StepStatus.PENDING -> Color(0xFF9E9E9E)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$index",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontSize = 10.sp
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = step.details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }

        // Status text
        Text(
            text = when (step.status) {
                StepStatus.DONE -> "Done"
                StepStatus.RUNNING -> "Running"
                StepStatus.ERROR -> "Error"
                StepStatus.PENDING -> "Pending"
            },
            style = MaterialTheme.typography.labelSmall,
            color = when (step.status) {
                StepStatus.DONE -> Color(0xFF4CAF50)
                StepStatus.RUNNING -> Color(0xFF2196F3)
                StepStatus.ERROR -> Color(0xFFF44336)
                StepStatus.PENDING -> Color(0xFF9E9E9E)
            },
            fontSize = 10.sp
        )
    }
}

@Composable
private fun LabelValue(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF4FC3F7)
        )
    }
}
