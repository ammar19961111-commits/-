package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.example.data.model.AdhkarTranslations
import com.example.data.model.DailyReflection
import com.example.data.model.Dhikr
import com.example.ui.theme.AppLanguage

object ShareHelper {

    fun buildShareDhikrText(
        dhikr: Dhikr,
        appLanguage: AppLanguage = AppLanguage.ARABIC
    ): String {
        val en = if (appLanguage == AppLanguage.ENGLISH) AdhkarTranslations.forDhikr(dhikr.id) else null
        return buildString {
            append(en?.titleEn ?: dhikr.title)
            append("\n\n")
            append(dhikr.content)
            if (en != null) {
                append("\n\n")
                append(en.translationEn)
            }
            val virtueText = en?.virtueEn ?: dhikr.virtue
            if (virtueText.isNotBlank()) {
                append(if (appLanguage == AppLanguage.ENGLISH) "\n\nVirtue: " else "\n\nالفضل: ")
                append(virtueText)
            }
            val sourceText = en?.sourceEn ?: dhikr.source
            if (sourceText.isNotBlank()) {
                append("\n(")
                append(sourceText)
                append(")")
            }
        }
    }

    fun createShareDhikrIntent(
        dhikr: Dhikr,
        appLanguage: AppLanguage = AppLanguage.ARABIC
    ): Intent {
        val en = if (appLanguage == AppLanguage.ENGLISH) AdhkarTranslations.forDhikr(dhikr.id) else null
        val subject = en?.titleEn ?: dhikr.title
        val formatted = buildShareDhikrText(dhikr, appLanguage)
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, formatted)
        }
    }

    fun shareDhikr(
        context: Context,
        dhikr: Dhikr,
        appLanguage: AppLanguage = AppLanguage.ARABIC
    ) {
        val intent = createShareDhikrIntent(dhikr, appLanguage)
        val title = if (appLanguage == AppLanguage.ENGLISH) "Share Dhikr" else "مشاركة الذكر"
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun copyDhikr(
        context: Context,
        dhikr: Dhikr,
        appLanguage: AppLanguage = AppLanguage.ARABIC
    ) {
        val en = if (appLanguage == AppLanguage.ENGLISH) AdhkarTranslations.forDhikr(dhikr.id) else null
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val text = if (en != null) {
            "${en.titleEn}\n\n${dhikr.content}\n\n${en.translationEn}\n\n(${en.sourceEn})"
        } else {
            "${dhikr.content}\n\n(${dhikr.source})"
        }
        val clip = ClipData.newPlainText(en?.titleEn ?: dhikr.title, text)
        clipboard?.setPrimaryClip(clip)
    }

    fun shareReflection(
        context: Context,
        reflection: DailyReflection,
        chooserTitle: String,
        appLanguage: AppLanguage = AppLanguage.ARABIC
    ) {
        val formatted = buildString {
            append(reflection.arabicText)
            if (appLanguage == AppLanguage.ENGLISH && reflection.englishText.isNotBlank()) {
                append("\n\n")
                append(reflection.englishText)
                append("\n\n— ")
                append(reflection.englishSource.ifBlank { reflection.sourceReference })
                append("\n\n")
                append(reflection.englishReflection.ifBlank { reflection.spiritualReflection })
            } else {
                append("\n\n— ")
                append(reflection.sourceReference)
                append("\n\n")
                append(reflection.spiritualReflection)
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, formatted)
        }
        val chooser = Intent.createChooser(intent, chooserTitle)
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun copyReflection(
        context: Context,
        reflection: DailyReflection,
        appLanguage: AppLanguage = AppLanguage.ARABIC
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val text = if (appLanguage == AppLanguage.ENGLISH && reflection.englishText.isNotBlank()) {
            "${reflection.arabicText}\n\n${reflection.englishText}\n(${reflection.englishSource})\n${reflection.englishReflection}"
        } else {
            "${reflection.arabicText}\n(${reflection.sourceReference})\n${reflection.spiritualReflection}"
        }
        val clip = ClipData.newPlainText(
            if (appLanguage == AppLanguage.ENGLISH) reflection.englishSource else reflection.sourceReference,
            text
        )
        clipboard?.setPrimaryClip(clip)
    }

    fun copyText(
        context: Context,
        label: String,
        text: String
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    fun shareText(
        context: Context,
        text: String,
        chooserTitle: String
    ) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
