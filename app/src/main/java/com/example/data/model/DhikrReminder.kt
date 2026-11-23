package com.example.data.model

import java.util.Locale

data class DhikrReminder(
    val category: DhikrCategory,
    val enabled: Boolean = false,
    val hour: Int,
    val minute: Int
) {
    val formattedTime24: String
        get() = String.format(Locale.US, "%02d:%02d", hour, minute)

    val formattedTime12Ar: String
        get() {
            val period = if (hour < 12) "ص" else "م"
            val hour12 = when (val h = hour % 12) {
                0 -> 12
                else -> h
            }
            return String.format(Locale.US, "%02d:%02d %s", hour12, minute, period)
        }

    val formattedTime12En: String
        get() {
            val period = if (hour < 12) "AM" else "PM"
            val hour12 = when (val h = hour % 12) {
                0 -> 12
                else -> h
            }
            return String.format(Locale.US, "%02d:%02d %s", hour12, minute, period)
        }
}

object DhikrReminderDefaults {
    fun defaultTimeFor(category: DhikrCategory): Pair<Int, Int> = when (category) {
        DhikrCategory.MORNING -> 6 to 0
        DhikrCategory.EVENING -> 17 to 0
        DhikrCategory.AFTER_PRAYER -> 13 to 0
        DhikrCategory.SLEEP -> 22 to 0
        DhikrCategory.WAKING -> 5 to 30
        DhikrCategory.DUAS -> 20 to 0
    }

    fun defaultReminders(): List<DhikrReminder> =
        DhikrCategory.entries.map { category ->
            val (hour, minute) = defaultTimeFor(category)
            DhikrReminder(
                category = category,
                enabled = false,
                hour = hour,
                minute = minute
            )
        }
}
