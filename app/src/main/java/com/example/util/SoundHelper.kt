package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.example.data.model.TasbihTargetSoundEffect
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object SoundHelper {
    private const val SAMPLE_RATE = 22050

    /**
     * Plays a calm, gentle two-tone chime (C5 -> G5 harmonic chord) with soft attack
     * and exponential decay when the user completes their daily Tasbeeh target.
     */
    fun playGentleChime(context: Context) {
        playTargetSoundEffect(context, TasbihTargetSoundEffect.GENTLE_CHIME)
    }

    /**
     * Plays a calm, distinct four-note ascending chime (G4 -> B4 -> D5 -> G5)
     * when a timed Dhikr / Tasbeeh session completes.
     */
    fun playSessionEndAlert(context: Context) {
        playTargetSoundEffect(context, TasbihTargetSoundEffect.SACRED_HARP)
    }

    fun playTargetSoundEffect(context: Context, effect: TasbihTargetSoundEffect) {
        if (effect == TasbihTargetSoundEffect.NONE) return
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager?.ringerMode == AudioManager.RINGER_MODE_SILENT) return

        val (pcm, durationMs) = when (effect) {
            TasbihTargetSoundEffect.GENTLE_CHIME -> generateChimePcm() to 900L
            TasbihTargetSoundEffect.SACRED_HARP -> generateSessionEndPcm() to 1450L
            TasbihTargetSoundEffect.CRYSTAL_BELL -> generateCrystalBellPcm() to 1050L
            TasbihTargetSoundEffect.SOFT_TONE -> generateSoftTonePcm() to 750L
            TasbihTargetSoundEffect.NONE -> return
        }
        Thread {
            playPcm(pcm, sleepMillis = durationMs)
        }.start()
    }

    internal fun generateChimePcm(): ShortArray {
        val durationSec = 0.85
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(numSamples)
        val noteTwoStartSample = (SAMPLE_RATE * 0.16).toInt()

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val env1 = envelope(t, attackSec = 0.015, decayRate = 4.2)
            val tone1 = (sin(2.0 * PI * 523.25 * t) * 0.65 +
                sin(2.0 * PI * 1046.50 * t) * 0.2) * env1

            val tone2 = if (i >= noteTwoStartSample) {
                val t2 = (i - noteTwoStartSample).toDouble() / SAMPLE_RATE
                val env2 = envelope(t2, attackSec = 0.018, decayRate = 3.6)
                (sin(2.0 * PI * 783.99 * t2) * 0.7 +
                    sin(2.0 * PI * 1567.98 * t2) * 0.18) * env2
            } else {
                0.0
            }

            // Gentle master volume (~28% peak) for a calm, unobtrusive chime
            val mixed = ((tone1 + tone2) * 0.28).coerceIn(-1.0, 1.0)
            samples[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
        }
        return samples
    }

    internal fun generateSessionEndPcm(): ShortArray {
        val durationSec = 1.35
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(numSamples)
        val note2Start = (SAMPLE_RATE * 0.18).toInt()
        val note3Start = (SAMPLE_RATE * 0.36).toInt()
        val note4Start = (SAMPLE_RATE * 0.56).toInt()

        for (i in 0 until numSamples) {
            val t1 = i.toDouble() / SAMPLE_RATE
            val n1 = sin(2.0 * PI * 392.00 * t1) * envelope(t1, 0.015, 4.5) // G4

            val n2 = if (i >= note2Start) {
                val t = (i - note2Start).toDouble() / SAMPLE_RATE
                sin(2.0 * PI * 493.88 * t) * envelope(t, 0.015, 4.2) // B4
            } else 0.0

            val n3 = if (i >= note3Start) {
                val t = (i - note3Start).toDouble() / SAMPLE_RATE
                sin(2.0 * PI * 587.33 * t) * envelope(t, 0.015, 3.8) // D5
            } else 0.0

            val n4 = if (i >= note4Start) {
                val t = (i - note4Start).toDouble() / SAMPLE_RATE
                (sin(2.0 * PI * 783.99 * t) * 0.75 + sin(2.0 * PI * 1567.98 * t) * 0.2) *
                    envelope(t, 0.018, 2.8) // G5
            } else 0.0

            val mixed = ((n1 + n2 + n3 + n4) * 0.26).coerceIn(-1.0, 1.0)
            samples[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
        }
        return samples
    }

    internal fun generateCrystalBellPcm(): ShortArray {
        val durationSec = 1.0
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(numSamples)
        val shimmerStart = (SAMPLE_RATE * 0.14).toInt()

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val bell1 = (sin(2.0 * PI * 659.25 * t) * 0.68 +
                sin(2.0 * PI * 1318.51 * t) * 0.24) * envelope(t, 0.010, 3.4) // E5

            val bell2 = if (i >= shimmerStart) {
                val t2 = (i - shimmerStart).toDouble() / SAMPLE_RATE
                (sin(2.0 * PI * 987.77 * t2) * 0.65 +
                    sin(2.0 * PI * 1975.53 * t2) * 0.2) * envelope(t2, 0.012, 3.1) // B5
            } else 0.0

            val mixed = ((bell1 + bell2) * 0.27).coerceIn(-1.0, 1.0)
            samples[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
        }
        return samples
    }

    internal fun generateSoftTonePcm(): ShortArray {
        val durationSec = 0.70
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val tone = (sin(2.0 * PI * 440.00 * t) * 0.78 +
                sin(2.0 * PI * 880.00 * t) * 0.16) * envelope(t, 0.020, 4.0) // A4 warm
            val mixed = (tone * 0.28).coerceIn(-1.0, 1.0)
            samples[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
        }
        return samples
    }

    private fun envelope(t: Double, attackSec: Double, decayRate: Double): Double {
        val attack = if (t < attackSec) t / attackSec else 1.0
        return attack * exp(-decayRate * t)
    }

    private fun playPcm(pcm: ShortArray, sleepMillis: Long = 900L) {
        var track: AudioTrack? = null
        try {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            val bufferSizeBytes = pcm.size * 2

            track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSizeBytes)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcm, 0, pcm.size)
            track.play()
            Thread.sleep(sleepMillis)
        } catch (_: Throwable) {
            // Gracefully ignore on devices or test runners without audio output
        } finally {
            runCatching {
                track?.stop()
                track?.release()
            }
        }
    }
}
