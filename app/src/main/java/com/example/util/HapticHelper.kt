package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.model.TasbihVibrationPattern

object HapticHelper {

    fun performTap(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        runCatching {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    vibrator.vibrate(VibrationEffect.createOneShot(18L, 80))
                }
                else -> {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(18L)
                }
            }
        }
    }

    fun performMilestone(context: Context) {
        performTargetPattern(context, TasbihVibrationPattern.GENTLE_PULSE)
    }

    fun performTargetPattern(context: Context, pattern: TasbihVibrationPattern) {
        if (pattern == TasbihVibrationPattern.NONE || pattern.patternMillis.isEmpty()) return
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(
                        pattern.patternMillis,
                        pattern.amplitudes,
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern.patternMillis, -1)
            }
        }
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
