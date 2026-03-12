package dev.skrip.aichallenge.pipeline

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

data class PipelineStep(
    val step: String,
    val status: StepStatus,
    val toolName: String? = null,
    val toolArgs: String? = null,
    val result: String? = null
)

enum class StepStatus {
    PENDING, RUNNING, DONE, ERROR
}

data class PipelineResult(
    val steps: List<PipelineStep>,
    val searchResult: SearchResultPreview?,
    val summary: String?,
    val savedFile: SavedFileInfo?,
    val finalResponse: String
)

data class SearchResultPreview(
    val count: Int,
    val posts: List<PostPreview>
)

data class PostPreview(
    val id: Int,
    val title: String
)

data class SavedFileInfo(
    val path: String,
    val success: Boolean
)

class PipelineAgent(private val mcpClient: McpClient) {
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val apiKey: String
        get() = System.getenv("ANTHROPIC_API_KEY") ?: ""

    private val _steps = MutableStateFlow<List<PipelineStep>>(emptyList())
    val steps: StateFlow<List<PipelineStep>> = _steps

    private val systemPrompt = """
You are an assistant that executes multi-step content pipelines using tools.
When the user asks to find information, summarize it, and save it, you should automatically use the available tools in the correct order.

Available workflow:
1. search_posts(query) - Search posts by query string
2. summarize_posts(postsJson) - Create summary from search results (pass the EXACT output from search_posts as postsJson)
3. save_to_file(filename, content) - Save content to a file

Rules:
- Use tools in sequence when needed
- Pass outputs from one tool into the next correctly
- IMPORTANT: When calling summarize_posts, pass the EXACT JSON string output from search_posts as the postsJson parameter
- IMPORTANT: When calling save_to_file, extract the summaryText from summarize_posts result and use it as content
- Do not skip steps if the user asked for the full pipeline
- After saving, clearly report what was found, summarized, and where it was saved
- Keep the final response clear and concise
""".trimIndent()

    suspend fun runPipeline(userRequest: String): PipelineResult {
        _steps.value = emptyList()
        addStep("User request received", StepStatus.DONE)

        val tools = mcpClient.listTools()
        if (tools.isEmpty()) {
            addStep("Failed to load MCP tools", StepStatus.ERROR)
            return PipelineResult(
                steps = _steps.value,
                searchResult = null,
                summary = null,
                savedFile = null,
                finalResponse = "Error: Could not connect to MCP server"
            )
        }

        addStep("Request sent to Claude", StepStatus.RUNNING)

        val messages = mutableListOf<JsonObject>()
        messages.add(buildJsonObject {
            put("role", "user")
            put("content", userRequest)
        })

        var searchResult: SearchResultPreview? = null
        var summary: String? = null
        var savedFile: SavedFileInfo? = null
        var finalResponse = ""

        // Conversation loop
        var maxIterations = 10
        while (maxIterations > 0) {
            maxIterations--

            val response = callClaude(messages, tools)

            val stopReason = response["stop_reason"]?.jsonPrimitive?.content
            val content = response["content"]?.jsonArray ?: continue

            // Process response content
            val toolUses = mutableListOf<JsonObject>()
            val textBlocks = mutableListOf<String>()

            for (block in content) {
                val blockObj = block.jsonObject
                when (blockObj["type"]?.jsonPrimitive?.content) {
                    "text" -> {
                        textBlocks.add(blockObj["text"]?.jsonPrimitive?.content ?: "")
                    }
                    "tool_use" -> {
                        toolUses.add(blockObj)
                    }
                }
            }

            // Add assistant message to history
            messages.add(buildJsonObject {
                put("role", "assistant")
                put("content", content)
            })

            if (stopReason == "end_turn" || toolUses.isEmpty()) {
                updateLastStep(StepStatus.DONE)
                finalResponse = textBlocks.joinToString("\n")
                break
            }

            // Process tool calls
            val toolResults = mutableListOf<JsonObject>()

            for (toolUse in toolUses) {
                val toolId = toolUse["id"]?.jsonPrimitive?.content ?: ""
                val toolName = toolUse["name"]?.jsonPrimitive?.content ?: ""
                val toolInput = toolUse["input"]?.jsonObject ?: buildJsonObject {}

                addStep("Claude called $toolName", StepStatus.RUNNING, toolName, toolInput.toString())

                // Convert input to Map<String, String>
                val args = mutableMapOf<String, String>()
                toolInput.forEach { key, value ->
                    args[key] = when (value) {
                        is JsonPrimitive -> value.content
                        else -> value.toString()
                    }
                }

                val result = mcpClient.callTool(toolName, args)

                // Parse results for UI
                when (toolName) {
                    "search_posts" -> {
                        try {
                            val resultJson = json.parseToJsonElement(result).jsonObject
                            val count = resultJson["count"]?.jsonPrimitive?.int ?: 0
                            val posts = resultJson["posts"]?.jsonArray?.take(3)?.map { post ->
                                val postObj = post.jsonObject
                                PostPreview(
                                    id = postObj["id"]?.jsonPrimitive?.int ?: 0,
                                    title = postObj["title"]?.jsonPrimitive?.content ?: ""
                                )
                            } ?: emptyList()
                            searchResult = SearchResultPreview(count, posts)
                        } catch (e: Exception) {
                            // Ignore parse errors
                        }
                        updateLastStep(StepStatus.DONE, "Found ${searchResult?.count ?: 0} posts")
                    }
                    "summarize_posts" -> {
                        try {
                            val resultJson = json.parseToJsonElement(result).jsonObject
                            summary = resultJson["summaryText"]?.jsonPrimitive?.content
                        } catch (e: Exception) {
                            summary = result
                        }
                        updateLastStep(StepStatus.DONE, "Summary created")
                    }
                    "save_to_file" -> {
                        val fileInfo = try {
                            val resultJson = json.parseToJsonElement(result).jsonObject
                            SavedFileInfo(
                                path = resultJson["savedPath"]?.jsonPrimitive?.content ?: "",
                                success = resultJson["success"]?.jsonPrimitive?.boolean ?: false
                            )
                        } catch (e: Exception) {
                            SavedFileInfo(result, false)
                        }
                        savedFile = fileInfo
                        updateLastStep(StepStatus.DONE, "File saved: ${fileInfo.path}")
                    }
                    else -> {
                        updateLastStep(StepStatus.DONE)
                    }
                }

                toolResults.add(buildJsonObject {
                    put("type", "tool_result")
                    put("tool_use_id", toolId)
                    put("content", result)
                })
            }

            // Add tool results to messages
            messages.add(buildJsonObject {
                put("role", "user")
                put("content", buildJsonArray {
                    toolResults.forEach { add(it) }
                })
            })
        }

        addStep("Pipeline completed", StepStatus.DONE)

        return PipelineResult(
            steps = _steps.value,
            searchResult = searchResult,
            summary = summary,
            savedFile = savedFile,
            finalResponse = finalResponse
        )
    }

    private suspend fun callClaude(messages: List<JsonObject>, tools: List<McpTool>): JsonObject {
        val requestBody = buildJsonObject {
            put("model", "claude-sonnet-4-20250514")
            put("max_tokens", 4096)
            put("system", systemPrompt)
            put("messages", buildJsonArray {
                messages.forEach { add(it) }
            })
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

        val response: JsonObject = httpClient.post("https://api.anthropic.com/v1/messages") {
            contentType(ContentType.Application.Json)
            header("x-api-key", apiKey)
            header("anthropic-version", "2023-06-01")
            setBody(requestBody)
        }.body()

        return response
    }

    private fun addStep(step: String, status: StepStatus, toolName: String? = null, toolArgs: String? = null) {
        _steps.value = _steps.value + PipelineStep(step, status, toolName, toolArgs)
    }

    private fun updateLastStep(status: StepStatus, result: String? = null) {
        val current = _steps.value.toMutableList()
        if (current.isNotEmpty()) {
            val last = current.last()
            current[current.lastIndex] = last.copy(status = status, result = result)
            _steps.value = current
        }
    }
}
