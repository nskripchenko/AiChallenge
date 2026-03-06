package dev.skrip.aichallenge.domain.statemachine

import dev.skrip.aichallenge.util.currentTimeMillis
import dev.skrip.aichallenge.util.generateId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Deterministic state machine for task execution.
 *
 * Key principles:
 * 1. Transitions only happen when artifacts are approved
 * 2. Each phase has specific entry/exit conditions
 * 3. State can be persisted and resumed at any point
 * 4. No phase can be skipped
 */
class TaskStateMachine(
    private val storage: TaskStateStorage
) {
    private val _state = MutableStateFlow<TaskState?>(null)
    val state: StateFlow<TaskState?> = _state.asStateFlow()

    private val _error = MutableStateFlow<StateMachineError?>(null)
    val error: StateFlow<StateMachineError?> = _error.asStateFlow()

    /**
     * Initialize or restore state
     * Clears stale tasks that were left in incomplete states
     */
    suspend fun initialize() {
        val savedState = storage.loadState()

        // Clear stale tasks (planning without a submitted plan, etc.)
        // These are likely from interrupted sessions
        if (savedState != null) {
            val phase = savedState.currentPhase
            val shouldClear = when (phase) {
                // Planning phase with no plan steps is stale
                is TaskPhase.Planning -> phase.planSteps.isEmpty()
                // Clarifying without pending questions but incomplete is stale
                is TaskPhase.Clarifying -> true // Usually needs fresh start
                // Other phases might be resumable, but for simplicity reset
                is TaskPhase.Idle -> false // Already idle, keep it
                is TaskPhase.Completed -> false // Completed, keep for history
                else -> true // Reset executing/validating/paused for fresh start
            }

            if (shouldClear) {
                _state.value = null
                storage.clearState()
            } else {
                _state.value = savedState
            }
        } else {
            _state.value = null
        }
    }

    /**
     * Start a new task
     */
    fun startTask(request: String): Result<TaskState> {
        val currentState = _state.value
        if (currentState != null && currentState.currentPhase !is TaskPhase.Idle &&
            currentState.currentPhase !is TaskPhase.Completed) {
            return Result.failure(
                StateMachineError.InvalidTransition(
                    "Cannot start new task while another is in progress. " +
                    "Current phase: ${currentState.currentPhase.name}"
                )
            )
        }

        val now = currentTimeMillis()
        val newState = TaskState.new(
            taskId = generateId(),
            request = request,
            timestamp = now
        )

        _state.value = newState
        saveState()
        return Result.success(newState)
    }

    /**
     * Transition to clarifying phase with questions
     */
    fun startClarifying(questions: List<String>): Result<TaskState> {
        return transition { state ->
            validateTransition(state.currentPhase, "clarifying")

            val newPhase = TaskPhase.Clarifying(
                questions = questions,
                pendingQuestions = questions
            )

            state.copy(
                currentPhase = newPhase,
                history = state.history + PhaseTransition(
                    fromPhase = state.currentPhase.name,
                    toPhase = "clarifying",
                    timestamp = currentTimeMillis(),
                    reason = "Starting requirements gathering"
                ),
                updatedAt = currentTimeMillis()
            )
        }
    }

    /**
     * Answer a clarifying question
     */
    fun answerQuestion(question: String, answer: String): Result<TaskState> {
        return transition { state ->
            val phase = state.currentPhase as? TaskPhase.Clarifying
                ?: throw StateMachineError.InvalidState("Not in clarifying phase")

            val newAnswers = phase.answers + (question to answer)
            val newPending = phase.pendingQuestions - question

            state.copy(
                currentPhase = phase.copy(
                    answers = newAnswers,
                    pendingQuestions = newPending
                ),
                updatedAt = currentTimeMillis()
            )
        }
    }

    /**
     * Complete clarifying phase and produce requirements artifact
     */
    fun completeClarifying(finalRequirements: String): Result<TaskState> {
        return transition { state ->
            val phase = state.currentPhase as? TaskPhase.Clarifying
                ?: throw StateMachineError.InvalidState("Not in clarifying phase")

            if (phase.pendingQuestions.isNotEmpty()) {
                throw StateMachineError.InvalidTransition(
                    "Cannot complete clarifying: ${phase.pendingQuestions.size} questions unanswered"
                )
            }

            val artifact = TaskArtifact.Requirements(
                timestamp = currentTimeMillis(),
                originalRequest = state.originalRequest,
                clarifications = phase.answers,
                finalRequirements = finalRequirements
            )

            state.copy(
                artifacts = state.artifacts + artifact,
                updatedAt = currentTimeMillis()
            )
        }
    }

    /**
     * Transition to planning phase
     */
    fun startPlanning(): Result<TaskState> {
        return transition { state ->
            validateTransition(state.currentPhase, "planning")

            state.copy(
                currentPhase = TaskPhase.Planning(),
                history = state.history + PhaseTransition(
                    fromPhase = state.currentPhase.name,
                    toPhase = "planning",
                    timestamp = currentTimeMillis(),
                    reason = "Starting plan creation"
                ),
                updatedAt = currentTimeMillis()
            )
        }
    }

    /**
     * Submit plan for approval
     */
    fun submitPlan(
        steps: List<PlanStep>,
        complexity: String,
        risks: List<String> = emptyList()
    ): Result<TaskState> {
        return transition { state ->
            val phase = state.currentPhase as? TaskPhase.Planning
                ?: throw StateMachineError.InvalidState("Not in planning phase")

            val artifact = TaskArtifact.Plan(
                timestamp = currentTimeMillis(),
                steps = steps,
                estimatedComplexity = complexity,
                risks = risks
            )

            state.copy(
                currentPhase = phase.copy(planSteps = steps),
                artifacts = state.artifacts + artifact,
                metadata = state.metadata.copy(estimatedSteps = steps.size),
                updatedAt = currentTimeMillis()
            )
        }
    }

    /**
     * Approve plan and transition to execution
     */
    fun approvePlan(): Result<TaskState> {
        return transition { state ->
            val phase = state.currentPhase as? TaskPhase.Planning
                ?: throw StateMachineError.InvalidState("Not in planning phase")

            if (phase.planSteps.isEmpty()) {
                throw StateMachineError.InvalidTransition("No plan to approve")
            }

            // Mark plan artifact as approved
            val updatedArtifacts = state.artifacts.map { artifact ->
                if (artifact is TaskArtifact.Plan && !artifact.isApproved) {
                    artifact.copy(isApproved = true)
                } else artifact
            }

            state.copy(
                currentPhase = TaskPhase.Executing(
                    currentStepIndex = 0,
                    totalSteps = phase.planSteps.size
                ),
                artifacts = updatedArtifacts,
                history = state.history + PhaseTransition(
                    fromPhase = "planning",
                    toPhase = "executing",
                    timestamp = currentTimeMillis(),
                    reason = "Plan approved by user"
                ),
                updatedAt = currentTimeMillis()
            )
        }
    }

    /**
     * Reject plan and go back to planning or clarifying
     */
    fun rejectPlan(reason: String, goToClarifying: Boolean = false): Result<TaskState> {
        return transition { state ->
            val phase = state.currentPhase as? TaskPhase.Planning
                ?: throw StateMachineError.InvalidState("Not in planning phase")

            val targetPhase = if (goToClarifying) "clarifying" else "planning"

            state.copy(
                currentPhase = if (goToClarifying) {
                    TaskPhase.Clarifying(
                        questions = listOf("What changes would you like to the plan?"),
                        pendingQuestions = listOf("What changes would you like to the plan?")
                    )
                } else {
                    TaskPhase.Planning()
                },
                history = state.history + PhaseTransition(
                    fromPhase = "planning",
                    toPhase = targetPhase,
                    timestamp = currentTimeMillis(),
                    reason = "Plan rejected: $reason"
                ),
                updatedAt = currentTimeMillis()
            )
        }
    }

    /**
     * Complete current execution step
     */
    fun completeStep(output: String, success: Boolean, error: String? = null): Result<TaskState> {
        return transition { state ->
            val phase = state.currentPhase as? TaskPhase.Executing
                ?: throw StateMachineError.InvalidState("Not in executing phase")

            val stepResult = ExecutionResult(
                stepIndex = phase.currentStepIndex,
                output = output,
                success = success,
                error = error
            )

            val stepTitle = state.planSteps.getOrNull(phase.currentStepIndex)?.title ?: "Step ${phase.currentStepIndex + 1}"

            val stepArtifact = TaskArtifact.StepExecution(
                timestamp = currentTimeMillis(),
                stepIndex = phase.currentStepIndex,
                stepTitle = stepTitle,
                output = output,
                success = success
            )

            state.copy(
                currentPhase = phase.copy(
                    completedSteps = phase.completedSteps + stepResult,
                    isStepApproved = false // Reset for next step
                ),
                artifacts = state.artifacts + stepArtifact,
                metadata = state.metadata.copy(completedSteps = phase.completedSteps.size + 1),
                updatedAt = currentTimeMillis()
            )
        }
    }

    /**
     * Approve current step and move to next
     */
    fun approveStep(): Result<TaskState> {
        return transition { state ->
            val phase = state.currentPhase as? TaskPhase.Executing
                ?: throw StateMachineError.InvalidState("Not in executing phase")

            // Mark latest step artifact as approved
            val updatedArtifacts = state.artifacts.mapIndexed { index, artifact ->
                if (index == state.artifacts.lastIndex && artifact is TaskArtifact.StepExecution) {
                    artifact.copy(isApproved = true)
                } else artifact
            }

            val nextStepIndex = phase.currentStepIndex + 1
            val isComplete = nextStepIndex >= phase.totalSteps

            if (isComplete) {
                // Move to validation
                state.copy(
                    currentPhase = TaskPhase.Validating(),
                    artifacts = updatedArtifacts,
                    history = state.history + PhaseTransition(
                        fromPhase = "executing",
                        toPhase = "validating",
                        timestamp = currentTimeMillis(),
                        reason = "All steps completed"
                    ),
                    updatedAt = currentTimeMillis()
                )
            } else {
                // Move to next step
                state.copy(
                    currentPhase = phase.copy(
                        currentStepIndex = nextStepIndex,
                        isStepApproved = true
                    ),
                    artifacts = updatedArtifacts,
                    updatedAt = currentTimeMillis()
                )
            }
        }
    }

    /**
     * Submit validation results
     */
    fun submitValidation(
        checks: List<ValidationCheck>,
        summary: String
    ): Result<TaskState> {
        return transition { state ->
            val phase = state.currentPhase as? TaskPhase.Validating
                ?: throw StateMachineError.InvalidState("Not in validating phase")

            val artifact = TaskArtifact.ValidationReport(
                timestamp = currentTimeMillis(),
                checks = checks,
                overallSuccess = checks.all { it.passed },
                summary = summary
            )

            state.copy(
                currentPhase = phase.copy(validationChecks = checks),
                artifacts = state.artifacts + artifact,
                updatedAt = currentTimeMillis()
            )
        }
    }

    /**
     * Approve validation and complete task
     */
    fun approveValidation(): Result<TaskState> {
        return transition { state ->
            val phase = state.currentPhase as? TaskPhase.Validating
                ?: throw StateMachineError.InvalidState("Not in validating phase")

            if (phase.validationChecks.isEmpty()) {
                throw StateMachineError.InvalidTransition("No validation to approve")
            }

            // Mark validation artifact as approved
            val updatedArtifacts = state.artifacts.map { artifact ->
                if (artifact is TaskArtifact.ValidationReport && !artifact.isApproved) {
                    artifact.copy(isApproved = true)
                } else artifact
            }

            val completionArtifact = TaskArtifact.CompletionSummary(
                timestamp = currentTimeMillis(),
                taskDescription = state.originalRequest,
                allArtifacts = state.artifacts.map { it.phaseId },
                finalSummary = "Task completed successfully"
            )

            state.copy(
                currentPhase = TaskPhase.Completed(
                    summary = "Task completed",
                    artifacts = state.artifacts.map { it.phaseId }
                ),
                artifacts = updatedArtifacts + completionArtifact,
                history = state.history + PhaseTransition(
                    fromPhase = "validating",
                    toPhase = "completed",
                    timestamp = currentTimeMillis(),
                    reason = "Validation approved"
                ),
                updatedAt = currentTimeMillis()
            )
        }
    }

    /**
     * Pause the task at any phase
     */
    fun pause(reason: String = ""): Result<TaskState> {
        return transition { state ->
            val currentPhase = state.currentPhase
            if (currentPhase is TaskPhase.Idle || currentPhase is TaskPhase.Completed) {
                throw StateMachineError.InvalidTransition("Cannot pause: task is ${currentPhase.name}")
            }

            state.copy(
                currentPhase = TaskPhase.Paused(
                    previousPhase = currentPhase.name,
                    pauseReason = reason
                ),
                history = state.history + PhaseTransition(
                    fromPhase = currentPhase.name,
                    toPhase = "paused",
                    timestamp = currentTimeMillis(),
                    reason = "Paused: $reason"
                ),
                updatedAt = currentTimeMillis()
            )
        }
    }

    /**
     * Resume from paused state
     */
    fun resume(): Result<TaskState> {
        return transition { state ->
            val pausedPhase = state.currentPhase as? TaskPhase.Paused
                ?: throw StateMachineError.InvalidState("Task is not paused")

            // Find the last phase of the target type in history and restore it
            val restoredPhase = restorePhase(pausedPhase.previousPhase, state)

            state.copy(
                currentPhase = restoredPhase,
                history = state.history + PhaseTransition(
                    fromPhase = "paused",
                    toPhase = pausedPhase.previousPhase,
                    timestamp = currentTimeMillis(),
                    reason = "Resumed"
                ),
                updatedAt = currentTimeMillis()
            )
        }
    }

    /**
     * Cancel the current task
     */
    fun cancel(): Result<TaskState> {
        return transition { state ->
            state.copy(
                currentPhase = TaskPhase.Idle,
                history = state.history + PhaseTransition(
                    fromPhase = state.currentPhase.name,
                    toPhase = "idle",
                    timestamp = currentTimeMillis(),
                    reason = "Task cancelled"
                ),
                updatedAt = currentTimeMillis()
            )
        }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }

    // Private helpers

    private fun transition(block: (TaskState) -> TaskState): Result<TaskState> {
        val currentState = _state.value
            ?: return Result.failure(StateMachineError.NoActiveTask)

        return try {
            val newState = block(currentState)
            _state.value = newState
            saveState()
            Result.success(newState)
        } catch (e: StateMachineError) {
            _error.value = e
            Result.failure(e)
        }
    }

    private fun validateTransition(from: TaskPhase, to: String) {
        if (to !in from.allowedTransitions) {
            throw StateMachineError.InvalidTransition(
                "Cannot transition from ${from.name} to $to. " +
                "Allowed: ${from.allowedTransitions.joinToString()}"
            )
        }
    }

    private fun restorePhase(phaseName: String, state: TaskState): TaskPhase {
        return when (phaseName) {
            "clarifying" -> {
                val artifact = state.getLatestArtifact<TaskArtifact.Requirements>()
                TaskPhase.Clarifying(
                    questions = artifact?.clarifications?.keys?.toList() ?: emptyList(),
                    answers = artifact?.clarifications ?: emptyMap(),
                    pendingQuestions = emptyList()
                )
            }
            "planning" -> {
                val artifact = state.getLatestArtifact<TaskArtifact.Plan>()
                TaskPhase.Planning(
                    planSteps = artifact?.steps ?: emptyList(),
                    isApproved = artifact?.isApproved ?: false
                )
            }
            "executing" -> {
                val artifact = state.getLatestArtifact<TaskArtifact.Plan>()
                val completedSteps = state.artifacts
                    .filterIsInstance<TaskArtifact.StepExecution>()
                    .filter { it.isApproved }
                    .map { ExecutionResult(it.stepIndex, it.output, it.success) }

                TaskPhase.Executing(
                    currentStepIndex = completedSteps.size,
                    totalSteps = artifact?.steps?.size ?: 0,
                    completedSteps = completedSteps,
                    isStepApproved = false
                )
            }
            "validating" -> {
                val artifact = state.getLatestArtifact<TaskArtifact.ValidationReport>()
                TaskPhase.Validating(
                    validationChecks = artifact?.checks ?: emptyList(),
                    isApproved = artifact?.isApproved ?: false
                )
            }
            else -> TaskPhase.Idle
        }
    }

    private fun saveState() {
        _state.value?.let { storage.saveState(it) }
    }
}

sealed class StateMachineError : Exception() {
    data object NoActiveTask : StateMachineError() {
        private fun readResolve(): Any = NoActiveTask
        override val message = "No active task"
    }

    data class InvalidState(override val message: String) : StateMachineError()
    data class InvalidTransition(override val message: String) : StateMachineError()
}
