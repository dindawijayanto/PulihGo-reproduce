package com.pulihgo.app.sensor

sealed interface SensorHealth {
    data object Unavailable : SensorHealth

    data object RegistrationFailed : SensorHealth

    data object WaitingFirstSample : SensorHealth

    data object NoData : SensorHealth

    data class Stalled(val millisSinceLastSample: Long) : SensorHealth

    data class Streaming(val sampleRateHz: Double) : SensorHealth
}

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

    fun sampleRateHz(): Double {
        if (sampleTimes.size < 2) return 0.0
        val spanMs = sampleTimes.last() - sampleTimes.first()
        if (spanMs <= 0) return 0.0
        return (sampleTimes.size - 1) * 1000.0 / spanMs
    }
}
