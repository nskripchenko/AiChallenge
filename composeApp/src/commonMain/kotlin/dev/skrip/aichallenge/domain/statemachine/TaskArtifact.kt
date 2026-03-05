package dev.skrip.aichallenge.domain.statemachine

import kotlinx.serialization.Serializable

/**
 * Artifacts produced by each phase.
 * These are the "outputs" that must be validated before transitioning.
 */
@Serializable
sealed class TaskArtifact {
    abstract val phaseId: String
    abstract val timestamp: Long
    abstract val isApproved: Boolean

    /**
     * Artifact from clarifying phase - collected requirements
     */
    @Serializable
    data class Requirements(
        override val timestamp: Long,
        override val isApproved: Boolean = false,
        val originalRequest: String,
        val clarifications: Map<String, String>,
        val finalRequirements: String
    ) : TaskArtifact() {
        override val phaseId = "clarifying"
    }

    /**
     * Artifact from planning phase - execution plan
     */
    @Serializable
    data class Plan(
        override val timestamp: Long,
        override val isApproved: Boolean = false,
        val steps: List<PlanStep>,
        val estimatedComplexity: String,
        val risks: List<String> = emptyList(),
        val dependencies: List<String> = emptyList()
    ) : TaskArtifact() {
        override val phaseId = "planning"

        fun toMarkdown(): String = buildString {
            appendLine("## Execution Plan")
            appendLine()
            appendLine("**Complexity:** $estimatedComplexity")
            appendLine()
            appendLine("### Steps:")
            steps.forEach { step ->
                appendLine("${step.index + 1}. **${step.title}**")
                appendLine("   - ${step.description}")
                appendLine("   - Expected: ${step.expectedOutput}")
                appendLine()
            }
            if (risks.isNotEmpty()) {
                appendLine("### Risks:")
                risks.forEach { appendLine("- $it") }
                appendLine()
            }
            if (dependencies.isNotEmpty()) {
                appendLine("### Dependencies:")
                dependencies.forEach { appendLine("- $it") }
            }
        }
    }

    /**
     * Artifact from execution phase - step result
     */
    @Serializable
    data class StepExecution(
        override val timestamp: Long,
        override val isApproved: Boolean = false,
        val stepIndex: Int,
        val stepTitle: String,
        val output: String,
        val changes: List<CodeChange> = emptyList(),
        val success: Boolean
    ) : TaskArtifact() {
        override val phaseId = "executing"
    }

    /**
     * Artifact from validation phase - validation report
     */
    @Serializable
    data class ValidationReport(
        override val timestamp: Long,
        override val isApproved: Boolean = false,
        val checks: List<ValidationCheck>,
        val overallSuccess: Boolean,
        val summary: String
    ) : TaskArtifact() {
        override val phaseId = "validating"

        fun toMarkdown(): String = buildString {
            appendLine("## Validation Report")
            appendLine()
            appendLine("**Status:** ${if (overallSuccess) "PASSED" else "FAILED"}")
            appendLine()
            appendLine("### Checks:")
            checks.forEach { check ->
                val icon = if (check.passed) "[OK]" else "[FAIL]"
                appendLine("$icon **${check.name}**")
                appendLine("   ${check.description}")
                if (check.details.isNotBlank()) {
                    appendLine("   Details: ${check.details}")
                }
                appendLine()
            }
            appendLine("### Summary:")
            appendLine(summary)
        }
    }

    /**
     * Final artifact - task completion summary
     */
    @Serializable
    data class CompletionSummary(
        override val timestamp: Long,
        override val isApproved: Boolean = true,
        val taskDescription: String,
        val allArtifacts: List<String>,
        val finalSummary: String
    ) : TaskArtifact() {
        override val phaseId = "completed"
    }
}

@Serializable
data class CodeChange(
    val filePath: String,
    val changeType: ChangeType,
    val description: String
)

@Serializable
enum class ChangeType {
    CREATED,
    MODIFIED,
    DELETED
}
