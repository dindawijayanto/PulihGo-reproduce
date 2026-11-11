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

/** Sumber sensor, diurutkan dari yang paling diutamakan. */
enum class SensorSource(val sensorType: Int, val label: String, val supportsYaw: Boolean) {
    /** Hasil fusion gyro + accelerometer oleh OS, tanpa magnetometer (tidak terganggu logam). Padanan DeviceMotion di repo asli. */
    GAME_ROTATION_VECTOR(Sensor.TYPE_GAME_ROTATION_VECTOR, "Game rotation vector (gyro + accel)", true),

    /** Fusion gyro + accel + magnetometer. Yaw bisa terganggu oleh benda logam. */
    ROTATION_VECTOR(Sensor.TYPE_ROTATION_VECTOR, "Rotation vector (gyro + accel + mag)", true),

    /** Mode terbatas: hanya kemiringan dari gravitasi, tanpa yaw, dan berisik saat bergerak. */
    ACCELEROMETER(Sensor.TYPE_ACCELEROMETER, "Accelerometer saja (mode terbatas)", false),
}

/** Akurasi yang dilaporkan sistem melalui `onAccuracyChanged`. */
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

/** Satu pembacaan orientasi. [elapsedMs] memakai `SystemClock.elapsedRealtime()`. */
data class AngleReading(val angles: Angles, val elapsedMs: Long)

sealed interface SensorUpdate {
    data class Reading(val reading: AngleReading) : SensorUpdate
    data class Accuracy(val accuracy: SensorAccuracy) : SensorUpdate
    /** Pendaftaran listener ke SensorManager gagal. */
    data object RegistrationFailed : SensorUpdate
}

/**
 * Membaca orientasi ponsel secara langsung (PGF-SEN-01).
 *
 * Android TIDAK memerlukan runtime permission untuk sensor gerak di 50 Hz
 * (izin HIGH_SAMPLING_RATE_SENSORS baru dibutuhkan di atas 200 Hz). Karena itu,
 * kondisi gagal yang benar-benar ditangani adalah: sensor tidak ada, pendaftaran
 * gagal, data tidak masuk, atau akurasi rendah.
 */
class DeviceAngleSensor(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager?

    /** Sumber terbaik yang tersedia di perangkat ini, atau null jika tidak ada sama sekali. */
    val source: SensorSource? = sensorManager?.let { manager ->
        SensorSource.entries.firstOrNull { manager.getDefaultSensor(it.sensorType) != null }
    }

    /** Daftar semua sumber dan ketersediaannya di perangkat, untuk layar diagnostik. */
    fun availability(): Map<SensorSource, Boolean> =
        SensorSource.entries.associateWith { sensorManager?.getDefaultSensor(it.sensorType) != null }

    /**
     * Flow pembacaan sudut. Listener didaftarkan saat flow mulai di-collect dan
     * dilepas otomatis saat collect berhenti (layar ditutup atau aplikasi ke background).
     */
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
        /** 20 000 µs = 50 Hz, sama dengan updateMs = 50 di repo asli. */
        const val SAMPLING_PERIOD_50_HZ_US = 20_000
    }
}
