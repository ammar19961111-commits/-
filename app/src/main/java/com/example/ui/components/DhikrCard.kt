package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AdhkarTranslations
import com.example.data.model.Dhikr
import com.example.data.model.DhikrAudioCatalog
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.LocalAppLanguage

@Composable
fun DhikrCard(
    dhikr: Dhikr,
    fontSizeSp: Float,
    onTapIncrement: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    categoryLabel: String? = null,
    isAudioActive: Boolean = false,
    isAudioPlaying: Boolean = false,
    isAudioBuffering: Boolean = false,
    onToggleAudio: (() -> Unit)? = null,
    audioPositionMs: Long = 0L,
    audioDurationMs: Long = 0L,
    audioPlaybackSpeed: Float = 1.0f,
    audioRepeatCurrent: Boolean = false,
    audioHasError: Boolean = false,
    onSeekAudio: (Long) -> Unit = {},
    onCycleAudioSpeed: () -> Unit = {},
    onToggleAudioRepeat: () -> Unit = {},
    onRetryAudio: () -> Unit = {},
    onStopAudio: () -> Unit = {},
    audioTagPrefix: String = "",
    cardTestTag: String = "dhikr_card_${dhikr.id}"
) {
    val language = LocalAppLanguage.current
    val english = if (language == AppLanguage.ENGLISH) AdhkarTranslations.forDhikr(dhikr.id) else null
    val displayTitle = english?.titleEn ?: dhikr.title
    val displaySource = english?.sourceEn ?: dhikr.source
    val displayVirtue = english?.virtueEn ?: dhikr.virtue
    val isCompleted = dhikr.isCompleted
    val progress by animateFloatAsState(
        targetValue = if (dhikr.targetCount <= 0) 1f else (dhikr.currentCount.toFloat() / dhikr.targetCount).coerceIn(0f, 1f),
        animationSpec = spring(),
        label = "dhikr_progress"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isCompleted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "card_container_color"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isAudioActive -> MaterialTheme.colorScheme.primary
            isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            else -> MaterialTheme.colorScheme.outlineVariant
        },
        label = "card_border_color"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onTapIncrement)
            .testTag(cardTestTag),
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        tonalElevation = if (isCompleted) 0.dp else 2.dp,
        border = BorderStroke(if (isAudioActive) 1.75.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row: title + source + action icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (!categoryLabel.isNullOrBlank()) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = categoryLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleMedium,
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onToggleAudio != null) {
                        IconButton(
                            onClick = onToggleAudio,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("play_dhikr_audio_${dhikr.id}")
                        ) {
                            if (isAudioActive && isAudioBuffering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = if (isAudioActive && isAudioPlaying) {
                                        Icons.Rounded.Pause
                                    } else {
                                        Icons.Rounded.PlayArrow
                                    },
                                    contentDescription = stringResource(
                                        if (isAudioActive && isAudioPlaying) {
                                            R.string.dhikr_audio_pause_single
                                        } else {
                                            R.string.dhikr_audio_play_single
                                        }
                                    ),
                                    tint = if (isAudioActive) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("copy_dhikr_${dhikr.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = stringResource(R.string.copy_dhikr),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("favorite_dhikr_${dhikr.id}")
                    ) {
                        Icon(
                            imageVector = if (dhikr.isFavorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = stringResource(R.string.toggle_favorite),
                            tint = if (dhikr.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Arabic Text in Amiri (always shown for authentic recitation)
            Text(
                text = dhikr.content,
                fontFamily = AmiriFontFamily,
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp * 1.72f).sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dhikr_content_${dhikr.id}")
            )

            // English Translation (when English mode is active)
            if (english != null) {
                val translationSizeSp = (fontSizeSp * 0.68f).coerceIn(14f, 22f)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dhikr_translation_${dhikr.id}"),
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

            // Virtue box
            if (displayVirtue.isNotBlank()) {
                val virtueSizeSp = (fontSizeSp * 0.62f).coerceIn(13f, 20f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = displayVirtue,
                        fontSize = virtueSizeSp.sp,
                        lineHeight = (virtueSizeSp * 1.55f).sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            // Counter Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeWidth = 4.dp,
                            strokeCap = StrokeCap.Round
                        )
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = stringResource(R.string.dhikr_completed_badge),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = "${dhikr.targetCount - dhikr.currentCount}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Column {
                        Text(
                            text = if (isCompleted) {
                                stringResource(R.string.dhikr_completed_badge)
                            } else {
                                stringResource(R.string.tap_to_count)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${dhikr.currentCount} / ${dhikr.targetCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (dhikr.currentCount > 0) {
                        Surface(
                            onClick = onReset,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("reset_dhikr_${dhikr.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = stringResource(R.string.reset_single),
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.reset_single),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = onShare,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("share_dhikr_${dhikr.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = stringResource(R.string.share_dhikr),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = stringResource(R.string.share_dhikr),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Inline High-Quality Entry Audio Player
            if (onToggleAudio != null) {
                val audioTrack = remember(dhikr.id, dhikr.category, dhikr.title) {
                    DhikrAudioCatalog.trackFor(dhikr)
                }
                DhikrEntryAudioPlayer(
                    dhikrId = dhikr.id,
                    track = audioTrack,
                    isActive = isAudioActive,
                    isPlaying = isAudioPlaying,
                    isBuffering = isAudioBuffering,
                    positionMs = if (isAudioActive) audioPositionMs else 0L,
                    durationMs = if (isAudioActive) audioDurationMs else 0L,
                    playbackSpeed = audioPlaybackSpeed,
                    repeatCurrent = audioRepeatCurrent,
                    hasError = isAudioActive && audioHasError,
                    onTogglePlayPause = onToggleAudio,
                    onSeekTo = onSeekAudio,
                    onCycleSpeed = onCycleAudioSpeed,
                    onToggleRepeat = onToggleAudioRepeat,
                    onRetry = onRetryAudio,
                    onStop = onStopAudio,
                    tagPrefix = audioTagPrefix
                )
            }
        }
    }
}
