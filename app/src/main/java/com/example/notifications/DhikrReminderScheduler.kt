package com.example.notifications

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.data.model.DhikrCategory
import com.example.data.model.DhikrReminder
import java.util.Calendar
import java.util.concurrent.TimeUnit

interface ReminderScheduler {
    fun syncAll(reminders: List<DhikrReminder>)
}

class AndroidReminderScheduler(private val context: Context) : ReminderScheduler {
    override fun syncAll(reminders: List<DhikrReminder>) {
        DhikrReminderScheduler.syncAllReminders(context, reminders)
    }
}

object NoOpReminderScheduler : ReminderScheduler {
    override fun syncAll(reminders: List<DhikrReminder>) = Unit
}

object DhikrReminderScheduler {

    const val WORK_TAG_ALL_REMINDERS = "adhkar_daily_reminders"
    const val REPEAT_INTERVAL_HOURS = 24L

    fun computeNextTriggerMillis(
        hour: Int,
        minute: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis <= nowMillis) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    fun computeInitialDelayMillis(
        hour: Int,
        minute: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        return (computeNextTriggerMillis(hour, minute, nowMillis) - nowMillis).coerceAtLeast(0L)
    }

    fun uniqueWorkNameFor(category: DhikrCategory): String = "dhikr_daily_reminder_${category.id}"

    fun scheduleReminder(
        context: Context,
        reminder: DhikrReminder,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        if (!reminder.enabled) {
            cancelReminder(context, reminder.category)
            return
        }

        val initialDelayMs = computeInitialDelayMillis(reminder.hour, reminder.minute, nowMillis)
        val inputData = workDataOf(DhikrNotificationHelper.EXTRA_CATEGORY_ID to reminder.category.id)

        val request = PeriodicWorkRequestBuilder<DhikrReminderWorker>(
            REPEAT_INTERVAL_HOURS,
            TimeUnit.HOURS
        )
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(WORK_TAG_ALL_REMINDERS)
            .addTag(uniqueWorkNameFor(reminder.category))
            .build()

        runCatching {
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                uniqueWorkNameFor(reminder.category),
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }

    fun scheduleMorningAndEveningPeriodicReminders(
        context: Context,
        morningHour: Int = 6,
        morningMinute: Int = 0,
        eveningHour: Int = 17,
        eveningMinute: Int = 0,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        scheduleReminder(
            context = context,
            reminder = DhikrReminder(
                category = DhikrCategory.MORNING,
                enabled = true,
                hour = morningHour,
                minute = morningMinute
            ),
            nowMillis = nowMillis
        )
        scheduleReminder(
            context = context,
            reminder = DhikrReminder(
                category = DhikrCategory.EVENING,
                enabled = true,
                hour = eveningHour,
                minute = eveningMinute
            ),
            nowMillis = nowMillis
        )
        updateBootReceiverState(context, hasEnabled = true)
    }

    fun cancelReminder(context: Context, category: DhikrCategory) {
        runCatching {
            WorkManager.getInstance(context.applicationContext)
                .cancelUniqueWork(uniqueWorkNameFor(category))
        }
    }

    fun syncAllReminders(
        context: Context,
        reminders: List<DhikrReminder>,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        reminders.forEach { reminder ->
            if (reminder.enabled) {
                scheduleReminder(context, reminder, nowMillis)
            } else {
                cancelReminder(context, reminder.category)
            }
        }
        updateBootReceiverState(context, hasEnabled = reminders.any { it.enabled })
    }

    @Suppress("unused")
    fun buildLaunchIntent(context: Context, category: DhikrCategory): Intent {
        return Intent(context, DhikrReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_DHIKR_REMINDER
            putExtra(DhikrNotificationHelper.EXTRA_CATEGORY_ID, category.id)
        }
    }

    private fun updateBootReceiverState(context: Context, hasEnabled: Boolean) {
        runCatching {
            val receiver = ComponentName(context, BootCompletedReceiver::class.java)
            val newState = if (hasEnabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            context.packageManager.setComponentEnabledSetting(
                receiver,
                newState,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    const val ACTION_TRIGGER_DHIKR_REMINDER = "com.example.action.TRIGGER_DHIKR_REMINDER"
}
