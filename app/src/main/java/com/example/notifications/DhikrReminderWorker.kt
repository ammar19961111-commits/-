package com.example.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.DhikrCategory
import kotlinx.coroutines.flow.first

class DhikrReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val categoryId = inputData.getString(DhikrNotificationHelper.EXTRA_CATEGORY_ID)
            ?: return Result.failure()
        val category = DhikrCategory.fromId(categoryId)

        val prefs = UserPreferencesRepository(applicationContext).preferencesFlow.first()
        val reminder = prefs.reminders.find { it.category == category }
        if (reminder?.enabled == true) {
            DhikrNotificationHelper.showCategoryReminder(
                context = applicationContext,
                category = category,
                appLanguage = prefs.appLanguage
            )
        } else {
            DhikrReminderScheduler.cancelReminder(applicationContext, category)
        }
        return Result.success()
    }
}
