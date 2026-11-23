package com.example.audio

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.Dhikr
import com.example.data.model.DhikrAudioCatalog
import com.example.data.model.DhikrAudioTrack
import com.example.data.remote.HisnAudioTrackResolver
import com.example.data.remote.HttpHisnAudioTrackResolver
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

data class DhikrAudioPlayerState(
    val queue: List<DhikrAudioTrack> = emptyList(),
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val hasStarted: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val repeatCurrent: Boolean = false,
    val hasError: Boolean = false
) {
    val currentTrack: DhikrAudioTrack?
        get() = queue.getOrNull(currentIndex)

    val activeDhikrId: Int?
        get() = if (hasStarted) currentTrack?.dhikrId else null

    val progressFraction: Float
        get() = if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
}

interface DhikrAudioPlayerController {
    val state: StateFlow<DhikrAudioPlayerState>
    fun playDisplayedAdhkar(adhkar: List<Dhikr>, startAtDhikrId: Int? = null)
    fun togglePlayPause(dhikr: Dhikr? = null, displayedAdhkar: List<Dhikr> = emptyList())
    fun pauseIfPlaying()
    fun nextTrack()
    fun previousTrack()
    fun seekTo(positionMs: Long)
    fun cyclePlaybackSpeed()
    fun toggleRepeatCurrent()
    fun retryCurrent()
    fun stopAndHide()
    fun release()
}

object NoOpDhikrAudioPlayerController : DhikrAudioPlayerController {
    private val _state = MutableStateFlow(DhikrAudioPlayerState())
    override val state: StateFlow<DhikrAudioPlayerState> = _state.asStateFlow()
    override fun playDisplayedAdhkar(adhkar: List<Dhikr>, startAtDhikrId: Int?) = Unit
    override fun togglePlayPause(dhikr: Dhikr?, displayedAdhkar: List<Dhikr>) = Unit
    override fun pauseIfPlaying() = Unit
    override fun nextTrack() = Unit
    override fun previousTrack() = Unit
    override fun seekTo(positionMs: Long) = Unit
    override fun cyclePlaybackSpeed() = Unit
    override fun toggleRepeatCurrent() = Unit
    override fun retryCurrent() = Unit
    override fun stopAndHide() = Unit
    override fun release() = Unit
}

class ExoDhikrAudioPlayerController(
    context: Context,
    private val trackResolver: HisnAudioTrackResolver = HttpHisnAudioTrackResolver()
) : DhikrAudioPlayerController {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(DhikrAudioPlayerState())
    override val state: StateFlow<DhikrAudioPlayerState> = _state.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var progressJob: Job? = null
    private var resolveJob: Job? = null
    private var lastDisplayedAdhkar: List<Dhikr> = emptyList()

    private fun ensurePlayer(): ExoPlayer {
        exoPlayer?.let { return it }
        val player = ExoPlayer.Builder(appContext)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                true
            )
            .build()

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
                val queueSize = _state.value.queue.size
                if (queueSize <= 0) return
                val idx = player.currentMediaItemIndex.coerceIn(0, queueSize - 1)
                _state.update {
                    it.copy(
                        currentIndex = idx,
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
        exoPlayer = player
        return player
    }

    override fun playDisplayedAdhkar(adhkar: List<Dhikr>, startAtDhikrId: Int?) {
        if (adhkar.isEmpty()) return
        lastDisplayedAdhkar = adhkar
        val immediateQueue = DhikrAudioCatalog.buildQueue(adhkar)
        val targetIndex = if (startAtDhikrId != null) {
            immediateQueue.indexOfFirst { it.dhikrId == startAtDhikrId }.coerceAtLeast(0)
        } else {
            0
        }

        val currentQueueIds = _state.value.queue.map { it.dhikrId }
        val newQueueIds = immediateQueue.map { it.dhikrId }
        val needsFullQueueReplace = currentQueueIds != newQueueIds || exoPlayer == null

        _state.update {
            it.copy(
                queue = if (needsFullQueueReplace) immediateQueue else it.queue,
                currentIndex = targetIndex,
                hasStarted = true,
                isBuffering = true,
                hasError = false,
                positionMs = 0L
            )
        }

        if (needsFullQueueReplace) {
            resolveJob?.cancel()
            resolveJob = scope.launch {
                val resolvedQueue = runCatching { trackResolver.resolveTracks(adhkar) }
                    .getOrDefault(immediateQueue)
                val updatedTargetIndex = if (startAtDhikrId != null) {
                    resolvedQueue.indexOfFirst { it.dhikrId == startAtDhikrId }.coerceAtLeast(0)
                } else {
                    _state.value.currentIndex.coerceIn(0, (resolvedQueue.size - 1).coerceAtLeast(0))
                }
                loadQueueIntoPlayer(resolvedQueue, updatedTargetIndex)
            }
        } else {
            val player = ensurePlayer()
            player.seekTo(targetIndex, 0L)
            if (player.playbackState == Player.STATE_IDLE) {
                player.prepare()
            }
            player.play()
        }
    }

    private fun loadQueueIntoPlayer(queue: List<DhikrAudioTrack>, startIndex: Int) {
        if (queue.isEmpty()) return
        val player = ensurePlayer()
        val mediaItems = queue.map { track ->
            MediaItem.Builder()
                .setMediaId("dhikr_${track.dhikrId}")
                .setUri(track.streamUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.titleAr)
                        .build()
                )
                .build()
        }
        player.setMediaItems(mediaItems, startIndex, 0L)
        player.repeatMode = if (_state.value.repeatCurrent) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player.setPlaybackSpeed(_state.value.playbackSpeed)
        player.prepare()
        player.play()

        _state.update {
            it.copy(
                queue = queue,
                currentIndex = startIndex,
                hasStarted = true,
                hasError = false,
                positionMs = 0L
            )
        }
    }

    override fun togglePlayPause(dhikr: Dhikr?, displayedAdhkar: List<Dhikr>) {
        if (dhikr != null) {
            val currentState = _state.value
            val isSameActiveItem = currentState.hasStarted && currentState.activeDhikrId == dhikr.id
            if (!isSameActiveItem) {
                val queueSource = if (displayedAdhkar.isNotEmpty()) displayedAdhkar else listOf(dhikr)
                playDisplayedAdhkar(queueSource, startAtDhikrId = dhikr.id)
                return
            }
        }

        val existing = exoPlayer
        if (existing == null) {
            val source = if (displayedAdhkar.isNotEmpty()) displayedAdhkar else lastDisplayedAdhkar
            if (source.isNotEmpty()) {
                playDisplayedAdhkar(source, startAtDhikrId = dhikr?.id)
            }
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

    override fun pauseIfPlaying() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            }
        }
    }

    override fun nextTrack() {
        val queue = _state.value.queue
        if (queue.isEmpty()) return
        val next = (_state.value.currentIndex + 1) % queue.size
        seekToTrackIndex(next)
    }

    override fun previousTrack() {
        val queue = _state.value.queue
        if (queue.isEmpty()) return
        val prev = if (_state.value.currentIndex - 1 < 0) queue.lastIndex else _state.value.currentIndex - 1
        seekToTrackIndex(prev)
    }

    private fun seekToTrackIndex(index: Int) {
        val queue = _state.value.queue
        if (index !in queue.indices) return
        val player = ensurePlayer()
        _state.update {
            it.copy(
                currentIndex = index,
                hasStarted = true,
                hasError = false,
                positionMs = 0L
            )
        }
        if (player.mediaItemCount != queue.size) {
            loadQueueIntoPlayer(queue, index)
        } else {
            player.seekTo(index, 0L)
            if (player.playbackState == Player.STATE_IDLE) {
                player.prepare()
            }
            player.play()
        }
    }

    override fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceAtLeast(0L)
        exoPlayer?.seekTo(clamped)
        _state.update { it.copy(positionMs = clamped) }
    }

    override fun cyclePlaybackSpeed() {
        val nextSpeed = when (_state.value.playbackSpeed) {
            1.0f -> 1.25f
            1.25f -> 0.75f
            else -> 1.0f
        }
        exoPlayer?.setPlaybackSpeed(nextSpeed)
        _state.update { it.copy(playbackSpeed = nextSpeed) }
    }

    override fun toggleRepeatCurrent() {
        val nextRepeat = !_state.value.repeatCurrent
        exoPlayer?.repeatMode = if (nextRepeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        _state.update { it.copy(repeatCurrent = nextRepeat) }
    }

    override fun retryCurrent() {
        val currentIdx = _state.value.currentIndex
        val queue = _state.value.queue
        if (queue.isEmpty()) return
        val player = ensurePlayer()
        _state.update { it.copy(hasError = false, isBuffering = true) }
        player.seekTo(currentIdx, 0L)
        player.prepare()
        player.play()
    }

    override fun stopAndHide() {
        resolveJob?.cancel()
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
        resolveJob?.cancel()
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
