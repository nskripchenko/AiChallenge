package dev.skrip.aichallenge

import kotlinx.serialization.json.*

class JsonPlaceholderAgent(
    private val mcpClient: McpJsonPlaceholderAgent,
    private val claudeClient: ClaudeApiClient
) {

    suspend fun processQuery(userQuery: String, onTraceUpdate: (AgentTrace) -> Unit): AgentResult {
        val trace = AgentTrace()

        // Step 1: User request
        trace.addStep(TraceStep("User Request", userQuery, StepStatus.DONE))
        onTraceUpdate(trace.copy())

        val messages = mutableListOf(
            ClaudeMessage(
                role = "user",
                content = JsonPrimitive(userQuery)
            )
        )

        // Step 2: Sending to Claude
        trace.addStep(TraceStep("Sending to Claude", "Requesting response from Claude API...", StepStatus.RUNNING))
        onTraceUpdate(trace.copy())

        var response = claudeClient.sendMessage(messages)

        trace.updateLastStep(StepStatus.DONE, "Response received (stop_reason: ${response.stopReason})")
        onTraceUpdate(trace.copy())

        // Step 3: Check for tool use
        while (response.stopReason == "tool_use") {
            val toolUseBlocks = response.content.filter { it.type == "tool_use" }
            if (toolUseBlocks.isEmpty()) break

            // Add assistant message
            messages.add(ClaudeMessage(
                role = "assistant",
                content = Json.encodeToJsonElement(response.content)
            ))

            val toolResults = mutableListOf<JsonObject>()

            for (toolBlock in toolUseBlocks) {
                val toolName = toolBlock.name ?: continue
                val toolId = toolBlock.id ?: continue
                val input = toolBlock.input ?: JsonObject(emptyMap())

                // Step: Claude requested tool
                trace.addStep(TraceStep(
                    "Claude Requested Tool",
                    "Tool: $toolName",
                    StepStatus.DONE
                ))

                // Update tool call info
                trace.toolCall = ToolCallInfo(
                    toolName = toolName,
                    arguments = input.toString(),
                    argumentsPretty = formatJsonPretty(input)
                )
                onTraceUpdate(trace.copy())

                // Step: Calling MCP tool
                trace.addStep(TraceStep(
                    "Calling MCP Tool",
                    "Executing $toolName via MCP server...",
                    StepStatus.RUNNING
                ))
                onTraceUpdate(trace.copy())

                val result = executeToolCall(toolName, input)

                trace.updateLastStep(StepStatus.DONE, "Tool executed successfully")
                trace.toolResult = result
                onTraceUpdate(trace.copy())

                // Step: Tool result received
                trace.addStep(TraceStep(
                    "Tool Result Received",
                    "Got response from MCP server",
                    StepStatus.DONE
                ))
                onTraceUpdate(trace.copy())

                toolResults.add(buildJsonObject {
                    put("type", JsonPrimitive("tool_result"))
                    put("tool_use_id", JsonPrimitive(toolId))
                    put("content", JsonPrimitive(result))
                })
            }

            // Add tool results message
            messages.add(ClaudeMessage(
                role = "user",
                content = JsonArray(toolResults)
            ))

            // Step: Sending tool result back
            trace.addStep(TraceStep(
                "Sending Result to Claude",
                "Returning tool result for final response...",
                StepStatus.RUNNING
            ))
            onTraceUpdate(trace.copy())

            response = claudeClient.sendMessage(messages)

            trace.updateLastStep(StepStatus.DONE, "Claude received result (stop_reason: ${response.stopReason})")
            onTraceUpdate(trace.copy())
        }

        // Extract final response
        val finalResponse = response.content
            .filter { it.type == "text" }
            .mapNotNull { it.text }
            .joinToString("\n")

        // Step: Final response
        trace.addStep(TraceStep(
            "Final Response Ready",
            "Claude generated final answer",
            StepStatus.DONE
        ))
        trace.finalResponse = finalResponse
        onTraceUpdate(trace.copy())

        return AgentResult(
            response = finalResponse,
            trace = trace
        )
    }

    private suspend fun executeToolCall(toolName: String, input: JsonObject): String {
        return when (toolName) {
            "get_post" -> {
                val id = input["id"]?.jsonPrimitive?.intOrNull ?: 1
                mcpClient.getPost(id)
            }
            "get_user" -> {
                val id = input["id"]?.jsonPrimitive?.intOrNull ?: 1
                mcpClient.getUser(id)
            }
            else -> "Unknown tool: $toolName"
        }
    }

    private fun formatJsonPretty(json: JsonObject): String {
        return buildString {
            appendLine("{")
            json.entries.forEachIndexed { index, (key, value) ->
                val comma = if (index < json.size - 1) "," else ""
                appendLine("  \"$key\": $value$comma")
            }
            append("}")
        }
    }
}

// Trace step with status
data class TraceStep(
    val title: String,
    var details: String,
    var status: StepStatus
)

enum class StepStatus {
    PENDING, RUNNING, DONE, ERROR
}

// Tool call details
data class ToolCallInfo(
    val toolName: String,
    val arguments: String,
    val argumentsPretty: String
)

// Full agent trace
data class AgentTrace(
    val steps: MutableList<TraceStep> = mutableListOf(),
    var toolCall: ToolCallInfo? = null,
    var toolResult: String? = null,
    var finalResponse: String? = null
) {
    fun addStep(step: TraceStep) {
        steps.add(step)
    }

    fun updateLastStep(status: StepStatus, details: String? = null) {
        steps.lastOrNull()?.let {
            it.status = status
            if (details != null) it.details = details
        }
    }

    fun copy(): AgentTrace = AgentTrace(
        steps = steps.map { it.copy() }.toMutableList(),
        toolCall = toolCall?.copy(),
        toolResult = toolResult,
        finalResponse = finalResponse
    )
}

// Result with full trace
data class AgentResult(
    val response: String,
    val trace: AgentTrace
)
