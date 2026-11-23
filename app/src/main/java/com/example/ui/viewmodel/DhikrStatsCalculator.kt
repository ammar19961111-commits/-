package com.example.ui.viewmodel

import com.example.data.model.DailyActivity
import com.example.ui.theme.AppLanguage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class StatsPeriod(val days: Int) {
    WEEKLY(7),
    MONTHLY(30)
}

enum class StatsSeriesFilter {
    ALL,
    TASBIH,
    DHIKR
}

data class DailyTrendPoint(
    val dateKey: String,
    val shortLabel: String,
    val fullDateLabel: String,
    val dhikrCount: Int,
    val tasbihCount: Int,
    val isToday: Boolean,
    val sessionsCount: Int = 0
) {
    val totalCount: Int
        get() = dhikrCount + tasbihCount

    fun valueFor(filter: StatsSeriesFilter): Int = when (filter) {
        StatsSeriesFilter.ALL -> totalCount
        StatsSeriesFilter.TASBIH -> tasbihCount
        StatsSeriesFilter.DHIKR -> dhikrCount
    }
}

data class WeeklyTasbihPoint(
    val weekIndex: Int,
    val shortLabel: String,
    val fullRangeLabel: String,
    val tasbihCount: Int,
    val dhikrCount: Int,
    val activeDays: Int,
    val dailyAverage: Int,
    val isCurrentWeek: Boolean
)

data class DhikrStatsSummary(
    val points: List<DailyTrendPoint> = emptyList(),
    val periodTotal: Int = 0,
    val periodDhikrTotal: Int = 0,
    val periodTasbihTotal: Int = 0,
    val periodSessionsTotal: Int = 0,
    val dailyAverage: Int = 0,
    val bestDayCount: Int = 0,
    val bestDayLabel: String = "",
    val activeDaysCount: Int = 0,
    val currentStreakDays: Int = 0,
    val last7DaysSessions: List<DailyTrendPoint> = emptyList(),
    val last7DaysSessionsTotal: Int = 0,
    val last7DaysSessionsAverage: Float = 0f,
    val last7DaysBestSessionDayCount: Int = 0,
    val last7DaysBestSessionDayLabel: String = "",
    val last7DaysActiveSessionDays: Int = 0,
    val last7DaysTasbihTotal: Int = 0,
    val last7DaysTasbihAverage: Int = 0,
    val last7DaysBestTasbihCount: Int = 0,
    val last7DaysBestTasbihLabel: String = "",
    val weeklyTasbihPoints: List<WeeklyTasbihPoint> = emptyList(),
    val currentWeekTasbihTotal: Int = 0,
    val previousWeekTasbihTotal: Int = 0
)

object DhikrStatsCalculator {

    private val arabicLocale = Locale("ar")

    fun buildSummary(
        activities: List<DailyActivity>,
        period: StatsPeriod,
        filter: StatsSeriesFilter,
        nowMillis: Long = System.currentTimeMillis(),
        language: AppLanguage = AppLanguage.ARABIC
    ): DhikrStatsSummary {
        val labelLocale = if (language == AppLanguage.ENGLISH) Locale.ENGLISH else arabicLocale
        val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val weekdayFormat = SimpleDateFormat("EEE", labelLocale)
        val monthDayFormat = SimpleDateFormat("d/M", Locale.US)
        val weekRangeFormat = SimpleDateFormat("d MMM", labelLocale)
        val fullDateFormat = SimpleDateFormat("EEEE d MMMM", labelLocale)

        val activityByDate = activities.associateBy { it.dateKey }
        val baseCal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayKey = keyFormat.format(baseCal.time)

        val points = (period.days - 1 downTo 0).map { daysAgo ->
            val cal = (baseCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }
            val date = cal.time
            val key = keyFormat.format(date)
            val record = activityByDate[key]
            val shortLabel = when (period) {
                StatsPeriod.WEEKLY -> weekdayFormat.format(date)
                StatsPeriod.MONTHLY -> monthDayFormat.format(date)
            }
            DailyTrendPoint(
                dateKey = key,
                shortLabel = shortLabel,
                fullDateLabel = fullDateFormat.format(date),
                dhikrCount = record?.dhikrCount ?: 0,
                tasbihCount = record?.tasbihCount ?: 0,
                isToday = key == todayKey,
                sessionsCount = record?.sessionsCount ?: 0
            )
        }

        val last7DaysSessions = (StatsPeriod.WEEKLY.days - 1 downTo 0).map { daysAgo ->
            val cal = (baseCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }
            val date = cal.time
            val key = keyFormat.format(date)
            val record = activityByDate[key]
            DailyTrendPoint(
                dateKey = key,
                shortLabel = weekdayFormat.format(date),
                fullDateLabel = fullDateFormat.format(date),
                dhikrCount = record?.dhikrCount ?: 0,
                tasbihCount = record?.tasbihCount ?: 0,
                isToday = key == todayKey,
                sessionsCount = record?.sessionsCount ?: 0
            )
        }

        val weeklyTasbihPoints = (3 downTo 0).mapIndexed { index, weekOffset ->
            val endOffset = weekOffset * 7
            val startOffset = endOffset + 6
            val startCal = (baseCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -startOffset)
            }
            val endCal = (baseCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -endOffset)
            }
            var weekTasbih = 0
            var weekDhikr = 0
            var weekActiveDays = 0
            for (dayOffset in startOffset downTo endOffset) {
                val dayCal = (baseCal.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, -dayOffset)
                }
                val record = activityByDate[keyFormat.format(dayCal.time)]
                val dayTasbih = record?.tasbihCount ?: 0
                val dayDhikr = record?.dhikrCount ?: 0
                weekTasbih += dayTasbih
                weekDhikr += dayDhikr
                if (dayTasbih > 0) weekActiveDays++
            }
            WeeklyTasbihPoint(
                weekIndex = index,
                shortLabel = monthDayFormat.format(startCal.time),
                fullRangeLabel = "${weekRangeFormat.format(startCal.time)} – ${weekRangeFormat.format(endCal.time)}",
                tasbihCount = weekTasbih,
                dhikrCount = weekDhikr,
                activeDays = weekActiveDays,
                dailyAverage = weekTasbih / 7,
                isCurrentWeek = weekOffset == 0
            )
        }

        val periodDhikr = points.sumOf { it.dhikrCount }
        val periodTasbih = points.sumOf { it.tasbihCount }
        val periodSessions = points.sumOf { it.sessionsCount }
        val periodTotal = points.sumOf { it.valueFor(filter) }
        val dailyAverage = if (points.isEmpty()) 0 else periodTotal / points.size
        val bestPoint = points.maxByOrNull { it.valueFor(filter) }
        val bestCount = bestPoint?.valueFor(filter) ?: 0
        val activeDays = points.count { it.valueFor(filter) > 0 }
        val streak = computeCurrentStreak(activityByDate, baseCal, keyFormat)

        val sessions7Total = last7DaysSessions.sumOf { it.sessionsCount }
        val sessions7Average = if (last7DaysSessions.isEmpty()) 0f else sessions7Total.toFloat() / last7DaysSessions.size
        val bestSession7Point = last7DaysSessions.maxByOrNull { it.sessionsCount }
        val bestSession7Count = bestSession7Point?.sessionsCount ?: 0
        val activeSession7Days = last7DaysSessions.count { it.sessionsCount > 0 }

        val tasbih7Total = last7DaysSessions.sumOf { it.tasbihCount }
        val tasbih7Average = if (last7DaysSessions.isEmpty()) 0 else tasbih7Total / last7DaysSessions.size
        val bestTasbih7Point = last7DaysSessions.maxByOrNull { it.tasbihCount }
        val bestTasbih7Count = bestTasbih7Point?.tasbihCount ?: 0
        val currentWeekTotal = weeklyTasbihPoints.lastOrNull()?.tasbihCount ?: 0
        val previousWeekTotal = weeklyTasbihPoints.getOrNull(weeklyTasbihPoints.lastIndex - 1)?.tasbihCount ?: 0

        return DhikrStatsSummary(
            points = points,
            periodTotal = periodTotal,
            periodDhikrTotal = periodDhikr,
            periodTasbihTotal = periodTasbih,
            periodSessionsTotal = periodSessions,
            dailyAverage = dailyAverage,
            bestDayCount = bestCount,
            bestDayLabel = if (bestCount > 0) bestPoint?.fullDateLabel.orEmpty() else "",
            activeDaysCount = activeDays,
            currentStreakDays = streak,
            last7DaysSessions = last7DaysSessions,
            last7DaysSessionsTotal = sessions7Total,
            last7DaysSessionsAverage = sessions7Average,
            last7DaysBestSessionDayCount = bestSession7Count,
            last7DaysBestSessionDayLabel = if (bestSession7Count > 0) bestSession7Point?.fullDateLabel.orEmpty() else "",
            last7DaysActiveSessionDays = activeSession7Days,
            last7DaysTasbihTotal = tasbih7Total,
            last7DaysTasbihAverage = tasbih7Average,
            last7DaysBestTasbihCount = bestTasbih7Count,
            last7DaysBestTasbihLabel = if (bestTasbih7Count > 0) bestTasbih7Point?.fullDateLabel.orEmpty() else "",
            weeklyTasbihPoints = weeklyTasbihPoints,
            currentWeekTasbihTotal = currentWeekTotal,
            previousWeekTasbihTotal = previousWeekTotal
        )
    }

    private fun computeCurrentStreak(
        activityByDate: Map<String, DailyActivity>,
        baseCal: Calendar,
        keyFormat: SimpleDateFormat
    ): Int {
        val todayKey = keyFormat.format(baseCal.time)
        val todayRecord = activityByDate[todayKey]
        val hasTodayActivity = ((todayRecord?.totalCount ?: 0) > 0) || ((todayRecord?.sessionsCount ?: 0) > 0)
        var offset = if (hasTodayActivity) 0 else 1
        var streak = 0

        while (true) {
            val checkCal = (baseCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -offset)
            }
            val key = keyFormat.format(checkCal.time)
            val record = activityByDate[key]
            val hasActivity = ((record?.totalCount ?: 0) > 0) || ((record?.sessionsCount ?: 0) > 0)
            if (hasActivity) {
                streak++
                offset++
            } else {
                break
            }
        }
        return streak
    }
}
