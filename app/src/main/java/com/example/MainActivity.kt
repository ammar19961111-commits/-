package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ads.AdMobController
import com.example.ads.AndroidAdMobController
import com.example.ads.NoOpAdMobController
import com.example.ads.findActivity
import com.example.ai.FirebaseAdhkarChatService
import com.example.ai.configureAppCheck
import com.example.audio.ExoDhikrAudioPlayerController
import com.example.audio.ExoQuranPlayerController
import com.example.data.local.AdhkarDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.RewardedUnlockTarget
import com.example.data.remote.AlAdhanPrayerTimesRemoteSource
import com.example.data.repository.AdhkarRepository
import com.example.location.AndroidDeviceLocationProvider
import com.example.notifications.AndroidReminderScheduler
import com.example.notifications.DhikrNotificationHelper
import com.example.qibla.AndroidQiblaSensorSource
import com.example.ui.components.AdMobBannerBar
import com.example.ui.components.DhikrRecitationBar
import com.example.ui.components.QuranMiniPlayerBar
import com.example.ui.components.RewardedUnlockDialog
import com.example.ui.navigation.AdhkarNavHost
import com.example.ui.navigation.AdhkarRoutes
import com.example.ui.navigation.BottomNavTab
import com.example.ui.theme.AdhkarTheme
import com.example.ui.viewmodel.AdEvent
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.ChimeEvent
import com.example.ui.viewmodel.DhikrViewModel
import com.example.ui.viewmodel.HapticEvent
import com.example.util.HapticHelper
import com.example.util.SoundHelper
import com.example.widget.TasbihWidgetProvider
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var pendingCategoryId by mutableStateOf<String?>(null)
    private var pendingOpenTab by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        DhikrNotificationHelper.ensureChannel(applicationContext)
        pendingCategoryId = intent?.getStringExtra(DhikrNotificationHelper.EXTRA_CATEGORY_ID)
        pendingOpenTab = intent?.getStringExtra(TasbihWidgetProvider.EXTRA_OPEN_TAB)

        val database = AdhkarDatabase.getInstance(applicationContext)
        val prefsRepository = UserPreferencesRepository(applicationContext)
        val repository = AdhkarRepository(
            dhikrDao = database.dhikrDao(),
            counterDao = database.counterDao(),
            prefsRepo = prefsRepository,
            dailyActivityDao = database.dailyActivityDao()
        )
        val reminderScheduler = AndroidReminderScheduler(applicationContext)

        setContent {
            val quranPlayerController = remember(applicationContext) {
                ExoQuranPlayerController(applicationContext)
            }
            val dhikrAudioController = remember(applicationContext) {
                ExoDhikrAudioPlayerController(applicationContext)
            }
            val locationProvider = remember(applicationContext) {
                AndroidDeviceLocationProvider(applicationContext)
            }
            val prayerTimesRemoteSource = remember {
                AlAdhanPrayerTimesRemoteSource()
            }
            val adMobController = remember(applicationContext) {
                AndroidAdMobController(applicationContext)
            }
            val qiblaSensorSource = remember(applicationContext) {
                AndroidQiblaSensorSource(applicationContext)
            }
            LaunchedEffect(adMobController) {
                adMobController.initialize()
            }
            val viewModel: DhikrViewModel = viewModel(
                factory = DhikrViewModel.provideFactory(
                    repository = repository,
                    reminderScheduler = reminderScheduler,
                    quranPlayerController = quranPlayerController,
                    deviceLocationProvider = locationProvider,
                    prayerTimesRemoteSource = prayerTimesRemoteSource,
                    dhikrAudioController = dhikrAudioController,
                    qiblaSensorSource = qiblaSensorSource
                )
            )
            val chatViewModel: ChatViewModel = viewModel(
                factory = ChatViewModel.provideFactory(
                    chatService = FirebaseAdhkarChatService(),
                    chatDao = database.chatDao()
                )
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            AdhkarTheme(
                themeMode = uiState.preferences.themeMode,
                nightModeEnabled = uiState.preferences.nightModeEnabled,
                themeStyle = uiState.preferences.appThemeStyle,
                appLanguage = uiState.preferences.appLanguage
            ) {
                AdhkarAppRoot(
                    viewModel = viewModel,
                    pendingCategoryId = pendingCategoryId,
                    onConsumedPendingCategory = { pendingCategoryId = null },
                    pendingOpenTab = pendingOpenTab,
                    onConsumedPendingOpenTab = { pendingOpenTab = null },
                    adMobController = adMobController,
                    chatViewModel = chatViewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val catId = intent.getStringExtra(DhikrNotificationHelper.EXTRA_CATEGORY_ID)
        if (!catId.isNullOrBlank()) {
            pendingCategoryId = catId
        }
        val openTab = intent.getStringExtra(TasbihWidgetProvider.EXTRA_OPEN_TAB)
        if (!openTab.isNullOrBlank()) {
            pendingOpenTab = openTab
        }
    }
}

@Composable
fun AdhkarAppRoot(
    viewModel: DhikrViewModel,
    pendingCategoryId: String? = null,
    onConsumedPendingCategory: () -> Unit = {},
    pendingOpenTab: String? = null,
    onConsumedPendingOpenTab: () -> Unit = {},
    adMobController: AdMobController = NoOpAdMobController,
    chatViewModel: ChatViewModel? = null
) {
    val resolvedChatViewModel: ChatViewModel = chatViewModel ?: viewModel(
        factory = ChatViewModel.provideFactory()
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val adControllerState by adMobController.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleResumeEffect(uiState.isAdFreeActive) {
        viewModel.onRefreshAdFreeStatus()
        adMobController.preloadIfNeeded(uiState.isAdFreeActive)
        onPauseOrDispose { }
    }

    LaunchedEffect(
        uiState.todayTasbihSum,
        uiState.dailyTasbihTarget,
        uiState.selectedTasbih?.phrase,
        uiState.preferences.appLanguage
    ) {
        if (!uiState.isLoading) {
            DhikrNotificationHelper.ensureChannel(context, uiState.preferences.appLanguage)
            TasbihWidgetProvider.updateAllWidgets(
                context = context,
                phrase = uiState.selectedTasbih?.phrase ?: "سُبْحَانَ اللَّهِ",
                todayCount = uiState.todayTasbihSum,
                dailyTarget = uiState.dailyTasbihTarget,
                appLanguage = uiState.preferences.appLanguage
            )
        }
    }

    LaunchedEffect(viewModel) {
        launch {
            viewModel.hapticEvents.collect { event ->
                when (event) {
                    HapticEvent.Tap -> HapticHelper.performTap(context)
                    HapticEvent.Milestone -> HapticHelper.performTargetPattern(
                        context = context,
                        pattern = viewModel.uiState.value.preferences.tasbihTargetVibrationPattern
                    )
                    is HapticEvent.PreviewPattern -> HapticHelper.performTargetPattern(
                        context = context,
                        pattern = event.pattern
                    )
                }
            }
        }
        launch {
            viewModel.chimeEvents.collect { event ->
                when (event) {
                    ChimeEvent.DailyGoalCompleted,
                    ChimeEvent.AutoCountTargetReached -> SoundHelper.playTargetSoundEffect(
                        context = context,
                        effect = viewModel.uiState.value.preferences.tasbihTargetSoundEffect
                    )
                    ChimeEvent.SessionTimerFinished -> SoundHelper.playSessionEndAlert(context)
                    is ChimeEvent.TargetSoundEffect -> SoundHelper.playTargetSoundEffect(
                        context = context,
                        effect = event.effect
                    )
                }
            }
        }
        launch {
            viewModel.adEvents.collect { event ->
                when (event) {
                    AdEvent.TryShowInterstitial -> {
                        if (!viewModel.uiState.value.isAdFreeActive) {
                            adMobController.maybeShowInterstitial(context.findActivity())
                        }
                    }
                }
            }
        }
    }

    val navController = rememberNavController()

    LaunchedEffect(pendingCategoryId) {
        if (!pendingCategoryId.isNullOrBlank()) {
            navController.navigate(AdhkarRoutes.dhikrListRoute(pendingCategoryId)) {
                launchSingleTop = true
            }
            onConsumedPendingCategory()
        }
    }
    LaunchedEffect(pendingOpenTab) {
        if (pendingOpenTab == TasbihWidgetProvider.TAB_TASBIH) {
            navController.navigate(AdhkarRoutes.TASBIH) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            onConsumedPendingOpenTab()
        }
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copiedMessage = stringResource(R.string.copied_toast)
    val resetDoneMessage = stringResource(R.string.settings_reset_done)
    val rewardGrantedMessage = stringResource(R.string.ads_reward_granted_toast)
    val rewardExtendedMessage = stringResource(R.string.ads_reward_extended_toast)
    val rewardUnavailableMessage = stringResource(R.string.ads_reward_unavailable_toast)
    var pendingUnlockTarget by remember { mutableStateOf<RewardedUnlockTarget?>(null) }

    val onWatchRewardedAd: () -> Unit = {
        val wasAlreadyAdFree = viewModel.uiState.value.isAdFreeActive
        adMobController.showRewardedAd(
            activity = context.findActivity(),
            onRewardEarned = {
                viewModel.onGrantAdFreeReward()
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (wasAlreadyAdFree) rewardExtendedMessage else rewardGrantedMessage
                    )
                }
            },
            onUnavailable = {
                scope.launch { snackbarHostState.showSnackbar(rewardUnavailableMessage) }
            }
        )
    }

    val currentPendingUnlock = pendingUnlockTarget
    if (currentPendingUnlock != null) {
        RewardedUnlockDialog(
            target = currentPendingUnlock,
            isRewardedAdLoading = adControllerState.isRewardedLoading,
            onConfirmWatchAd = {
                val targetToUnlock = currentPendingUnlock
                adMobController.showRewardedAd(
                    activity = context.findActivity(),
                    onRewardEarned = {
                        pendingUnlockTarget = null
                        when (targetToUnlock) {
                            is RewardedUnlockTarget.ThemeStyle -> {
                                viewModel.onUnlockAndSelectThemeStyle(targetToUnlock.style)
                                val name = context.getString(targetToUnlock.style.titleRes)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.ads_unlock_theme_granted_toast, name)
                                    )
                                }
                            }
                            is RewardedUnlockTarget.BeadDesign -> {
                                viewModel.onUnlockAndSelectBeadDesign(targetToUnlock.design)
                                val name = context.getString(targetToUnlock.design.titleRes)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.ads_unlock_bead_granted_toast, name)
                                    )
                                }
                            }
                        }
                    },
                    onUnavailable = {
                        pendingUnlockTarget = null
                        scope.launch { snackbarHostState.showSnackbar(rewardUnavailableMessage) }
                    }
                )
            },
            onDismiss = { pendingUnlockTarget = null }
        )
    }

    val topLevelRoutes = remember { BottomNavTab.entries.map { it.route }.toSet() }
    val showTopLevelNav = currentRoute in topLevelRoutes
    val isDhikrListRoute = currentRoute?.startsWith("dhikr_list/") == true
    val showGlobalDhikrAudioBar = !isDhikrListRoute &&
        uiState.dhikrAudio.hasStarted &&
        uiState.dhikrAudio.currentTrack != null &&
        !uiState.quranPlayer.isPlaying

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 700.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail && (showTopLevelNav || uiState.quranPlayer.hasStarted || showGlobalDhikrAudioBar)) {
                    Column(
                        modifier = if (!showTopLevelNav) Modifier.navigationBarsPadding() else Modifier
                    ) {
                        AnimatedVisibility(visible = showGlobalDhikrAudioBar) {
                            DhikrRecitationBar(
                                audioState = uiState.dhikrAudio,
                                onTogglePlayPause = viewModel::onToggleDhikrAudioBarPlayPause,
                                onNext = viewModel::onNextDhikrAudio,
                                onPrevious = viewModel::onPreviousDhikrAudio,
                                onSeekTo = viewModel::onSeekDhikrAudio,
                                onCycleSpeed = viewModel::onCycleDhikrAudioSpeed,
                                onToggleRepeat = viewModel::onToggleDhikrAudioRepeat,
                                onRetry = viewModel::onRetryDhikrAudio,
                                onClose = viewModel::onStopDhikrAudio
                            )
                        }
                        AnimatedVisibility(visible = uiState.quranPlayer.hasStarted && !showGlobalDhikrAudioBar) {
                            QuranMiniPlayerBar(
                                playerState = uiState.quranPlayer,
                                onTogglePlayPause = viewModel::onToggleQuranPlayPause,
                                onNextTrack = viewModel::onNextQuranTrack,
                                onClose = viewModel::onStopQuranPlayer
                            )
                        }
                        if (showTopLevelNav) {
                            AdMobBannerBar(isAdFreeActive = uiState.isAdFreeActive)
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 4.dp
                            ) {
                                BottomNavTab.entries.forEach { tab ->
                                    val selected = currentRoute == tab.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateToTab(tab.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = stringResource(tab.labelRes)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = stringResource(tab.labelRes),
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        modifier = Modifier.testTag(tab.testTag)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showTopLevelNav && useNavRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        BottomNavTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    AdhkarNavHost(
                        navController = navController,
                        viewModel = viewModel,
                        chatViewModel = resolvedChatViewModel,
                        uiState = uiState,
                        onNavigateToTab = navigateToTab,
                        onShowMessage = { message ->
                            scope.launch { snackbarHostState.showSnackbar(message) }
                        },
                        copiedMessage = copiedMessage,
                        resetDoneMessage = resetDoneMessage,
                        isRewardedAdLoading = adControllerState.isRewardedLoading,
                        onWatchRewardedAd = onWatchRewardedAd,
                        onRequestUnlockTarget = { target -> pendingUnlockTarget = target },
                        modifier = Modifier.weight(1f)
                    )
                    if (useNavRail) {
                        AnimatedVisibility(visible = showGlobalDhikrAudioBar) {
                            DhikrRecitationBar(
                                audioState = uiState.dhikrAudio,
                                onTogglePlayPause = viewModel::onToggleDhikrAudioBarPlayPause,
                                onNext = viewModel::onNextDhikrAudio,
                                onPrevious = viewModel::onPreviousDhikrAudio,
                                onSeekTo = viewModel::onSeekDhikrAudio,
                                onCycleSpeed = viewModel::onCycleDhikrAudioSpeed,
                                onToggleRepeat = viewModel::onToggleDhikrAudioRepeat,
                                onRetry = viewModel::onRetryDhikrAudio,
                                onClose = viewModel::onStopDhikrAudio
                            )
                        }
                        AnimatedVisibility(visible = uiState.quranPlayer.hasStarted && !showGlobalDhikrAudioBar) {
                            QuranMiniPlayerBar(
                                playerState = uiState.quranPlayer,
                                onTogglePlayPause = viewModel::onToggleQuranPlayPause,
                                onNextTrack = viewModel::onNextQuranTrack,
                                onClose = viewModel::onStopQuranPlayer
                            )
                        }
                        if (showTopLevelNav) {
                            AdMobBannerBar(isAdFreeActive = uiState.isAdFreeActive)
                        }
                    }
                }
            }
        }
    }
}
