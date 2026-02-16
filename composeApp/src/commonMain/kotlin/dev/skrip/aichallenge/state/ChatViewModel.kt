package dev.skrip.aichallenge.state

import dev.skrip.aichallenge.data.AnthropicClient
import dev.skrip.aichallenge.domain.ChatMessage
import dev.skrip.aichallenge.domain.Role
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ChatViewModel(
    private val client: AnthropicClient,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _state = MutableStateFlow(ChatState())
    val state = _state.asStateFlow()

    fun onInputChange(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    fun onSend() {
        val text = _state.value.inputText.trim()
        if (text.isEmpty() || _state.value.isLoading) return

        _state.update {
            it.copy(
                messages = it.messages + ChatMessage(role = Role.USER, content = text),
                inputText = "",
                isLoading = true
            )
        }

        scope.launch {
            val result = client.sendMessage(_state.value.messages)
            val newMessage = result.fold(
                onSuccess = { ChatMessage(role = Role.ASSISTANT, content = it) },
                onFailure = { ChatMessage(role = Role.ERROR, content = "Ошибка: ${it.message}") }
            )
            _state.update { it.copy(messages = it.messages + newMessage, isLoading = false) }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
private fun ChatMessage(role: Role, content: String) = ChatMessage(
    id = Uuid.random().toString(),
    role = role,
    content = content
)
