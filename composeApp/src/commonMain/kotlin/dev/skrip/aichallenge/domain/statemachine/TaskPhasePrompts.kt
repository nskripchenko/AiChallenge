package dev.skrip.aichallenge.domain.statemachine

/**
 * System prompts for each phase of the state machine.
 * Each phase has a specialized prompt that produces structured output for parsing.
 */
object TaskPhasePrompts {

    /**
     * Build the complete system prompt for the current phase
     */
    fun buildPrompt(state: TaskState): String {
        return when (val phase = state.currentPhase) {
            is TaskPhase.Idle -> IDLE_PROMPT
            is TaskPhase.Clarifying -> buildClarifyingPrompt(state, phase)
            is TaskPhase.Planning -> buildPlanningPrompt(state, phase)
            is TaskPhase.Executing -> buildExecutingPrompt(state, phase)
            is TaskPhase.Validating -> buildValidatingPrompt(state, phase)
            is TaskPhase.Completed -> COMPLETED_PROMPT
            is TaskPhase.Paused -> buildPausedPrompt(phase)
        }
    }

    private const val BREVITY_RULE = "КРАТКОСТЬ: Отвечай кратко и по делу. Максимум 2-3 предложения на каждый пункт."

    private val IDLE_PROMPT = """
You are a crypto investment assistant with structured task execution.

FOR SIMPLE QUESTIONS (prices, info, quick analysis):
- Answer directly without a plan (2-3 sentences max)

FOR TASKS THAT REQUIRE MULTIPLE STEPS (create strategy, analyze portfolio, build plan):
- Create an execution plan in this format:

```plan
STEP 1: [Action title]
Description: [1 sentence]
Expected Output: [1 sentence]

STEP 2: [Action title]
Description: [1 sentence]
Expected Output: [1 sentence]

COMPLEXITY: [Low/Medium/High]
```

$BREVITY_RULE
    """.trimIndent()

    private fun buildClarifyingPrompt(state: TaskState, phase: TaskPhase.Clarifying): String {
        return """
You are gathering requirements for: "${state.originalRequest}"

ANSWERED SO FAR:
${phase.answers.entries.joinToString("\n") { "Q: ${it.key}\nA: ${it.value}" }.ifEmpty { "(none)" }}

PENDING QUESTIONS:
${phase.pendingQuestions.joinToString("\n") { "- $it" }.ifEmpty { "(all answered)" }}

${if (phase.pendingQuestions.isEmpty()) """
All questions are answered. Summarize the requirements:

```requirements
[Write comprehensive requirements based on the original request and all clarifications]
```

Then output: "Requirements complete. Ready to create execution plan."
""" else """
Wait for the user to answer the pending questions.
If the user's response answers a question, acknowledge it.
If you need more questions, add them as:
Q1: [question]
Q2: [question]
"""}
        """.trimIndent()
    }

    private fun buildPlanningPrompt(state: TaskState, phase: TaskPhase.Planning): String {
        val requirements = state.getLatestArtifact<TaskArtifact.Requirements>()

        return if (phase.planSteps.isEmpty()) {
            """
TASK: ${state.originalRequest}
${requirements?.let { "REQUIREMENTS:\n${it.finalRequirements}" } ?: ""}

You MUST respond with an execution plan in this EXACT format:

```plan
STEP 1: [Action title]
Description: [What to do]
Expected Output: [Result]

STEP 2: [Action title]
Description: [What to do]
Expected Output: [Result]

STEP 3: [Action title]
Description: [What to do]
Expected Output: [Result]

COMPLEXITY: Medium
```

CRITICAL: Your response MUST contain ```plan block with numbered steps.

$BREVITY_RULE
            """.trimIndent()
        } else {
            """
Plan created and waiting for user approval.

CURRENT PLAN (${phase.planSteps.size} steps):
${phase.planSteps.joinToString("\n") { "Step ${it.index + 1}: ${it.title}" }}

The user will approve or reject the plan.
If approved, we proceed to execution.
If rejected, create a new plan based on feedback.
            """.trimIndent()
        }
    }

    private fun buildExecutingPrompt(state: TaskState, phase: TaskPhase.Executing): String {
        val currentStep = state.planSteps.getOrNull(phase.currentStepIndex)

        return """
You are executing step ${phase.currentStepIndex + 1} of ${phase.totalSteps}.

COMPLETED STEPS:
${phase.completedSteps.joinToString("\n") {
    val step = state.planSteps.getOrNull(it.stepIndex)
    "[DONE] Step ${it.stepIndex + 1}: ${step?.title ?: "Unknown"}"
}.ifEmpty { "(none yet)" }}

CURRENT STEP:
Title: ${currentStep?.title ?: "Unknown"}
Description: ${currentStep?.description ?: "No description"}
Expected Output: ${currentStep?.expectedOutput ?: "No expected output"}

Execute THIS step and report results in this EXACT format:

```result
STATUS: SUCCESS

OUTPUT:
[Describe what was accomplished]
[Include any relevant details, code snippets, or results]
```

Or if the step failed:

```result
STATUS: FAILED

OUTPUT:
[Describe what went wrong]
[Include error details]
```

RULES:
- Execute ONLY this step, not future steps
- Be specific about what was done
- STATUS must be SUCCESS or FAILED

$BREVITY_RULE
        """.trimIndent()
    }

    private fun buildValidatingPrompt(state: TaskState, phase: TaskPhase.Validating): String {
        val executionResults = state.artifacts.filterIsInstance<TaskArtifact.StepExecution>()

        return if (phase.validationChecks.isEmpty()) {
            """
You are validating the completed task.

ORIGINAL TASK: ${state.originalRequest}

EXECUTED STEPS:
${executionResults.joinToString("\n\n") {
    "Step ${it.stepIndex + 1}: ${it.stepTitle}\n" +
    "Status: ${if (it.success) "SUCCESS" else "FAILED"}\n" +
    "Output: ${it.output.take(200)}"
}}

Validate the results in this EXACT format:

```validation
CHECK 1: [What you're checking]
Status: PASS
Details: [Evidence or reasoning]

CHECK 2: [What you're checking]
Status: PASS
Details: [Evidence or reasoning]

CHECK 3: [What you're checking]
Status: FAIL
Details: [What's wrong]

OVERALL: PASS

SUMMARY: [Brief summary of validation results]
```

RULES:
- Check if all requirements were met
- Check if outputs are correct
- OVERALL is PASS only if all checks pass
- Be honest about failures

$BREVITY_RULE
            """.trimIndent()
        } else {
            """
Validation complete.

RESULTS:
${phase.validationChecks.joinToString("\n") { "${if (it.passed) "[PASS]" else "[FAIL]"} ${it.name}" }}

Overall: ${if (phase.allChecksPassed) "ALL PASSED" else "SOME FAILED"}

Waiting for user to approve or request fixes.
            """.trimIndent()
        }
    }

    private val COMPLETED_PROMPT = """
Task completed successfully.

You can:
- Summarize what was accomplished
- Answer questions about the work done
- Help with a new task (just describe it)
    """.trimIndent()

    private fun buildPausedPrompt(phase: TaskPhase.Paused): String {
        return """
Task is PAUSED.

Previous phase: ${phase.previousPhase}
Reason: ${phase.pauseReason.ifEmpty { "User requested pause" }}

Waiting for user to:
- Resume (continue from where we stopped)
- Cancel (abandon the task)
        """.trimIndent()
    }

    /**
     * Get a short status message for the current phase
     */
    fun getStatusMessage(state: TaskState): String {
        return when (val phase = state.currentPhase) {
            is TaskPhase.Idle -> "Ready"
            is TaskPhase.Clarifying -> {
                if (phase.pendingQuestions.isNotEmpty()) {
                    "Waiting for ${phase.pendingQuestions.size} answer(s)"
                } else {
                    "Requirements gathered"
                }
            }
            is TaskPhase.Planning -> {
                if (phase.planSteps.isEmpty()) "Creating plan..."
                else "Plan ready - awaiting approval"
            }
            is TaskPhase.Executing -> {
                "Step ${phase.currentStepIndex + 1}/${phase.totalSteps}"
            }
            is TaskPhase.Validating -> {
                if (phase.validationChecks.isEmpty()) "Validating..."
                else "Validation complete - awaiting approval"
            }
            is TaskPhase.Completed -> "Completed"
            is TaskPhase.Paused -> "Paused"
        }
    }
}
