package dev.skrip.aichallenge.domain.statemachine

import kotlinx.serialization.Serializable

/**
 * Represents the phases of a task in the state machine.
 * Each phase has specific entry/exit conditions and produces artifacts.
 */
@Serializable
sealed class TaskPhase {
    abstract val name: String
    abstract val description: String
    abstract val allowedTransitions: Set<String>

    /**
     * Initial state - no task is active
     */
    @Serializable
    data object Idle : TaskPhase() {
        override val name = "idle"
        override val description = "No active task"
        override val allowedTransitions = setOf("clarifying", "planning")
    }

    /**
     * Gathering requirements and asking clarifying questions.
     * Blocks until all questions are answered.
     */
    @Serializable
    data class Clarifying(
        val questions: List<String> = emptyList(),
        val answers: Map<String, String> = emptyMap(),
        val pendingQuestions: List<String> = emptyList()
    ) : TaskPhase() {
        override val name = "clarifying"
        override val description = "Gathering requirements"
        override val allowedTransitions = setOf("planning", "idle")

        val isComplete: Boolean
            get() = pendingQuestions.isEmpty() && questions.isNotEmpty()
    }

    /**
     * Creating a detailed execution plan.
     * Produces a plan artifact that must be approved.
     */
    @Serializable
    data class Planning(
        val planSteps: List<PlanStep> = emptyList(),
        val isApproved: Boolean = false
    ) : TaskPhase() {
        override val name = "planning"
        override val description = "Creating execution plan"
        override val allowedTransitions = setOf("executing", "clarifying", "idle")

        val isComplete: Boolean
            get() = planSteps.isNotEmpty() && isApproved
    }

    /**
     * Executing the approved plan step by step.
     * Produces code/changes as artifacts.
     */
    @Serializable
    data class Executing(
        val currentStepIndex: Int = 0,
        val totalSteps: Int = 0,
        val completedSteps: List<ExecutionResult> = emptyList(),
        val isStepApproved: Boolean = false
    ) : TaskPhase() {
        override val name = "executing"
        override val description = "Executing plan"
        override val allowedTransitions = setOf("validating", "planning", "paused", "idle")

        val isComplete: Boolean
            get() = currentStepIndex >= totalSteps && completedSteps.size == totalSteps

        val progress: Float
            get() = if (totalSteps > 0) completedSteps.size.toFloat() / totalSteps else 0f
    }

    /**
     * Validating the execution results.
     * Produces a validation report.
     */
    @Serializable
    data class Validating(
        val validationChecks: List<ValidationCheck> = emptyList(),
        val isApproved: Boolean = false
    ) : TaskPhase() {
        override val name = "validating"
        override val description = "Validating results"
        override val allowedTransitions = setOf("completed", "executing", "idle")

        val isComplete: Boolean
            get() = validationChecks.isNotEmpty() && isApproved

        val allChecksPassed: Boolean
            get() = validationChecks.all { it.passed }
    }

    /**
     * Task completed successfully.
     */
    @Serializable
    data class Completed(
        val summary: String = "",
        val artifacts: List<String> = emptyList()
    ) : TaskPhase() {
        override val name = "completed"
        override val description = "Task completed"
        override val allowedTransitions = setOf("idle")
    }

    /**
     * Task is paused - can resume from any phase.
     */
    @Serializable
    data class Paused(
        val previousPhase: String,
        val pauseReason: String = "",
        val resumeData: String = "" // Serialized previous phase state
    ) : TaskPhase() {
        override val name = "paused"
        override val description = "Task paused"
        override val allowedTransitions = setOf(previousPhase, "idle")
    }
}

@Serializable
data class PlanStep(
    val index: Int,
    val title: String,
    val description: String,
    val expectedOutput: String,
    val isCompleted: Boolean = false
)

@Serializable
data class ExecutionResult(
    val stepIndex: Int,
    val output: String,
    val success: Boolean,
    val error: String? = null
)

@Serializable
data class ValidationCheck(
    val name: String,
    val description: String,
    val passed: Boolean,
    val details: String = ""
)
