package dev.skrip.aichallenge.data.repository

import dev.skrip.aichallenge.data.source.LlmDataSource
import dev.skrip.aichallenge.domain.model.Message
import dev.skrip.aichallenge.domain.model.ModelId
import dev.skrip.aichallenge.domain.model.Role
import dev.skrip.aichallenge.domain.repository.FactsExtractor
import dev.skrip.aichallenge.ui.state.Fact
import dev.skrip.aichallenge.util.currentTimeMillis
import dev.skrip.aichallenge.util.generateId

class HaikuFactsExtractor(
    private val llmDataSource: LlmDataSource
) : FactsExtractor {

    override suspend fun extractFacts(
        userMessage: String,
        assistantResponse: String,
        existingFacts: List<Fact>
    ): List<Fact> {
        val prompt = buildExtractionPrompt(userMessage, assistantResponse, existingFacts)

        val request = listOf(
            Message(
                id = generateId(),
                role = Role.USER,
                text = prompt,
                timestamp = currentTimeMillis()
            )
        )

        val result = llmDataSource.sendMessage(
            messages = request,
            model = ModelId.HAIKU_4_5.apiId,
            temperature = 0.1,
            maxTokens = 500,
            tag = "FACTS"
        )

        return result.map { response ->
            parseFacts(response.text, existingFacts)
        }.getOrElse {
            // On error, keep existing facts
            existingFacts
        }
    }

    private fun buildExtractionPrompt(
        userMessage: String,
        assistantResponse: String,
        existingFacts: List<Fact>
    ): String = buildString {
        appendLine("Extract important facts from this conversation exchange that should be remembered.")
        appendLine()
        appendLine("Current facts:")
        if (existingFacts.isEmpty()) {
            appendLine("(none)")
        } else {
            existingFacts.forEach { fact ->
                appendLine("- ${fact.key}: ${fact.value}")
            }
        }
        appendLine()
        appendLine("New exchange:")
        appendLine("User: $userMessage")
        appendLine("Assistant: $assistantResponse")
        appendLine()
        appendLine("Instructions:")
        appendLine("1. Extract new facts worth remembering (user preferences, names, technical details, decisions)")
        appendLine("2. Update existing facts if new information changes them")
        appendLine("3. Remove facts that are no longer relevant")
        appendLine("4. Keep facts concise - key should be 1-3 words, value 1-2 sentences max")
        appendLine()
        appendLine("Output ONLY the updated facts in this exact format (one per line):")
        appendLine("KEY: value")
        appendLine()
        appendLine("If no facts to remember, output: NONE")
    }

    private fun parseFacts(response: String, existingFacts: List<Fact>): List<Fact> {
        val trimmed = response.trim()

        if (trimmed.equals("NONE", ignoreCase = true) || trimmed.isEmpty()) {
            return existingFacts
        }

        val newFacts = mutableListOf<Fact>()
        val timestamp = currentTimeMillis()

        trimmed.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && it.contains(":") }
            .forEach { line ->
                val colonIndex = line.indexOf(":")
                if (colonIndex > 0) {
                    val key = line.substring(0, colonIndex).trim()
                    val value = line.substring(colonIndex + 1).trim()
                    if (key.isNotBlank() && value.isNotBlank()) {
                        newFacts.add(Fact(key = key, value = value, timestamp = timestamp))
                    }
                }
            }

        return if (newFacts.isNotEmpty()) newFacts else existingFacts
    }
}
