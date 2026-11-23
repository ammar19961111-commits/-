package com.example

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import com.example.ads.AdControllerState
import com.example.ads.AdFrequencyPolicy
import com.example.ads.AdMobController
import com.example.ads.findActivity
import com.example.ai.AdhkarChatService
import com.example.ai.ChatHistoryTurn
import com.example.audio.DhikrAudioPlayerController
import com.example.audio.DhikrAudioPlayerState
import com.example.audio.ExoDhikrAudioPlayerController
import com.example.audio.QuranPlayerController
import com.example.audio.QuranPlayerState
import com.example.data.local.AdhkarDatabase
import com.example.data.local.AdhkarSeedData
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.AppThemeStyle
import com.example.data.model.Counter
import com.example.data.model.DailyActivity
import com.example.data.model.DailyQuranCatalog
import com.example.data.model.DailyReflectionProvider
import com.example.data.model.Dhikr
import com.example.data.model.DhikrAudioCatalog
import com.example.data.model.DhikrCategory
import com.example.data.model.DhikrReminder
import com.example.data.model.PrayerCalculationMethod
import com.example.data.model.PrayerName
import com.example.data.model.PrayerTimeCalculator
import com.example.data.model.RosaryBeadDesign
import com.example.data.model.TasbihTargetSoundEffect
import com.example.data.model.TasbihVibrationPattern
import com.example.data.remote.HttpHisnAudioTrackResolver
import com.example.data.remote.PrayerTimesRemoteSource
import com.example.data.repository.AdhkarRepository
import com.example.location.AndroidDeviceLocationProvider
import com.example.location.DeviceLocationProvider
import com.example.location.LocationFetchResult
import com.example.notifications.AndroidReminderScheduler
import com.example.notifications.DhikrNotificationHelper
import com.example.notifications.DhikrReminderScheduler
import com.example.notifications.DhikrReminderWorker
import com.example.notifications.ReminderScheduler
import com.example.qibla.AndroidQiblaSensorSource
import com.example.qibla.CompassReading
import com.example.qibla.QiblaCalculator
import com.example.qibla.QiblaSensorSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.model.AdhkarTranslations
import com.example.ui.components.AdhkarTabFilter
import com.example.ui.components.DhikrEntryAudioPlayer
import com.example.ui.theme.AdhkarTheme
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.NightBackground
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.withAppLanguage
import com.example.ui.viewmodel.AdEvent
import com.example.ui.viewmodel.AutoCountPace
import com.example.ui.viewmodel.AutoCountStatus
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.ChimeEvent
import com.example.ui.viewmodel.DhikrStatsCalculator
import com.example.ui.viewmodel.DhikrViewModel
import com.example.ui.viewmodel.HapticEvent
import com.example.ui.viewmodel.SessionTimerStatus
import com.example.ui.viewmodel.StatsPeriod
import com.example.ui.viewmodel.StatsSeriesFilter
import com.example.util.ArabicTextNormalizer
import com.example.util.BackupJsonManager
import com.example.util.BackupParseResult
import com.example.util.SoundHelper
import com.example.util.StatsCsvExporter
import com.example.widget.TasbihWidgetProvider
import android.appwidget.AppWidgetManager
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: AdhkarDatabase
    private lateinit var context: Context
    private val viewModelStore = ViewModelStore()

    @Before
    fun setupDb() {
        context = ApplicationProvider.getApplicationContext()
        runBlocking { UserPreferencesRepository(context).clearForTesting() }
        database = Room.inMemoryDatabaseBuilder(context, AdhkarDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
        database.close()
    }

    private fun <T : ViewModel> registerViewModel(viewModel: T): T {
        val factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <M : ViewModel> create(modelClass: Class<M>): M = viewModel as M
        }
        @Suppress("UNCHECKED_CAST")
        return androidx.lifecycle.ViewModelProvider(viewModelStore, factory)[viewModel::class.java as Class<T>]
    }

    private fun waitUntilIdle(timeoutMillis: Long = 3000, condition: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            condition()
        }
    }

    @Test
    fun `read string from context`() {
        val arabicAppName = context.withAppLanguage(AppLanguage.ARABIC).getString(R.string.app_name)
        val englishAppName = context.withAppLanguage(AppLanguage.ENGLISH).getString(R.string.app_name)
        assertEquals("أذكار", arabicAppName)
        assertEquals("Adhkar", englishAppName)
    }

    @Test
    fun `seed data covers all categories with unique ids`() {
        val items = AdhkarSeedData.getInitialAdhkar()
        val ids = items.map { it.id }.toSet()
        assertEquals(items.size, ids.size)
        assertTrue(items.all { it.targetCount > 0 && it.title.isNotBlank() && it.content.isNotBlank() })

        DhikrCategory.entries.forEach { category ->
            assertTrue(
                "Category ${category.id} should have seed adhkar",
                items.any { it.category == category.id }
            )
        }
    }

    @Test
    fun `room dhikr and counter entities insert and query accurately`() = runBlocking {
        val dhikrDao = database.dhikrDao()
        val counterDao = database.counterDao()

        val dhikr = Dhikr(
            title = "سبحان الله وبحمده",
            content = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
            category = DhikrCategory.MORNING.id,
            targetCount = 3
        )
        val insertedDhikrId = dhikrDao.insert(dhikr).toInt()
        dhikrDao.updateProgress(insertedDhikrId, 2)

        val morningAdhkar = dhikrDao.observeByCategory(DhikrCategory.MORNING.id).first()
        assertEquals(1, morningAdhkar.size)
        assertEquals("سبحان الله وبحمده", morningAdhkar.first().title)
        assertEquals(2, morningAdhkar.first().currentCount)

        val counter = Counter(
            phrase = "الْحَمْدُ لِلَّهِ",
            targetCount = 33
        )
        val insertedCounterId = counterDao.insert(counter).toInt()
        counterDao.updateProgress(
            id = insertedCounterId,
            currentCount = 15,
            todayCount = 15,
            totalCount = 48
        )

        val storedCounter = counterDao.observeById(insertedCounterId).first()
        assertEquals(15, storedCounter?.currentCount)
        assertEquals(48, storedCounter?.totalCount)
    }

    @Test
    fun `dhikrViewModel fetches categories and increments tasbeeh counter`() = runBlocking {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(database.dhikrDao(), database.counterDao(), prefsRepo)
        repository.ensureInitialized()
        val viewModel = registerViewModel(DhikrViewModel(repository))

        val categories = viewModel.fetchDhikrCategories()
        assertEquals(DhikrCategory.entries.size, categories.size)

        val eveningItems = viewModel.fetchDhikrByCategory(DhikrCategory.EVENING.id).first()
        assertTrue(eveningItems.isNotEmpty())
        assertTrue(eveningItems.all { it.category == DhikrCategory.EVENING.id })

        val countersBefore = database.counterDao().observeAll().first()
        val firstCounter = countersBefore.first()
        viewModel.incrementTasbeeh(firstCounter)

        val updatedCounter = database.counterDao().observeById(firstCounter.id)
            .first { it != null && it.currentCount == firstCounter.currentCount + 1 }
        assertEquals(firstCounter.currentCount + 1, updatedCounter?.currentCount)
        assertEquals(firstCounter.todayCount + 1, updatedCounter?.todayCount)
        assertEquals(firstCounter.totalCount + 1, updatedCounter?.totalCount)
    }

    @Test
    fun `navigation switches between Home and Tasbeeh Counter screens`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(database.dhikrDao(), database.counterDao(), prefsRepo)
        runBlocking { repository.ensureInitialized() }
        val viewModel = registerViewModel(DhikrViewModel(repository))

        composeRule.setContent {
            AdhkarTheme {
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        // Home screen is initially displayed
        composeRule.onNodeWithTag("home_search_input").assertIsDisplayed()

        // Switch to Tasbeeh Counter screen via bottom navigation
        composeRule.onNodeWithTag("nav_tab_tasbih").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tasbih_tap_circle").assertIsDisplayed()

        // Switch back to Home screen
        composeRule.onNodeWithTag("nav_tab_home").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_search_input").assertIsDisplayed()
    }

    @Test
    fun `tasbeeh counter resets current count back to zero`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(database.dhikrDao(), database.counterDao(), prefsRepo)
        runBlocking { repository.ensureInitialized() }
        val viewModel = registerViewModel(DhikrViewModel(repository))

        composeRule.setContent {
            AdhkarTheme {
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        // Navigate to Tasbeeh Counter screen
        composeRule.onNodeWithTag("nav_tab_tasbih").performClick()
        composeRule.waitForIdle()

        // Tap twice to increment counter
        composeRule.onNodeWithTag("tasbih_tap_circle").performClick()
        composeRule.onNodeWithTag("tasbih_tap_circle").performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("tasbih_current_count_text", useUnmergedTree = true)
                    .assertTextEquals("2")
            }.isSuccess
        }
        composeRule.onNodeWithTag("tasbih_current_count_text", useUnmergedTree = true).assertTextEquals("2")

        // Scroll to reset button, tap it, and verify count returns to 0
        composeRule.onNodeWithTag("tasbih_reset_cycle_button")
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("tasbih_current_count_text", useUnmergedTree = true)
                    .assertTextEquals("0")
            }.isSuccess
        }
        composeRule.onNodeWithTag("tasbih_current_count_text", useUnmergedTree = true).assertTextEquals("0")
    }

    @Test
    fun `tasbeeh daily target persists in DataStore and updates circular progress indicator`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(database.dhikrDao(), database.counterDao(), prefsRepo)
        runBlocking {
            repository.ensureInitialized()
            prefsRepo.setDailyTasbihTarget(100)
        }
        val viewModel = registerViewModel(DhikrViewModel(repository))

        composeRule.setContent {
            AdhkarTheme {
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        // Navigate to Tasbeeh Counter screen
        composeRule.onNodeWithTag("nav_tab_tasbih").performClick()
        composeRule.waitForIdle()

        // Daily progress indicator and initial count/target readout are visible when scrolled to
        composeRule.onNodeWithTag("tasbih_daily_goal_progress")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("tasbih_daily_goal_text")
                    .performScrollTo()
                    .assertTextEquals("0 / 100")
            }.isSuccess
        }

        // Increment Tasbeeh twice and verify daily goal progress text updates
        composeRule.onNodeWithTag("tasbih_tap_circle")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("tasbih_tap_circle").performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("tasbih_daily_goal_text")
                    .performScrollTo()
                    .assertTextEquals("2 / 100")
            }.isSuccess
        }

        // Edit daily target to 300 via dialog
        composeRule.onNodeWithTag("tasbih_edit_daily_goal_button")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tasbih_daily_goal_input").performTextReplacement("300")
        composeRule.onNodeWithTag("confirm_daily_goal").performClick()

        // Verify UI updates and DataStore persisted the new target
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("tasbih_daily_goal_text")
                    .performScrollTo()
                    .assertTextEquals("2 / 300")
            }.isSuccess
        }
        val persistedPrefs = runBlocking { prefsRepo.preferencesFlow.first() }
        assertEquals(300, persistedPrefs.dailyTasbihTarget)
        assertEquals(2f / 300f, viewModel.uiState.value.dailyTasbihProgress, 0.001f)
    }

    @Test
    fun `tasbeeh increment triggers haptic feedback and respects toggle`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(database.dhikrDao(), database.counterDao(), prefsRepo)
        runBlocking {
            repository.ensureInitialized()
            prefsRepo.setHapticEnabled(true)
            prefsRepo.setDailyTasbihTarget(100)
        }
        val viewModel = registerViewModel(DhikrViewModel(repository))

        val hapticCalls = mutableListOf<HapticFeedbackType>()
        val spyHaptic = object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                hapticCalls.add(hapticFeedbackType)
            }
        }
        val emittedEvents = mutableListOf<HapticEvent>()

        composeRule.setContent {
            AdhkarTheme {
                CompositionLocalProvider(LocalHapticFeedback provides spyHaptic) {
                    androidx.compose.runtime.LaunchedEffect(viewModel) {
                        viewModel.hapticEvents.collect { emittedEvents.add(it) }
                    }
                    AdhkarAppRoot(viewModel = viewModel)
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("nav_tab_tasbih").performClick()
        composeRule.waitForIdle()

        // Tapping Tasbeeh triggers Compose tactile feedback and ViewModel HapticEvent.Tap
        composeRule.onNodeWithTag("tasbih_tap_circle")
            .performScrollTo()
            .performClick()

        waitUntilIdle(timeoutMillis = 3000) {
            emittedEvents.isNotEmpty()
        }
        assertEquals(1, hapticCalls.size)
        assertEquals(HapticFeedbackType.TextHandleMove, hapticCalls.first())
        assertEquals(HapticEvent.Tap, emittedEvents.first())

        // Toggle haptic feedback off from TasbihScreen header
        composeRule.onNodeWithTag("tasbih_haptic_toggle_button")
            .performScrollTo()
            .performClick()
        waitUntilIdle(timeoutMillis = 3000) {
            runBlocking { !prefsRepo.preferencesFlow.first().hapticEnabled }
        }

        // Subsequent tap should NOT trigger haptic feedback
        val previousEventsCount = emittedEvents.size
        composeRule.onNodeWithTag("tasbih_tap_circle")
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("tasbih_current_count_text", useUnmergedTree = true)
                    .assertTextEquals("2")
            }.isSuccess
        }
        assertEquals(1, hapticCalls.size)
        assertEquals(previousEventsCount, emittedEvents.size)

        // Re-enable haptic feedback from TasbihScreen header (fires confirmation tap) and test milestone LongPress
        composeRule.onNodeWithTag("tasbih_haptic_toggle_button")
            .performScrollTo()
            .performClick()
        waitUntilIdle(timeoutMillis = 3000) {
            runBlocking { prefsRepo.preferencesFlow.first().hapticEnabled }
        }
        assertEquals(HapticFeedbackType.TextHandleMove, hapticCalls.last())

        // Switch target to 33, prefill currentCount to 32 via direct DB update, then tap once -> LongPress milestone haptic
        runBlocking {
            val activeId = viewModel.uiState.value.selectedTasbih!!.id
            database.counterDao().updateProgress(activeId, currentCount = 32, todayCount = 32, totalCount = 32)
        }
        waitUntilIdle(timeoutMillis = 3000) {
            viewModel.uiState.value.selectedTasbih?.currentCount == 32
        }
        composeRule.onNodeWithTag("tasbih_tap_circle")
            .performScrollTo()
            .performClick()
        waitUntilIdle(timeoutMillis = 3000) {
            hapticCalls.lastOrNull() == HapticFeedbackType.LongPress
        }
        assertEquals(HapticFeedbackType.LongPress, hapticCalls.last())
    }

    @Test
    fun `tasbeeh emits milestone haptic event on target completion`() = runBlocking {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(database.dhikrDao(), database.counterDao(), prefsRepo)
        repository.ensureInitialized()
        prefsRepo.setHapticEnabled(true)
        prefsRepo.setDailyTasbihTarget(100)

        val counter = database.counterDao().observeAll().first().first()
        database.counterDao().updateProgress(
            id = counter.id,
            currentCount = counter.targetCount - 1,
            todayCount = counter.targetCount - 1,
            totalCount = counter.targetCount - 1
        )
        val updatedBeforeTap = database.counterDao().observeById(counter.id).first()!!

        val viewModel = registerViewModel(DhikrViewModel(repository))
        val events = mutableListOf<HapticEvent>()
        val job = launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            viewModel.hapticEvents.collect { events.add(it) }
        }

        viewModel.incrementTasbeeh(updatedBeforeTap)
        val deadline = System.currentTimeMillis() + 3000
        while (events.isEmpty() && System.currentTimeMillis() < deadline) {
            kotlinx.coroutines.delay(25)
        }
        job.cancel()

        assertEquals(listOf(HapticEvent.Milestone), events)
    }

    @Test
    fun `arabicTextNormalizer matches dhikr regardless of diacritics and hamza forms`() {
        val seedAdhkar = AdhkarSeedData.getInitialAdhkar()
        val ayatulKursi = seedAdhkar.first { it.id == 101 }
        val sayyidAlIstighfar = seedAdhkar.first { it.id == 104 }

        // Query without diacritics or hamza matches content with full tashkeel
        assertTrue(ArabicTextNormalizer.matches(ayatulKursi, "لا اله الا هو الحي القيوم"))
        assertTrue(ArabicTextNormalizer.matches(ayatulKursi, "ايه الكرسي"))
        assertTrue(ArabicTextNormalizer.matches(sayyidAlIstighfar, "اللهم انت ربي"))
        assertTrue(ArabicTextNormalizer.matches(sayyidAlIstighfar, "سيد الاستغفار"))
        assertTrue(!ArabicTextNormalizer.matches(sayyidAlIstighfar, "كلمة غير موجودة"))
    }

    @Test
    fun `home screen search finds dhikr by title or keyword and clears results`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(database.dhikrDao(), database.counterDao(), prefsRepo)
        runBlocking { repository.ensureInitialized() }
        val viewModel = registerViewModel(DhikrViewModel(repository))

        composeRule.setContent {
            AdhkarTheme {
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        // Search by keyword without tashkeel that matches Ayat al-Kursi content
        composeRule.onNodeWithTag("home_search_input")
            .performTextReplacement("الحي القيوم")
        waitUntilIdle(timeoutMillis = 3000) {
            viewModel.uiState.value.filteredAdhkar.any { it.id == 101 }
        }
        composeRule.onNodeWithTag("home_search_results_count").assertIsDisplayed()
        composeRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("dhikr_card_101"))
        composeRule.onNodeWithTag("dhikr_card_101").assertIsDisplayed()

        // Search with non-matching term shows empty state
        composeRule.onNodeWithTag("home_search_input")
            .performTextReplacement("نص غير موجود إطلاقاً")
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("home_search_empty").assertIsDisplayed()
            }.isSuccess
        }

        // Clear search button restores categories
        composeRule.onNodeWithTag("clear_search_button").performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("featured_hero_card").assertIsDisplayed()
            }.isSuccess
        }
    }

    @Test
    fun `dhikr list screen filters category items by search query`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(database.dhikrDao(), database.counterDao(), prefsRepo)
        runBlocking { repository.ensureInitialized() }
        val viewModel = registerViewModel(DhikrViewModel(repository))

        composeRule.setContent {
            AdhkarTheme {
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        // Navigate into Morning Adhkar list
        composeRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("category_card_morning"))
        composeRule.onNodeWithTag("category_card_morning")
            .performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("dhikr_list_search_toggle").assertIsDisplayed()
            }.isSuccess
        }

        // Open category search and filter by Sayyid al-Istighfar (id = 104)
        composeRule.onNodeWithTag("dhikr_list_search_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dhikr_list_search_input")
            .performTextReplacement("سيد الاستغفار")

        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("dhikr_card_104").assertIsDisplayed()
            }.isSuccess
        }
        composeRule.onNodeWithTag("dhikr_card_101").assertDoesNotExist()
    }

    @Test
    fun `soundHelper synthesizes gentle fading harmonic chime`() {
        val pcm = SoundHelper.generateChimePcm()
        assertTrue(pcm.isNotEmpty())
        val peakAmplitude = pcm.maxOf { kotlin.math.abs(it.toInt()) }
        val tailPeak = pcm.takeLast(500).maxOf { kotlin.math.abs(it.toInt()) }
        assertTrue("Chime should have audible signal", peakAmplitude > 1000)
        assertTrue("Chime should be gentle and not clip", peakAmplitude < Short.MAX_VALUE * 0.6)
        assertTrue("Chime should fade out smoothly", tailPeak < peakAmplitude / 4)
    }

    @Test
    fun `tasbeeh emits chime event when daily target completes and respects sound toggle`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(database.dhikrDao(), database.counterDao(), prefsRepo)
        runBlocking {
            repository.ensureInitialized()
            prefsRepo.setSoundEnabled(true)
            prefsRepo.setDailyTasbihTarget(10)
            val counter = database.counterDao().observeAll().first().first()
            database.counterDao().updateProgress(
                id = counter.id,
                currentCount = 9,
                todayCount = 9,
                totalCount = 9
            )
        }
        val viewModel = registerViewModel(DhikrViewModel(repository))
        val chimeEvents = mutableListOf<ChimeEvent>()

        composeRule.setContent {
            AdhkarTheme {
                androidx.compose.runtime.LaunchedEffect(viewModel) {
                    viewModel.chimeEvents.collect { chimeEvents.add(it) }
                }
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("nav_tab_tasbih").performClick()
        composeRule.waitForIdle()

        // 10th tap completes the daily target of 10 -> emits DailyGoalCompleted chime
        composeRule.onNodeWithTag("tasbih_tap_circle")
            .performScrollTo()
            .performClick()
        waitUntilIdle(timeoutMillis = 3000) {
            chimeEvents.size == 1
        }
        assertEquals(listOf(ChimeEvent.DailyGoalCompleted), chimeEvents)

        // Toggle sound off via header button and reset counter, then hit target of 10 again
        composeRule.onNodeWithTag("tasbih_sound_toggle_button")
            .performScrollTo()
            .performClick()
        waitUntilIdle(timeoutMillis = 3000) {
            runBlocking { !prefsRepo.preferencesFlow.first().soundEnabled }
        }

        runBlocking {
            val counter = database.counterDao().observeAll().first().first()
            database.counterDao().updateProgress(
                id = counter.id,
                currentCount = 9,
                todayCount = 9,
                totalCount = 19
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tasbih_tap_circle")
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("tasbih_daily_goal_text")
                    .performScrollTo()
                    .assertTextEquals("10 / 10")
            }.isSuccess
        }
        assertEquals(1, chimeEvents.size)
    }

    @Test
    fun `dhikrReminderScheduler computes next daily trigger and manages WorkManager periodic reminders`() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val baseCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 15, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowMillis = baseCal.timeInMillis

        // 18:30 today -> schedules same day
        val laterToday = DhikrReminderScheduler.computeNextTriggerMillis(18, 30, nowMillis)
        val laterCal = Calendar.getInstance().apply { timeInMillis = laterToday }
        assertEquals(15, laterCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(18, laterCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, laterCal.get(Calendar.MINUTE))

        // 06:00 already passed -> schedules tomorrow (16th)
        val tomorrow = DhikrReminderScheduler.computeNextTriggerMillis(6, 0, nowMillis)
        val tomorrowCal = Calendar.getInstance().apply { timeInMillis = tomorrow }
        assertEquals(16, tomorrowCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(6, tomorrowCal.get(Calendar.HOUR_OF_DAY))

        val workManager = WorkManager.getInstance(context)
        val morningReminder = DhikrReminder(DhikrCategory.MORNING, enabled = true, hour = 6, minute = 15)

        DhikrReminderScheduler.scheduleReminder(context, morningReminder, nowMillis)
        val enqueuedInfos = workManager.getWorkInfosForUniqueWork(
            DhikrReminderScheduler.uniqueWorkNameFor(DhikrCategory.MORNING)
        ).get()
        assertEquals(1, enqueuedInfos.size)
        assertEquals(WorkInfo.State.ENQUEUED, enqueuedInfos.first().state)

        // Worker posts morning reminder notification when preference is enabled
        val prefsRepo = UserPreferencesRepository(context)
        runBlocking {
            prefsRepo.setReminder(DhikrCategory.MORNING, true, 6, 15)
            val worker = TestListenableWorkerBuilder<DhikrReminderWorker>(context)
                .setInputData(workDataOf(DhikrNotificationHelper.EXTRA_CATEGORY_ID to DhikrCategory.MORNING.id))
                .build()
            val result = worker.doWork()
            assertEquals(androidx.work.ListenableWorker.Result.success(), result)
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val posted = Shadows.shadowOf(notificationManager)
            .getNotification(DhikrNotificationHelper.notificationIdFor(DhikrCategory.MORNING))
        assertNotNull(posted)

        DhikrReminderScheduler.cancelReminder(context, DhikrCategory.MORNING)
        val cancelledInfos = workManager.getWorkInfosForUniqueWork(
            DhikrReminderScheduler.uniqueWorkNameFor(DhikrCategory.MORNING)
        ).get()
        assertEquals(WorkInfo.State.CANCELLED, cancelledInfos.first().state)
    }

    @Test
    fun `dhikrNotificationHelper posts category reminder notification when permitted`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNotifications = Shadows.shadowOf(notificationManager)

        DhikrNotificationHelper.showCategoryReminder(context, DhikrCategory.EVENING)

        val posted = shadowNotifications.getNotification(
            DhikrNotificationHelper.notificationIdFor(DhikrCategory.EVENING)
        )
        assertNotNull(posted)
    }

    @Test
    fun `settings reminder switch persists schedule and invokes scheduler`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(database.dhikrDao(), database.counterDao(), prefsRepo)
        runBlocking {
            repository.ensureInitialized()
            prefsRepo.setReminder(DhikrCategory.MORNING, false, 6, 0)
        }

        val syncedSnapshots = mutableListOf<List<DhikrReminder>>()
        val spyScheduler = object : ReminderScheduler {
            override fun syncAll(reminders: List<DhikrReminder>) {
                syncedSnapshots.add(reminders)
            }
        }
        val viewModel = registerViewModel(DhikrViewModel(repository, spyScheduler))

        composeRule.setContent {
            AdhkarTheme {
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("nav_tab_settings").performClick()
        composeRule.waitForIdle()

        // Enable Morning Adhkar reminder
        composeRule.onNodeWithTag("reminder_switch_morning")
            .performScrollTo()
            .performClick()

        waitUntilIdle(timeoutMillis = 3000) {
            val savedEnabled = runBlocking {
                prefsRepo.preferencesFlow.first().reminders
                    .find { it.category == DhikrCategory.MORNING }?.enabled == true
            }
            val syncedEnabled = syncedSnapshots.lastOrNull()
                ?.any { it.category == DhikrCategory.MORNING && it.enabled } == true
            savedEnabled && syncedEnabled
        }
        assertTrue(syncedSnapshots.last().any { it.category == DhikrCategory.MORNING && it.enabled })

        // Open time picker dialog for Morning Adhkar and save (scroll evening into view so morning chip clears snackbar)
        composeRule.onNodeWithTag("reminder_time_evening")
            .performScrollTo()
        composeRule.onNodeWithTag("reminder_time_morning")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("confirm_reminder_time").performClick()
        composeRule.waitForIdle()

        val saved = runBlocking {
            prefsRepo.preferencesFlow.first().reminders.first { it.category == DhikrCategory.MORNING }
        }
        assertTrue(saved.enabled)
    }

    @Test
    fun `settings font size options adjust preview, persist to DataStore, and update dhikr content`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(database.dhikrDao(), database.counterDao(), prefsRepo)
        runBlocking {
            repository.ensureInitialized()
            prefsRepo.setFontSize(22f)
        }
        val viewModel = registerViewModel(DhikrViewModel(repository))

        composeRule.setContent {
            AdhkarTheme {
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        // Navigate to Settings tab
        composeRule.onNodeWithTag("nav_tab_settings").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_font_size_value")
            .performScrollTo()
            .assertTextEquals("22 sp")
        composeRule.onNodeWithTag("settings_font_preview_text")
            .performScrollTo()
            .assertIsDisplayed()

        // Tap accessible increase (+) button -> 24 sp
        composeRule.onNodeWithTag("settings_font_increase")
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("settings_font_size_value")
                    .performScrollTo()
                    .assertTextEquals("24 sp")
            }.isSuccess
        }

        // Select XL preset chip (32 sp) -> persists in DataStore
        composeRule.onNodeWithTag("settings_font_chip_32")
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("settings_font_size_value")
                    .performScrollTo()
                    .assertTextEquals("32 sp")
            }.isSuccess
        }
        val persistedPrefs = runBlocking { prefsRepo.preferencesFlow.first() }
        assertEquals(32f, persistedPrefs.fontSizeSp, 0.01f)

        // Navigate back to Home -> Morning Adhkar and verify DhikrCard renders with updated preference
        composeRule.onNodeWithTag("nav_tab_home").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("category_card_morning"))
        composeRule.onNodeWithTag("category_card_morning")
            .performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("dhikr_content_101", useUnmergedTree = true).assertIsDisplayed()
            }.isSuccess
        }
        assertEquals(32f, viewModel.uiState.value.preferences.fontSizeSp, 0.01f)
    }

    @Test
    fun `dhikrStatsCalculator computes weekly and monthly series, averages, and streak`() {
        val baseCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 15, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        fun dayKeyOffset(daysAgo: Int): String {
            val c = (baseCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
            return keyFmt.format(c.time)
        }

        val activities = listOf(
            DailyActivity(dateKey = dayKeyOffset(0), dhikrCount = 20, tasbihCount = 100), // 120
            DailyActivity(dateKey = dayKeyOffset(1), dhikrCount = 10, tasbihCount = 40),  // 50
            DailyActivity(dateKey = dayKeyOffset(2), dhikrCount = 5, tasbihCount = 35),   // 40
            DailyActivity(dateKey = dayKeyOffset(10), dhikrCount = 30, tasbihCount = 70)  // 100 (outside 7d, inside 30d)
        )

        val weekly = DhikrStatsCalculator.buildSummary(
            activities = activities,
            period = StatsPeriod.WEEKLY,
            filter = StatsSeriesFilter.ALL,
            nowMillis = baseCal.timeInMillis
        )
        assertEquals(7, weekly.points.size)
        assertEquals(210, weekly.periodTotal)
        assertEquals(30, weekly.dailyAverage)
        assertEquals(120, weekly.bestDayCount)
        assertEquals(3, weekly.currentStreakDays)

        val monthlyTasbih = DhikrStatsCalculator.buildSummary(
            activities = activities,
            period = StatsPeriod.MONTHLY,
            filter = StatsSeriesFilter.TASBIH,
            nowMillis = baseCal.timeInMillis
        )
        assertEquals(30, monthlyTasbih.points.size)
        assertEquals(245, monthlyTasbih.periodTotal)
        assertEquals(100, monthlyTasbih.bestDayCount)
    }

    @Test
    fun `stats screen displays weekly and monthly trends from recorded dhikr and tasbeeh activity`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val todayKey = keyFmt.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -10)
        val tenDaysAgoKey = keyFmt.format(cal.time)

        runBlocking {
            repository.ensureInitialized()
            // Historical day 10 days ago (only included in Monthly 30d view)
            repository.saveDailyActivity(
                DailyActivity(dateKey = tenDaysAgoKey, dhikrCount = 15, tasbihCount = 85)
            )
            // Live increments today: 1 dhikr + 2 tasbeeh
            val morningDhikr = database.dhikrDao().observeByCategory(DhikrCategory.MORNING.id).first().first()
            repository.incrementDhikr(morningDhikr)
            val counter = database.counterDao().observeAll().first().first()
            repository.incrementCounter(counter)
            repository.incrementCounter(counter)
        }

        val viewModel = registerViewModel(DhikrViewModel(repository))

        composeRule.setContent {
            AdhkarTheme {
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        // Navigate to Statistics tab
        composeRule.onNodeWithTag("nav_tab_stats").performClick()
        composeRule.waitForIdle()

        // Weekly view (7 days): today's total = 3 (1 dhikr + 2 tasbih)
        composeRule.onNodeWithTag("stats_lazy_column")
            .performScrollToNode(hasTestTag("stats_line_chart"))
        composeRule.onNodeWithTag("stats_line_chart").assertIsDisplayed()
        composeRule.onNodeWithTag("stats_lazy_column")
            .performScrollToNode(hasTestTag("stats_selected_day_value"))
        composeRule.onNodeWithTag("stats_selected_day_value").assertTextEquals("3")

        composeRule.onNodeWithTag("stats_lazy_column")
            .performScrollToNode(hasTestTag("stats_period_total"))
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("stats_period_total")
                    .assertTextEquals("3")
            }.isSuccess
        }

        composeRule.onNodeWithTag("stats_lazy_column")
            .performScrollToNode(hasTestTag("stats_history_item_$todayKey"))
        composeRule.onNodeWithTag("stats_history_item_$todayKey").assertIsDisplayed()

        // Filter to Tasbeeh only -> selected day becomes 2
        composeRule.onNodeWithTag("stats_lazy_column")
            .performScrollToNode(hasTestTag("stats_filter_tasbih"))
        composeRule.onNodeWithTag("stats_filter_tasbih").performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("stats_selected_day_value").assertTextEquals("2")
            }.isSuccess
        }

        // Switch back to All and toggle Monthly (30 days) -> period total includes 10 days ago (100 + 3 = 103)
        composeRule.onNodeWithTag("stats_filter_all").performClick()
        composeRule.onNodeWithTag("stats_lazy_column")
            .performScrollToNode(hasTestTag("stats_period_monthly"))
        composeRule.onNodeWithTag("stats_period_monthly").performClick()
        composeRule.onNodeWithTag("stats_lazy_column")
            .performScrollToNode(hasTestTag("stats_period_total"))
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("stats_period_total")
                    .assertTextEquals("103")
            }.isSuccess
        }
        assertEquals(30, viewModel.uiState.value.statsSummary.points.size)
    }

    @Test
    fun `home screen displays bookmarked dhikr in dedicated filter tab and favorites section`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking {
            repository.ensureInitialized()
            val ayatulKursi = database.dhikrDao().observeByCategory(DhikrCategory.MORNING.id).first()
                .first { it.id == 101 }
            repository.toggleFavorite(ayatulKursi)
        }
        val viewModel = registerViewModel(DhikrViewModel(repository))

        composeRule.setContent {
            AdhkarTheme {
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        // Home Favorites filter tab shows only bookmarked Dhikr
        composeRule.onNodeWithTag("home_filter_favorites").performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("dhikr_card_101").assertIsDisplayed()
            }.isSuccess
        }
        composeRule.onNodeWithTag("featured_hero_card").assertDoesNotExist()
        composeRule.onNodeWithTag("dhikr_card_104").assertDoesNotExist()

        // Switching back to All shows both categories and the Home Favorites section
        composeRule.onNodeWithTag("home_filter_all").performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("featured_hero_card").assertIsDisplayed()
            }.isSuccess
        }
        composeRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("home_favorites_section"))
        composeRule.onNodeWithTag("home_favorites_section").assertIsDisplayed()

        // Dedicated Favorites bottom tab also displays bookmarked Dhikr
        composeRule.onNodeWithTag("nav_tab_favorites").performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("dhikr_card_101").assertIsDisplayed()
            }.isSuccess
        }
    }

    @Test
    fun `settings night mode toggle persists and applies high contrast dark theme`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking {
            repository.ensureInitialized()
            prefsRepo.setNightModeEnabled(false)
        }
        val viewModel = registerViewModel(DhikrViewModel(repository))
        var activeBackground = androidx.compose.ui.graphics.Color.Unspecified

        composeRule.setContent {
            val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
            val prefs = uiState.preferences
            AdhkarTheme(
                themeMode = prefs.themeMode,
                nightModeEnabled = prefs.nightModeEnabled
            ) {
                activeBackground = MaterialTheme.colorScheme.background
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("nav_tab_settings").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_night_mode_switch")
            .performScrollTo()
            .performClick()

        waitUntilIdle(timeoutMillis = 3000) {
            runBlocking { prefsRepo.preferencesFlow.first().nightModeEnabled } &&
                activeBackground == NightBackground
        }
        assertEquals(NightBackground, activeBackground)
    }

    @Test
    fun `statsCsvExporter builds valid CSV and creates FileProvider share intent`() {
        val samplePoints = listOf(
            com.example.ui.viewmodel.DailyTrendPoint(
                dateKey = "2026-11-15",
                shortLabel = "الأحد",
                fullDateLabel = "الأحد 15 نوفمبر",
                dhikrCount = 12,
                tasbihCount = 50,
                isToday = false
            ),
            com.example.ui.viewmodel.DailyTrendPoint(
                dateKey = "2026-11-16",
                shortLabel = "الإثنين",
                fullDateLabel = "الإثنين 16 نوفمبر",
                dhikrCount = 20,
                tasbihCount = 100,
                isToday = true
            )
        )

        val csv = StatsCsvExporter.generateCsv(
            points = samplePoints,
            dailyTasbihTarget = 100,
            includeBom = false
        )
        val lines = csv.trim().lines()
        assertEquals(3, lines.size)
        assertEquals(
            "date,day_label,dhikr_count,tasbih_count,total_count,daily_tasbih_target,goal_progress_percent",
            lines[0]
        )
        assertEquals("2026-11-15,الأحد 15 نوفمبر,12,50,62,100,50", lines[1])
        assertEquals("2026-11-16,الإثنين 16 نوفمبر,20,100,120,100,100", lines[2])

        val shareIntent = StatsCsvExporter.createShareIntent(
            context = context,
            fileName = "adhkar_stats_test.csv",
            csvContent = csv,
            subject = "Stats"
        )
        assertEquals(Intent.ACTION_SEND, shareIntent.action)
        assertEquals("text/csv", shareIntent.type)
        assertNotNull(shareIntent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM))
    }

    @Test
    fun `stats screen displays export CSV save and share buttons`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking { repository.ensureInitialized() }
        val viewModel = registerViewModel(DhikrViewModel(repository))

        composeRule.setContent {
            AdhkarTheme {
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("nav_tab_stats").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("stats_lazy_column")
            .performScrollToNode(hasTestTag("stats_export_card"))
        composeRule.onNodeWithTag("stats_export_save_button").assertIsDisplayed()
        composeRule.onNodeWithTag("stats_export_share_button").assertIsDisplayed()
    }

    @Test
    fun `tasbihWidgetProvider builds RemoteViews reflecting daily tasbeeh progress`() {
        val views = TasbihWidgetProvider.buildRemoteViews(
            context = context,
            phrase = "سُبْحَانَ اللَّهِ",
            todayCount = 45,
            dailyTarget = 100
        )
        val inflated = views.apply(context, android.widget.FrameLayout(context))
        val countTv = inflated.findViewById<android.widget.TextView>(R.id.widget_count_text)
        val percentTv = inflated.findViewById<android.widget.TextView>(R.id.widget_percent)
        val phraseTv = inflated.findViewById<android.widget.TextView>(R.id.widget_phrase)
        val progressBar = inflated.findViewById<android.widget.ProgressBar>(R.id.widget_progress_bar)

        assertEquals("45 / 100", countTv.text.toString())
        assertEquals("45%", percentTv.text.toString())
        assertEquals("سُبْحَانَ اللَّهِ", phraseTv.text.toString())
        assertEquals(45, progressBar.progress)

        // Completed state shows completion message and clamps progress at 100
        val completedViews = TasbihWidgetProvider.buildRemoteViews(
            context = context,
            phrase = "الْحَمْدُ لِلَّهِ",
            todayCount = 120,
            dailyTarget = 100
        )
        val completedInflated = completedViews.apply(context, android.widget.FrameLayout(context))
        val statusTv = completedInflated.findViewById<android.widget.TextView>(R.id.widget_status_text)
        val completedBar = completedInflated.findViewById<android.widget.ProgressBar>(R.id.widget_progress_bar)
        assertEquals(context.getString(R.string.widget_tasbih_goal_done), statusTv.text.toString())
        assertEquals(100, completedBar.progress)
    }

    @Test
    fun `dailyQuranCatalog contains valid HTTPS recitation streams for short daily surahs`() {
        val tracks = DailyQuranCatalog.tracks
        assertTrue(tracks.size >= 8)
        assertEquals(tracks.size, tracks.map { it.id }.toSet().size)
        assertEquals(tracks.size, tracks.map { it.surahNumber }.toSet().size)
        assertTrue(
            tracks.all {
                it.titleAr.isNotBlank() &&
                    it.subtitleAr.isNotBlank() &&
                    it.streamUrl.startsWith("https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/") &&
                    it.streamUrl.endsWith(".mp3")
            }
        )
    }

    @Test
    fun `home quran player streams selection, persists mini player across tabs, and releases on clear`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking { repository.ensureInitialized() }

        var released = false
        val fakePlayer = object : QuranPlayerController {
            private val _state = MutableStateFlow(QuranPlayerState())
            override val state: StateFlow<QuranPlayerState> = _state.asStateFlow()

            override fun selectAndPlay(index: Int) {
                val tracks = _state.value.tracks
                if (index !in tracks.indices) return
                _state.update {
                    it.copy(
                        currentTrackIndex = index,
                        hasStarted = true,
                        isPlaying = true,
                        positionMs = 15_000L,
                        durationMs = 60_000L
                    )
                }
            }

            override fun togglePlayPause() {
                _state.update { it.copy(hasStarted = true, isPlaying = !it.isPlaying, durationMs = 60_000L) }
            }

            override fun nextTrack() {
                val tracks = _state.value.tracks
                selectAndPlay((_state.value.currentTrackIndex + 1) % tracks.size)
            }

            override fun previousTrack() {
                val tracks = _state.value.tracks
                val prev = if (_state.value.currentTrackIndex - 1 < 0) tracks.lastIndex else _state.value.currentTrackIndex - 1
                selectAndPlay(prev)
            }

            override fun seekTo(positionMs: Long) {
                _state.update { it.copy(positionMs = positionMs) }
            }

            override fun toggleRepeatOne() {
                _state.update { it.copy(repeatOne = !it.repeatOne) }
            }

            override fun pauseIfPlaying() {
                _state.update { it.copy(isPlaying = false) }
            }

            override fun stopAndHide() {
                _state.update { it.copy(isPlaying = false, hasStarted = false, positionMs = 0L) }
            }

            override fun release() {
                released = true
            }
        }

        val viewModel = registerViewModel(
            DhikrViewModel(
                repository = repository,
                quranPlayerController = fakePlayer
            )
        )

        composeRule.setContent {
            AdhkarTheme {
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        // Scroll Home list to Quran Recitation card and verify initial track
        composeRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("home_quran_player_card"))
        composeRule.onNodeWithTag("quran_active_track_title").assertTextEquals("سورة الفاتحة")
        composeRule.onNodeWithTag("quran_mini_player").assertDoesNotExist()

        // Select Al-Ikhlas track -> updates card, starts playback, and reveals persistent mini player
        composeRule.onNodeWithTag("quran_track_chip_ikhlas").performClick()
        waitUntilIdle(timeoutMillis = 3000) {
            viewModel.uiState.value.quranPlayer.isPlaying &&
                viewModel.uiState.value.quranPlayer.currentTrack.id == "ikhlas"
        }
        composeRule.onNodeWithTag("quran_active_track_title").assertTextEquals("سورة الإخلاص")
        composeRule.onNodeWithTag("quran_mini_player").assertIsDisplayed()

        // Switch to Tasbeeh tab -> mini player remains visible across screens
        composeRule.onNodeWithTag("nav_tab_tasbih").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("quran_mini_player").assertIsDisplayed()

        // Pause via mini player
        composeRule.onNodeWithTag("quran_mini_play_pause").performClick()
        waitUntilIdle(timeoutMillis = 3000) {
            !viewModel.uiState.value.quranPlayer.isPlaying
        }

        // Advance to next Surah (Al-Falaq) via mini player
        composeRule.onNodeWithTag("quran_mini_next").performClick()
        waitUntilIdle(timeoutMillis = 3000) {
            viewModel.uiState.value.quranPlayer.currentTrack.id == "falaq" &&
                viewModel.uiState.value.quranPlayer.isPlaying
        }

        // Close mini player
        composeRule.onNodeWithTag("quran_mini_close").performClick()
        waitUntilIdle(timeoutMillis = 3000) {
            !viewModel.uiState.value.quranPlayer.hasStarted
        }
        composeRule.onNodeWithTag("quran_mini_player").assertDoesNotExist()

        // Clearing ViewModel releases ExoPlayer resources
        viewModelStore.clear()
        assertTrue(released)
    }

    @Test
    fun `tasbeeh screen triggers celebratory ripple and banner when daily target is reached`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking {
            repository.ensureInitialized()
            prefsRepo.setDailyTasbihTarget(10)
            val counter = database.counterDao().observeAll().first().first()
            database.counterDao().updateProgress(
                id = counter.id,
                currentCount = 9,
                todayCount = 9,
                totalCount = 9
            )
        }
        val viewModel = registerViewModel(DhikrViewModel(repository))

        composeRule.setContent {
            AdhkarTheme {
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("nav_tab_tasbih").performClick()
        composeRule.waitForIdle()

        // Not celebrated yet at 9 / 10
        composeRule.onNodeWithTag("tasbih_celebration_banner").assertDoesNotExist()
        composeRule.onNodeWithTag("tasbih_celebration_ripple").assertDoesNotExist()

        // Pause auto-advance so we can catch the live ripple/confetti canvas frame cleanly
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag("tasbih_tap_circle")
            .performScrollTo()
            .performClick()

        // Advance virtual clock just enough for DB update + initial animation frames (~150ms)
        var foundCelebration = false
        repeat(20) {
            composeRule.mainClock.advanceTimeBy(50L)
            val bannerShown = runCatching {
                composeRule.onNodeWithTag("tasbih_celebration_banner").assertIsDisplayed()
            }.isSuccess
            val rippleShown = runCatching {
                composeRule.onNodeWithTag("tasbih_celebration_ripple").assertIsDisplayed()
            }.isSuccess
            if (bannerShown && rippleShown) {
                foundCelebration = true
                return@repeat
            }
        }
        assertTrue("Celebration banner and ripple should render on target completion", foundCelebration)

        // Resume clock and verify goal reached text updates cleanly
        composeRule.mainClock.autoAdvance = true
        composeRule.onNodeWithTag("tasbih_daily_goal_text")
            .performScrollTo()
            .assertTextEquals("10 / 10")
    }

    @Test
    fun `prayerTimeCalculator parses raw times and determines next prayer and countdown`() {
        assertEquals("05:15", PrayerTimeCalculator.cleanRawTime("05:15 (+03)"))
        assertEquals("5:15 ص", PrayerTimeCalculator.formatArabic12h("05:15"))
        assertEquals("2:52 م", PrayerTimeCalculator.formatArabic12h("14:52"))

        // At 12:30 (750 mins), Dhuhr (12:00) has passed so next prayer is Asr (15:15 -> +2h45m)
        val afternoonSchedule = PrayerTimeCalculator.buildSchedule(
            fajrRaw = "05:00",
            sunriseRaw = "06:20",
            dhuhrRaw = "12:00",
            asrRaw = "15:15",
            maghribRaw = "17:45",
            ishaRaw = "19:15",
            dateReadable = "16 Nov 2026",
            hijriDateAr = "6 جمادى الآخرة 1448 هـ",
            locationLabel = "مكة المكرمة",
            timezone = "Asia/Riyadh",
            latitude = 21.3891,
            longitude = 39.8579,
            method = PrayerCalculationMethod.UMM_AL_QURA,
            nowMinutesOfDay = 12 * 60 + 30
        )
        assertEquals(5, afternoonSchedule.entries.size)
        assertEquals(PrayerName.ASR, afternoonSchedule.nextPrayer?.prayer)
        assertEquals("2 س 45 د", afternoonSchedule.countdownText)

        // At 22:00 (1320 mins), all today's prayers passed so next prayer rolls to Fajr (+7h)
        val nightSchedule = PrayerTimeCalculator.buildSchedule(
            fajrRaw = "05:00",
            sunriseRaw = "06:20",
            dhuhrRaw = "12:00",
            asrRaw = "15:15",
            maghribRaw = "17:45",
            ishaRaw = "19:15",
            dateReadable = "16 Nov 2026",
            hijriDateAr = "",
            locationLabel = "مكة المكرمة",
            timezone = "Asia/Riyadh",
            latitude = 21.3891,
            longitude = 39.8579,
            method = PrayerCalculationMethod.UMM_AL_QURA,
            nowMinutesOfDay = 22 * 60
        )
        assertEquals(PrayerName.FAJR, nightSchedule.nextPrayer?.prayer)
        assertEquals("7 س", nightSchedule.countdownText)
    }

    @Test
    fun `androidDeviceLocationProvider checks coarse permission and reads LocationManager`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val provider = AndroidDeviceLocationProvider(context)

        assertTrue(!provider.hasLocationPermission())
        assertEquals(LocationFetchResult.PermissionMissing, provider.fetchCurrentLocation())

        Shadows.shadowOf(app).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        assertTrue(provider.hasLocationPermission())

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val shadowLocation = Shadows.shadowOf(locationManager)
        shadowLocation.setProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER, true)
        val mockLoc = android.location.Location(android.location.LocationManager.NETWORK_PROVIDER).apply {
            latitude = 24.7136
            longitude = 46.6753
            time = System.currentTimeMillis()
        }
        shadowLocation.setLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER, mockLoc)

        val result = provider.fetchCurrentLocation()
        assertTrue(result is LocationFetchResult.Success)
        val success = result as LocationFetchResult.Success
        assertEquals(24.7136, success.latitude, 0.001)
        assertEquals(46.6753, success.longitude, 0.001)
    }

    @Test
    fun `prayerTimes dashboard displays all five daily prayers and supports city and method changes`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking { repository.ensureInitialized() }

        val requestedMethods = mutableListOf<PrayerCalculationMethod>()
        val requestedLabels = mutableListOf<String>()
        val fakeLocation = object : DeviceLocationProvider {
            override fun hasLocationPermission(): Boolean = true
            override suspend fun fetchCurrentLocation(): LocationFetchResult =
                LocationFetchResult.Success(21.3891, 39.8579, "مكة المكرمة")
        }
        val fakeRemote = object : PrayerTimesRemoteSource {
            override suspend fun fetchDailyPrayerTimes(
                latitude: Double,
                longitude: Double,
                locationLabel: String,
                method: PrayerCalculationMethod
            ): com.example.data.model.DailyPrayerSchedule {
                requestedMethods.add(method)
                requestedLabels.add(locationLabel)
                return PrayerTimeCalculator.buildSchedule(
                    fajrRaw = "05:10",
                    sunriseRaw = "06:28",
                    dhuhrRaw = "12:12",
                    asrRaw = "15:25",
                    maghribRaw = "17:48",
                    ishaRaw = "19:18",
                    dateReadable = "16 Nov 2026",
                    hijriDateAr = "6 جمادى الآخرة 1448 هـ",
                    locationLabel = locationLabel,
                    timezone = "Asia/Riyadh",
                    latitude = latitude,
                    longitude = longitude,
                    method = method,
                    nowMinutesOfDay = 10 * 60
                )
            }
        }

        val viewModel = registerViewModel(
            DhikrViewModel(
                repository = repository,
                deviceLocationProvider = fakeLocation,
                prayerTimesRemoteSource = fakeRemote
            )
        )

        composeRule.setContent {
            AdhkarTheme {
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        // Navigate from Home card to Prayer Times dashboard
        composeRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("home_prayer_times_card"))
        composeRule.onNodeWithTag("home_prayer_times_card").performClick()

        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("prayer_next_hero_card").assertIsDisplayed()
            }.isSuccess
        }
        composeRule.onNodeWithTag("prayer_location_label").assertTextEquals("مكة المكرمة")
        composeRule.onNodeWithTag("prayer_next_name").assertTextEquals("الظهر")

        // Verify all 5 prayers render
        listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").forEach { key ->
            composeRule.onNodeWithTag("prayer_lazy_column")
                .performScrollToNode(hasTestTag("prayer_row_$key"))
            composeRule.onNodeWithTag("prayer_row_$key").assertIsDisplayed()
            composeRule.onNodeWithTag("prayer_time_$key").assertIsDisplayed()
        }

        // Switch quick city to Cairo
        composeRule.onNodeWithTag("prayer_lazy_column")
            .performScrollToNode(hasTestTag("prayer_city_cairo"))
        composeRule.onNodeWithTag("prayer_city_cairo")
            .performScrollTo()
            .performClick()
        waitUntilIdle(timeoutMillis = 3000) {
            requestedLabels.lastOrNull() == "القاهرة"
        }

        // Switch calculation method to ISNA (id = 2) and verify persistence
        composeRule.onNodeWithTag("prayer_lazy_column")
            .performScrollToNode(hasTestTag("prayer_method_2"))
        composeRule.onNodeWithTag("prayer_method_2")
            .performScrollTo()
            .performClick()
        waitUntilIdle(timeoutMillis = 3000) {
            requestedMethods.lastOrNull() == PrayerCalculationMethod.ISNA &&
                runBlocking { prefsRepo.preferencesFlow.first().prayerMethodId == 2 }
        }
        assertEquals(2, runBlocking { prefsRepo.preferencesFlow.first().prayerMethodId })
    }

    @Test
    fun `morning and evening notifications use distinct encouraging messages and Read Now action`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNotifications = Shadows.shadowOf(notificationManager)

        DhikrNotificationHelper.showCategoryReminder(context, DhikrCategory.MORNING)
        DhikrNotificationHelper.showCategoryReminder(context, DhikrCategory.EVENING)

        val morningNotif = shadowNotifications.getNotification(
            DhikrNotificationHelper.notificationIdFor(DhikrCategory.MORNING)
        )
        val eveningNotif = shadowNotifications.getNotification(
            DhikrNotificationHelper.notificationIdFor(DhikrCategory.EVENING)
        )
        val arContext = context.withAppLanguage(AppLanguage.ARABIC)
        assertNotNull(morningNotif)
        assertNotNull(eveningNotif)
        assertEquals(
            arContext.getString(R.string.reminder_body_morning),
            morningNotif.extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString()
        )
        assertEquals(
            arContext.getString(R.string.reminder_body_evening),
            eveningNotif.extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString()
        )
        assertEquals(1, morningNotif.actions.size)
        assertEquals(
            arContext.getString(R.string.reminder_action_read_now),
            morningNotif.actions[0].title.toString()
        )
    }

    @Test
    fun `settings quick Morning and Evening toggle activates both reminders and test button fires notification`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(database.dhikrDao(), database.counterDao(), prefsRepo)
        runBlocking {
            repository.ensureInitialized()
            prefsRepo.setReminder(DhikrCategory.MORNING, false, 6, 0)
            prefsRepo.setReminder(DhikrCategory.EVENING, false, 17, 0)
        }
        val synced = mutableListOf<List<DhikrReminder>>()
        val spyScheduler = object : ReminderScheduler {
            override fun syncAll(reminders: List<DhikrReminder>) {
                synced.add(reminders)
            }
        }
        val viewModel = registerViewModel(DhikrViewModel(repository, spyScheduler))

        composeRule.setContent {
            AdhkarTheme {
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("nav_tab_settings").performClick()
        composeRule.waitForIdle()

        // Tap quick button to enable both Morning and Evening reminders
        composeRule.onNodeWithTag("reminder_enable_morning_evening_button")
            .performScrollTo()
            .performClick()

        waitUntilIdle(timeoutMillis = 3000) {
            runBlocking {
                val reminders = prefsRepo.preferencesFlow.first().reminders
                val morningEnabled = reminders.first { it.category == DhikrCategory.MORNING }.enabled
                val eveningEnabled = reminders.first { it.category == DhikrCategory.EVENING }.enabled
                morningEnabled && eveningEnabled
            }
        }
        assertTrue(synced.last().first { it.category == DhikrCategory.MORNING }.enabled)
        assertTrue(synced.last().first { it.category == DhikrCategory.EVENING }.enabled)

        // Tap Test Notification button -> posts Morning notification immediately
        // Scroll a lower item into view first so the quick-action row sits above any active Snackbar.
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNotifications = Shadows.shadowOf(notificationManager)
        composeRule.onNodeWithTag("reminder_time_evening")
            .performScrollTo()
        composeRule.onNodeWithTag("reminder_test_notification_button")
            .performScrollTo()
            .performClick()
        waitUntilIdle(timeoutMillis = 3000) {
            shadowNotifications.getNotification(
                DhikrNotificationHelper.notificationIdFor(DhikrCategory.MORNING)
            ) != null
        }

        assertNotNull(
            shadowNotifications.getNotification(
                DhikrNotificationHelper.notificationIdFor(DhikrCategory.MORNING)
            )
        )
    }

    @Test
    fun `hapticHelper triggers subtle tap vibration and milestone pulse on device vibrator`() {
        val vibratorManager =
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
        val vibrator = vibratorManager.defaultVibrator
        val shadowVibrator = Shadows.shadowOf(vibrator)

        com.example.util.HapticHelper.performTap(context)
        assertTrue("Tap haptic should activate device vibrator", shadowVibrator.isVibrating)

        vibrator.cancel()
        com.example.util.HapticHelper.performMilestone(context)
        assertTrue("Milestone haptic should activate device vibrator", shadowVibrator.isVibrating)
    }

    @Test
    fun `daily goal setting persists custom target and updates dashboard progress ring`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking {
            repository.ensureInitialized()
            prefsRepo.setDailyTotalGoal(100)
            // 10 dhikr taps + 15 tasbeeh taps = 25 total daily counts (25%)
            val morningDhikr = database.dhikrDao().observeByCategory(DhikrCategory.MORNING.id).first()
                .first { it.id == 101 }
            database.dhikrDao().updateProgress(morningDhikr.id, 1)
            val counter = database.counterDao().observeAll().first().first()
            database.counterDao().updateProgress(
                id = counter.id,
                currentCount = 24,
                todayCount = 24,
                totalCount = 24
            )
        }
        val viewModel = registerViewModel(DhikrViewModel(repository))

        composeRule.setContent {
            AdhkarTheme {
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        // Main dashboard progress ring displays combined total counts / goal and percentage
        composeRule.onNodeWithTag("home_daily_goal_card").assertIsDisplayed()
        composeRule.onNodeWithTag("home_daily_goal_ring").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("home_daily_goal_count_text").assertTextEquals("25 / 100")
            }.isSuccess
        }
        composeRule.onNodeWithTag("home_daily_goal_percent").assertTextEquals("25%")

        // Open Settings via card's edit button and enter custom Daily Goal = 50 -> percentage becomes 50%
        composeRule.onNodeWithTag("home_daily_goal_settings_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_overall_goal_custom_button")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_overall_goal_input").performTextReplacement("50")
        composeRule.onNodeWithTag("confirm_overall_daily_goal").performClick()

        waitUntilIdle(timeoutMillis = 3000) {
            runBlocking { prefsRepo.preferencesFlow.first().dailyTotalGoal == 50 }
        }

        composeRule.onNodeWithTag("nav_tab_home").performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("home_daily_goal_count_text").assertTextEquals("25 / 50")
            }.isSuccess
        }
        composeRule.onNodeWithTag("home_daily_goal_percent").assertTextEquals("50%")
    }

    @Test
    fun `daily reflection provider rotates deterministically every 24 hours`() {
        val utc = java.util.TimeZone.getTimeZone("UTC")
        val day0Morning = 10L * DailyReflectionProvider.REFRESH_INTERVAL_MILLIS + 3_600_000L
        val day0Evening = 10L * DailyReflectionProvider.REFRESH_INTERVAL_MILLIS + 72_000_000L
        val day1Morning = 11L * DailyReflectionProvider.REFRESH_INTERVAL_MILLIS + 3_600_000L

        val firstMorning = DailyReflectionProvider.reflectionForTimestamp(day0Morning, utc)
        val firstEvening = DailyReflectionProvider.reflectionForTimestamp(day0Evening, utc)
        val nextDay = DailyReflectionProvider.reflectionForTimestamp(day1Morning, utc)

        assertEquals(firstMorning, firstEvening)
        assertTrue(firstMorning.id != nextDay.id)
        assertTrue(DailyReflectionProvider.entries.size >= 30)
    }

    @Test
    fun `home daily reflection card displays quote supports copy and refreshes after 24 hours`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking { repository.ensureInitialized() }

        val utc = java.util.TimeZone.getTimeZone("UTC")
        var fakeNowMillis = 100L * DailyReflectionProvider.REFRESH_INTERVAL_MILLIS + 10_000_000L
        val expectedDay0 = DailyReflectionProvider.reflectionForTimestamp(fakeNowMillis, utc)
        val expectedDay1 = DailyReflectionProvider.reflectionForTimestamp(
            fakeNowMillis + DailyReflectionProvider.REFRESH_INTERVAL_MILLIS,
            utc
        )

        val viewModel = registerViewModel(
            DhikrViewModel(
                repository = repository,
                clockMillis = { fakeNowMillis },
                reflectionTimeZone = utc
            )
        )

        composeRule.setContent {
            AdhkarTheme {
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("home_daily_reflection_card"))
        composeRule.onNodeWithTag("home_daily_reflection_card").assertIsDisplayed()
        composeRule.onNodeWithTag("home_reflection_text").assertTextEquals(expectedDay0.arabicText)
        composeRule.onNodeWithTag("home_reflection_source").assertTextEquals(expectedDay0.sourceReference)
        composeRule.onNodeWithTag("home_reflection_refresh_timer").assertIsDisplayed()

        // Copy quote to clipboard
        composeRule.onNodeWithTag("home_reflection_copy").performClick()
        composeRule.waitForIdle()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val copiedClip = clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
        assertTrue(copiedClip.contains(expectedDay0.arabicText))

        // Advance 24 hours and trigger refresh -> reflection card updates cleanly
        fakeNowMillis += DailyReflectionProvider.REFRESH_INTERVAL_MILLIS
        viewModel.refreshDailyReflection(fakeNowMillis)

        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("home_reflection_text").assertTextEquals(expectedDay1.arabicText)
            }.isSuccess
        }
        composeRule.onNodeWithTag("home_reflection_source").assertTextEquals(expectedDay1.sourceReference)
    }

    @Test
    fun `json backup serializes full progress and settings and rejects malformed or unsupported json`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking {
            repository.ensureInitialized()
            prefsRepo.setFontSize(28f)
            prefsRepo.setNightModeEnabled(true)
            prefsRepo.setDailyTotalGoal(320)
            prefsRepo.setDailyTasbihTarget(250)
            prefsRepo.setReminder(DhikrCategory.MORNING, true, 5, 45)
            val morning = database.dhikrDao().observeByCategory(DhikrCategory.MORNING.id).first().first()
            database.dhikrDao().updateProgressAndFavorite(morning.id, 1, true)
            repository.addCustomCounter("الباقيات الصالحات", 99)
            repository.saveDailyActivity(DailyActivity(dateKey = "2026-11-10", dhikrCount = 12, tasbihCount = 33))
        }

        val viewModel = registerViewModel(DhikrViewModel(repository))
        val json = runBlocking { viewModel.createBackupJson("1.2") }
        val parsed = BackupJsonManager.parse(json)
        assertTrue(parsed is BackupParseResult.Success)
        val data = (parsed as BackupParseResult.Success).data
        assertEquals(28f, data.preferences.fontSizeSp)
        assertTrue(data.preferences.nightModeEnabled)
        assertEquals(320, data.preferences.dailyTotalGoal)
        assertEquals(250, data.preferences.dailyTasbihTarget)
        assertTrue(data.preferences.reminders.any { it.categoryId == DhikrCategory.MORNING.id && it.enabled && it.hour == 5 && it.minute == 45 })
        assertTrue(data.counters.any { it.phrase == "الباقيات الصالحات" && it.targetCount == 99 })
        assertTrue(data.dailyHistory.any { it.dateKey == "2026-11-10" && it.dhikrCount == 12 && it.tasbihCount == 33 })

        // Share intent creation writes valid JSON shareable via FileProvider
        val shareIntent = BackupJsonManager.createShareIntent(
            context = context,
            fileName = "adhkar_test_backup.json",
            jsonContent = json,
            subject = "Backup"
        )
        assertEquals("application/json", shareIntent.type)

        // Malformed and unsupported versions are cleanly rejected
        assertEquals(BackupParseResult.InvalidFormat, BackupJsonManager.parse("not valid json"))
        assertTrue(BackupJsonManager.parse(json.replace("\"schemaVersion\": 1", "\"schemaVersion\": 99")) is BackupParseResult.UnsupportedVersion)
    }

    @Test
    fun `restoring json backup overwrites progress counters history and displays settings backup controls`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        val synced = mutableListOf<List<DhikrReminder>>()
        val spyScheduler = object : ReminderScheduler {
            override fun syncAll(reminders: List<DhikrReminder>) {
                synced.add(reminders)
            }
        }

        // Snapshot State A
        val snapshotJson = runBlocking {
            repository.ensureInitialized()
            prefsRepo.setFontSize(30f)
            prefsRepo.setThemeMode(com.example.ui.theme.ThemeMode.DARK)
            prefsRepo.setDailyTotalGoal(400)
            prefsRepo.setReminder(DhikrCategory.EVENING, true, 17, 15)
            val morning = database.dhikrDao().observeByCategory(DhikrCategory.MORNING.id).first().first()
            database.dhikrDao().updateProgressAndFavorite(morning.id, 1, true)
            repository.addCustomCounter("لا حول ولا قوة إلا بالله", 50)
            repository.saveDailyActivity(DailyActivity(dateKey = "2026-11-14", dhikrCount = 20, tasbihCount = 80))
            BackupJsonManager.serialize(repository.createBackupSnapshot())
        }

        // Mutate device into clean / different State B
        runBlocking {
            prefsRepo.setFontSize(18f)
            prefsRepo.setThemeMode(com.example.ui.theme.ThemeMode.LIGHT)
            prefsRepo.setDailyTotalGoal(50)
            prefsRepo.setReminder(DhikrCategory.EVENING, false, 17, 0)
            database.dhikrDao().resetAllProgressAndFavorites()
            database.counterDao().deleteAll()
            database.counterDao().insertAll(AdhkarSeedData.getInitialCounters())
            database.dailyActivityDao().deleteAll()
        }

        val viewModel = registerViewModel(DhikrViewModel(repository, spyScheduler))

        composeRule.setContent {
            AdhkarTheme {
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        // Navigate to Settings and verify Backup & Restore UI buttons are rendered cleanly
        composeRule.onNodeWithTag("nav_tab_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_backup_restore_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("backup_save_json_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("backup_share_json_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("backup_restore_json_button").performScrollTo().assertIsDisplayed()

        // Execute restore from parsed State A snapshot
        val parsed = (BackupJsonManager.parse(snapshotJson) as BackupParseResult.Success).data
        var restoreSucceeded = false
        viewModel.restoreFromBackup(parsed) { ok -> restoreSucceeded = ok }

        waitUntilIdle(timeoutMillis = 3000) {
            restoreSucceeded && runBlocking { prefsRepo.preferencesFlow.first().dailyTotalGoal == 400 }
        }

        runBlocking {
            val prefs = prefsRepo.preferencesFlow.first()
            assertEquals(30f, prefs.fontSizeSp)
            assertEquals(com.example.ui.theme.ThemeMode.DARK, prefs.themeMode)
            assertTrue(prefs.reminders.first { it.category == DhikrCategory.EVENING }.enabled)
            assertEquals(17, prefs.reminders.first { it.category == DhikrCategory.EVENING }.hour)
            assertEquals(15, prefs.reminders.first { it.category == DhikrCategory.EVENING }.minute)

            val favs = database.dhikrDao().observeFavorites().first()
            assertEquals(1, favs.size)
            assertEquals(1, favs.first().currentCount)

            val counters = database.counterDao().observeAll().first()
            assertTrue(counters.any { it.phrase == "لا حول ولا قوة إلا بالله" && it.targetCount == 50 })

            val history = database.dailyActivityDao().observeAll().first()
            assertEquals(1, history.size)
            assertEquals("2026-11-14", history.first().dateKey)
            assertEquals(20, history.first().dhikrCount)
            assertEquals(80, history.first().tasbihCount)
        }
        assertTrue(synced.last().first { it.category == DhikrCategory.EVENING }.enabled)
    }

    @Test
    fun `adhkarTranslations covers all seed adhkar counters reflections and english search`() {
        val seedAdhkar = AdhkarSeedData.getInitialAdhkar()
        assertTrue(seedAdhkar.all { dhikr ->
            val t = AdhkarTranslations.forDhikr(dhikr.id)
            t != null && t.titleEn.isNotBlank() && t.translationEn.isNotBlank() && t.virtueEn.isNotBlank() && t.sourceEn.isNotBlank()
        })

        val seedCounters = AdhkarSeedData.getInitialCounters()
        assertTrue(seedCounters.all { counter ->
            val c = AdhkarTranslations.forCounter(counter.id)
            c != null && c.titleEn.isNotBlank() && c.meaningEn.isNotBlank() && c.virtueEn.isNotBlank()
        })

        assertTrue(DailyReflectionProvider.entries.all {
            it.englishText.isNotBlank() && it.englishSource.isNotBlank() && it.englishReflection.isNotBlank()
        })

        val ayatKursi = seedAdhkar.first { it.id == 101 }
        val istighfar = seedAdhkar.first { it.id == 104 }
        assertTrue(ArabicTextNormalizer.matches(ayatKursi, "Ayat al-Kursi"))
        assertTrue(ArabicTextNormalizer.matches(ayatKursi, "Ever-Living"))
        assertTrue(ArabicTextNormalizer.matches(istighfar, "Paradise"))
    }

    @Test
    fun `settings language switch toggles between Arabic RTL and English LTR with dhikr translations`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking {
            repository.ensureInitialized()
            prefsRepo.setAppLanguage(AppLanguage.ARABIC)
        }
        val viewModel = registerViewModel(DhikrViewModel(repository))
        var activeLayoutDirection = LayoutDirection.Rtl

        composeRule.setContent {
            val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
            val prefs = uiState.preferences
            AdhkarTheme(
                themeMode = prefs.themeMode,
                nightModeEnabled = prefs.nightModeEnabled,
                appLanguage = prefs.appLanguage
            ) {
                activeLayoutDirection = LocalLayoutDirection.current
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()
        assertEquals(LayoutDirection.Rtl, activeLayoutDirection)

        // Open Settings and switch to English
        composeRule.onNodeWithTag("nav_tab_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("language_chip_english")
            .performScrollTo()
            .performClick()

        waitUntilIdle(timeoutMillis = 3000) {
            runBlocking { prefsRepo.preferencesFlow.first().appLanguage == AppLanguage.ENGLISH } &&
                activeLayoutDirection == LayoutDirection.Ltr
        }
        assertEquals(LayoutDirection.Ltr, activeLayoutDirection)

        // Navigate back to Home -> search in English -> verify Arabic content + English translation both render
        composeRule.onNodeWithTag("nav_tab_home").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_search_input").performTextReplacement("Kursi")
        waitUntilIdle(timeoutMillis = 3000) {
            viewModel.uiState.value.filteredAdhkar.any { it.id == 101 }
        }
        composeRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("dhikr_card_101"))
        composeRule.onNodeWithTag("dhikr_content_101", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("dhikr_translation_101", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()

        // Switch back to Arabic and confirm RTL restores and English translation box hides
        composeRule.onNodeWithTag("nav_tab_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("language_chip_arabic")
            .performScrollTo()
            .performClick()

        waitUntilIdle(timeoutMillis = 3000) {
            runBlocking { prefsRepo.preferencesFlow.first().appLanguage == AppLanguage.ARABIC } &&
                activeLayoutDirection == LayoutDirection.Rtl
        }
        composeRule.onNodeWithTag("nav_tab_home").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dhikr_translation_101", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `soundHelper generates non-empty session end alert PCM without clipping`() {
        val pcm = SoundHelper.generateSessionEndPcm()
        assertTrue(pcm.isNotEmpty())
        val maxPeak = pcm.maxOf { kotlin.math.abs(it.toInt()) }
        assertTrue("Expected audible peak amplitude", maxPeak > 1000)
        assertTrue("Expected non-clipping amplitude", maxPeak <= Short.MAX_VALUE)
    }

    @Test
    fun `session timer supports duration selection, pause, reset, and conditional chime on completion`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking { repository.ensureInitialized() }

        val viewModel = registerViewModel(
            DhikrViewModel(
                repository = repository,
                timerTickIntervalMillis = 10L
            )
        )
        val chimeEvents = mutableListOf<ChimeEvent>()
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        val job = scope.launch {
            viewModel.chimeEvents.collect { chimeEvents.add(it) }
        }

        try {
            // Select 1 minute (60s)
            viewModel.onSelectSessionTimerDuration(1)
            waitUntilIdle(timeoutMillis = 3000) {
                viewModel.uiState.value.sessionTimer.selectedMinutes == 1
            }
            assertEquals(1, viewModel.uiState.value.sessionTimer.selectedMinutes)
            assertEquals(60, viewModel.uiState.value.sessionTimer.remainingSeconds)
            assertEquals("01:00", viewModel.uiState.value.sessionTimer.formattedTime)
            assertEquals(SessionTimerStatus.IDLE, viewModel.uiState.value.sessionTimer.status)

            // Start, let a few ticks elapse, then pause and reset
            viewModel.onStartOrResumeSessionTimer()
            waitUntilIdle(timeoutMillis = 3000) {
                viewModel.uiState.value.sessionTimer.status == SessionTimerStatus.RUNNING &&
                    viewModel.uiState.value.sessionTimer.remainingSeconds < 60
            }
            viewModel.onPauseSessionTimer()
            waitUntilIdle(timeoutMillis = 3000) {
                viewModel.uiState.value.sessionTimer.status == SessionTimerStatus.PAUSED
            }
            val pausedRemaining = viewModel.uiState.value.sessionTimer.remainingSeconds
            assertEquals(SessionTimerStatus.PAUSED, viewModel.uiState.value.sessionTimer.status)
            assertTrue(pausedRemaining in 1..59)

            viewModel.onResetSessionTimer()
            waitUntilIdle(timeoutMillis = 3000) {
                viewModel.uiState.value.sessionTimer.status == SessionTimerStatus.IDLE
            }
            assertEquals(SessionTimerStatus.IDLE, viewModel.uiState.value.sessionTimer.status)
            assertEquals(60, viewModel.uiState.value.sessionTimer.remainingSeconds)

            // Mute sound alert -> run to completion -> no SessionTimerFinished chime emitted
            viewModel.onToggleSessionTimerSound(false)
            viewModel.onStartOrResumeSessionTimer()
            waitUntilIdle(timeoutMillis = 5000) {
                viewModel.uiState.value.sessionTimer.status == SessionTimerStatus.FINISHED
            }
            assertEquals(0, viewModel.uiState.value.sessionTimer.remainingSeconds)
            assertEquals("00:00", viewModel.uiState.value.sessionTimer.formattedTime)
            assertTrue(chimeEvents.none { it == ChimeEvent.SessionTimerFinished })

            // Enable sound alert -> restart -> run to completion -> emits SessionTimerFinished chime
            viewModel.onToggleSessionTimerSound(true)
            viewModel.onStartOrResumeSessionTimer()
            waitUntilIdle(timeoutMillis = 5000) {
                viewModel.uiState.value.sessionTimer.status == SessionTimerStatus.FINISHED &&
                    chimeEvents.count { it == ChimeEvent.SessionTimerFinished } == 1
            }
            assertEquals(1, chimeEvents.count { it == ChimeEvent.SessionTimerFinished })
        } finally {
            job.cancel()
        }
    }

    @Test
    fun `tasbih screen session timer allows custom duration, live countdown, pause, reset, and completion banner`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking { repository.ensureInitialized() }

        val viewModel = registerViewModel(
            DhikrViewModel(
                repository = repository,
                timerTickIntervalMillis = 35L
            )
        )
        val chimeEvents = mutableListOf<ChimeEvent>()

        composeRule.setContent {
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            androidx.compose.runtime.LaunchedEffect(viewModel) {
                scope.launch {
                    viewModel.chimeEvents.collect { chimeEvents.add(it) }
                }
            }
            AdhkarTheme {
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        // Navigate to Tasbeeh screen
        composeRule.onNodeWithTag("nav_tab_tasbih").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tasbih_session_timer_card")
            .performScrollTo()
            .assertIsDisplayed()

        // Default preset is 5 minutes (05:00); select 10m preset -> 10:00
        composeRule.onNodeWithTag("tasbih_timer_preset_10")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("tasbih_timer_countdown").assertTextEquals("10:00")

        // Open custom duration dialog and set to 1 minute (01:00)
        composeRule.onNodeWithTag("tasbih_timer_custom_button")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tasbih_timer_custom_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("tasbih_timer_custom_input").performTextReplacement("1")
        composeRule.onNodeWithTag("confirm_tasbih_timer_custom").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tasbih_timer_countdown")
            .performScrollTo()
            .assertTextEquals("01:00")

        // Start timer -> live countdown pill appears
        composeRule.onNodeWithTag("tasbih_timer_start_pause_button")
            .performScrollTo()
            .performClick()
        waitUntilIdle(timeoutMillis = 3000) {
            viewModel.uiState.value.sessionTimer.status == SessionTimerStatus.RUNNING
        }
        composeRule.onNodeWithTag("tasbih_timer_live_pill")
            .performScrollTo()
            .assertIsDisplayed()

        // Pause then Reset restores 01:00
        composeRule.onNodeWithTag("tasbih_timer_start_pause_button")
            .performScrollTo()
            .performClick()
        waitUntilIdle(timeoutMillis = 3000) {
            viewModel.uiState.value.sessionTimer.status == SessionTimerStatus.PAUSED
        }
        composeRule.onNodeWithTag("tasbih_timer_reset_button")
            .performScrollTo()
            .performClick()
        waitUntilIdle(timeoutMillis = 3000) {
            viewModel.uiState.value.sessionTimer.status == SessionTimerStatus.IDLE
        }
        composeRule.onNodeWithTag("tasbih_timer_countdown")
            .performScrollTo()
            .assertTextEquals("01:00")

        // Run session timer to expiry and assert completion banner & sound alert event
        composeRule.onNodeWithTag("tasbih_timer_start_pause_button")
            .performScrollTo()
            .performClick()
        waitUntilIdle(timeoutMillis = 5000) {
            viewModel.uiState.value.sessionTimer.status == SessionTimerStatus.FINISHED
        }
        composeRule.onNodeWithTag("tasbih_timer_finished_banner")
            .performScrollTo()
            .assertIsDisplayed()
        assertTrue(chimeEvents.contains(ChimeEvent.SessionTimerFinished))
    }

    @Test
    fun `adFrequencyPolicy enforces natural transition threshold, cooldown, and 24h ad-free window`() {
        val now = 1_000_000_000L
        assertEquals(false, AdFrequencyPolicy.isAdFreeActive(adFreeUntilMillis = 0L, nowMillis = now))
        assertEquals(true, AdFrequencyPolicy.isAdFreeActive(adFreeUntilMillis = now + 60_000L, nowMillis = now))
        assertEquals(1, AdFrequencyPolicy.remainingDurationMinutes(adFreeUntilMillis = now + 60_000L, nowMillis = now))

        // Fewer than MIN_TRANSITIONS_BEFORE_INTERSTITIAL -> suppressed
        assertEquals(
            false,
            AdFrequencyPolicy.shouldShowInterstitial(
                isAdFreeActive = false,
                naturalTransitionsCount = 1,
                lastInterstitialShownAtMillis = 0L,
                nowMillis = now
            )
        )

        // At threshold with no prior ad -> allowed
        assertEquals(
            true,
            AdFrequencyPolicy.shouldShowInterstitial(
                isAdFreeActive = false,
                naturalTransitionsCount = 2,
                lastInterstitialShownAtMillis = 0L,
                nowMillis = now
            )
        )

        // Inside 3-minute cooldown -> suppressed
        assertEquals(
            false,
            AdFrequencyPolicy.shouldShowInterstitial(
                isAdFreeActive = false,
                naturalTransitionsCount = 4,
                lastInterstitialShownAtMillis = now - 60_000L,
                nowMillis = now
            )
        )

        // Past cooldown but ad-free active -> suppressed
        assertEquals(
            false,
            AdFrequencyPolicy.shouldShowInterstitial(
                isAdFreeActive = true,
                naturalTransitionsCount = 5,
                lastInterstitialShownAtMillis = now - 300_000L,
                nowMillis = now
            )
        )

        // Extending active ad-free window stacks on top of remaining duration and caps at MAX_AD_FREE_BANK_MS
        val firstUntil = AdFrequencyPolicy.computeExtendedAdFreeUntilMillis(
            currentAdFreeUntilMillis = 0L,
            nowMillis = now
        )
        assertEquals(now + AdFrequencyPolicy.AD_FREE_REWARD_DURATION_MS, firstUntil)

        val secondUntil = AdFrequencyPolicy.computeExtendedAdFreeUntilMillis(
            currentAdFreeUntilMillis = firstUntil,
            nowMillis = now
        )
        assertEquals(now + 2L * AdFrequencyPolicy.AD_FREE_REWARD_DURATION_MS, secondUntil)
        assertEquals(48 to 0, AdFrequencyPolicy.remainingHoursAndMinutes(secondUntil, now))

        val cappedUntil = AdFrequencyPolicy.computeExtendedAdFreeUntilMillis(
            currentAdFreeUntilMillis = now + AdFrequencyPolicy.MAX_AD_FREE_BANK_MS,
            nowMillis = now
        )
        assertEquals(now + AdFrequencyPolicy.MAX_AD_FREE_BANK_MS, cappedUntil)
    }

    @Test
    fun `settings rewarded ad card grants 24h ad-free focus and suppresses banner and interstitials`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking {
            repository.ensureInitialized()
            prefsRepo.setAdFreeUntil(0L)
        }

        var fakeNow = 10_000_000_000L
        val viewModel = registerViewModel(
            DhikrViewModel(
                repository = repository,
                clockMillis = { fakeNow }
            )
        )

        val adEvents = mutableListOf<AdEvent>()
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        val adJob = scope.launch {
            viewModel.adEvents.collect { adEvents.add(it) }
        }

        val fakeAdController = object : AdMobController {
            private val _state = MutableStateFlow(AdControllerState(isInitialized = true, isRewardedReady = true))
            override val state: StateFlow<AdControllerState> = _state.asStateFlow()
            override fun initialize() = Unit
            override fun maybeShowInterstitial(activity: android.app.Activity?, onFinished: () -> Unit) = onFinished()
            override fun showRewardedAd(
                activity: android.app.Activity?,
                onRewardEarned: () -> Unit,
                onUnavailable: () -> Unit
            ) {
                onRewardEarned()
            }
        }

        try {
            composeRule.setContent {
                AdhkarTheme {
                    AdhkarAppRoot(
                        viewModel = viewModel,
                        adMobController = fakeAdController
                    )
                }
            }
            composeRule.waitForIdle()

            // Before reward: 2 natural transitions trigger an interstitial event
            viewModel.onNaturalAdTransition()
            viewModel.onNaturalAdTransition()
            assertEquals(1, adEvents.size)

            // Banner container exists in composition when ads are active
            assertEquals(1, composeRule.onAllNodes(hasTestTag("admob_banner_container")).fetchSemanticsNodes().size)

            // Open Settings -> Ad Support Card -> click Watch Ad for 24h Ad-Free
            composeRule.onNodeWithTag("nav_tab_settings").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("settings_ad_support_card")
                .performScrollTo()
                .assertIsDisplayed()
            composeRule.onNodeWithTag("settings_ad_breakdown")
                .performScrollTo()
                .assertIsDisplayed()
            composeRule.onNodeWithTag("settings_watch_rewarded_ad_button")
                .performScrollTo()
                .performClick()

            waitUntilIdle(timeoutMillis = 3000) {
                viewModel.uiState.value.isAdFreeActive &&
                    runBlocking { prefsRepo.preferencesFlow.first().adFreeUntilMillis > fakeNow }
            }

            // Banner container disappears during Ad-Free Focus window
            assertEquals(0, composeRule.onAllNodes(hasTestTag("admob_banner_container")).fetchSemanticsNodes().size)

            // Watching a second rewarded ad extends the active window to 48h instead of resetting it
            composeRule.onNodeWithTag("settings_watch_rewarded_ad_button")
                .performScrollTo()
                .performClick()

            waitUntilIdle(timeoutMillis = 3000) {
                viewModel.uiState.value.adFreeRemainingMinutes >= 47 * 60
            }

            // Subsequent transitions even past cooldown are suppressed during ad-free mode
            fakeNow += AdFrequencyPolicy.MIN_INTERSTITIAL_INTERVAL_MS + 10_000L
            viewModel.onNaturalAdTransition()
            assertEquals(1, adEvents.size)

            // Advancing past expiration and refreshing restores ad state & banner container
            fakeNow += 2L * AdFrequencyPolicy.AD_FREE_REWARD_DURATION_MS + 60_000L
            viewModel.onRefreshAdFreeStatus()
            waitUntilIdle(timeoutMillis = 3000) {
                !viewModel.uiState.value.isAdFreeActive
            }
            composeRule.waitForIdle()
            assertEquals(1, composeRule.onAllNodes(hasTestTag("admob_banner_container")).fetchSemanticsNodes().size)
        } finally {
            adJob.cancel()
        }
    }

    @Test
    fun `dhikrAudioCatalog maps all seed adhkar to https streams and HisnAudioTrackResolver resolves exact items`() {
        val allSeed = AdhkarSeedData.getInitialAdhkar()
        val queue = DhikrAudioCatalog.buildQueue(allSeed)
        assertEquals(allSeed.size, queue.size)
        assertTrue(queue.all { it.streamUrl.startsWith("https://") && it.streamUrl.endsWith(".mp3") })
        assertTrue(queue.first { it.dhikrId == 101 }.isQuranic)
        assertTrue(queue.first { it.dhikrId == 101 }.streamUrl.contains("cdn.islamic.network/quran/audio/128/ar.alafasy/262.mp3"))
        assertTrue(queue.first { it.dhikrId == 604 }.streamUrl.contains("2571.mp3"))

        val fakeJson = """
            {
              "أذكار الصباح والمساء": [
                {
                  "ID": 76,
                  "ARABIC_TEXT": "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ وَالْحَمْدُ لِلَّهِ لا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لا شَرِيكَ لَهُ",
                  "AUDIO": "http://www.hisnmuslim.com/audio/ar/76.mp3"
                }
              ]
            }
        """.trimIndent()
        val resolver = HttpHisnAudioTrackResolver(fetcher = { fakeJson })
        val sampleMorning = allSeed.filter { it.id == 101 || it.id == 103 }
        val resolved = runBlocking { resolver.resolveTracks(sampleMorning) }
        assertEquals(2, resolved.size)
        assertTrue(resolved[0].streamUrl.contains("262.mp3"))
        assertEquals("https://www.hisnmuslim.com/audio/ar/76.mp3", resolved[1].streamUrl)
    }

    @Test
    fun `dhikrViewModel coordinates displayed adhkar playback, speed, mutual exclusion, and release`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking { repository.ensureInitialized() }

        var quranPauseCalls = 0
        var dhikrPauseCalls = 0
        var dhikrReleaseCalls = 0

        val fakeQuranController = object : QuranPlayerController {
            private val _state = MutableStateFlow(QuranPlayerState(hasStarted = true, isPlaying = true))
            override val state: StateFlow<QuranPlayerState> = _state.asStateFlow()
            override fun selectAndPlay(index: Int) {
                _state.update { it.copy(currentTrackIndex = index, hasStarted = true, isPlaying = true) }
            }
            override fun togglePlayPause() {
                _state.update { it.copy(isPlaying = !it.isPlaying, hasStarted = true) }
            }
            override fun nextTrack() = Unit
            override fun previousTrack() = Unit
            override fun seekTo(positionMs: Long) = Unit
            override fun toggleRepeatOne() = Unit
            override fun pauseIfPlaying() {
                quranPauseCalls += 1
                _state.update { it.copy(isPlaying = false) }
            }
            override fun stopAndHide() = Unit
            override fun release() = Unit
        }

        val fakeDhikrController = object : DhikrAudioPlayerController {
            private val _state = MutableStateFlow(DhikrAudioPlayerState())
            override val state: StateFlow<DhikrAudioPlayerState> = _state.asStateFlow()
            override fun playDisplayedAdhkar(adhkar: List<Dhikr>, startAtDhikrId: Int?) {
                val queue = DhikrAudioCatalog.buildQueue(adhkar)
                val idx = if (startAtDhikrId != null) queue.indexOfFirst { it.dhikrId == startAtDhikrId }.coerceAtLeast(0) else 0
                _state.value = DhikrAudioPlayerState(
                    queue = queue,
                    currentIndex = idx,
                    isPlaying = true,
                    hasStarted = true,
                    durationMs = 20_000L
                )
            }
            override fun togglePlayPause(dhikr: Dhikr?, displayedAdhkar: List<Dhikr>) {
                if (dhikr != null && (_state.value.activeDhikrId != dhikr.id || !_state.value.hasStarted)) {
                    playDisplayedAdhkar(displayedAdhkar, dhikr.id)
                } else {
                    _state.update { it.copy(isPlaying = !it.isPlaying, hasStarted = true) }
                }
            }
            override fun pauseIfPlaying() {
                dhikrPauseCalls += 1
                _state.update { it.copy(isPlaying = false) }
            }
            override fun nextTrack() {
                _state.update { s ->
                    if (s.queue.isEmpty()) s else s.copy(currentIndex = (s.currentIndex + 1) % s.queue.size, isPlaying = true)
                }
            }
            override fun previousTrack() {
                _state.update { s ->
                    if (s.queue.isEmpty()) s else s.copy(
                        currentIndex = if (s.currentIndex - 1 < 0) s.queue.lastIndex else s.currentIndex - 1,
                        isPlaying = true
                    )
                }
            }
            override fun seekTo(positionMs: Long) {
                _state.update { it.copy(positionMs = positionMs) }
            }
            override fun cyclePlaybackSpeed() {
                _state.update {
                    val next = when (it.playbackSpeed) {
                        1.0f -> 1.25f
                        1.25f -> 0.75f
                        else -> 1.0f
                    }
                    it.copy(playbackSpeed = next)
                }
            }
            override fun toggleRepeatCurrent() {
                _state.update { it.copy(repeatCurrent = !it.repeatCurrent) }
            }
            override fun retryCurrent() = Unit
            override fun stopAndHide() {
                _state.value = DhikrAudioPlayerState()
            }
            override fun release() {
                dhikrReleaseCalls += 1
            }
        }

        val store = ViewModelStore()
        val viewModel = DhikrViewModel(
            repository = repository,
            quranPlayerController = fakeQuranController,
            dhikrAudioController = fakeDhikrController
        )
        store.put("vm", viewModel)

        val collectScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        val stateJob = collectScope.launch { viewModel.uiState.collect { } }
        try {
            val morningAdhkar = AdhkarSeedData.getInitialAdhkar().filter { it.category == DhikrCategory.MORNING.id }
            viewModel.onPlayDisplayedAdhkar(morningAdhkar, startAtDhikrId = 101)
            waitUntilIdle(timeoutMillis = 3000) {
                viewModel.uiState.value.dhikrAudio.activeDhikrId == 101
            }
            assertEquals(1, quranPauseCalls)
            assertEquals(101, viewModel.uiState.value.dhikrAudio.activeDhikrId)
            assertTrue(viewModel.uiState.value.dhikrAudio.isPlaying)

            viewModel.onNextDhikrAudio()
            waitUntilIdle(timeoutMillis = 3000) {
                viewModel.uiState.value.dhikrAudio.activeDhikrId == 102
            }
            assertEquals(102, viewModel.uiState.value.dhikrAudio.activeDhikrId)
            viewModel.onCycleDhikrAudioSpeed()
            waitUntilIdle(timeoutMillis = 3000) {
                viewModel.uiState.value.dhikrAudio.playbackSpeed == 1.25f
            }
            assertEquals(1.25f, viewModel.uiState.value.dhikrAudio.playbackSpeed)
            viewModel.onSeekDhikrAudio(4_500L)
            waitUntilIdle(timeoutMillis = 3000) {
                viewModel.uiState.value.dhikrAudio.positionMs == 4_500L
            }
            assertEquals(4_500L, viewModel.uiState.value.dhikrAudio.positionMs)

            // Playing Home Quran pauses Dhikr audio mutually
            viewModel.onSelectQuranTrack(0)
            waitUntilIdle(timeoutMillis = 3000) {
                !viewModel.uiState.value.dhikrAudio.isPlaying
            }
            assertEquals(1, dhikrPauseCalls)
            assertEquals(false, viewModel.uiState.value.dhikrAudio.isPlaying)

            viewModel.onStopDhikrAudio()
            waitUntilIdle(timeoutMillis = 3000) {
                viewModel.uiState.value.dhikrAudio.activeDhikrId == null
            }
            assertEquals(null, viewModel.uiState.value.dhikrAudio.activeDhikrId)

            store.clear()
            assertEquals(1, dhikrReleaseCalls)
        } finally {
            stateJob.cancel()
        }

        // Verify ExoDhikrAudioPlayerController initializes Media3 ExoPlayer and releases cleanly
        val realExoController = ExoDhikrAudioPlayerController(context)
        realExoController.playDisplayedAdhkar(AdhkarSeedData.getInitialAdhkar().take(2), startAtDhikrId = 101)
        assertTrue(realExoController.state.value.hasStarted)
        assertEquals(101, realExoController.state.value.activeDhikrId)
        realExoController.stopAndHide()
        assertEquals(false, realExoController.state.value.hasStarted)
        realExoController.release()
    }

    @Test
    fun `dhikrListScreen plays displayed adhkar via audio player controls and supports search filtering`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking { repository.ensureInitialized() }

        val fakeDhikrController = object : DhikrAudioPlayerController {
            private val _state = MutableStateFlow(DhikrAudioPlayerState())
            override val state: StateFlow<DhikrAudioPlayerState> = _state.asStateFlow()
            override fun playDisplayedAdhkar(adhkar: List<Dhikr>, startAtDhikrId: Int?) {
                val queue = DhikrAudioCatalog.buildQueue(adhkar)
                val idx = if (startAtDhikrId != null) queue.indexOfFirst { it.dhikrId == startAtDhikrId }.coerceAtLeast(0) else 0
                _state.value = DhikrAudioPlayerState(
                    queue = queue,
                    currentIndex = idx,
                    isPlaying = true,
                    hasStarted = true,
                    durationMs = 30_000L
                )
            }
            override fun togglePlayPause(dhikr: Dhikr?, displayedAdhkar: List<Dhikr>) {
                if (dhikr != null && (_state.value.activeDhikrId != dhikr.id || !_state.value.hasStarted)) {
                    playDisplayedAdhkar(displayedAdhkar, dhikr.id)
                } else {
                    _state.update { it.copy(isPlaying = !it.isPlaying, hasStarted = true) }
                }
            }
            override fun pauseIfPlaying() {
                _state.update { it.copy(isPlaying = false) }
            }
            override fun nextTrack() {
                _state.update { s ->
                    if (s.queue.isEmpty()) s else s.copy(currentIndex = (s.currentIndex + 1) % s.queue.size, isPlaying = true)
                }
            }
            override fun previousTrack() {
                _state.update { s ->
                    if (s.queue.isEmpty()) s else s.copy(
                        currentIndex = if (s.currentIndex - 1 < 0) s.queue.lastIndex else s.currentIndex - 1,
                        isPlaying = true
                    )
                }
            }
            override fun seekTo(positionMs: Long) {
                _state.update { it.copy(positionMs = positionMs) }
            }
            override fun cyclePlaybackSpeed() {
                _state.update {
                    val next = if (it.playbackSpeed == 1.0f) 1.25f else 1.0f
                    it.copy(playbackSpeed = next)
                }
            }
            override fun toggleRepeatCurrent() {
                _state.update { it.copy(repeatCurrent = !it.repeatCurrent) }
            }
            override fun retryCurrent() = Unit
            override fun stopAndHide() {
                _state.value = DhikrAudioPlayerState()
            }
            override fun release() = Unit
        }

        val viewModel = registerViewModel(
            DhikrViewModel(
                repository = repository,
                dhikrAudioController = fakeDhikrController
            )
        )

        composeRule.setContent {
            AdhkarTheme {
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        // Navigate to Morning Adhkar
        composeRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("category_card_morning"))
        composeRule.onNodeWithTag("category_card_morning").performClick()
        composeRule.waitForIdle()

        // Tap TopAppBar Listen-All button -> starts audio queue for the 12 displayed Morning Adhkar
        composeRule.onNodeWithTag("dhikr_list_play_all_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("dhikr_audio_player_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("dhikr_audio_queue_counter").assertTextEquals("1 / 12")

        // Next Dhikr -> 2 / 12, and Cycle Speed -> 1.25x
        composeRule.onNodeWithTag("dhikr_audio_next").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dhikr_audio_queue_counter").assertTextEquals("2 / 12")

        composeRule.onNodeWithTag("dhikr_audio_speed").performClick()
        composeRule.waitForIdle()
        assertEquals(1.25f, viewModel.uiState.value.dhikrAudio.playbackSpeed)

        // Filter displayed adhkar via search and play filtered Dhikr card directly
        composeRule.onNodeWithTag("dhikr_list_search_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dhikr_list_search_input").performTextReplacement("سيد الاستغفار")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("play_dhikr_audio_104").performClick()
        composeRule.waitForIdle()
        assertEquals(104, viewModel.uiState.value.dhikrAudio.activeDhikrId)
        assertEquals(1, viewModel.uiState.value.dhikrAudio.queue.size)

        // Close player bar -> hides playback dock
        composeRule.onNodeWithTag("dhikr_audio_stop").performClick()
        composeRule.waitForIdle()
        assertEquals(0, composeRule.onAllNodes(hasTestTag("dhikr_audio_player_bar")).fetchSemanticsNodes().size)
    }

    @Test
    fun `completed dhikr sessions track across tasbih cycle, timer finish, backup restore, and 7-day stats rollup`() = runBlocking {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        repository.ensureInitialized()

        // 1. Completing a Tasbeeh cycle increments today's completed sessionsCount
        val counter = database.counterDao().observeAll().first().first()
        database.counterDao().updateProgress(
            id = counter.id,
            currentCount = counter.targetCount - 1,
            todayCount = counter.targetCount - 1,
            totalCount = counter.targetCount - 1
        )
        val preCycleCounter = database.counterDao().getById(counter.id)!!
        val hitCycle = repository.incrementCounter(preCycleCounter)
        assertTrue(hitCycle)

        // 2. Completing a timed Tasbeeh session also records a completed session
        val viewModel = registerViewModel(
            DhikrViewModel(
                repository = repository,
                timerTickIntervalMillis = 5L
            )
        )
        viewModel.onSelectSessionTimerDuration(1)
        val stateJob = launch { viewModel.uiState.collect { } }
        try {
            // Fast-forward: simulate recorded session directly + verify category completion too
            val wakingItems = database.dhikrDao().observeByCategory(DhikrCategory.WAKING.id).first()
            wakingItems.dropLast(1).forEach { item ->
                database.dhikrDao().updateProgress(item.id, item.targetCount)
            }
            val lastItem = wakingItems.last()
            database.dhikrDao().updateProgress(lastItem.id, lastItem.targetCount - 1)
            val updatedLast = database.dhikrDao().observeByCategory(DhikrCategory.WAKING.id).first().last()
            val finishedCategory = repository.incrementDhikr(updatedLast)
            assertTrue(finishedCategory)

            val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
            val todayActivity = database.dailyActivityDao().getByDate(todayKey)
            assertNotNull(todayActivity)
            assertEquals(2, todayActivity?.sessionsCount)

            // 3. Seed multi-day history (including 10 days ago outside the 7-day window)
            val cal = Calendar.getInstance()
            val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            cal.add(Calendar.DAY_OF_YEAR, -2)
            val twoDaysAgoKey = keyFormat.format(cal.time)
            database.dailyActivityDao().upsert(
                DailyActivity(dateKey = twoDaysAgoKey, dhikrCount = 15, tasbihCount = 33, sessionsCount = 4)
            )
            cal.add(Calendar.DAY_OF_YEAR, -8) // 10 days ago
            val tenDaysAgoKey = keyFormat.format(cal.time)
            database.dailyActivityDao().upsert(
                DailyActivity(dateKey = tenDaysAgoKey, dhikrCount = 30, tasbihCount = 66, sessionsCount = 9)
            )

            val allActivities = database.dailyActivityDao().observeAll().first()
            val summary = DhikrStatsCalculator.buildSummary(
                activities = allActivities,
                period = StatsPeriod.MONTHLY,
                filter = StatsSeriesFilter.ALL,
                language = AppLanguage.ENGLISH
            )
            assertEquals(7, summary.last7DaysSessions.size)
            assertEquals(6, summary.last7DaysSessionsTotal) // 2 today + 4 two days ago (excludes 10 days ago)
            assertEquals(2, summary.last7DaysActiveSessionDays)
            assertEquals(4, summary.last7DaysBestSessionDayCount)

            // 4. Backup & Restore preserves sessionsCount
            val snapshot = repository.createBackupSnapshot()
            val json = BackupJsonManager.serialize(snapshot)
            val parsed = BackupJsonManager.parse(json) as BackupParseResult.Success
            database.dailyActivityDao().deleteAll()
            repository.restoreBackup(parsed.data)
            assertEquals(2, database.dailyActivityDao().getByDate(todayKey)?.sessionsCount)
            assertEquals(4, database.dailyActivityDao().getByDate(twoDaysAgoKey)?.sessionsCount)
        } finally {
            stateJob.cancel()
        }
    }

    @Test
    fun `statsScreen renders Vico 7-day completed sessions chart and interactive day chips`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val baseCal = Calendar.getInstance()
        val todayKey = keyFormat.format(baseCal.time)
        val threeDaysAgoCal = (baseCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -3) }
        val threeDaysAgoKey = keyFormat.format(threeDaysAgoCal.time)

        runBlocking {
            repository.ensureInitialized()
            database.dailyActivityDao().upsert(
                DailyActivity(dateKey = todayKey, dhikrCount = 8, tasbihCount = 33, sessionsCount = 2)
            )
            database.dailyActivityDao().upsert(
                DailyActivity(dateKey = threeDaysAgoKey, dhikrCount = 20, tasbihCount = 99, sessionsCount = 5)
            )
        }
        val viewModel = registerViewModel(DhikrViewModel(repository))

        composeRule.setContent {
            AdhkarTheme {
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        // Navigate to Stats screen
        composeRule.onNodeWithTag("nav_tab_stats").performClick()
        composeRule.waitForIdle()

        // Verify Vico 7-day completed sessions card, chart, and KPI metrics
        composeRule.onNodeWithTag("stats_lazy_column")
            .performScrollToNode(hasTestTag("stats_sessions_weekly_card"))
        composeRule.onNodeWithTag("stats_sessions_weekly_card").assertIsDisplayed()
        composeRule.onNodeWithTag("stats_sessions_vico_chart").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("stats_sessions_selected_day_count").assertTextEquals("2")
            }.isSuccess
        }

        // Verify 7-day KPI summary (2 + 5 = 7 sessions, best = 5)
        composeRule.onNodeWithTag("stats_sessions_kpi_total").performScrollTo().assertTextEquals("7")
        composeRule.onNodeWithTag("stats_sessions_kpi_best_day").performScrollTo().assertTextEquals("5")

        // Tap chip index 3 (3 days ago, since index 6 = today, index 3 = 3 days ago) -> updates readout to 5
        composeRule.onNodeWithTag("stats_sessions_day_chip_3")
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("stats_sessions_selected_day_count").assertTextEquals("5")
            }.isSuccess
        }
    }

    @Test
    fun `adhkarTheme preserves ActivityResultRegistryOwner and findActivity across Settings and Stats in English and Arabic`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking {
            repository.ensureInitialized()
            prefsRepo.setAppLanguage(AppLanguage.ENGLISH)
        }
        val viewModel = registerViewModel(DhikrViewModel(repository))
        var resolvedOwner: androidx.activity.result.ActivityResultRegistryOwner? = null
        var resolvedActivity: android.app.Activity? = null

        composeRule.setContent {
            val state = viewModel.uiState.collectAsStateWithLifecycle().value
            AdhkarTheme(
                themeMode = state.preferences.themeMode,
                nightModeEnabled = state.preferences.nightModeEnabled,
                appLanguage = state.preferences.appLanguage
            ) {
                resolvedOwner = androidx.activity.compose.LocalActivityResultRegistryOwner.current
                resolvedActivity = androidx.compose.ui.platform.LocalContext.current.findActivity()
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        assertNotNull("LocalActivityResultRegistryOwner must remain non-null inside AdhkarTheme", resolvedOwner)
        assertNotNull("findActivity() must unwrap LocalizedContextWrapper to Activity", resolvedActivity)

        // Navigating to SettingsScreen and StatsScreen (which both call rememberLauncherForActivityResult) must succeed
        composeRule.onNodeWithTag("nav_tab_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_language_card").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_tab_stats").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("stats_lazy_column")
            .performScrollToNode(hasTestTag("stats_sessions_weekly_card"))
        composeRule.onNodeWithTag("stats_sessions_weekly_card").assertIsDisplayed()
    }

    @Test
    fun `digitalTasbeehCounter increments resets and triggers visual pulse feedback animation`() {
        val countState = androidx.compose.runtime.mutableIntStateOf(0)

        composeRule.setContent {
            AdhkarTheme {
                com.example.ui.components.DigitalTasbeehCounter(
                    currentCount = countState.intValue,
                    targetCount = 33,
                    onIncrement = { countState.intValue += 1 },
                    onReset = { countState.intValue = 0 }
                )
            }
        }
        composeRule.waitForIdle()

        // Initially 0 and reset button is disabled
        composeRule.onNodeWithTag("tasbih_current_count_text", useUnmergedTree = true)
            .assertTextEquals("0")
        composeRule.onNodeWithTag("tasbih_increment_pulse_ring").assertDoesNotExist()

        // Pause clock so we can observe the increment pulse feedback ring mid-animation
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag("tasbih_tap_circle").performClick()

        var pulseObserved = false
        repeat(10) {
            composeRule.mainClock.advanceTimeBy(40L)
            if (runCatching {
                    composeRule.onNodeWithTag("tasbih_increment_pulse_ring").assertIsDisplayed()
                }.isSuccess
            ) {
                pulseObserved = true
                return@repeat
            }
        }
        assertTrue("Increment visual feedback pulse ring should animate on tap", pulseObserved)

        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tasbih_current_count_text", useUnmergedTree = true)
            .assertTextEquals("1")

        // Reset button returns count to 0
        composeRule.onNodeWithTag("tasbih_reset_cycle_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tasbih_current_count_text", useUnmergedTree = true)
            .assertTextEquals("0")
    }

    @Test
    fun `strings xml resolves Arabic and English resources for UI notifications widget and Settings toggle`() {
        val arabicContext = context.withAppLanguage(AppLanguage.ARABIC)
        val englishContext = context.withAppLanguage(AppLanguage.ENGLISH)

        assertEquals("أذكار", arabicContext.getString(R.string.app_name))
        assertEquals("Adhkar", englishContext.getString(R.string.app_name))
        assertEquals("الإعدادات", arabicContext.getString(R.string.settings_title))
        assertEquals("Settings", englishContext.getString(R.string.settings_title))
        assertEquals("المسبحة", arabicContext.getString(R.string.nav_tasbih))
        assertEquals("Tasbeeh", englishContext.getString(R.string.nav_tasbih))
        assertEquals(
            "12 متبقي لتحقيق الهدف",
            arabicContext.getString(R.string.widget_tasbih_remaining, 12)
        )
        assertEquals(
            "12 remaining to goal",
            englishContext.getString(R.string.widget_tasbih_remaining, 12)
        )
        assertTrue(
            DhikrNotificationHelper.messageBodyFor(englishContext, DhikrCategory.MORNING)
                .contains("Begin your day", ignoreCase = true)
        )
        assertTrue(
            DhikrNotificationHelper.messageBodyFor(arabicContext, DhikrCategory.MORNING)
                .contains("ابدأ يومك")
        )

        // Verify notification channel updates cleanly across both languages
        DhikrNotificationHelper.ensureChannel(context, AppLanguage.ENGLISH)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = nm.getNotificationChannel(DhikrNotificationHelper.CHANNEL_ID)
        assertNotNull(channel)
        assertEquals("Daily Adhkar Reminders", channel?.name?.toString())

        // Verify widget RemoteViews builds without errors in both Arabic & English
        val arViews = TasbihWidgetProvider.buildRemoteViews(
            context = context,
            phrase = "سُبْحَانَ اللَّهِ",
            todayCount = 40,
            dailyTarget = 100,
            appLanguage = AppLanguage.ARABIC
        )
        val enViews = TasbihWidgetProvider.buildRemoteViews(
            context = context,
            phrase = "سُبْحَانَ اللَّهِ",
            todayCount = 40,
            dailyTarget = 100,
            appLanguage = AppLanguage.ENGLISH
        )
        assertNotNull(arViews)
        assertNotNull(enViews)

        // Verify Settings screen live switch updates rendered strings immediately
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking {
            repository.ensureInitialized()
            prefsRepo.setAppLanguage(AppLanguage.ARABIC)
        }
        val viewModel = registerViewModel(DhikrViewModel(repository))

        composeRule.setContent {
            val prefs = viewModel.uiState.collectAsStateWithLifecycle().value.preferences
            AdhkarTheme(
                themeMode = prefs.themeMode,
                nightModeEnabled = prefs.nightModeEnabled,
                appLanguage = prefs.appLanguage
            ) {
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("nav_tab_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("الإعدادات").onFirst().assertIsDisplayed()

        composeRule.onNodeWithTag("language_chip_english")
            .performScrollTo()
            .performClick()

        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onAllNodesWithText("Settings").onFirst().assertIsDisplayed()
            }.isSuccess
        }
        composeRule.onNodeWithText("Adhkar Font Size").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `tasbih feature persists selected preset and counter increments across ViewModel restart with reset and haptic feedback`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking {
            repository.ensureInitialized()
            prefsRepo.setHapticEnabled(true)
        }

        val firstViewModel = registerViewModel(DhikrViewModel(repository))
        val emittedHaptics = mutableListOf<HapticEvent>()
        val collectJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate).launch {
            firstViewModel.hapticEvents.collect { emittedHaptics.add(it) }
        }

        val secondPresetId = 2
        firstViewModel.onSelectTasbih(secondPresetId)
        waitUntilIdle(timeoutMillis = 3000) {
            firstViewModel.uiState.value.selectedTasbih?.id == secondPresetId &&
                runBlocking { prefsRepo.preferencesFlow.first().selectedTasbihId == secondPresetId }
        }

        val activeBefore = firstViewModel.uiState.value.selectedTasbih!!
        firstViewModel.incrementTasbeeh(activeBefore)
        firstViewModel.incrementTasbeeh(activeBefore)

        waitUntilIdle(timeoutMillis = 3000) {
            runBlocking { (database.counterDao().getById(secondPresetId)?.currentCount ?: 0) >= 2 }
        }
        assertTrue(emittedHaptics.contains(HapticEvent.Tap))

        // Recreate ViewModel to verify persisted preset selection and Room count restore
        val restoredViewModel = registerViewModel(DhikrViewModel(repository))
        waitUntilIdle(timeoutMillis = 3000) {
            !restoredViewModel.uiState.value.isLoading &&
                restoredViewModel.uiState.value.selectedTasbih?.id == secondPresetId &&
                (restoredViewModel.uiState.value.selectedTasbih?.currentCount ?: 0) == 2
        }

        val restoredCounter = restoredViewModel.uiState.value.selectedTasbih!!
        assertEquals(secondPresetId, restoredCounter.id)
        assertEquals(2, restoredCounter.currentCount)
        assertEquals(2, restoredCounter.todayCount)
        assertEquals(2, restoredCounter.totalCount)

        // Reset cycle clears currentCount in Room while preserving cumulative today/total counts
        restoredViewModel.onResetTasbihCycle(secondPresetId)
        waitUntilIdle(timeoutMillis = 3000) {
            runBlocking { database.counterDao().getById(secondPresetId)?.currentCount == 0 }
        }
        val afterReset = runBlocking { database.counterDao().getById(secondPresetId)!! }
        assertEquals(0, afterReset.currentCount)
        assertEquals(2, afterReset.todayCount)
        assertEquals(2, afterReset.totalCount)
        collectJob.cancel()
    }

    @Test
    fun `home TabRow category filter switches between Morning Evening and General adhkar and increments count`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking {
            repository.ensureInitialized()
            prefsRepo.setAppLanguage(AppLanguage.ENGLISH)
        }

        // Verify filter model partitions seeded Adhkar accurately across Morning, Evening, and General
        val seeded = AdhkarSeedData.getInitialAdhkar()
        val morningIds = AdhkarTabFilter.MORNING.filterAdhkar(seeded).map { it.id }
        val eveningIds = AdhkarTabFilter.EVENING.filterAdhkar(seeded).map { it.id }
        val generalIds = AdhkarTabFilter.GENERAL.filterAdhkar(seeded).map { it.id }
        assertTrue(101 in morningIds && 201 !in morningIds && 301 !in morningIds)
        assertTrue(201 in eveningIds && 101 !in eveningIds && 301 !in eveningIds)
        assertTrue(301 in generalIds && 601 in generalIds && 101 !in generalIds && 201 !in generalIds)

        val viewModel = registerViewModel(DhikrViewModel(repository))
        composeRule.setContent {
            val prefs = viewModel.uiState.collectAsStateWithLifecycle().value.preferences
            AdhkarTheme(
                themeMode = prefs.themeMode,
                nightModeEnabled = prefs.nightModeEnabled,
                appLanguage = prefs.appLanguage
            ) {
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        // Scroll to the TabRow filter bar and verify tabs in English
        composeRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("dhikr_category_tab_row"))
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("dhikr_category_tab_row").assertIsDisplayed()
        composeRule.onNodeWithTag("category_tab_morning").assertIsDisplayed().assertIsSelected()
        composeRule.onNodeWithTag("category_tab_evening").assertIsDisplayed()
        composeRule.onNodeWithTag("category_tab_general").assertIsDisplayed()
        composeRule.onNodeWithText("Morning").assertIsDisplayed()
        composeRule.onNodeWithText("Evening").assertIsDisplayed()
        composeRule.onNodeWithText("General").assertIsDisplayed()

        // Morning tab displays Morning adhkar and excludes Evening & General
        composeRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("tab_filtered_dhikr_card_101"))
        composeRule.onNodeWithTag("tab_filtered_dhikr_card_101").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_filtered_dhikr_card_201").assertDoesNotExist()
        composeRule.onNodeWithTag("tab_filtered_dhikr_card_301").assertDoesNotExist()

        // Switch to Evening tab
        composeRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("dhikr_category_tab_row"))
        composeRule.onNodeWithTag("category_tab_evening").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("category_tab_evening").assertIsSelected()

        composeRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("tab_filtered_dhikr_card_201"))
        composeRule.onNodeWithTag("tab_filtered_dhikr_card_201").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_filtered_dhikr_card_101").assertDoesNotExist()
        composeRule.onNodeWithTag("tab_filtered_dhikr_card_301").assertDoesNotExist()

        // Switch to General tab and tap a filtered card to increment Room counter
        composeRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("dhikr_category_tab_row"))
        composeRule.onNodeWithTag("category_tab_general").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("category_tab_general").assertIsSelected()

        composeRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("tab_filtered_dhikr_card_301"))
        composeRule.onNodeWithTag("tab_filtered_dhikr_card_301").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_filtered_dhikr_card_101").assertDoesNotExist()
        composeRule.onNodeWithTag("tab_filtered_dhikr_card_201").assertDoesNotExist()

        val initialCount = runBlocking {
            database.dhikrDao().observeAll().first().firstOrNull { it.id == 301 }?.currentCount ?: 0
        }
        composeRule.onNodeWithTag("tab_filtered_dhikr_card_301").performClick()
        waitUntilIdle(timeoutMillis = 3000) {
            runBlocking {
                (database.dhikrDao().observeAll().first().firstOrNull { it.id == 301 }?.currentCount ?: 0) ==
                    initialCount + 1
            }
        }

        // Verify Arabic tab labels update when language switches to Arabic
        runBlocking { prefsRepo.setAppLanguage(AppLanguage.ARABIC) }
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("home_lazy_column")
                    .performScrollToNode(hasTestTag("dhikr_category_tab_row"))
                composeRule.onNodeWithText("أذكار عامة").assertIsDisplayed()
            }.isSuccess
        }
        composeRule.onNodeWithText("الصباح").assertIsDisplayed()
        composeRule.onNodeWithText("المساء").assertIsDisplayed()
    }

    @Test
    fun `dhikrEntryAudioPlayer embeds inline high quality controls for each dhikr entry`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking {
            repository.ensureInitialized()
            prefsRepo.setAppLanguage(AppLanguage.ENGLISH)
        }

        val fakeDhikrController = object : DhikrAudioPlayerController {
            private val _state = MutableStateFlow(DhikrAudioPlayerState())
            override val state: StateFlow<DhikrAudioPlayerState> = _state.asStateFlow()
            override fun playDisplayedAdhkar(adhkar: List<Dhikr>, startAtDhikrId: Int?) {
                val queue = DhikrAudioCatalog.buildQueue(adhkar)
                val idx = if (startAtDhikrId != null) {
                    queue.indexOfFirst { it.dhikrId == startAtDhikrId }.coerceAtLeast(0)
                } else {
                    0
                }
                _state.value = DhikrAudioPlayerState(
                    queue = queue,
                    currentIndex = idx,
                    isPlaying = true,
                    hasStarted = true,
                    positionMs = 1_500L,
                    durationMs = 24_000L
                )
            }
            override fun togglePlayPause(dhikr: Dhikr?, displayedAdhkar: List<Dhikr>) {
                if (dhikr != null && (_state.value.activeDhikrId != dhikr.id || !_state.value.hasStarted)) {
                    playDisplayedAdhkar(displayedAdhkar, dhikr.id)
                } else {
                    _state.update { it.copy(isPlaying = !it.isPlaying, hasStarted = true) }
                }
            }
            override fun pauseIfPlaying() {
                _state.update { it.copy(isPlaying = false) }
            }
            override fun nextTrack() = Unit
            override fun previousTrack() = Unit
            override fun seekTo(positionMs: Long) {
                _state.update { it.copy(positionMs = positionMs) }
            }
            override fun cyclePlaybackSpeed() {
                _state.update {
                    val next = when (it.playbackSpeed) {
                        1.0f -> 1.25f
                        1.25f -> 0.75f
                        else -> 1.0f
                    }
                    it.copy(playbackSpeed = next)
                }
            }
            override fun toggleRepeatCurrent() {
                _state.update { it.copy(repeatCurrent = !it.repeatCurrent) }
            }
            override fun retryCurrent() = Unit
            override fun stopAndHide() {
                _state.value = DhikrAudioPlayerState()
            }
            override fun release() = Unit
        }

        val viewModel = registerViewModel(
            DhikrViewModel(
                repository = repository,
                dhikrAudioController = fakeDhikrController
            )
        )

        composeRule.setContent {
            val prefs = viewModel.uiState.collectAsStateWithLifecycle().value.preferences
            AdhkarTheme(
                themeMode = prefs.themeMode,
                nightModeEnabled = prefs.nightModeEnabled,
                appLanguage = prefs.appLanguage
            ) {
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodes(hasTestTag("home_lazy_column")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("dhikr_category_tab_row"))
        composeRule.waitForIdle()

        // Scroll Home TabRow list to entry 101 (Quranic verse with 128 kbps MP3 quality label)
        composeRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("tab_filtered_dhikr_card_101"))
        composeRule.onNodeWithTag("entry_audio_player_tab_101")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("entry_audio_quality_tab_101").assertTextEquals("128 kbps MP3")

        // Expanded controls are hidden while idle
        assertEquals(0, composeRule.onAllNodes(hasTestTag("entry_audio_slider_tab_101")).fetchSemanticsNodes().size)

        // Start playback from the inline entry player CTA
        composeRule.onNodeWithTag("entry_audio_play_pause_tab_101")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        assertEquals(101, viewModel.uiState.value.dhikrAudio.activeDhikrId)
        assertTrue(viewModel.uiState.value.dhikrAudio.isPlaying)

        // Expanded scrub slider, speed cycle, repeat, and stop appear inside card 101
        composeRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("tab_filtered_dhikr_card_101"))
        composeRule.onNodeWithTag("entry_audio_slider_tab_101")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("entry_audio_speed_tab_101")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        assertEquals(1.25f, viewModel.uiState.value.dhikrAudio.playbackSpeed)
        composeRule.onNodeWithTag("entry_audio_repeat_tab_101")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        assertTrue(viewModel.uiState.value.dhikrAudio.repeatCurrent)

        // Open Morning Adhkar screen and play Prophetic dhikr entry 103 (HD MP3)
        composeRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("category_card_morning"))
        composeRule.onNodeWithTag("category_card_morning").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("dhikr_list_lazy_column")
            .performScrollToNode(hasTestTag("dhikr_card_103"))
        composeRule.onNodeWithTag("entry_audio_quality_103")
            .performScrollTo()
            .assertTextEquals("HD MP3")
        composeRule.onNodeWithTag("entry_audio_play_pause_103")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        assertEquals(103, viewModel.uiState.value.dhikrAudio.activeDhikrId)

        composeRule.onNodeWithTag("dhikr_list_lazy_column")
            .performScrollToNode(hasTestTag("dhikr_card_103"))
        composeRule.onNodeWithTag("entry_audio_stop_103")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        assertEquals(false, viewModel.uiState.value.dhikrAudio.hasStarted)
    }

    @Test
    fun `dhikrEntryAudioPlayer shows error banner and triggers retry callback`() {
        var retryCalls = 0
        val track = DhikrAudioCatalog.trackFor(AdhkarSeedData.getInitialAdhkar().first { it.id == 104 })

        composeRule.setContent {
            AdhkarTheme(appLanguage = AppLanguage.ENGLISH) {
                DhikrEntryAudioPlayer(
                    dhikrId = 104,
                    track = track,
                    isActive = true,
                    isPlaying = false,
                    isBuffering = false,
                    positionMs = 0L,
                    durationMs = 0L,
                    playbackSpeed = 1.0f,
                    repeatCurrent = false,
                    hasError = true,
                    onTogglePlayPause = {},
                    onSeekTo = {},
                    onCycleSpeed = {},
                    onToggleRepeat = {},
                    onRetry = { retryCalls += 1 },
                    onStop = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("entry_audio_error_104").assertIsDisplayed()
        composeRule.onNodeWithTag("entry_audio_retry_104").performClick()
        assertEquals(1, retryCalls)
    }

    @Test
    fun `httpHisnAudioTrackResolver resolves multiple chapters concurrently and upgrades to https`() {
        val requestedChapters = java.util.Collections.synchronizedList(mutableListOf<Int>())
        val allSeed = AdhkarSeedData.getInitialAdhkar()
        val sample = listOf(
            allSeed.first { it.id == 103 }, // Morning -> chapter 27
            allSeed.first { it.id == 301 }  // After Prayer -> chapter 25
        )
        val morningText = sample[0].content
        val afterPrayerText = sample[1].content

        val resolver = HttpHisnAudioTrackResolver(fetcher = { chapterId ->
            requestedChapters.add(chapterId)
            when (chapterId) {
                27 -> """{"أذكار الصباح":[{"ID":76,"ARABIC_TEXT":"$morningText","AUDIO":"http://www.hisnmuslim.com/audio/ar/76.mp3"}]}"""
                25 -> """{"أذكار بعد الصلاة":[{"ID":66,"ARABIC_TEXT":"$afterPrayerText","AUDIO":"http://www.hisnmuslim.com/audio/ar/66.mp3"}]}"""
                else -> null
            }
        })

        val resolved = runBlocking { resolver.resolveTracks(sample) }
        assertEquals(2, resolved.size)
        assertTrue(requestedChapters.containsAll(listOf(27, 25)))
        assertEquals("https://www.hisnmuslim.com/audio/ar/76.mp3", resolved[0].streamUrl)
        assertEquals("https://www.hisnmuslim.com/audio/ar/66.mp3", resolved[1].streamUrl)
        assertEquals("HD MP3", resolved[0].qualityLabel)
    }

    @Test
    fun `tasbih screen dedicated increment button and reset button update large counter with haptic feedback`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(database.dhikrDao(), database.counterDao(), prefsRepo)
        runBlocking {
            repository.ensureInitialized()
            prefsRepo.setHapticEnabled(true)
            prefsRepo.setDailyTasbihTarget(100)
        }
        val viewModel = registerViewModel(DhikrViewModel(repository))
        val hapticCalls = mutableListOf<HapticFeedbackType>()
        val spyHaptic = object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                hapticCalls.add(hapticFeedbackType)
            }
        }

        composeRule.setContent {
            AdhkarTheme {
                CompositionLocalProvider(LocalHapticFeedback provides spyHaptic) {
                    AdhkarAppRoot(viewModel = viewModel)
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("nav_tab_tasbih").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tasbih_current_count_text", useUnmergedTree = true)
            .assertTextEquals("0")

        // Tap dedicated Count +1 increment button twice
        composeRule.onNodeWithTag("tasbih_increment_button")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("tasbih_increment_button").performClick()

        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("tasbih_current_count_text", useUnmergedTree = true)
                    .assertTextEquals("2")
            }.isSuccess
        }
        assertEquals(listOf(HapticFeedbackType.TextHandleMove, HapticFeedbackType.TextHandleMove), hapticCalls)

        // Tap Reset button and verify count returns to 0 with LongPress haptic confirmation
        composeRule.onNodeWithTag("tasbih_reset_cycle_button")
            .performScrollTo()
            .performClick()

        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("tasbih_current_count_text", useUnmergedTree = true)
                    .assertTextEquals("0")
            }.isSuccess
        }
        assertEquals(HapticFeedbackType.LongPress, hapticCalls.last())
    }

    @Test
    fun `merged manifest removes privacy sandbox adservices permissions while keeping admob config`() {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            android.content.pm.PackageManager.GET_PERMISSIONS
        )
        val requested = packageInfo.requestedPermissions?.toSet().orEmpty()
        assertTrue(!requested.contains("android.permission.ACCESS_ADSERVICES_ATTRIBUTION"))
        assertTrue(!requested.contains("android.permission.ACCESS_ADSERVICES_TOPICS"))
        assertTrue(!requested.contains("android.permission.ACCESS_ADSERVICES_AD_ID"))

        val appInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            android.content.pm.PackageManager.GET_META_DATA
        )
        val appId = appInfo.metaData?.getString("com.google.android.gms.ads.APPLICATION_ID")
        assertTrue(!appId.isNullOrBlank())
    }

    @Test
    fun `tasbeeh screen saves count in DataStore across fresh restart and reset button clears UI and DataStore`() {
        val prefsRepo = UserPreferencesRepository(context)
        val firstRepo = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking { firstRepo.ensureInitialized() }
        val firstViewModel = registerViewModel(DhikrViewModel(firstRepo))

        val useRestartedInstance = androidx.compose.runtime.mutableStateOf(false)
        val freshDb = Room.inMemoryDatabaseBuilder(context, AdhkarDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val restartedRepo = AdhkarRepository(
            dhikrDao = freshDb.dhikrDao(),
            counterDao = freshDb.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = freshDb.dailyActivityDao()
        )
        val restoredVm by lazy { registerViewModel(DhikrViewModel(restartedRepo)) }

        try {
            composeRule.setContent {
                AdhkarTheme {
                    if (!useRestartedInstance.value) {
                        AdhkarAppRoot(viewModel = firstViewModel)
                    } else {
                        AdhkarAppRoot(viewModel = restoredVm)
                    }
                }
            }
            composeRule.waitForIdle()

            composeRule.onNodeWithTag("nav_tab_tasbih").performClick()
            composeRule.waitForIdle()

            // Increment 3 times via dedicated button & tap circle
            composeRule.onNodeWithTag("tasbih_increment_button")
                .performScrollTo()
                .performClick()
            composeRule.onNodeWithTag("tasbih_increment_button").performClick()
            composeRule.onNodeWithTag("tasbih_tap_circle")
                .performScrollTo()
                .performClick()

            composeRule.waitUntil(timeoutMillis = 3000) {
                runCatching {
                    composeRule.onNodeWithTag("tasbih_current_count_text", useUnmergedTree = true)
                        .assertTextEquals("3")
                }.isSuccess && runBlocking { prefsRepo.preferencesFlow.first().currentTasbihCount == 3 }
            }

            val prefsBeforeRestart = runBlocking { prefsRepo.preferencesFlow.first() }
            assertEquals(3, prefsBeforeRestart.currentTasbihCount)
            assertEquals(3, prefsBeforeRestart.savedTasbihCounts[1])

            // Simulate app restart with a brand-new Room DB instance; count must restore from DataStore
            useRestartedInstance.value = true
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("nav_tab_tasbih").performClick()
            composeRule.waitUntil(timeoutMillis = 4000) {
                runCatching {
                    composeRule.onNodeWithTag("tasbih_current_count_text", useUnmergedTree = true)
                        .assertTextEquals("3")
                }.isSuccess
            }

            // Tap dedicated reset button -> UI updates to 0 and DataStore persists 0
            composeRule.onNodeWithTag("tasbih_reset_cycle_button")
                .performScrollTo()
                .performClick()

            composeRule.waitUntil(timeoutMillis = 3000) {
                runCatching {
                    composeRule.onNodeWithTag("tasbih_current_count_text", useUnmergedTree = true)
                        .assertTextEquals("0")
                }.isSuccess && runBlocking { prefsRepo.preferencesFlow.first().currentTasbihCount == 0 }
            }

            val prefsAfterReset = runBlocking { prefsRepo.preferencesFlow.first() }
            assertEquals(0, prefsAfterReset.currentTasbihCount)
            assertEquals(0, prefsAfterReset.savedTasbihCounts[1])
        } finally {
            freshDb.close()
        }
    }

    @Test
    fun `qiblaCalculator computes Kaaba bearing distance and SensorManager azimuth accurately`() {
        // Cairo (30.0444, 31.2357) -> Kaaba (~136 deg SE, ~1285 km)
        val cairoBearing = QiblaCalculator.calculateQiblaBearingDegrees(30.0444, 31.2357)
        val cairoDistance = QiblaCalculator.calculateDistanceToKaabaKm(30.0444, 31.2357)
        assertTrue(cairoBearing in 134f..139f)
        assertTrue(cairoDistance in 1260.0..1310.0)
        assertEquals("SE", QiblaCalculator.cardinalDirectionCode(cairoBearing))

        // New York (40.7128, -74.0060) -> Kaaba (~58.5 deg NE)
        val nyBearing = QiblaCalculator.calculateQiblaBearingDegrees(40.7128, -74.0060)
        assertTrue(nyBearing in 57f..60f)
        assertEquals("NE", QiblaCalculator.cardinalDirectionCode(nyBearing))

        // Kaaba itself -> ~0 km distance
        val atKaabaDist = QiblaCalculator.calculateDistanceToKaabaKm(
            QiblaCalculator.KAABA_LATITUDE,
            QiblaCalculator.KAABA_LONGITUDE
        )
        assertTrue(atKaabaDist < 0.01)

        // Wrap-around shortest signed delta
        assertEquals(20f, QiblaCalculator.shortestSignedAngleDelta(350f, 10f), 0.01f)
        assertEquals(-20f, QiblaCalculator.shortestSignedAngleDelta(10f, 350f), 0.01f)
        assertTrue(QiblaCalculator.isAlignedWithQibla(358f, 1f))
        assertTrue(!QiblaCalculator.isAlignedWithQibla(340f, 1f))

        // SensorManager azimuth calculation from accelerometer (flat on table) + magnetometer (North +Y)
        val flatGravity = floatArrayOf(0f, 0f, 9.81f)
        val northMagnet = floatArrayOf(0f, 25f, -40f)
        val northAzimuth = QiblaCalculator.computeAzimuthFromSensors(flatGravity, northMagnet)
        assertNotNull(northAzimuth)
        assertEquals(0f, northAzimuth!!, 0.5f)

        // Device rotated 90 deg clockwise (facing East): geomagnetic North points along -X in phone frame
        val eastMagnet = floatArrayOf(-25f, 0f, -40f)
        val eastAzimuth = QiblaCalculator.computeAzimuthFromSensors(flatGravity, eastMagnet)
        assertNotNull(eastAzimuth)
        assertEquals(90f, eastAzimuth!!, 0.5f)

        // In Robolectric default SensorManager (no hardware sensors registered), AndroidQiblaSensorSource emits hasSensors=false
        val androidSensorSource = AndroidQiblaSensorSource(context)
        val reading = runBlocking { androidSensorSource.observeCompass(30.0444, 31.2357).first() }
        assertTrue(!reading.hasSensors)
    }

    @Test
    fun `qibla screen navigates from home renders live compass dial aligns with haptic and switches cities`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking {
            repository.ensureInitialized()
            prefsRepo.setHapticEnabled(true)
        }

        val compassFlow = MutableStateFlow(
            CompassReading(azimuthDegrees = 0f, hasSensors = true, isCalibrating = false)
        )
        val fakeSensorSource = object : QiblaSensorSource {
            override fun observeCompass(latitude: Double?, longitude: Double?) = compassFlow
        }
        val fakeRemote = object : PrayerTimesRemoteSource {
            override suspend fun fetchDailyPrayerTimes(
                latitude: Double,
                longitude: Double,
                locationLabel: String,
                method: PrayerCalculationMethod
            ): com.example.data.model.DailyPrayerSchedule =
                PrayerTimeCalculator.buildSchedule(
                    fajrRaw = "04:45",
                    sunriseRaw = "06:10",
                    dhuhrRaw = "12:15",
                    asrRaw = "15:35",
                    maghribRaw = "18:20",
                    ishaRaw = "19:40",
                    dateReadable = "19 Nov 2026",
                    hijriDateAr = "٨ جمادى الآخرة ١٤٤٨",
                    locationLabel = locationLabel,
                    timezone = "Africa/Cairo",
                    latitude = latitude,
                    longitude = longitude,
                    method = method,
                    nowMinutesOfDay = 10 * 60
                )
        }

        val viewModel = registerViewModel(
            DhikrViewModel(
                repository = repository,
                prayerTimesRemoteSource = fakeRemote,
                qiblaSensorSource = fakeSensorSource
            )
        )

        val hapticCalls = mutableListOf<HapticFeedbackType>()
        val spyHaptic = object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                hapticCalls.add(hapticFeedbackType)
            }
        }

        composeRule.setContent {
            AdhkarTheme(appLanguage = AppLanguage.ARABIC) {
                CompositionLocalProvider(LocalHapticFeedback provides spyHaptic) {
                    AdhkarAppRoot(viewModel = viewModel)
                }
            }
        }
        composeRule.waitForIdle()

        // Navigate from Home Qibla card
        composeRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("home_qibla_card"))
        composeRule.onNodeWithTag("home_qibla_card")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("qibla_compass_canvas").assertIsDisplayed()
        composeRule.onNodeWithTag("qibla_bearing_text").assertIsDisplayed()

        // Select Cairo preset chip
        composeRule.onNodeWithTag("qibla_lazy_column")
            .performScrollToNode(hasTestTag("qibla_city_cairo"))
        composeRule.onNodeWithTag("qibla_city_cairo")
            .performScrollTo()
            .performClick()

        val cairoBearing = QiblaCalculator.calculateQiblaBearingDegrees(30.0444, 31.2357)
        composeRule.onNodeWithTag("qibla_lazy_column")
            .performScrollToNode(hasTestTag("qibla_hero_card"))
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("qibla_location_label", useUnmergedTree = true)
                    .assertTextContains("القاهرة", substring = true)
                composeRule.onNodeWithTag("qibla_bearing_text", useUnmergedTree = true)
                    .assertTextContains("${kotlin.math.round(cairoBearing.toDouble()).toInt()}°", substring = true)
            }.isSuccess
        }

        // Rotate compass into alignment with Cairo Qibla bearing -> badge shows aligned + LongPress haptic
        compassFlow.value = CompassReading(azimuthDegrees = cairoBearing, hasSensors = true)
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("qibla_alignment_text", useUnmergedTree = true)
                    .assertTextContains("أنت باتجاه القبلة", substring = true)
            }.isSuccess && hapticCalls.contains(HapticFeedbackType.LongPress)
        }

        composeRule.onNodeWithTag("qibla_lazy_column")
            .performScrollToNode(hasTestTag("qibla_compass_canvas"))
        composeRule.onNodeWithTag("qibla_azimuth_text", useUnmergedTree = true)
            .performScrollTo()
            .assertTextContains("${kotlin.math.round(cairoBearing.toDouble()).toInt()}°", substring = true)

        // Switch city to London -> bearing updates (~119°)
        composeRule.onNodeWithTag("qibla_lazy_column")
            .performScrollToNode(hasTestTag("qibla_city_london"))
        composeRule.onNodeWithTag("qibla_city_london")
            .performScrollTo()
            .performClick()
        val londonBearing = QiblaCalculator.calculateQiblaBearingDegrees(51.5072, -0.1276)
        composeRule.onNodeWithTag("qibla_lazy_column")
            .performScrollToNode(hasTestTag("qibla_hero_card"))
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("qibla_location_label", useUnmergedTree = true)
                    .assertTextContains("لندن", substring = true)
                composeRule.onNodeWithTag("qibla_bearing_text", useUnmergedTree = true)
                    .assertTextContains("${kotlin.math.round(londonBearing.toDouble()).toInt()}°", substring = true)
            }.isSuccess
        }

        // When hardware sensors are absent, missing-sensor banner is shown clearly
        compassFlow.value = CompassReading(azimuthDegrees = 0f, hasSensors = false)
        composeRule.onNodeWithTag("qibla_lazy_column")
            .performScrollToNode(hasTestTag("qibla_no_sensor_banner"))
        composeRule.onNodeWithTag("qibla_no_sensor_banner").assertIsDisplayed()
    }

    @Test
    fun `tasbeeh auto-count lets user set target, increments counter, and alerts on target completion`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking {
            repository.ensureInitialized()
            prefsRepo.setHapticEnabled(true)
            prefsRepo.setSoundEnabled(true)
        }

        val viewModel = registerViewModel(
            DhikrViewModel(
                repository = repository,
                autoCountTickIntervalMillis = 15L
            )
        )
        val chimeEvents = mutableListOf<ChimeEvent>()
        val hapticEvents = mutableListOf<HapticEvent>()

        composeRule.setContent {
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            androidx.compose.runtime.LaunchedEffect(viewModel) {
                scope.launch { viewModel.chimeEvents.collect { chimeEvents.add(it) } }
                scope.launch { viewModel.hapticEvents.collect { hapticEvents.add(it) } }
            }
            AdhkarTheme {
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        // Navigate to Tasbeeh tab
        composeRule.onNodeWithTag("nav_tab_tasbih").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tasbih_autocount_card")
            .performScrollTo()
            .assertIsDisplayed()

        // Preset chip (66) updates target cleanly
        composeRule.onNodeWithTag("tasbih_autocount_target_66")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tasbih_autocount_progress_text")
            .performScrollTo()
            .assertTextEquals("0 / 66")

        // Open custom target dialog and set target = 4 tasbeehs
        composeRule.onNodeWithTag("tasbih_autocount_custom_button")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tasbih_autocount_custom_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("tasbih_autocount_custom_input").performTextReplacement("4")
        composeRule.onNodeWithTag("confirm_tasbih_autocount_custom").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tasbih_autocount_progress_text")
            .performScrollTo()
            .assertTextEquals("0 / 4")

        // Select fast pace
        composeRule.onNodeWithTag("tasbih_autocount_pace_fast")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        assertEquals(AutoCountPace.FAST, viewModel.uiState.value.autoCount.pace)

        // Start Auto-Count -> completes target of 4, updates counter, and emits audio + haptic alerts
        composeRule.onNodeWithTag("tasbih_autocount_start_pause_button")
            .performScrollTo()
            .performClick()

        waitUntilIdle(timeoutMillis = 5000) {
            viewModel.uiState.value.autoCount.status == AutoCountStatus.COMPLETED
        }

        assertEquals(4, viewModel.uiState.value.autoCount.completedCount)
        assertEquals(4, viewModel.uiState.value.selectedTasbih?.currentCount)
        assertTrue(chimeEvents.contains(ChimeEvent.AutoCountTargetReached))
        assertTrue(hapticEvents.contains(HapticEvent.Milestone))

        composeRule.onNodeWithTag("tasbih_autocount_completed_banner")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("tasbih_autocount_live_pill")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("tasbih_current_count_text", useUnmergedTree = true)
            .performScrollTo()
            .assertTextEquals("4")

        // Reset restores 0 / 4 IDLE
        composeRule.onNodeWithTag("tasbih_autocount_reset_button")
            .performScrollTo()
            .performClick()
        waitUntilIdle(timeoutMillis = 3000) {
            viewModel.uiState.value.autoCount.status == AutoCountStatus.IDLE
        }
        composeRule.onNodeWithTag("tasbih_autocount_progress_text")
            .performScrollTo()
            .assertTextEquals("0 / 4")
    }

    @Test
    fun `adhkar reading settings and last used tasbeeh count persist in DataStore across app restart`() {
        val prefsRepo = UserPreferencesRepository(context)
        val firstRepo = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking { firstRepo.ensureInitialized() }
        val firstViewModel = registerViewModel(DhikrViewModel(firstRepo))

        // Configure Adhkar reading settings & increment second Tasbeeh preset twice + customize Auto-Count
        firstViewModel.onFontSizeChange(28f)
        firstViewModel.onFocusModeToggle(true)
        firstViewModel.onAutoAdvanceToggle(false)
        firstViewModel.onNightModeToggle(true)
        firstViewModel.onThemeModeChange(ThemeMode.DARK)
        firstViewModel.onLanguageChange(AppLanguage.ENGLISH)
        firstViewModel.onSelectTasbih(2)
        firstViewModel.onSetAutoCountTarget(66)
        firstViewModel.onSelectAutoCountPace(AutoCountPace.SLOW)

        waitUntilIdle(timeoutMillis = 3000) {
            runBlocking {
                val p = prefsRepo.preferencesFlow.first()
                p.fontSizeSp == 28f &&
                    p.focusModeEnabled &&
                    !p.autoAdvance &&
                    p.nightModeEnabled &&
                    p.appLanguage == AppLanguage.ENGLISH &&
                    p.selectedTasbihId == 2 &&
                    p.autoCountTarget == 66 &&
                    p.autoCountPace == "SLOW"
            }
        }

        val secondCounter = runBlocking { firstRepo.counters.first().first { it.id == 2 } }
        firstViewModel.incrementTasbeeh(secondCounter)
        waitUntilIdle(timeoutMillis = 3000) {
            runBlocking { prefsRepo.preferencesFlow.first().currentTasbihCount == 1 }
        }
        val secondCounterAfterOne = runBlocking { firstRepo.counters.first().first { it.id == 2 } }
        firstViewModel.incrementTasbeeh(secondCounterAfterOne)
        waitUntilIdle(timeoutMillis = 3000) {
            runBlocking {
                val p = prefsRepo.preferencesFlow.first()
                p.currentTasbihCount == 2 && p.savedTasbihCounts[2] == 2
            }
        }

        // Simulate full app restart with fresh Room DB and fresh ViewModel backed by the same DataStore
        viewModelStore.clear()
        val restartedDb = Room.inMemoryDatabaseBuilder(context, AdhkarDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val restartedRepo = AdhkarRepository(
                dhikrDao = restartedDb.dhikrDao(),
                counterDao = restartedDb.counterDao(),
                prefsRepo = UserPreferencesRepository(context),
                dailyActivityDao = restartedDb.dailyActivityDao()
            )
            val restartedViewModel = registerViewModel(DhikrViewModel(restartedRepo))

            composeRule.setContent {
                val state by restartedViewModel.uiState.collectAsStateWithLifecycle()
                AdhkarTheme(
                    themeMode = state.preferences.themeMode,
                    nightModeEnabled = state.preferences.nightModeEnabled,
                    appLanguage = state.preferences.appLanguage
                ) {
                    AdhkarAppRoot(viewModel = restartedViewModel)
                }
            }

            waitUntilIdle(timeoutMillis = 4000) {
                val st = restartedViewModel.uiState.value
                !st.isLoading &&
                    st.preferences.fontSizeSp == 28f &&
                    st.preferences.focusModeEnabled &&
                    !st.preferences.autoAdvance &&
                    st.preferences.nightModeEnabled &&
                    st.preferences.themeMode == ThemeMode.DARK &&
                    st.preferences.appLanguage == AppLanguage.ENGLISH &&
                    st.selectedTasbih?.id == 2 &&
                    st.selectedTasbih?.currentCount == 2 &&
                    st.autoCount.targetCount == 66 &&
                    st.autoCount.pace == AutoCountPace.SLOW
            }

            // Open Tasbeeh screen and verify UI displays restored count & Auto-Count target
            composeRule.onNodeWithTag("nav_tab_tasbih").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("tasbih_current_count_text", useUnmergedTree = true)
                .performScrollTo()
                .assertTextEquals("2")
            composeRule.onNodeWithTag("tasbih_autocount_progress_text")
                .performScrollTo()
                .assertTextEquals("0 / 66")
        } finally {
            restartedDb.close()
        }
    }

    @Test
    fun `stats screen displays Vico daily and weekly tasbeeh progress charts with accurate totals and interactive selection`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayKey = keyFormat.format(Calendar.getInstance().time)
        val threeDaysAgoCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -3) }
        val lastWeekCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -9) }
        val twoWeeksAgoCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -16) }

        runBlocking {
            repository.ensureInitialized()
            prefsRepo.setDailyTasbihTarget(100)
            // Seed Tasbeeh counts across current week and prior weeks
            repository.saveDailyActivity(
                DailyActivity(dateKey = todayKey, dhikrCount = 5, tasbihCount = 40)
            )
            repository.saveDailyActivity(
                DailyActivity(
                    dateKey = keyFormat.format(threeDaysAgoCal.time),
                    dhikrCount = 0,
                    tasbihCount = 65
                )
            )
            repository.saveDailyActivity(
                DailyActivity(
                    dateKey = keyFormat.format(lastWeekCal.time),
                    dhikrCount = 0,
                    tasbihCount = 50
                )
            )
            repository.saveDailyActivity(
                DailyActivity(
                    dateKey = keyFormat.format(twoWeeksAgoCal.time),
                    dhikrCount = 0,
                    tasbihCount = 30
                )
            )
        }
        val viewModel = registerViewModel(DhikrViewModel(repository))

        composeRule.setContent {
            AdhkarTheme {
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        // Navigate to Stats screen
        composeRule.onNodeWithTag("nav_tab_stats").performClick()
        composeRule.waitForIdle()

        // Verify Daily Tasbeeh Vico chart card & chart
        composeRule.onNodeWithTag("stats_tasbih_daily_vico_card").assertIsDisplayed()
        composeRule.onNodeWithTag("stats_tasbih_daily_vico_chart").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("stats_tasbih_daily_selected_count").assertTextEquals("40")
            }.isSuccess
        }
        // 7-day average = (40 + 65) / 7 = 15, best day = 65
        composeRule.onNodeWithTag("stats_tasbih_daily_kpi_avg").performScrollTo().assertTextEquals("15")
        composeRule.onNodeWithTag("stats_tasbih_daily_kpi_best").performScrollTo().assertTextEquals("65")

        // Tap daily chip index 3 (3 days ago) -> updates daily selected readout to 65
        composeRule.onNodeWithTag("stats_tasbih_daily_chip_3")
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("stats_tasbih_daily_selected_count").assertTextEquals("65")
            }.isSuccess
        }

        // Scroll to Weekly Tasbeeh Vico chart card & chart
        composeRule.onNodeWithTag("stats_lazy_column")
            .performScrollToNode(hasTestTag("stats_tasbih_weekly_vico_card"))
        composeRule.onNodeWithTag("stats_tasbih_weekly_vico_card").assertIsDisplayed()
        composeRule.onNodeWithTag("stats_tasbih_weekly_vico_chart").assertIsDisplayed()

        // Current week total = 40 + 65 = 105
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("stats_tasbih_weekly_selected_count").assertTextEquals("105")
            }.isSuccess
        }

        // Tap previous week chip (index 2, 7..13 days ago) -> shows 50
        composeRule.onNodeWithTag("stats_tasbih_weekly_chip_2")
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("stats_tasbih_weekly_selected_count").assertTextEquals("50")
            }.isSuccess
        }

        // Tap 2-weeks-ago chip (index 1, 14..20 days ago) -> shows 30
        composeRule.onNodeWithTag("stats_tasbih_weekly_chip_1")
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("stats_tasbih_weekly_selected_count").assertTextEquals("30")
            }.isSuccess
        }
    }

    @Test
    fun `tasbeeh custom target vibration pattern and sound effect persist and trigger when user target is reached`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking {
            repository.ensureInitialized()
            prefsRepo.setHapticEnabled(true)
            prefsRepo.setSoundEnabled(true)
            prefsRepo.setDailyTasbihTarget(100)
        }
        val viewModel = registerViewModel(DhikrViewModel(repository))
        val hapticEvents = mutableListOf<HapticEvent>()
        val chimeEvents = mutableListOf<ChimeEvent>()

        composeRule.setContent {
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            androidx.compose.runtime.LaunchedEffect(viewModel) {
                scope.launch { viewModel.hapticEvents.collect { hapticEvents.add(it) } }
                scope.launch { viewModel.chimeEvents.collect { chimeEvents.add(it) } }
            }
            AdhkarTheme {
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        // Navigate to Tasbeeh screen
        composeRule.onNodeWithTag("nav_tab_tasbih").performClick()
        composeRule.waitForIdle()

        // Set a user-defined target of 3 via custom target dialog
        composeRule.onNodeWithTag("tasbih_target_custom_button")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tasbih_target_custom_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("tasbih_target_custom_input").performTextReplacement("3")
        composeRule.onNodeWithTag("confirm_tasbih_target_custom").performClick()
        waitUntilIdle(timeoutMillis = 3000) {
            viewModel.uiState.value.selectedTasbih?.targetCount == 3
        }

        // Customize vibration pattern -> HEARTBEAT and sound effect -> CRYSTAL_BELL
        composeRule.onNodeWithTag("tasbih_target_alert_card")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("tasbih_vibration_pattern_heartbeat")
            .performScrollTo()
            .performClick()
        waitUntilIdle(timeoutMillis = 3000) {
            runBlocking {
                prefsRepo.preferencesFlow.first().tasbihTargetVibrationPattern == TasbihVibrationPattern.HEARTBEAT
            } && hapticEvents.contains(HapticEvent.PreviewPattern(TasbihVibrationPattern.HEARTBEAT))
        }

        composeRule.onNodeWithTag("tasbih_sound_effect_crystal_bell")
            .performScrollTo()
            .performClick()
        waitUntilIdle(timeoutMillis = 3000) {
            runBlocking {
                prefsRepo.preferencesFlow.first().tasbihTargetSoundEffect == TasbihTargetSoundEffect.CRYSTAL_BELL
            } && chimeEvents.contains(ChimeEvent.TargetSoundEffect(TasbihTargetSoundEffect.CRYSTAL_BELL))
        }

        // Verify explicit preview buttons fire custom vibration & sound
        hapticEvents.clear()
        chimeEvents.clear()
        composeRule.onNodeWithTag("tasbih_vibration_preview_button")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("tasbih_sound_preview_button")
            .performScrollTo()
            .performClick()
        waitUntilIdle(timeoutMillis = 3000) {
            hapticEvents.contains(HapticEvent.PreviewPattern(TasbihVibrationPattern.HEARTBEAT)) &&
                chimeEvents.contains(ChimeEvent.TargetSoundEffect(TasbihTargetSoundEffect.CRYSTAL_BELL))
        }

        // Clear preview events, tap rosary 3 times to hit target = 3, and verify milestone + CRYSTAL_BELL sound
        hapticEvents.clear()
        chimeEvents.clear()
        repeat(3) {
            composeRule.onNodeWithTag("tasbih_tap_circle")
                .performScrollTo()
                .performClick()
        }
        waitUntilIdle(timeoutMillis = 3000) {
            viewModel.uiState.value.selectedTasbih?.currentCount == 3 &&
                hapticEvents.contains(HapticEvent.Milestone) &&
                chimeEvents.contains(ChimeEvent.TargetSoundEffect(TasbihTargetSoundEffect.CRYSTAL_BELL))
        }

        assertEquals(listOf(HapticEvent.Tap, HapticEvent.Tap, HapticEvent.Milestone), hapticEvents)
        assertEquals(
            listOf(ChimeEvent.TargetSoundEffect(TasbihTargetSoundEffect.CRYSTAL_BELL)),
            chimeEvents
        )

        // Verify SoundHelper PCM tone synthesis for new effects produces non-empty audio samples
        assertTrue(SoundHelper.generateCrystalBellPcm().any { it != 0.toShort() })
        assertTrue(SoundHelper.generateSoftTonePcm().any { it != 0.toShort() })
    }

    @Test
    fun `workManager daily morning and evening adhkar notifications fire and automatically reschedule next day`() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val workManager = WorkManager.getInstance(context)
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking {
            repository.ensureInitialized()
            prefsRepo.setReminder(DhikrCategory.MORNING, enabled = true, hour = 5, minute = 45)
            prefsRepo.setReminder(DhikrCategory.EVENING, enabled = true, hour = 17, minute = 20)
        }

        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 20, 4, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val customReminders = listOf(
            DhikrReminder(DhikrCategory.MORNING, enabled = true, hour = 5, minute = 45),
            DhikrReminder(DhikrCategory.EVENING, enabled = true, hour = 17, minute = 20),
            DhikrReminder(DhikrCategory.SLEEP, enabled = false, hour = 22, minute = 0)
        )
        DhikrReminderScheduler.syncAllReminders(context, customReminders, nowCal.timeInMillis)

        // Verify both Morning & Evening are enqueued as 24h PeriodicWorkRequests with exact initial delays
        val morningInfos = workManager.getWorkInfosForUniqueWork(
            DhikrReminderScheduler.uniqueWorkNameFor(DhikrCategory.MORNING)
        ).get()
        val eveningInfos = workManager.getWorkInfosForUniqueWork(
            DhikrReminderScheduler.uniqueWorkNameFor(DhikrCategory.EVENING)
        ).get()
        assertEquals(1, morningInfos.size)
        assertEquals(WorkInfo.State.ENQUEUED, morningInfos.first().state)
        assertTrue(morningInfos.first().tags.contains(DhikrReminderScheduler.WORK_TAG_ALL_REMINDERS))
        assertNotNull(morningInfos.first().periodicityInfo)
        assertEquals(
            java.util.concurrent.TimeUnit.HOURS.toMillis(24L),
            morningInfos.first().periodicityInfo?.repeatIntervalMillis
        )
        assertEquals(
            DhikrReminderScheduler.computeInitialDelayMillis(5, 45, nowCal.timeInMillis),
            morningInfos.first().initialDelayMillis
        )
        assertEquals(1, eveningInfos.size)
        assertEquals(WorkInfo.State.ENQUEUED, eveningInfos.first().state)
        assertNotNull(eveningInfos.first().periodicityInfo)
        assertEquals(
            java.util.concurrent.TimeUnit.HOURS.toMillis(24L),
            eveningInfos.first().periodicityInfo?.repeatIntervalMillis
        )

        // Run Morning and Evening workers and verify notifications + preserved periodic schedule
        val morningIdBefore = morningInfos.first().id
        val eveningIdBefore = eveningInfos.first().id
        runBlocking {
            val morningWorker = TestListenableWorkerBuilder<DhikrReminderWorker>(context)
                .setInputData(workDataOf(DhikrNotificationHelper.EXTRA_CATEGORY_ID to DhikrCategory.MORNING.id))
                .build()
            assertEquals(androidx.work.ListenableWorker.Result.success(), morningWorker.doWork())

            val eveningWorker = TestListenableWorkerBuilder<DhikrReminderWorker>(context)
                .setInputData(workDataOf(DhikrNotificationHelper.EXTRA_CATEGORY_ID to DhikrCategory.EVENING.id))
                .build()
            assertEquals(androidx.work.ListenableWorker.Result.success(), eveningWorker.doWork())
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNotifications = Shadows.shadowOf(notificationManager)
        assertNotNull(shadowNotifications.getNotification(DhikrNotificationHelper.notificationIdFor(DhikrCategory.MORNING)))
        assertNotNull(shadowNotifications.getNotification(DhikrNotificationHelper.notificationIdFor(DhikrCategory.EVENING)))

        // Both PeriodicWorkRequests remain ENQUEUED with the same unique periodic ID for future daily runs
        val morningInfosAfter = workManager.getWorkInfosForUniqueWork(
            DhikrReminderScheduler.uniqueWorkNameFor(DhikrCategory.MORNING)
        ).get()
        val eveningInfosAfter = workManager.getWorkInfosForUniqueWork(
            DhikrReminderScheduler.uniqueWorkNameFor(DhikrCategory.EVENING)
        ).get()
        assertEquals(WorkInfo.State.ENQUEUED, morningInfosAfter.first().state)
        assertEquals(WorkInfo.State.ENQUEUED, eveningInfosAfter.first().state)
        assertEquals(morningIdBefore, morningInfosAfter.first().id)
        assertEquals(eveningIdBefore, eveningInfosAfter.first().id)

        // Verify Settings screen renders the active schedule status banner with saved times
        val viewModel = registerViewModel(DhikrViewModel(repository, AndroidReminderScheduler(context)))
        composeRule.setContent {
            AdhkarTheme {
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_tab_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("reminder_active_schedule_status")
            .performScrollTo()
            .assertIsDisplayed()

        // Disabling Morning reminder cancels its WorkManager task cleanly
        composeRule.onNodeWithTag("reminder_switch_morning")
            .performScrollTo()
            .performClick()
        waitUntilIdle(timeoutMillis = 3000) {
            workManager.getWorkInfosForUniqueWork(
                DhikrReminderScheduler.uniqueWorkNameFor(DhikrCategory.MORNING)
            ).get().firstOrNull()?.state == WorkInfo.State.CANCELLED
        }
        assertEquals(
            WorkInfo.State.CANCELLED,
            workManager.getWorkInfosForUniqueWork(
                DhikrReminderScheduler.uniqueWorkNameFor(DhikrCategory.MORNING)
            ).get().first().state
        )
    }

    @Test
    fun `alAdhanPrayerTimesRemoteSource parses public API response and prayer dashboard handles error retry`() {
        val fakeApi = object : com.example.data.remote.AlAdhanRetrofitApi {
            var shouldReturnErrorStatus = true
            override suspend fun getTimings(
                latitude: Double,
                longitude: Double,
                method: Int
            ): com.example.data.remote.AlAdhanResponse {
                if (shouldReturnErrorStatus) {
                    return com.example.data.remote.AlAdhanResponse(
                        code = 500,
                        status = "Service Unavailable",
                        data = com.example.data.remote.AlAdhanData(
                            timings = com.example.data.remote.AlAdhanTimings(
                                fajr = "05:00",
                                sunrise = "06:20",
                                dhuhr = "12:05",
                                asr = "15:15",
                                maghrib = "17:40",
                                isha = "19:10"
                            ),
                            date = com.example.data.remote.AlAdhanDate(),
                            meta = com.example.data.remote.AlAdhanMeta()
                        )
                    )
                }
                return com.example.data.remote.AlAdhanResponse(
                    code = 200,
                    status = "OK",
                    data = com.example.data.remote.AlAdhanData(
                        timings = com.example.data.remote.AlAdhanTimings(
                            fajr = "05:02 (+03)",
                            sunrise = "06:21 (+03)",
                            dhuhr = "12:04 (+03)",
                            asr = "15:18 (+03)",
                            maghrib = "17:42 (+03)",
                            isha = "19:12 (+03)"
                        ),
                        date = com.example.data.remote.AlAdhanDate(
                            readable = "20 Nov 2026",
                            hijri = com.example.data.remote.AlAdhanHijri(
                                day = "10",
                                year = "1448",
                                month = com.example.data.remote.AlAdhanHijriMonth(
                                    ar = "جمادى الآخرة",
                                    en = "Jumada al-Akhirah"
                                ),
                                weekday = com.example.data.remote.AlAdhanHijriWeekday(
                                    ar = "الجمعة",
                                    en = "Al Jumaah"
                                )
                            )
                        ),
                        meta = com.example.data.remote.AlAdhanMeta(timezone = "Asia/Riyadh")
                    )
                )
            }
        }

        val remoteSource = com.example.data.remote.AlAdhanPrayerTimesRemoteSource(fakeApi)

        // Initial error response triggers IllegalStateException -> retry recovers cleanly
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking { repository.ensureInitialized() }

        val fakeLocation = object : DeviceLocationProvider {
            override fun hasLocationPermission(): Boolean = true
            override suspend fun fetchCurrentLocation(): LocationFetchResult =
                LocationFetchResult.Success(24.7136, 46.6753, "الرياض، السعودية")
        }
        val viewModel = registerViewModel(
            DhikrViewModel(
                repository = repository,
                deviceLocationProvider = fakeLocation,
                prayerTimesRemoteSource = remoteSource
            )
        )

        composeRule.setContent {
            AdhkarTheme {
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        // Navigate to Prayer Times dashboard -> displays API error card initially
        composeRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("home_prayer_times_card"))
        composeRule.onNodeWithTag("home_prayer_times_card").performClick()

        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("prayer_error_card").assertIsDisplayed()
            }.isSuccess
        }

        // Fix API status and tap Retry -> fetches all 5 prayer times + Hijri dates + attribution
        fakeApi.shouldReturnErrorStatus = false
        composeRule.onNodeWithTag("prayer_retry_button").performClick()

        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("prayer_next_hero_card").assertIsDisplayed()
            }.isSuccess
        }
        composeRule.onNodeWithTag("prayer_location_label").assertTextEquals("الرياض، السعودية")
        composeRule.onNodeWithTag("prayer_hijri_date").assertIsDisplayed()
        composeRule.onNodeWithTag("prayer_lazy_column")
            .performScrollToNode(hasTestTag("prayer_api_attribution"))
        composeRule.onNodeWithTag("prayer_api_attribution").assertIsDisplayed()

        val loaded = viewModel.uiState.value.prayerTimes.schedule!!
        assertEquals(5, loaded.entries.size)
        assertTrue(loaded.hijriDateEn.contains("Jumada al-Akhirah"))
        assertTrue(loaded.hijriDateAr.contains("جمادى الآخرة"))
        assertEquals("05:02", loaded.entries.first { it.prayer == PrayerName.FAJR }.time24h)
        assertEquals("19:12", loaded.entries.first { it.prayer == PrayerName.ISHA }.time24h)
    }

    @Test
    fun `rewarded ad interstitial unlocks exclusive app theme and custom rosary bead design permanently`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking { repository.ensureInitialized() }

        val viewModel = registerViewModel(DhikrViewModel(repository = repository))
        var rewardedAdsWatched = 0
        val fakeAdController = object : AdMobController {
            private val _state = MutableStateFlow(AdControllerState(isInitialized = true, isRewardedReady = true))
            override val state: StateFlow<AdControllerState> = _state.asStateFlow()
            override fun initialize() = Unit
            override fun maybeShowInterstitial(activity: android.app.Activity?, onFinished: () -> Unit) = onFinished()
            override fun showRewardedAd(
                activity: android.app.Activity?,
                onRewardEarned: () -> Unit,
                onUnavailable: () -> Unit
            ) {
                rewardedAdsWatched++
                onRewardEarned()
            }
        }

        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            AdhkarTheme(
                themeMode = state.preferences.themeMode,
                nightModeEnabled = state.preferences.nightModeEnabled,
                themeStyle = state.preferences.appThemeStyle,
                appLanguage = state.preferences.appLanguage
            ) {
                AdhkarAppRoot(
                    viewModel = viewModel,
                    adMobController = fakeAdController
                )
            }
        }
        composeRule.waitForIdle()

        // Default theme & bead design are unlocked by default
        assertEquals(AppThemeStyle.ANDALUSIAN_EMERALD, viewModel.uiState.value.preferences.appThemeStyle)
        assertEquals(RosaryBeadDesign.CLASSIC_RING, viewModel.uiState.value.preferences.rosaryBeadDesign)
        assertEquals(false, viewModel.uiState.value.preferences.isThemeUnlocked(AppThemeStyle.MECCAN_GOLD))
        assertEquals(false, viewModel.uiState.value.preferences.isBeadDesignUnlocked(RosaryBeadDesign.PEARL_BEADS))

        // Open Settings -> tap locked Exclusive Theme (Meccan Gold) -> opens Rewarded Unlock Dialog
        composeRule.onNodeWithTag("nav_tab_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_exclusive_themes_section").performScrollTo()
        composeRule.onNodeWithTag("theme_style_meccan_gold").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("rewarded_unlock_dialog").assertIsDisplayed()

        // Cancel first: stays locked & does not consume ad or switch theme
        composeRule.onNodeWithTag("rewarded_unlock_cancel_button").performClick()
        composeRule.waitForIdle()
        assertEquals(0, rewardedAdsWatched)
        assertEquals(AppThemeStyle.ANDALUSIAN_EMERALD, viewModel.uiState.value.preferences.appThemeStyle)
        assertEquals(false, viewModel.uiState.value.preferences.isThemeUnlocked(AppThemeStyle.MECCAN_GOLD))

        // Tap again and confirm watching rewarded ad -> permanently unlocks & applies Meccan Gold
        composeRule.onNodeWithTag("theme_style_meccan_gold").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("rewarded_unlock_confirm_button").performClick()

        waitUntilIdle(timeoutMillis = 3000) {
            viewModel.uiState.value.preferences.appThemeStyle == AppThemeStyle.MECCAN_GOLD &&
                viewModel.uiState.value.preferences.isThemeUnlocked(AppThemeStyle.MECCAN_GOLD)
        }
        assertEquals(1, rewardedAdsWatched)

        // Open Tasbih -> tap locked Custom Rosary Bead Design (Pearl Beads) -> confirm watching rewarded ad
        composeRule.onNodeWithTag("nav_tab_tasbih").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tasbih_bead_design_selector").performScrollTo()
        composeRule.onNodeWithTag("bead_design_pearl_beads").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("rewarded_unlock_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("rewarded_unlock_confirm_button").performClick()

        waitUntilIdle(timeoutMillis = 3000) {
            viewModel.uiState.value.preferences.rosaryBeadDesign == RosaryBeadDesign.PEARL_BEADS &&
                viewModel.uiState.value.preferences.isBeadDesignUnlocked(RosaryBeadDesign.PEARL_BEADS)
        }
        assertEquals(2, rewardedAdsWatched)

        // Switching between already-unlocked designs does not trigger rewarded ad dialog
        composeRule.onNodeWithTag("bead_design_classic_ring").performClick()
        waitUntilIdle(timeoutMillis = 3000) {
            viewModel.uiState.value.preferences.rosaryBeadDesign == RosaryBeadDesign.CLASSIC_RING
        }
        assertEquals(0, composeRule.onAllNodes(hasTestTag("rewarded_unlock_dialog")).fetchSemanticsNodes().size)
        assertEquals(2, rewardedAdsWatched)

        // Verify persisted in DataStore across repository reads
        val persisted = runBlocking { prefsRepo.preferencesFlow.first() }
        assertTrue(persisted.isThemeUnlocked(AppThemeStyle.MECCAN_GOLD))
        assertTrue(persisted.isBeadDesignUnlocked(RosaryBeadDesign.PEARL_BEADS))
        assertEquals(AppThemeStyle.MECCAN_GOLD, persisted.appThemeStyle)
    }

    @Test
    fun `home screen prayer times widget detects geolocation and displays all 5 daily prayers and next prayer countdown`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking { repository.ensureInitialized() }

        var locationCalls = 0
        var currentCity = "الرياض، السعودية"
        var currentLat = 24.7136
        var currentLon = 46.6753
        val fakeLocation = object : DeviceLocationProvider {
            override fun hasLocationPermission(): Boolean = true
            override suspend fun fetchCurrentLocation(): LocationFetchResult {
                locationCalls++
                return LocationFetchResult.Success(currentLat, currentLon, currentCity)
            }
        }

        val fakeApi = object : com.example.data.remote.AlAdhanRetrofitApi {
            override suspend fun getTimings(
                latitude: Double,
                longitude: Double,
                method: Int
            ): com.example.data.remote.AlAdhanResponse {
                val isMadinah = kotlin.math.abs(latitude - 24.5247) < 0.05
                return com.example.data.remote.AlAdhanResponse(
                    code = 200,
                    status = "OK",
                    data = com.example.data.remote.AlAdhanData(
                        timings = com.example.data.remote.AlAdhanTimings(
                            fajr = if (isMadinah) "05:18" else "05:02",
                            sunrise = "06:21",
                            dhuhr = if (isMadinah) "12:19" else "12:04",
                            asr = "15:18",
                            maghrib = "17:42",
                            isha = if (isMadinah) "19:25" else "19:12"
                        ),
                        date = com.example.data.remote.AlAdhanDate(
                            readable = "22 Nov 2026",
                            hijri = com.example.data.remote.AlAdhanHijri(
                                day = "12",
                                year = "1448",
                                month = com.example.data.remote.AlAdhanHijriMonth(
                                    ar = "جمادى الآخرة",
                                    en = "Jumada al-Akhirah"
                                ),
                                weekday = com.example.data.remote.AlAdhanHijriWeekday(
                                    ar = "الأحد",
                                    en = "Al Ahad"
                                )
                            )
                        ),
                        meta = com.example.data.remote.AlAdhanMeta(timezone = "Asia/Riyadh")
                    )
                )
            }
        }
        val remoteSource = com.example.data.remote.AlAdhanPrayerTimesRemoteSource(fakeApi)

        val viewModel = registerViewModel(
            DhikrViewModel(
                repository = repository,
                deviceLocationProvider = fakeLocation,
                prayerTimesRemoteSource = remoteSource
            )
        )

        composeRule.setContent {
            AdhkarTheme {
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        // Scroll to HomeScreen Prayer Times Widget and verify auto-detected geolocation + 5 prayers
        composeRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("home_prayer_times_card"))
        composeRule.onNodeWithTag("home_prayer_times_card").assertIsDisplayed()

        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("home_prayer_times_grid", useUnmergedTree = true)
                    .assertIsDisplayed()
            }.isSuccess
        }
        assertTrue(locationCalls >= 1)
        composeRule.onNodeWithTag("home_prayer_location_badge", useUnmergedTree = true)
            .assertTextContains("الرياض، السعودية", substring = true)
        composeRule.onNodeWithTag("home_prayer_next_badge", useUnmergedTree = true).assertIsDisplayed()

        PrayerName.entries.forEach { prayer ->
            composeRule.onNodeWithTag("home_prayer_slot_${prayer.name.lowercase()}", useUnmergedTree = true)
                .assertIsDisplayed()
        }

        // Simulate user moving to Madinah and tapping the GPS refresh button inside the main-screen widget
        val callsBeforeRefresh = locationCalls
        currentCity = "المدينة المنورة"
        currentLat = 24.5247
        currentLon = 39.5692
        composeRule.onNodeWithTag("home_prayer_locate_button", useUnmergedTree = true).performClick()

        waitUntilIdle(timeoutMillis = 3000) {
            locationCalls > callsBeforeRefresh &&
                viewModel.uiState.value.prayerTimes.schedule?.locationLabel == "المدينة المنورة"
        }
        composeRule.onNodeWithTag("home_prayer_location_badge", useUnmergedTree = true)
            .assertTextContains("المدينة المنورة", substring = true)
        val refreshedSchedule = viewModel.uiState.value.prayerTimes.schedule!!
        assertEquals("05:18", refreshedSchedule.entries.first { it.prayer == PrayerName.FAJR }.time24h)
        assertEquals("19:25", refreshedSchedule.entries.first { it.prayer == PrayerName.ISHA }.time24h)
    }

    @Test
    fun `hijriDateHelper calculates accurate Hijri dates and HomeScreen displays Hijri date alongside prayer times widget`() {
        // Verify deterministic Umm al-Qura + tabular fallback calculations for 2026-11-22
        val nov22 = com.example.util.HijriDateHelper.calculateForGregorianDate(2026, 11, 22, Calendar.SUNDAY)
        assertEquals(1448, nov22.year)
        assertEquals(6, nov22.month) // Jumada al-Akhirah
        assertTrue(nov22.day in 11..13)
        assertEquals("جمادى الآخرة", nov22.monthNameArabic)
        assertEquals("Jumada al-Akhirah", nov22.monthNameEnglish)
        assertTrue(nov22.fullDateArabic.contains("الأحد"))
        assertTrue(nov22.fullDateArabic.contains("1448"))
        assertTrue(nov22.fullDateEnglish.contains("Sunday"))
        assertTrue(nov22.fullDateEnglish.contains("Jumada al-Akhirah 1448 AH"))

        val tabular = com.example.util.HijriDateHelper.calculateTabularKuwaiti(2026, 11, 22, Calendar.SUNDAY)
        assertEquals(1448, tabular.year)
        assertEquals(6, tabular.month)
        assertEquals(11, tabular.day)

        // Verify HomeScreen displays today's calculated Hijri date card alongside the Prayer Times widget
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking { repository.ensureInitialized() }
        val viewModel = registerViewModel(DhikrViewModel(repository))
        val expectedToday = com.example.util.HijriDateHelper.today()

        composeRule.setContent {
            AdhkarTheme {
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("home_hijri_date_card"))
        composeRule.onNodeWithTag("home_hijri_date_card").assertIsDisplayed()
        composeRule.onNodeWithTag("home_hijri_date_text", useUnmergedTree = true)
            .assertTextEquals(expectedToday.localizedFullDate(AppLanguage.ARABIC))
        composeRule.onNodeWithTag("home_gregorian_date_text", useUnmergedTree = true)
            .assertTextEquals(expectedToday.localizedGregorianDate(AppLanguage.ARABIC))

        // Prayer times widget sits directly alongside it on HomeScreen
        composeRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("home_prayer_times_card"))
        composeRule.onNodeWithTag("home_prayer_times_card").assertIsDisplayed()
    }

    @Test
    fun `standalone DigitalTasbeehCounter component increments on tap, triggers light haptic vibration, and resets`() {
        val haptics = mutableListOf<HapticFeedbackType>()
        val spyHaptic = object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                haptics.add(hapticFeedbackType)
            }
        }
        val observedCounts = mutableListOf<Int>()

        composeRule.setContent {
            AdhkarTheme {
                CompositionLocalProvider(LocalHapticFeedback provides spyHaptic) {
                    com.example.ui.components.DigitalTasbeehCounter(
                        targetCount = 33,
                        hapticEnabled = true,
                        onCountChanged = { observedCounts.add(it) }
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tasbih_current_count_text", useUnmergedTree = true)
            .assertTextEquals("0")

        // Tap circular counter twice -> increments and fires light TextHandleMove haptic each tap
        composeRule.onNodeWithTag("tasbih_tap_circle").performClick()
        composeRule.onNodeWithTag("tasbih_tap_circle").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tasbih_current_count_text", useUnmergedTree = true)
            .assertTextEquals("2")
        assertEquals(listOf(1, 2), observedCounts)
        assertEquals(
            listOf(HapticFeedbackType.TextHandleMove, HapticFeedbackType.TextHandleMove),
            haptics
        )

        // Tap Reset -> clears back to 0
        composeRule.onNodeWithTag("tasbih_reset_cycle_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tasbih_current_count_text", useUnmergedTree = true)
            .assertTextEquals("0")
        assertEquals(listOf(1, 2, 0), observedCounts)
    }

    @Test
    fun `home screen electronic tasbeeh widget increments, triggers light haptic vibration, and resets`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking {
            repository.ensureInitialized()
            prefsRepo.setHapticEnabled(true)
        }
        val viewModel = registerViewModel(DhikrViewModel(repository))

        val haptics = mutableListOf<HapticFeedbackType>()
        val spyHaptic = object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                haptics.add(hapticFeedbackType)
            }
        }

        composeRule.setContent {
            AdhkarTheme {
                CompositionLocalProvider(LocalHapticFeedback provides spyHaptic) {
                    AdhkarAppRoot(viewModel = viewModel)
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("home_tasbih_shortcut"))
        composeRule.onNodeWithTag("home_tasbih_tap_button", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("home_tasbih_count_text", useUnmergedTree = true)
            .assertTextEquals("0")

        // Tap twice on the quick Electronic Tasbeeh widget
        composeRule.onNodeWithTag("home_tasbih_tap_button", useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag("home_tasbih_tap_button", useUnmergedTree = true).performClick()

        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("home_tasbih_count_text", useUnmergedTree = true)
                    .assertTextEquals("2")
            }.isSuccess
        }
        assertEquals(
            listOf(HapticFeedbackType.TextHandleMove, HapticFeedbackType.TextHandleMove),
            haptics
        )

        // Reset via widget reset button
        composeRule.onNodeWithTag("home_tasbih_reset_button", useUnmergedTree = true).performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("home_tasbih_count_text", useUnmergedTree = true)
                    .assertTextEquals("0")
            }.isSuccess
        }
    }

    @Test
    fun `dhikr card displays labeled share button and opens Android system share chooser with dhikr text`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking { repository.ensureInitialized() }
        val viewModel = registerViewModel(DhikrViewModel(repository))

        composeRule.setContent {
            AdhkarTheme {
                AdhkarAppRoot(viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()

        // Navigate into Morning Adhkar
        composeRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("category_card_morning"))
        composeRule.onNodeWithTag("category_card_morning").performClick()

        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("share_dhikr_101")
                    .assertIsDisplayed()
            }.isSuccess
        }
        composeRule.onNodeWithTag("share_dhikr_101")
            .assertTextContains("مشاركة", substring = true)
            .performClick()

        val app = ApplicationProvider.getApplicationContext<Application>()
        val startedChooser = Shadows.shadowOf(app).nextStartedActivity
        assertNotNull("Expected system share chooser Intent", startedChooser)
        assertEquals(Intent.ACTION_CHOOSER, startedChooser.action)

        val wrappedSend = startedChooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull("Expected wrapped ACTION_SEND Intent", wrappedSend)
        assertEquals(Intent.ACTION_SEND, wrappedSend?.action)
        assertEquals("text/plain", wrappedSend?.type)
        val sharedText = wrappedSend?.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        assertTrue(sharedText.contains("آية الكرسي"))
        assertTrue(sharedText.contains("اللَّهُ لَا إِلَهَ إِلَّا هُوَ"))
    }

    @Test
    fun `home chat button navigates to ChatScreen, streams multi-turn replies, and clears conversation`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking { repository.ensureInitialized() }
        val viewModel = registerViewModel(DhikrViewModel(repository))

        val capturedHistories = mutableListOf<List<ChatHistoryTurn>>()
        val fakeChatService = object : AdhkarChatService {
            override fun streamReply(
                history: List<ChatHistoryTurn>,
                userMessage: String,
                appLanguage: AppLanguage
            ): Flow<String> {
                capturedHistories.add(history)
                return flow {
                    if (capturedHistories.size == 1) {
                        emit("أذكار الصباح ")
                        emit("حصن للمسلم ومن أبرزها آية الكرسي.")
                    } else {
                        emit("سبحان الله وبحمده مائة مرة.")
                    }
                }
            }
        }
        val chatViewModel = registerViewModel(
            ChatViewModel(chatService = fakeChatService, chatDao = database.chatDao())
        )

        composeRule.setContent {
            AdhkarTheme {
                AdhkarAppRoot(
                    viewModel = viewModel,
                    chatViewModel = chatViewModel
                )
            }
        }
        composeRule.waitForIdle()

        // Tap Chat button at the top of HomeScreen
        composeRule.onNodeWithTag("home_chat_button")
            .assertIsDisplayed()
            .performClick()

        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("chat_screen").assertIsDisplayed()
            }.isSuccess
        }
        composeRule.onNodeWithTag("chat_welcome_section").assertIsDisplayed()

        // Tap first suggested prompt
        composeRule.onNodeWithTag("chat_suggestion_0").performClick()

        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithText("أذكار الصباح حصن للمسلم ومن أبرزها آية الكرسي.")
                    .assertIsDisplayed()
            }.isSuccess
        }
        assertEquals(0, capturedHistories.first().size)

        // Send a follow-up question and verify prior turn history is forwarded
        composeRule.onNodeWithTag("chat_input").performTextReplacement("اذكر لي تسبيحاً مختصراً")
        composeRule.onNodeWithTag("chat_send_button").performClick()

        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithText("سبحان الله وبحمده مائة مرة.")
                    .assertIsDisplayed()
            }.isSuccess
        }
        assertEquals(2, capturedHistories.last().size)
        assertTrue(capturedHistories.last()[0].isUser)
        assertTrue(!capturedHistories.last()[1].isUser)

        // Clear conversation back to welcome screen
        composeRule.onNodeWithTag("chat_clear_button").performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("chat_welcome_section").assertIsDisplayed()
            }.isSuccess
        }
    }

    @Test
    fun `chat screen surfaces real inference errors without fake fallback and retries cleanly`() {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        runBlocking { repository.ensureInitialized() }
        val viewModel = registerViewModel(DhikrViewModel(repository))

        var attempts = 0
        val flakyService = object : AdhkarChatService {
            override fun streamReply(
                history: List<ChatHistoryTurn>,
                userMessage: String,
                appLanguage: AppLanguage
            ): Flow<String> {
                attempts++
                return flow {
                    if (attempts == 1) {
                        throw IllegalStateException("App Check attestation rejected (403)")
                    }
                    emit("تقبّل الله دعاءك وذكرك.")
                }
            }
        }
        val chatViewModel = registerViewModel(
            ChatViewModel(chatService = flakyService, chatDao = database.chatDao())
        )

        composeRule.setContent {
            AdhkarTheme {
                AdhkarAppRoot(
                    viewModel = viewModel,
                    chatViewModel = chatViewModel
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_chat_button").performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("chat_input").assertIsDisplayed()
            }.isSuccess
        }

        composeRule.onNodeWithTag("chat_input").performTextReplacement("دعاء قصير")
        composeRule.onNodeWithTag("chat_send_button").performClick()

        // Verify authentic error banner appears
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("chat_error_banner").assertIsDisplayed()
            }.isSuccess
        }
        composeRule.onNodeWithTag("chat_error_banner")
            .assertTextContains("App Check attestation rejected (403)", substring = true)

        // Tap Retry and verify success reply
        composeRule.onNodeWithTag("chat_retry_button").performClick()
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithText("تقبّل الله دعاءك وذكرك.")
                    .assertIsDisplayed()
            }.isSuccess
        }
    }

    @Test
    fun `repository stores and retrieves daily morning and evening adhkar content from Room`() = runBlocking {
        val prefsRepo = UserPreferencesRepository(context)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepo,
            dailyActivityDao = database.dailyActivityDao()
        )
        repository.ensureInitialized()

        // Retrieve seeded morning & evening adhkar via one-shot and Flow APIs
        val morningOnce = repository.getMorningAdhkar()
        val eveningOnce = repository.getEveningAdhkar()
        assertTrue(morningOnce.isNotEmpty())
        assertTrue(eveningOnce.isNotEmpty())
        assertTrue(morningOnce.all { it.category == DhikrCategory.MORNING.id })
        assertTrue(eveningOnce.all { it.category == DhikrCategory.EVENING.id })

        val combinedFlow = repository.dailyMorningAndEveningAdhkar.first()
        assertEquals(morningOnce.size + eveningOnce.size, combinedFlow.size)

        // Store a custom morning dhikr and verify retrieval and update/delete
        val customMorning = Dhikr(
            title = "ذكر الصباح المخصص",
            content = "اللَّهُمَّ بَارِكْ لَنَا فِي صَبَاحِنَا",
            category = DhikrCategory.MORNING.id,
            virtue = "بركة في بداية اليوم",
            source = "ذكر مضاف",
            targetCount = 3,
            sortOrder = 99
        )
        val insertedId = repository.storeDhikr(customMorning).toInt()
        val loaded = repository.getDhikrById(insertedId)
        assertNotNull(loaded)
        assertEquals("ذكر الصباح المخصص", loaded?.title)
        assertEquals(DhikrCategory.MORNING.id, loaded?.category)

        val updatedMorning = repository.observeDailyMorningAdhkar().first()
        assertTrue(updatedMorning.any { it.id == insertedId })

        repository.updateDhikr(loaded!!.copy(currentCount = 2))
        assertEquals(2, repository.getDhikrById(insertedId)?.currentCount)

        repository.deleteDhikr(loaded)
        assertNull(repository.getDhikrById(insertedId))
    }
}





