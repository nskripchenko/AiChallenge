package dev.skrip.aichallenge.domain.statemachine

import kotlinx.serialization.Serializable

/**
 * Complete state of a task being executed through the state machine.
 * This is the single source of truth for task progress.
 */
@Serializable
data class TaskState(
    val taskId: String,
    val originalRequest: String,
    val currentPhase: TaskPhase,
    val artifacts: List<TaskArtifact> = emptyList(),
    val history: List<PhaseTransition> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long,
    val metadata: TaskMetadata = TaskMetadata()
) {
    /**
     * Get current step description for display
     */
    val currentStepDescription: String
        get() = when (val phase = currentPhase) {
            is TaskPhase.Idle -> "Ready to start"
            is TaskPhase.Clarifying -> {
                if (phase.pendingQuestions.isNotEmpty()) {
                    "Waiting for answers to ${phase.pendingQuestions.size} question(s)"
                } else {
                    "Requirements gathered"
                }
            }
            is TaskPhase.Planning -> {
                if (phase.planSteps.isEmpty()) {
                    "Creating execution plan..."
                } else if (!phase.isApproved) {
                    "Plan ready - waiting for approval"
                } else {
                    "Plan approved"
                }
            }
            is TaskPhase.Executing -> {
                "Step ${phase.currentStepIndex + 1}/${phase.totalSteps}: " +
                    if (!phase.isStepApproved) "Waiting for step approval" else "Executing..."
            }
            is TaskPhase.Validating -> {
                if (phase.validationChecks.isEmpty()) {
                    "Running validation..."
                } else if (!phase.isApproved) {
                    "Validation complete - waiting for approval"
                } else {
                    "Validation approved"
                }
            }
            is TaskPhase.Completed -> "Task completed"
            is TaskPhase.Paused -> "Paused: ${phase.pauseReason}"
        }

    /**
     * Get expected next action
     */
    val expectedAction: ExpectedAction
        get() = when (val phase = currentPhase) {
            is TaskPhase.Idle -> ExpectedAction.UserInput("Describe your task")
            is TaskPhase.Clarifying -> {
                if (phase.pendingQuestions.isNotEmpty()) {
                    ExpectedAction.UserInput("Answer: ${phase.pendingQuestions.first()}")
                } else {
                    ExpectedAction.SystemAction("Proceeding to planning")
                }
            }
            is TaskPhase.Planning -> {
                if (phase.planSteps.isEmpty()) {
                    ExpectedAction.SystemAction("AI is creating a plan")
                } else {
                    ExpectedAction.Approval("Review and approve the plan")
                }
            }
            is TaskPhase.Executing -> {
                if (!phase.isStepApproved) {
                    ExpectedAction.Approval("Approve step ${phase.currentStepIndex + 1}")
                } else {
                    ExpectedAction.SystemAction("Executing step")
                }
            }
            is TaskPhase.Validating -> {
                if (!phase.isApproved) {
                    ExpectedAction.Approval("Approve validation results")
                } else {
                    ExpectedAction.SystemAction("Completing task")
                }
            }
            is TaskPhase.Completed -> ExpectedAction.None
            is TaskPhase.Paused -> ExpectedAction.UserInput("Resume or cancel task")
        }

    /**
     * Check if user action is required
     */
    val requiresUserAction: Boolean
        get() = expectedAction is ExpectedAction.UserInput ||
                expectedAction is ExpectedAction.Approval

    /**
     * Get latest artifact of a specific type
     */
    inline fun <reified T : TaskArtifact> getLatestArtifact(): T? {
        return artifacts.filterIsInstance<T>().lastOrNull()
    }

    /**
     * Get plan steps from planning artifact
     */
    val planSteps: List<PlanStep>
        get() = getLatestArtifact<TaskArtifact.Plan>()?.steps ?: emptyList()

    companion object {
        fun new(taskId: String, request: String, timestamp: Long): TaskState {
            return TaskState(
                taskId = taskId,
                originalRequest = request,
                currentPhase = TaskPhase.Idle,
                createdAt = timestamp,
                updatedAt = timestamp
            )
        }
    }
}

@Serializable
sealed class ExpectedAction {
    @Serializable
    data class UserInput(val prompt: String) : ExpectedAction()

    @Serializable
    data class Approval(val description: String) : ExpectedAction()

    @Serializable
    data class SystemAction(val description: String) : ExpectedAction()

    @Serializable
    data object None : ExpectedAction()
}

@Serializable
data class PhaseTransition(
    val fromPhase: String,
    val toPhase: String,
    val timestamp: Long,
    val reason: String = "",
    val artifactId: String? = null
)

@Serializable
data class TaskMetadata(
    val priority: TaskPriority = TaskPriority.NORMAL,
    val tags: List<String> = emptyList(),
    val estimatedSteps: Int = 0,
    val completedSteps: Int = 0
)

@Serializable
enum class TaskPriority {
    LOW, NORMAL, HIGH, URGENT
}
