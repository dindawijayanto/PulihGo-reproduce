package com.pulihgo.app.quality

import com.pulihgo.app.detection.AngleSample
import com.pulihgo.app.detection.Repetition
import kotlin.math.abs

/** Heuristic 0..1, not a clinical measurement or a safety clearance. */
data class SmoothnessScore(
    val repetitionNumber: Int,
    val value: Double,
    val pathEfficiency: Double,
    val speedConsistency: Double,
)

/** Immutable snapshot intended for future Session Summary / report integration. */
data class SessionSmoothnessSummary(val byRepetition: List<SmoothnessScore>) {
    val evaluatedRepetitions: Int get() = byRepetition.size
    val averageScore: Double? get() = byRepetition.takeIf { it.isNotEmpty() }?.map { it.value }?.average()
}

/**
 * Quantifies within-repetition movement consistency from calibrated angle samples.
 *
 * Uses absolute ROM so both supination and pronation (signed angles) are supported.
 * Path efficiency penalizes direction reversals / oscillations, while time-weighted
 * speed consistency penalizes uneven movement. Both factors stay in [0,1].
 *
 * Invalid, stationary, too-short, or non-monotonic readings return null rather than
 * misrepresenting an unreliable score as 0 (which implies known poor movement).
 */
object MovementSmoothnessEvaluator {
    fun evaluate(repetition: Repetition): SmoothnessScore? =
        evaluate(repetition.repetitionNumber, repetition.samples)

    fun evaluate(repetitionNumber: Int, samples: List<AngleSample>): SmoothnessScore? {
        if (samples.size < 5 || samples.any { !it.angleDeg.isFinite() }) return null
        if (samples.zipWithNext().any { (a, b) -> b.timestampMs <= a.timestampMs }) return null

        val magnitudes = samples.map { abs(it.angleDeg) }
        val peak = magnitudes.maxOrNull() ?: return null
        val expectedTravel = (peak - magnitudes.first()) + (peak - magnitudes.last())
        if (expectedTravel < 1.0) return null // insufficient range to score movement

        var actualTravel = 0.0
        val segmentVelocities = ArrayList<Pair<Double, Double>>(samples.size - 1)
        for (i in 1 until samples.size) {
            val dtMs = (samples[i].timestampMs - samples[i - 1].timestampMs).toDouble()
            val distance = abs(magnitudes[i] - magnitudes[i - 1])
            actualTravel += distance
            segmentVelocities += (distance / dtMs) to dtMs
        }
        if (actualTravel < 1.0 || !actualTravel.isFinite()) return null

        val totalDurationMs = (samples.last().timestampMs - samples.first().timestampMs).toDouble()
        if (totalDurationMs <= 0.0) return null
        val meanSpeed = actualTravel / totalDurationMs
        val deviation = segmentVelocities.sumOf { (speed, duration) ->
            abs(speed - meanSpeed) * duration
        }
        val pathEfficiency = (expectedTravel / actualTravel).coerceIn(0.0, 1.0)
        val speedConsistency = (1.0 - deviation / (2.0 * actualTravel)).coerceIn(0.0, 1.0)
        val score = (pathEfficiency * speedConsistency).coerceIn(0.0, 1.0)
        if (!score.isFinite()) return null
        return SmoothnessScore(repetitionNumber, score, pathEfficiency, speedConsistency)
    }
}

/** Holds per-repetition scores for the current session, without touching detector state. */
class SessionSmoothnessTracker {
    private val scores = mutableListOf<SmoothnessScore>()

    fun record(repetition: Repetition): SmoothnessScore? {
        val score = MovementSmoothnessEvaluator.evaluate(repetition) ?: return null
        val previous = scores.indexOfFirst { it.repetitionNumber == score.repetitionNumber }
        if (previous >= 0) scores[previous] = score else scores.add(score)
        return score
    }

    fun snapshot(): SessionSmoothnessSummary = SessionSmoothnessSummary(scores.toList())

    fun reset() { scores.clear() }
}
