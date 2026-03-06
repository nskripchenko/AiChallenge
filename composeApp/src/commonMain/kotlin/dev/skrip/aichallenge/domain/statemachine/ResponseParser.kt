package dev.skrip.aichallenge.domain.statemachine

/**
 * Parses AI responses to extract structured artifacts for state machine transitions.
 */
object ResponseParser {

    /**
     * Parse response and determine what action to take
     */
    fun parse(response: String, currentPhase: TaskPhase): ParsedResponse {
        return when (currentPhase) {
            is TaskPhase.Idle -> parseIdleResponse(response)
            is TaskPhase.Clarifying -> parseClarifyingResponse(response, currentPhase)
            is TaskPhase.Planning -> parsePlanningResponse(response)
            is TaskPhase.Executing -> parseExecutingResponse(response, currentPhase)
            is TaskPhase.Validating -> parseValidatingResponse(response)
            is TaskPhase.Completed -> ParsedResponse.NoAction
            is TaskPhase.Paused -> ParsedResponse.NoAction
        }
    }

    private fun parseIdleResponse(response: String): ParsedResponse {
        // Only parse structured output - don't try to extract plans from regular responses
        // This prevents triggering Task State Machine on simple Q&A or invariant refusals

        // Check for explicit plan block (requires intentional formatting)
        if (response.contains("```plan", ignoreCase = true)) {
            val plan = extractPlan(response)
            if (plan != null && plan.steps.isNotEmpty()) {
                return ParsedResponse.SubmitPlan(plan)
            }
        }

        // Check if AI is explicitly asking clarifying questions with Q1:, Q2: format
        if (response.contains("Q1:", ignoreCase = true) || response.contains("Q1.", ignoreCase = true)) {
            val questions = extractQuestions(response)
            if (questions.isNotEmpty()) {
                return ParsedResponse.StartClarifying(questions)
            }
        }

        // Default: no action - don't auto-start task state machine
        // Task will only be started when AI explicitly provides structured output
        return ParsedResponse.NoAction
    }

    private fun parseClarifyingResponse(response: String, phase: TaskPhase.Clarifying): ParsedResponse {
        // Check if requirements are complete
        if (response.contains("```requirements", ignoreCase = true) ||
            response.contains("REQUIREMENTS COMPLETE", ignoreCase = true) ||
            response.contains("Ready to create", ignoreCase = true)) {
            val requirements = extractRequirements(response)
            return ParsedResponse.CompleteRequirements(requirements)
        }

        // Check for more questions
        val questions = extractQuestions(response)
        if (questions.isNotEmpty()) {
            return ParsedResponse.AddQuestions(questions)
        }

        return ParsedResponse.NoAction
    }

    private fun parsePlanningResponse(response: String): ParsedResponse {
        // Only extract plan if there's explicit structured output
        // This prevents parsing regular numbered lists as plans
        if (response.contains("```plan", ignoreCase = true) ||
            response.contains("ПЛАН ВЫПОЛНЕНИЯ", ignoreCase = true) ||
            response.contains("EXECUTION PLAN", ignoreCase = true)) {
            val plan = extractPlan(response)
            if (plan != null && plan.steps.size >= 2) { // Require at least 2 steps
                return ParsedResponse.SubmitPlan(plan)
            }
        }
        return ParsedResponse.NoAction
    }

    private fun parseExecutingResponse(response: String, phase: TaskPhase.Executing): ParsedResponse {
        // Check for step completion
        if (response.contains("```result", ignoreCase = true) ||
            response.contains("STATUS:", ignoreCase = true)) {
            val result = extractStepResult(response)
            return ParsedResponse.CompleteStep(
                output = result.output,
                success = result.success
            )
        }
        return ParsedResponse.NoAction
    }

    private fun parseValidatingResponse(response: String): ParsedResponse {
        if (response.contains("```validation", ignoreCase = true) ||
            response.contains("VALIDATION RESULTS", ignoreCase = true)) {
            val validation = extractValidation(response)
            return ParsedResponse.SubmitValidation(
                checks = validation.checks,
                summary = validation.summary,
                allPassed = validation.allPassed
            )
        }
        return ParsedResponse.NoAction
    }

    // Extraction helpers

    private fun extractQuestions(response: String): List<String> {
        val questions = mutableListOf<String>()

        // Pattern: Q1: ..., Q2: ..., etc.
        val qPattern = Regex("""Q\d+[:\.]?\s*(.+?)(?=Q\d+[:\.]|$)""", RegexOption.DOT_MATCHES_ALL)
        qPattern.findAll(response).forEach { match ->
            val question = match.groupValues[1].trim()
            if (question.isNotBlank() && question.length > 5) {
                questions.add(question.lines().first().trim())
            }
        }

        if (questions.isEmpty()) {
            // Pattern: - Question? or * Question?
            val bulletPattern = Regex("""^[\-\*]\s*(.+\?)""", RegexOption.MULTILINE)
            bulletPattern.findAll(response).forEach { match ->
                questions.add(match.groupValues[1].trim())
            }
        }

        return questions.take(5) // Max 5 questions
    }

    private fun extractRequirements(response: String): String {
        // Extract from ```requirements block
        val blockPattern = Regex("""```requirements\s*\n(.*?)```""", RegexOption.DOT_MATCHES_ALL)
        blockPattern.find(response)?.let {
            return it.groupValues[1].trim()
        }

        // Fallback: return the whole response
        return response.take(1000)
    }

    private fun extractPlan(response: String): ExtractedPlan? {
        val steps = mutableListOf<PlanStep>()
        var complexity = "Medium"
        val risks = mutableListOf<String>()

        // Extract from ```plan block or STEP patterns
        val planContent = run {
            val blockPattern = Regex("""```plan\s*\n(.*?)```""", RegexOption.DOT_MATCHES_ALL)
            blockPattern.find(response)?.groupValues?.get(1) ?: response
        }

        // Extract steps: STEP N: Title or N. Title
        val stepPattern = Regex(
            """(?:STEP\s*)?(\d+)[:\.\)]\s*(.+?)(?:Description:|Описание:)?\s*(.+?)(?:Expected(?:\s*Output)?:|Ожидаемый результат:)\s*(.+?)(?=(?:STEP\s*)?\d+[:\.\)]|COMPLEXITY|RISKS|$)""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )

        stepPattern.findAll(planContent).forEachIndexed { index, match ->
            val title = match.groupValues[2].trim().lines().first()
            val description = match.groupValues[3].trim().lines().first()
            val expected = match.groupValues[4].trim().lines().first()

            steps.add(PlanStep(
                index = index,
                title = title,
                description = description,
                expectedOutput = expected
            ))
        }

        // Simpler pattern if complex one fails
        if (steps.isEmpty()) {
            val simplePattern = Regex("""(?:STEP\s*)?(\d+)[:\.\)]\s*\*?\*?([^\n]+)""", RegexOption.IGNORE_CASE)
            simplePattern.findAll(planContent).forEachIndexed { index, match ->
                val title = match.groupValues[2].trim().removeSurrounding("**")
                if (title.length > 3 && !title.startsWith("http")) {
                    steps.add(PlanStep(
                        index = index,
                        title = title,
                        description = title,
                        expectedOutput = "Completed"
                    ))
                }
            }
        }

        // Even simpler: just numbered list like "1. Do something"
        if (steps.isEmpty()) {
            val numberedPattern = Regex("""^(\d+)\.\s+(.+)$""", RegexOption.MULTILINE)
            numberedPattern.findAll(planContent).forEachIndexed { index, match ->
                val title = match.groupValues[2].trim()
                if (title.length > 5 && index < 10) {
                    steps.add(PlanStep(
                        index = index,
                        title = title,
                        description = title,
                        expectedOutput = "Completed"
                    ))
                }
            }
        }

        if (steps.isEmpty()) return null

        // Extract complexity
        val complexityPattern = Regex("""COMPLEXITY[:\s]*(\w+)""", RegexOption.IGNORE_CASE)
        complexityPattern.find(planContent)?.let {
            complexity = it.groupValues[1]
        }

        // Extract risks
        val risksSection = Regex("""RISKS?:(.*?)(?=DEPENDENCIES|$)""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        risksSection.find(planContent)?.let { match ->
            val riskPattern = Regex("""[\-\*]\s*(.+)""")
            riskPattern.findAll(match.groupValues[1]).forEach {
                risks.add(it.groupValues[1].trim())
            }
        }

        return ExtractedPlan(steps, complexity, risks)
    }

    private fun extractStepResult(response: String): StepResult {
        var output = response
        var success = true

        // Extract from ```result block
        val blockPattern = Regex("""```result\s*\n(.*?)```""", RegexOption.DOT_MATCHES_ALL)
        blockPattern.find(response)?.let {
            output = it.groupValues[1].trim()
        }

        // Check status
        val statusPattern = Regex("""STATUS[:\s]*(SUCCESS|FAILED|OK|ERROR)""", RegexOption.IGNORE_CASE)
        statusPattern.find(response)?.let {
            val status = it.groupValues[1].uppercase()
            success = status == "SUCCESS" || status == "OK"
        }

        // Extract output section
        val outputPattern = Regex("""OUTPUT[:\s]*\n(.+?)(?=STATUS|$)""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        outputPattern.find(response)?.let {
            output = it.groupValues[1].trim()
        }

        return StepResult(output.take(500), success)
    }

    private fun extractValidation(response: String): ValidationResult {
        val checks = mutableListOf<ValidationCheck>()
        var summary = ""
        var allPassed = true

        val content = run {
            val blockPattern = Regex("""```validation\s*\n(.*?)```""", RegexOption.DOT_MATCHES_ALL)
            blockPattern.find(response)?.groupValues?.get(1) ?: response
        }

        // Extract checks: CHECK N: ... Status: PASS/FAIL
        val checkPattern = Regex(
            """CHECK\s*\d*[:\s]*(.+?)Status[:\s]*(PASS|FAIL)(?:.*?Details[:\s]*(.+?))?(?=CHECK|OVERALL|SUMMARY|$)""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )

        checkPattern.findAll(content).forEach { match ->
            val name = match.groupValues[1].trim().lines().first()
            val passed = match.groupValues[2].uppercase() == "PASS"
            val details = match.groupValues.getOrNull(3)?.trim()?.lines()?.firstOrNull() ?: ""

            if (!passed) allPassed = false

            checks.add(ValidationCheck(
                name = name,
                description = name,
                passed = passed,
                details = details
            ))
        }

        // Simpler pattern: [PASS] or [FAIL] prefix
        if (checks.isEmpty()) {
            val simplePattern = Regex("""\[(PASS|FAIL|OK)\]\s*(.+)""", RegexOption.IGNORE_CASE)
            simplePattern.findAll(content).forEach { match ->
                val passed = match.groupValues[1].uppercase() in listOf("PASS", "OK")
                val name = match.groupValues[2].trim()
                if (!passed) allPassed = false
                checks.add(ValidationCheck(name, name, passed))
            }
        }

        // Extract summary
        val summaryPattern = Regex("""SUMMARY[:\s]*(.+?)$""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        summaryPattern.find(content)?.let {
            summary = it.groupValues[1].trim().take(200)
        }

        // Check overall
        val overallPattern = Regex("""OVERALL[:\s]*(PASS|FAIL)""", RegexOption.IGNORE_CASE)
        overallPattern.find(content)?.let {
            allPassed = it.groupValues[1].uppercase() == "PASS"
        }

        // Default check if none found
        if (checks.isEmpty()) {
            checks.add(ValidationCheck("Task Review", "Manual review required", true))
        }

        return ValidationResult(checks, summary, allPassed)
    }

    // Data classes

    data class ExtractedPlan(
        val steps: List<PlanStep>,
        val complexity: String,
        val risks: List<String>
    )

    data class StepResult(
        val output: String,
        val success: Boolean
    )

    data class ValidationResult(
        val checks: List<ValidationCheck>,
        val summary: String,
        val allPassed: Boolean
    )
}

/**
 * Result of parsing an AI response
 */
sealed class ParsedResponse {
    data object NoAction : ParsedResponse()
    data object MoveToPlanningPhase : ParsedResponse()
    data class StartClarifying(val questions: List<String>) : ParsedResponse()
    data class AddQuestions(val questions: List<String>) : ParsedResponse()
    data class CompleteRequirements(val requirements: String) : ParsedResponse()
    data class SubmitPlan(val plan: ResponseParser.ExtractedPlan) : ParsedResponse()
    data class CompleteStep(val output: String, val success: Boolean) : ParsedResponse()
    data class SubmitValidation(
        val checks: List<ValidationCheck>,
        val summary: String,
        val allPassed: Boolean
    ) : ParsedResponse()
}
