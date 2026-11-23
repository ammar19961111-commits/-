package com.example.data.model

data class QuranRecitation(
    val id: String,
    val surahNumber: Int,
    val titleAr: String,
    val subtitleAr: String,
    val titleEn: String,
    val subtitleEn: String,
    val streamUrl: String
)

object DailyQuranCatalog {
    private const val BASE_SURAH_URL = "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy"

    val tracks: List<QuranRecitation> = listOf(
        QuranRecitation(
            id = "fatiha",
            surahNumber = 1,
            titleAr = "سورة الفاتحة",
            subtitleAr = "السبع المثاني • ٧ آيات",
            titleEn = "Surah Al-Fatihah",
            subtitleEn = "The Opening • 7 Ayahs",
            streamUrl = "$BASE_SURAH_URL/1.mp3"
        ),
        QuranRecitation(
            id = "ikhlas",
            surahNumber = 112,
            titleAr = "سورة الإخلاص",
            subtitleAr = "تعدل ثلث القرآن • ٤ آيات",
            titleEn = "Surah Al-Ikhlas",
            subtitleEn = "The Sincerity • 4 Ayahs",
            streamUrl = "$BASE_SURAH_URL/112.mp3"
        ),
        QuranRecitation(
            id = "falaq",
            surahNumber = 113,
            titleAr = "سورة الفلق",
            subtitleAr = "المعوّذات • ٥ آيات",
            titleEn = "Surah Al-Falaq",
            subtitleEn = "The Daybreak • 5 Ayahs",
            streamUrl = "$BASE_SURAH_URL/113.mp3"
        ),
        QuranRecitation(
            id = "nas",
            surahNumber = 114,
            titleAr = "سورة الناس",
            subtitleAr = "المعوّذات • ٦ آيات",
            titleEn = "Surah An-Nas",
            subtitleEn = "Mankind • 6 Ayahs",
            streamUrl = "$BASE_SURAH_URL/114.mp3"
        ),
        QuranRecitation(
            id = "sharh",
            surahNumber = 94,
            titleAr = "سورة الشرح",
            subtitleAr = "طمأنينة الصدر • ٨ آيات",
            titleEn = "Surah Ash-Sharh",
            subtitleEn = "Comfort of the Heart • 8 Ayahs",
            streamUrl = "$BASE_SURAH_URL/94.mp3"
        ),
        QuranRecitation(
            id = "qadr",
            surahNumber = 97,
            titleAr = "سورة القدر",
            subtitleAr = "فضل الليل والذكر • ٥ آيات",
            titleEn = "Surah Al-Qadr",
            subtitleEn = "The Night of Decree • 5 Ayahs",
            streamUrl = "$BASE_SURAH_URL/97.mp3"
        ),
        QuranRecitation(
            id = "asr",
            surahNumber = 103,
            titleAr = "سورة العصر",
            subtitleAr = "التواصي بالحق والصبر • ٣ آيات",
            titleEn = "Surah Al-Asr",
            subtitleEn = "Truth & Patience • 3 Ayahs",
            streamUrl = "$BASE_SURAH_URL/103.mp3"
        ),
        QuranRecitation(
            id = "kawthar",
            surahNumber = 108,
            titleAr = "سورة الكوثر",
            subtitleAr = "الخير الكثير • ٣ آيات",
            titleEn = "Surah Al-Kawthar",
            subtitleEn = "Abundance of Good • 3 Ayahs",
            streamUrl = "$BASE_SURAH_URL/108.mp3"
        )
    )
}
