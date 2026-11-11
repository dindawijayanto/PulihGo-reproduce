package com.pulihgo.app.sensor

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object AngleNormalizer {
    fun wrapDeg180(deg: Double): Double {
        val mod360 = ((deg % 360.0) + 360.0) % 360.0
        return if (mod360 > 180.0) mod360 - 360.0 else mod360
    }

    fun angularDistance(a: Double, b: Double): Double = abs(wrapDeg180(a - b))

    fun circularMeanDeg(values: List<Double>): Double {
        require(values.isNotEmpty()) { "values must not be empty" }
        var sumSin = 0.0
        var sumCos = 0.0
        for (v in values) {
            val rad = Math.toRadians(v)
            sumSin += sin(rad)
            sumCos += cos(rad)
        }
        return wrapDeg180(Math.toDegrees(atan2(sumSin, sumCos)))
    }

    fun relative(raw: Angles, offset: Angles): Angles = Angles(
        pitch = wrapDeg180(raw.pitch - offset.pitch),
        roll = wrapDeg180(raw.roll - offset.roll),
        yaw = wrapDeg180(raw.yaw - offset.yaw),
    )
}

sealed interface CalibrationState {
    data object NotCalibrated : CalibrationState
    data class Collecting(val collected: Int, val required: Int) : CalibrationState {
        val progress: Float get() = collected.toFloat() / required
    }
    data class Calibrated(val offset: Angles) : CalibrationState
    data class FailedUnstable(val spreadDeg: Double) : CalibrationState
}

class AngleCalibrator(
    private val requiredSamples: Int = 25,
    private val maxSpreadDeg: Double = 4.0,
    initialOffset: Angles? = null,
) {
    init {
        require(requiredSamples > 0) { "requiredSamples must be > 0" }
    }

    private val buffer = mutableListOf<Angles>()

    private var previousOffset: Angles? = initialOffset

    var state: CalibrationState =
        initialOffset?.let { CalibrationState.Calibrated(it) } ?: CalibrationState.NotCalibrated
        private set

    val offset: Angles? get() = (state as? CalibrationState.Calibrated)?.offset

    fun start() {
        buffer.clear()
        state = CalibrationState.Collecting(0, requiredSamples)
    }

    fun onSample(raw: Angles): CalibrationState {
        if (state !is CalibrationState.Collecting) return state
        buffer += raw
        if (buffer.size < requiredSamples) {
            state = CalibrationState.Collecting(buffer.size, requiredSamples)
            return state
        }

        val mean = Angles(
            pitch = AngleNormalizer.circularMeanDeg(buffer.map { it.pitch }),
            roll = AngleNormalizer.circularMeanDeg(buffer.map { it.roll }),
            yaw = AngleNormalizer.circularMeanDeg(buffer.map { it.yaw }),
        )
        val spread = buffer.maxOf { s ->
            maxOf(
                AngleNormalizer.angularDistance(s.pitch, mean.pitch),
                AngleNormalizer.angularDistance(s.roll, mean.roll),
                AngleNormalizer.angularDistance(s.yaw, mean.yaw),
            )
        }
        buffer.clear()
        state = if (spread > maxSpreadDeg) {
            CalibrationState.FailedUnstable(spread)
        } else {
            previousOffset = mean
            CalibrationState.Calibrated(mean)
        }
        return state
    }

    fun relative(raw: Angles): Angles = AngleNormalizer.relative(raw, offset ?: previousOffset ?: Angles.ZERO)

    fun reset() {
        buffer.clear()
        previousOffset = null
        state = CalibrationState.NotCalibrated
    }
}
