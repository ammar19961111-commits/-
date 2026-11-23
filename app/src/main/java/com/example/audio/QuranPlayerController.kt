package com.example.audio

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.DailyQuranCatalog
import com.example.data.model.QuranRecitation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class QuranPlayerState(
    val tracks: List<QuranRecitation> = DailyQuranCatalog.tracks,
    val currentTrackIndex: Int = 0,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val repeatOne: Boolean = false,
    val hasStarted: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val hasError: Boolean = false
) {
    val currentTrack: QuranRecitation
        get() = tracks.getOrElse(currentTrackIndex) { tracks.first() }

    val progressFraction: Float
        get() = if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
}

interface QuranPlayerController {
    val state: StateFlow<QuranPlayerState>
    fun selectAndPlay(index: Int)
    fun togglePlayPause()
    fun nextTrack()
    fun previousTrack()
    fun seekTo(positionMs: Long)
    fun toggleRepeatOne()
    fun pauseIfPlaying()
    fun stopAndHide()
    fun release()
}

object NoOpQuranPlayerController : QuranPlayerController {
    private val _state = MutableStateFlow(QuranPlayerState())
    override val state: StateFlow<QuranPlayerState> = _state.asStateFlow()
    override fun selectAndPlay(index: Int) = Unit
    override fun togglePlayPause() = Unit
    override fun nextTrack() = Unit
    override fun previousTrack() = Unit
    override fun seekTo(positionMs: Long) = Unit
    override fun toggleRepeatOne() = Unit
    override fun pauseIfPlaying() = Unit
    override fun stopAndHide() = Unit
    override fun release() = Unit
}

class ExoQuranPlayerController(
    context: Context
) : QuranPlayerController {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(QuranPlayerState())
    override val state: StateFlow<QuranPlayerState> = _state.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var progressJob: Job? = null

    private fun ensurePlayer(): ExoPlayer {
        exoPlayer?.let { return it }
        val tracks = _state.value.tracks
        val player = ExoPlayer.Builder(appContext)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                true
            )
            .build()

        val mediaItems = tracks.map { track ->
            MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(track.streamUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.titleAr)
                        .setArtist("مشاري راشد العفاسي")
                        .build()
                )
                .build()
        }
        player.setMediaItems(mediaItems)
        player.repeatMode = if (_state.value.repeatOne) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update {
                    it.copy(
                        isPlaying = isPlaying,
                        durationMs = player.duration.coerceAtLeast(0L)
                    )
                }
                if (isPlaying) startProgressTicker() else stopProgressTicker()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val buffering = playbackState == Player.STATE_BUFFERING
                val dur = player.duration.coerceAtLeast(0L)
                _state.update {
                    it.copy(
                        isBuffering = buffering,
                        durationMs = if (dur > 0L) dur else it.durationMs,
                        hasError = if (playbackState == Player.STATE_READY) false else it.hasError
                    )
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val idx = player.currentMediaItemIndex.coerceIn(0, tracks.lastIndex)
                _state.update {
                    it.copy(
                        currentTrackIndex = idx,
                        positionMs = 0L,
                        durationMs = player.duration.coerceAtLeast(0L)
                    )
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _state.update {
                    it.copy(
                        isPlaying = false,
                        isBuffering = false,
                        hasError = true
                    )
                }
            }
        })
        player.prepare()
        exoPlayer = player
        return player
    }

    override fun selectAndPlay(index: Int) {
        val tracks = _state.value.tracks
        if (index !in tracks.indices) return
        val player = ensurePlayer()
        _state.update {
            it.copy(
                currentTrackIndex = index,
                hasStarted = true,
                hasError = false,
                positionMs = 0L
            )
        }
        player.seekTo(index, 0L)
        if (player.playbackState == Player.STATE_IDLE) {
            player.prepare()
        }
        player.play()
    }

    override fun togglePlayPause() {
        val existing = exoPlayer
        if (existing == null) {
            selectAndPlay(_state.value.currentTrackIndex)
            return
        }
        if (existing.isPlaying) {
            existing.pause()
        } else {
            _state.update { it.copy(hasStarted = true, hasError = false) }
            if (existing.playbackState == Player.STATE_IDLE || existing.playbackState == Player.STATE_ENDED) {
                if (existing.playbackState == Player.STATE_ENDED) {
                    existing.seekTo(0L)
                }
                existing.prepare()
            }
            existing.play()
        }
    }

    override fun nextTrack() {
        val tracks = _state.value.tracks
        val next = (_state.value.currentTrackIndex + 1) % tracks.size
        selectAndPlay(next)
    }

    override fun previousTrack() {
        val tracks = _state.value.tracks
        val prev = if (_state.value.currentTrackIndex - 1 < 0) tracks.lastIndex else _state.value.currentTrackIndex - 1
        selectAndPlay(prev)
    }

    override fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceAtLeast(0L)
        exoPlayer?.seekTo(clamped)
        _state.update { it.copy(positionMs = clamped) }
    }

    override fun toggleRepeatOne() {
        val nextRepeat = !_state.value.repeatOne
        exoPlayer?.repeatMode = if (nextRepeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        _state.update { it.copy(repeatOne = nextRepeat) }
    }

    override fun pauseIfPlaying() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            }
        }
    }

    override fun stopAndHide() {
        stopProgressTicker()
        exoPlayer?.pause()
        exoPlayer?.seekTo(0L)
        _state.update {
            it.copy(
                isPlaying = false,
                isBuffering = false,
                hasStarted = false,
                positionMs = 0L,
                hasError = false
            )
        }
    }

    override fun release() {
        stopProgressTicker()
        exoPlayer?.release()
        exoPlayer = null
        scope.cancel()
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val player = exoPlayer
                if (player != null) {
                    val dur = player.duration.coerceAtLeast(0L)
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    _state.update {
                        it.copy(
                            positionMs = pos,
                            durationMs = if (dur > 0L) dur else it.durationMs
                        )
                    }
                }
                delay(250L)
            }
        }
    }

    private fun stopProgressTicker() {
        progressJob?.cancel()
        progressJob = null
    }
}
