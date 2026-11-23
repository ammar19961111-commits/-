package com.example.data.repository

import com.example.data.local.AdhkarSeedData
import com.example.data.local.CounterDao
import com.example.data.local.DailyActivityDao
import com.example.data.local.DhikrDao
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.AdhkarBackupData
import com.example.data.model.AppThemeStyle
import com.example.data.model.BackupCounter
import com.example.data.model.BackupDailyActivity
import com.example.data.model.BackupDhikrProgress
import com.example.data.model.BackupPreferences
import com.example.data.model.BackupReminder
import com.example.data.model.Counter
import com.example.data.model.DailyActivity
import com.example.data.model.Dhikr
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdhkarRepository(
    private val dhikrDao: DhikrDao,
    private val counterDao: CounterDao,
    private val prefsRepo: UserPreferencesRepository,
    private val dailyActivityDao: DailyActivityDao? = null
) {
    val allAdhkar: Flow<List<Dhikr>> = dhikrDao.observeAll()
    val morningAdhkar: Flow<List<Dhikr>> = dhikrDao.observeByCategory(DhikrCategory.MORNING.id)
    val eveningAdhkar: Flow<List<Dhikr>> = dhikrDao.observeByCategory(DhikrCategory.EVENING.id)
    val dailyMorningAndEveningAdhkar: Flow<List<Dhikr>> = dhikrDao.observeMorningAndEvening()
    val favoriteAdhkar: Flow<List<Dhikr>> = dhikrDao.observeFavorites()
    val counters: Flow<List<Counter>> = counterDao.observeAll()
    val userPreferences: Flow<UserPreferences> = prefsRepo.preferencesFlow
    val dailyHistory: Flow<List<DailyActivity>> = dailyActivityDao?.observeAll() ?: flowOf(emptyList())

    fun observeCategory(category: String): Flow<List<Dhikr>> =
        dhikrDao.observeByCategory(category)

    fun observeDailyMorningAdhkar(): Flow<List<Dhikr>> = morningAdhkar

    fun observeDailyEveningAdhkar(): Flow<List<Dhikr>> = eveningAdhkar

    suspend fun getMorningAdhkar(): List<Dhikr> =
        dhikrDao.getByCategory(DhikrCategory.MORNING.id)

    suspend fun getEveningAdhkar(): List<Dhikr> =
        dhikrDao.getByCategory(DhikrCategory.EVENING.id)

    suspend fun getDhikrById(id: Int): Dhikr? = dhikrDao.getById(id)

    suspend fun storeDhikr(dhikr: Dhikr): Long = dhikrDao.insert(dhikr)

    suspend fun storeAdhkar(items: List<Dhikr>) = dhikrDao.upsertAll(items)

    suspend fun updateDhikr(dhikr: Dhikr) = dhikrDao.update(dhikr)

    suspend fun deleteDhikr(dhikr: Dhikr) = dhikrDao.delete(dhikr)

    suspend fun ensureInitialized() {
        if (dhikrDao.getCount() == 0) {
            dhikrDao.insertAll(AdhkarSeedData.getInitialAdhkar())
        }
        if (counterDao.getCount() == 0) {
            counterDao.insertAll(AdhkarSeedData.getInitialCounters())
        }
        val today = currentDayKey()
        val prefs = prefsRepo.preferencesFlow.first()
        val rolledOverDay = prefs.lastActiveDate.isNotEmpty() && prefs.lastActiveDate != today
        if (rolledOverDay) {
            // Preserve previous day's totals in history before resetting daily progress
            if (dailyActivityDao != null && dailyActivityDao.getByDate(prefs.lastActiveDate) == null) {
                val prevDhikrSum = dhikrDao.observeAll().first().sumOf { it.currentCount }
                val prevTasbihSum = counterDao.observeAll().first().sumOf { it.todayCount }
                if (prevDhikrSum > 0 || prevTasbihSum > 0) {
                    dailyActivityDao.upsert(
                        DailyActivity(
                            dateKey = prefs.lastActiveDate,
                            dhikrCount = prevDhikrSum,
                            tasbihCount = prevTasbihSum
                        )
                    )
                }
            }
            dhikrDao.resetAllProgress()
            counterDao.resetDailyCounts()
        } else if (dailyActivityDao != null && dailyActivityDao.getByDate(today) == null) {
            // Backfill today's activity if upgrading from an earlier DB version
            val todayDhikrSum = dhikrDao.observeAll().first().sumOf { it.currentCount }
            val todayTasbihSum = counterDao.observeAll().first().sumOf { it.todayCount }
            if (todayDhikrSum > 0 || todayTasbihSum > 0) {
                dailyActivityDao.upsert(
                    DailyActivity(
                        dateKey = today,
                        dhikrCount = todayDhikrSum,
                        tasbihCount = todayTasbihSum
                    )
                )
            }
        }
        if (prefs.lastActiveDate != today) {
            prefsRepo.updateLastActiveDate(today)
        }
        // Reconcile persisted DataStore Tasbeeh count when Room was freshly initialized or restarted
        val dbCounters = counterDao.observeAll().first()
        dbCounters.forEach { counter ->
            val savedForId = prefs.savedTasbihCounts[counter.id]
            val fallbackActive = if (
                savedForId == null &&
                prefs.selectedTasbihId == counter.id &&
                prefs.currentTasbihCount > 0
            ) prefs.currentTasbihCount else null
            val restoredCount = savedForId ?: fallbackActive
            if (restoredCount != null && counter.currentCount == 0 && restoredCount > 0) {
                counterDao.updateProgress(
                    id = counter.id,
                    currentCount = restoredCount,
                    todayCount = if (rolledOverDay) 0 else maxOf(counter.todayCount, restoredCount),
                    totalCount = maxOf(counter.totalCount, restoredCount)
                )
            }
        }
        val refreshedCounters = counterDao.observeAll().first()
        val activeCounter = prefs.selectedTasbihId?.let { id -> refreshedCounters.find { it.id == id } }
            ?: refreshedCounters.firstOrNull()
        if (activeCounter != null && activeCounter.currentCount > 0 && prefs.currentTasbihCount == 0) {
            prefsRepo.saveTasbihCount(activeCounter.id, activeCounter.currentCount)
        }
    }

    suspend fun incrementDhikr(dhikr: Dhikr): Boolean {
        if (dhikr.currentCount >= dhikr.targetCount) return false
        val nextCount = dhikr.currentCount + 1
        dhikrDao.updateProgress(dhikr.id, nextCount)
        val todayKey = currentDayKey()
        dailyActivityDao?.recordDhikrIncrement(todayKey)
        val justFinished = nextCount == dhikr.targetCount
        if (justFinished) {
            prefsRepo.incrementTotalCompleted()
            val categoryItems = dhikrDao.observeByCategory(dhikr.category).first()
            if (categoryItems.isNotEmpty() && categoryItems.all { it.isCompleted }) {
                dailyActivityDao?.recordSessionCompleted(todayKey)
            }
        }
        return justFinished
    }

    suspend fun resetDhikr(id: Int) {
        dhikrDao.updateProgress(id, 0)
    }

    suspend fun resetCategory(category: String) {
        dhikrDao.resetCategory(category)
    }

    suspend fun resetAllToday() {
        dhikrDao.resetAllProgress()
        counterDao.resetDailyCounts()
        dailyActivityDao?.resetDate(currentDayKey())
    }

    suspend fun toggleFavorite(dhikr: Dhikr) {
        dhikrDao.updateFavorite(dhikr.id, !dhikr.isFavorite)
    }

    suspend fun incrementCounter(counter: Counter): Boolean {
        counterDao.incrementAtomically(counter.id)
        val todayKey = currentDayKey()
        dailyActivityDao?.recordTasbihIncrement(todayKey)
        val updated = counterDao.getById(counter.id) ?: return false
        prefsRepo.saveTasbihCount(counter.id, updated.currentCount)
        val hitCycleTarget = updated.targetCount > 0 && updated.currentCount == updated.targetCount
        if (hitCycleTarget) {
            dailyActivityDao?.recordSessionCompleted(todayKey)
        }
        return hitCycleTarget
    }

    suspend fun recordCompletedSession(delta: Int = 1, dateKey: String = currentDayKey()) {
        dailyActivityDao?.recordSessionCompleted(dateKey = dateKey, delta = delta)
    }

    suspend fun resetCounterCycle(id: Int) {
        counterDao.resetCycle(id)
        prefsRepo.saveTasbihCount(id, 0)
    }

    suspend fun updateCounterTarget(id: Int, targetCount: Int) {
        counterDao.updateTargetAndReset(id, targetCount)
        prefsRepo.saveTasbihCount(id, 0)
    }

    suspend fun addCustomCounter(phrase: String, targetCount: Int): Long {
        val cleanPhrase = phrase.trim()
        if (cleanPhrase.isEmpty()) return -1L
        return counterDao.insert(
            Counter(
                phrase = cleanPhrase,
                virtue = "ذكر مخصص",
                targetCount = targetCount,
                sortOrder = 99
            )
        )
    }

    suspend fun deleteCounter(id: Int) {
        counterDao.deleteById(id)
        prefsRepo.clearTasbihCount(id)
    }

    suspend fun saveDailyActivity(activity: DailyActivity) {
        dailyActivityDao?.upsert(activity)
    }

    suspend fun setFontSize(sizeSp: Float) = prefsRepo.setFontSize(sizeSp)
    suspend fun setHapticEnabled(enabled: Boolean) = prefsRepo.setHapticEnabled(enabled)
    suspend fun setSoundEnabled(enabled: Boolean) = prefsRepo.setSoundEnabled(enabled)
    suspend fun setAutoAdvance(enabled: Boolean) = prefsRepo.setAutoAdvance(enabled)
    suspend fun setFocusModeEnabled(enabled: Boolean) = prefsRepo.setFocusModeEnabled(enabled)
    suspend fun setAutoCountTarget(target: Int) = prefsRepo.setAutoCountTarget(target)
    suspend fun setAutoCountPace(pace: String) = prefsRepo.setAutoCountPace(pace)
    suspend fun setTasbihTargetVibrationPattern(pattern: TasbihVibrationPattern) =
        prefsRepo.setTasbihTargetVibrationPattern(pattern)
    suspend fun setTasbihTargetSoundEffect(effect: TasbihTargetSoundEffect) =
        prefsRepo.setTasbihTargetSoundEffect(effect)
    suspend fun setThemeMode(mode: ThemeMode) = prefsRepo.setThemeMode(mode)
    suspend fun setThemeStyle(style: AppThemeStyle) = prefsRepo.setThemeStyle(style)
    suspend fun unlockAndSelectThemeStyle(style: AppThemeStyle) = prefsRepo.unlockAndSelectThemeStyle(style)
    suspend fun setRosaryBeadDesign(design: RosaryBeadDesign) = prefsRepo.setRosaryBeadDesign(design)
    suspend fun unlockAndSelectBeadDesign(design: RosaryBeadDesign) = prefsRepo.unlockAndSelectBeadDesign(design)
    suspend fun setNightModeEnabled(enabled: Boolean) = prefsRepo.setNightModeEnabled(enabled)
    suspend fun setDailyTasbihTarget(target: Int) = prefsRepo.setDailyTasbihTarget(target)
    suspend fun setDailyTotalGoal(target: Int) = prefsRepo.setDailyTotalGoal(target)
    suspend fun setReminder(
        category: DhikrCategory,
        enabled: Boolean,
        hour: Int,
        minute: Int
    ) = prefsRepo.setReminder(category, enabled, hour, minute)

    suspend fun setPrayerCalculationMethod(methodId: Int) =
        prefsRepo.setPrayerCalculationMethod(methodId)

    suspend fun setAppLanguage(language: AppLanguage) =
        prefsRepo.setAppLanguage(language)

    suspend fun setPrayerLocation(latitude: Double, longitude: Double, label: String) =
        prefsRepo.setPrayerLocation(latitude, longitude, label)

    suspend fun createBackupSnapshot(appVersion: String = "1.0"): AdhkarBackupData {
        if (dhikrDao.getCount() == 0 || counterDao.getCount() == 0) {
            ensureInitialized()
        }
        val prefs = prefsRepo.preferencesFlow.first()
        val allDhikr = dhikrDao.observeAll().first()
        val allCounters = counterDao.observeAll().first()
        val history = dailyActivityDao?.observeAll()?.first().orEmpty()

        return AdhkarBackupData(
            schemaVersion = AdhkarBackupData.CURRENT_SCHEMA_VERSION,
            exportedAtMillis = System.currentTimeMillis(),
            appVersion = appVersion,
            preferences = BackupPreferences(
                fontSizeSp = prefs.fontSizeSp,
                hapticEnabled = prefs.hapticEnabled,
                soundEnabled = prefs.soundEnabled,
                autoAdvance = prefs.autoAdvance,
                themeMode = prefs.themeMode.name,
                nightModeEnabled = prefs.nightModeEnabled,
                lastActiveDate = prefs.lastActiveDate.ifBlank { currentDayKey() },
                totalCompletedAllTime = prefs.totalCompletedAllTime,
                dailyTasbihTarget = prefs.dailyTasbihTarget,
                dailyTotalGoal = prefs.dailyTotalGoal,
                prayerMethodId = prefs.prayerMethodId,
                prayerLatitude = prefs.prayerLatitude,
                prayerLongitude = prefs.prayerLongitude,
                prayerLocationLabel = prefs.prayerLocationLabel,
                appLanguage = prefs.appLanguage.name,
                adFreeUntilMillis = prefs.adFreeUntilMillis,
                appThemeStyle = prefs.appThemeStyle.name,
                unlockedThemeStyles = prefs.unlockedThemeStyles.toList(),
                rosaryBeadDesign = prefs.rosaryBeadDesign.name,
                unlockedBeadDesigns = prefs.unlockedBeadDesigns.toList(),
                reminders = prefs.reminders.map { reminder ->
                    BackupReminder(
                        categoryId = reminder.category.id,
                        enabled = reminder.enabled,
                        hour = reminder.hour,
                        minute = reminder.minute
                    )
                }
            ),
            dhikrProgress = allDhikr.map { dhikr ->
                BackupDhikrProgress(
                    id = dhikr.id,
                    currentCount = dhikr.currentCount,
                    isFavorite = dhikr.isFavorite
                )
            },
            counters = allCounters.map { counter ->
                BackupCounter(
                    id = counter.id,
                    phrase = counter.phrase,
                    virtue = counter.virtue,
                    targetCount = counter.targetCount,
                    currentCount = counter.currentCount,
                    todayCount = counter.todayCount,
                    totalCount = counter.totalCount,
                    sortOrder = counter.sortOrder,
                    lastUpdatedAt = counter.lastUpdatedAt
                )
            },
            dailyHistory = history.map { day ->
                BackupDailyActivity(
                    dateKey = day.dateKey,
                    dhikrCount = day.dhikrCount,
                    tasbihCount = day.tasbihCount,
                    sessionsCount = day.sessionsCount,
                    updatedAt = day.updatedAt
                )
            }
        )
    }

    suspend fun restoreBackup(backup: AdhkarBackupData): UserPreferences {
        ensureInitialized()
        val bp = backup.preferences
        val themeMode = ThemeMode.entries.find { it.name == bp.themeMode } ?: ThemeMode.SYSTEM
        val validMethodId = PrayerCalculationMethod.fromId(bp.prayerMethodId).apiMethodId
        val reminderMap = bp.reminders.associateBy { it.categoryId }
        val restoredReminders = DhikrCategory.entries.map { category ->
            val saved = reminderMap[category.id]
            val (defaultHour, defaultMinute) = DhikrReminderDefaults.defaultTimeFor(category)
            DhikrReminder(
                category = category,
                enabled = saved?.enabled ?: false,
                hour = (saved?.hour ?: defaultHour).coerceIn(0, 23),
                minute = (saved?.minute ?: defaultMinute).coerceIn(0, 59)
            )
        }

        val restoredPrefs = UserPreferences(
            fontSizeSp = bp.fontSizeSp.coerceIn(16f, 36f),
            hapticEnabled = bp.hapticEnabled,
            soundEnabled = bp.soundEnabled,
            autoAdvance = bp.autoAdvance,
            themeMode = themeMode,
            nightModeEnabled = bp.nightModeEnabled,
            lastActiveDate = bp.lastActiveDate.ifBlank { currentDayKey() },
            totalCompletedAllTime = bp.totalCompletedAllTime.coerceAtLeast(0),
            dailyTasbihTarget = bp.dailyTasbihTarget.coerceIn(10, 10000),
            dailyTotalGoal = bp.dailyTotalGoal.coerceIn(10, 10000),
            reminders = restoredReminders,
            prayerMethodId = validMethodId,
            prayerLatitude = bp.prayerLatitude,
            prayerLongitude = bp.prayerLongitude,
            prayerLocationLabel = bp.prayerLocationLabel,
            appLanguage = AppLanguage.fromCode(bp.appLanguage),
            adFreeUntilMillis = bp.adFreeUntilMillis.coerceAtLeast(0L),
            appThemeStyle = AppThemeStyle.fromName(bp.appThemeStyle),
            unlockedThemeStyles = bp.unlockedThemeStyles.toSet() + AppThemeStyle.ANDALUSIAN_EMERALD.name,
            rosaryBeadDesign = RosaryBeadDesign.fromName(bp.rosaryBeadDesign),
            unlockedBeadDesigns = bp.unlockedBeadDesigns.toSet() + RosaryBeadDesign.CLASSIC_RING.name
        )
        prefsRepo.restorePreferences(restoredPrefs)

        dhikrDao.resetAllProgressAndFavorites()
        backup.dhikrProgress.forEach { progress ->
            dhikrDao.updateProgressAndFavorite(
                id = progress.id,
                count = progress.currentCount.coerceAtLeast(0),
                isFavorite = progress.isFavorite
            )
        }

        counterDao.deleteAll()
        if (backup.counters.isNotEmpty()) {
            val restoredCounters = backup.counters.map { counter ->
                Counter(
                    id = counter.id,
                    phrase = counter.phrase.trim(),
                    virtue = counter.virtue,
                    targetCount = counter.targetCount.coerceAtLeast(0),
                    currentCount = counter.currentCount.coerceAtLeast(0),
                    todayCount = counter.todayCount.coerceAtLeast(0),
                    totalCount = counter.totalCount.coerceAtLeast(0),
                    sortOrder = counter.sortOrder,
                    lastUpdatedAt = if (counter.lastUpdatedAt > 0L) counter.lastUpdatedAt else System.currentTimeMillis()
                )
            }
            counterDao.insertAllReplace(restoredCounters)
        } else {
            counterDao.insertAll(AdhkarSeedData.getInitialCounters())
        }

        dailyActivityDao?.let { dao ->
            dao.deleteAll()
            if (backup.dailyHistory.isNotEmpty()) {
                val activities = backup.dailyHistory
                    .filter { it.dateKey.isNotBlank() }
                    .map { item ->
                        DailyActivity(
                            dateKey = item.dateKey,
                            dhikrCount = item.dhikrCount.coerceAtLeast(0),
                            tasbihCount = item.tasbihCount.coerceAtLeast(0),
                            sessionsCount = item.sessionsCount.coerceAtLeast(0),
                            updatedAt = if (item.updatedAt > 0L) item.updatedAt else System.currentTimeMillis()
                        )
                    }
                if (activities.isNotEmpty()) {
                    dao.insertAll(activities)
                }
            }
        }
        return restoredPrefs
    }

    suspend fun setAdFreeUntil(untilMillis: Long) {
        prefsRepo.setAdFreeUntil(untilMillis)
    }

    suspend fun setSelectedTasbihId(id: Int?) {
        val currentCount = if (id != null) {
            counterDao.getById(id)?.currentCount
                ?: prefsRepo.preferencesFlow.first().savedTasbihCounts[id]
                ?: 0
        } else {
            0
        }
        prefsRepo.setSelectedTasbihAndSyncCount(id, currentCount)
    }

    private fun currentDayKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }
}
