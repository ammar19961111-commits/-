package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ads.AdFrequencyPolicy
import com.example.audio.DhikrAudioPlayerController
import com.example.audio.DhikrAudioPlayerState
import com.example.audio.NoOpDhikrAudioPlayerController
import com.example.audio.NoOpQuranPlayerController
import com.example.audio.QuranPlayerController
import com.example.audio.QuranPlayerState
import com.example.data.local.UserPreferences
import com.example.data.model.AdhkarBackupData
import com.example.data.model.AppThemeStyle
import com.example.data.model.Counter
import com.example.data.model.DailyActivity
import com.example.data.model.DailyPrayerSchedule
import com.example.data.model.DailyReflection
import com.example.data.model.DailyReflectionProvider
import com.example.data.model.Dhikr
import com.example.data.model.DhikrCategory
import com.example.data.model.PrayerCalculationMethod
import com.example.data.model.PrayerCityPreset
import com.example.data.model.RosaryBeadDesign
import com.example.data.model.TasbihTargetSoundEffect
import com.example.data.model.TasbihVibrationPattern
import com.example.data.remote.AlAdhanPrayerTimesRemoteSource
import com.example.data.remote.PrayerTimesRemoteSource
import com.example.data.repository.AdhkarRepository
import com.example.location.DeviceLocationProvider
import com.example.location.LocationFetchResult
import com.example.location.NoOpDeviceLocationProvider
import com.example.notifications.NoOpReminderScheduler
import com.example.notifications.ReminderScheduler
import com.example.qibla.NoOpQiblaSensorSource
import com.example.qibla.QiblaSensorSource
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.ThemeMode
import com.example.util.ArabicTextNormalizer
import com.example.util.BackupJsonManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class CategoryProgress(
    val category: DhikrCategory,
    val totalItems: Int,
    val completedItems: Int
) {
    val progressFraction: Float
        get() = if (totalItems == 0) 0f else completedItems.toFloat() / totalItems.toFloat()
}

data class PrayerTimesUiState(
    val isLoading: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val locationUnavailable: Boolean = false,
    val errorMessage: String? = null,
    val schedule: DailyPrayerSchedule? = null,
    val selectedMethod: PrayerCalculationMethod = PrayerCalculationMethod.UMM_AL_QURA
)

enum class SessionTimerStatus {
    IDLE,
    RUNNING,
    PAUSED,
    FINISHED
}

data class TasbihSessionTimerState(
    val selectedMinutes: Int = 5,
    val totalSeconds: Int = 5 * 60,
    val remainingSeconds: Int = 5 * 60,
    val status: SessionTimerStatus = SessionTimerStatus.IDLE,
    val soundAlertEnabled: Boolean = true
) {
    val progressFraction: Float
        get() = if (totalSeconds <= 0) 0f
        else (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)

    val formattedTime: String
        get() {
            val safe = remainingSeconds.coerceAtLeast(0)
            val mins = safe / 60
            val secs = safe % 60
            return String.format(Locale.US, "%02d:%02d", mins, secs)
        }
}

enum class AutoCountStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED
}

enum class AutoCountPace(val intervalMillis: Long) {
    SLOW(2500L),
    MEDIUM(1500L),
    FAST(800L)
}

data class TasbihAutoCountState(
    val targetCount: Int = 33,
    val completedCount: Int = 0,
    val status: AutoCountStatus = AutoCountStatus.IDLE,
    val pace: AutoCountPace = AutoCountPace.MEDIUM,
    val soundAlertEnabled: Boolean = true,
    val hapticAlertEnabled: Boolean = true
) {
    val progressFraction: Float
        get() = if (targetCount <= 0) 0f
        else (completedCount.toFloat() / targetCount.toFloat()).coerceIn(0f, 1f)

    val remainingCount: Int
        get() = (targetCount - completedCount).coerceAtLeast(0)
}

data class AdhkarUiState(
    val isLoading: Boolean = true,
    val allAdhkar: List<Dhikr> = emptyList(),
    val favoriteAdhkar: List<Dhikr> = emptyList(),
    val categoryProgress: List<CategoryProgress> = emptyList(),
    val featuredCategory: DhikrCategory = DhikrCategory.MORNING,
    val tasbihPresets: List<Counter> = emptyList(),
    val selectedTasbihId: Int? = null,
    val searchQuery: String = "",
    val preferences: UserPreferences = UserPreferences(),
    val dailyHistory: List<DailyActivity> = emptyList(),
    val statsPeriod: StatsPeriod = StatsPeriod.WEEKLY,
    val statsSeriesFilter: StatsSeriesFilter = StatsSeriesFilter.ALL,
    val statsSummary: DhikrStatsSummary = DhikrStatsSummary(),
    val quranPlayer: QuranPlayerState = QuranPlayerState(),
    val dhikrAudio: DhikrAudioPlayerState = DhikrAudioPlayerState(),
    val prayerTimes: PrayerTimesUiState = PrayerTimesUiState(),
    val dailyReflection: DailyReflection = DailyReflectionProvider.reflectionForTimestamp(0L),
    val reflectionNextRefreshMillis: Long = DailyReflectionProvider.REFRESH_INTERVAL_MILLIS,
    val sessionTimer: TasbihSessionTimerState = TasbihSessionTimerState(),
    val autoCount: TasbihAutoCountState = TasbihAutoCountState(),
    val isAdFreeActive: Boolean = false,
    val adFreeRemainingMinutes: Int = 0
) {
    val selectedTasbih: Counter?
        get() = tasbihPresets.find { it.id == selectedTasbihId } ?: tasbihPresets.firstOrNull()

    val todayCompletedDhikrCount: Int
        get() = allAdhkar.count { it.isCompleted }

    val totalDhikrCount: Int
        get() = allAdhkar.size

    val todayDhikrTapCount: Int
        get() = allAdhkar.sumOf { it.currentCount }

    val todayTasbihSum: Int
        get() = tasbihPresets.sumOf { it.todayCount }

    val todayTotalCounts: Int
        get() = todayDhikrTapCount + todayTasbihSum

    val dailyTotalGoal: Int
        get() = preferences.dailyTotalGoal

    val dailyTotalGoalProgress: Float
        get() = if (dailyTotalGoal <= 0) 0f else (todayTotalCounts.toFloat() / dailyTotalGoal.toFloat()).coerceIn(0f, 1f)

    val isDailyTotalGoalReached: Boolean
        get() = dailyTotalGoal > 0 && todayTotalCounts >= dailyTotalGoal

    val dailyTasbihTarget: Int
        get() = preferences.dailyTasbihTarget

    val dailyTasbihProgress: Float
        get() = if (dailyTasbihTarget <= 0) 0f else (todayTasbihSum.toFloat() / dailyTasbihTarget.toFloat()).coerceIn(0f, 1f)

    val totalTasbihAllTime: Int
        get() = tasbihPresets.sumOf { it.totalCount }

    val filteredAdhkar: List<Dhikr>
        get() {
            val q = searchQuery.trim()
            if (q.isEmpty()) return emptyList()
            return allAdhkar.filter { ArabicTextNormalizer.matches(it, q) }
        }
}

sealed interface HapticEvent {
    data object Tap : HapticEvent
    data object Milestone : HapticEvent
    data class PreviewPattern(val pattern: TasbihVibrationPattern) : HapticEvent
}

sealed interface ChimeEvent {
    data object DailyGoalCompleted : ChimeEvent
    data object SessionTimerFinished : ChimeEvent
    data object AutoCountTargetReached : ChimeEvent
    data class TargetSoundEffect(val effect: TasbihTargetSoundEffect) : ChimeEvent
}

sealed interface AdEvent {
    data object TryShowInterstitial : AdEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class DhikrViewModel(
    private val repository: AdhkarRepository,
    private val reminderScheduler: ReminderScheduler = NoOpReminderScheduler,
    private val quranPlayerController: QuranPlayerController = NoOpQuranPlayerController,
    private val deviceLocationProvider: DeviceLocationProvider = NoOpDeviceLocationProvider,
    private val prayerTimesRemoteSource: PrayerTimesRemoteSource? = null,
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
    private val reflectionTimeZone: TimeZone = TimeZone.getDefault(),
    private val timerTickIntervalMillis: Long = 1000L,
    private val dhikrAudioController: DhikrAudioPlayerController = NoOpDhikrAudioPlayerController,
    private val qiblaSensorSource: QiblaSensorSource = NoOpQiblaSensorSource,
    private val autoCountTickIntervalMillis: Long? = null
) : ViewModel() {

    val compassSensorSource: QiblaSensorSource
        get() = qiblaSensorSource

    private val searchQueryFlow = MutableStateFlow("")
    private val selectedCategoryIdFlow = MutableStateFlow<String?>(null)
    private val selectedTasbihIdFlow = MutableStateFlow<Int?>(null)
    private val initializedFlow = MutableStateFlow(false)
    private val statsPeriodFlow = MutableStateFlow(StatsPeriod.WEEKLY)
    private val statsSeriesFilterFlow = MutableStateFlow(StatsSeriesFilter.ALL)
    private val prayerTimesStateFlow = MutableStateFlow(
        PrayerTimesUiState(hasLocationPermission = deviceLocationProvider.hasLocationPermission())
    )
    private val dailyReflectionStateFlow = MutableStateFlow(buildReflectionState(clockMillis()))
    private val sessionTimerStateFlow = MutableStateFlow(TasbihSessionTimerState())
    private val autoCountStateFlow = MutableStateFlow(TasbihAutoCountState())
    private val adFreeRefreshTriggerFlow = MutableStateFlow(0L)
    private var sessionTimerJob: Job? = null
    private var autoCountJob: Job? = null
    private var naturalTransitionsCount = 0
    private var lastInterstitialShownAtMillis = 0L

    private val resolvedPrayerRemote: PrayerTimesRemoteSource by lazy {
        prayerTimesRemoteSource ?: AlAdhanPrayerTimesRemoteSource()
    }

    private val _hapticEvents = MutableSharedFlow<HapticEvent>(extraBufferCapacity = 8)
    val hapticEvents = _hapticEvents.asSharedFlow()

    private val _chimeEvents = MutableSharedFlow<ChimeEvent>(extraBufferCapacity = 4)
    val chimeEvents = _chimeEvents.asSharedFlow()

    private val _adEvents = MutableSharedFlow<AdEvent>(extraBufferCapacity = 4)
    val adEvents = _adEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
            initializedFlow.value = true
            val prefs = repository.userPreferences.first()
            selectedTasbihIdFlow.value = prefs.selectedTasbihId
            val restoredPace = runCatching { AutoCountPace.valueOf(prefs.autoCountPace) }
                .getOrDefault(AutoCountPace.MEDIUM)
            autoCountStateFlow.update {
                it.copy(
                    targetCount = prefs.autoCountTarget.coerceIn(1, 10000),
                    pace = restoredPace
                )
            }
            reminderScheduler.syncAll(prefs.reminders)
            prayerTimesStateFlow.updateState(prefs)
            val hasSavedCoords = prefs.prayerLatitude != null && prefs.prayerLongitude != null
            if (hasSavedCoords || deviceLocationProvider.hasLocationPermission()) {
                loadInitialOrSavedPrayerTimes(prefs)
            }
        }
        viewModelScope.launch {
            while (isActive) {
                val now = clockMillis()
                dailyReflectionStateFlow.value = buildReflectionState(now)
                val waitMillis = DailyReflectionProvider.millisUntilNextRefresh(now, reflectionTimeZone)
                delay(waitMillis)
            }
        }
    }

    private fun MutableStateFlow<PrayerTimesUiState>.updateState(prefs: UserPreferences) {
        value = value.copy(
            hasLocationPermission = deviceLocationProvider.hasLocationPermission(),
            selectedMethod = PrayerCalculationMethod.fromId(prefs.prayerMethodId)
        )
    }

    private data class StatsParams(
        val history: List<DailyActivity>,
        val period: StatsPeriod,
        val filter: StatsSeriesFilter
    )

    private data class ReflectionStateParams(
        val reflection: DailyReflection,
        val millisUntilNextRefresh: Long
    )

    private fun buildReflectionState(nowMillis: Long): ReflectionStateParams {
        return ReflectionStateParams(
            reflection = DailyReflectionProvider.reflectionForTimestamp(nowMillis, reflectionTimeZone),
            millisUntilNextRefresh = DailyReflectionProvider.millisUntilNextRefresh(nowMillis, reflectionTimeZone)
        )
    }

    private data class ExtraFeaturesParams(
        val stats: StatsParams,
        val quranPlayer: QuranPlayerState,
        val dhikrAudio: DhikrAudioPlayerState,
        val prayerTimes: PrayerTimesUiState,
        val reflectionState: ReflectionStateParams,
        val sessionTimer: TasbihSessionTimerState,
        val autoCount: TasbihAutoCountState
    )

    private val statsFlow = combine(
        repository.dailyHistory,
        statsPeriodFlow,
        statsSeriesFilterFlow
    ) { history, period, filter ->
        StatsParams(history, period, filter)
    }

    private val audioPlayersFlow = combine(
        quranPlayerController.state,
        dhikrAudioController.state
    ) { quran, dhikrAudio -> quran to dhikrAudio }

    private val tasbihToolsFlow = combine(
        sessionTimerStateFlow,
        autoCountStateFlow
    ) { timer, autoCount -> timer to autoCount }

    private val extraFeaturesFlow = combine(
        statsFlow,
        audioPlayersFlow,
        prayerTimesStateFlow,
        dailyReflectionStateFlow,
        tasbihToolsFlow
    ) { stats, audioPair, prayer, reflectionState, tasbihTools ->
        ExtraFeaturesParams(
            stats = stats,
            quranPlayer = audioPair.first,
            dhikrAudio = audioPair.second,
            prayerTimes = prayer,
            reflectionState = reflectionState,
            sessionTimer = tasbihTools.first,
            autoCount = tasbihTools.second
        )
    }

    val uiState: StateFlow<AdhkarUiState> = combine(
        repository.allAdhkar,
        repository.favoriteAdhkar,
        repository.counters,
        repository.userPreferences,
        searchQueryFlow,
        selectedTasbihIdFlow,
        initializedFlow,
        extraFeaturesFlow,
        adFreeRefreshTriggerFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val allAdhkar = values[0] as List<Dhikr>
        @Suppress("UNCHECKED_CAST")
        val favorites = values[1] as List<Dhikr>
        @Suppress("UNCHECKED_CAST")
        val tasbihList = values[2] as List<Counter>
        val prefs = values[3] as UserPreferences
        val query = values[4] as String
        val selectedTasbihId = values[5] as Int?
        val initialized = values[6] as Boolean
        val extras = values[7] as ExtraFeaturesParams
        val statsParams = extras.stats
        val quranState = extras.quranPlayer
        val prayerState = extras.prayerTimes
        val history = statsParams.history
        val statsPeriod = statsParams.period
        val statsFilter = statsParams.filter

        val progressList = DhikrCategory.entries.map { cat ->
            val items = allAdhkar.filter { it.category == cat.id }
            CategoryProgress(
                category = cat,
                totalItems = items.size,
                completedItems = items.count { it.isCompleted }
            )
        }

        val statsSummary = DhikrStatsCalculator.buildSummary(
            activities = history,
            period = statsPeriod,
            filter = statsFilter,
            language = prefs.appLanguage
        )

        val now = clockMillis()
        val isAdFree = AdFrequencyPolicy.isAdFreeActive(prefs.adFreeUntilMillis, now)
        val remainingMinutes = AdFrequencyPolicy.remainingDurationMinutes(prefs.adFreeUntilMillis, now)

        AdhkarUiState(
            isLoading = !initialized && allAdhkar.isEmpty(),
            allAdhkar = allAdhkar,
            favoriteAdhkar = favorites,
            categoryProgress = progressList,
            featuredCategory = recommendedCategoryForCurrentHour(),
            tasbihPresets = tasbihList,
            selectedTasbihId = selectedTasbihId?.takeIf { id -> tasbihList.any { it.id == id } }
                ?: tasbihList.firstOrNull()?.id,
            searchQuery = query,
            preferences = prefs,
            dailyHistory = history,
            statsPeriod = statsPeriod,
            statsSeriesFilter = statsFilter,
            statsSummary = statsSummary,
            quranPlayer = quranState,
            dhikrAudio = extras.dhikrAudio,
            prayerTimes = prayerState.copy(
                selectedMethod = PrayerCalculationMethod.fromId(prefs.prayerMethodId)
            ),
            dailyReflection = extras.reflectionState.reflection,
            reflectionNextRefreshMillis = extras.reflectionState.millisUntilNextRefresh,
            sessionTimer = extras.sessionTimer,
            autoCount = extras.autoCount,
            isAdFreeActive = isAdFree,
            adFreeRemainingMinutes = remainingMinutes
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AdhkarUiState()
    )

    val categories: StateFlow<List<CategoryProgress>> = uiState
        .map { it.categoryProgress }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val selectedCategoryAdhkar: StateFlow<List<Dhikr>> = selectedCategoryIdFlow
        .filterNotNull()
        .flatMapLatest { categoryId -> repository.observeCategory(categoryId) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun fetchDhikrCategories(): List<DhikrCategory> = DhikrCategory.entries

    fun fetchDhikrByCategory(categoryId: String): Flow<List<Dhikr>> =
        repository.observeCategory(categoryId)

    fun selectCategory(categoryId: String) {
        selectedCategoryIdFlow.value = categoryId
    }

    fun onSearchQueryChange(query: String) {
        searchQueryFlow.value = query
    }

    fun onStatsPeriodChange(period: StatsPeriod) {
        statsPeriodFlow.value = period
    }

    fun onStatsSeriesFilterChange(filter: StatsSeriesFilter) {
        statsSeriesFilterFlow.value = filter
    }

    fun onIncrementDhikr(dhikr: Dhikr) {
        viewModelScope.launch {
            val finished = repository.incrementDhikr(dhikr)
            val hapticEnabled = repository.userPreferences.first().hapticEnabled
            if (hapticEnabled) {
                if (finished) {
                    _hapticEvents.tryEmit(HapticEvent.Milestone)
                } else if (!dhikr.isCompleted) {
                    _hapticEvents.tryEmit(HapticEvent.Tap)
                }
            }
        }
    }

    fun onResetDhikr(id: Int) {
        viewModelScope.launch {
            repository.resetDhikr(id)
        }
    }

    fun onResetCategory(categoryId: String) {
        viewModelScope.launch {
            repository.resetCategory(categoryId)
        }
    }

    fun onToggleFavorite(dhikr: Dhikr) {
        viewModelScope.launch {
            repository.toggleFavorite(dhikr)
        }
    }

    fun onSelectTasbih(id: Int) {
        selectedTasbihIdFlow.value = id
        viewModelScope.launch {
            repository.setSelectedTasbihId(id)
        }
    }

    fun incrementTasbeeh(counter: Counter) {
        viewModelScope.launch {
            val prefs = repository.userPreferences.first()
            val dailyTarget = prefs.dailyTasbihTarget
            val hitCycleTarget = repository.incrementCounter(counter)
            val newTodaySum = repository.counters.first().sumOf { it.todayCount }
            val hitDailyGoal = newTodaySum == dailyTarget
            if (prefs.hapticEnabled) {
                if (hitCycleTarget || hitDailyGoal) {
                    if (prefs.tasbihTargetVibrationPattern != TasbihVibrationPattern.NONE) {
                        _hapticEvents.tryEmit(HapticEvent.Milestone)
                    }
                } else {
                    _hapticEvents.tryEmit(HapticEvent.Tap)
                }
            }
            if (prefs.soundEnabled && prefs.tasbihTargetSoundEffect != TasbihTargetSoundEffect.NONE) {
                if (hitDailyGoal) {
                    _chimeEvents.tryEmit(ChimeEvent.DailyGoalCompleted)
                } else if (hitCycleTarget) {
                    _chimeEvents.tryEmit(ChimeEvent.TargetSoundEffect(prefs.tasbihTargetSoundEffect))
                }
            }
        }
    }

    fun incrementActiveTasbeeh() {
        val current = uiState.value.selectedTasbih ?: return
        incrementTasbeeh(current)
    }

    fun onResetTasbihCycle(id: Int) {
        val wasCompleted = uiState.value.tasbihPresets
            .firstOrNull { it.id == id }
            ?.let { it.currentCount >= it.targetCount } == true
        if (wasCompleted) {
            onNaturalAdTransition()
        }
        viewModelScope.launch {
            repository.resetCounterCycle(id)
            if (repository.userPreferences.first().hapticEnabled) {
                _hapticEvents.tryEmit(HapticEvent.Tap)
            }
        }
    }

    fun onUpdateTasbihTarget(id: Int, targetCount: Int) {
        viewModelScope.launch {
            repository.updateCounterTarget(id, targetCount)
        }
    }

    fun onAddCustomTasbih(phrase: String, targetCount: Int) {
        viewModelScope.launch {
            val newId = repository.addCustomCounter(phrase, targetCount)
            if (newId > 0) {
                selectedTasbihIdFlow.value = newId.toInt()
                repository.setSelectedTasbihId(newId.toInt())
            }
        }
    }

    fun onDeleteTasbihPreset(id: Int) {
        viewModelScope.launch {
            repository.deleteCounter(id)
        }
    }

    fun onFontSizeChange(sizeSp: Float) {
        viewModelScope.launch {
            repository.setFontSize(sizeSp)
        }
    }

    fun onHapticToggle(enabled: Boolean) {
        viewModelScope.launch {
            repository.setHapticEnabled(enabled)
        }
    }

    fun onSoundToggle(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSoundEnabled(enabled)
        }
    }

    fun onSelectTargetVibrationPattern(pattern: TasbihVibrationPattern) {
        viewModelScope.launch {
            repository.setTasbihTargetVibrationPattern(pattern)
            if (pattern != TasbihVibrationPattern.NONE) {
                _hapticEvents.tryEmit(HapticEvent.PreviewPattern(pattern))
            }
        }
    }

    fun onPreviewTargetVibration(pattern: TasbihVibrationPattern) {
        if (pattern != TasbihVibrationPattern.NONE) {
            _hapticEvents.tryEmit(HapticEvent.PreviewPattern(pattern))
        }
    }

    fun onSelectTargetSoundEffect(effect: TasbihTargetSoundEffect) {
        viewModelScope.launch {
            repository.setTasbihTargetSoundEffect(effect)
            if (effect != TasbihTargetSoundEffect.NONE) {
                _chimeEvents.tryEmit(ChimeEvent.TargetSoundEffect(effect))
            }
        }
    }

    fun onPreviewTargetSound(effect: TasbihTargetSoundEffect) {
        if (effect != TasbihTargetSoundEffect.NONE) {
            _chimeEvents.tryEmit(ChimeEvent.TargetSoundEffect(effect))
        }
    }

    fun onAutoAdvanceToggle(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoAdvance(enabled)
        }
    }

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun onSelectThemeStyle(style: AppThemeStyle) {
        if (!uiState.value.preferences.isThemeUnlocked(style)) return
        viewModelScope.launch {
            repository.setThemeStyle(style)
        }
    }

    fun onUnlockAndSelectThemeStyle(style: AppThemeStyle) {
        viewModelScope.launch {
            repository.unlockAndSelectThemeStyle(style)
        }
    }

    fun onSelectRosaryBeadDesign(design: RosaryBeadDesign) {
        if (!uiState.value.preferences.isBeadDesignUnlocked(design)) return
        viewModelScope.launch {
            repository.setRosaryBeadDesign(design)
        }
    }

    fun onUnlockAndSelectBeadDesign(design: RosaryBeadDesign) {
        viewModelScope.launch {
            repository.unlockAndSelectBeadDesign(design)
        }
    }

    fun onLanguageChange(language: AppLanguage) {
        viewModelScope.launch {
            repository.setAppLanguage(language)
        }
    }

    fun onGrantAdFreeReward(durationMillis: Long = AdFrequencyPolicy.AD_FREE_REWARD_DURATION_MS) {
        viewModelScope.launch {
            val currentUntil = repository.userPreferences.first().adFreeUntilMillis
            val newUntil = AdFrequencyPolicy.computeExtendedAdFreeUntilMillis(
                currentAdFreeUntilMillis = currentUntil,
                nowMillis = clockMillis(),
                durationMillis = durationMillis
            )
            repository.setAdFreeUntil(newUntil)
        }
    }

    fun onRefreshAdFreeStatus() {
        adFreeRefreshTriggerFlow.value += 1L
    }

    fun onNaturalAdTransition() {
        val now = clockMillis()
        naturalTransitionsCount += 1
        val isAdFree = AdFrequencyPolicy.isAdFreeActive(
            uiState.value.preferences.adFreeUntilMillis,
            now
        )
        if (
            AdFrequencyPolicy.shouldShowInterstitial(
                isAdFreeActive = isAdFree,
                naturalTransitionsCount = naturalTransitionsCount,
                lastInterstitialShownAtMillis = lastInterstitialShownAtMillis,
                nowMillis = now
            )
        ) {
            lastInterstitialShownAtMillis = now
            _adEvents.tryEmit(AdEvent.TryShowInterstitial)
        }
    }

    fun onNightModeToggle(enabled: Boolean) {
        viewModelScope.launch {
            repository.setNightModeEnabled(enabled)
        }
    }

    fun onDailyTasbihTargetChange(target: Int) {
        viewModelScope.launch {
            repository.setDailyTasbihTarget(target)
        }
    }

    fun onDailyTotalGoalChange(target: Int) {
        viewModelScope.launch {
            repository.setDailyTotalGoal(target)
        }
    }

    fun onUpdateReminder(
        category: DhikrCategory,
        enabled: Boolean,
        hour: Int,
        minute: Int
    ) {
        viewModelScope.launch {
            repository.setReminder(category, enabled, hour, minute)
            reminderScheduler.syncAll(repository.userPreferences.first().reminders)
        }
    }

    fun onResetAllToday() {
        viewModelScope.launch {
            repository.resetAllToday()
        }
    }

    suspend fun createBackupJson(appVersion: String = "1.0"): String {
        val snapshot = repository.createBackupSnapshot(appVersion)
        return BackupJsonManager.serialize(snapshot)
    }

    fun restoreFromBackup(
        backup: AdhkarBackupData,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = runCatching {
                val restoredPrefs = repository.restoreBackup(backup)
                reminderScheduler.syncAll(restoredPrefs.reminders)
                prayerTimesStateFlow.updateState(restoredPrefs)
                val lat = restoredPrefs.prayerLatitude
                val lon = restoredPrefs.prayerLongitude
                if (lat != null && lon != null) {
                    fetchForCoordinates(
                        latitude = lat,
                        longitude = lon,
                        locationLabel = restoredPrefs.prayerLocationLabel,
                        method = PrayerCalculationMethod.fromId(restoredPrefs.prayerMethodId)
                    )
                }
            }
            onComplete(result.isSuccess)
        }
    }

    fun onSelectQuranTrack(index: Int) {
        dhikrAudioController.pauseIfPlaying()
        quranPlayerController.selectAndPlay(index)
    }

    fun onToggleQuranPlayPause() {
        if (!uiState.value.quranPlayer.isPlaying) {
            dhikrAudioController.pauseIfPlaying()
        }
        quranPlayerController.togglePlayPause()
    }

    fun onNextQuranTrack() {
        dhikrAudioController.pauseIfPlaying()
        quranPlayerController.nextTrack()
    }

    fun onPreviousQuranTrack() {
        dhikrAudioController.pauseIfPlaying()
        quranPlayerController.previousTrack()
    }

    fun onSeekQuranTo(positionMs: Long) {
        quranPlayerController.seekTo(positionMs)
    }

    fun onToggleQuranRepeat() {
        quranPlayerController.toggleRepeatOne()
    }

    fun onStopQuranPlayer() {
        quranPlayerController.stopAndHide()
    }

    fun onPlayDisplayedAdhkar(displayedAdhkar: List<Dhikr>, startAtDhikrId: Int? = null) {
        if (displayedAdhkar.isEmpty()) return
        quranPlayerController.pauseIfPlaying()
        dhikrAudioController.playDisplayedAdhkar(displayedAdhkar, startAtDhikrId)
    }

    fun onToggleDhikrAudio(dhikr: Dhikr, displayedAdhkar: List<Dhikr>) {
        val current = uiState.value.dhikrAudio
        val isResumingOrSwitching = current.activeDhikrId != dhikr.id || !current.isPlaying
        if (isResumingOrSwitching) {
            quranPlayerController.pauseIfPlaying()
        }
        dhikrAudioController.togglePlayPause(dhikr = dhikr, displayedAdhkar = displayedAdhkar)
    }

    fun onToggleDhikrAudioBarPlayPause() {
        if (!uiState.value.dhikrAudio.isPlaying) {
            quranPlayerController.pauseIfPlaying()
        }
        dhikrAudioController.togglePlayPause()
    }

    fun onNextDhikrAudio() {
        quranPlayerController.pauseIfPlaying()
        dhikrAudioController.nextTrack()
    }

    fun onPreviousDhikrAudio() {
        quranPlayerController.pauseIfPlaying()
        dhikrAudioController.previousTrack()
    }

    fun onSeekDhikrAudio(positionMs: Long) {
        dhikrAudioController.seekTo(positionMs)
    }

    fun onCycleDhikrAudioSpeed() {
        dhikrAudioController.cyclePlaybackSpeed()
    }

    fun onToggleDhikrAudioRepeat() {
        dhikrAudioController.toggleRepeatCurrent()
    }

    fun onRetryDhikrAudio() {
        quranPlayerController.pauseIfPlaying()
        dhikrAudioController.retryCurrent()
    }

    fun onStopDhikrAudio() {
        dhikrAudioController.stopAndHide()
    }

    fun refreshDailyReflection(nowMillis: Long = clockMillis()) {
        dailyReflectionStateFlow.value = buildReflectionState(nowMillis)
    }

    fun onEnsurePrayerTimesLoaded() {
        val current = prayerTimesStateFlow.value
        val permission = deviceLocationProvider.hasLocationPermission()
        prayerTimesStateFlow.value = current.copy(hasLocationPermission = permission)
        if (current.schedule == null && !current.isLoading) {
            viewModelScope.launch {
                val prefs = repository.userPreferences.first()
                loadInitialOrSavedPrayerTimes(prefs)
            }
        }
    }

    fun onDetectLocationAndFetchPrayerTimes() {
        viewModelScope.launch {
            val prefs = repository.userPreferences.first()
            val method = PrayerCalculationMethod.fromId(prefs.prayerMethodId)
            val permitted = deviceLocationProvider.hasLocationPermission()
            prayerTimesStateFlow.value = prayerTimesStateFlow.value.copy(
                isLoading = true,
                hasLocationPermission = permitted,
                locationUnavailable = false,
                errorMessage = null
            )

            when (val locResult = deviceLocationProvider.fetchCurrentLocation()) {
                is LocationFetchResult.Success -> {
                    repository.setPrayerLocation(
                        latitude = locResult.latitude,
                        longitude = locResult.longitude,
                        label = locResult.locationLabel
                    )
                    fetchForCoordinates(
                        latitude = locResult.latitude,
                        longitude = locResult.longitude,
                        locationLabel = locResult.locationLabel,
                        method = method
                    )
                }
                LocationFetchResult.PermissionMissing -> {
                    prayerTimesStateFlow.value = prayerTimesStateFlow.value.copy(
                        isLoading = false,
                        hasLocationPermission = false
                    )
                }
                LocationFetchResult.Unavailable -> {
                    val savedLat = prefs.prayerLatitude
                    val savedLon = prefs.prayerLongitude
                    if (savedLat != null && savedLon != null) {
                        fetchForCoordinates(savedLat, savedLon, prefs.prayerLocationLabel, method)
                    } else {
                        prayerTimesStateFlow.value = prayerTimesStateFlow.value.copy(
                            isLoading = false,
                            locationUnavailable = true
                        )
                    }
                }
            }
        }
    }

    fun onLocationPermissionDenied() {
        prayerTimesStateFlow.value = prayerTimesStateFlow.value.copy(
            hasLocationPermission = false,
            isLoading = false
        )
    }

    fun onSelectPrayerCity(city: PrayerCityPreset) {
        viewModelScope.launch {
            val prefs = repository.userPreferences.first()
            val method = PrayerCalculationMethod.fromId(prefs.prayerMethodId)
            repository.setPrayerLocation(city.latitude, city.longitude, city.arabicName)
            fetchForCoordinates(
                latitude = city.latitude,
                longitude = city.longitude,
                locationLabel = city.arabicName,
                method = method
            )
        }
    }

    fun onSelectPrayerMethod(method: PrayerCalculationMethod) {
        viewModelScope.launch {
            repository.setPrayerCalculationMethod(method.apiMethodId)
            val currentSchedule = prayerTimesStateFlow.value.schedule
            val prefs = repository.userPreferences.first()
            val lat = currentSchedule?.latitude ?: prefs.prayerLatitude
            val lon = currentSchedule?.longitude ?: prefs.prayerLongitude
            val label = currentSchedule?.locationLabel ?: prefs.prayerLocationLabel
            prayerTimesStateFlow.value = prayerTimesStateFlow.value.copy(selectedMethod = method)
            if (lat != null && lon != null) {
                fetchForCoordinates(lat, lon, label, method)
            }
        }
    }

    fun onRefreshPrayerTimes() {
        viewModelScope.launch {
            val currentSchedule = prayerTimesStateFlow.value.schedule
            val prefs = repository.userPreferences.first()
            val method = PrayerCalculationMethod.fromId(prefs.prayerMethodId)
            val lat = currentSchedule?.latitude ?: prefs.prayerLatitude
            val lon = currentSchedule?.longitude ?: prefs.prayerLongitude
            val label = currentSchedule?.locationLabel ?: prefs.prayerLocationLabel
            if (lat != null && lon != null) {
                fetchForCoordinates(lat, lon, label, method)
            } else {
                onDetectLocationAndFetchPrayerTimes()
            }
        }
    }

    private suspend fun loadInitialOrSavedPrayerTimes(prefs: UserPreferences) {
        val method = PrayerCalculationMethod.fromId(prefs.prayerMethodId)
        val savedLat = prefs.prayerLatitude
        val savedLon = prefs.prayerLongitude
        if (savedLat != null && savedLon != null) {
            fetchForCoordinates(savedLat, savedLon, prefs.prayerLocationLabel, method)
        } else if (deviceLocationProvider.hasLocationPermission()) {
            onDetectLocationAndFetchPrayerTimes()
        }
    }

    private suspend fun fetchForCoordinates(
        latitude: Double,
        longitude: Double,
        locationLabel: String,
        method: PrayerCalculationMethod
    ) {
        prayerTimesStateFlow.value = prayerTimesStateFlow.value.copy(
            isLoading = true,
            locationUnavailable = false,
            errorMessage = null,
            selectedMethod = method
        )
        runCatching {
            resolvedPrayerRemote.fetchDailyPrayerTimes(
                latitude = latitude,
                longitude = longitude,
                locationLabel = locationLabel,
                method = method
            )
        }.onSuccess { schedule ->
            prayerTimesStateFlow.value = prayerTimesStateFlow.value.copy(
                isLoading = false,
                schedule = schedule,
                errorMessage = null,
                locationUnavailable = false,
                hasLocationPermission = deviceLocationProvider.hasLocationPermission()
            )
        }.onFailure { err ->
            prayerTimesStateFlow.value = prayerTimesStateFlow.value.copy(
                isLoading = false,
                errorMessage = err.message ?: "Network error"
            )
        }
    }

    fun onSelectSessionTimerDuration(minutes: Int) {
        val clamped = minutes.coerceIn(1, 180)
        val totalSecs = clamped * 60
        sessionTimerJob?.cancel()
        sessionTimerJob = null
        sessionTimerStateFlow.update {
            it.copy(
                selectedMinutes = clamped,
                totalSeconds = totalSecs,
                remainingSeconds = totalSecs,
                status = SessionTimerStatus.IDLE
            )
        }
    }

    fun onToggleSessionTimerSound(enabled: Boolean) {
        sessionTimerStateFlow.update { it.copy(soundAlertEnabled = enabled) }
    }

    fun onStartOrResumeSessionTimer() {
        val current = sessionTimerStateFlow.value
        if (current.status == SessionTimerStatus.RUNNING) return

        val restartingAfterFinish = current.status == SessionTimerStatus.FINISHED
        val startingSeconds = if (restartingAfterFinish || current.remainingSeconds <= 0) {
            current.totalSeconds
        } else {
            current.remainingSeconds
        }
        if (restartingAfterFinish) {
            onNaturalAdTransition()
        }

        sessionTimerJob?.cancel()
        sessionTimerStateFlow.update {
            it.copy(
                remainingSeconds = startingSeconds,
                status = SessionTimerStatus.RUNNING
            )
        }

        sessionTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(timerTickIntervalMillis.coerceAtLeast(1L))
                val updatedRemaining = (sessionTimerStateFlow.value.remainingSeconds - 1).coerceAtLeast(0)
                if (updatedRemaining == 0) {
                    val finishedState = sessionTimerStateFlow.value.copy(
                        remainingSeconds = 0,
                        status = SessionTimerStatus.FINISHED
                    )
                    sessionTimerStateFlow.value = finishedState
                    repository.recordCompletedSession()
                    if (finishedState.soundAlertEnabled) {
                        _chimeEvents.tryEmit(ChimeEvent.SessionTimerFinished)
                    }
                    if (repository.userPreferences.first().hapticEnabled) {
                        _hapticEvents.tryEmit(HapticEvent.Milestone)
                    }
                    break
                } else {
                    sessionTimerStateFlow.update { it.copy(remainingSeconds = updatedRemaining) }
                }
            }
        }
    }

    fun onPauseSessionTimer() {
        sessionTimerJob?.cancel()
        sessionTimerJob = null
        sessionTimerStateFlow.update { state ->
            if (state.status == SessionTimerStatus.RUNNING) {
                state.copy(status = SessionTimerStatus.PAUSED)
            } else {
                state
            }
        }
    }

    fun onResetSessionTimer() {
        sessionTimerJob?.cancel()
        sessionTimerJob = null
        sessionTimerStateFlow.update { state ->
            state.copy(
                remainingSeconds = state.totalSeconds,
                status = SessionTimerStatus.IDLE
            )
        }
    }

    fun onFocusModeToggle(enabled: Boolean) {
        viewModelScope.launch {
            repository.setFocusModeEnabled(enabled)
        }
    }

    fun onSetAutoCountTarget(target: Int) {
        val clamped = target.coerceIn(1, 10000)
        autoCountJob?.cancel()
        autoCountJob = null
        autoCountStateFlow.update {
            it.copy(
                targetCount = clamped,
                completedCount = 0,
                status = AutoCountStatus.IDLE
            )
        }
        viewModelScope.launch {
            repository.setAutoCountTarget(clamped)
        }
    }

    fun onSelectAutoCountPace(pace: AutoCountPace) {
        autoCountStateFlow.update { it.copy(pace = pace) }
        viewModelScope.launch {
            repository.setAutoCountPace(pace.name)
        }
    }

    fun onToggleAutoCountSoundAlert(enabled: Boolean) {
        autoCountStateFlow.update { it.copy(soundAlertEnabled = enabled) }
    }

    fun onToggleAutoCountHapticAlert(enabled: Boolean) {
        autoCountStateFlow.update { it.copy(hapticAlertEnabled = enabled) }
    }

    fun onStartOrResumeAutoCount() {
        val current = autoCountStateFlow.value
        if (current.status == AutoCountStatus.RUNNING) return

        val restartingAfterComplete = current.status == AutoCountStatus.COMPLETED ||
            current.completedCount >= current.targetCount
        val initialCompleted = if (restartingAfterComplete) 0 else current.completedCount
        if (restartingAfterComplete) {
            onNaturalAdTransition()
        }

        autoCountJob?.cancel()
        autoCountStateFlow.update {
            it.copy(
                completedCount = initialCompleted,
                status = AutoCountStatus.RUNNING
            )
        }

        autoCountJob = viewModelScope.launch {
            while (isActive) {
                val state = autoCountStateFlow.value
                val tickMs = autoCountTickIntervalMillis ?: state.pace.intervalMillis
                delay(tickMs.coerceAtLeast(1L))

                val latestState = autoCountStateFlow.value
                if (latestState.status != AutoCountStatus.RUNNING) break

                val prefs = repository.userPreferences.first()
                val countersList = repository.counters.first()
                val activeCounter = selectedTasbihIdFlow.value
                    ?.let { id -> countersList.find { it.id == id } }
                    ?: countersList.firstOrNull()

                val hitCycleTarget = if (activeCounter != null) {
                    repository.incrementCounter(activeCounter)
                } else {
                    false
                }

                val nextCompleted = latestState.completedCount + 1
                val target = latestState.targetCount.coerceAtLeast(1)
                if (nextCompleted >= target) {
                    val finishedState = latestState.copy(
                        completedCount = target,
                        status = AutoCountStatus.COMPLETED
                    )
                    autoCountStateFlow.value = finishedState
                    if (!hitCycleTarget) {
                        repository.recordCompletedSession()
                    }
                    if (finishedState.soundAlertEnabled &&
                        prefs.soundEnabled &&
                        prefs.tasbihTargetSoundEffect != TasbihTargetSoundEffect.NONE
                    ) {
                        _chimeEvents.tryEmit(ChimeEvent.AutoCountTargetReached)
                    }
                    if (finishedState.hapticAlertEnabled &&
                        prefs.hapticEnabled &&
                        prefs.tasbihTargetVibrationPattern != TasbihVibrationPattern.NONE
                    ) {
                        _hapticEvents.tryEmit(HapticEvent.Milestone)
                    }
                    break
                } else {
                    autoCountStateFlow.update { it.copy(completedCount = nextCompleted) }
                    val updatedSum = repository.counters.first().sumOf { it.todayCount }
                    val hitDailyGoal = updatedSum == prefs.dailyTasbihTarget
                    if (hitDailyGoal &&
                        prefs.soundEnabled &&
                        prefs.tasbihTargetSoundEffect != TasbihTargetSoundEffect.NONE
                    ) {
                        _chimeEvents.tryEmit(ChimeEvent.DailyGoalCompleted)
                    } else if (hitCycleTarget &&
                        prefs.soundEnabled &&
                        prefs.tasbihTargetSoundEffect != TasbihTargetSoundEffect.NONE
                    ) {
                        _chimeEvents.tryEmit(ChimeEvent.TargetSoundEffect(prefs.tasbihTargetSoundEffect))
                    }
                    if (prefs.hapticEnabled) {
                        if (hitCycleTarget || hitDailyGoal) {
                            if (prefs.tasbihTargetVibrationPattern != TasbihVibrationPattern.NONE) {
                                _hapticEvents.tryEmit(HapticEvent.Milestone)
                            }
                        } else {
                            _hapticEvents.tryEmit(HapticEvent.Tap)
                        }
                    }
                }
            }
        }
    }

    fun onPauseAutoCount() {
        autoCountJob?.cancel()
        autoCountJob = null
        autoCountStateFlow.update { state ->
            if (state.status == AutoCountStatus.RUNNING) {
                state.copy(status = AutoCountStatus.PAUSED)
            } else {
                state
            }
        }
    }

    fun onResetAutoCount() {
        autoCountJob?.cancel()
        autoCountJob = null
        autoCountStateFlow.update { state ->
            state.copy(
                completedCount = 0,
                status = AutoCountStatus.IDLE
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        sessionTimerJob?.cancel()
        autoCountJob?.cancel()
        quranPlayerController.release()
        dhikrAudioController.release()
    }

    private fun recommendedCategoryForCurrentHour(): DhikrCategory {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 4..11 -> DhikrCategory.MORNING
            in 12..19 -> DhikrCategory.EVENING
            else -> DhikrCategory.SLEEP
        }
    }

    companion object {
        fun provideFactory(
            repository: AdhkarRepository,
            reminderScheduler: ReminderScheduler = NoOpReminderScheduler,
            quranPlayerController: QuranPlayerController = NoOpQuranPlayerController,
            deviceLocationProvider: DeviceLocationProvider = NoOpDeviceLocationProvider,
            prayerTimesRemoteSource: PrayerTimesRemoteSource? = null,
            dhikrAudioController: DhikrAudioPlayerController = NoOpDhikrAudioPlayerController,
            qiblaSensorSource: QiblaSensorSource = NoOpQiblaSensorSource
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DhikrViewModel(
                        repository = repository,
                        reminderScheduler = reminderScheduler,
                        quranPlayerController = quranPlayerController,
                        deviceLocationProvider = deviceLocationProvider,
                        prayerTimesRemoteSource = prayerTimesRemoteSource,
                        dhikrAudioController = dhikrAudioController,
                        qiblaSensorSource = qiblaSensorSource
                    ) as T
                }
            }
    }
}
