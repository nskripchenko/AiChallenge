package dev.skrip.aichallenge.orchestration

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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun OrchestrationScreen() {
    val scope = rememberCoroutineScope()
    var userRequest by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(true) }
    var result by remember { mutableStateOf<OrchestrationResult?>(null) }
    var router by remember { mutableStateOf<McpRouter?>(null) }
    var agent by remember { mutableStateOf<OrchestrationAgent?>(null) }
    var steps by remember { mutableStateOf<List<TraceStep>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var connectedServers by remember { mutableStateOf<List<String>>(emptyList()) }

    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        isConnecting = true
        try {
            val projectRoot = findProjectRoot()
            System.err.println("Project root: $projectRoot")
            val r = McpRouter()
            if (r.initialize(projectRoot)) {
                router = r
                agent = OrchestrationAgent(r)
                connectedServers = listOf("search-mcp", "summary-mcp", "file-mcp")
                error = null
            } else {
                error = "Failed to connect to MCP servers"
            }
        } catch (e: Exception) {
            error = "Init error: ${e.message}"
        }
        isConnecting = false
    }

    LaunchedEffect(agent) {
        agent?.steps?.collect { steps = it }
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(scrollState)
            ) {
                Text(
                    "Multi-Server Orchestration Demo",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Day 20: Claude orchestrates tools across multiple MCP servers",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Connection status
                if (isConnecting) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Connecting to MCP servers...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (connectedServers.isNotEmpty()) {
                    Row(modifier = Modifier.padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        connectedServers.forEach { server ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).background(Color(0xFF4CAF50), RoundedCornerShape(4.dp)))
                                Spacer(Modifier.width(4.dp))
                                Text(server, fontSize = 12.sp, color = Color(0xFF4CAF50))
                            }
                        }
                    }
                }

                error?.let {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF5C1010)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Text(it, color = Color.White, modifier = Modifier.padding(16.dp))
                    }
                }

                // User Request
                SectionCard("User Request") {
                    OutlinedTextField(
                        value = userRequest,
                        onValueChange = { userRequest = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        enabled = !isRunning && !isConnecting,
                        placeholder = {
                            Text(
                                "Find posts about \"qui\", summarize the key themes, extract keywords, save the report to report.txt, and confirm what was saved.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                isRunning = true
                                result = null
                                error = null
                                steps = emptyList()
                                try {
                                    result = agent?.runOrchestration(userRequest)
                                } catch (e: Exception) {
                                    error = "Error: ${e.message}"
                                }
                                isRunning = false
                            }
                        },
                        enabled = !isRunning && router != null && userRequest.isNotBlank(),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        if (isRunning) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (isRunning) "Running Flow..." else "Run Flow")
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Execution Trace
                SectionCard("Execution Trace") {
                    if (steps.isEmpty() && !isRunning) {
                        Text("Flow not started yet", color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = FontStyle.Italic)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            steps.forEach { step -> StepRow(step) }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Intermediate Results
                result?.searchPreview?.let { preview ->
                    SectionCard("Search Result Preview") {
                        Text(preview, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                }

                result?.summaryPreview?.let { preview ->
                    SectionCard("Summary Preview") {
                        Text(
                            preview,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(12.dp).fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }

                result?.keywordsPreview?.let { keywords ->
                    SectionCard("Keywords") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            keywords.split(", ").forEach { kw ->
                                Box(
                                    modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(kw, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                result?.savedFile?.let { saved ->
                    SectionCard("Saved File") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).background(if (saved.success) Color(0xFF4CAF50) else Color(0xFFF44336), RoundedCornerShape(5.dp)))
                            Spacer(Modifier.width(8.dp))
                            Text(if (saved.success) "Success" else "Failed", fontWeight = FontWeight.SemiBold, color = if (saved.success) Color(0xFF4CAF50) else Color(0xFFF44336))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Path: ${saved.path}", fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                }

                result?.finalResponse?.takeIf { it.isNotBlank() }?.let { response ->
                    SectionCard("Final Claude Response") {
                        Text(response, lineHeight = 22.sp)
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp))
            content()
        }
    }
}

@Composable
private fun StepRow(step: TraceStep) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(8.dp).background(
                when (step.status) {
                    StepStatus.PENDING -> Color.Gray
                    StepStatus.RUNNING -> Color(0xFFFFC107)
                    StepStatus.DONE -> Color(0xFF4CAF50)
                    StepStatus.ERROR -> Color(0xFFF44336)
                },
                RoundedCornerShape(4.dp)
            )
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(step.step, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            step.serverName?.let { server ->
                Text("Server: $server", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
            }
            step.toolName?.let { tool ->
                val argsPreview = step.toolArgs?.take(50)?.let { if (step.toolArgs.length > 50) "$it..." else it } ?: ""
                Text("$tool($argsPreview)", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            step.result?.let { res ->
                Text(res, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            when (step.status) {
                StepStatus.PENDING -> "Pending"
                StepStatus.RUNNING -> "Running"
                StepStatus.DONE -> "Done"
                StepStatus.ERROR -> "Error"
            },
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = when (step.status) {
                StepStatus.PENDING -> Color.Gray
                StepStatus.RUNNING -> Color(0xFFFFC107)
                StepStatus.DONE -> Color(0xFF4CAF50)
                StepStatus.ERROR -> Color(0xFFF44336)
            }
        )
    }
}

private fun findProjectRoot(): File {
    val candidates = listOf(
        File(System.getProperty("user.dir")),
        File(System.getProperty("user.dir")).parentFile,
        File(System.getProperty("user.dir"), ".."),
    )
    for (c in candidates) {
        if (c != null && File(c, "search-mcp").exists()) return c
    }
    return File(System.getProperty("user.dir"))
}
