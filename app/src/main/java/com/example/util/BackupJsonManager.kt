package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.AdhkarBackupData
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.nio.charset.StandardCharsets

sealed interface BackupParseResult {
    data class Success(val data: AdhkarBackupData) : BackupParseResult
    data object InvalidFormat : BackupParseResult
    data class UnsupportedVersion(val version: Int) : BackupParseResult
}

object BackupJsonManager {

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val adapter by lazy {
        moshi.adapter(AdhkarBackupData::class.java).indent("  ")
    }

    fun serialize(backup: AdhkarBackupData): String {
        return adapter.toJson(backup)
    }

    fun parse(json: String): BackupParseResult {
        val trimmed = json.trim()
        if (trimmed.isEmpty() || !trimmed.startsWith("{")) {
            return BackupParseResult.InvalidFormat
        }
        val parsed = runCatching { adapter.fromJson(trimmed) }.getOrNull()
            ?: return BackupParseResult.InvalidFormat

        if (parsed.schemaVersion <= 0 || parsed.schemaVersion > AdhkarBackupData.CURRENT_SCHEMA_VERSION) {
            return BackupParseResult.UnsupportedVersion(parsed.schemaVersion)
        }
        if (parsed.counters.any { it.phrase.isBlank() }) {
            return BackupParseResult.InvalidFormat
        }
        return BackupParseResult.Success(parsed)
    }

    fun writeToUri(context: Context, uri: Uri, jsonContent: String): Boolean {
        return runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(jsonContent.toByteArray(StandardCharsets.UTF_8))
                stream.flush()
            } ?: return false
            true
        }.getOrDefault(false)
    }

    fun readFromUri(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader(StandardCharsets.UTF_8)?.use { reader ->
                reader.readText()
            }
        }.getOrNull()
    }

    fun createShareIntent(
        context: Context,
        fileName: String,
        jsonContent: String,
        subject: String
    ): Intent {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val file = File(exportDir, safeName)
        file.writeText(jsonContent, StandardCharsets.UTF_8)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
