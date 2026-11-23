package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.RosaryBeadDesign
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Reusable Material 3 Digital Tasbeeh counter component with a large circular tap medallion,
 * animated progress ring, expanding pulse & numeral pop visual feedback on every increment,
 * optional haptic feedback, and a reset button.
 */
@Composable
fun DigitalTasbeehCounter(
    currentCount: Int,
    targetCount: Int,
    onIncrement: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    celebrationTrigger: Int = 0,
    hapticEnabled: Boolean = true,
    dailyTasbihTarget: Int = 0,
    todayTasbihSum: Int = 0,
    beadDesign: RosaryBeadDesign = RosaryBeadDesign.CLASSIC_RING
) {
    val hapticFeedback = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 420f),
        label = "tasbih_press_scale"
    )

    val targetProgress = if (targetCount <= 0) {
        (currentCount % 33) / 33f
    } else {
        (currentCount.toFloat() / targetCount.toFloat()).coerceIn(0f, 1f)
    }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 300f),
        label = "tasbih_ring_progress"
    )

    // Visual feedback animation triggered whenever currentCount increments
    var previousCount by remember { mutableIntStateOf(currentCount) }
    var isPulseVisible by remember { mutableStateOf(false) }
    val pulseProgress = remember { Animatable(0f) }
    val countPopScale = remember { Animatable(1f) }

    LaunchedEffect(currentCount) {
        if (currentCount > previousCount) {
            isPulseVisible = true
            pulseProgress.snapTo(0f)
            countPopScale.snapTo(1f)
            val pulseJob = launch {
                pulseProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
                )
                isPulseVisible = false
            }
            val popJob = launch {
                countPopScale.animateTo(
                    targetValue = 1.15f,
                    animationSpec = tween(durationMillis = 95, easing = FastOutSlowInEasing)
                )
                countPopScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 450f)
                )
            }
            pulseJob.join()
            popJob.join()
        }
        previousCount = currentCount
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val tapContentDescription = stringResource(R.string.tap_to_count)

    val triggerIncrement: () -> Unit = {
        if (hapticEnabled) {
            val isCycleMilestone = targetCount > 0 && currentCount + 1 == targetCount
            val isDailyGoalMilestone = dailyTasbihTarget > 0 &&
                todayTasbihSum + 1 == dailyTasbihTarget
            hapticFeedback.performHapticFeedback(
                if (isCycleMilestone || isDailyGoalMilestone) {
                    HapticFeedbackType.LongPress
                } else {
                    HapticFeedbackType.TextHandleMove
                }
            )
        }
        onIncrement()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(276.dp),
            contentAlignment = Alignment.Center
        ) {
            TasbihRippleBurstCanvas(
                celebrationTrigger = celebrationTrigger,
                modifier = Modifier.matchParentSize()
            )

            // Increment pulse halo feedback ring
            if (isPulseVisible) {
                Canvas(
                    modifier = Modifier
                        .size(270.dp)
                        .testTag("tasbih_increment_pulse_ring")
                ) {
                    val t = pulseProgress.value.coerceIn(0f, 1f)
                    val baseRadius = size.minDimension * 0.37f
                    val maxRadius = size.minDimension * 0.50f
                    val radius = baseRadius + (maxRadius - baseRadius) * t
                    val alpha = ((1f - t) * 0.5f).coerceIn(0f, 1f)
                    drawCircle(
                        color = primaryColor.copy(alpha = alpha * 0.16f),
                        radius = radius,
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                    drawCircle(
                        color = tertiaryColor.copy(alpha = alpha),
                        radius = radius,
                        center = Offset(size.width / 2f, size.height / 2f),
                        style = Stroke(width = (5f * (1f - t * 0.65f)).dp.toPx().coerceAtLeast(1.5f))
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(236.dp)
                    .scale(pressScale)
                    .clip(CircleShape)
                    .semantics {
                        contentDescription = tapContentDescription
                    }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(
                            bounded = true,
                            color = primaryColor.copy(alpha = 0.25f)
                        ),
                        role = Role.Button,
                        onClick = triggerIncrement
                    )
                    .testTag("tasbih_tap_circle"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    val diameter = size.minDimension - strokeWidth * 2
                    val topLeft = Offset(
                        (size.width - diameter) / 2f,
                        (size.height - diameter) / 2f
                    )
                    if (beadDesign == RosaryBeadDesign.CLASSIC_RING) {
                        // Track arc
                        drawArc(
                            color = trackColor,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(diameter, diameter),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        // Progress arc
                        drawArc(
                            color = primaryColor,
                            startAngle = -90f,
                            sweepAngle = animatedProgress * 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(diameter, diameter),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        // Golden bead indicator at tip
                        if (animatedProgress > 0f) {
                            val angleRad = Math.toRadians((-90.0 + animatedProgress * 360.0))
                            val radius = diameter / 2f
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val beadPos = Offset(
                                x = center.x + (radius * cos(angleRad)).toFloat(),
                                y = center.y + (radius * sin(angleRad)).toFloat()
                            )
                            drawCircle(
                                color = tertiaryColor,
                                radius = strokeWidth * 0.62f,
                                center = beadPos
                            )
                        }
                    } else {
                        drawCustomRosaryBeads(
                            beadDesign = beadDesign,
                            animatedProgress = animatedProgress,
                            diameter = diameter,
                            ringWidth = strokeWidth,
                            outlineTint = trackColor
                        )
                    }
                }

                Surface(
                    modifier = Modifier.size(180.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        AnimatedContent(
                            targetState = currentCount,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    (slideInVertically(tween(130)) { it / 4 } + fadeIn(tween(130)))
                                        .togetherWith(slideOutVertically(tween(110)) { -it / 4 } + fadeOut(tween(110)))
                                } else {
                                    fadeIn(tween(160)) togetherWith fadeOut(tween(120))
                                }
                            },
                            label = "tasbih_count_digit_transition"
                        ) { countValue ->
                            Text(
                                text = "$countValue",
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 56.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .scale(countPopScale.value)
                                    .testTag("tasbih_current_count_text")
                            )
                        }
                        Text(
                            text = if (targetCount > 0) {
                                stringResource(R.string.tasbih_of_target, targetCount)
                            } else {
                                stringResource(R.string.tasbih_open_target)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = triggerIncrement,
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                modifier = Modifier
                    .weight(1.35f)
                    .heightIn(min = 52.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("tasbih_increment_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.tasbih_increment_button),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1
                )
            }

            FilledTonalButton(
                onClick = {
                    if (hapticEnabled) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    onReset()
                },
                enabled = currentCount > 0,
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("tasbih_reset_cycle_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = stringResource(R.string.tasbih_reset_cycle),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1
                )
            }
        }
    }
}

private fun DrawScope.drawCustomRosaryBeads(
    beadDesign: RosaryBeadDesign,
    animatedProgress: Float,
    diameter: Float,
    ringWidth: Float,
    outlineTint: Color
) {
    val totalBeads = 33
    val radius = diameter / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    val litCount = (animatedProgress * totalBeads).roundToInt().coerceIn(0, totalBeads)

    // Braided cord beneath the beads
    drawCircle(
        color = beadDesign.dividerBeadColor.copy(alpha = 0.35f),
        radius = radius,
        center = center,
        style = Stroke(width = 2.5.dp.toPx())
    )

    for (index in 0 until totalBeads) {
        val angleDeg = -90.0 + (360.0 * index / totalBeads)
        val angleRad = Math.toRadians(angleDeg)
        val beadCenter = Offset(
            x = center.x + (radius * cos(angleRad)).toFloat(),
            y = center.y + (radius * sin(angleRad)).toFloat()
        )
        val isDivider = index % 11 == 0
        val isLit = index < litCount
        val isActiveTip = litCount in 1..totalBeads && index == litCount - 1

        val baseRadius = when {
            isDivider -> ringWidth * 0.56f
            isActiveTip -> ringWidth * 0.52f
            else -> ringWidth * 0.43f
        }

        val fillColor = when {
            isDivider -> beadDesign.dividerBeadColor
            isLit -> beadDesign.primaryBeadColor
            else -> beadDesign.trackBeadColor.copy(alpha = 0.72f)
        }

        if (isActiveTip) {
            drawCircle(
                color = beadDesign.sheenColor.copy(alpha = 0.45f),
                radius = baseRadius * 1.38f,
                center = beadCenter
            )
        }

        drawCircle(
            color = outlineTint.copy(alpha = 0.45f),
            radius = baseRadius + 1.dp.toPx(),
            center = beadCenter
        )
        drawCircle(
            color = fillColor,
            radius = baseRadius,
            center = beadCenter
        )

        // Material-specific texture accents
        when (beadDesign) {
            RosaryBeadDesign.PEARL_BEADS -> {
                if (isLit || isDivider) {
                    drawCircle(
                        color = beadDesign.sheenColor.copy(alpha = 0.85f),
                        radius = baseRadius * 0.32f,
                        center = beadCenter - Offset(baseRadius * 0.28f, baseRadius * 0.28f)
                    )
                }
            }
            RosaryBeadDesign.AMBER_TASBEEH -> {
                drawCircle(
                    color = beadDesign.sheenColor.copy(alpha = if (isLit) 0.55f else 0.25f),
                    radius = baseRadius * 0.52f,
                    center = beadCenter
                )
            }
            RosaryBeadDesign.LAPIS_LAZULI -> {
                if (isLit || isDivider) {
                    drawCircle(
                        color = beadDesign.dividerBeadColor.copy(alpha = 0.9f),
                        radius = baseRadius * 0.24f,
                        center = beadCenter + Offset(baseRadius * 0.22f, -baseRadius * 0.2f)
                    )
                }
            }
            RosaryBeadDesign.OLIVE_WOOD -> {
                drawCircle(
                    color = beadDesign.sheenColor.copy(alpha = if (isLit) 0.45f else 0.2f),
                    radius = baseRadius * 0.62f,
                    center = beadCenter,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }
            RosaryBeadDesign.CLASSIC_RING -> Unit
        }
    }
}

/**
 * Self-contained stateful Electronic Tasbeeh UI component with built-in counter state,
 * tap-to-increment interaction, Reset button, and light haptic vibration on each tap.
 */
@Composable
fun DigitalTasbeehCounter(
    modifier: Modifier = Modifier,
    initialCount: Int = 0,
    targetCount: Int = 33,
    hapticEnabled: Boolean = true,
    beadDesign: RosaryBeadDesign = RosaryBeadDesign.CLASSIC_RING,
    onCountChanged: (Int) -> Unit = {}
) {
    var count by rememberSaveable { mutableIntStateOf(initialCount) }
    var celebrationTrigger by remember { mutableIntStateOf(0) }

    DigitalTasbeehCounter(
        currentCount = count,
        targetCount = targetCount,
        onIncrement = {
            val next = count + 1
            count = next
            if (targetCount > 0 && next == targetCount) {
                celebrationTrigger++
            }
            onCountChanged(next)
        },
        onReset = {
            count = 0
            onCountChanged(0)
        },
        modifier = modifier,
        celebrationTrigger = celebrationTrigger,
        hapticEnabled = hapticEnabled,
        beadDesign = beadDesign
    )
}


