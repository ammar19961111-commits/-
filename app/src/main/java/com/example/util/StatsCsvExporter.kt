package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.ui.viewmodel.DailyTrendPoint
import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.math.roundToInt

object StatsCsvExporter {

    private const val UTF8_BOM = "\uFEFF"

    fun generateCsv(
        points: List<DailyTrendPoint>,
        dailyTasbihTarget: Int,
        includeBom: Boolean = true
    ): String {
        val sb = StringBuilder()
        if (includeBom) {
            sb.append(UTF8_BOM)
        }
        sb.appendLine("date,day_label,dhikr_count,tasbih_count,total_count,daily_tasbih_target,goal_progress_percent")
        points.forEach { point ->
            val goalPct = if (dailyTasbihTarget <= 0) {
                0
            } else {
                ((point.tasbihCount * 100f) / dailyTasbihTarget).roundToInt().coerceAtLeast(0)
            }
            sb.append(escapeCsv(point.dateKey)).append(',')
                .append(escapeCsv(point.fullDateLabel)).append(',')
                .append(point.dhikrCount).append(',')
                .append(point.tasbihCount).append(',')
                .append(point.totalCount).append(',')
                .append(dailyTasbihTarget).append(',')
                .append(goalPct)
                .appendLine()
        }
        return sb.toString()
    }

    fun writeToUri(context: Context, uri: Uri, csvContent: String): Boolean {
        return runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(csvContent.toByteArray(StandardCharsets.UTF_8))
                stream.flush()
            } ?: return false
            true
        }.getOrDefault(false)
    }

    fun writeExportCacheFile(context: Context, fileName: String, csvContent: String): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val file = File(exportDir, safeName)
        file.writeText(csvContent, StandardCharsets.UTF_8)
        return file
    }

    fun createShareIntent(
        context: Context,
        fileName: String,
        csvContent: String,
        subject: String
    ): Intent {
        val file = writeExportCacheFile(context, fileName, csvContent)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun escapeCsv(value: String): String {
        val needsQuoting = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
        if (!needsQuoting) return value
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
