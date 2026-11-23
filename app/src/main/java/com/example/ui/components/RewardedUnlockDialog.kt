package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.RewardedUnlockTarget
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RewardedUnlockDialog(
    target: RewardedUnlockTarget,
    isRewardedAdLoading: Boolean,
    onConfirmWatchAd: () -> Unit,
    onDismiss: () -> Unit
) {
    val itemTitle = when (target) {
        is RewardedUnlockTarget.ThemeStyle -> stringResource(target.style.titleRes)
        is RewardedUnlockTarget.BeadDesign -> stringResource(target.design.titleRes)
    }
    val itemSubtitle = when (target) {
        is RewardedUnlockTarget.ThemeStyle -> stringResource(target.style.subtitleRes)
        is RewardedUnlockTarget.BeadDesign -> stringResource(target.design.subtitleRes)
    }
    val bodyText = when (target) {
        is RewardedUnlockTarget.ThemeStyle -> stringResource(R.string.reward_unlock_dialog_theme_body, itemTitle)
        is RewardedUnlockTarget.BeadDesign -> stringResource(R.string.reward_unlock_dialog_bead_body, itemTitle)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("rewarded_unlock_dialog"),
        icon = {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.reward_unlock_dialog_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (target) {
                            is RewardedUnlockTarget.ThemeStyle -> {
                                Canvas(modifier = Modifier.size(40.dp)) {
                                    drawCircle(color = target.style.previewSurface)
                                    drawCircle(
                                        color = target.style.previewPrimary,
                                        radius = size.minDimension * 0.36f,
                                        center = Offset(size.width * 0.4f, size.height * 0.45f)
                                    )
                                    drawCircle(
                                        color = target.style.previewSecondary,
                                        radius = size.minDimension * 0.2f,
                                        center = Offset(size.width * 0.68f, size.height * 0.68f)
                                    )
                                }
                            }
                            is RewardedUnlockTarget.BeadDesign -> {
                                Canvas(modifier = Modifier.size(40.dp)) {
                                    val r = size.minDimension * 0.36f
                                    val c = Offset(size.width / 2f, size.height / 2f)
                                    for (i in 0 until 8) {
                                        val angle = Math.toRadians(i * 45.0)
                                        val pos = Offset(
                                            x = c.x + (r * cos(angle)).toFloat(),
                                            y = c.y + (r * sin(angle)).toFloat()
                                        )
                                        val isDivider = i % 4 == 0
                                        drawCircle(
                                            color = if (isDivider) target.design.dividerBeadColor else target.design.primaryBeadColor,
                                            radius = if (isDivider) 4.2.dp.toPx() else 3.2.dp.toPx(),
                                            center = pos
                                        )
                                    }
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = itemTitle,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = itemSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    text = bodyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmWatchAd,
                enabled = !isRewardedAdLoading,
                shape = CircleShape,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("rewarded_unlock_confirm_button")
            ) {
                if (isRewardedAdLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.OndemandVideo,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = if (isRewardedAdLoading) {
                        stringResource(R.string.ads_watch_rewarded_loading)
                    } else {
                        stringResource(R.string.reward_unlock_confirm_cta)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("rewarded_unlock_cancel_button")
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
