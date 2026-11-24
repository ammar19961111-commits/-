package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.DhikrCategory
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.withAppLanguage

object DhikrNotificationHelper {
    const val CHANNEL_ID = "adhkar_daily_reminders"
    const val EXTRA_CATEGORY_ID = "extra_reminder_category_id"

    fun ensureChannel(context: Context, appLanguage: AppLanguage = AppLanguage.ARABIC) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appCtx = context.applicationContext ?: context
            val localized = appCtx.withAppLanguage(appLanguage)
            val manager = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                localized.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = localized.getString(R.string.reminder_channel_description)
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        val appCtx = context.applicationContext ?: context
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appCtx,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showCategoryReminder(
        context: Context,
        category: DhikrCategory,
        appLanguage: AppLanguage = AppLanguage.ARABIC
    ) {
        val appCtx = context.applicationContext ?: context
        if (!hasNotificationPermission(appCtx)) return
        val localized = appCtx.withAppLanguage(appLanguage)
        ensureChannel(appCtx, appLanguage)

        val tapIntent = Intent(appCtx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_CATEGORY_ID, category.id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            appCtx,
            notificationIdFor(category),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val categoryTitle = localized.getString(category.titleRes)
        val title = localized.getString(R.string.reminder_notification_title, categoryTitle)
        val body = messageBodyFor(localized, category)

        val notification = NotificationCompat.Builder(appCtx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_dhikr)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_notification_dhikr,
                localized.getString(R.string.reminder_action_read_now),
                contentPendingIntent
            )
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                appCtx,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        try {
            NotificationManagerCompat.from(appCtx).notify(notificationIdFor(category), notification)
        } catch (_: SecurityException) {
            // Permission revoked before posting notification
        }
    }

    fun messageBodyFor(context: Context, category: DhikrCategory): String {
        val resId = when (category) {
            DhikrCategory.MORNING -> R.string.reminder_body_morning
            DhikrCategory.EVENING -> R.string.reminder_body_evening
            DhikrCategory.AFTER_PRAYER -> R.string.reminder_body_after_prayer
            DhikrCategory.SLEEP -> R.string.reminder_body_sleep
            DhikrCategory.WAKING -> R.string.reminder_body_waking
            DhikrCategory.DUAS -> R.string.reminder_body_duas
        }
        return context.getString(resId)
    }

    fun notificationIdFor(category: DhikrCategory): Int = 4000 + category.ordinal
}
