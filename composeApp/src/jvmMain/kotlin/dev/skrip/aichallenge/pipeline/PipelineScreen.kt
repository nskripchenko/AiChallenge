package dev.skrip.aichallenge.pipeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun PipelineScreen(mcpServerJarPath: String) {
    val scope = rememberCoroutineScope()

    var userRequest by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<PipelineResult?>(null) }
    var mcpClient by remember { mutableStateOf<McpClient?>(null) }
    var agent by remember { mutableStateOf<PipelineAgent?>(null) }
    var steps by remember { mutableStateOf<List<PipelineStep>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var isConnecting by remember { mutableStateOf(true) }

    val scrollState = rememberScrollState()

    // Initialize MCP client
    LaunchedEffect(mcpServerJarPath) {
        isConnecting = true
        try {
            val client = McpClient(mcpServerJarPath)
            if (client.start()) {
                mcpClient = client
                agent = PipelineAgent(client)
                error = null
            } else {
                error = client.lastError ?: "Failed to start MCP server"
            }
        } catch (e: Exception) {
            error = "Error initializing MCP: ${e.message}"
        }
        isConnecting = false
    }

    // Collect steps
    LaunchedEffect(agent) {
        agent?.steps?.collect { newSteps ->
            steps = newSteps
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(scrollState)
            ) {
                // Header
                Text(
                    text = "Content Pipeline Demo",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Day 19: MCP Tool Composition with Claude Orchestration",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Error display
                error?.let {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF5C1010)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = it,
                            color = Color.White,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // Connection status
                if (isConnecting) {
                    Row(
                        modifier = Modifier.padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Connecting to MCP server...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (mcpClient != null) {
                    Row(
                        modifier = Modifier.padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0xFF4CAF50), RoundedCornerShape(5.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MCP server connected",
                            color = Color(0xFF4CAF50)
                        )
                    }
                }

                // User Request Section
                SectionCard(title = "User Request") {
                    OutlinedTextField(
                        value = userRequest,
                        onValueChange = { userRequest = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        enabled = !isRunning && !isConnecting,
                        placeholder = {
                            Text(
                                text = "Find posts about \"qui\", summarize them, and save the result to posts-summary.txt",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                isRunning = true
                                result = null
                                error = null
                                steps = emptyList()
                                try {
                                    result = agent?.runPipeline(userRequest)
                                } catch (e: Exception) {
                                    error = "Pipeline error: ${e.message}"
                                }
                                isRunning = false
                            }
                        },
                        enabled = !isRunning && mcpClient != null && userRequest.isNotBlank(),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        if (isRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isRunning) "Running Pipeline..." else "Run Pipeline")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Execution Trace
                SectionCard(title = "Execution Trace") {
                    if (steps.isEmpty() && !isRunning) {
                        Text(
                            text = "Pipeline not started yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            steps.forEach { step ->
                                StepRow(step)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search Result Preview
                result?.searchResult?.let { search ->
                    SectionCard(title = "Search Result Preview") {
                        Text(
                            text = "Posts found: ${search.count}",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        search.posts.forEach { post ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "#${post.id}",
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.width(40.dp)
                                )
                                Text(
                                    text = post.title,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (search.count > 3) {
                            Text(
                                text = "... and ${search.count - 3} more posts",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Summary
                result?.summary?.let { summary ->
                    SectionCard(title = "Summary") {
                        Text(
                            text = summary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Saved File
                result?.savedFile?.let { saved ->
                    SectionCard(title = "Saved File") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        if (saved.success) Color(0xFF4CAF50) else Color(0xFFF44336),
                                        RoundedCornerShape(6.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (saved.success) "Success" else "Failed",
                                fontWeight = FontWeight.SemiBold,
                                color = if (saved.success) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Path: ${saved.path}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Final Claude Response
                result?.finalResponse?.takeIf { it.isNotBlank() }?.let { response ->
                    SectionCard(title = "Final Claude Response") {
                        Text(
                            text = response,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(12.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
private fun StepRow(step: PipelineStep) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status indicator
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(
                    when (step.status) {
                        StepStatus.PENDING -> Color.Gray
                        StepStatus.RUNNING -> Color(0xFFFFC107)
                        StepStatus.DONE -> Color(0xFF4CAF50)
                        StepStatus.ERROR -> Color(0xFFF44336)
                    },
                    RoundedCornerShape(5.dp)
                )
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.step,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )

            step.toolName?.let { toolName ->
                Text(
                    text = "$toolName(${step.toolArgs?.take(60) ?: ""}${if ((step.toolArgs?.length ?: 0) > 60) "..." else ""})",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            step.result?.let { result ->
                Text(
                    text = result,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Status badge
        Text(
            text = when (step.status) {
                StepStatus.PENDING -> "Pending"
                StepStatus.RUNNING -> "Running"
                StepStatus.DONE -> "Done"
                StepStatus.ERROR -> "Error"
            },
            fontSize = 11.sp,
            color = when (step.status) {
                StepStatus.PENDING -> Color.Gray
                StepStatus.RUNNING -> Color(0xFFFFC107)
                StepStatus.DONE -> Color(0xFF4CAF50)
                StepStatus.ERROR -> Color(0xFFF44336)
            },
            fontWeight = FontWeight.Bold
        )
    }
}
