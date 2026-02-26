package dev.skrip.aichallenge.data.storage

import dev.skrip.aichallenge.domain.model.ConversationState
import dev.skrip.aichallenge.domain.model.Message
import dev.skrip.aichallenge.domain.repository.ChatHistoryStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class JsonChatHistoryStorage(
    private val filePath: String = getDefaultFilePath()
) : ChatHistoryStorage {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override suspend fun saveState(state: ConversationState) = withContext(Dispatchers.IO) {
        val file = File(filePath)
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(state))
    }

    override suspend fun loadState(): ConversationState = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) return@withContext ConversationState()

        try {
            // Try to load new format (ConversationState)
            json.decodeFromString<ConversationState>(file.readText())
        } catch (e: Exception) {
            // Fallback: try to load old format (List<Message>) for migration
            try {
                val messages = json.decodeFromString<List<Message>>(file.readText())
                ConversationState(messages = messages)
            } catch (e2: Exception) {
                ConversationState()
            }
        }
    }

    override suspend fun clearHistory() = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (file.exists()) {
            file.delete()
        }
    }

    companion object {
        private fun getDefaultFilePath(): String {
            val userHome = System.getProperty("user.home")
            return "$userHome/.aichallenge/chat_history.json"
        }
    }
}
