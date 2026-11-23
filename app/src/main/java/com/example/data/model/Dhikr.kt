package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dhikr",
    indices = [Index(value = ["category"])]
)
data class Dhikr(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val category: String,
    val virtue: String = "",
    val source: String = "",
    val targetCount: Int = 1,
    val currentCount: Int = 0,
    val isFavorite: Boolean = false,
    val sortOrder: Int = 0
) {
    val isCompleted: Boolean
        get() = currentCount >= targetCount
}
