package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.viewmodel.DailyTrendPoint
import com.example.ui.viewmodel.DhikrStatsSummary
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun DhikrSessionsWeeklyCard(
    summary: DhikrStatsSummary,
    modifier: Modifier = Modifier
) {
    val weekPoints = summary.last7DaysSessions
    var selectedDayIndex by remember(weekPoints.size) {
        mutableIntStateOf((weekPoints.size - 1).coerceAtLeast(0))
    }

    LaunchedEffect(weekPoints.size) {
        if (weekPoints.isNotEmpty() && selectedDayIndex !in weekPoints.indices) {
            selectedDayIndex = weekPoints.lastIndex
        }
    }

    val selectedPoint = weekPoints.getOrNull(selectedDayIndex) ?: weekPoints.lastOrNull()
    val maxSessions = weekPoints.maxOfOrNull { it.sessionsCount } ?: 0
    val hasAnySessions = maxSessions > 0
    val yAxisMax = max(4, ((maxSessions + 1) / 2) * 2 + 2)

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(weekPoints) {
        val values = if (weekPoints.isEmpty()) List(7) { 0 } else weekPoints.map { it.sessionsCount }
        modelProducer.runTransaction {
            columnSeries {
                series(values)
            }
        }
    }

    val xAxisFormatter = remember(weekPoints) {
        CartesianValueFormatter { _, x, _ ->
            val idx = x.roundToInt()
            weekPoints.getOrNull(idx)?.shortLabel ?: " "
        }
    }
    val yAxisFormatter = remember {
        CartesianValueFormatter { _, y, _ ->
            y.roundToInt().toString()
        }
    }
    val rangeProvider = remember(yAxisMax) {
        CartesianLayerRangeProvider.fixed(minY = 0.0, maxY = yAxisMax.toDouble())
    }

    val chartCd = stringResource(R.string.stats_sessions_chart_cd)
    val emptyTip = stringResource(R.string.stats_sessions_empty_tip)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("stats_sessions_weekly_card"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.stats_sessions_card_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.stats_sessions_card_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = stringResource(
                            R.string.stats_sessions_badge_count,
                            summary.last7DaysSessionsTotal
                        ),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("stats_sessions_7day_total")
                    )
                }
            }

            // Selected Day Readout
            if (selectedPoint != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = selectedPoint.fullDateLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("stats_sessions_selected_day_label")
                                )
                                if (selectedPoint.isToday) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = stringResource(R.string.stats_today_badge),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${stringResource(R.string.stats_dhikr_label)}: ${selectedPoint.dhikrCount}  •  ${stringResource(R.string.stats_tasbih_label)}: ${selectedPoint.tasbihCount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "${selectedPoint.sessionsCount}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                                    .testTag("stats_sessions_selected_day_count")
                            )
                        }
                    }
                }
            }

            // Vico Material 3 Column Chart (Last 7 Days Completed Sessions)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(195.dp)
                    .semantics { contentDescription = chartCd }
                    .testTag("stats_sessions_vico_chart")
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ProvideVicoTheme(rememberM3VicoTheme()) {
                        val primaryColumn = rememberLineComponent(
                            fill = fill(MaterialTheme.colorScheme.primary),
                            thickness = 18.dp,
                            shape = CorneredShape.rounded(topLeftPercent = 35, topRightPercent = 35)
                        )
                        CartesianChartHost(
                            chart = rememberCartesianChart(
                                rememberColumnCartesianLayer(
                                    columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                                        primaryColumn
                                    ),
                                    rangeProvider = rangeProvider
                                ),
                                startAxis = VerticalAxis.rememberStart(
                                    valueFormatter = yAxisFormatter,
                                    itemPlacer = VerticalAxis.ItemPlacer.count({ 5 })
                                ),
                                bottomAxis = HorizontalAxis.rememberBottom(
                                    valueFormatter = xAxisFormatter,
                                    guideline = null
                                )
                            ),
                            modelProducer = modelProducer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(195.dp)
                        )
                    }
                }

                if (!hasAnySessions) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                                text = emptyTip,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Accessible 7-Day Selector Strip (1-tap day inspection with 48dp min touch targets)
            if (weekPoints.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    weekPoints.forEachIndexed { index, dayPoint ->
                        SessionDayChip(
                            point = dayPoint,
                            isSelected = index == selectedDayIndex,
                            onClick = { selectedDayIndex = index },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stats_sessions_day_chip_$index")
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 7-Day Sessions Summary Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SessionMiniMetric(
                    label = stringResource(R.string.stats_sessions_kpi_total),
                    value = "${summary.last7DaysSessionsTotal}",
                    tag = "stats_sessions_kpi_total",
                    modifier = Modifier.weight(1f)
                )
                SessionMiniMetric(
                    label = stringResource(R.string.stats_sessions_kpi_active_days),
                    value = stringResource(
                        R.string.stats_sessions_kpi_active_value,
                        summary.last7DaysActiveSessionDays
                    ),
                    tag = "stats_sessions_kpi_active_days",
                    modifier = Modifier.weight(1f)
                )
                SessionMiniMetric(
                    label = stringResource(R.string.stats_sessions_kpi_best_day),
                    value = if (summary.last7DaysBestSessionDayCount > 0) {
                        "${summary.last7DaysBestSessionDayCount}"
                    } else {
                        "0"
                    },
                    tag = "stats_sessions_kpi_best_day",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SessionDayChip(
    point: DailyTrendPoint,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        point.sessionsCount > 0 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }
    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        point.sessionsCount > 0 -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = if (point.isToday && !isSelected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = point.shortLabel,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${point.sessionsCount}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SessionMiniMetric(
    label: String,
    value: String,
    tag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.testTag(tag)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
