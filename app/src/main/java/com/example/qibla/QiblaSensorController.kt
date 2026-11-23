package com.example.qibla

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf

data class CompassReading(
    val azimuthDegrees: Float = 0f,
    val accuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
    val hasSensors: Boolean = true,
    val isCalibrating: Boolean = false
)

interface QiblaSensorSource {
    fun observeCompass(latitude: Double?, longitude: Double?): Flow<CompassReading>
}

object NoOpQiblaSensorSource : QiblaSensorSource {
    override fun observeCompass(latitude: Double?, longitude: Double?): Flow<CompassReading> =
        flowOf(CompassReading(azimuthDegrees = 0f, hasSensors = true, isCalibrating = false))
}

class AndroidQiblaSensorSource(
    context: Context
) : QiblaSensorSource {

    private val appContext = context.applicationContext

    override fun observeCompass(latitude: Double?, longitude: Double?): Flow<CompassReading> =
        callbackFlow {
            val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

            if (sensorManager == null || accelerometer == null || magnetometer == null) {
                trySend(CompassReading(azimuthDegrees = 0f, hasSensors = false, isCalibrating = false))
                awaitClose { }
                return@callbackFlow
            }

            val declination = if (latitude != null && longitude != null) {
                runCatching {
                    GeomagneticField(
                        latitude.toFloat(),
                        longitude.toFloat(),
                        0f,
                        System.currentTimeMillis()
                    ).declination
                }.getOrDefault(0f)
            } else {
                0f
            }

            val gravity = FloatArray(3)
            val geomagnetic = FloatArray(3)
            var hasGravity = false
            var hasMagnet = false
            var magnetAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
            val alpha = 0.18f

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    when (event.sensor.type) {
                        Sensor.TYPE_ACCELEROMETER -> {
                            if (!hasGravity) {
                                event.values.copyInto(gravity, endIndex = 3)
                                hasGravity = true
                            } else {
                                for (i in 0..2) {
                                    gravity[i] = gravity[i] + alpha * (event.values[i] - gravity[i])
                                }
                            }
                        }

                        Sensor.TYPE_MAGNETIC_FIELD -> {
                            if (!hasMagnet) {
                                event.values.copyInto(geomagnetic, endIndex = 3)
                                hasMagnet = true
                            } else {
                                for (i in 0..2) {
                                    geomagnetic[i] = geomagnetic[i] + alpha * (event.values[i] - geomagnetic[i])
                                }
                            }
                        }
                    }

                    if (hasGravity && hasMagnet) {
                        val azimuth = QiblaCalculator.computeAzimuthFromSensors(
                            gravity = gravity,
                            geomagnetic = geomagnetic,
                            declinationDegrees = declination
                        )
                        if (azimuth != null) {
                            val needsCalibration = magnetAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE ||
                                magnetAccuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW
                            trySend(
                                CompassReading(
                                    azimuthDegrees = azimuth,
                                    accuracy = magnetAccuracy,
                                    hasSensors = true,
                                    isCalibrating = needsCalibration
                                )
                            )
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                    if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                        magnetAccuracy = accuracy
                    }
                }
            }

            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)

            awaitClose {
                sensorManager.unregisterListener(listener)
            }
        }
}
