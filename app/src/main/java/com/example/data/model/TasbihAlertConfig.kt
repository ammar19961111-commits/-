package com.example.data.model

enum class TasbihVibrationPattern(
    val patternMillis: LongArray,
    val amplitudes: IntArray
) {
    GENTLE_PULSE(
        patternMillis = longArrayOf(0L, 45L, 50L, 75L),
        amplitudes = intArrayOf(0, 160, 0, 230)
    ),
    HEARTBEAT(
        patternMillis = longArrayOf(0L, 60L, 70L, 110L, 90L, 60L),
        amplitudes = intArrayOf(0, 170, 0, 255, 0, 160)
    ),
    LONG_HOLD(
        patternMillis = longArrayOf(0L, 260L),
        amplitudes = intArrayOf(0, 230)
    ),
    CRESCENDO(
        patternMillis = longArrayOf(0L, 30L, 45L, 50L, 45L, 80L, 50L, 130L),
        amplitudes = intArrayOf(0, 90, 0, 140, 0, 195, 0, 255)
    ),
    NONE(
        patternMillis = longArrayOf(),
        amplitudes = intArrayOf()
    );

    companion object {
        fun fromName(name: String?): TasbihVibrationPattern =
            entries.find { it.name == name } ?: GENTLE_PULSE
    }
}

enum class TasbihTargetSoundEffect {
    GENTLE_CHIME,
    SACRED_HARP,
    CRYSTAL_BELL,
    SOFT_TONE,
    NONE;

    companion object {
        fun fromName(name: String?): TasbihTargetSoundEffect =
            entries.find { it.name == name } ?: GENTLE_CHIME
    }
}
