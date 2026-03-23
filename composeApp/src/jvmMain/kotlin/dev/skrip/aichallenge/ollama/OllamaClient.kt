package dev.skrip.aichallenge.ollama

import dev.skrip.aichallenge.model.Config
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class EmbeddingRequest(
    val model: String,
    val prompt: String
)

@Serializable
data class EmbeddingResponse(
    val embedding: List<Float>
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatResponse(
    val message: ChatMessage
)

class OllamaClient(
    private val baseUrl: String = Config.OLLAMA_BASE_URL,
    private val embeddingModel: String = Config.EMBEDDING_MODEL,
    private val chatModel: String = Config.CHAT_MODEL
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000  // 2 minutes for LLM responses
            connectTimeoutMillis = 10_000   // 10 seconds to connect
            socketTimeoutMillis = 120_000   // 2 minutes for socket
        }
    }

    suspend fun getEmbedding(text: String): List<Float> {
        val response = client.post("$baseUrl/api/embeddings") {
            contentType(ContentType.Application.Json)
            setBody(EmbeddingRequest(model = embeddingModel, prompt = text))
        }
        return response.body<EmbeddingResponse>().embedding
    }

    suspend fun getEmbeddings(texts: List<String>): List<List<Float>> {
        return texts.map { getEmbedding(it) }
    }

    suspend fun chat(
        userMessage: String,
        context: String? = null
    ): String {
        val messages = mutableListOf<ChatMessage>()

        if (context != null) {
            messages.add(ChatMessage(
                role = "system",
                content = """You are a helpful assistant that answers questions based only on the provided context.
                    |If the answer cannot be found in the context, say "I don't have enough information to answer this question."
                    |Be concise and direct in your answers.
                    |
                    |Context:
                    |$context""".trimMargin()
            ))
        }

        messages.add(ChatMessage(role = "user", content = userMessage))

        val response = client.post("$baseUrl/api/chat") {
            contentType(ContentType.Application.Json)
            setBody(ChatRequest(
                model = chatModel,
                messages = messages,
                stream = false
            ))
        }

        // Ollama returns ndjson - multiple JSON lines with partial content
        // Concatenate all message.content parts
        val responseText = response.bodyAsText()
        val lines = responseText.trim().lines().filter { it.isNotBlank() }

        val fullContent = StringBuilder()
        for (line in lines) {
            try {
                val part = json.decodeFromString<ChatResponse>(line)
                fullContent.append(part.message.content)
            } catch (e: Exception) {
                // Skip malformed lines
            }
        }

        return fullContent.toString()
    }

    /**
     * Grounded chat - строгий режим с анти-галлюцинациями
     */
    suspend fun chatGrounded(
        userMessage: String,
        context: String
    ): String {
        val messages = listOf(
            ChatMessage(
                role = "system",
                content = """You are a precise assistant that answers questions ONLY based on the provided context.

STRICT RULES:
1. Answer ONLY using information from the context below
2. If the context doesn't contain the answer, say "I cannot find this information in the provided documents"
3. Do NOT add information from your general knowledge
4. Be concise and factual
5. Quote or paraphrase directly from the context when possible

Context:
$context"""
            ),
            ChatMessage(role = "user", content = userMessage)
        )

        val response = client.post("$baseUrl/api/chat") {
            contentType(ContentType.Application.Json)
            setBody(ChatRequest(
                model = chatModel,
                messages = messages,
                stream = false
            ))
        }

        val responseText = response.bodyAsText()
        val lines = responseText.trim().lines().filter { it.isNotBlank() }

        val fullContent = StringBuilder()
        for (line in lines) {
            try {
                val part = json.decodeFromString<ChatResponse>(line)
                fullContent.append(part.message.content)
            } catch (e: Exception) {
                // Skip malformed lines
            }
        }

        return fullContent.toString()
    }

    /**
     * Прямой запрос к LLM без контекста (режим PLAIN)
     */
    suspend fun chatPlain(userMessage: String): String {
        val messages = listOf(
            ChatMessage(
                role = "system",
                content = "You are a helpful assistant. Answer questions concisely and directly."
            ),
            ChatMessage(role = "user", content = userMessage)
        )

        val response = client.post("$baseUrl/api/chat") {
            contentType(ContentType.Application.Json)
            setBody(ChatRequest(
                model = chatModel,
                messages = messages,
                stream = false
            ))
        }

        val responseText = response.bodyAsText()
        val lines = responseText.trim().lines().filter { it.isNotBlank() }

        val fullContent = StringBuilder()
        for (line in lines) {
            try {
                val part = json.decodeFromString<ChatResponse>(line)
                fullContent.append(part.message.content)
            } catch (e: Exception) {
                // Skip malformed lines
            }
        }

        return fullContent.toString()
    }

    /**
     * Day 25: Chat с историей диалога
     *
     * @param systemPrompt Динамический system prompt (от SystemPromptBuilder)
     * @param history История диалога [(role, content), ...]
     * @param userMessage Текущее сообщение пользователя
     */
    suspend fun chatWithHistory(
        systemPrompt: String,
        history: List<Pair<String, String>>,
        userMessage: String
    ): String {
        val messages = mutableListOf<ChatMessage>()

        // 1. System prompt
        messages.add(ChatMessage(role = "system", content = systemPrompt))

        // 2. Conversation history
        for ((role, content) in history) {
            messages.add(ChatMessage(role = role, content = content))
        }

        // 3. Current user message
        messages.add(ChatMessage(role = "user", content = userMessage))

        val response = client.post("$baseUrl/api/chat") {
            contentType(ContentType.Application.Json)
            setBody(ChatRequest(
                model = chatModel,
                messages = messages,
                stream = false
            ))
        }

        val responseText = response.bodyAsText()
        val lines = responseText.trim().lines().filter { it.isNotBlank() }

        val fullContent = StringBuilder()
        for (line in lines) {
            try {
                val part = json.decodeFromString<ChatResponse>(line)
                fullContent.append(part.message.content)
            } catch (e: Exception) {
                // Skip malformed lines
            }
        }

        return fullContent.toString()
    }

    suspend fun isAvailable(): Boolean {
        return try {
            client.get("$baseUrl/api/tags")
            true
        } catch (e: Exception) {
            false
        }
    }

    fun close() {
        client.close()
    }
}
