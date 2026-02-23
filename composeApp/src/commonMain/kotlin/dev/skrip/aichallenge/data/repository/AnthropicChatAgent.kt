package dev.skrip.aichallenge.data.repository

import dev.skrip.aichallenge.data.source.LlmDataSource
import dev.skrip.aichallenge.domain.model.AgentConfig
import dev.skrip.aichallenge.domain.model.Message
import dev.skrip.aichallenge.domain.model.Role
import dev.skrip.aichallenge.domain.repository.ChatAgent
import dev.skrip.aichallenge.util.currentTimeMillis
import dev.skrip.aichallenge.util.generateId

class AnthropicChatAgent(
    private val dataSource: LlmDataSource
) : ChatAgent {

    override suspend fun sendMessage(
        userMessage: String,
        history: List<Message>,
        config: AgentConfig
    ): Result<Message> {
        val messages = buildMessageList(userMessage, history, config)

        return dataSource.sendMessage(
            messages = messages,
            model = config.model.apiId,
            temperature = config.temperature,
            maxTokens = config.maxTokens
        )
    }

    private fun buildMessageList(
        userMessage: String,
        history: List<Message>,
        config: AgentConfig
    ): List<Message> = buildList {
        config.systemPrompt?.takeIf { it.isNotBlank() }?.let { prompt ->
            add(Message(
                id = generateId(),
                role = Role.SYSTEM,
                text = prompt,
                timestamp = currentTimeMillis()
            ))
        }
        addAll(history)
        add(Message(
            id = generateId(),
            role = Role.USER,
            text = userMessage,
            timestamp = currentTimeMillis()
        ))
    }
}
