package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.viewmodel.DailyTrendPoint
import com.example.ui.viewmodel.StatsPeriod
import com.example.ui.viewmodel.StatsSeriesFilter
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun DhikrTrendLineChart(
    points: List<DailyTrendPoint>,
    period: StatsPeriod,
    filter: StatsSeriesFilter,
    dailyTarget: Int,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val targetColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val emptyMessage = stringResource(R.string.stats_empty_chart_tip)
    val chartCd = stringResource(R.string.stats_chart_cd)

    val values = points.map { it.valueFor(filter) }
    val maxDataValue = values.maxOrNull() ?: 0
    val hasAnyActivity = maxDataValue > 0
    val yMax = computeNiceYMax(
        maxValue = maxDataValue,
        targetHint = if (hasAnyActivity && filter != StatsSeriesFilter.DHIKR) dailyTarget else 0
    )

    val revealProgress by animateFloatAsState(
        targetValue = if (points.isEmpty()) 0f else 1f,
        animationSpec = tween(durationMillis = 450),
        label = "chart_reveal"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = chartCd }
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Y-axis numeric ticks
                Column(
                    modifier = Modifier
                        .width(38.dp)
                        .fillMaxHeight()
                        .padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "$yMax",
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor
                    )
                    Text(
                        text = "${yMax / 2}",
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor
                    )
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Interactive Line Chart Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(points) {
                            if (points.isEmpty()) return@pointerInput
                            detectTapGestures { offset ->
                                val idx = nearestIndexForX(offset.x, size.width.toFloat(), points.size)
                                onSelectIndex(idx)
                            }
                        }
                        .pointerInput(points) {
                            if (points.isEmpty()) return@pointerInput
                            detectHorizontalDragGestures { change, _ ->
                                change.consume()
                                val idx = nearestIndexForX(change.position.x, size.width.toFloat(), points.size)
                                onSelectIndex(idx)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (points.isEmpty()) return@Canvas

                        val topPad = 14.dp.toPx()
                        val bottomPad = 14.dp.toPx()
                        val horizPad = 10.dp.toPx()
                        val plotWidth = (size.width - horizPad * 2f).coerceAtLeast(1f)
                        val plotHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)
                        val baselineY = topPad + plotHeight

                        // Horizontal gridlines (max, mid, 0)
                        val gridDash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        listOf(0f, 0.5f, 1f).forEach { fraction ->
                            val y = topPad + plotHeight * fraction
                            drawLine(
                                color = gridColor,
                                start = Offset(horizPad, y),
                                end = Offset(horizPad + plotWidth, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = if (fraction < 1f) gridDash else null
                            )
                        }

                        // Optional daily goal reference line
                        if (dailyTarget > 0 && dailyTarget <= yMax && filter != StatsSeriesFilter.DHIKR) {
                            val targetY = baselineY - (dailyTarget.toFloat() / yMax.toFloat()) * plotHeight
                            drawLine(
                                color = targetColor.copy(alpha = 0.75f),
                                start = Offset(horizPad, targetY),
                                end = Offset(horizPad + plotWidth, targetY),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                            )
                        }

                        val stepX = if (points.size <= 1) 0f else plotWidth / (points.size - 1)
                        val coords = points.mapIndexed { index, point ->
                            val x = if (points.size <= 1) horizPad + plotWidth / 2f else horizPad + index * stepX
                            val v = point.valueFor(filter).toFloat()
                            val y = baselineY - (v / yMax.toFloat()).coerceIn(0f, 1f) * plotHeight
                            Offset(x, y)
                        }

                        // Smooth monotonic-friendly Bezier path
                        val linePath = Path().apply {
                            moveTo(coords.first().x, coords.first().y)
                            for (i in 0 until coords.lastIndex) {
                                val p0 = coords[i]
                                val p1 = coords[i + 1]
                                val midX = (p0.x + p1.x) / 2f
                                cubicTo(
                                    midX, p0.y,
                                    midX, p1.y,
                                    p1.x, p1.y
                                )
                            }
                        }

                        val fillPath = Path().apply {
                            addPath(linePath)
                            lineTo(coords.last().x, baselineY)
                            lineTo(coords.first().x, baselineY)
                            close()
                        }

                        clipRect(right = horizPad + plotWidth * revealProgress + 16.dp.toPx()) {
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        lineColor.copy(alpha = 0.28f),
                                        lineColor.copy(alpha = 0.02f)
                                    ),
                                    startY = topPad,
                                    endY = baselineY
                                )
                            )

                            drawPath(
                                path = linePath,
                                color = lineColor,
                                style = Stroke(
                                    width = 3.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }

                        // Vertical crosshair & point markers
                        val safeSelected = selectedIndex.coerceIn(0, coords.lastIndex)
                        val selectedCoord = coords[safeSelected]

                        drawLine(
                            color = targetColor.copy(alpha = 0.55f),
                            start = Offset(selectedCoord.x, topPad),
                            end = Offset(selectedCoord.x, baselineY),
                            strokeWidth = 1.25.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )

                        // Draw regular dots for weekly view (or non-zero peaks in monthly view)
                        coords.forEachIndexed { idx, offset ->
                            val isSelected = idx == safeSelected
                            val showDot = period == StatsPeriod.WEEKLY || isSelected || values[idx] > 0
                            if (showDot) {
                                if (isSelected) {
                                    drawCircle(
                                        color = targetColor.copy(alpha = 0.28f),
                                        radius = 10.dp.toPx(),
                                        center = offset
                                    )
                                    drawCircle(
                                        color = surfaceColor,
                                        radius = 5.5.dp.toPx(),
                                        center = offset
                                    )
                                    drawCircle(
                                        color = targetColor,
                                        radius = 4.dp.toPx(),
                                        center = offset
                                    )
                                } else {
                                    val dotRadius = if (period == StatsPeriod.WEEKLY) 4.dp.toPx() else 2.5.dp.toPx()
                                    drawCircle(
                                        color = surfaceColor,
                                        radius = dotRadius + 1.5.dp.toPx(),
                                        center = offset
                                    )
                                    drawCircle(
                                        color = lineColor,
                                        radius = dotRadius,
                                        center = offset
                                    )
                                }
                            }
                        }
                    }

                    if (!hasAnyActivity) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = emptyMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // X-axis chronological labels
            if (points.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 46.dp, end = 6.dp, top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val labelIndices = when (period) {
                        StatsPeriod.WEEKLY -> points.indices.toList()
                        StatsPeriod.MONTHLY -> listOf(0, 7, 14, 21, points.lastIndex).distinct()
                            .filter { it in points.indices }
                    }
                    labelIndices.forEach { idx ->
                        val isSelected = idx == selectedIndex
                        Text(
                            text = points[idx].shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else labelColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

private fun nearestIndexForX(x: Float, width: Float, count: Int): Int {
    if (count <= 1 || width <= 0f) return 0
    val fraction = (x / width).coerceIn(0f, 1f)
    return (fraction * (count - 1)).roundToInt().coerceIn(0, count - 1)
}

private fun computeNiceYMax(maxValue: Int, targetHint: Int): Int {
    val rawMax = max(maxValue, if (maxValue > 0 && targetHint in 1..(maxValue * 2)) targetHint else 0)
    if (rawMax <= 10) return 10
    val magnitude = when {
        rawMax <= 50 -> 10
        rawMax <= 200 -> 25
        rawMax <= 1000 -> 100
        else -> 250
    }
    val rounded = ceil(rawMax.toDouble() / magnitude).toInt() * magnitude
    return max(rounded, rawMax + (magnitude / 5))
}
