package com.example.qibla

import android.hardware.SensorManager
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

object QiblaCalculator {
    const val KAABA_LATITUDE = 21.422487
    const val KAABA_LONGITUDE = 39.826206
    private const val EARTH_RADIUS_KM = 6371.0
    const val ALIGNMENT_TOLERANCE_DEGREES = 5f

    /**
     * Computes great-circle initial bearing from the given coordinates to the Kaaba in Mecca,
     * normalized in degrees clockwise from True North [0, 360).
     */
    fun calculateQiblaBearingDegrees(latitude: Double, longitude: Double): Float {
        val phi1 = Math.toRadians(latitude)
        val phi2 = Math.toRadians(KAABA_LATITUDE)
        val deltaLambda = Math.toRadians(KAABA_LONGITUDE - longitude)

        val y = sin(deltaLambda)
        val x = cos(phi1) * tan(phi2) - sin(phi1) * cos(deltaLambda)
        val rawDeg = Math.toDegrees(atan2(y, x)).toFloat()
        return normalizeAngle360(rawDeg)
    }

    /**
     * Computes Haversine great-circle distance in kilometers from the given coordinates to the Kaaba.
     */
    fun calculateDistanceToKaabaKm(latitude: Double, longitude: Double): Double {
        val dLat = Math.toRadians(KAABA_LATITUDE - latitude)
        val dLon = Math.toRadians(KAABA_LONGITUDE - longitude)
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(KAABA_LATITUDE)

        val a = sin(dLat / 2.0) * sin(dLat / 2.0) +
            cos(lat1) * cos(lat2) * sin(dLon / 2.0) * sin(dLon / 2.0)
        val c = 2.0 * atan2(sqrt(a), sqrt((1.0 - a).coerceAtLeast(0.0)))
        return EARTH_RADIUS_KM * c
    }

    /**
     * Computes azimuth clockwise from North [0, 360) using Android's SensorManager rotation matrix.
     */
    fun computeAzimuthFromSensors(
        gravity: FloatArray,
        geomagnetic: FloatArray,
        declinationDegrees: Float = 0f
    ): Float? {
        if (gravity.size < 3 || geomagnetic.size < 3) return null
        val r = FloatArray(9)
        val i = FloatArray(9)
        val ok = SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)
        if (!ok) return null
        val orientation = FloatArray(3)
        SensorManager.getOrientation(r, orientation)
        val azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat() + declinationDegrees
        return normalizeAngle360(azimuthDeg)
    }

    fun normalizeAngle360(degrees: Float): Float {
        val mod = degrees % 360f
        return if (mod < 0f) mod + 360f else mod
    }

    /**
     * Returns shortest signed delta in (-180, 180] from [fromDegrees] to [toDegrees].
     * Positive means turn clockwise (right); negative means turn counter-clockwise (left).
     */
    fun shortestSignedAngleDelta(fromDegrees: Float, toDegrees: Float): Float {
        var delta = normalizeAngle360(toDegrees) - normalizeAngle360(fromDegrees)
        while (delta > 180f) delta -= 360f
        while (delta <= -180f) delta += 360f
        return delta
    }

    fun isAlignedWithQibla(
        azimuthDegrees: Float,
        qiblaBearingDegrees: Float,
        toleranceDegrees: Float = ALIGNMENT_TOLERANCE_DEGREES
    ): Boolean {
        return abs(shortestSignedAngleDelta(azimuthDegrees, qiblaBearingDegrees)) <= toleranceDegrees
    }

    fun cardinalDirectionCode(degrees: Float): String {
        val normalized = normalizeAngle360(degrees)
        val index = ((normalized + 22.5f) / 45f).toInt() % 8
        return when (index) {
            0 -> "N"
            1 -> "NE"
            2 -> "E"
            3 -> "SE"
            4 -> "S"
            5 -> "SW"
            6 -> "W"
            else -> "NW"
        }
    }
}
