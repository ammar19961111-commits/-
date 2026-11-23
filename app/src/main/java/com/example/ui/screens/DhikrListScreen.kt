package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.DhikrAudioPlayerState
import com.example.data.model.AdhkarTranslations
import com.example.data.model.Dhikr
import com.example.data.model.DhikrAudioCatalog
import com.example.data.model.DhikrCategory
import com.example.data.model.DhikrReminder
import com.example.data.model.DhikrReminderDefaults
import com.example.notifications.DhikrNotificationHelper
import com.example.ui.components.DhikrCard
import com.example.ui.components.DhikrEntryAudioPlayer
import com.example.ui.components.DhikrRecitationBar
import com.example.ui.components.ReminderTimePickerDialog
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.LocalAppLanguage
import com.example.util.ArabicTextNormalizer
import com.example.util.ShareHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DhikrListScreen(
    category: DhikrCategory,
    adhkar: List<Dhikr>,
    fontSizeSp: Float,
    autoAdvance: Boolean,
    onBack: () -> Unit,
    onIncrementDhikr: (Dhikr) -> Unit,
    onToggleFavorite: (Dhikr) -> Unit,
    onResetDhikr: (Int) -> Unit,
    onResetCategory: () -> Unit,
    onShowCopiedToast: () -> Unit,
    modifier: Modifier = Modifier,
    reminder: DhikrReminder? = null,
    onUpdateReminder: (Boolean, Int, Int) -> Unit = { _, _, _ -> },
    audioState: DhikrAudioPlayerState = DhikrAudioPlayerState(),
    onPlayDisplayedAdhkar: (List<Dhikr>, Int?) -> Unit = { _, _ -> },
    onToggleDhikrAudio: (Dhikr, List<Dhikr>) -> Unit = { _, _ -> },
    onToggleAudioBarPlayPause: () -> Unit = {},
    onNextAudio: () -> Unit = {},
    onPreviousAudio: () -> Unit = {},
    onSeekAudio: (Long) -> Unit = {},
    onCycleAudioSpeed: () -> Unit = {},
    onToggleAudioRepeat: () -> Unit = {},
    onRetryAudio: () -> Unit = {},
    onStopAudio: () -> Unit = {},
    initialFocusMode: Boolean = false,
    onFocusModeChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var isFocusMode by rememberSaveable { mutableStateOf(initialFocusMode) }
    LaunchedEffect(initialFocusMode) {
        isFocusMode = initialFocusMode
    }
    var isSearchVisible by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var pendingTime by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val time = pendingTime
        pendingTime = null
        if (granted && time != null) {
            onUpdateReminder(true, time.first, time.second)
        }
    }

    BackHandler {
        if (isSearchVisible) {
            searchQuery = ""
            isSearchVisible = false
        } else {
            onBack()
        }
    }

    val displayedAdhkar = remember(adhkar, searchQuery) {
        if (searchQuery.isBlank()) adhkar
        else adhkar.filter { ArabicTextNormalizer.matches(it, searchQuery) }
    }

    val completedCount = adhkar.count { it.isCompleted }
    val allDone = adhkar.isNotEmpty() && completedCount == adhkar.size
    val overallProgress by animateFloatAsState(
        targetValue = if (adhkar.isEmpty()) 0f else completedCount.toFloat() / adhkar.size,
        animationSpec = spring(),
        label = "category_overall_progress"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(category.titleRes),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "$completedCount / ${adhkar.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("dhikr_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back_cd)
                        )
                    }
                },
                actions = {
                    val isPlayingDisplayed = audioState.hasStarted &&
                        audioState.isPlaying &&
                        displayedAdhkar.any { it.id == audioState.activeDhikrId }
                    IconButton(
                        onClick = {
                            if (isPlayingDisplayed) {
                                onToggleAudioBarPlayPause()
                            } else if (displayedAdhkar.isNotEmpty()) {
                                onPlayDisplayedAdhkar(displayedAdhkar, displayedAdhkar.first().id)
                            }
                        },
                        enabled = displayedAdhkar.isNotEmpty(),
                        modifier = Modifier.testTag("dhikr_list_play_all_button")
                    ) {
                        Icon(
                            imageVector = if (isPlayingDisplayed) Icons.Rounded.Pause else Icons.Rounded.Headphones,
                            contentDescription = stringResource(
                                if (isPlayingDisplayed) R.string.dhikr_audio_pause_single else R.string.dhikr_audio_listen_all
                            ),
                            tint = if (isPlayingDisplayed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showReminderDialog = true },
                        modifier = Modifier.testTag("dhikr_list_reminder_button")
                    ) {
                        val isEnabled = reminder?.enabled == true
                        Icon(
                            imageVector = if (isEnabled) Icons.Rounded.NotificationsActive else Icons.Outlined.Notifications,
                            contentDescription = stringResource(R.string.reminder_pick_time_title),
                            tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            if (isSearchVisible) {
                                searchQuery = ""
                                isSearchVisible = false
                            } else {
                                isSearchVisible = true
                            }
                        },
                        modifier = Modifier.testTag("dhikr_list_search_toggle")
                    ) {
                        Icon(
                            imageVector = if (isSearchVisible) Icons.Rounded.Close else Icons.Rounded.Search,
                            contentDescription = stringResource(
                                if (isSearchVisible) R.string.clear_search else R.string.search_placeholder
                            ),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = {
                            val nextFocus = !isFocusMode
                            isFocusMode = nextFocus
                            onFocusModeChange(nextFocus)
                        },
                        modifier = Modifier.testTag("toggle_focus_mode_button")
                    ) {
                        Icon(
                            imageVector = if (isFocusMode) Icons.Rounded.ViewAgenda else Icons.Rounded.ViewCarousel,
                            contentDescription = stringResource(
                                if (isFocusMode) R.string.mode_list else R.string.mode_focus
                            ),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.testTag("reset_category_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.reset_category)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            AnimatedVisibility(visible = audioState.hasStarted && audioState.currentTrack != null) {
                DhikrRecitationBar(
                    audioState = audioState,
                    onTogglePlayPause = onToggleAudioBarPlayPause,
                    onNext = onNextAudio,
                    onPrevious = onPreviousAudio,
                    onSeekTo = onSeekAudio,
                    onCycleSpeed = onCycleAudioSpeed,
                    onToggleRepeat = onToggleAudioRepeat,
                    onRetry = onRetryAudio,
                    onClose = onStopAudio,
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = { overallProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            AnimatedVisibility(visible = isSearchVisible) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 800.dp)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("dhikr_list_search_input"),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_placeholder),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.testTag("dhikr_list_clear_search")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.clear_search)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { keyboardController?.hide() }
                    ),
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            AnimatedVisibility(visible = allDone && searchQuery.isBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 800.dp)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.category_all_done_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(R.string.category_all_done_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            if (isFocusMode && displayedAdhkar.isNotEmpty() && searchQuery.isBlank()) {
                FocusPagerContent(
                    adhkar = displayedAdhkar,
                    fontSizeSp = fontSizeSp,
                    autoAdvance = autoAdvance,
                    audioState = audioState,
                    onToggleDhikrAudio = { item -> onToggleDhikrAudio(item, displayedAdhkar) },
                    onSeekAudio = onSeekAudio,
                    onCycleAudioSpeed = onCycleAudioSpeed,
                    onToggleAudioRepeat = onToggleAudioRepeat,
                    onRetryAudio = onRetryAudio,
                    onStopAudio = onStopAudio,
                    onIncrementDhikr = onIncrementDhikr,
                    onToggleFavorite = onToggleFavorite,
                    onShare = { ShareHelper.shareDhikr(context, it, language) },
                    onCopy = {
                        ShareHelper.copyDhikr(context, it, language)
                        onShowCopiedToast()
                    },
                    onResetDhikr = onResetDhikr
                )
            } else if (displayedAdhkar.isEmpty() && searchQuery.isNotBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 800.dp)
                        .padding(16.dp)
                        .testTag("dhikr_list_search_empty"),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = stringResource(R.string.search_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 800.dp)
                        .testTag("dhikr_list_lazy_column"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(displayedAdhkar, key = { it.id }) { dhikr ->
                        val isCardAudioActive = audioState.hasStarted && audioState.activeDhikrId == dhikr.id
                        DhikrCard(
                            dhikr = dhikr,
                            fontSizeSp = fontSizeSp,
                            onTapIncrement = { onIncrementDhikr(dhikr) },
                            onToggleFavorite = { onToggleFavorite(dhikr) },
                            onShare = { ShareHelper.shareDhikr(context, dhikr, language) },
                            onCopy = {
                                ShareHelper.copyDhikr(context, dhikr, language)
                                onShowCopiedToast()
                            },
                            onReset = { onResetDhikr(dhikr.id) },
                            isAudioActive = isCardAudioActive,
                            isAudioPlaying = isCardAudioActive && audioState.isPlaying,
                            isAudioBuffering = isCardAudioActive && audioState.isBuffering,
                            onToggleAudio = { onToggleDhikrAudio(dhikr, displayedAdhkar) },
                            audioPositionMs = audioState.positionMs,
                            audioDurationMs = audioState.durationMs,
                            audioPlaybackSpeed = audioState.playbackSpeed,
                            audioRepeatCurrent = audioState.repeatCurrent,
                            audioHasError = audioState.hasError,
                            onSeekAudio = onSeekAudio,
                            onCycleAudioSpeed = onCycleAudioSpeed,
                            onToggleAudioRepeat = onToggleAudioRepeat,
                            onRetryAudio = onRetryAudio,
                            onStopAudio = onStopAudio
                        )
                    }
                }
            }
        }
    }

    if (showReminderDialog) {
        val currentReminder = reminder ?: run {
            val (h, m) = DhikrReminderDefaults.defaultTimeFor(category)
            DhikrReminder(category = category, enabled = false, hour = h, minute = m)
        }
        ReminderTimePickerDialog(
            reminder = currentReminder,
            onDismiss = { showReminderDialog = false },
            onConfirm = { hour, minute ->
                showReminderDialog = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !DhikrNotificationHelper.hasNotificationPermission(context)
                ) {
                    pendingTime = hour to minute
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    onUpdateReminder(true, hour, minute)
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.reset_category_confirm_title)) },
            text = { Text(stringResource(R.string.reset_category_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onResetCategory()
                    },
                    modifier = Modifier.testTag("confirm_reset_category")
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun FocusPagerContent(
    adhkar: List<Dhikr>,
    fontSizeSp: Float,
    autoAdvance: Boolean,
    audioState: DhikrAudioPlayerState,
    onToggleDhikrAudio: (Dhikr) -> Unit,
    onSeekAudio: (Long) -> Unit,
    onCycleAudioSpeed: () -> Unit,
    onToggleAudioRepeat: () -> Unit,
    onRetryAudio: () -> Unit,
    onStopAudio: () -> Unit,
    onIncrementDhikr: (Dhikr) -> Unit,
    onToggleFavorite: (Dhikr) -> Unit,
    onShare: (Dhikr) -> Unit,
    onCopy: (Dhikr) -> Unit,
    onResetDhikr: (Int) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { adhkar.size })
    val scope = rememberCoroutineScope()
    val currentDhikr = adhkar.getOrNull(pagerState.currentPage)

    // Auto-advance when current dhikr just completed
    LaunchedEffect(currentDhikr?.currentCount, currentDhikr?.id, autoAdvance) {
        if (autoAdvance && currentDhikr != null && currentDhikr.isCompleted) {
            val nextPage = pagerState.currentPage + 1
            if (nextPage < adhkar.size) {
                delay(380L)
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    // Sync Focus pager when audio player advances to another displayed Dhikr
    LaunchedEffect(audioState.activeDhikrId, audioState.hasStarted) {
        val activeId = audioState.activeDhikrId
        if (audioState.hasStarted && activeId != null) {
            val targetIdx = adhkar.indexOfFirst { it.id == activeId }
            if (targetIdx in adhkar.indices && targetIdx != pagerState.currentPage) {
                pagerState.animateScrollToPage(targetIdx)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 760.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            val item = adhkar[page]
            val language = LocalAppLanguage.current
            val english = if (language == AppLanguage.ENGLISH) AdhkarTranslations.forDhikr(item.id) else null
            val displayTitle = english?.titleEn ?: item.title
            val displaySource = english?.sourceEn ?: item.source
            val displayVirtue = english?.virtueEn ?: item.virtue
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayTitle,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (displaySource.isNotBlank()) {
                                Text(
                                    text = displaySource,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row {
                            val isFocusAudioActive = audioState.hasStarted && audioState.activeDhikrId == item.id
                            IconButton(
                                onClick = { onToggleDhikrAudio(item) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("focus_play_dhikr_audio_${item.id}")
                            ) {
                                Icon(
                                    imageVector = if (isFocusAudioActive && audioState.isPlaying) {
                                        Icons.Rounded.Pause
                                    } else {
                                        Icons.Rounded.PlayArrow
                                    },
                                    contentDescription = stringResource(
                                        if (isFocusAudioActive && audioState.isPlaying) {
                                            R.string.dhikr_audio_pause_single
                                        } else {
                                            R.string.dhikr_audio_play_single
                                        }
                                    ),
                                    tint = if (isFocusAudioActive) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            IconButton(
                                onClick = { onCopy(item) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = stringResource(R.string.copy_dhikr),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = { onShare(item) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("focus_share_dhikr_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = stringResource(R.string.share_dhikr),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = { onToggleFavorite(item) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = if (item.isFavorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = stringResource(R.string.toggle_favorite),
                                    tint = if (item.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Text(
                        text = item.content,
                        fontFamily = AmiriFontFamily,
                        fontSize = fontSizeSp.sp,
                        lineHeight = (fontSizeSp * 1.78f).sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (english != null) {
                        val translationSizeSp = (fontSizeSp * 0.68f).coerceIn(14f, 22f)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("focus_dhikr_translation_${item.id}"),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.dhikr_translation_label),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = english.translationEn,
                                    fontSize = translationSizeSp.sp,
                                    lineHeight = (translationSizeSp * 1.5f).sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (displayVirtue.isNotBlank()) {
                        val virtueSizeSp = (fontSizeSp * 0.62f).coerceIn(13f, 20f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = displayVirtue,
                                fontSize = virtueSizeSp.sp,
                                lineHeight = (virtueSizeSp * 1.55f).sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }

                    val isFocusAudioActive = audioState.hasStarted && audioState.activeDhikrId == item.id
                    val focusTrack = remember(item.id, item.category, item.title) {
                        DhikrAudioCatalog.trackFor(item)
                    }
                    DhikrEntryAudioPlayer(
                        dhikrId = item.id,
                        track = focusTrack,
                        isActive = isFocusAudioActive,
                        isPlaying = isFocusAudioActive && audioState.isPlaying,
                        isBuffering = isFocusAudioActive && audioState.isBuffering,
                        positionMs = if (isFocusAudioActive) audioState.positionMs else 0L,
                        durationMs = if (isFocusAudioActive) audioState.durationMs else 0L,
                        playbackSpeed = audioState.playbackSpeed,
                        repeatCurrent = audioState.repeatCurrent,
                        hasError = isFocusAudioActive && audioState.hasError,
                        onTogglePlayPause = { onToggleDhikrAudio(item) },
                        onSeekTo = onSeekAudio,
                        onCycleSpeed = onCycleAudioSpeed,
                        onToggleRepeat = onToggleAudioRepeat,
                        onRetry = onRetryAudio,
                        onStop = onStopAudio,
                        tagPrefix = "focus_"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Page indicator + Ergonomic Large Tap Circle
        if (currentDhikr != null) {
            val progress by animateFloatAsState(
                targetValue = if (currentDhikr.targetCount <= 0) 1f else (currentDhikr.currentCount.toFloat() / currentDhikr.targetCount).coerceIn(0f, 1f),
                animationSpec = spring(),
                label = "focus_progress"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${adhkar.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .clickable { onIncrementDhikr(currentDhikr) }
                        .testTag("focus_tap_counter"),
                    shape = CircleShape,
                    color = if (currentDhikr.isCompleted) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    tonalElevation = 6.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(92.dp),
                            color = if (currentDhikr.isCompleted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.tertiaryContainer
                            },
                            trackColor = Color.Transparent,
                            strokeWidth = 5.dp,
                            strokeCap = StrokeCap.Round
                        )
                        if (currentDhikr.isCompleted) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = stringResource(R.string.dhikr_completed_badge),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${currentDhikr.targetCount - currentDhikr.currentCount}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = "${currentDhikr.currentCount}/${currentDhikr.targetCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }

                if (currentDhikr.currentCount > 0) {
                    IconButton(
                        onClick = { onResetDhikr(currentDhikr.id) },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.reset_single)
                        )
                    }
                } else {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val next = (pagerState.currentPage + 1) % adhkar.size
                                pagerState.animateScrollToPage(next)
                            }
                        }
                    ) {
                        Text(stringResource(R.string.focus_next))
                    }
                }
            }
        }
    }
}
