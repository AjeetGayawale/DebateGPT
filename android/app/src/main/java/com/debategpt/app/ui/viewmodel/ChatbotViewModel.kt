package com.debategpt.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.debategpt.app.data.ApiClient
import com.debategpt.app.data.ChatRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatbotUiState(
    val topic: String = "",
    val stance: String = "favor",
    val messageInput: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ChatbotViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChatbotUiState())
    val uiState: StateFlow<ChatbotUiState> = _uiState.asStateFlow()

    fun setTopic(topic: String) {
        _uiState.value = _uiState.value.copy(topic = topic)
    }

    fun setStance(stance: String) {
        _uiState.value = _uiState.value.copy(stance = stance)
    }

    fun setMessageInput(input: String) {
        _uiState.value = _uiState.value.copy(messageInput = input)
    }

    fun sendMessage() {
        val topic = _uiState.value.topic.trim()
        val stance = _uiState.value.stance
        val message = _uiState.value.messageInput.trim()

        if (topic.isEmpty() || message.isEmpty()) return

        val userMessage = ChatMessage(text = message, isUser = true)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage,
            messageInput = "",
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            try {
                val response = ApiClient.api.chatbotRespond(
                    ChatRequest(topic = topic, stance = stance, message = message)
                )
                if (response.isSuccessful) {
                    val reply = response.body()?.reply ?: "No response"
                    val botMessage = ChatMessage(text = reply, isUser = false)
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + botMessage,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.message() ?: "Request failed"
                    )
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Network error"
                val friendlyMsg = if (msg.contains("ETIMEDOUT") || msg.contains("timed out") || msg.contains("failed to connect")) {
                    "Cannot reach backend. Test connection on Home screen first."
                } else msg
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = friendlyMsg
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetChat() {
        _uiState.value = ChatbotUiState(
            topic = _uiState.value.topic,
            stance = _uiState.value.stance
        )
    }
}
