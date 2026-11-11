package com.pulihgo.app.sensor

/** Kondisi pembacaan sensor yang ditampilkan ke pengguna (PGF-SEN-01). */
sealed interface SensorHealth {
    /** Ponsel tidak punya rotation vector maupun accelerometer. Latihan tidak bisa dilakukan. */
    data object Unavailable : SensorHealth

    /** Sensor ada, tapi gagal didaftarkan (misalnya dibatasi sistem). */
    data object RegistrationFailed : SensorHealth

    /** Sudah terdaftar, menunggu sampel pertama (masih dalam batas waktu startup). */
    data object WaitingFirstSample : SensorHealth

    /** Melewati batas waktu startup tanpa satu pun sampel. */
    data object NoData : SensorHealth

    /** Sampel pernah masuk lalu berhenti lebih lama dari batas stall. */
    data class Stalled(val millisSinceLastSample: Long) : SensorHealth

    /** Data mengalir normal. */
    data class Streaming(val sampleRateHz: Double) : SensorHealth
}

/**
 * Logika murni untuk menilai apakah sensor benar-benar mengirim data.
 * Semua waktu dalam milidetik dari jam monotonic (`SystemClock.elapsedRealtime()`),
 * diberikan oleh pemanggil sehingga mudah diuji.
 */
class SensorHealthMonitor(
    private val startupTimeoutMs: Long = 2_000,
    private val stallTimeoutMs: Long = 1_000,
    private val rateWindow: Int = 25,
) {
    private var startedAtMs: Long? = null
    private var unavailable = false
    private var registrationFailed = false
    private val sampleTimes = ArrayDeque<Long>()

    fun markUnavailable() { unavailable = true }

    fun markRegistrationFailed() { registrationFailed = true }

    fun onStarted(nowMs: Long) {
        startedAtMs = nowMs
        sampleTimes.clear()
    }

    fun onSample(nowMs: Long) {
        sampleTimes.addLast(nowMs)
        while (sampleTimes.size > rateWindow) sampleTimes.removeFirst()
    }

    fun evaluate(nowMs: Long): SensorHealth {
        if (unavailable) return SensorHealth.Unavailable
        if (registrationFailed) return SensorHealth.RegistrationFailed
        val last = sampleTimes.lastOrNull()
        if (last == null) {
            val started = startedAtMs ?: return SensorHealth.WaitingFirstSample
            return if (nowMs - started > startupTimeoutMs) SensorHealth.NoData else SensorHealth.WaitingFirstSample
        }
        val sinceLast = nowMs - last
        if (sinceLast > stallTimeoutMs) return SensorHealth.Stalled(sinceLast)
        return SensorHealth.Streaming(sampleRateHz())
    }

    /** Rata-rata frekuensi sampel di jendela terakhir. Bernilai 0 jika sampel belum cukup. */
    fun sampleRateHz(): Double {
        if (sampleTimes.size < 2) return 0.0
        val spanMs = sampleTimes.last() - sampleTimes.first()
        if (spanMs <= 0) return 0.0
        return (sampleTimes.size - 1) * 1000.0 / spanMs
    }
}
