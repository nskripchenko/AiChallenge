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
data class ChatOptions(
    val temperature: Float = Config.LLM.TEMPERATURE,
    val num_predict: Int = Config.LLM.MAX_TOKENS,
    val num_ctx: Int = Config.LLM.CONTEXT_SIZE,
    val top_p: Float = Config.LLM.TOP_P,
    val repeat_penalty: Float = Config.LLM.REPEAT_PENALTY
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false,
    val options: ChatOptions = ChatOptions()
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

    /**
     * Day 29: Optimized RAG chat with context
     */
    suspend fun chat(
        userMessage: String,
        context: String? = null
    ): String {
        val messages = mutableListOf<ChatMessage>()

        if (context != null) {
            messages.add(ChatMessage(
                role = "system",
                content = """Answer based on the provided context. Be concise and direct.
If the answer is not in the context, say so.

Context:
$context"""
            ))
        }

        messages.add(ChatMessage(role = "user", content = userMessage))

        // Day 29: Optimized parameters for RAG
        val ragOptions = ChatOptions(
            temperature = 0.3f,
            num_predict = Config.LLM.MAX_TOKENS,
            num_ctx = Config.LLM.CONTEXT_SIZE,
            top_p = Config.LLM.TOP_P,
            repeat_penalty = Config.LLM.REPEAT_PENALTY
        )

        val response = client.post("$baseUrl/api/chat") {
            contentType(ContentType.Application.Json)
            setBody(ChatRequest(
                model = chatModel,
                messages = messages,
                stream = false,
                options = ragOptions
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
     * Day 29: Optimized prompt and parameters for RAG
     */
    suspend fun chatGrounded(
        userMessage: String,
        context: String
    ): String {
        val messages = listOf(
            ChatMessage(
                role = "system",
                content = """You are a technical documentation assistant. Answer questions using ONLY the provided context.

RULES:
- Use ONLY facts from the context below
- If information is missing, say "Not found in documents"
- Be concise: 1-3 sentences when possible
- Use technical terms from the context
- No general knowledge or assumptions

CONTEXT:
$context"""
            ),
            ChatMessage(role = "user", content = userMessage)
        )

        // Day 29: Lower temperature for grounded mode (more deterministic)
        val groundedOptions = ChatOptions(
            temperature = 0.1f,  // Very low for factual answers
            num_predict = 256,   // Shorter answers for grounded mode
            num_ctx = Config.LLM.CONTEXT_SIZE,
            top_p = 0.8f,
            repeat_penalty = 1.1f
        )

        val response = client.post("$baseUrl/api/chat") {
            contentType(ContentType.Application.Json)
            setBody(ChatRequest(
                model = chatModel,
                messages = messages,
                stream = false,
                options = groundedOptions
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
     * Day 29: Higher temperature for creative responses
     */
    suspend fun chatPlain(userMessage: String): String {
        val messages = listOf(
            ChatMessage(
                role = "system",
                content = "You are a helpful assistant. Be concise and direct."
            ),
            ChatMessage(role = "user", content = userMessage)
        )

        // Day 29: Slightly higher temperature for plain mode (more creative)
        val plainOptions = ChatOptions(
            temperature = 0.5f,
            num_predict = Config.LLM.MAX_TOKENS,
            num_ctx = Config.LLM.CONTEXT_SIZE,
            top_p = Config.LLM.TOP_P,
            repeat_penalty = Config.LLM.REPEAT_PENALTY
        )

        val response = client.post("$baseUrl/api/chat") {
            contentType(ContentType.Application.Json)
            setBody(ChatRequest(
                model = chatModel,
                messages = messages,
                stream = false,
                options = plainOptions
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
     * Day 29: Optimized for multi-turn conversations
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

        // Day 29: Memory mode needs larger context for history
        val memoryOptions = ChatOptions(
            temperature = 0.3f,
            num_predict = Config.LLM.MAX_TOKENS,
            num_ctx = Config.LLM.CONTEXT_SIZE,
            top_p = Config.LLM.TOP_P,
            repeat_penalty = Config.LLM.REPEAT_PENALTY
        )

        val response = client.post("$baseUrl/api/chat") {
            contentType(ContentType.Application.Json)
            setBody(ChatRequest(
                model = chatModel,
                messages = messages,
                stream = false,
                options = memoryOptions
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
