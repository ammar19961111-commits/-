package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.TasbihTargetSoundEffect
import com.example.data.model.TasbihVibrationPattern

@Composable
fun TasbihTargetAlertCard(
    activeTargetCount: Int,
    selectedVibrationPattern: TasbihVibrationPattern,
    selectedSoundEffect: TasbihTargetSoundEffect,
    hapticEnabled: Boolean,
    soundEnabled: Boolean,
    onSelectVibrationPattern: (TasbihVibrationPattern) -> Unit,
    onPreviewVibration: (TasbihVibrationPattern) -> Unit,
    onSelectSoundEffect: (TasbihTargetSoundEffect) -> Unit,
    onPreviewSound: (TasbihTargetSoundEffect) -> Unit,
    onEditCustomTarget: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tasbih_target_alert_card"),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.tasbih_target_alert_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (activeTargetCount > 0) {
                                stringResource(R.string.tasbih_target_alert_sub, activeTargetCount)
                            } else {
                                stringResource(R.string.tasbih_target_alert_sub_open)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FilterChip(
                    selected = activeTargetCount > 0 && activeTargetCount !in listOf(33, 99, 100),
                    onClick = onEditCustomTarget,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text(
                            text = if (activeTargetCount > 0) "$activeTargetCount"
                            else stringResource(R.string.tasbih_open_target),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    modifier = Modifier.testTag("tasbih_target_alert_edit_target")
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Vibration Pattern Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Vibration,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.tasbih_target_alert_vibration_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedButton(
                        onClick = { onPreviewVibration(selectedVibrationPattern) },
                        enabled = hapticEnabled && selectedVibrationPattern != TasbihVibrationPattern.NONE,
                        shape = CircleShape,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("tasbih_vibration_preview_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = stringResource(R.string.tasbih_target_alert_preview_vibration),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val patterns = listOf(
                        TasbihVibrationPattern.GENTLE_PULSE to R.string.tasbih_vibration_gentle_pulse,
                        TasbihVibrationPattern.HEARTBEAT to R.string.tasbih_vibration_heartbeat,
                        TasbihVibrationPattern.LONG_HOLD to R.string.tasbih_vibration_long_hold,
                        TasbihVibrationPattern.CRESCENDO to R.string.tasbih_vibration_crescendo,
                        TasbihVibrationPattern.NONE to R.string.tasbih_vibration_none
                    )
                    patterns.forEach { (pattern, labelRes) ->
                        FilterChip(
                            selected = selectedVibrationPattern == pattern,
                            onClick = { onSelectVibrationPattern(pattern) },
                            label = {
                                Text(
                                    text = stringResource(labelRes),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("tasbih_vibration_pattern_${pattern.name.lowercase()}")
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Sound Effect Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.tasbih_target_alert_sound_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedButton(
                        onClick = { onPreviewSound(selectedSoundEffect) },
                        enabled = soundEnabled && selectedSoundEffect != TasbihTargetSoundEffect.NONE,
                        shape = CircleShape,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("tasbih_sound_preview_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = stringResource(R.string.tasbih_target_alert_preview_sound),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sounds = listOf(
                        TasbihTargetSoundEffect.GENTLE_CHIME to R.string.tasbih_sound_gentle_chime,
                        TasbihTargetSoundEffect.SACRED_HARP to R.string.tasbih_sound_sacred_harp,
                        TasbihTargetSoundEffect.CRYSTAL_BELL to R.string.tasbih_sound_crystal_bell,
                        TasbihTargetSoundEffect.SOFT_TONE to R.string.tasbih_sound_soft_tone,
                        TasbihTargetSoundEffect.NONE to R.string.tasbih_sound_none
                    )
                    sounds.forEach { (effect, labelRes) ->
                        FilterChip(
                            selected = selectedSoundEffect == effect,
                            onClick = { onSelectSoundEffect(effect) },
                            label = {
                                Text(
                                    text = stringResource(labelRes),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                                selectedLabelColor = MaterialTheme.colorScheme.onTertiary
                            ),
                            modifier = Modifier.testTag("tasbih_sound_effect_${effect.name.lowercase()}")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomTasbihCycleTargetDialog(
    currentTarget: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val initial = if (currentTarget > 0) currentTarget.toString() else "33"
    var targetText by remember { mutableStateOf(initial) }
    val parsed = targetText.toIntOrNull()
    val isValid = parsed != null && parsed in 1..10000

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("tasbih_target_custom_dialog"),
        title = { Text(stringResource(R.string.tasbih_custom_target_title)) },
        text = {
            OutlinedTextField(
                value = targetText,
                onValueChange = { input ->
                    targetText = input.filter { it.isDigit() }.take(5)
                },
                label = { Text(stringResource(R.string.tasbih_custom_target_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = targetText.isNotEmpty() && !isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tasbih_target_custom_input")
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (parsed != null && isValid) {
                        onConfirm(parsed)
                    }
                },
                enabled = isValid,
                modifier = Modifier.testTag("confirm_tasbih_target_custom")
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
