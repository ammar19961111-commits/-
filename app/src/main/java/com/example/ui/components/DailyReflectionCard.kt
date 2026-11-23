package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.DailyReflection
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.LocalAppLanguage

@Composable
fun DailyReflectionCard(
    reflection: DailyReflection,
    millisUntilNextRefresh: Long,
    fontSizeSp: Float,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val language = LocalAppLanguage.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val refreshLabel = formatRefreshCountdown(millisUntilNextRefresh)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_daily_reflection_card"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.28f),
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
                        )
                    )
                )
        ) {
            // Subtle decorative geometric crescent & arch rings on the canvas
            Canvas(modifier = Modifier.matchParentSize()) {
                val ringCenter = Offset(x = size.width * 0.12f, y = size.height * 0.18f)
                drawCircle(
                    color = tertiaryColor.copy(alpha = 0.08f),
                    radius = 54.dp.toPx(),
                    center = ringCenter,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawArc(
                    color = primaryColor.copy(alpha = 0.09f),
                    startAngle = 210f,
                    sweepAngle = 190f,
                    useCenter = false,
                    topLeft = Offset(
                        x = size.width - 92.dp.toPx(),
                        y = size.height - 88.dp.toPx()
                    ),
                    size = Size(72.dp.toPx(), 72.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header: Title + Kind pill + 24h refresh badge + Copy/Share actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.reflection_card_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.testTag("home_reflection_kind_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MenuBook,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = stringResource(reflection.kind.labelRes),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                                modifier = Modifier.testTag("home_reflection_refresh_timer")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = refreshLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("home_reflection_copy")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = stringResource(R.string.reflection_copy_cd),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = onShare,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("home_reflection_share")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = stringResource(R.string.reflection_share_cd),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Quote text & source with vertical gold accent pill
                AnimatedContent(
                    targetState = reflection,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "daily_reflection_content"
                ) { current ->
                    val displayReflection = if (language == AppLanguage.ENGLISH && current.englishReflection.isNotBlank()) {
                        current.englishReflection
                    } else {
                        current.spiritualReflection
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(54.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(tertiaryColor, primaryColor)
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val quoteSizeSp = (fontSizeSp * 0.92f).coerceIn(18f, 28f)
                                val displayQuote = if (language == AppLanguage.ENGLISH && current.englishText.isNotBlank()) {
                                    current.englishText
                                } else {
                                    current.arabicText
                                }
                                val displaySource = if (language == AppLanguage.ENGLISH && current.englishSource.isNotBlank()) {
                                    current.englishSource
                                } else {
                                    current.sourceReference
                                }
                                Text(
                                    text = displayQuote,
                                    fontFamily = if (language == AppLanguage.ARABIC) AmiriFontFamily else MaterialTheme.typography.bodyLarge.fontFamily,
                                    fontSize = quoteSizeSp.sp,
                                    lineHeight = (quoteSizeSp * 1.6f).sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("home_reflection_text")
                                )
                                Text(
                                    text = displaySource,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.testTag("home_reflection_source")
                                )
                            }
                        }

                        // Practical daily reflection note
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_reflection_tip"),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.36f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.reflection_tip_label),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = displayReflection,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun formatRefreshCountdown(millisRemaining: Long): String {
    val totalMinutes = (millisRemaining / 60_000L).coerceAtLeast(1L)
    val hours = (totalMinutes / 60L).toInt()
    return if (hours >= 1) {
        stringResource(R.string.reflection_refresh_hours, hours)
    } else {
        stringResource(R.string.reflection_refresh_minutes, totalMinutes.toInt())
    }
}
