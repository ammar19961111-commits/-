package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.AdhkarDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.AdhkarRepository
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.withAppLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class TasbihWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        widgetScope.launch {
            try {
                refreshWidgetsFromStorage(context, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_INCREMENT_TASBIH) {
            val pendingResult = goAsync()
            widgetScope.launch {
                try {
                    val database = AdhkarDatabase.getInstance(context.applicationContext)
                    val prefsRepo = UserPreferencesRepository(context.applicationContext)
                    val repository = AdhkarRepository(
                        dhikrDao = database.dhikrDao(),
                        counterDao = database.counterDao(),
                        prefsRepo = prefsRepo,
                        dailyActivityDao = database.dailyActivityDao()
                    )
                    repository.ensureInitialized()
                    val counters = repository.counters.first()
                    val primary = counters.firstOrNull()
                    if (primary != null) {
                        repository.incrementCounter(primary)
                    }
                    val manager = AppWidgetManager.getInstance(context)
                    val ids = manager.getAppWidgetIds(
                        ComponentName(context, TasbihWidgetProvider::class.java)
                    )
                    refreshWidgetsFromStorage(context, manager, ids)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_INCREMENT_TASBIH = "com.example.action.WIDGET_INCREMENT_TASBIH"
        const val EXTRA_OPEN_TAB = "extra_open_tab"
        const val TAB_TASBIH = "tasbih"

        private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        suspend fun refreshWidgetsFromStorage(
            context: Context,
            appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context),
            appWidgetIds: IntArray = appWidgetManager.getAppWidgetIds(
                ComponentName(context, TasbihWidgetProvider::class.java)
            )
        ) {
            if (appWidgetIds.isEmpty()) return
            val database = AdhkarDatabase.getInstance(context.applicationContext)
            val prefsRepo = UserPreferencesRepository(context.applicationContext)
            val counters = database.counterDao().observeAll().first()
            val prefs = prefsRepo.preferencesFlow.first()
            val todaySum = counters.sumOf { it.todayCount }
            val phrase = counters.firstOrNull()?.phrase ?: "سُبْحَانَ اللَّهِ"
            updateAllWidgets(
                context = context,
                phrase = phrase,
                todayCount = todaySum,
                dailyTarget = prefs.dailyTasbihTarget,
                appLanguage = prefs.appLanguage,
                appWidgetManager = appWidgetManager,
                appWidgetIds = appWidgetIds
            )
        }

        fun updateAllWidgets(
            context: Context,
            phrase: String,
            todayCount: Int,
            dailyTarget: Int,
            appLanguage: AppLanguage = AppLanguage.ARABIC,
            appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context),
            appWidgetIds: IntArray = appWidgetManager.getAppWidgetIds(
                ComponentName(context, TasbihWidgetProvider::class.java)
            )
        ) {
            if (appWidgetIds.isEmpty()) return
            val views = buildRemoteViews(
                context = context,
                phrase = phrase,
                todayCount = todayCount,
                dailyTarget = dailyTarget,
                appLanguage = appLanguage
            )
            appWidgetIds.forEach { widgetId ->
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }

        fun buildRemoteViews(
            context: Context,
            phrase: String,
            todayCount: Int,
            dailyTarget: Int,
            appLanguage: AppLanguage = AppLanguage.ARABIC
        ): RemoteViews {
            val localized = context.withAppLanguage(appLanguage)
            val safeTarget = dailyTarget.coerceAtLeast(1)
            val progressPercent = ((todayCount.toFloat() / safeTarget.toFloat()) * 100f)
                .roundToInt()
                .coerceIn(0, 100)
            val remaining = (safeTarget - todayCount).coerceAtLeast(0)
            val statusText = if (todayCount >= safeTarget) {
                localized.getString(R.string.widget_tasbih_goal_done)
            } else {
                localized.getString(R.string.widget_tasbih_remaining, remaining)
            }

            val openAppIntent = Intent(localized, MainActivity::class.java).apply {
                putExtra(EXTRA_OPEN_TAB, TAB_TASBIH)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                localized,
                9101,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val incrementIntent = Intent(localized, TasbihWidgetProvider::class.java).apply {
                action = ACTION_INCREMENT_TASBIH
            }
            val incrementPendingIntent = PendingIntent.getBroadcast(
                localized,
                9102,
                incrementIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            return RemoteViews(localized.packageName, R.layout.widget_tasbih_progress).apply {
                setTextViewText(R.id.widget_title, localized.getString(R.string.widget_tasbih_name))
                setTextViewText(R.id.widget_percent, "$progressPercent%")
                setTextViewText(R.id.widget_phrase, phrase)
                setTextViewText(R.id.widget_count_text, "$todayCount / $safeTarget")
                setProgressBar(R.id.widget_progress_bar, 100, progressPercent, false)
                setTextViewText(R.id.widget_status_text, statusText)
                setTextViewText(
                    R.id.widget_increment_button,
                    localized.getString(R.string.widget_tasbih_increment)
                )
                setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)
                setOnClickPendingIntent(R.id.widget_increment_button, incrementPendingIntent)
            }
        }
    }
}
