package com.example.data.remote

import com.example.data.model.Dhikr
import com.example.data.model.DhikrAudioCatalog
import com.example.data.model.DhikrAudioTrack
import com.example.util.ArabicTextNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

data class HisnRemoteItem(
    val normalizedArabicText: String,
    val audioUrl: String
)

interface HisnAudioTrackResolver {
    suspend fun resolveTracks(adhkar: List<Dhikr>): List<DhikrAudioTrack>
}

object NoOpHisnAudioTrackResolver : HisnAudioTrackResolver {
    override suspend fun resolveTracks(adhkar: List<Dhikr>): List<DhikrAudioTrack> =
        DhikrAudioCatalog.buildQueue(adhkar)
}

/**
 * Resolves exact per-dhikr HTTPS MP3 URLs from the official Hisn al-Muslim chapter API
 * (e.g. https://www.hisnmuslim.com/api/ar/27.json) and caches them in memory per chapter.
 * Safely falls back to [DhikrAudioCatalog] URLs whenever offline or unmatched.
 */
class HttpHisnAudioTrackResolver(
    private val fetcher: suspend (Int) -> String? = ::fetchChapterJson
) : HisnAudioTrackResolver {

    private val chapterCache = ConcurrentHashMap<Int, List<HisnRemoteItem>>()

    override suspend fun resolveTracks(adhkar: List<Dhikr>): List<DhikrAudioTrack> {
        val baseTracks = DhikrAudioCatalog.buildQueue(adhkar)
        val neededChapters = baseTracks.mapNotNull { it.hisnChapterId }.distinct()
        if (neededChapters.isEmpty()) return baseTracks

        val dhikrById = adhkar.associateBy { it.id }

        val uncachedChapters = neededChapters.filter { !chapterCache.containsKey(it) }
        if (uncachedChapters.isNotEmpty()) {
            coroutineScope {
                uncachedChapters.map { chapterId ->
                    async(Dispatchers.IO) {
                        val json = runCatching { fetcher(chapterId) }.getOrNull()
                        if (!json.isNullOrBlank()) {
                            val parsed = parseChapterItems(json)
                            if (parsed.isNotEmpty()) {
                                chapterCache[chapterId] = parsed
                            }
                        }
                    }
                }.awaitAll()
            }
        }

        return baseTracks.map { track ->
            val chapterId = track.hisnChapterId ?: return@map track
            val items = chapterCache[chapterId] ?: return@map track
            val dhikr = dhikrById[track.dhikrId] ?: return@map track
            val matchedUrl = findMatchingAudioUrl(dhikr.content, items)
            if (matchedUrl != null) {
                track.copy(streamUrl = matchedUrl)
            } else {
                track
            }
        }
    }

    private fun stripPunctuation(normalized: String): String =
        normalized.replace(Regex("[،,;:.!?()\\[\\]{}\"«»\\-۝۞]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    internal fun findMatchingAudioUrl(content: String, items: List<HisnRemoteItem>): String? {
        val normalizedTarget = stripPunctuation(ArabicTextNormalizer.normalize(content))
        if (normalizedTarget.isBlank()) return null

        val targetPrefix = normalizedTarget.take(32)
        val targetTokens = normalizedTarget.split(' ').filter { it.length >= 3 }.toSet()

        var bestUrl: String? = null
        var bestScore = 0.0

        for (item in items) {
            val candidate = stripPunctuation(item.normalizedArabicText)
            if (candidate.contains(targetPrefix) || normalizedTarget.contains(candidate.take(32))) {
                return item.audioUrl
            }
            if (targetTokens.isEmpty()) continue
            val candidateTokens = candidate.split(' ').filter { it.length >= 3 }.toSet()
            val overlap = targetTokens.intersect(candidateTokens).size
            val score = overlap.toDouble() / targetTokens.size.toDouble()
            if (score > bestScore && score >= 0.45) {
                bestScore = score
                bestUrl = item.audioUrl
            }
        }
        return bestUrl
    }

    internal fun parseChapterItems(rawJson: String): List<HisnRemoteItem> {
        val results = mutableListOf<HisnRemoteItem>()
        val root = JSONObject(rawJson)
        val keys = root.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val array = root.optJSONArray(key) ?: continue
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val arabicText = obj.optString("ARABIC_TEXT").orEmpty()
                val rawAudio = obj.optString("AUDIO").orEmpty().trim()
                if (arabicText.isNotBlank() && rawAudio.startsWith("http")) {
                    val httpsAudio = rawAudio.replaceFirst("http://", "https://")
                    results.add(
                        HisnRemoteItem(
                            normalizedArabicText = ArabicTextNormalizer.normalize(arabicText),
                            audioUrl = httpsAudio
                        )
                    )
                }
            }
        }
        return results
    }

    companion object {
        suspend fun fetchChapterJson(chapterId: Int): String? = withContext(Dispatchers.IO) {
            val url = URL("https://www.hisnmuslim.com/api/ar/$chapterId.json")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 2500
                readTimeout = 2500
                requestMethod = "GET"
            }
            try {
                if (connection.responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    null
                }
            } finally {
                connection.disconnect()
            }
        }
    }
}
