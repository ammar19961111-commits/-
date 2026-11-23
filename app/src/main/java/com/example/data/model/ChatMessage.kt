package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val isUser: Boolean,
    val content: String,
    val timestampMillis: Long = System.currentTimeMillis()
)
