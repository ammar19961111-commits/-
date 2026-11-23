package com.example.data.model

import com.example.ui.theme.AppLanguage
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

enum class PrayerName(
    val apiKey: String,
    val arabicName: String,
    val englishName: String
) {
    FAJR("Fajr", "الفجر", "Fajr"),
    DHUHR("Dhuhr", "الظهر", "Dhuhr"),
    ASR("Asr", "العصر", "Asr"),
    MAGHRIB("Maghrib", "المغرب", "Maghrib"),
    ISHA("Isha", "العشاء", "Isha");

    fun displayName(language: AppLanguage): String =
        if (language == AppLanguage.ENGLISH) englishName else arabicName
}

enum class PrayerCalculationMethod(
    val apiMethodId: Int,
    val arabicLabel: String,
    val englishLabel: String
) {
    UMM_AL_QURA(4, "أم القرى", "Umm al-Qura"),
    MUSLIM_WORLD_LEAGUE(3, "رابطة العالم الإسلامي", "Muslim World League"),
    EGYPTIAN(5, "الهيئة المصرية", "Egyptian Authority"),
    ISNA(2, "أمريكا الشمالية", "ISNA"),
    KARACHI(1, "كراتشي", "Karachi");

    fun displayLabel(language: AppLanguage): String =
        if (language == AppLanguage.ENGLISH) englishLabel else arabicLabel

    companion object {
        fun fromId(id: Int): PrayerCalculationMethod =
            entries.find { it.apiMethodId == id } ?: UMM_AL_QURA
    }
}

data class PrayerCityPreset(
    val id: String,
    val arabicName: String,
    val englishName: String,
    val latitude: Double,
    val longitude: Double
) {
    fun displayName(language: AppLanguage): String =
        if (language == AppLanguage.ENGLISH) englishName else arabicName
}

object PrayerCityCatalog {
    val cities: List<PrayerCityPreset> = listOf(
        PrayerCityPreset("makkah", "مكة المكرمة", "Makkah", 21.3891, 39.8579),
        PrayerCityPreset("madinah", "المدينة المنورة", "Madinah", 24.5247, 39.5692),
        PrayerCityPreset("riyadh", "الرياض", "Riyadh", 24.7136, 46.6753),
        PrayerCityPreset("cairo", "القاهرة", "Cairo", 30.0444, 31.2357),
        PrayerCityPreset("amman", "عمّان", "Amman", 31.9454, 35.9284),
        PrayerCityPreset("dubai", "دبي", "Dubai", 25.2048, 55.2708),
        PrayerCityPreset("london", "لندن", "London", 51.5072, -0.1276)
    )

    fun localizeLabel(rawLabel: String, language: AppLanguage): String {
        if (language == AppLanguage.ARABIC) return rawLabel
        if (rawLabel == "موقعي الحالي") return "Current Location"
        return cities.find { it.arabicName == rawLabel }?.englishName ?: rawLabel
    }
}

data class PrayerTimeEntry(
    val prayer: PrayerName,
    val time24h: String,
    val formattedTime: String,
    val minutesOfDay: Int,
    val isNext: Boolean = false
) {
    fun localizedTime(language: AppLanguage): String =
        if (language == AppLanguage.ENGLISH) {
            PrayerTimeCalculator.formatEnglish12h(time24h)
        } else {
            formattedTime
        }
}

data class DailyPrayerSchedule(
    val dateReadable: String,
    val hijriDateAr: String,
    val hijriDateEn: String = "",
    val sunriseTime: String,
    val sunriseTime24: String = "",
    val locationLabel: String,
    val timezone: String,
    val latitude: Double,
    val longitude: Double,
    val method: PrayerCalculationMethod,
    val entries: List<PrayerTimeEntry>,
    val nextPrayer: PrayerTimeEntry?,
    val countdownText: String,
    val remainingMinutes: Int = 0
) {
    fun localizedHijriDate(language: AppLanguage): String =
        if (language == AppLanguage.ENGLISH && hijriDateEn.isNotBlank()) {
            hijriDateEn
        } else {
            hijriDateAr
        }

    fun localizedSunrise(language: AppLanguage): String =
        if (language == AppLanguage.ENGLISH && sunriseTime24.isNotBlank()) {
            PrayerTimeCalculator.formatEnglish12h(sunriseTime24)
        } else {
            sunriseTime
        }

    fun localizedCountdown(language: AppLanguage): String =
        if (language == AppLanguage.ENGLISH && remainingMinutes > 0) {
            PrayerTimeCalculator.formatRemainingEnglish(remainingMinutes)
        } else {
            countdownText
        }

    val formattedCoordinates: String
        get() = String.format(Locale.US, "%.2f°, %.2f°", latitude, longitude)
}

object PrayerTimeCalculator {

    fun cleanRawTime(raw: String): String {
        val token = raw.trim().substringBefore(" ")
        val parts = token.split(":")
        if (parts.size < 2) return "00:00"
        val hour = parts[0].toIntOrNull()?.coerceIn(0, 23) ?: 0
        val minute = parts[1].toIntOrNull()?.coerceIn(0, 59) ?: 0
        return String.format(Locale.US, "%02d:%02d", hour, minute)
    }

    fun parseMinutesOfDay(time24h: String): Int {
        val cleaned = cleanRawTime(time24h)
        val parts = cleaned.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return hour * 60 + minute
    }

    fun formatArabic12h(time24h: String): String {
        val cleaned = cleanRawTime(time24h)
        val parts = cleaned.split(":")
        val hour24 = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val period = if (hour24 < 12) "ص" else "م"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return String.format(Locale.US, "%d:%02d %s", hour12, minute, period)
    }

    fun formatEnglish12h(time24h: String): String {
        val cleaned = cleanRawTime(time24h)
        val parts = cleaned.split(":")
        val hour24 = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val period = if (hour24 < 12) "AM" else "PM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return String.format(Locale.US, "%d:%02d %s", hour12, minute, period)
    }

    fun buildSchedule(
        fajrRaw: String,
        sunriseRaw: String,
        dhuhrRaw: String,
        asrRaw: String,
        maghribRaw: String,
        ishaRaw: String,
        dateReadable: String,
        hijriDateAr: String,
        hijriDateEn: String = "",
        locationLabel: String,
        timezone: String,
        latitude: Double,
        longitude: Double,
        method: PrayerCalculationMethod,
        nowMinutesOfDay: Int = currentMinutesOfDay(timezone)
    ): DailyPrayerSchedule {
        val rawMap = listOf(
            PrayerName.FAJR to fajrRaw,
            PrayerName.DHUHR to dhuhrRaw,
            PrayerName.ASR to asrRaw,
            PrayerName.MAGHRIB to maghribRaw,
            PrayerName.ISHA to ishaRaw
        )

        val baseEntries = rawMap.map { (name, raw) ->
            val clean = cleanRawTime(raw)
            PrayerTimeEntry(
                prayer = name,
                time24h = clean,
                formattedTime = formatArabic12h(clean),
                minutesOfDay = parseMinutesOfDay(clean)
            )
        }

        val upcoming = baseEntries.firstOrNull { it.minutesOfDay > nowMinutesOfDay } ?: baseEntries.first()
        val deltaMinutes = if (upcoming.minutesOfDay > nowMinutesOfDay) {
            upcoming.minutesOfDay - nowMinutesOfDay
        } else {
            (24 * 60 - nowMinutesOfDay) + upcoming.minutesOfDay
        }

        val markedEntries = baseEntries.map { entry ->
            entry.copy(isNext = entry.prayer == upcoming.prayer)
        }
        val nextEntry = markedEntries.first { it.prayer == upcoming.prayer }
        val cleanSunrise = cleanRawTime(sunriseRaw)

        return DailyPrayerSchedule(
            dateReadable = dateReadable,
            hijriDateAr = hijriDateAr,
            hijriDateEn = hijriDateEn,
            sunriseTime = formatArabic12h(cleanSunrise),
            sunriseTime24 = cleanSunrise,
            locationLabel = locationLabel,
            timezone = timezone,
            latitude = latitude,
            longitude = longitude,
            method = method,
            entries = markedEntries,
            nextPrayer = nextEntry,
            countdownText = formatRemainingArabic(deltaMinutes),
            remainingMinutes = deltaMinutes
        )
    }

    fun formatRemainingArabic(totalMinutes: Int): String {
        val safe = totalMinutes.coerceAtLeast(1)
        val hours = safe / 60
        val mins = safe % 60
        return when {
            hours > 0 && mins > 0 -> "$hours س $mins د"
            hours > 0 -> "$hours س"
            else -> "$mins د"
        }
    }

    fun formatRemainingEnglish(totalMinutes: Int): String {
        val safe = totalMinutes.coerceAtLeast(1)
        val hours = safe / 60
        val mins = safe % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }

    private fun currentMinutesOfDay(timezone: String = ""): Int {
        val tz = if (timezone.isNotBlank() && TimeZone.getAvailableIDs().contains(timezone)) {
            TimeZone.getTimeZone(timezone)
        } else {
            TimeZone.getDefault()
        }
        val cal = Calendar.getInstance(tz)
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }
}
