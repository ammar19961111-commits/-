package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AdhkarBackupData(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0",
    val preferences: BackupPreferences = BackupPreferences(),
    val dhikrProgress: List<BackupDhikrProgress> = emptyList(),
    val counters: List<BackupCounter> = emptyList(),
    val dailyHistory: List<BackupDailyActivity> = emptyList()
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

@JsonClass(generateAdapter = true)
data class BackupPreferences(
    val fontSizeSp: Float = 22f,
    val hapticEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val autoAdvance: Boolean = true,
    val themeMode: String = "SYSTEM",
    val nightModeEnabled: Boolean = false,
    val lastActiveDate: String = "",
    val totalCompletedAllTime: Int = 0,
    val dailyTasbihTarget: Int = 100,
    val dailyTotalGoal: Int = 150,
    val prayerMethodId: Int = PrayerCalculationMethod.UMM_AL_QURA.apiMethodId,
    val prayerLatitude: Double? = null,
    val prayerLongitude: Double? = null,
    val prayerLocationLabel: String = "",
    val appLanguage: String = "ARABIC",
    val adFreeUntilMillis: Long = 0L,
    val appThemeStyle: String = AppThemeStyle.ANDALUSIAN_EMERALD.name,
    val unlockedThemeStyles: List<String> = listOf(AppThemeStyle.ANDALUSIAN_EMERALD.name),
    val rosaryBeadDesign: String = RosaryBeadDesign.CLASSIC_RING.name,
    val unlockedBeadDesigns: List<String> = listOf(RosaryBeadDesign.CLASSIC_RING.name),
    val reminders: List<BackupReminder> = emptyList()
)

@JsonClass(generateAdapter = true)
data class BackupReminder(
    val categoryId: String,
    val enabled: Boolean,
    val hour: Int,
    val minute: Int
)

@JsonClass(generateAdapter = true)
data class BackupDhikrProgress(
    val id: Int,
    val currentCount: Int,
    val isFavorite: Boolean
)

@JsonClass(generateAdapter = true)
data class BackupCounter(
    val id: Int = 0,
    val phrase: String,
    val virtue: String = "",
    val targetCount: Int = 33,
    val currentCount: Int = 0,
    val todayCount: Int = 0,
    val totalCount: Int = 0,
    val sortOrder: Int = 0,
    val lastUpdatedAt: Long = 0L
)

@JsonClass(generateAdapter = true)
data class BackupDailyActivity(
    val dateKey: String,
    val dhikrCount: Int,
    val tasbihCount: Int,
    val sessionsCount: Int = 0,
    val updatedAt: Long = 0L
)
