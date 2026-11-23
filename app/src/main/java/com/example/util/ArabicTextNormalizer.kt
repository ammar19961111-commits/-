package com.example.util

import com.example.data.model.AdhkarTranslations
import com.example.data.model.Dhikr

object ArabicTextNormalizer {
    private val diacriticsAndTatweelRegex =
        Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]")
    private val multiSpaceRegex = Regex("\\s+")

    fun normalize(input: String): String {
        if (input.isBlank()) return ""
        val stripped = diacriticsAndTatweelRegex.replace(input, "")
        val sb = StringBuilder(stripped.length)
        for (ch in stripped) {
            when (ch) {
                'أ', 'إ', 'آ', 'ٱ' -> sb.append('ا')
                'ى' -> sb.append('ي')
                'ة' -> sb.append('ه')
                'ؤ' -> sb.append('و')
                'ئ' -> sb.append('ي')
                else -> sb.append(ch.lowercaseChar())
            }
        }
        return multiSpaceRegex.replace(sb.toString().trim(), " ")
    }

    fun matches(dhikr: Dhikr, query: String): Boolean {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isEmpty()) return true
        val arabicMatch = normalize(dhikr.title).contains(normalizedQuery) ||
            normalize(dhikr.content).contains(normalizedQuery) ||
            normalize(dhikr.virtue).contains(normalizedQuery) ||
            normalize(dhikr.source).contains(normalizedQuery)
        if (arabicMatch) return true

        val english = AdhkarTranslations.forDhikr(dhikr.id) ?: return false
        return normalize(english.titleEn).contains(normalizedQuery) ||
            normalize(english.translationEn).contains(normalizedQuery) ||
            normalize(english.virtueEn).contains(normalizedQuery) ||
            normalize(english.sourceEn).contains(normalizedQuery)
    }
}
