package dev.skrip.aichallenge.domain.repository

import dev.skrip.aichallenge.ui.state.Fact

interface FactsExtractor {
    /**
     * Extracts or updates facts from the latest conversation exchange.
     * Returns updated list of facts.
     */
    suspend fun extractFacts(
        userMessage: String,
        assistantResponse: String,
        existingFacts: List<Fact>
    ): List<Fact>
}
