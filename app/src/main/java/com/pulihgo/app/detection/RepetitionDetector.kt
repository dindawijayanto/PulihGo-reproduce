package com.pulihgo.app.detection

import kotlin.math.abs

data class AngleSample(
    val timestampMs: Long,
    val angleDeg: Double,
)

data class Repetition(
    val repetitionNumber: Int,
    val peakRomDeg: Double,
    val durationMs: Long,
    val samples: List<AngleSample>,
)

enum class RepetitionState{
    IDLE,
    ARMED,
}

class RepetitionDetector(targetRomDeg: Double? = null,) {
    private val enterThreshold: Double =
        targetRomDeg?.let { it * 0.45 } ?: 40.0
    private val exitThreshold: Double =
        targetRomDeg?.let { it * 0.10 } ?: 15.0

    init {
        require(targetRomDeg == null || targetRomDeg > 0.0) {
            "targetRomDeg must be greater than 0"
        }
        require(exitThreshold < enterThreshold) {
            "Exit threshold must be lower than enter threshold"
        }
    }

    private var currentState = RepetitionState.IDLE
    private val currentSamples = mutableListOf<AngleSample>()

    private var peakRomDeg = 0.0
    private var startTimeMs = 0L
    private var repCounter = 0

    val count: Int
        get() = repCounter

    val state: RepetitionState
        get() = currentState

    fun processAngle(
        angleDeg: Double,
        timestampMs: Long,
    ): Repetition? {
        if (!angleDeg.isFinite()) return null

        val absAngle = abs(angleDeg)

        return when (currentState) {
            RepetitionState.IDLE -> {
                if (absAngle > enterThreshold) {
                    currentState = RepetitionState.ARMED
                    startTimeMs = timestampMs
                    peakRomDeg = absAngle

                    currentSamples.clear()
                    currentSamples.add(
                        AngleSample(timestampMs, angleDeg)
                    )
                }

                null
            }

            RepetitionState.ARMED -> {
                currentSamples.add(
                    AngleSample(timestampMs, angleDeg)
                )

                if (absAngle > peakRomDeg) {
                    peakRomDeg = absAngle
                }

                if (absAngle < exitThreshold) {
                    repCounter++

                    val completedRep = Repetition(
                        repetitionNumber = repCounter,
                        peakRomDeg = peakRomDeg,
                        durationMs = (timestampMs - startTimeMs)
                            .coerceAtLeast(0L),
                        samples = currentSamples.toList(),
                    )

                    currentState = RepetitionState.IDLE
                    currentSamples.clear()
                    peakRomDeg = 0.0
                    startTimeMs = 0L

                    completedRep
                } else {
                    null
                }
            }
        }
    }

    fun reset() {
        currentState = RepetitionState.IDLE
        currentSamples.clear()
        peakRomDeg = 0.0
        startTimeMs = 0L
        repCounter = 0
    }
}
