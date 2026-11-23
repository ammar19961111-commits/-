package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppThemeStyle
import com.example.data.model.DhikrCategory
import com.example.data.model.DhikrReminder
import com.example.data.model.DhikrReminderDefaults
import com.example.data.model.PrayerCalculationMethod
import com.example.data.model.RosaryBeadDesign
import com.example.data.model.TasbihTargetSoundEffect
import com.example.data.model.TasbihVibrationPattern
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "adhkar_prefs")

data class UserPreferences(
    val fontSizeSp: Float = 22f,
    val hapticEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val autoAdvance: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val nightModeEnabled: Boolean = false,
    val lastActiveDate: String = "",
    val totalCompletedAllTime: Int = 0,
    val dailyTasbihTarget: Int = 100,
    val dailyTotalGoal: Int = 150,
    val reminders: List<DhikrReminder> = DhikrReminderDefaults.defaultReminders(),
    val prayerMethodId: Int = PrayerCalculationMethod.UMM_AL_QURA.apiMethodId,
    val prayerLatitude: Double? = null,
    val prayerLongitude: Double? = null,
    val prayerLocationLabel: String = "",
    val appLanguage: AppLanguage = AppLanguage.ARABIC,
    val adFreeUntilMillis: Long = 0L,
    val selectedTasbihId: Int? = null,
    val currentTasbihCount: Int = 0,
    val savedTasbihCounts: Map<Int, Int> = emptyMap(),
    val focusModeEnabled: Boolean = false,
    val autoCountTarget: Int = 33,
    val autoCountPace: String = "MEDIUM",
    val tasbihTargetVibrationPattern: TasbihVibrationPattern = TasbihVibrationPattern.GENTLE_PULSE,
    val tasbihTargetSoundEffect: TasbihTargetSoundEffect = TasbihTargetSoundEffect.GENTLE_CHIME,
    val appThemeStyle: AppThemeStyle = AppThemeStyle.ANDALUSIAN_EMERALD,
    val unlockedThemeStyles: Set<String> = setOf(AppThemeStyle.ANDALUSIAN_EMERALD.name),
    val rosaryBeadDesign: RosaryBeadDesign = RosaryBeadDesign.CLASSIC_RING,
    val unlockedBeadDesigns: Set<String> = setOf(RosaryBeadDesign.CLASSIC_RING.name)
) {
    fun isThemeUnlocked(style: AppThemeStyle): Boolean {
        return !style.isExclusive || style.name in unlockedThemeStyles
    }

    fun isBeadDesignUnlocked(design: RosaryBeadDesign): Boolean {
        return !design.isExclusive || design.name in unlockedBeadDesigns
    }
}

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val FONT_SIZE_SP = floatPreferencesKey("font_size_sp")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val AUTO_ADVANCE = booleanPreferencesKey("auto_advance")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NIGHT_MODE_ENABLED = booleanPreferencesKey("night_mode_enabled")
        val LAST_ACTIVE_DATE = stringPreferencesKey("last_active_date")
        val TOTAL_COMPLETED = intPreferencesKey("total_completed_all_time")
        val DAILY_TASBIH_TARGET = intPreferencesKey("daily_tasbih_target")
        val DAILY_TOTAL_GOAL = intPreferencesKey("daily_total_goal")
        val PRAYER_METHOD_ID = intPreferencesKey("prayer_method_id")
        val PRAYER_LATITUDE = doublePreferencesKey("prayer_latitude")
        val PRAYER_LONGITUDE = doublePreferencesKey("prayer_longitude")
        val PRAYER_LOCATION_LABEL = stringPreferencesKey("prayer_location_label")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val AD_FREE_UNTIL_MILLIS = longPreferencesKey("ad_free_until_millis")
        val SELECTED_TASBIH_ID = intPreferencesKey("selected_tasbih_id")
        val CURRENT_TASBIH_COUNT = intPreferencesKey("current_tasbih_count")
        val FOCUS_MODE_ENABLED = booleanPreferencesKey("focus_mode_enabled")
        val AUTO_COUNT_TARGET = intPreferencesKey("auto_count_target")
        val AUTO_COUNT_PACE = stringPreferencesKey("auto_count_pace")
        val TASBIH_TARGET_VIBRATION_PATTERN = stringPreferencesKey("tasbih_target_vibration_pattern")
        val TASBIH_TARGET_SOUND_EFFECT = stringPreferencesKey("tasbih_target_sound_effect")
        val APP_THEME_STYLE = stringPreferencesKey("app_theme_style")
        val UNLOCKED_THEME_STYLES = stringSetPreferencesKey("unlocked_theme_styles")
        val ROSARY_BEAD_DESIGN = stringPreferencesKey("rosary_bead_design")
        val UNLOCKED_BEAD_DESIGNS = stringSetPreferencesKey("unlocked_bead_designs")
        const val TASBIH_COUNT_PREFIX = "tasbih_count_"

        fun tasbihCount(id: Int) = intPreferencesKey("$TASBIH_COUNT_PREFIX$id")

        fun reminderEnabled(category: DhikrCategory) =
            booleanPreferencesKey("reminder_enabled_${category.id}")

        fun reminderHour(category: DhikrCategory) =
            intPreferencesKey("reminder_hour_${category.id}")

        fun reminderMinute(category: DhikrCategory) =
            intPreferencesKey("reminder_minute_${category.id}")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val themeStr = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val themeMode = runCatching { ThemeMode.valueOf(themeStr) }.getOrDefault(ThemeMode.SYSTEM)
        val reminders = DhikrCategory.entries.map { category ->
            val (defaultHour, defaultMinute) = DhikrReminderDefaults.defaultTimeFor(category)
            DhikrReminder(
                category = category,
                enabled = prefs[Keys.reminderEnabled(category)] ?: false,
                hour = (prefs[Keys.reminderHour(category)] ?: defaultHour).coerceIn(0, 23),
                minute = (prefs[Keys.reminderMinute(category)] ?: defaultMinute).coerceIn(0, 59)
            )
        }
        val savedTasbihCounts = prefs.asMap().entries.mapNotNull { (key, value) ->
            val id = key.name.removePrefix(Keys.TASBIH_COUNT_PREFIX)
                .takeIf { key.name.startsWith(Keys.TASBIH_COUNT_PREFIX) }
                ?.toIntOrNull()
            val count = (value as? Int)?.coerceAtLeast(0)
            if (id != null && count != null) id to count else null
        }.toMap()
        UserPreferences(
            fontSizeSp = (prefs[Keys.FONT_SIZE_SP] ?: 22f).coerceIn(16f, 36f),
            hapticEnabled = prefs[Keys.HAPTIC_ENABLED] ?: true,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            autoAdvance = prefs[Keys.AUTO_ADVANCE] ?: true,
            themeMode = themeMode,
            nightModeEnabled = prefs[Keys.NIGHT_MODE_ENABLED] ?: false,
            lastActiveDate = prefs[Keys.LAST_ACTIVE_DATE] ?: "",
            totalCompletedAllTime = prefs[Keys.TOTAL_COMPLETED] ?: 0,
            dailyTasbihTarget = (prefs[Keys.DAILY_TASBIH_TARGET] ?: 100).coerceIn(10, 10000),
            dailyTotalGoal = (prefs[Keys.DAILY_TOTAL_GOAL] ?: 150).coerceIn(10, 10000),
            reminders = reminders,
            prayerMethodId = prefs[Keys.PRAYER_METHOD_ID] ?: PrayerCalculationMethod.UMM_AL_QURA.apiMethodId,
            prayerLatitude = prefs[Keys.PRAYER_LATITUDE],
            prayerLongitude = prefs[Keys.PRAYER_LONGITUDE],
            prayerLocationLabel = prefs[Keys.PRAYER_LOCATION_LABEL] ?: "",
            appLanguage = AppLanguage.fromCode(prefs[Keys.APP_LANGUAGE]),
            adFreeUntilMillis = prefs[Keys.AD_FREE_UNTIL_MILLIS] ?: 0L,
            selectedTasbihId = prefs[Keys.SELECTED_TASBIH_ID],
            currentTasbihCount = (prefs[Keys.CURRENT_TASBIH_COUNT] ?: 0).coerceAtLeast(0),
            savedTasbihCounts = savedTasbihCounts,
            focusModeEnabled = prefs[Keys.FOCUS_MODE_ENABLED] ?: false,
            autoCountTarget = (prefs[Keys.AUTO_COUNT_TARGET] ?: 33).coerceIn(1, 10000),
            autoCountPace = prefs[Keys.AUTO_COUNT_PACE] ?: "MEDIUM",
            tasbihTargetVibrationPattern = TasbihVibrationPattern.fromName(prefs[Keys.TASBIH_TARGET_VIBRATION_PATTERN]),
            tasbihTargetSoundEffect = TasbihTargetSoundEffect.fromName(prefs[Keys.TASBIH_TARGET_SOUND_EFFECT]),
            appThemeStyle = AppThemeStyle.fromName(prefs[Keys.APP_THEME_STYLE]),
            unlockedThemeStyles = (prefs[Keys.UNLOCKED_THEME_STYLES] ?: emptySet()) + AppThemeStyle.ANDALUSIAN_EMERALD.name,
            rosaryBeadDesign = RosaryBeadDesign.fromName(prefs[Keys.ROSARY_BEAD_DESIGN]),
            unlockedBeadDesigns = (prefs[Keys.UNLOCKED_BEAD_DESIGNS] ?: emptySet()) + RosaryBeadDesign.CLASSIC_RING.name
        )
    }

    suspend fun setFontSize(sizeSp: Float) {
        context.dataStore.edit { it[Keys.FONT_SIZE_SP] = sizeSp.coerceIn(16f, 36f) }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTIC_ENABLED] = enabled }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setAutoAdvance(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_ADVANCE] = enabled }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setNightModeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NIGHT_MODE_ENABLED] = enabled }
    }

    suspend fun updateLastActiveDate(date: String) {
        context.dataStore.edit { it[Keys.LAST_ACTIVE_DATE] = date }
    }

    suspend fun incrementTotalCompleted() {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.TOTAL_COMPLETED] ?: 0
            prefs[Keys.TOTAL_COMPLETED] = current + 1
        }
    }

    suspend fun setDailyTasbihTarget(target: Int) {
        context.dataStore.edit { it[Keys.DAILY_TASBIH_TARGET] = target.coerceIn(10, 10000) }
    }

    suspend fun setDailyTotalGoal(target: Int) {
        context.dataStore.edit { it[Keys.DAILY_TOTAL_GOAL] = target.coerceIn(10, 10000) }
    }

    suspend fun setReminder(
        category: DhikrCategory,
        enabled: Boolean,
        hour: Int,
        minute: Int
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.reminderEnabled(category)] = enabled
            prefs[Keys.reminderHour(category)] = hour.coerceIn(0, 23)
            prefs[Keys.reminderMinute(category)] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setPrayerCalculationMethod(methodId: Int) {
        context.dataStore.edit { it[Keys.PRAYER_METHOD_ID] = methodId }
    }

    suspend fun setPrayerLocation(latitude: Double, longitude: Double, label: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PRAYER_LATITUDE] = latitude
            prefs[Keys.PRAYER_LONGITUDE] = longitude
            prefs[Keys.PRAYER_LOCATION_LABEL] = label
        }
    }

    suspend fun restorePreferences(restored: UserPreferences) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FONT_SIZE_SP] = restored.fontSizeSp.coerceIn(16f, 36f)
            prefs[Keys.HAPTIC_ENABLED] = restored.hapticEnabled
            prefs[Keys.SOUND_ENABLED] = restored.soundEnabled
            prefs[Keys.AUTO_ADVANCE] = restored.autoAdvance
            prefs[Keys.THEME_MODE] = restored.themeMode.name
            prefs[Keys.NIGHT_MODE_ENABLED] = restored.nightModeEnabled
            prefs[Keys.LAST_ACTIVE_DATE] = restored.lastActiveDate
            prefs[Keys.TOTAL_COMPLETED] = restored.totalCompletedAllTime.coerceAtLeast(0)
            prefs[Keys.DAILY_TASBIH_TARGET] = restored.dailyTasbihTarget.coerceIn(10, 10000)
            prefs[Keys.DAILY_TOTAL_GOAL] = restored.dailyTotalGoal.coerceIn(10, 10000)
            prefs[Keys.PRAYER_METHOD_ID] = restored.prayerMethodId
            if (restored.prayerLatitude != null && restored.prayerLongitude != null) {
                prefs[Keys.PRAYER_LATITUDE] = restored.prayerLatitude
                prefs[Keys.PRAYER_LONGITUDE] = restored.prayerLongitude
            } else {
                prefs.remove(Keys.PRAYER_LATITUDE)
                prefs.remove(Keys.PRAYER_LONGITUDE)
            }
            prefs[Keys.PRAYER_LOCATION_LABEL] = restored.prayerLocationLabel
            prefs[Keys.APP_LANGUAGE] = restored.appLanguage.name
            prefs[Keys.AD_FREE_UNTIL_MILLIS] = restored.adFreeUntilMillis.coerceAtLeast(0L)
            prefs[Keys.FOCUS_MODE_ENABLED] = restored.focusModeEnabled
            prefs[Keys.AUTO_COUNT_TARGET] = restored.autoCountTarget.coerceIn(1, 10000)
            prefs[Keys.AUTO_COUNT_PACE] = restored.autoCountPace
            prefs[Keys.TASBIH_TARGET_VIBRATION_PATTERN] = restored.tasbihTargetVibrationPattern.name
            prefs[Keys.TASBIH_TARGET_SOUND_EFFECT] = restored.tasbihTargetSoundEffect.name
            prefs[Keys.APP_THEME_STYLE] = restored.appThemeStyle.name
            prefs[Keys.UNLOCKED_THEME_STYLES] = restored.unlockedThemeStyles + AppThemeStyle.ANDALUSIAN_EMERALD.name
            prefs[Keys.ROSARY_BEAD_DESIGN] = restored.rosaryBeadDesign.name
            prefs[Keys.UNLOCKED_BEAD_DESIGNS] = restored.unlockedBeadDesigns + RosaryBeadDesign.CLASSIC_RING.name
            restored.reminders.forEach { reminder ->
                prefs[Keys.reminderEnabled(reminder.category)] = reminder.enabled
                prefs[Keys.reminderHour(reminder.category)] = reminder.hour.coerceIn(0, 23)
                prefs[Keys.reminderMinute(reminder.category)] = reminder.minute.coerceIn(0, 59)
            }
        }
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.APP_LANGUAGE] = language.name }
    }

    suspend fun setAdFreeUntil(untilMillis: Long) {
        context.dataStore.edit { it[Keys.AD_FREE_UNTIL_MILLIS] = untilMillis.coerceAtLeast(0L) }
    }

    suspend fun setSelectedTasbihId(id: Int?) {
        context.dataStore.edit { prefs ->
            if (id != null) {
                prefs[Keys.SELECTED_TASBIH_ID] = id
            } else {
                prefs.remove(Keys.SELECTED_TASBIH_ID)
            }
        }
    }

    suspend fun saveTasbihCount(tasbihId: Int, count: Int) {
        val safeCount = count.coerceAtLeast(0)
        context.dataStore.edit { prefs ->
            prefs[Keys.SELECTED_TASBIH_ID] = tasbihId
            prefs[Keys.CURRENT_TASBIH_COUNT] = safeCount
            prefs[Keys.tasbihCount(tasbihId)] = safeCount
        }
    }

    suspend fun setSelectedTasbihAndSyncCount(id: Int?, count: Int) {
        val safeCount = count.coerceAtLeast(0)
        context.dataStore.edit { prefs ->
            if (id != null) {
                prefs[Keys.SELECTED_TASBIH_ID] = id
                prefs[Keys.tasbihCount(id)] = safeCount
            } else {
                prefs.remove(Keys.SELECTED_TASBIH_ID)
            }
            prefs[Keys.CURRENT_TASBIH_COUNT] = safeCount
        }
    }

    suspend fun setCurrentTasbihCount(count: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CURRENT_TASBIH_COUNT] = count.coerceAtLeast(0)
        }
    }

    suspend fun clearTasbihCount(tasbihId: Int) {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.tasbihCount(tasbihId))
            if (prefs[Keys.SELECTED_TASBIH_ID] == tasbihId) {
                prefs.remove(Keys.SELECTED_TASBIH_ID)
                prefs[Keys.CURRENT_TASBIH_COUNT] = 0
            }
        }
    }

    suspend fun setFocusModeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FOCUS_MODE_ENABLED] = enabled }
    }

    suspend fun setAutoCountTarget(target: Int) {
        context.dataStore.edit { it[Keys.AUTO_COUNT_TARGET] = target.coerceIn(1, 10000) }
    }

    suspend fun setAutoCountPace(pace: String) {
        context.dataStore.edit { it[Keys.AUTO_COUNT_PACE] = pace }
    }

    suspend fun setTasbihTargetVibrationPattern(pattern: TasbihVibrationPattern) {
        context.dataStore.edit { it[Keys.TASBIH_TARGET_VIBRATION_PATTERN] = pattern.name }
    }

    suspend fun setTasbihTargetSoundEffect(effect: TasbihTargetSoundEffect) {
        context.dataStore.edit { it[Keys.TASBIH_TARGET_SOUND_EFFECT] = effect.name }
    }

    suspend fun setThemeStyle(style: AppThemeStyle) {
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_THEME_STYLE] = style.name
        }
    }

    suspend fun unlockAndSelectThemeStyle(style: AppThemeStyle) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.UNLOCKED_THEME_STYLES] ?: setOf(AppThemeStyle.ANDALUSIAN_EMERALD.name)
            prefs[Keys.UNLOCKED_THEME_STYLES] = current + AppThemeStyle.ANDALUSIAN_EMERALD.name + style.name
            prefs[Keys.APP_THEME_STYLE] = style.name
        }
    }

    suspend fun setRosaryBeadDesign(design: RosaryBeadDesign) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ROSARY_BEAD_DESIGN] = design.name
        }
    }

    suspend fun unlockAndSelectBeadDesign(design: RosaryBeadDesign) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.UNLOCKED_BEAD_DESIGNS] ?: setOf(RosaryBeadDesign.CLASSIC_RING.name)
            prefs[Keys.UNLOCKED_BEAD_DESIGNS] = current + RosaryBeadDesign.CLASSIC_RING.name + design.name
            prefs[Keys.ROSARY_BEAD_DESIGN] = design.name
        }
    }

    suspend fun clearForTesting() {
        context.dataStore.edit { it.clear() }
    }
}
