package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.R
import com.example.data.model.Counter
import com.example.data.model.Dhikr
import com.example.data.model.DhikrCategory
import com.example.data.model.PrayerCityCatalog
import com.example.qibla.QiblaCalculator
import com.example.ui.components.AdhkarCategoryFilterBar
import com.example.ui.components.AdhkarTabFilter
import com.example.ui.components.DailyReflectionCard
import com.example.ui.components.DhikrCard
import com.example.ui.components.ElectronicTasbeehCard
import com.example.ui.components.HijriDateHomeCard
import com.example.ui.components.PrayerTimesHomeWidget
import com.example.ui.components.QuranRecitationCard
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.viewmodel.AdhkarUiState
import com.example.ui.viewmodel.CategoryProgress
import com.example.util.HijriDateHelper
import com.example.util.ShareHelper

@Composable
fun HomeScreen(
    uiState: AdhkarUiState,
    onCategoryClick: (DhikrCategory) -> Unit,
    onNavigateToTasbih: () -> Unit,
    onNavigateToStats: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToPrayerTimes: () -> Unit = {},
    onNavigateToQibla: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit,
    onIncrementDhikr: (Dhikr) -> Unit,
    onToggleFavorite: (Dhikr) -> Unit,
    onResetDhikr: (Int) -> Unit,
    onShowCopiedToast: () -> Unit,
    onShowReflectionCopiedToast: () -> Unit = onShowCopiedToast,
    onRefreshDailyReflection: () -> Unit = {},
    onSelectQuranTrack: (Int) -> Unit = {},
    onToggleQuranPlayPause: () -> Unit = {},
    onNextQuranTrack: () -> Unit = {},
    onPreviousQuranTrack: () -> Unit = {},
    onSeekQuranTo: (Long) -> Unit = {},
    onToggleQuranRepeat: () -> Unit = {},
    onToggleDhikrAudio: (Dhikr, List<Dhikr>) -> Unit = { _, _ -> },
    onSeekDhikrAudio: (Long) -> Unit = {},
    onCycleDhikrAudioSpeed: () -> Unit = {},
    onToggleDhikrAudioRepeat: () -> Unit = {},
    onRetryDhikrAudio: () -> Unit = {},
    onStopDhikrAudio: () -> Unit = {},
    onEnsurePrayerTimesLoaded: () -> Unit = {},
    onDetectPrayerLocation: () -> Unit = {},
    onPrayerLocationPermissionDenied: () -> Unit = {},
    onRefreshPrayerTimes: () -> Unit = {},
    onQuickIncrementTasbih: (Counter) -> Unit = {},
    onQuickResetTasbih: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var showFavoritesOnly by rememberSaveable { mutableStateOf(false) }
    var selectedTabFilter by rememberSaveable { mutableStateOf(AdhkarTabFilter.MORNING) }
    val tabFilteredAdhkar = remember(uiState.allAdhkar, selectedTabFilter) {
        selectedTabFilter.filterAdhkar(uiState.allAdhkar)
    }

    var hijriDateInfo by remember { mutableStateOf(HijriDateHelper.today()) }

    LaunchedEffect(Unit) {
        onEnsurePrayerTimesLoaded()
        hijriDateInfo = HijriDateHelper.today()
    }

    LifecycleResumeEffect(Unit) {
        onRefreshDailyReflection()
        hijriDateInfo = HijriDateHelper.today()
        onPauseOrDispose { }
    }
    val dailyFraction by animateFloatAsState(
        targetValue = if (uiState.totalDhikrCount == 0) 0f
        else uiState.todayCompletedDhikrCount.toFloat() / uiState.totalDhikrCount.toFloat(),
        animationSpec = spring(),
        label = "daily_progress"
    )

    val greetingRes = when (uiState.featuredCategory) {
        DhikrCategory.MORNING -> R.string.greeting_morning
        DhikrCategory.EVENING -> R.string.greeting_evening
        else -> R.string.greeting_night
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val useSideBySidePrayerHijri = maxWidth >= 720.dp
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .testTag("home_lazy_column"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Greeting + Daily Progress Ring
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(greetingRes),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.home_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Surface(
                        onClick = onNavigateToChat,
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 1.dp,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("home_chat_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Forum,
                                contentDescription = stringResource(R.string.chat_open_cd),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.chat_home_cta),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        onClick = onNavigateToStats,
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        tonalElevation = 1.dp,
                        modifier = Modifier.testTag("home_daily_progress_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { dailyFraction },
                                    modifier = Modifier.size(40.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    strokeWidth = 4.dp,
                                    strokeCap = StrokeCap.Round
                                )
                                Text(
                                    text = "${(dailyFraction * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.daily_progress_title),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${uiState.todayCompletedDhikrCount} / ${uiState.totalDhikrCount}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Overall Daily Achievement Progress Ring Card
            item {
                DailyGoalAchievementCard(
                    todayTotalCounts = uiState.todayTotalCounts,
                    dailyTotalGoal = uiState.dailyTotalGoal,
                    todayDhikrTapCount = uiState.todayDhikrTapCount,
                    todayTasbihSum = uiState.todayTasbihSum,
                    progressFraction = uiState.dailyTotalGoalProgress,
                    isGoalReached = uiState.isDailyTotalGoalReached,
                    onOpenStats = onNavigateToStats,
                    onEditGoal = onNavigateToSettings
                )
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_search_input"),
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
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.testTag("clear_search_button")
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

            if (uiState.searchQuery.isNotBlank()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.search_results_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${uiState.filteredAdhkar.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 2.dp)
                                    .testTag("home_search_results_count")
                            )
                        }
                    }
                }
                if (uiState.filteredAdhkar.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_search_empty"),
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
                    }
                } else {
                    items(uiState.filteredAdhkar, key = { it.id }) { dhikr ->
                        val catTitle = stringResource(DhikrCategory.fromId(dhikr.category).titleRes)
                        val isCardAudioActive = uiState.dhikrAudio.hasStarted && uiState.dhikrAudio.activeDhikrId == dhikr.id
                        DhikrCard(
                            dhikr = dhikr,
                            fontSizeSp = uiState.preferences.fontSizeSp,
                            onTapIncrement = { onIncrementDhikr(dhikr) },
                            onToggleFavorite = { onToggleFavorite(dhikr) },
                            onShare = { ShareHelper.shareDhikr(context, dhikr, language) },
                            onCopy = {
                                ShareHelper.copyDhikr(context, dhikr, language)
                                onShowCopiedToast()
                            },
                            onReset = { onResetDhikr(dhikr.id) },
                            categoryLabel = catTitle,
                            isAudioActive = isCardAudioActive,
                            isAudioPlaying = isCardAudioActive && uiState.dhikrAudio.isPlaying,
                            isAudioBuffering = isCardAudioActive && uiState.dhikrAudio.isBuffering,
                            onToggleAudio = { onToggleDhikrAudio(dhikr, uiState.filteredAdhkar) },
                            audioPositionMs = uiState.dhikrAudio.positionMs,
                            audioDurationMs = uiState.dhikrAudio.durationMs,
                            audioPlaybackSpeed = uiState.dhikrAudio.playbackSpeed,
                            audioRepeatCurrent = uiState.dhikrAudio.repeatCurrent,
                            audioHasError = uiState.dhikrAudio.hasError,
                            onSeekAudio = onSeekDhikrAudio,
                            onCycleAudioSpeed = onCycleDhikrAudioSpeed,
                            onToggleAudioRepeat = onToggleDhikrAudioRepeat,
                            onRetryAudio = onRetryDhikrAudio,
                            onStopAudio = onStopDhikrAudio
                        )
                    }
                }
            } else {
                // Home Filter Chips: All vs Bookmarked Favorites Tab
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = !showFavoritesOnly,
                            onClick = { showFavoritesOnly = false },
                            label = {
                                Text(
                                    text = stringResource(R.string.home_filter_all),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("home_filter_all")
                        )
                        FilterChip(
                            selected = showFavoritesOnly,
                            onClick = { showFavoritesOnly = true },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (showFavoritesOnly) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.home_filter_favorites, uiState.favoriteAdhkar.size),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("home_filter_favorites")
                        )
                    }
                }

                if (showFavoritesOnly) {
                    if (uiState.favoriteAdhkar.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("home_favorites_empty"),
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FavoriteBorder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.favorites_empty_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.favorites_empty_body),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(uiState.favoriteAdhkar, key = { "home_fav_${it.id}" }) { dhikr ->
                            val catTitle = stringResource(DhikrCategory.fromId(dhikr.category).titleRes)
                            val isCardAudioActive = uiState.dhikrAudio.hasStarted && uiState.dhikrAudio.activeDhikrId == dhikr.id
                            DhikrCard(
                                dhikr = dhikr,
                                fontSizeSp = uiState.preferences.fontSizeSp,
                                onTapIncrement = { onIncrementDhikr(dhikr) },
                                onToggleFavorite = { onToggleFavorite(dhikr) },
                                onShare = { ShareHelper.shareDhikr(context, dhikr, language) },
                                onCopy = {
                                    ShareHelper.copyDhikr(context, dhikr, language)
                                    onShowCopiedToast()
                                },
                                onReset = { onResetDhikr(dhikr.id) },
                                categoryLabel = catTitle,
                                isAudioActive = isCardAudioActive,
                                isAudioPlaying = isCardAudioActive && uiState.dhikrAudio.isPlaying,
                                isAudioBuffering = isCardAudioActive && uiState.dhikrAudio.isBuffering,
                                onToggleAudio = { onToggleDhikrAudio(dhikr, uiState.favoriteAdhkar) },
                                audioPositionMs = uiState.dhikrAudio.positionMs,
                                audioDurationMs = uiState.dhikrAudio.durationMs,
                                audioPlaybackSpeed = uiState.dhikrAudio.playbackSpeed,
                                audioRepeatCurrent = uiState.dhikrAudio.repeatCurrent,
                                audioHasError = uiState.dhikrAudio.hasError,
                                onSeekAudio = onSeekDhikrAudio,
                                onCycleAudioSpeed = onCycleDhikrAudioSpeed,
                                onToggleAudioRepeat = onToggleDhikrAudioRepeat,
                                onRetryAudio = onRetryDhikrAudio,
                                onStopAudio = onStopDhikrAudio
                            )
                        }
                    }
                } else {
                    // Featured Time-of-Day Hero Banner
                    item {
                        FeaturedHeroCard(
                            category = uiState.featuredCategory,
                            progress = uiState.categoryProgress.find { it.category == uiState.featuredCategory },
                            onClick = { onCategoryClick(uiState.featuredCategory) }
                        )
                    }

                    // Daily Inspirational Reflection Card (refreshes every 24h)
                    item {
                        val shareChooserTitle = stringResource(R.string.reflection_share_chooser)
                        DailyReflectionCard(
                            reflection = uiState.dailyReflection,
                            millisUntilNextRefresh = uiState.reflectionNextRefreshMillis,
                            fontSizeSp = uiState.preferences.fontSizeSp,
                            onCopy = {
                                ShareHelper.copyReflection(context, uiState.dailyReflection, language)
                                onShowReflectionCopiedToast()
                            },
                            onShare = {
                                ShareHelper.shareReflection(
                                    context = context,
                                    reflection = uiState.dailyReflection,
                                    chooserTitle = shareChooserTitle,
                                    appLanguage = language
                                )
                            }
                        )
                    }

                    // Current Hijri Date & Geolocation Prayer Times Widget
                    if (useSideBySidePrayerHijri) {
                        item(key = "home_hijri_and_prayer_row") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                HijriDateHomeCard(
                                    hijriDateInfo = hijriDateInfo,
                                    onClick = onNavigateToPrayerTimes,
                                    modifier = Modifier.weight(1f)
                                )
                                PrayerTimesHomeWidget(
                                    prayerState = uiState.prayerTimes,
                                    onOpenFullDashboard = onNavigateToPrayerTimes,
                                    onDetectLocationAndRefresh = onDetectPrayerLocation,
                                    onLocationPermissionDenied = onPrayerLocationPermissionDenied,
                                    onRetry = onRefreshPrayerTimes,
                                    modifier = Modifier.weight(1.45f)
                                )
                            }
                        }
                    } else {
                        item(key = "home_hijri_date") {
                            HijriDateHomeCard(
                                hijriDateInfo = hijriDateInfo,
                                onClick = onNavigateToPrayerTimes
                            )
                        }
                        item(key = "home_prayer_times") {
                            PrayerTimesHomeWidget(
                                prayerState = uiState.prayerTimes,
                                onOpenFullDashboard = onNavigateToPrayerTimes,
                                onDetectLocationAndRefresh = onDetectPrayerLocation,
                                onLocationPermissionDenied = onPrayerLocationPermissionDenied,
                                onRetry = onRefreshPrayerTimes
                            )
                        }
                    }

                    // Qibla Compass Entry Card
                    item {
                        val savedLat = uiState.preferences.prayerLatitude
                        val savedLon = uiState.preferences.prayerLongitude
                        val savedLabel = uiState.preferences.prayerLocationLabel
                        val qiblaPreview = remember(savedLat, savedLon, savedLabel, language) {
                            if (savedLat != null && savedLon != null) {
                                val deg = kotlin.math.round(
                                    QiblaCalculator.calculateQiblaBearingDegrees(savedLat, savedLon).toDouble()
                                ).toInt()
                                val city = PrayerCityCatalog.localizeLabel(
                                    savedLabel.ifBlank { PrayerCityCatalog.cities.first().arabicName },
                                    language
                                )
                                "$deg° — $city"
                            } else {
                                null
                            }
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(onClick = onNavigateToQibla)
                                .testTag("home_qibla_card"),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Explore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = stringResource(R.string.qibla_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = qiblaPreview ?: stringResource(R.string.qibla_card_subtitle),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = stringResource(R.string.qibla_open_cd),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Spiritual Companion Chat Entry Card
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(onClick = onNavigateToChat)
                                .testTag("home_chat_card"),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = stringResource(R.string.chat_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(R.string.chat_subtitle),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = stringResource(R.string.chat_open_cd),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Categories Section Title
                    item {
                        Text(
                            text = stringResource(R.string.categories_section_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Responsive Category Grid
                    item {
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val columns = if (maxWidth > 620.dp) 3 else 2
                            val rows = uiState.categoryProgress.chunked(columns)
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                rows.forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowItems.forEach { item ->
                                            CategoryGridCard(
                                                progress = item,
                                                onClick = { onCategoryClick(item.category) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        repeat(columns - rowItems.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Interactive TabRow Filtering: Morning / Evening / General
                    item(key = "home_tab_filter_header") {
                        AdhkarCategoryFilterBar(
                            selectedTab = selectedTabFilter,
                            onSelectTab = { selectedTabFilter = it },
                            filteredCount = tabFilteredAdhkar.size,
                            onOpenLinkedCategory = onCategoryClick
                        )
                    }
                    items(
                        items = tabFilteredAdhkar,
                        key = { "home_tab_${selectedTabFilter.id}_${it.id}" }
                    ) { dhikr ->
                        val catTitle = stringResource(DhikrCategory.fromId(dhikr.category).titleRes)
                        val isCardAudioActive = uiState.dhikrAudio.hasStarted &&
                            uiState.dhikrAudio.activeDhikrId == dhikr.id
                        DhikrCard(
                            dhikr = dhikr,
                            fontSizeSp = uiState.preferences.fontSizeSp,
                            onTapIncrement = { onIncrementDhikr(dhikr) },
                            onToggleFavorite = { onToggleFavorite(dhikr) },
                            onShare = { ShareHelper.shareDhikr(context, dhikr, language) },
                            onCopy = {
                                ShareHelper.copyDhikr(context, dhikr, language)
                                onShowCopiedToast()
                            },
                            onReset = { onResetDhikr(dhikr.id) },
                            categoryLabel = if (selectedTabFilter == AdhkarTabFilter.GENERAL) catTitle else null,
                            isAudioActive = isCardAudioActive,
                            isAudioPlaying = isCardAudioActive && uiState.dhikrAudio.isPlaying,
                            isAudioBuffering = isCardAudioActive && uiState.dhikrAudio.isBuffering,
                            onToggleAudio = { onToggleDhikrAudio(dhikr, tabFilteredAdhkar) },
                            audioPositionMs = uiState.dhikrAudio.positionMs,
                            audioDurationMs = uiState.dhikrAudio.durationMs,
                            audioPlaybackSpeed = uiState.dhikrAudio.playbackSpeed,
                            audioRepeatCurrent = uiState.dhikrAudio.repeatCurrent,
                            audioHasError = uiState.dhikrAudio.hasError,
                            onSeekAudio = onSeekDhikrAudio,
                            onCycleAudioSpeed = onCycleDhikrAudioSpeed,
                            onToggleAudioRepeat = onToggleDhikrAudioRepeat,
                            onRetryAudio = onRetryDhikrAudio,
                            onStopAudio = onStopDhikrAudio,
                            audioTagPrefix = "tab_",
                            cardTestTag = "tab_filtered_dhikr_card_${dhikr.id}"
                        )
                    }

                    // Dedicated Bookmarked Dhikr Section on Home (when favorites exist)
                    if (uiState.favoriteAdhkar.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("home_favorites_section"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Favorite,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.home_favorites_section_title),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                TextButton(
                                    onClick = onNavigateToFavorites,
                                    modifier = Modifier.testTag("home_favorites_view_all")
                                ) {
                                    Text(stringResource(R.string.home_favorites_view_all))
                                }
                            }
                        }
                        items(
                            items = uiState.favoriteAdhkar.take(3),
                            key = { "home_section_fav_${it.id}" }
                        ) { dhikr ->
                            val catTitle = stringResource(DhikrCategory.fromId(dhikr.category).titleRes)
                            val isCardAudioActive = uiState.dhikrAudio.hasStarted && uiState.dhikrAudio.activeDhikrId == dhikr.id
                            DhikrCard(
                                dhikr = dhikr,
                                fontSizeSp = uiState.preferences.fontSizeSp,
                                onTapIncrement = { onIncrementDhikr(dhikr) },
                                onToggleFavorite = { onToggleFavorite(dhikr) },
                                onShare = { ShareHelper.shareDhikr(context, dhikr, language) },
                                onCopy = {
                                    ShareHelper.copyDhikr(context, dhikr, language)
                                    onShowCopiedToast()
                                },
                                onReset = { onResetDhikr(dhikr.id) },
                                categoryLabel = catTitle,
                                isAudioActive = isCardAudioActive,
                                isAudioPlaying = isCardAudioActive && uiState.dhikrAudio.isPlaying,
                                isAudioBuffering = isCardAudioActive && uiState.dhikrAudio.isBuffering,
                                onToggleAudio = { onToggleDhikrAudio(dhikr, uiState.favoriteAdhkar) },
                                audioPositionMs = uiState.dhikrAudio.positionMs,
                                audioDurationMs = uiState.dhikrAudio.durationMs,
                                audioPlaybackSpeed = uiState.dhikrAudio.playbackSpeed,
                                audioRepeatCurrent = uiState.dhikrAudio.repeatCurrent,
                                audioHasError = uiState.dhikrAudio.hasError,
                                onSeekAudio = onSeekDhikrAudio,
                                onCycleAudioSpeed = onCycleDhikrAudioSpeed,
                                onToggleAudioRepeat = onToggleDhikrAudioRepeat,
                                onRetryAudio = onRetryDhikrAudio,
                                onStopAudio = onStopDhikrAudio,
                                audioTagPrefix = "home_fav_"
                            )
                        }
                    }

                    // Interactive Quick Electronic Tasbeeh UI Component
                    item {
                        val activeTasbih = uiState.selectedTasbih ?: uiState.tasbihPresets.firstOrNull()
                        if (activeTasbih != null) {
                            ElectronicTasbeehCard(
                                presetId = activeTasbih.id,
                                phraseArabic = activeTasbih.phrase,
                                currentCount = activeTasbih.currentCount,
                                targetCount = activeTasbih.targetCount,
                                todayTasbihSum = uiState.todayTasbihSum,
                                hapticEnabled = uiState.preferences.hapticEnabled,
                                onIncrement = { onQuickIncrementTasbih(activeTasbih) },
                                onReset = { onQuickResetTasbih(activeTasbih.id) },
                                onOpenFullScreen = onNavigateToTasbih
                            )
                        }
                    }

                    // Daily Quranic Recitations Streaming Player
                    item {
                        QuranRecitationCard(
                            playerState = uiState.quranPlayer,
                            onSelectTrack = onSelectQuranTrack,
                            onTogglePlayPause = onToggleQuranPlayPause,
                            onNextTrack = onNextQuranTrack,
                            onPreviousTrack = onPreviousQuranTrack,
                            onSeekTo = onSeekQuranTo,
                            onToggleRepeat = onToggleQuranRepeat
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedHeroCard(
    category: DhikrCategory,
    progress: CategoryProgress?,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(196.dp)
            .clip(RoundedCornerShape(26.dp))
            .clickable(onClick = onClick)
            .testTag("featured_hero_card"),
        shape = RoundedCornerShape(26.dp),
        tonalElevation = 4.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = category.bannerRes),
                contentDescription = stringResource(category.titleRes),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xCC0D2E25),
                                Color(0x99113B30),
                                Color(0x44113B30)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0x33FFFFFF)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = null,
                                tint = Color(0xFFF6E7C4),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(category.subtitleRes),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFF6E7C4)
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(category.titleRes),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onClick,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE5C16C),
                                contentColor = Color(0xFF1D241E)
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("featured_start_button")
                        ) {
                            Text(
                                text = stringResource(R.string.featured_start_cta),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        if (progress != null) {
                            Text(
                                text = "${progress.completedItems} / ${progress.totalItems}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFFEAF3EE)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryGridCard(
    progress: CategoryProgress,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val category = progress.category
    val animatedProgress by animateFloatAsState(
        targetValue = progress.progressFraction,
        animationSpec = spring(),
        label = "cat_progress_${category.id}"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("category_card_${category.id}"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${progress.completedItems}/${progress.totalItems}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(category.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(category.subtitleRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun DailyGoalAchievementCard(
    todayTotalCounts: Int,
    dailyTotalGoal: Int,
    todayDhikrTapCount: Int,
    todayTasbihSum: Int,
    progressFraction: Float,
    isGoalReached: Boolean,
    onOpenStats: () -> Unit,
    onEditGoal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedSweep by animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1f),
        animationSpec = spring(),
        label = "overall_daily_goal_ring"
    )
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val percentText = "${(progressFraction.coerceIn(0f, 1f) * 100).toInt()}%"

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onOpenStats)
            .testTag("home_daily_goal_card"),
        shape = RoundedCornerShape(22.dp),
        color = if (isGoalReached) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = 2.dp,
        border = BorderStroke(
            width = if (isGoalReached) 1.5.dp else 1.dp,
            color = if (isGoalReached) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .testTag("home_daily_goal_ring"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 8.dp.toPx()
                    val diameter = size.minDimension - stroke
                    val topLeft = Offset(stroke / 2f, stroke / 2f)
                    val arcSize = Size(diameter, diameter)
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    if (animatedSweep > 0f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(primaryColor, tertiaryColor, primaryColor)
                            ),
                            startAngle = -90f,
                            sweepAngle = animatedSweep * 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                }
                Text(
                    text = percentText,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("home_daily_goal_percent")
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_daily_goal_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isGoalReached) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = "$todayTotalCounts / $dailyTotalGoal",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("home_daily_goal_count_text")
                )
                Text(
                    text = if (isGoalReached) {
                        stringResource(R.string.home_daily_goal_achieved)
                    } else {
                        stringResource(R.string.home_daily_goal_breakdown, todayDhikrTapCount, todayTasbihSum)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("home_daily_goal_breakdown")
                )
            }

            IconButton(
                onClick = onEditGoal,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("home_daily_goal_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.settings_daily_goal_title),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
