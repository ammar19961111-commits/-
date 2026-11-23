package com.example.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.model.DhikrCategory
import com.example.data.model.RewardedUnlockTarget
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DhikrListScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PrayerTimesScreen
import com.example.ui.screens.QiblaScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.TasbihScreen
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.viewmodel.AdhkarUiState
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.DhikrViewModel

@Composable
fun AdhkarNavHost(
    navController: NavHostController,
    viewModel: DhikrViewModel,
    chatViewModel: ChatViewModel,
    uiState: AdhkarUiState,
    onNavigateToTab: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    copiedMessage: String,
    resetDoneMessage: String,
    isRewardedAdLoading: Boolean = false,
    onWatchRewardedAd: () -> Unit = {},
    onRequestUnlockTarget: (RewardedUnlockTarget) -> Unit = {},
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AdhkarRoutes.HOME,
        enterTransition = { fadeIn(animationSpec = tween(220)) },
        exitTransition = { fadeOut(animationSpec = tween(180)) },
        popEnterTransition = { fadeIn(animationSpec = tween(220)) },
        popExitTransition = { fadeOut(animationSpec = tween(180)) },
        modifier = modifier
    ) {
        composable(AdhkarRoutes.HOME) {
            val reflectionCopiedMessage = stringResource(R.string.reflection_copied_toast)
            HomeScreen(
                uiState = uiState,
                onCategoryClick = { category ->
                    navController.navigate(AdhkarRoutes.dhikrListRoute(category.id))
                },
                onNavigateToTasbih = {
                    onNavigateToTab(AdhkarRoutes.TASBIH)
                },
                onNavigateToStats = {
                    onNavigateToTab(AdhkarRoutes.STATS)
                },
                onNavigateToFavorites = {
                    onNavigateToTab(AdhkarRoutes.FAVORITES)
                },
                onNavigateToPrayerTimes = {
                    navController.navigate(AdhkarRoutes.PRAYER_TIMES)
                },
                onNavigateToQibla = {
                    navController.navigate(AdhkarRoutes.QIBLA)
                },
                onNavigateToSettings = {
                    onNavigateToTab(AdhkarRoutes.SETTINGS)
                },
                onNavigateToChat = {
                    navController.navigate(AdhkarRoutes.CHAT) {
                        launchSingleTop = true
                    }
                },
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onIncrementDhikr = viewModel::onIncrementDhikr,
                onToggleFavorite = viewModel::onToggleFavorite,
                onResetDhikr = viewModel::onResetDhikr,
                onShowCopiedToast = { onShowMessage(copiedMessage) },
                onShowReflectionCopiedToast = { onShowMessage(reflectionCopiedMessage) },
                onRefreshDailyReflection = viewModel::refreshDailyReflection,
                onSelectQuranTrack = viewModel::onSelectQuranTrack,
                onToggleQuranPlayPause = viewModel::onToggleQuranPlayPause,
                onNextQuranTrack = viewModel::onNextQuranTrack,
                onPreviousQuranTrack = viewModel::onPreviousQuranTrack,
                onSeekQuranTo = viewModel::onSeekQuranTo,
                onToggleQuranRepeat = viewModel::onToggleQuranRepeat,
                onToggleDhikrAudio = viewModel::onToggleDhikrAudio,
                onSeekDhikrAudio = viewModel::onSeekDhikrAudio,
                onCycleDhikrAudioSpeed = viewModel::onCycleDhikrAudioSpeed,
                onToggleDhikrAudioRepeat = viewModel::onToggleDhikrAudioRepeat,
                onRetryDhikrAudio = viewModel::onRetryDhikrAudio,
                onStopDhikrAudio = viewModel::onStopDhikrAudio,
                onEnsurePrayerTimesLoaded = viewModel::onEnsurePrayerTimesLoaded,
                onDetectPrayerLocation = viewModel::onDetectLocationAndFetchPrayerTimes,
                onPrayerLocationPermissionDenied = viewModel::onLocationPermissionDenied,
                onRefreshPrayerTimes = viewModel::onRefreshPrayerTimes,
                onQuickIncrementTasbih = viewModel::incrementTasbeeh,
                onQuickResetTasbih = viewModel::onResetTasbihCycle
            )
        }

        composable(
            route = AdhkarRoutes.DHIKR_LIST,
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: DhikrCategory.MORNING.id
            val category = DhikrCategory.fromId(categoryId)
            val categoryAdhkar by remember(categoryId) {
                viewModel.fetchDhikrByCategory(categoryId)
            }.collectAsStateWithLifecycle(initialValue = emptyList())

            val categoryReminder = uiState.preferences.reminders.find { it.category == category }
            DhikrListScreen(
                category = category,
                adhkar = categoryAdhkar,
                fontSizeSp = uiState.preferences.fontSizeSp,
                autoAdvance = uiState.preferences.autoAdvance,
                onBack = {
                    if (categoryAdhkar.isNotEmpty() && categoryAdhkar.all { it.isCompleted }) {
                        viewModel.onNaturalAdTransition()
                    }
                    navController.popBackStack()
                },
                onIncrementDhikr = viewModel::onIncrementDhikr,
                onToggleFavorite = viewModel::onToggleFavorite,
                onResetDhikr = viewModel::onResetDhikr,
                onResetCategory = {
                    if (categoryAdhkar.isNotEmpty() && categoryAdhkar.all { it.isCompleted }) {
                        viewModel.onNaturalAdTransition()
                    }
                    viewModel.onResetCategory(categoryId)
                },
                onShowCopiedToast = { onShowMessage(copiedMessage) },
                reminder = categoryReminder,
                onUpdateReminder = { enabled, hour, minute ->
                    viewModel.onUpdateReminder(category, enabled, hour, minute)
                },
                audioState = uiState.dhikrAudio,
                onPlayDisplayedAdhkar = viewModel::onPlayDisplayedAdhkar,
                onToggleDhikrAudio = viewModel::onToggleDhikrAudio,
                onToggleAudioBarPlayPause = viewModel::onToggleDhikrAudioBarPlayPause,
                onNextAudio = viewModel::onNextDhikrAudio,
                onPreviousAudio = viewModel::onPreviousDhikrAudio,
                onSeekAudio = viewModel::onSeekDhikrAudio,
                onCycleAudioSpeed = viewModel::onCycleDhikrAudioSpeed,
                onToggleAudioRepeat = viewModel::onToggleDhikrAudioRepeat,
                onRetryAudio = viewModel::onRetryDhikrAudio,
                onStopAudio = viewModel::onStopDhikrAudio,
                initialFocusMode = uiState.preferences.focusModeEnabled,
                onFocusModeChange = viewModel::onFocusModeToggle
            )
        }

        composable(AdhkarRoutes.TASBIH) {
            TasbihScreen(
                uiState = uiState,
                onSelectPreset = viewModel::onSelectTasbih,
                onIncrementTasbih = viewModel::incrementTasbeeh,
                onResetCycle = viewModel::onResetTasbihCycle,
                onUpdateTarget = viewModel::onUpdateTasbihTarget,
                onUpdateDailyTarget = viewModel::onDailyTasbihTargetChange,
                onToggleHaptic = viewModel::onHapticToggle,
                onToggleSound = viewModel::onSoundToggle,
                onAddCustomTasbih = viewModel::onAddCustomTasbih,
                onDeleteCustomTasbih = viewModel::onDeleteTasbihPreset,
                onSelectTimerDuration = viewModel::onSelectSessionTimerDuration,
                onStartOrResumeTimer = viewModel::onStartOrResumeSessionTimer,
                onPauseTimer = viewModel::onPauseSessionTimer,
                onResetTimer = viewModel::onResetSessionTimer,
                onToggleTimerSound = viewModel::onToggleSessionTimerSound,
                onSetAutoCountTarget = viewModel::onSetAutoCountTarget,
                onSelectAutoCountPace = viewModel::onSelectAutoCountPace,
                onStartOrResumeAutoCount = viewModel::onStartOrResumeAutoCount,
                onPauseAutoCount = viewModel::onPauseAutoCount,
                onResetAutoCount = viewModel::onResetAutoCount,
                onToggleAutoCountSound = viewModel::onToggleAutoCountSoundAlert,
                onToggleAutoCountHaptic = viewModel::onToggleAutoCountHapticAlert,
                onSelectTargetVibrationPattern = viewModel::onSelectTargetVibrationPattern,
                onPreviewTargetVibration = viewModel::onPreviewTargetVibration,
                onSelectTargetSoundEffect = viewModel::onSelectTargetSoundEffect,
                onPreviewTargetSound = viewModel::onPreviewTargetSound,
                onSelectBeadDesign = { design ->
                    if (uiState.preferences.isBeadDesignUnlocked(design)) {
                        viewModel.onSelectRosaryBeadDesign(design)
                    } else {
                        onRequestUnlockTarget(RewardedUnlockTarget.BeadDesign(design))
                    }
                },
                onShowMessage = onShowMessage
            )
        }

        composable(AdhkarRoutes.STATS) {
            StatsScreen(
                uiState = uiState,
                onPeriodChange = viewModel::onStatsPeriodChange,
                onSeriesFilterChange = viewModel::onStatsSeriesFilterChange,
                onShowMessage = onShowMessage
            )
        }

        composable(AdhkarRoutes.FAVORITES) {
            FavoritesScreen(
                uiState = uiState,
                onIncrementDhikr = viewModel::onIncrementDhikr,
                onToggleFavorite = viewModel::onToggleFavorite,
                onResetDhikr = viewModel::onResetDhikr,
                onShowCopiedToast = { onShowMessage(copiedMessage) },
                onToggleDhikrAudio = viewModel::onToggleDhikrAudio,
                onSeekAudio = viewModel::onSeekDhikrAudio,
                onCycleAudioSpeed = viewModel::onCycleDhikrAudioSpeed,
                onToggleAudioRepeat = viewModel::onToggleDhikrAudioRepeat,
                onRetryAudio = viewModel::onRetryDhikrAudio,
                onStopAudio = viewModel::onStopDhikrAudio
            )
        }

        composable(AdhkarRoutes.SETTINGS) {
            SettingsScreen(
                uiState = uiState,
                onFontSizeChange = viewModel::onFontSizeChange,
                onLanguageChange = viewModel::onLanguageChange,
                onThemeModeChange = viewModel::onThemeModeChange,
                onSelectThemeStyle = { style ->
                    if (uiState.preferences.isThemeUnlocked(style)) {
                        viewModel.onSelectThemeStyle(style)
                    } else {
                        onRequestUnlockTarget(RewardedUnlockTarget.ThemeStyle(style))
                    }
                },
                onSelectBeadDesign = { design ->
                    if (uiState.preferences.isBeadDesignUnlocked(design)) {
                        viewModel.onSelectRosaryBeadDesign(design)
                    } else {
                        onRequestUnlockTarget(RewardedUnlockTarget.BeadDesign(design))
                    }
                },
                onNightModeToggle = viewModel::onNightModeToggle,
                onDailyTotalGoalChange = viewModel::onDailyTotalGoalChange,
                onDailyTasbihTargetChange = viewModel::onDailyTasbihTargetChange,
                onHapticToggle = viewModel::onHapticToggle,
                onSoundToggle = viewModel::onSoundToggle,
                onAutoAdvanceToggle = viewModel::onAutoAdvanceToggle,
                onUpdateReminder = viewModel::onUpdateReminder,
                onResetAllToday = {
                    viewModel.onResetAllToday()
                    onShowMessage(resetDoneMessage)
                },
                onCreateBackupJson = { viewModel.createBackupJson() },
                onRestoreBackup = viewModel::restoreFromBackup,
                isRewardedAdLoading = isRewardedAdLoading,
                onWatchRewardedAd = onWatchRewardedAd,
                onShowMessage = onShowMessage
            )
        }

        composable(AdhkarRoutes.PRAYER_TIMES) {
            PrayerTimesScreen(
                prayerState = uiState.prayerTimes,
                onBack = { navController.popBackStack() },
                onEnsureLoaded = viewModel::onEnsurePrayerTimesLoaded,
                onDetectLocationAndRefresh = viewModel::onDetectLocationAndFetchPrayerTimes,
                onLocationPermissionDenied = viewModel::onLocationPermissionDenied,
                onSelectCity = viewModel::onSelectPrayerCity,
                onSelectMethod = viewModel::onSelectPrayerMethod,
                onRetry = viewModel::onRefreshPrayerTimes,
                onNavigateToQibla = {
                    navController.navigate(AdhkarRoutes.QIBLA)
                }
            )
        }

        composable(AdhkarRoutes.QIBLA) {
            QiblaScreen(
                latitude = uiState.preferences.prayerLatitude,
                longitude = uiState.preferences.prayerLongitude,
                locationLabel = uiState.preferences.prayerLocationLabel,
                hasLocationPermission = uiState.prayerTimes.hasLocationPermission,
                hapticEnabled = uiState.preferences.hapticEnabled,
                sensorSource = viewModel.compassSensorSource,
                onBack = { navController.popBackStack() },
                onDetectLocation = viewModel::onDetectLocationAndFetchPrayerTimes,
                onLocationPermissionDenied = viewModel::onLocationPermissionDenied,
                onSelectCity = { city -> viewModel.onSelectPrayerCity(city) }
            )
        }

        composable(AdhkarRoutes.CHAT) {
            val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()
            val appLanguage = LocalAppLanguage.current
            ChatScreen(
                uiState = chatState,
                onInputChange = chatViewModel::onInputChange,
                onSendMessage = { chatViewModel.sendMessage(appLanguage) },
                onSendSuggestedPrompt = { prompt ->
                    chatViewModel.sendMessage(appLanguage = appLanguage, overridePrompt = prompt)
                },
                onRetry = { chatViewModel.retryLastFailed(appLanguage) },
                onDismissError = chatViewModel::dismissError,
                onClearConversation = chatViewModel::clearConversation,
                onShowMessage = onShowMessage,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
