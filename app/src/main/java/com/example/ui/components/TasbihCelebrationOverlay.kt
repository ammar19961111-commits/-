package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Immutable
private data class CelebrationParticle(
    val angleRad: Float,
    val speedFactor: Float,
    val radiusDp: Float,
    val startDelayFraction: Float,
    val driftGravity: Float,
    val spinDegrees: Float,
    val isDiamond: Boolean,
    val colorIndex: Int
)

/**
 * Renders subtle concentric ripples around the Tasbeeh medallion along with delicate
 * emerald-and-gold confetti motes when the user completes their daily Tasbeeh target.
 */
@Composable
fun TasbihRippleBurstCanvas(
    celebrationTrigger: Int,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    var isActive by remember { mutableStateOf(false) }

    val particles = remember {
        val rng = Random(2026)
        List(28) { i ->
            val baseAngle = (i.toFloat() / 28f) * (2f * PI.toFloat())
            val jitter = (rng.nextFloat() - 0.5f) * 0.25f
            CelebrationParticle(
                angleRad = baseAngle + jitter,
                speedFactor = 0.55f + rng.nextFloat() * 0.55f,
                radiusDp = 2.5f + rng.nextFloat() * 3.0f,
                startDelayFraction = rng.nextFloat() * 0.14f,
                driftGravity = 26f + rng.nextFloat() * 34f,
                spinDegrees = (rng.nextFloat() - 0.5f) * 280f,
                isDiamond = i % 3 == 0,
                colorIndex = i % 4
            )
        }
    }

    LaunchedEffect(celebrationTrigger) {
        if (celebrationTrigger > 0) {
            isActive = true
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1900, easing = LinearEasing)
            )
            isActive = false
        }
    }

    if (!isActive) return

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val palette = remember(primary, tertiary) {
        listOf(
            tertiary,
            primary,
            Color(0xFFF3D58D),
            Color(0xFF74B89B)
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("tasbih_celebration_ripple")
    ) {
        val t = progress.value.coerceIn(0f, 1f)
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxRadius = size.minDimension * 0.56f
        val innerEdge = size.minDimension * 0.34f

        // Three soft concentric ripples radiating from the medallion
        val rippleOffsets = floatArrayOf(0f, 0.16f, 0.32f)
        rippleOffsets.forEachIndexed { idx, offset ->
            val localT = ((t - offset) / (1f - offset)).coerceIn(0f, 1f)
            if (localT > 0f && localT < 1f) {
                val eased = 1f - (1f - localT) * (1f - localT)
                val radius = innerEdge + (maxRadius - innerEdge) * eased
                val alpha = ((1f - localT) * 0.42f).coerceIn(0f, 1f)
                val stroke = (4.5f - idx * 1.1f).dp.toPx() * (1f - localT * 0.5f)
                drawCircle(
                    color = (if (idx % 2 == 0) tertiary else primary).copy(alpha = alpha),
                    radius = radius,
                    center = Offset(cx, cy),
                    style = Stroke(width = stroke.coerceAtLeast(1f))
                )
            }
        }

        // Delicate confetti sparkles fanning softly outward
        val burstSpan = size.minDimension * 0.30f
        particles.forEach { p ->
            val pt = ((t - p.startDelayFraction) / (1f - p.startDelayFraction)).coerceIn(0f, 1f)
            if (pt > 0f && pt < 1f) {
                val eased = 1f - (1f - pt) * (1f - pt) * (1f - pt)
                val dist = innerEdge * 0.85f + burstSpan * p.speedFactor * eased
                val x = cx + cos(p.angleRad) * dist
                val y = cy + sin(p.angleRad) * dist + (p.driftGravity.dp.toPx() * pt * pt)
                val fadeInAlpha = (pt / 0.12f).coerceIn(0f, 1f)
                val fadeOutAlpha = ((1f - pt) / 0.35f).coerceIn(0f, 1f)
                val alpha = minOf(fadeInAlpha, fadeOutAlpha) * 0.88f
                val color = palette[p.colorIndex].copy(alpha = alpha)
                val r = p.radiusDp.dp.toPx()

                rotate(degrees = p.spinDegrees * pt, pivot = Offset(x, y)) {
                    if (p.isDiamond) {
                        val diamond = Path().apply {
                            moveTo(x, y - r * 1.3f)
                            lineTo(x + r, y)
                            lineTo(x, y + r * 1.3f)
                            lineTo(x - r, y)
                            close()
                        }
                        drawPath(path = diamond, color = color)
                    } else {
                        drawOval(
                            color = color,
                            topLeft = Offset(x - r, y - r * 0.6f),
                            size = Size(r * 2f, r * 1.2f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TasbihCelebrationBanner(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(260)) + scaleIn(initialScale = 0.92f, animationSpec = tween(260)),
            exit = fadeOut(tween(320)) + scaleOut(targetScale = 0.95f, animationSpec = tween(320))
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)),
                tonalElevation = 2.dp,
                modifier = Modifier.testTag("tasbih_celebration_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.tasbih_celebration_message),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}
