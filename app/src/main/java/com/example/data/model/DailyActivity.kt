package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_activity")
data class DailyActivity(
    @PrimaryKey val dateKey: String,
    val dhikrCount: Int = 0,
    val tasbihCount: Int = 0,
    val sessionsCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val totalCount: Int
        get() = dhikrCount + tasbihCount
}
