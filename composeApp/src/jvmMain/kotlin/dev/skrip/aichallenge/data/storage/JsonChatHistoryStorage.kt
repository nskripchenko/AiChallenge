package dev.skrip.aichallenge.data.storage

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

    override suspend fun saveHistory(messages: List<Message>) = withContext(Dispatchers.IO) {
        val file = File(filePath)
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(messages))
    }

    override suspend fun loadHistory(): List<Message> = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) return@withContext emptyList()

        try {
            json.decodeFromString<List<Message>>(file.readText())
        } catch (e: Exception) {
            emptyList()
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
