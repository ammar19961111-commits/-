package com.example.util

import android.os.Build
import com.example.ui.theme.AppLanguage
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.util.Calendar
import java.util.TimeZone

data class HijriDateInfo(
    val day: Int,
    val month: Int,
    val year: Int,
    val dayOfWeek: Int,
    val gregorianYear: Int,
    val gregorianMonth: Int,
    val gregorianDay: Int
) {
    val monthNameArabic: String
        get() = HijriDateHelper.arabicMonthName(month)

    val monthNameEnglish: String
        get() = HijriDateHelper.englishMonthName(month)

    val weekdayArabic: String
        get() = HijriDateHelper.arabicWeekdayName(dayOfWeek)

    val weekdayEnglish: String
        get() = HijriDateHelper.englishWeekdayName(dayOfWeek)

    val shortDateArabic: String
        get() = "$day $monthNameArabic $year هـ"

    val shortDateEnglish: String
        get() = "$day $monthNameEnglish $year AH"

    val fullDateArabic: String
        get() = "$weekdayArabic، $shortDateArabic"

    val fullDateEnglish: String
        get() = "$weekdayEnglish, $shortDateEnglish"

    val gregorianArabic: String
        get() = "$gregorianDay ${HijriDateHelper.arabicGregorianMonthName(gregorianMonth)} $gregorianYear م"

    val gregorianEnglish: String
        get() = "$gregorianDay ${HijriDateHelper.englishGregorianMonthName(gregorianMonth)} $gregorianYear"

    fun localizedFullDate(language: AppLanguage): String =
        if (language == AppLanguage.ENGLISH) fullDateEnglish else fullDateArabic

    fun localizedShortDate(language: AppLanguage): String =
        if (language == AppLanguage.ENGLISH) shortDateEnglish else shortDateArabic

    fun localizedGregorianDate(language: AppLanguage): String =
        if (language == AppLanguage.ENGLISH) gregorianEnglish else gregorianArabic
}

object HijriDateHelper {

    private val arabicMonths = listOf(
        "محرم",
        "صفر",
        "ربيع الأول",
        "ربيع الآخر",
        "جمادى الأولى",
        "جمادى الآخرة",
        "رجب",
        "شعبان",
        "رمضان",
        "شوال",
        "ذو القعدة",
        "ذو الحجة"
    )

    private val englishMonths = listOf(
        "Muharram",
        "Safar",
        "Rabi' al-Awwal",
        "Rabi' al-Thani",
        "Jumada al-Ula",
        "Jumada al-Akhirah",
        "Rajab",
        "Sha'ban",
        "Ramadan",
        "Shawwal",
        "Dhu al-Qi'dah",
        "Dhu al-Hijjah"
    )

    private val arabicGregorianMonths = listOf(
        "يناير",
        "فبراير",
        "مارس",
        "أبريل",
        "مايو",
        "يونيو",
        "يوليو",
        "أغسطس",
        "سبتمبر",
        "أكتوبر",
        "نوفمبر",
        "ديسمبر"
    )

    private val englishGregorianMonths = listOf(
        "Jan",
        "Feb",
        "Mar",
        "Apr",
        "May",
        "Jun",
        "Jul",
        "Aug",
        "Sep",
        "Oct",
        "Nov",
        "Dec"
    )

    fun arabicMonthName(month: Int): String =
        arabicMonths.getOrElse((month - 1).coerceIn(0, 11)) { arabicMonths.first() }

    fun englishMonthName(month: Int): String =
        englishMonths.getOrElse((month - 1).coerceIn(0, 11)) { englishMonths.first() }

    fun arabicGregorianMonthName(month: Int): String =
        arabicGregorianMonths.getOrElse((month - 1).coerceIn(0, 11)) { arabicGregorianMonths.first() }

    fun englishGregorianMonthName(month: Int): String =
        englishGregorianMonths.getOrElse((month - 1).coerceIn(0, 11)) { englishGregorianMonths.first() }

    fun arabicWeekdayName(calendarDayOfWeek: Int): String = when (calendarDayOfWeek) {
        Calendar.SUNDAY -> "الأحد"
        Calendar.MONDAY -> "الاثنين"
        Calendar.TUESDAY -> "الثلاثاء"
        Calendar.WEDNESDAY -> "الأربعاء"
        Calendar.THURSDAY -> "الخميس"
        Calendar.FRIDAY -> "الجمعة"
        Calendar.SATURDAY -> "السبت"
        else -> "الأحد"
    }

    fun englishWeekdayName(calendarDayOfWeek: Int): String = when (calendarDayOfWeek) {
        Calendar.SUNDAY -> "Sunday"
        Calendar.MONDAY -> "Monday"
        Calendar.TUESDAY -> "Tuesday"
        Calendar.WEDNESDAY -> "Wednesday"
        Calendar.THURSDAY -> "Thursday"
        Calendar.FRIDAY -> "Friday"
        Calendar.SATURDAY -> "Saturday"
        else -> "Sunday"
    }

    fun today(
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): HijriDateInfo {
        val calendar = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
        }
        val gYear = calendar.get(Calendar.YEAR)
        val gMonth = calendar.get(Calendar.MONTH) + 1
        val gDay = calendar.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val zoneId = runCatching { timeZone.toZoneId() }.getOrElse { ZoneId.systemDefault() }
            val localDate = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
            return calculateFromLocalDate(localDate, dayOfWeek)
        }
        return calculateForGregorianDate(gYear, gMonth, gDay, dayOfWeek)
    }

    fun calculateForGregorianDate(
        gregorianYear: Int,
        gregorianMonth: Int,
        gregorianDay: Int,
        calendarDayOfWeek: Int? = null
    ): HijriDateInfo {
        val resolvedDow = calendarDayOfWeek ?: Calendar.getInstance().apply {
            set(gregorianYear, gregorianMonth - 1, gregorianDay, 12, 0, 0)
        }.get(Calendar.DAY_OF_WEEK)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val localDate = LocalDate.of(gregorianYear, gregorianMonth, gregorianDay)
            return calculateFromLocalDate(localDate, resolvedDow)
        }
        return calculateTabularKuwaiti(gregorianYear, gregorianMonth, gregorianDay, resolvedDow)
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.O)
    private fun calculateFromLocalDate(
        localDate: LocalDate,
        calendarDayOfWeek: Int
    ): HijriDateInfo {
        val hijrah = runCatching { HijrahDate.from(localDate) }.getOrNull()
        if (hijrah != null) {
            return HijriDateInfo(
                day = hijrah.get(ChronoField.DAY_OF_MONTH),
                month = hijrah.get(ChronoField.MONTH_OF_YEAR),
                year = hijrah.get(ChronoField.YEAR_OF_ERA),
                dayOfWeek = calendarDayOfWeek,
                gregorianYear = localDate.year,
                gregorianMonth = localDate.monthValue,
                gregorianDay = localDate.dayOfMonth
            )
        }
        return calculateTabularKuwaiti(
            gregorianYear = localDate.year,
            gregorianMonth = localDate.monthValue,
            gregorianDay = localDate.dayOfMonth,
            calendarDayOfWeek = calendarDayOfWeek
        )
    }

    internal fun calculateTabularKuwaiti(
        gregorianYear: Int,
        gregorianMonth: Int,
        gregorianDay: Int,
        calendarDayOfWeek: Int
    ): HijriDateInfo {
        val y = gregorianYear
        val m = gregorianMonth
        val d = gregorianDay
        val jd = (1461 * (y + 4800 + (m - 14) / 12)) / 4 +
            (367 * (m - 2 - 12 * ((m - 14) / 12))) / 12 -
            (3 * ((y + 4900 + (m - 14) / 12) / 100)) / 4 +
            d - 32075

        val l = jd - 1948440 + 10632
        val n = (l - 1) / 10631
        val lPrime = l - 10631 * n + 354
        val j = ((10985 - lPrime) / 5316) * ((50 * lPrime) / 17719) +
            (lPrime / 5670) * ((43 * lPrime) / 15238)
        val lDoublePrime = lPrime -
            ((30 - j) / 15) * ((17719 * j) / 50) -
            (j / 16) * ((15238 * j) / 43) +
            29
        val hMonth = (24 * lDoublePrime) / 709
        val hDay = lDoublePrime - (709 * hMonth) / 24
        val hYear = 30 * n + j - 30

        return HijriDateInfo(
            day = hDay.coerceIn(1, 30),
            month = hMonth.coerceIn(1, 12),
            year = hYear.coerceAtLeast(1),
            dayOfWeek = calendarDayOfWeek,
            gregorianYear = gregorianYear,
            gregorianMonth = gregorianMonth,
            gregorianDay = gregorianDay
        )
    }
}
