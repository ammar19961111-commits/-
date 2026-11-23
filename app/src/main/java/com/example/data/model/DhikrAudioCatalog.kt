package com.example.data.model

/**
 * Represents a playable audio track mapped to a displayed [Dhikr].
 */
data class DhikrAudioTrack(
    val dhikrId: Int,
    val titleAr: String,
    val titleEn: String,
    val streamUrl: String,
    val isQuranic: Boolean,
    val categoryId: String,
    val hisnChapterId: Int? = null,
    val qualityLabel: String = if (isQuranic) "128 kbps MP3" else "HD MP3"
)

/**
 * Maps seed & displayed [Dhikr] entries to verified authentic HTTPS MP3 streams:
 * - Quranic verses & surahs use Mishary Rashid Alafasy via cdn.islamic.network.
 * - Prophetic Adhkar use Hisn al-Muslim chapters (upgraded at runtime to per-item
 *   MP3s via HisnAudioResolver when the official Hisn API responds).
 */
object DhikrAudioCatalog {

    private const val QURAN_AYAH_BASE = "https://cdn.islamic.network/quran/audio/128/ar.alafasy"
    private const val QURAN_SURAH_BASE = "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy"
    private const val HISN_AUDIO_BASE = "https://www.hisnmuslim.com/audio/ar"

    private val quranicStreamByDhikrId: Map<Int, String> = mapOf(
        // Ayat al-Kursi (Al-Baqarah 2:255 -> global ayah 262)
        101 to "$QURAN_AYAH_BASE/262.mp3",
        201 to "$QURAN_AYAH_BASE/262.mp3",
        307 to "$QURAN_AYAH_BASE/262.mp3",
        402 to "$QURAN_AYAH_BASE/262.mp3",
        // Al-Mu'awwidhat (Surah Al-Ikhlas 112)
        102 to "$QURAN_SURAH_BASE/112.mp3",
        202 to "$QURAN_SURAH_BASE/112.mp3",
        401 to "$QURAN_SURAH_BASE/112.mp3",
        // Sayyid al-Istighfar (known verified Hisn item URL)
        // Quranic Duas:
        601 to "$QURAN_AYAH_BASE/208.mp3",   // Al-Baqarah 2:201
        604 to "$QURAN_AYAH_BASE/2571.mp3",  // Al-Anbiya 21:87
        605 to "$QURAN_AYAH_BASE/2353.mp3",  // Taha 20:25
        606 to "$QURAN_AYAH_BASE/933.mp3"    // Al-A'raf 7:23
    )

    private val specificHisnStreamByDhikrId: Map<Int, String> = mapOf(
        104 to "$HISN_AUDIO_BASE/79.mp3",
        204 to "$HISN_AUDIO_BASE/79.mp3"
    )

    fun hisnChapterIdForCategory(categoryId: String): Int = when (categoryId) {
        DhikrCategory.MORNING.id, DhikrCategory.EVENING.id -> 27
        DhikrCategory.AFTER_PRAYER.id -> 25
        DhikrCategory.SLEEP.id -> 28
        DhikrCategory.WAKING.id -> 1
        DhikrCategory.DUAS.id -> 27
        else -> 27
    }

    fun defaultCategoryStreamUrl(categoryId: String): String = when (categoryId) {
        DhikrCategory.MORNING.id, DhikrCategory.EVENING.id ->
            "$HISN_AUDIO_BASE/ar_7esn_AlMoslem_by_Doors_028.mp3"
        DhikrCategory.AFTER_PRAYER.id ->
            "$HISN_AUDIO_BASE/ar_7esn_AlMoslem_by_Doors_026.mp3"
        DhikrCategory.SLEEP.id ->
            "$HISN_AUDIO_BASE/ar_7esn_AlMoslem_by_Doors_029.mp3"
        DhikrCategory.WAKING.id ->
            "$HISN_AUDIO_BASE/ar_7esn_AlMoslem_by_Doors_002.mp3"
        else ->
            "$HISN_AUDIO_BASE/ar_7esn_AlMoslem_by_Doors_028.mp3"
    }

    fun trackFor(dhikr: Dhikr): DhikrAudioTrack {
        val translation = AdhkarTranslations.forDhikr(dhikr.id)
        val titleEn = translation?.titleEn ?: dhikr.title
        val quranUrl = quranicStreamByDhikrId[dhikr.id]
        val isQuranic = quranUrl != null
        val fallbackUrl = quranUrl
            ?: specificHisnStreamByDhikrId[dhikr.id]
            ?: defaultCategoryStreamUrl(dhikr.category)

        return DhikrAudioTrack(
            dhikrId = dhikr.id,
            titleAr = dhikr.title,
            titleEn = titleEn,
            streamUrl = fallbackUrl,
            isQuranic = isQuranic,
            categoryId = dhikr.category,
            hisnChapterId = if (isQuranic) null else hisnChapterIdForCategory(dhikr.category),
            qualityLabel = if (isQuranic) "128 kbps MP3" else "HD MP3"
        )
    }

    fun buildQueue(adhkar: List<Dhikr>): List<DhikrAudioTrack> =
        adhkar.map(::trackFor)
}
