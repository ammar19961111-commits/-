package com.example.ai

import com.example.ui.theme.AppLanguage
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

data class ChatHistoryTurn(
    val isUser: Boolean,
    val text: String
)

interface AdhkarChatService {
    fun streamReply(
        history: List<ChatHistoryTurn>,
        userMessage: String,
        appLanguage: AppLanguage
    ): Flow<String>
}

class FirebaseAdhkarChatService : AdhkarChatService {
    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(SYSTEM_INSTRUCTION)
            }
        )
    }

    override fun streamReply(
        history: List<ChatHistoryTurn>,
        userMessage: String,
        appLanguage: AppLanguage
    ): Flow<String> {
        val sdkHistory = history
            .filter { it.text.isNotBlank() }
            .map { turn ->
                content(role = if (turn.isUser) "user" else "model") {
                    text(turn.text)
                }
            }
        val chat = model.startChat(history = sdkHistory)
        val prompt = if (sdkHistory.isEmpty()) {
            val langHint = when (appLanguage) {
                AppLanguage.ARABIC -> "[App language preference: Arabic — reply in Arabic unless the user wrote in English.]"
                AppLanguage.ENGLISH -> "[App language preference: English — reply in English unless the user wrote in Arabic.]"
            }
            "$langHint\n\n$userMessage"
        } else {
            userMessage
        }
        return chat.sendMessageStream(prompt).mapNotNull { chunk -> chunk.text }
    }

    companion object {
        private const val SYSTEM_INSTRUCTION =
            "You are a serene, knowledgeable Islamic Adhkar and Spiritual Companion inside an Adhkar & Tasbeeh mobile app. " +
                "Help users with authentic Morning & Evening Adhkar, Quranic and Prophetic Duas (Hisn al-Muslim), virtues of Dhikr and Tasbeeh, meanings and brief reflections on remembrances, and gentle practical tips for consistency in daily worship. " +
                "Always reply in the same language as the user's message (clear, eloquent Arabic when addressed in Arabic, or clear English when addressed in English). " +
                "Keep responses concise, structured, accurate, and comforting. Include Arabic text with diacritics when citing a short Dhikr or Dua, along with a brief explanation or source note. " +
                "If a user asks for complex juristic rulings (fatwa), advise consulting a qualified local scholar while sharing general established virtues if appropriate."
    }
}
