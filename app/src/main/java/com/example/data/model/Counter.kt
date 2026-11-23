package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "counter")
data class Counter(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phrase: String,
    val virtue: String = "",
    val targetCount: Int = 33,
    val currentCount: Int = 0,
    val todayCount: Int = 0,
    val totalCount: Int = 0,
    val sortOrder: Int = 0,
    val lastUpdatedAt: Long = System.currentTimeMillis()
)
