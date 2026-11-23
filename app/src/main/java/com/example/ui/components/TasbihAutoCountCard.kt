package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.viewmodel.AutoCountPace
import com.example.ui.viewmodel.AutoCountStatus
import com.example.ui.viewmodel.TasbihAutoCountState

@Composable
fun AutoCountCard(
    autoCountState: TasbihAutoCountState,
    onSelectTarget: (Int) -> Unit,
    onOpenCustomTarget: () -> Unit,
    onSelectPace: (AutoCountPace) -> Unit,
    onStartOrResume: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onToggleSoundAlert: (Boolean) -> Unit,
    onToggleHapticAlert: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val presetTargets = remember { listOf(33, 66, 99, 100) }
    val animatedProgress by animateFloatAsState(
        targetValue = autoCountState.progressFraction,
        animationSpec = spring(),
        label = "tasbih_autocount_progress"
    )
    val isRunning = autoCountState.status == AutoCountStatus.RUNNING
    val isPaused = autoCountState.status == AutoCountStatus.PAUSED
    val isCompleted = autoCountState.status == AutoCountStatus.COMPLETED

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tasbih_autocount_card"),
        shape = RoundedCornerShape(22.dp),
        color = if (isCompleted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.44f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isCompleted) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.52f)
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
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.tasbih_autocount_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.tasbih_autocount_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { onToggleHapticAlert(!autoCountState.hapticAlertEnabled) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("tasbih_autocount_haptic_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Vibration,
                            contentDescription = stringResource(R.string.tasbih_autocount_haptic_cd),
                            tint = if (autoCountState.hapticAlertEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            }
                        )
                    }

                    IconButton(
                        onClick = { onToggleSoundAlert(!autoCountState.soundAlertEnabled) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("tasbih_autocount_sound_toggle")
                    ) {
                        Icon(
                            imageVector = if (autoCountState.soundAlertEnabled) {
                                Icons.Rounded.NotificationsActive
                            } else {
                                Icons.Rounded.NotificationsOff
                            },
                            contentDescription = stringResource(R.string.tasbih_autocount_sound_cd),
                            tint = if (autoCountState.soundAlertEnabled) {
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
                            text = stringResource(
                                R.string.tasbih_autocount_progress_format,
                                autoCountState.completedCount,
                                autoCountState.targetCount
                            ),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = if (isRunning) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("tasbih_autocount_progress_text")
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
                color = if (isCompleted) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            // Target count presets + custom target dialog trigger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.tasbih_autocount_target_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                presetTargets.forEach { target ->
                    FilterChip(
                        selected = autoCountState.targetCount == target,
                        onClick = { onSelectTarget(target) },
                        label = {
                            Text(
                                text = "$target",
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("tasbih_autocount_target_$target")
                    )
                }

                FilterChip(
                    selected = autoCountState.targetCount !in presetTargets,
                    onClick = onOpenCustomTarget,
                    label = {
                        Text(
                            text = if (autoCountState.targetCount !in presetTargets) {
                                "${autoCountState.targetCount}"
                            } else {
                                stringResource(R.string.tasbih_autocount_custom)
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("tasbih_autocount_custom_button")
                )
            }

            // Counting pace selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val paces = listOf(
                    AutoCountPace.SLOW to R.string.tasbih_autocount_pace_slow,
                    AutoCountPace.MEDIUM to R.string.tasbih_autocount_pace_medium,
                    AutoCountPace.FAST to R.string.tasbih_autocount_pace_fast
                )
                paces.forEach { (pace, labelRes) ->
                    FilterChip(
                        selected = autoCountState.pace == pace,
                        onClick = { onSelectPace(pace) },
                        label = {
                            Text(
                                text = stringResource(labelRes),
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tasbih_autocount_pace_${pace.name.lowercase()}")
                    )
                }
            }

            // Start / Pause / Reset controls
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
                        .testTag("tasbih_autocount_start_pause_button")
                ) {
                    Icon(
                        imageVector = when {
                            isRunning -> Icons.Rounded.Pause
                            isCompleted -> Icons.Rounded.Replay
                            else -> Icons.Rounded.PlayArrow
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = when {
                            isRunning -> stringResource(R.string.tasbih_autocount_pause)
                            isPaused -> stringResource(R.string.tasbih_autocount_resume)
                            isCompleted -> stringResource(R.string.tasbih_autocount_restart)
                            else -> stringResource(R.string.tasbih_autocount_start)
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                FilledTonalButton(
                    onClick = onReset,
                    enabled = autoCountState.status != AutoCountStatus.IDLE ||
                        autoCountState.completedCount > 0,
                    shape = CircleShape,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("tasbih_autocount_reset_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = stringResource(R.string.tasbih_autocount_reset),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            AnimatedVisibility(
                visible = isCompleted,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tasbih_autocount_completed_banner"),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
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
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(
                                R.string.tasbih_autocount_completed_banner,
                                autoCountState.targetCount
                            ),
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
fun CustomAutoCountTargetDialog(
    currentTarget: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var targetText by remember { mutableStateOf(currentTarget.toString()) }
    val parsedTarget = targetText.toIntOrNull()
    val isValid = parsedTarget != null && parsedTarget in 1..10000

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("tasbih_autocount_custom_dialog"),
        title = { Text(stringResource(R.string.tasbih_autocount_custom_title)) },
        text = {
            OutlinedTextField(
                value = targetText,
                onValueChange = { input ->
                    targetText = input.filter { it.isDigit() }.take(5)
                },
                label = { Text(stringResource(R.string.tasbih_autocount_custom_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = targetText.isNotEmpty() && !isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tasbih_autocount_custom_input")
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (parsedTarget != null && isValid) {
                        onConfirm(parsedTarget)
                    }
                },
                enabled = isValid,
                modifier = Modifier.testTag("confirm_tasbih_autocount_custom")
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
