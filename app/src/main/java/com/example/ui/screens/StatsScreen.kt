package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FilePresent
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.DhikrSessionsWeeklyCard
import com.example.ui.components.DhikrTrendLineChart
import com.example.ui.components.TasbihDailyVicoCard
import com.example.ui.components.TasbihWeeklyVicoCard
import com.example.ui.viewmodel.AdhkarUiState
import com.example.ui.viewmodel.DailyTrendPoint
import com.example.ui.viewmodel.StatsPeriod
import com.example.ui.viewmodel.StatsSeriesFilter
import com.example.util.StatsCsvExporter

@Composable
fun StatsScreen(
    uiState: AdhkarUiState,
    onPeriodChange: (StatsPeriod) -> Unit,
    onSeriesFilterChange: (StatsSeriesFilter) -> Unit,
    onShowMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val summary = uiState.statsSummary
    val points = summary.points
    val period = uiState.statsPeriod
    val filter = uiState.statsSeriesFilter

    val exportSavedMsg = stringResource(R.string.stats_export_saved_toast)
    val exportErrorMsg = stringResource(R.string.stats_export_error_toast)
    val exportSubject = stringResource(R.string.stats_export_subject)
    val shareChooserTitle = stringResource(R.string.stats_export_share)

    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            val csv = StatsCsvExporter.generateCsv(
                points = points,
                dailyTasbihTarget = uiState.dailyTasbihTarget
            )
            val saved = StatsCsvExporter.writeToUri(context, uri, csv)
            onShowMessage(if (saved) exportSavedMsg else exportErrorMsg)
        }
    }

    var selectedIndex by remember(period, points.size) {
        mutableIntStateOf((points.size - 1).coerceAtLeast(0))
    }

    LaunchedEffect(points.size, period) {
        if (points.isNotEmpty() && selectedIndex !in points.indices) {
            selectedIndex = points.lastIndex
        }
    }

    val selectedPoint = points.getOrNull(selectedIndex) ?: points.lastOrNull()
    val recentActivePoints = remember(points, filter) {
        points.asReversed().filter { it.isToday || it.valueFor(filter) > 0 || it.sessionsCount > 0 }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .testTag("stats_lazy_column"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.stats_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.stats_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Vico Daily Tasbeeh Progress Chart Card (Last 7 Days)
            item {
                TasbihDailyVicoCard(
                    summary = summary,
                    todayTasbihSum = uiState.todayTasbihSum,
                    dailyTasbihTarget = uiState.dailyTasbihTarget
                )
            }

            // Vico Weekly Tasbeeh Progress Chart Card (Last 4 Weeks)
            item {
                TasbihWeeklyVicoCard(summary = summary)
            }

            // Vico 7-Day Completed Dhikr Sessions Visualization
            item {
                DhikrSessionsWeeklyCard(summary = summary)
            }

            // Period Selector (Weekly vs Monthly)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = period == StatsPeriod.WEEKLY,
                        onClick = { onPeriodChange(StatsPeriod.WEEKLY) },
                        label = {
                            Text(
                                text = stringResource(R.string.stats_period_weekly),
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stats_period_weekly")
                    )
                    FilterChip(
                        selected = period == StatsPeriod.MONTHLY,
                        onClick = { onPeriodChange(StatsPeriod.MONTHLY) },
                        label = {
                            Text(
                                text = stringResource(R.string.stats_period_monthly),
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stats_period_monthly")
                    )
                }
            }

            // Line Chart Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Selected Day Tooltip Readout
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = selectedPoint?.fullDateLabel.orEmpty(),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("stats_selected_day_label")
                                    )
                                    if (selectedPoint?.isToday == true) {
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
                                if (selectedPoint != null) {
                                    Text(
                                        text = "${stringResource(R.string.stats_dhikr_label)}: ${selectedPoint.dhikrCount}  •  ${stringResource(R.string.stats_tasbih_label)}: ${selectedPoint.tasbihCount}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${selectedPoint?.valueFor(filter) ?: 0}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                        .testTag("stats_selected_day_value")
                                )
                            }
                        }

                        // Series Filter Chips (All / Dhikr / Tasbih)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val seriesOptions = listOf(
                                StatsSeriesFilter.ALL to R.string.stats_filter_all,
                                StatsSeriesFilter.TASBIH to R.string.stats_filter_tasbih,
                                StatsSeriesFilter.DHIKR to R.string.stats_filter_dhikr
                            )
                            seriesOptions.forEach { (option, labelRes) ->
                                FilterChip(
                                    selected = filter == option,
                                    onClick = { onSeriesFilterChange(option) },
                                    label = {
                                        Text(
                                            text = stringResource(labelRes),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    modifier = Modifier.testTag("stats_filter_${option.name.lowercase()}")
                                )
                            }
                        }

                        // Line Chart
                        DhikrTrendLineChart(
                            points = points,
                            period = period,
                            filter = filter,
                            dailyTarget = uiState.dailyTasbihTarget,
                            selectedIndex = selectedIndex,
                            onSelectIndex = { selectedIndex = it },
                            modifier = Modifier.testTag("stats_line_chart")
                        )

                        // Chart Legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Text(
                                    text = stringResource(R.string.stats_total_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (filter != StatsSeriesFilter.DHIKR) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(14.dp)
                                            .height(2.dp)
                                            .background(MaterialTheme.colorScheme.tertiary)
                                    )
                                    Text(
                                        text = "${stringResource(R.string.stats_goal_line_label)} (${uiState.dailyTasbihTarget})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // KPI Summary Cards Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatKpiCard(
                        icon = Icons.AutoMirrored.Rounded.TrendingUp,
                        label = stringResource(R.string.stats_period_total),
                        value = "${summary.periodTotal}",
                        tag = "stats_period_total",
                        modifier = Modifier.weight(1f)
                    )
                    StatKpiCard(
                        icon = Icons.Rounded.CalendarMonth,
                        label = stringResource(R.string.stats_daily_average),
                        value = "${summary.dailyAverage}",
                        tag = "stats_daily_average",
                        modifier = Modifier.weight(1f)
                    )
                    StatKpiCard(
                        icon = Icons.Rounded.LocalFireDepartment,
                        label = stringResource(R.string.stats_streak_title),
                        value = stringResource(R.string.stats_streak_days, summary.currentStreakDays),
                        tag = "stats_streak_days",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Best Day + All-Time Summary Pill
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.stats_best_day),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (summary.bestDayCount > 0) {
                                        "${summary.bestDayCount} — ${summary.bestDayLabel}"
                                    } else {
                                        "—"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = stringResource(R.string.stats_all_time_completed),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${uiState.totalTasbihAllTime + uiState.preferences.totalCompletedAllTime}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Export CSV Card (Save or Share)
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stats_export_card"),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FilePresent,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.stats_export_title),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = stringResource(R.string.stats_export_sub),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val dateSuffix = points.lastOrNull()?.dateKey ?: "export"
                                    createCsvLauncher.launch("adhkar_stats_${period.name.lowercase()}_$dateSuffix.csv")
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("stats_export_save_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.stats_export_save))
                            }

                            Button(
                                onClick = {
                                    val dateSuffix = points.lastOrNull()?.dateKey ?: "export"
                                    val fileName = "adhkar_stats_${period.name.lowercase()}_$dateSuffix.csv"
                                    val csv = StatsCsvExporter.generateCsv(
                                        points = points,
                                        dailyTasbihTarget = uiState.dailyTasbihTarget
                                    )
                                    val shareIntent = StatsCsvExporter.createShareIntent(
                                        context = context,
                                        fileName = fileName,
                                        csvContent = csv,
                                        subject = exportSubject
                                    )
                                    context.startActivity(Intent.createChooser(shareIntent, shareChooserTitle))
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("stats_export_share_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.stats_export_share))
                            }
                        }
                    }
                }
            }

            // History List Section Title
            item {
                Text(
                    text = stringResource(R.string.stats_history_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (recentActivePoints.all { it.valueFor(filter) == 0 && it.sessionsCount == 0 }) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stats_empty_history"),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RadioButtonChecked,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = stringResource(R.string.stats_empty_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.stats_empty_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(recentActivePoints, key = { it.dateKey }) { dayPoint ->
                    DailyHistoryRowCard(
                        point = dayPoint,
                        filter = filter,
                        dailyTarget = uiState.dailyTasbihTarget
                    )
                }
            }
        }
    }
}

@Composable
private fun StatKpiCard(
    icon: ImageVector,
    label: String,
    value: String,
    tag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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

@Composable
private fun DailyHistoryRowCard(
    point: DailyTrendPoint,
    filter: StatsSeriesFilter,
    dailyTarget: Int
) {
    val value = point.valueFor(filter)
    val progress = if (dailyTarget <= 0) 0f else (value.toFloat() / dailyTarget.toFloat()).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stats_history_item_${point.dateKey}"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = point.fullDateLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (point.isToday) {
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
                    text = "$value",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${stringResource(R.string.stats_dhikr_label)}: ${point.dhikrCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${stringResource(R.string.stats_sessions_label)}: ${point.sessionsCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${stringResource(R.string.stats_tasbih_label)}: ${point.tasbihCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
