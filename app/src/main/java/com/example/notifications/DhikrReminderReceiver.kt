package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.DhikrCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DhikrReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val categoryId = intent.getStringExtra(DhikrNotificationHelper.EXTRA_CATEGORY_ID) ?: return
        val category = DhikrCategory.fromId(categoryId)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = UserPreferencesRepository(context.applicationContext).preferencesFlow.first()
                val reminder = prefs.reminders.find { it.category == category }
                if (reminder != null && reminder.enabled) {
                    DhikrNotificationHelper.showCategoryReminder(
                        context = context.applicationContext,
                        category = category,
                        appLanguage = prefs.appLanguage
                    )
                    DhikrReminderScheduler.scheduleReminder(
                        context = context.applicationContext,
                        reminder = reminder,
                        nowMillis = System.currentTimeMillis() + 65_000L
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
