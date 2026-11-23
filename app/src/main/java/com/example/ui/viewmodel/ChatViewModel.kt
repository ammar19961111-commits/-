package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.AdhkarChatService
import com.example.ai.ChatHistoryTurn
import com.example.ai.FirebaseAdhkarChatService
import com.example.data.local.ChatDao
import com.example.data.model.ChatMessage
import com.example.ui.theme.AppLanguage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "ChatViewModel"
private const val CHAT_TIMEOUT_MS = 60_000L

sealed interface ChatGenerationState {
    data object Idle : ChatGenerationState
    data object Loading : ChatGenerationState
    data class Streaming(val partialText: String) : ChatGenerationState
    data class Success(val text: String) : ChatGenerationState
    data class Error(val message: String, val isTimeout: Boolean = false) : ChatGenerationState
}

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val generationState: ChatGenerationState = ChatGenerationState.Idle,
    val lastFailedPrompt: String? = null
) {
    val isSending: Boolean
        get() = generationState is ChatGenerationState.Loading ||
            generationState is ChatGenerationState.Streaming

    val streamingText: String
        get() = (generationState as? ChatGenerationState.Streaming)?.partialText.orEmpty()
}

class ChatViewModel(
    private val chatService: AdhkarChatService = FirebaseAdhkarChatService(),
    private val chatDao: ChatDao? = null,
    private val timeoutMillis: Long = CHAT_TIMEOUT_MS
) : ViewModel() {

    private val inputTextFlow = MutableStateFlow("")
    private val generationStateFlow = MutableStateFlow<ChatGenerationState>(ChatGenerationState.Idle)
    private val lastFailedPromptFlow = MutableStateFlow<String?>(null)
    private val inMemoryMessagesFlow = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val messagesSourceFlow = chatDao?.observeMessages() ?: inMemoryMessagesFlow

    private var currentSendJob: Job? = null
    private var nextInMemoryId = 1L

    val uiState: StateFlow<ChatUiState> = combine(
        messagesSourceFlow,
        inputTextFlow,
        generationStateFlow,
        lastFailedPromptFlow
    ) { messages, input, genState, failedPrompt ->
        ChatUiState(
            messages = messages,
            inputText = input,
            generationState = genState,
            lastFailedPrompt = failedPrompt
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = ChatUiState()
    )

    fun onInputChange(newValue: String) {
        inputTextFlow.value = newValue
    }

    fun sendMessage(
        appLanguage: AppLanguage,
        overridePrompt: String? = null
    ) {
        val rawPrompt = (overridePrompt ?: inputTextFlow.value).trim()
        if (rawPrompt.isEmpty() || uiState.value.isSending) return

        if (overridePrompt == null) {
            inputTextFlow.value = ""
        }
        lastFailedPromptFlow.value = null

        currentSendJob?.cancel()
        currentSendJob = viewModelScope.launch {
            val historySnapshot = uiState.value.messages.map {
                ChatHistoryTurn(isUser = it.isUser, text = it.content)
            }
            val alreadyUserTail = historySnapshot.lastOrNull()?.let {
                it.isUser && it.text == rawPrompt
            } == true

            val historyForModel = if (alreadyUserTail) {
                historySnapshot.dropLast(1)
            } else {
                appendMessage(isUser = true, text = rawPrompt)
                historySnapshot
            }

            generationStateFlow.value = ChatGenerationState.Loading
            val accumulated = StringBuilder()

            try {
                withTimeout(timeoutMillis) {
                    chatService.streamReply(
                        history = historyForModel,
                        userMessage = rawPrompt,
                        appLanguage = appLanguage
                    ).collect { chunk ->
                        if (chunk.isNotEmpty()) {
                            accumulated.append(chunk)
                            generationStateFlow.value =
                                ChatGenerationState.Streaming(accumulated.toString())
                        }
                    }
                }
                val finalReply = accumulated.toString().trim()
                if (finalReply.isNotEmpty()) {
                    appendMessage(isUser = false, text = finalReply)
                    generationStateFlow.value = ChatGenerationState.Success(finalReply)
                } else {
                    lastFailedPromptFlow.value = rawPrompt
                    generationStateFlow.value = ChatGenerationState.Error(
                        message = "",
                        isTimeout = false
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Chat streaming timed out after ${timeoutMillis}ms", e)
                val partial = accumulated.toString().trim()
                if (partial.isNotEmpty()) {
                    appendMessage(isUser = false, text = partial)
                }
                lastFailedPromptFlow.value = rawPrompt
                generationStateFlow.value = ChatGenerationState.Error(
                    message = e.localizedMessage.orEmpty(),
                    isTimeout = true
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI chat generation failed", e)
                val partial = accumulated.toString().trim()
                if (partial.isNotEmpty()) {
                    appendMessage(isUser = false, text = partial)
                }
                lastFailedPromptFlow.value = rawPrompt
                generationStateFlow.value = ChatGenerationState.Error(
                    message = e.localizedMessage ?: e.javaClass.simpleName,
                    isTimeout = false
                )
            }
        }
    }

    fun retryLastFailed(appLanguage: AppLanguage) {
        val prompt = lastFailedPromptFlow.value ?: return
        sendMessage(appLanguage = appLanguage, overridePrompt = prompt)
    }

    fun dismissError() {
        if (generationStateFlow.value is ChatGenerationState.Error) {
            generationStateFlow.value = ChatGenerationState.Idle
        }
    }

    fun clearConversation() {
        currentSendJob?.cancel()
        currentSendJob = null
        generationStateFlow.value = ChatGenerationState.Idle
        lastFailedPromptFlow.value = null
        viewModelScope.launch {
            if (chatDao != null) {
                chatDao.clearAll()
            } else {
                inMemoryMessagesFlow.value = emptyList()
            }
        }
    }

    private suspend fun appendMessage(isUser: Boolean, text: String) {
        val msg = ChatMessage(
            id = if (chatDao == null) nextInMemoryId++ else 0L,
            isUser = isUser,
            content = text,
            timestampMillis = System.currentTimeMillis()
        )
        if (chatDao != null) {
            chatDao.insertMessage(msg)
        } else {
            inMemoryMessagesFlow.value = inMemoryMessagesFlow.value + msg
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentSendJob?.cancel()
    }

    companion object {
        fun provideFactory(
            chatService: AdhkarChatService = FirebaseAdhkarChatService(),
            chatDao: ChatDao? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(
                    chatService = chatService,
                    chatDao = chatDao
                ) as T
            }
        }
    }
}
