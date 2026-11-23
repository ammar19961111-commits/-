package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Dhikr
import com.example.data.model.DhikrCategory
import com.example.ui.components.DhikrCard
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.viewmodel.AdhkarUiState
import com.example.util.ShareHelper

@Composable
fun FavoritesScreen(
    uiState: AdhkarUiState,
    onIncrementDhikr: (Dhikr) -> Unit,
    onToggleFavorite: (Dhikr) -> Unit,
    onResetDhikr: (Int) -> Unit,
    onShowCopiedToast: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleDhikrAudio: (Dhikr, List<Dhikr>) -> Unit = { _, _ -> },
    onSeekAudio: (Long) -> Unit = {},
    onCycleAudioSpeed: () -> Unit = {},
    onToggleAudioRepeat: () -> Unit = {},
    onRetryAudio: () -> Unit = {},
    onStopAudio: () -> Unit = {}
) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 800.dp)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.favorites_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.testTag("favorites_screen_title")
                )
                if (uiState.favoriteAdhkar.isNotEmpty()) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${uiState.favoriteAdhkar.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .testTag("favorites_count_badge")
                        )
                    }
                }
            }

            if (uiState.favoriteAdhkar.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("favorites_empty_state"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.FavoriteBorder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.favorites_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(R.string.favorites_empty_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(uiState.favoriteAdhkar, key = { it.id }) { dhikr ->
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
}
