package dev.skrip.aichallenge.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ConversationState(
    val messages: List<Message> = emptyList(),
    val summary: String? = null,
    val summarizedCount: Int = 0
) {
    val unsummarizedCount: Int
        get() = messages.size - summarizedCount

    fun needsCompression(keepRecent: Int): Boolean {
        return unsummarizedCount > keepRecent * 2
    }

    fun getMessagesToSummarize(keepRecent: Int): List<Message> {
        if (!needsCompression(keepRecent)) return emptyList()
        val endIndex = messages.size - keepRecent
        return messages.subList(summarizedCount, endIndex)
    }

    fun getRecentMessages(keepRecent: Int): List<Message> {
        return messages.takeLast(keepRecent)
    }
}
