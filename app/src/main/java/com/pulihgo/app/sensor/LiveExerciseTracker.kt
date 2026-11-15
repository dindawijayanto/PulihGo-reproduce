package com.pulihgo.app.sensor

import com.pulihgo.app.data.session.RepetitionMetric
import com.pulihgo.app.detection.RepetitionDetector
import com.pulihgo.app.detection.RepetitionState
import com.pulihgo.app.quality.RomSafetyConfig
import com.pulihgo.app.quality.RomSafetyEvaluator
import com.pulihgo.app.quality.RomSafetyLevel
import com.pulihgo.app.quality.SessionSmoothnessTracker
import kotlin.math.abs

enum class LivePhase {
    BASELINE,
    TARGET_HIT,
    CEILING_EXCEEDED,
}

data class LiveExerciseSnapshot(
    val currentRomDeg: Double = 0.0,
    val peakRomDeg: Double = 0.0,
    val repetitions: Int = 0,
    val phase: LivePhase = LivePhase.BASELINE,
    val safetyLevel: RomSafetyLevel = RomSafetyLevel.WITHIN_LIMIT,
    val completedRepetitions: List<RepetitionMetric> = emptyList(),
    val averageSmoothness: Double? = null,
    val hasSamples: Boolean = false,
)

class LiveExerciseTracker(
    val targetRomDeg: Double,
    val ceilingDeg: Double,
) {
    init {
        require(targetRomDeg.isFinite() && targetRomDeg > 0.0) { "targetRomDeg must be > 0" }
        require(ceilingDeg.isFinite() && ceilingDeg > targetRomDeg) {
            "ceilingDeg must be greater than targetRomDeg"
        }
    }

    private val detector = RepetitionDetector(targetRomDeg = targetRomDeg)
    private val safety = RomSafetyEvaluator(RomSafetyConfig(ceilingDeg))
    private val smoothness = SessionSmoothnessTracker()

    private var targetLatched = false

    var snapshot: LiveExerciseSnapshot = LiveExerciseSnapshot()
        private set

    fun onAngle(angleDeg: Double, elapsedMs: Long): LiveExerciseSnapshot {
        if (!angleDeg.isFinite()) return snapshot

        val rom = abs(angleDeg)
        if (rom >= targetRomDeg) targetLatched = true

        val completed = detector.processAngle(angleDeg, elapsedMs)
        if (detector.state == RepetitionState.IDLE) targetLatched = false

        var reps = snapshot.completedRepetitions
        if (completed != null) {
            val score = smoothness.record(completed)
            reps = reps + RepetitionMetric(
                repetitionNumber = completed.repetitionNumber,
                peakRomDeg = completed.peakRomDeg,
                durationMs = completed.durationMs,
                smoothnessScore = score?.value,
            )
        }

        val level = safety.evaluate(angleDeg)?.level ?: RomSafetyLevel.WITHIN_LIMIT
        val phase = when {
            level == RomSafetyLevel.LIMIT_EXCEEDED -> LivePhase.CEILING_EXCEEDED
            targetLatched -> LivePhase.TARGET_HIT
            else -> LivePhase.BASELINE
        }

        snapshot = LiveExerciseSnapshot(
            currentRomDeg = rom,
            peakRomDeg = maxOf(snapshot.peakRomDeg, rom),
            repetitions = detector.count,
            phase = phase,
            safetyLevel = level,
            completedRepetitions = reps,
            averageSmoothness = smoothness.snapshot().averageScore,
            hasSamples = true,
        )
        return snapshot
    }

    fun reset() {
        detector.reset()
        smoothness.reset()
        targetLatched = false
        snapshot = LiveExerciseSnapshot()
    }
}
