package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AdhkarTranslations
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.LocalAppLanguage

/**
 * Interactive Electronic Tasbeeh UI component for quick counting anywhere (e.g., HomeScreen).
 * Features a tappable counter surface/button to increment, a Reset button, and light haptic
 * vibration on every tap.
 */
@Composable
fun ElectronicTasbeehCard(
    presetId: Int,
    phraseArabic: String,
    currentCount: Int,
    targetCount: Int,
    todayTasbihSum: Int,
    hapticEnabled: Boolean,
    onIncrement: () -> Unit,
    onReset: () -> Unit,
    onOpenFullScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val language = LocalAppLanguage.current
    val hapticFeedback = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 420f),
        label = "quick_tasbih_press_scale"
    )

    val progress = if (targetCount > 0) {
        (currentCount.toFloat() / targetCount.toFloat()).coerceIn(0f, 1f)
    } else {
        (currentCount % 33) / 33f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
        label = "quick_tasbih_progress"
    )

    val triggerIncrement: () -> Unit = {
        if (hapticEnabled) {
            val isMilestone = targetCount > 0 && currentCount + 1 == targetCount
            hapticFeedback.performHapticFeedback(
                if (isMilestone) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
            )
        }
        onIncrement()
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onOpenFullScreen)
            .testTag("home_tasbih_shortcut"),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.58f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row + open full screen arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RadioButtonChecked,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.tasbih_quick_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        val phraseDisplay = if (language == AppLanguage.ENGLISH) {
                            AdhkarTranslations.forCounter(presetId) ?: phraseArabic
                        } else {
                            phraseArabic
                        }
                        Text(
                            text = "$phraseDisplay • ${
                                stringResource(R.string.tasbih_today_label)
                            }: $todayTasbihSum",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onOpenFullScreen,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("home_tasbih_open_full_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = stringResource(R.string.tasbih_open_full_cd),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Interactive counter area: tap-to-count medallion + reset control
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .weight(1f)
                        .scale(pressScale)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                            role = Role.Button,
                            onClick = triggerIncrement
                        )
                        .heightIn(min = 56.dp)
                        .testTag("home_tasbih_tap_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(38.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier.size(38.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    strokeWidth = 3.5.dp,
                                    strokeCap = StrokeCap.Round
                                )
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    text = stringResource(R.string.tasbih_quick_tap_hint),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (targetCount > 0) {
                                        stringResource(R.string.tasbih_of_target, targetCount)
                                    } else {
                                        stringResource(R.string.tasbih_open_target)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        AnimatedContent(
                            targetState = currentCount,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    (slideInVertically(tween(120)) { it / 3 } + fadeIn(tween(120)))
                                        .togetherWith(slideOutVertically(tween(100)) { -it / 3 } + fadeOut(tween(100)))
                                } else {
                                    fadeIn(tween(150)) togetherWith fadeOut(tween(120))
                                }
                            },
                            label = "home_tasbih_count_anim"
                        ) { value ->
                            Text(
                                text = "$value",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("home_tasbih_count_text")
                            )
                        }
                    }
                }

                FilledTonalButton(
                    onClick = {
                        if (hapticEnabled) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onReset()
                    },
                    enabled = currentCount > 0,
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                    modifier = Modifier
                        .heightIn(min = 56.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("home_tasbih_reset_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.tasbih_reset_cycle),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = stringResource(R.string.reset_single),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
