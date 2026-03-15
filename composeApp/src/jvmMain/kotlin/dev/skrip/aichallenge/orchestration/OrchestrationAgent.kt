package dev.skrip.aichallenge.orchestration

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.*

data class TraceStep(
    val step: String,
    val status: StepStatus,
    val serverName: String? = null,
    val toolName: String? = null,
    val toolArgs: String? = null,
    val result: String? = null
)

enum class StepStatus { PENDING, RUNNING, DONE, ERROR }

data class OrchestrationResult(
    val steps: List<TraceStep>,
    val searchPreview: String?,
    val summaryPreview: String?,
    val keywordsPreview: String?,
    val savedFile: SavedFileInfo?,
    val finalResponse: String
)

data class SavedFileInfo(val path: String, val success: Boolean)

class OrchestrationAgent(private val router: McpRouter) {
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
    private val json = Json { ignoreUnknownKeys = true }
    private val apiKey: String get() = System.getenv("ANTHROPIC_API_KEY") ?: ""

    private val _steps = MutableStateFlow<List<TraceStep>>(emptyList())
    val steps: StateFlow<List<TraceStep>> = _steps

    private val systemPrompt = """
You are an orchestration agent that uses tools from multiple MCP servers.

Available servers and tools:
- search-mcp: search_posts(query)
- summary-mcp: summarize_posts(postsJson), extract_keywords(text)
- file-mcp: save_to_file(filename, content), read_file(filename)

Your job:
- Select the correct tool for each step
- Execute workflow in correct order
- Pass outputs between tools correctly
- Complete the user's multi-step request automatically

Rules:
- Use tools from different servers when needed
- Do not skip steps requested by the user
- When calling summarize_posts, pass the EXACT JSON output from search_posts as postsJson
- When calling extract_keywords, pass text content (like summaryText)
- When calling save_to_file, create a readable report with summary and keywords
- If user asks to confirm/verify saved content, use read_file
- Keep final response clear and concise
""".trimIndent()

    suspend fun runOrchestration(userRequest: String): OrchestrationResult {
        _steps.value = emptyList()
        addStep("User request received", StepStatus.DONE)

        val tools = router.getAllTools()
        if (tools.isEmpty()) {
            addStep("No tools available", StepStatus.ERROR)
            return OrchestrationResult(
                _steps.value, null, null, null, null, "Error: No MCP tools available"
            )
        }

        addStep("Request sent to Claude", StepStatus.RUNNING)

        val messages = mutableListOf<JsonObject>()
        messages.add(buildJsonObject {
            put("role", "user")
            put("content", userRequest)
        })

        var searchPreview: String? = null
        var summaryPreview: String? = null
        var keywordsPreview: String? = null
        var savedFile: SavedFileInfo? = null
        var finalResponse = ""

        var maxIterations = 15
        while (maxIterations > 0) {
            maxIterations--

            val response = callClaude(messages, tools)
            val stopReason = response["stop_reason"]?.jsonPrimitive?.content
            val content = response["content"]?.jsonArray ?: continue

            val toolUses = mutableListOf<JsonObject>()
            val textBlocks = mutableListOf<String>()

            for (block in content) {
                val obj = block.jsonObject
                when (obj["type"]?.jsonPrimitive?.content) {
                    "text" -> textBlocks.add(obj["text"]?.jsonPrimitive?.content ?: "")
                    "tool_use" -> toolUses.add(obj)
                }
            }

            messages.add(buildJsonObject {
                put("role", "assistant")
                put("content", content)
            })

            if (stopReason == "end_turn" || toolUses.isEmpty()) {
                updateLastStep(StepStatus.DONE)
                finalResponse = textBlocks.joinToString("\n")
                break
            }

            val toolResults = mutableListOf<JsonObject>()

            for (toolUse in toolUses) {
                val toolId = toolUse["id"]?.jsonPrimitive?.content ?: ""
                val toolName = toolUse["name"]?.jsonPrimitive?.content ?: ""
                val toolInput = toolUse["input"]?.jsonObject ?: buildJsonObject {}
                val serverName = router.getServerForTool(toolName) ?: "unknown"

                addStep("Claude selected server: $serverName", StepStatus.DONE)
                addStep("Tool called: $toolName", StepStatus.RUNNING, serverName, toolName, toolInput.toString())

                val args = mutableMapOf<String, String>()
                toolInput.forEach { k, v ->
                    args[k] = when (v) {
                        is JsonPrimitive -> v.content
                        else -> v.toString()
                    }
                }

                val result = router.callTool(toolName, args)

                // Parse for UI previews
                when (toolName) {
                    "search_posts" -> {
                        try {
                            val r = json.parseToJsonElement(result).jsonObject
                            val count = r["count"]?.jsonPrimitive?.int ?: 0
                            val posts = r["posts"]?.jsonArray?.take(3)?.map {
                                val p = it.jsonObject
                                "#${p["id"]?.jsonPrimitive?.int}: ${p["title"]?.jsonPrimitive?.content}"
                            } ?: emptyList()
                            searchPreview = "Found $count posts:\n${posts.joinToString("\n")}"
                            if (count > 3) searchPreview += "\n... and ${count - 3} more"
                        } catch (_: Exception) {}
                        updateLastStep(StepStatus.DONE, "Found posts")
                    }
                    "summarize_posts" -> {
                        try {
                            val r = json.parseToJsonElement(result).jsonObject
                            summaryPreview = r["summaryText"]?.jsonPrimitive?.content
                        } catch (_: Exception) {
                            summaryPreview = result.take(200)
                        }
                        updateLastStep(StepStatus.DONE, "Summary created")
                    }
                    "extract_keywords" -> {
                        try {
                            val r = json.parseToJsonElement(result).jsonObject
                            val kws = r["keywords"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                            keywordsPreview = kws.joinToString(", ")
                        } catch (_: Exception) {}
                        updateLastStep(StepStatus.DONE, "Keywords extracted")
                    }
                    "save_to_file" -> {
                        try {
                            val r = json.parseToJsonElement(result).jsonObject
                            savedFile = SavedFileInfo(
                                r["savedPath"]?.jsonPrimitive?.content ?: "",
                                r["success"]?.jsonPrimitive?.boolean ?: false
                            )
                        } catch (_: Exception) {}
                        updateLastStep(StepStatus.DONE, "File saved")
                    }
                    "read_file" -> {
                        updateLastStep(StepStatus.DONE, "File read")
                    }
                    else -> updateLastStep(StepStatus.DONE)
                }

                toolResults.add(buildJsonObject {
                    put("type", "tool_result")
                    put("tool_use_id", toolId)
                    put("content", result)
                })
            }

            messages.add(buildJsonObject {
                put("role", "user")
                put("content", buildJsonArray { toolResults.forEach { add(it) } })
            })
        }

        addStep("Flow completed", StepStatus.DONE)

        return OrchestrationResult(
            _steps.value, searchPreview, summaryPreview, keywordsPreview, savedFile, finalResponse
        )
    }

    private suspend fun callClaude(messages: List<JsonObject>, tools: List<McpTool>): JsonObject {
        val body = buildJsonObject {
            put("model", "claude-sonnet-4-20250514")
            put("max_tokens", 4096)
            put("system", systemPrompt)
            put("messages", buildJsonArray { messages.forEach { add(it) } })
            put("tools", buildJsonArray {
                tools.forEach { tool ->
                    add(buildJsonObject {
                        put("name", tool.name)
                        put("description", tool.description)
                        put("input_schema", tool.inputSchema)
                    })
                }
            })
        }

        return httpClient.post("https://api.anthropic.com/v1/messages") {
            contentType(ContentType.Application.Json)
            header("x-api-key", apiKey)
            header("anthropic-version", "2023-06-01")
            setBody(body)
        }.body()
    }

    private fun addStep(step: String, status: StepStatus, server: String? = null, tool: String? = null, args: String? = null) {
        _steps.value = _steps.value + TraceStep(step, status, server, tool, args)
    }

    private fun updateLastStep(status: StepStatus, result: String? = null) {
        val list = _steps.value.toMutableList()
        if (list.isNotEmpty()) {
            list[list.lastIndex] = list.last().copy(status = status, result = result)
            _steps.value = list
        }
    }
}
