package com.example.data.remote

import com.example.data.model.DailyPrayerSchedule
import com.example.data.model.PrayerCalculationMethod
import com.example.data.model.PrayerTimeCalculator
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class AlAdhanResponse(
    val code: Int,
    val status: String,
    val data: AlAdhanData
)

@JsonClass(generateAdapter = true)
data class AlAdhanData(
    val timings: AlAdhanTimings,
    val date: AlAdhanDate,
    val meta: AlAdhanMeta
)

@JsonClass(generateAdapter = true)
data class AlAdhanTimings(
    @Json(name = "Fajr") val fajr: String,
    @Json(name = "Sunrise") val sunrise: String,
    @Json(name = "Dhuhr") val dhuhr: String,
    @Json(name = "Asr") val asr: String,
    @Json(name = "Maghrib") val maghrib: String,
    @Json(name = "Isha") val isha: String
)

@JsonClass(generateAdapter = true)
data class AlAdhanDate(
    val readable: String = "",
    val hijri: AlAdhanHijri? = null
)

@JsonClass(generateAdapter = true)
data class AlAdhanHijri(
    val day: String = "",
    val year: String = "",
    val month: AlAdhanHijriMonth? = null,
    val weekday: AlAdhanHijriWeekday? = null
)

@JsonClass(generateAdapter = true)
data class AlAdhanHijriMonth(
    val ar: String = "",
    val en: String = ""
)

@JsonClass(generateAdapter = true)
data class AlAdhanHijriWeekday(
    val ar: String = "",
    val en: String = ""
)

@JsonClass(generateAdapter = true)
data class AlAdhanMeta(
    val timezone: String = ""
)

interface AlAdhanRetrofitApi {
    @GET("v1/timings")
    suspend fun getTimings(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int
    ): AlAdhanResponse
}

interface PrayerTimesRemoteSource {
    suspend fun fetchDailyPrayerTimes(
        latitude: Double,
        longitude: Double,
        locationLabel: String,
        method: PrayerCalculationMethod
    ): DailyPrayerSchedule
}

class AlAdhanPrayerTimesRemoteSource(
    private val api: AlAdhanRetrofitApi = createDefaultApi()
) : PrayerTimesRemoteSource {

    override suspend fun fetchDailyPrayerTimes(
        latitude: Double,
        longitude: Double,
        locationLabel: String,
        method: PrayerCalculationMethod
    ): DailyPrayerSchedule {
        val response = api.getTimings(
            latitude = latitude,
            longitude = longitude,
            method = method.apiMethodId
        )
        if (response.code != 200) {
            throw IllegalStateException("AlAdhan API error ${response.code}: ${response.status}")
        }
        val data = response.data
        val timings = data.timings
        val hijri = data.date.hijri
        val hijriDateAr = buildString {
            val weekday = hijri?.weekday?.ar.orEmpty()
            val day = hijri?.day.orEmpty()
            val month = hijri?.month?.ar.orEmpty()
            val year = hijri?.year.orEmpty()
            if (weekday.isNotBlank()) append(weekday).append(" • ")
            if (day.isNotBlank() && month.isNotBlank()) {
                append(day).append(" ").append(month)
                if (year.isNotBlank()) append(" ").append(year).append(" هـ")
            }
        }
        val hijriDateEn = buildString {
            val weekday = hijri?.weekday?.en.orEmpty()
            val day = hijri?.day.orEmpty()
            val month = hijri?.month?.en.orEmpty()
            val year = hijri?.year.orEmpty()
            if (weekday.isNotBlank()) append(weekday).append(" • ")
            if (day.isNotBlank() && month.isNotBlank()) {
                append(day).append(" ").append(month)
                if (year.isNotBlank()) append(" ").append(year).append(" AH")
            }
        }
        val resolvedLocation = locationLabel.ifBlank {
            data.meta.timezone.substringAfterLast("/").replace("_", " ")
        }

        return PrayerTimeCalculator.buildSchedule(
            fajrRaw = timings.fajr,
            sunriseRaw = timings.sunrise,
            dhuhrRaw = timings.dhuhr,
            asrRaw = timings.asr,
            maghribRaw = timings.maghrib,
            ishaRaw = timings.isha,
            dateReadable = data.date.readable,
            hijriDateAr = hijriDateAr,
            hijriDateEn = hijriDateEn,
            locationLabel = resolvedLocation,
            timezone = data.meta.timezone,
            latitude = latitude,
            longitude = longitude,
            method = method
        )
    }

    companion object {
        private const val BASE_URL = "https://api.aladhan.com/"

        fun createDefaultApi(): AlAdhanRetrofitApi {
            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(AlAdhanRetrofitApi::class.java)
        }
    }
}
