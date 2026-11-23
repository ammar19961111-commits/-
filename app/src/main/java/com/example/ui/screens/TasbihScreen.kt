package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AdhkarTranslations
import com.example.data.model.Counter
import com.example.data.model.RosaryBeadDesign
import com.example.data.model.TasbihTargetSoundEffect
import com.example.data.model.TasbihVibrationPattern
import com.example.ui.components.AutoCountCard
import com.example.ui.components.CustomAutoCountTargetDialog
import com.example.ui.components.CustomTasbihCycleTargetDialog
import com.example.ui.components.DigitalTasbeehCounter
import com.example.ui.components.TasbihCelebrationBanner
import com.example.ui.components.TasbihTargetAlertCard
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.viewmodel.AdhkarUiState
import com.example.ui.viewmodel.AutoCountPace
import com.example.ui.viewmodel.AutoCountStatus
import com.example.ui.viewmodel.SessionTimerStatus
import com.example.ui.viewmodel.TasbihSessionTimerState
import kotlinx.coroutines.delay

@Composable
fun TasbihScreen(
    uiState: AdhkarUiState,
    onSelectPreset: (Int) -> Unit,
    onIncrementTasbih: (Counter) -> Unit,
    onResetCycle: (Int) -> Unit,
    onUpdateTarget: (Int, Int) -> Unit,
    onUpdateDailyTarget: (Int) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onAddCustomTasbih: (String, Int) -> Unit,
    onDeleteCustomTasbih: (Int) -> Unit,
    onSelectTimerDuration: (Int) -> Unit = {},
    onStartOrResumeTimer: () -> Unit = {},
    onPauseTimer: () -> Unit = {},
    onResetTimer: () -> Unit = {},
    onToggleTimerSound: (Boolean) -> Unit = {},
    onSetAutoCountTarget: (Int) -> Unit = {},
    onSelectAutoCountPace: (AutoCountPace) -> Unit = {},
    onStartOrResumeAutoCount: () -> Unit = {},
    onPauseAutoCount: () -> Unit = {},
    onResetAutoCount: () -> Unit = {},
    onToggleAutoCountSound: (Boolean) -> Unit = {},
    onToggleAutoCountHaptic: (Boolean) -> Unit = {},
    onSelectTargetVibrationPattern: (TasbihVibrationPattern) -> Unit = {},
    onPreviewTargetVibration: (TasbihVibrationPattern) -> Unit = {},
    onSelectTargetSoundEffect: (TasbihTargetSoundEffect) -> Unit = {},
    onPreviewTargetSound: (TasbihTargetSoundEffect) -> Unit = {},
    onSelectBeadDesign: (RosaryBeadDesign) -> Unit = {},
    onShowMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val activePreset = uiState.selectedTasbih
    val hapticFeedback = LocalHapticFeedback.current
    var showAddDialog by remember { mutableStateOf(false) }
    var showDailyGoalDialog by remember { mutableStateOf(false) }
    var showCustomTimerDialog by remember { mutableStateOf(false) }
    var showCustomAutoCountDialog by remember { mutableStateOf(false) }
    var showCustomCycleTargetDialog by remember { mutableStateOf(false) }

    val isDailyGoalReached = uiState.dailyTasbihTarget > 0 && uiState.todayTasbihSum >= uiState.dailyTasbihTarget
    var hasInitializedGoalState by remember { mutableStateOf(false) }
    var wasGoalReached by remember { mutableStateOf(false) }
    var previousAutoCountStatus by remember { mutableStateOf(uiState.autoCount.status) }
    var celebrationBurstId by remember { mutableIntStateOf(0) }
    var showCelebrationBanner by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoading, isDailyGoalReached) {
        if (!uiState.isLoading) {
            if (!hasInitializedGoalState) {
                hasInitializedGoalState = true
                wasGoalReached = isDailyGoalReached
            } else if (isDailyGoalReached && !wasGoalReached) {
                wasGoalReached = true
                celebrationBurstId++
            } else if (!isDailyGoalReached) {
                wasGoalReached = false
                showCelebrationBanner = false
            }
        }
    }

    LaunchedEffect(uiState.autoCount.status, uiState.autoCount.targetCount) {
        val currentStatus = uiState.autoCount.status
        if (currentStatus == AutoCountStatus.COMPLETED &&
            previousAutoCountStatus != AutoCountStatus.COMPLETED
        ) {
            celebrationBurstId++
            onShowMessage(
                context.getString(
                    R.string.tasbih_autocount_completed_banner,
                    uiState.autoCount.targetCount
                )
            )
        }
        previousAutoCountStatus = currentStatus
    }

    var previousCycleCountForCelebration by remember(activePreset?.id) {
        mutableIntStateOf(activePreset?.currentCount ?: 0)
    }
    LaunchedEffect(activePreset?.id, activePreset?.currentCount, activePreset?.targetCount) {
        val preset = activePreset ?: return@LaunchedEffect
        val target = preset.targetCount
        val count = preset.currentCount
        if (target > 0 && count == target && previousCycleCountForCelebration < target) {
            celebrationBurstId++
        }
        previousCycleCountForCelebration = count
    }

    LaunchedEffect(celebrationBurstId) {
        if (celebrationBurstId > 0) {
            showCelebrationBanner = true
            delay(2800L)
            showCelebrationBanner = false
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.tasbih_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { onToggleSound(!uiState.preferences.soundEnabled) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("tasbih_sound_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.preferences.soundEnabled) {
                                Icons.AutoMirrored.Rounded.VolumeUp
                            } else {
                                Icons.AutoMirrored.Rounded.VolumeOff
                            },
                            contentDescription = stringResource(R.string.settings_sound),
                            tint = if (uiState.preferences.soundEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            }
                        )
                    }

                    IconButton(
                        onClick = {
                            val nextEnabled = !uiState.preferences.hapticEnabled
                            if (nextEnabled) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onToggleHaptic(nextEnabled)
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("tasbih_haptic_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Vibration,
                            contentDescription = stringResource(R.string.settings_haptic),
                            tint = if (uiState.preferences.hapticEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            }
                        )
                    }

                    Surface(
                        onClick = { showAddDialog = true },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("add_custom_tasbih_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.tasbih_add_custom),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Preset chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.tasbihPresets.forEach { preset ->
                    val selected = activePreset?.id == preset.id
                    val presetTranslation = if (language == AppLanguage.ENGLISH) {
                        AdhkarTranslations.forCounter(preset.id)
                    } else null
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectPreset(preset.id) },
                        label = {
                            Text(
                                text = presetTranslation?.titleEn ?: preset.phrase,
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        trailingIcon = if (preset.id > 7 && selected) {
                            {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = stringResource(R.string.delete_cd),
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { onDeleteCustomTasbih(preset.id) }
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("tasbih_preset_${preset.id}")
                    )
                }
            }

            if (activePreset != null) {
                val activeEn = if (language == AppLanguage.ENGLISH) {
                    AdhkarTranslations.forCounter(activePreset.id)
                } else null
                // Active phrase card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = activePreset.phrase,
                            fontFamily = AmiriFontFamily,
                            fontSize = 26.sp,
                            lineHeight = 44.sp,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        if (activeEn != null) {
                            Text(
                                text = activeEn.meaningEn,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.testTag("tasbih_english_meaning")
                            )
                        }
                        val virtueText = activeEn?.virtueEn ?: activePreset.virtue
                        if (virtueText.isNotBlank()) {
                            Text(
                                text = virtueText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Target cycle selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val presetTargets = listOf(33, 99, 100, 0)
                    presetTargets.forEach { target ->
                        val isSelected = activePreset.targetCount == target
                        FilterChip(
                            selected = isSelected,
                            onClick = { onUpdateTarget(activePreset.id, target) },
                            label = {
                                Text(
                                    text = if (target == 0) stringResource(R.string.tasbih_open_target) else "$target",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            modifier = Modifier.testTag("tasbih_target_$target")
                        )
                    }
                    val hasCustomTarget = activePreset.targetCount > 0 && activePreset.targetCount !in presetTargets
                    FilterChip(
                        selected = hasCustomTarget,
                        onClick = { showCustomCycleTargetDialog = true },
                        label = {
                            Text(
                                text = if (hasCustomTarget) {
                                    "${activePreset.targetCount}"
                                } else {
                                    stringResource(R.string.tasbih_custom_target_chip)
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        modifier = Modifier.testTag("tasbih_target_custom_button")
                    )
                }

                // Live compact countdown pill while session timer is active or paused
                AnimatedVisibility(
                    visible = uiState.sessionTimer.status == SessionTimerStatus.RUNNING ||
                        uiState.sessionTimer.status == SessionTimerStatus.PAUSED,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (uiState.sessionTimer.status == SessionTimerStatus.RUNNING) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (uiState.sessionTimer.status == SessionTimerStatus.RUNNING) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        ),
                        modifier = Modifier.testTag("tasbih_timer_live_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = stringResource(R.string.tasbih_timer_title),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = uiState.sessionTimer.formattedTime,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Live compact Auto-Count status pill while running, paused, or completed
                AnimatedVisibility(
                    visible = uiState.autoCount.status != AutoCountStatus.IDLE,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    val autoRunning = uiState.autoCount.status == AutoCountStatus.RUNNING
                    val autoCompleted = uiState.autoCount.status == AutoCountStatus.COMPLETED
                    Surface(
                        shape = CircleShape,
                        color = when {
                            autoCompleted -> MaterialTheme.colorScheme.tertiaryContainer
                            autoRunning -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (autoRunning || autoCompleted) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        ),
                        modifier = Modifier.testTag("tasbih_autocount_live_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (autoCompleted) {
                                    Icons.Rounded.CheckCircle
                                } else {
                                    Icons.Rounded.AutoAwesome
                                },
                                contentDescription = stringResource(R.string.tasbih_autocount_title),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(
                                    R.string.tasbih_autocount_live_pill,
                                    uiState.autoCount.completedCount,
                                    uiState.autoCount.targetCount
                                ),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Reusable Digital Tasbeeh Counter Component
                DigitalTasbeehCounter(
                    currentCount = activePreset.currentCount,
                    targetCount = activePreset.targetCount,
                    celebrationTrigger = celebrationBurstId,
                    hapticEnabled = uiState.preferences.hapticEnabled,
                    dailyTasbihTarget = uiState.dailyTasbihTarget,
                    todayTasbihSum = uiState.todayTasbihSum,
                    beadDesign = uiState.preferences.rosaryBeadDesign,
                    onIncrement = { onIncrementTasbih(activePreset) },
                    onReset = { onResetCycle(activePreset.id) }
                )

                TasbihCelebrationBanner(
                    visible = showCelebrationBanner,
                    modifier = Modifier.fillMaxWidth()
                )

                // Custom Rosary Bead Design Selector (Exclusive Rewarded Unlocks)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tasbih_bead_design_selector"),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.tasbih_bead_design_title),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = stringResource(R.string.tasbih_bead_design_sub),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RosaryBeadDesign.entries.forEach { design ->
                                val isSelected = uiState.preferences.rosaryBeadDesign == design
                                val isUnlocked = uiState.preferences.isBeadDesignUnlocked(design)
                                Surface(
                                    onClick = { onSelectBeadDesign(design) },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    },
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        }
                                    ),
                                    modifier = Modifier
                                        .widthIn(min = 128.dp)
                                        .minimumInteractiveComponentSize()
                                        .testTag("bead_design_${design.name.lowercase()}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Canvas(modifier = Modifier.size(28.dp)) {
                                            val r = size.minDimension * 0.36f
                                            val center = Offset(size.width / 2f, size.height / 2f)
                                            for (i in 0 until 6) {
                                                val rad = Math.toRadians(i * 60.0)
                                                val pos = Offset(
                                                    x = center.x + (r * kotlin.math.cos(rad)).toFloat(),
                                                    y = center.y + (r * kotlin.math.sin(rad)).toFloat()
                                                )
                                                drawCircle(
                                                    color = if (i % 3 == 0) design.dividerBeadColor else design.primaryBeadColor,
                                                    radius = if (i % 3 == 0) 3.6.dp.toPx() else 2.8.dp.toPx(),
                                                    center = pos
                                                )
                                            }
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = stringResource(design.titleRes),
                                                style = MaterialTheme.typography.labelLarge,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = when {
                                                        isSelected -> Icons.Rounded.CheckCircle
                                                        isUnlocked -> Icons.Rounded.CheckCircle
                                                        else -> Icons.Rounded.Lock
                                                    },
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = if (isUnlocked) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.tertiary
                                                    }
                                                )
                                                Text(
                                                    text = if (isUnlocked) {
                                                        stringResource(R.string.reward_unlock_badge_unlocked)
                                                    } else {
                                                        stringResource(R.string.reward_unlock_badge_locked)
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isUnlocked) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.tertiary
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Auto-Count Card with configurable target & completion alert
                AutoCountCard(
                    autoCountState = uiState.autoCount,
                    onSelectTarget = onSetAutoCountTarget,
                    onOpenCustomTarget = { showCustomAutoCountDialog = true },
                    onSelectPace = onSelectAutoCountPace,
                    onStartOrResume = onStartOrResumeAutoCount,
                    onPause = onPauseAutoCount,
                    onReset = onResetAutoCount,
                    onToggleSoundAlert = onToggleAutoCountSound,
                    onToggleHapticAlert = onToggleAutoCountHaptic
                )

                // Custom Target Vibration Pattern & Sound Effect Card
                TasbihTargetAlertCard(
                    activeTargetCount = activePreset.targetCount,
                    selectedVibrationPattern = uiState.preferences.tasbihTargetVibrationPattern,
                    selectedSoundEffect = uiState.preferences.tasbihTargetSoundEffect,
                    hapticEnabled = uiState.preferences.hapticEnabled,
                    soundEnabled = uiState.preferences.soundEnabled,
                    onSelectVibrationPattern = onSelectTargetVibrationPattern,
                    onPreviewVibration = onPreviewTargetVibration,
                    onSelectSoundEffect = onSelectTargetSoundEffect,
                    onPreviewSound = onPreviewTargetSound,
                    onEditCustomTarget = { showCustomCycleTargetDialog = true }
                )

                // Session Timer Card
                SessionTimerCard(
                    timerState = uiState.sessionTimer,
                    onSelectDuration = onSelectTimerDuration,
                    onOpenCustomDuration = { showCustomTimerDialog = true },
                    onStartOrResume = onStartOrResumeTimer,
                    onPause = onPauseTimer,
                    onReset = onResetTimer,
                    onToggleSoundAlert = onToggleTimerSound
                )

                // Daily Tasbeeh Goal Card with Circular Progress Indicator
                DailyGoalProgressCard(
                    todayCount = uiState.todayTasbihSum,
                    dailyTarget = uiState.dailyTasbihTarget,
                    progress = uiState.dailyTasbihProgress,
                    isCelebrating = showCelebrationBanner,
                    onEditClick = { showDailyGoalDialog = true }
                )

                // Bottom Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatCard(
                        label = stringResource(R.string.tasbih_today_label),
                        value = "${uiState.todayTasbihSum}",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = stringResource(R.string.tasbih_total_all),
                        value = "${uiState.totalTasbihAllTime}",
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    if (showAddDialog) {
        AddCustomTasbihDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { phrase, target ->
                onAddCustomTasbih(phrase, target)
                showAddDialog = false
            }
        )
    }

    if (showDailyGoalDialog) {
        DailyGoalDialog(
            currentGoal = uiState.dailyTasbihTarget,
            onDismiss = { showDailyGoalDialog = false },
            onConfirm = { newGoal ->
                onUpdateDailyTarget(newGoal)
                showDailyGoalDialog = false
            }
        )
    }

    if (showCustomTimerDialog) {
        CustomSessionTimerDialog(
            currentMinutes = uiState.sessionTimer.selectedMinutes,
            onDismiss = { showCustomTimerDialog = false },
            onConfirm = { minutes ->
                onSelectTimerDuration(minutes)
                showCustomTimerDialog = false
            }
        )
    }

    if (showCustomAutoCountDialog) {
        CustomAutoCountTargetDialog(
            currentTarget = uiState.autoCount.targetCount,
            onDismiss = { showCustomAutoCountDialog = false },
            onConfirm = { target ->
                onSetAutoCountTarget(target)
                showCustomAutoCountDialog = false
            }
        )
    }

    if (showCustomCycleTargetDialog && activePreset != null) {
        CustomTasbihCycleTargetDialog(
            currentTarget = activePreset.targetCount,
            onDismiss = { showCustomCycleTargetDialog = false },
            onConfirm = { target ->
                onUpdateTarget(activePreset.id, target)
                showCustomCycleTargetDialog = false
            }
        )
    }
}

@Composable
private fun SessionTimerCard(
    timerState: TasbihSessionTimerState,
    onSelectDuration: (Int) -> Unit,
    onOpenCustomDuration: () -> Unit,
    onStartOrResume: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onToggleSoundAlert: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val presetMinutes = remember { listOf(3, 5, 10, 15) }
    val animatedProgress by animateFloatAsState(
        targetValue = timerState.progressFraction.coerceIn(0f, 1f),
        animationSpec = spring(),
        label = "tasbih_session_timer_progress"
    )
    val isFinished = timerState.status == SessionTimerStatus.FINISHED
    val isRunning = timerState.status == SessionTimerStatus.RUNNING
    val isPaused = timerState.status == SessionTimerStatus.PAUSED

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tasbih_session_timer_card"),
        shape = RoundedCornerShape(22.dp),
        color = if (isFinished) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isFinished) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.tasbih_timer_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.tasbih_timer_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { onToggleSoundAlert(!timerState.soundAlertEnabled) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("tasbih_timer_sound_toggle")
                    ) {
                        Icon(
                            imageVector = if (timerState.soundAlertEnabled) {
                                Icons.Rounded.NotificationsActive
                            } else {
                                Icons.Rounded.NotificationsOff
                            },
                            contentDescription = stringResource(R.string.tasbih_timer_sound_cd),
                            tint = if (timerState.soundAlertEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            }
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isRunning) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = timerState.formattedTime,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = if (isRunning) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("tasbih_timer_countdown")
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (isFinished) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                presetMinutes.forEach { minutes ->
                    FilterChip(
                        selected = timerState.selectedMinutes == minutes,
                        onClick = { onSelectDuration(minutes) },
                        label = {
                            Text(
                                text = stringResource(R.string.tasbih_timer_minutes_chip, minutes),
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        modifier = Modifier.testTag("tasbih_timer_preset_$minutes")
                    )
                }

                FilterChip(
                    selected = timerState.selectedMinutes !in presetMinutes,
                    onClick = onOpenCustomDuration,
                    label = {
                        Text(
                            text = if (timerState.selectedMinutes !in presetMinutes) {
                                stringResource(R.string.tasbih_timer_minutes_chip, timerState.selectedMinutes)
                            } else {
                                stringResource(R.string.tasbih_timer_custom)
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    modifier = Modifier.testTag("tasbih_timer_custom_button")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (isRunning) onPause() else onStartOrResume()
                    },
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("tasbih_timer_start_pause_button")
                ) {
                    Icon(
                        imageVector = when {
                            isRunning -> Icons.Rounded.Pause
                            isFinished -> Icons.Rounded.Replay
                            else -> Icons.Rounded.PlayArrow
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = when {
                            isRunning -> stringResource(R.string.tasbih_timer_pause)
                            isPaused -> stringResource(R.string.tasbih_timer_resume)
                            isFinished -> stringResource(R.string.tasbih_timer_restart)
                            else -> stringResource(R.string.tasbih_timer_start)
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                FilledTonalButton(
                    onClick = onReset,
                    enabled = timerState.status != SessionTimerStatus.IDLE ||
                        timerState.remainingSeconds != timerState.totalSeconds,
                    shape = CircleShape,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("tasbih_timer_reset_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = stringResource(R.string.tasbih_timer_reset),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            AnimatedVisibility(
                visible = isFinished,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tasbih_timer_finished_banner"),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.tasbih_timer_finished_banner),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomSessionTimerDialog(
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var minutesText by remember { mutableStateOf(currentMinutes.toString()) }
    val parsedMinutes = minutesText.toIntOrNull()
    val isValid = parsedMinutes != null && parsedMinutes in 1..180

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("tasbih_timer_custom_dialog"),
        title = { Text(stringResource(R.string.tasbih_timer_custom_title)) },
        text = {
            OutlinedTextField(
                value = minutesText,
                onValueChange = { input ->
                    minutesText = input.filter { it.isDigit() }.take(3)
                },
                label = { Text(stringResource(R.string.tasbih_timer_custom_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = minutesText.isNotEmpty() && !isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tasbih_timer_custom_input")
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (parsedMinutes != null && isValid) {
                        onConfirm(parsedMinutes)
                    }
                },
                enabled = isValid,
                modifier = Modifier.testTag("confirm_tasbih_timer_custom")
            ) {
                Text(stringResource(R.string.tasbih_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun DailyGoalProgressCard(
    todayCount: Int,
    dailyTarget: Int,
    progress: Float,
    isCelebrating: Boolean,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(),
        label = "tasbih_daily_goal_progress"
    )
    val cardScale by animateFloatAsState(
        targetValue = if (isCelebrating) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 360f),
        label = "tasbih_daily_goal_celebration_scale"
    )
    val isGoalReached = todayCount >= dailyTarget && dailyTarget > 0

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(cardScale),
        shape = RoundedCornerShape(20.dp),
        color = if (isGoalReached) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isGoalReached) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("tasbih_daily_goal_progress"),
                    color = if (isGoalReached) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 5.dp,
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.tasbih_daily_goal_title),
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
                    text = "$todayCount / $dailyTarget",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("tasbih_daily_goal_text")
                )
                if (isGoalReached) {
                    Text(
                        text = stringResource(R.string.tasbih_daily_goal_done),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            IconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("tasbih_edit_daily_goal_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.tasbih_edit_daily_goal),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(68.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AddCustomTasbihDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var phrase by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("33") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tasbih_add_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = phrase,
                    onValueChange = { phrase = it },
                    label = { Text(stringResource(R.string.tasbih_phrase_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_tasbih_phrase_input")
                )
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { input ->
                        targetText = input.filter { it.isDigit() }.take(4)
                    },
                    label = { Text(stringResource(R.string.tasbih_target_input_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_tasbih_target_input")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val target = targetText.toIntOrNull() ?: 33
                    if (phrase.isNotBlank()) {
                        onConfirm(phrase, target)
                    }
                },
                enabled = phrase.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_tasbih")
            ) {
                Text(stringResource(R.string.tasbih_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun DailyGoalDialog(
    currentGoal: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var goalText by remember { mutableStateOf(currentGoal.toString()) }
    val parsedGoal = goalText.toIntOrNull()
    val isValid = parsedGoal != null && parsedGoal in 10..10000
    val presets = listOf(100, 300, 500, 1000)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tasbih_edit_daily_goal)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = parsedGoal == preset,
                            onClick = { goalText = preset.toString() },
                            label = {
                                Text(
                                    text = "$preset",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            modifier = Modifier.testTag("daily_goal_preset_$preset")
                        )
                    }
                }
                OutlinedTextField(
                    value = goalText,
                    onValueChange = { input ->
                        goalText = input.filter { it.isDigit() }.take(5)
                    },
                    label = { Text(stringResource(R.string.tasbih_daily_goal_input)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = goalText.isNotEmpty() && !isValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tasbih_daily_goal_input")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (parsedGoal != null) {
                        onConfirm(parsedGoal.coerceIn(10, 10000))
                    }
                },
                enabled = isValid,
                modifier = Modifier.testTag("confirm_daily_goal")
            ) {
                Text(stringResource(R.string.tasbih_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
