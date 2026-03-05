package dev.skrip.aichallenge.ui.state

import dev.skrip.aichallenge.domain.model.ModelId
import dev.skrip.aichallenge.domain.model.UserProfile
import dev.skrip.aichallenge.domain.statemachine.PlanStep
import dev.skrip.aichallenge.domain.statemachine.ValidationCheck

sealed class ChatViewEvent {
    // Chat events
    data object SendClicked : ChatViewEvent()
    data object StopGeneration : ChatViewEvent()
    data object ClearHistory : ChatViewEvent()
    data class InputChanged(val text: String) : ChatViewEvent()

    // Settings events
    data class SystemPromptChanged(val text: String) : ChatViewEvent()
    data class ModelChanged(val model: ModelId) : ChatViewEvent()
    data class TemperatureChanged(val text: String) : ChatViewEvent()
    data class MaxTokensChanged(val text: String) : ChatViewEvent()
    data class HistoryTokenLimitChanged(val text: String) : ChatViewEvent()

    // Working memory (notes)
    data class AddToWorkingMemory(val label: String, val content: String) : ChatViewEvent()
    data class RemoveFromWorkingMemory(val itemId: String) : ChatViewEvent()

    // Profile
    data class UpdateProfile(val profile: UserProfile) : ChatViewEvent()

    // Memory
    data object ClearAllMemory : ChatViewEvent()

    // Task State Machine events
    data class StartTask(val request: String) : ChatViewEvent()
    data class StartClarifying(val questions: List<String>) : ChatViewEvent()
    data class AnswerQuestion(val question: String, val answer: String) : ChatViewEvent()
    data class CompleteClarifying(val requirements: String) : ChatViewEvent()
    data object StartPlanning : ChatViewEvent()
    data class SubmitPlan(
        val steps: List<PlanStep>,
        val complexity: String,
        val risks: List<String> = emptyList()
    ) : ChatViewEvent()
    data object ApprovePlan : ChatViewEvent()
    data class RejectPlan(val reason: String, val goToClarifying: Boolean = false) : ChatViewEvent()
    data class CompleteStep(val output: String, val success: Boolean, val error: String? = null) : ChatViewEvent()
    data object ApproveStep : ChatViewEvent()
    data class SubmitValidation(val checks: List<ValidationCheck>, val summary: String) : ChatViewEvent()
    data object ApproveValidation : ChatViewEvent()
    data class PauseTask(val reason: String = "") : ChatViewEvent()
    data object ResumeTask : ChatViewEvent()
    data object CancelTask : ChatViewEvent()
}
