package com.pulihgo.app.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class SensorSource(val sensorType: Int, val label: String, val supportsYaw: Boolean) {
    GAME_ROTATION_VECTOR(Sensor.TYPE_GAME_ROTATION_VECTOR, "Game rotation vector (gyro + accel)", true),

    ROTATION_VECTOR(Sensor.TYPE_ROTATION_VECTOR, "Rotation vector (gyro + accel + mag)", true),

    ACCELEROMETER(Sensor.TYPE_ACCELEROMETER, "Accelerometer saja (mode terbatas)", false),
}

enum class SensorAccuracy { UNRELIABLE, LOW, MEDIUM, HIGH, UNKNOWN;

    companion object {
        fun fromAndroid(value: Int): SensorAccuracy = when (value) {
            SensorManager.SENSOR_STATUS_UNRELIABLE, SensorManager.SENSOR_STATUS_NO_CONTACT -> UNRELIABLE
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> LOW
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> MEDIUM
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> HIGH
            else -> UNKNOWN
        }
    }
}

data class AngleReading(val angles: Angles, val elapsedMs: Long)

sealed interface SensorUpdate {
    data class Reading(val reading: AngleReading) : SensorUpdate
    data class Accuracy(val accuracy: SensorAccuracy) : SensorUpdate
    data object RegistrationFailed : SensorUpdate
}

class DeviceAngleSensor(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager?

    val source: SensorSource? = sensorManager?.let { manager ->
        SensorSource.entries.firstOrNull { manager.getDefaultSensor(it.sensorType) != null }
    }

    fun availability(): Map<SensorSource, Boolean> =
        SensorSource.entries.associateWith { sensorManager?.getDefaultSensor(it.sensorType) != null }

    fun updates(samplingPeriodUs: Int = SAMPLING_PERIOD_50_HZ_US): Flow<SensorUpdate> = callbackFlow {
        val manager = sensorManager
        val selected = source
        val sensor = selected?.let { manager?.getDefaultSensor(it.sensorType) }
        if (manager == null || sensor == null) {
            trySend(SensorUpdate.RegistrationFailed)
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val angles = when (selected) {
                    SensorSource.ACCELEROMETER -> OrientationMath.fromGravity(event.values)
                    else -> OrientationMath.fromRotationVector(event.values)
                } ?: return
                if (angles.pitch.isNaN() || angles.roll.isNaN() || angles.yaw.isNaN()) return
                trySend(SensorUpdate.Reading(AngleReading(angles, SystemClock.elapsedRealtime())))
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                trySend(SensorUpdate.Accuracy(SensorAccuracy.fromAndroid(accuracy)))
            }
        }

        val registered = manager.registerListener(listener, sensor, samplingPeriodUs)
        if (!registered) {
            trySend(SensorUpdate.RegistrationFailed)
            close()
            return@callbackFlow
        }
        awaitClose { manager.unregisterListener(listener) }
    }

    companion object {
        const val SAMPLING_PERIOD_50_HZ_US = 20_000
    }
}
