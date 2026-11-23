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
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.StrokeCap
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
import com.example.ui.viewmodel.WeeklyTasbihPoint
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun TasbihDailyVicoCard(
    summary: DhikrStatsSummary,
    todayTasbihSum: Int,
    dailyTasbihTarget: Int,
    modifier: Modifier = Modifier
) {
    val weekDays = summary.last7DaysSessions
    var selectedDayIndex by remember(weekDays.size) {
        mutableIntStateOf((weekDays.size - 1).coerceAtLeast(0))
    }

    LaunchedEffect(weekDays.size) {
        if (weekDays.isNotEmpty() && selectedDayIndex !in weekDays.indices) {
            selectedDayIndex = weekDays.lastIndex
        }
    }

    val selectedPoint = weekDays.getOrNull(selectedDayIndex) ?: weekDays.lastOrNull()
    val maxDailyTasbih = weekDays.maxOfOrNull { it.tasbihCount } ?: 0
    val hasAnyTasbih = maxDailyTasbih > 0
    val yAxisMax = computeNiceAxisMax(maxValue = maxDailyTasbih, minFloor = 33)

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(weekDays) {
        val values = if (weekDays.isEmpty()) List(7) { 0 } else weekDays.map { it.tasbihCount }
        modelProducer.runTransaction {
            columnSeries {
                series(values)
            }
        }
    }

    val xAxisFormatter = remember(weekDays) {
        CartesianValueFormatter { _, x, _ ->
            val idx = x.roundToInt()
            weekDays.getOrNull(idx)?.shortLabel ?: " "
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

    val goalFraction = if (dailyTasbihTarget <= 0) 0f
    else (todayTasbihSum.toFloat() / dailyTasbihTarget.toFloat()).coerceIn(0f, 1f)
    val goalPercent = (goalFraction * 100f).roundToInt()
    val chartCd = stringResource(R.string.stats_tasbih_daily_chart_cd)
    val emptyTip = stringResource(R.string.stats_tasbih_daily_empty_tip)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("stats_tasbih_daily_vico_card"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                            imageVector = Icons.Rounded.RadioButtonChecked,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.stats_tasbih_daily_card_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.stats_tasbih_daily_card_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = stringResource(
                            R.string.stats_tasbih_daily_goal_badge,
                            todayTasbihSum,
                            dailyTasbihTarget
                        ),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("stats_tasbih_daily_goal_badge")
                    )
                }
            }

            // Today's Daily Goal Progress
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(
                        R.string.stats_tasbih_daily_goal_progress,
                        goalPercent,
                        dailyTasbihTarget
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { goalFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(CircleShape),
                    color = if (goalFraction >= 1f) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
            }

            // Selected Day Banner
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
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = selectedPoint.fullDateLabel,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("stats_tasbih_daily_selected_label")
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

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "${selectedPoint.tasbihCount}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                                    .testTag("stats_tasbih_daily_selected_count")
                            )
                        }
                    }
                }
            }

            // Vico Material 3 Column Chart (Daily Tasbeeh Progress - Last 7 Days)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(195.dp)
                    .semantics { contentDescription = chartCd }
                    .testTag("stats_tasbih_daily_vico_chart")
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

                if (!hasAnyTasbih) {
                    EmptyChartHintPill(
                        message = emptyTip,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // Interactive 7-day selector strip
            if (weekDays.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    weekDays.forEachIndexed { index, dayPoint ->
                        DailyTasbihDayChip(
                            point = dayPoint,
                            isSelected = index == selectedDayIndex,
                            onClick = { selectedDayIndex = index },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stats_tasbih_daily_chip_$index")
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TasbihChartMiniMetric(
                    label = stringResource(R.string.stats_tasbih_daily_kpi_today),
                    value = "$todayTasbihSum",
                    tag = "stats_tasbih_daily_kpi_today",
                    modifier = Modifier.weight(1f)
                )
                TasbihChartMiniMetric(
                    label = stringResource(R.string.stats_tasbih_daily_kpi_avg),
                    value = "${summary.last7DaysTasbihAverage}",
                    tag = "stats_tasbih_daily_kpi_avg",
                    modifier = Modifier.weight(1f)
                )
                TasbihChartMiniMetric(
                    label = stringResource(R.string.stats_tasbih_daily_kpi_best),
                    value = "${summary.last7DaysBestTasbihCount}",
                    tag = "stats_tasbih_daily_kpi_best",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun TasbihWeeklyVicoCard(
    summary: DhikrStatsSummary,
    modifier: Modifier = Modifier
) {
    val weeks = summary.weeklyTasbihPoints
    var selectedWeekIndex by remember(weeks.size) {
        mutableIntStateOf((weeks.size - 1).coerceAtLeast(0))
    }

    LaunchedEffect(weeks.size) {
        if (weeks.isNotEmpty() && selectedWeekIndex !in weeks.indices) {
            selectedWeekIndex = weeks.lastIndex
        }
    }

    val selectedWeek = weeks.getOrNull(selectedWeekIndex) ?: weeks.lastOrNull()
    val maxWeeklyTasbih = weeks.maxOfOrNull { it.tasbihCount } ?: 0
    val hasAnyWeeklyTasbih = maxWeeklyTasbih > 0
    val yAxisMax = computeNiceAxisMax(maxValue = maxWeeklyTasbih, minFloor = 100)

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(weeks) {
        val values = if (weeks.isEmpty()) List(4) { 0 } else weeks.map { it.tasbihCount }
        modelProducer.runTransaction {
            lineSeries {
                series(values)
            }
        }
    }

    val xAxisFormatter = remember(weeks) {
        CartesianValueFormatter { _, x, _ ->
            val idx = x.roundToInt()
            weeks.getOrNull(idx)?.shortLabel ?: " "
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

    val chartCd = stringResource(R.string.stats_tasbih_weekly_chart_cd)
    val emptyTip = stringResource(R.string.stats_tasbih_weekly_empty_tip)
    val currentWeekTotal = summary.currentWeekTasbihTotal
    val prevWeekTotal = summary.previousWeekTasbihTotal
    val deltaText = when {
        prevWeekTotal <= 0 && currentWeekTotal <= 0 -> null
        prevWeekTotal <= 0 && currentWeekTotal > 0 -> stringResource(R.string.stats_tasbih_weekly_delta_up, 100)
        else -> {
            val pct = (((currentWeekTotal - prevWeekTotal).toFloat() / prevWeekTotal.toFloat()) * 100f).roundToInt()
            when {
                pct > 0 -> stringResource(R.string.stats_tasbih_weekly_delta_up, pct)
                pct < 0 -> stringResource(R.string.stats_tasbih_weekly_delta_down, pct)
                else -> stringResource(R.string.stats_tasbih_weekly_delta_flat)
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("stats_tasbih_weekly_vico_card"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ShowChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.stats_tasbih_weekly_card_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.stats_tasbih_weekly_card_sub),
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
                            R.string.stats_tasbih_weekly_badge_format,
                            currentWeekTotal
                        ),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("stats_tasbih_weekly_current_total")
                    )
                }
            }

            if (deltaText != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoGraph,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = deltaText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.testTag("stats_tasbih_weekly_delta_badge")
                    )
                }
            }

            if (selectedWeek != null) {
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
                                    text = selectedWeek.fullRangeLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("stats_tasbih_weekly_selected_label")
                                )
                                if (selectedWeek.isCurrentWeek) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.tertiaryContainer
                                    ) {
                                        Text(
                                            text = stringResource(R.string.stats_tasbih_weekly_current_label),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = stringResource(
                                    R.string.stats_tasbih_weekly_active_avg,
                                    selectedWeek.activeDays,
                                    selectedWeek.dailyAverage
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("stats_tasbih_weekly_selected_avg")
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.tertiary
                        ) {
                            Text(
                                text = "${selectedWeek.tasbihCount}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = MaterialTheme.colorScheme.onTertiary,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                                    .testTag("stats_tasbih_weekly_selected_count")
                            )
                        }
                    }
                }
            }

            // Vico Material 3 Line + Area Chart (Weekly Tasbeeh Progress - Last 4 Weeks)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(195.dp)
                    .semantics { contentDescription = chartCd }
                    .testTag("stats_tasbih_weekly_vico_chart")
            ) {
                val lineColor = MaterialTheme.colorScheme.tertiary
                val surfaceColor = MaterialTheme.colorScheme.surface
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ProvideVicoTheme(rememberM3VicoTheme()) {
                        val pointDot = rememberShapeComponent(
                            fill = fill(lineColor),
                            shape = CorneredShape.Pill,
                            strokeFill = fill(surfaceColor),
                            strokeThickness = 2.dp
                        )
                        CartesianChartHost(
                            chart = rememberCartesianChart(
                                rememberLineCartesianLayer(
                                    lineProvider = LineCartesianLayer.LineProvider.series(
                                        LineCartesianLayer.rememberLine(
                                            fill = LineCartesianLayer.LineFill.single(fill(lineColor)),
                                            areaFill = LineCartesianLayer.AreaFill.single(
                                                fill(lineColor.copy(alpha = 0.22f))
                                            ),
                                            pointProvider = LineCartesianLayer.PointProvider.single(
                                                LineCartesianLayer.Point(
                                                    component = pointDot,
                                                    sizeDp = 10f
                                                )
                                            )
                                        )
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

                if (!hasAnyWeeklyTasbih) {
                    EmptyChartHintPill(
                        message = emptyTip,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // Interactive 4-week selector strip
            if (weeks.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    weeks.forEachIndexed { index, weekPoint ->
                        WeeklyTasbihChip(
                            point = weekPoint,
                            isSelected = index == selectedWeekIndex,
                            onClick = { selectedWeekIndex = index },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stats_tasbih_weekly_chip_$index")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyTasbihDayChip(
    point: DailyTrendPoint,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        point.tasbihCount > 0 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }
    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        point.tasbihCount > 0 -> MaterialTheme.colorScheme.onPrimaryContainer
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
                text = "${point.tasbihCount}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun WeeklyTasbihChip(
    point: WeeklyTasbihPoint,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.tertiary
        point.tasbihCount > 0 -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }
    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onTertiary
        point.tasbihCount > 0 -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = if (point.isCurrentWeek && !isSelected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = if (point.isCurrentWeek) {
                    stringResource(R.string.stats_tasbih_weekly_current_label)
                } else {
                    stringResource(R.string.stats_tasbih_weekly_week_short, point.weekIndex + 1)
                },
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${point.tasbihCount}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TasbihChartMiniMetric(
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
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
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

@Composable
private fun EmptyChartHintPill(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(horizontal = 16.dp),
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
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun computeNiceAxisMax(maxValue: Int, minFloor: Int): Int {
    val rawMax = max(maxValue, minFloor)
    val step = when {
        rawMax <= 50 -> 10
        rawMax <= 200 -> 25
        rawMax <= 1000 -> 100
        else -> 250
    }
    return ceil(rawMax.toDouble() / step).toInt() * step
}
