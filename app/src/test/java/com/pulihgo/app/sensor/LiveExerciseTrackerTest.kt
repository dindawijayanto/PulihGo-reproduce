package com.pulihgo.app.sensor

import com.pulihgo.app.quality.RomSafetyLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveExerciseTrackerTest {

    private fun tracker() = LiveExerciseTracker(targetRomDeg = 70.0, ceilingDeg = 80.0)

    private fun LiveExerciseTracker.feed(vararg angles: Double, startMs: Long = 0L): LiveExerciseSnapshot {
        var snap = snapshot
        angles.forEachIndexed { i, a -> snap = onAngle(a, startMs + i * 20L) }
        return snap
    }

    private fun rotation(peak: Double, steps: Int = 20): DoubleArray {
        val up = (0..steps).map { peak * it / steps }
        return (up + up.reversed().drop(1)).toDoubleArray()
    }

    @Test
    fun initialSnapshot_isBaselineWithNoData() {
        val s = tracker().snapshot
        assertEquals(LivePhase.BASELINE, s.phase)
        assertEquals(0, s.repetitions)
        assertEquals(0.0, s.peakRomDeg, 0.0)
        assertFalse(s.hasSamples)
    }

    @Test
    fun realTimeAngle_isAbsoluteCalibratedRoll() {
        val t = tracker()
        assertEquals(35.0, t.onAngle(35.0, 0).currentRomDeg, 1e-9)
        assertEquals(42.0, t.onAngle(-42.0, 20).currentRomDeg, 1e-9)
        assertTrue(t.snapshot.hasSamples)
    }

    @Test
    fun belowTarget_staysInBaselinePhase() {
        val s = tracker().feed(0.0, 20.0, 50.0, 69.9)
        assertEquals(LivePhase.BASELINE, s.phase)
        assertEquals(RomSafetyLevel.WITHIN_LIMIT, s.safetyLevel)
    }

    @Test
    fun reachingTarget_entersTargetHit_andStaysWhileReturning() {
        val t = tracker()
        assertEquals(LivePhase.TARGET_HIT, t.feed(0.0, 40.0, 70.0).phase)
        assertEquals(LivePhase.TARGET_HIT, t.onAngle(40.0, 100).phase)
        assertEquals(LivePhase.TARGET_HIT, t.onAngle(10.0, 120).phase)
        val back = t.onAngle(2.0, 140)
        assertEquals(LivePhase.BASELINE, back.phase)
        assertEquals(1, back.repetitions)
    }

    @Test
    fun approachingCeiling_reportsWarningLevelButKeepsTargetHit() {
        val s = tracker().feed(0.0, 40.0, 72.0)
        assertEquals(LivePhase.TARGET_HIT, s.phase)
        assertEquals(RomSafetyLevel.APPROACHING_LIMIT, s.safetyLevel)
    }

    @Test
    fun ceiling_overridesTargetHit_thenRecoversToTargetHit() {
        val t = tracker()
        val over = t.feed(0.0, 40.0, 75.0, 80.0)
        assertEquals(LivePhase.CEILING_EXCEEDED, over.phase)
        assertEquals(RomSafetyLevel.LIMIT_EXCEEDED, over.safetyLevel)
        assertEquals(LivePhase.CEILING_EXCEEDED, t.onAngle(-95.0, 100).phase)
        assertEquals(LivePhase.TARGET_HIT, t.onAngle(60.0, 120).phase)
    }

    @Test
    fun peakAngle_isSessionMaximum() {
        val t = tracker()
        t.feed(*rotation(73.0))
        val s = t.feed(*rotation(65.0), startMs = 10_000)
        assertEquals(73.0, s.peakRomDeg, 1e-9)
        assertEquals(0.0, s.currentRomDeg, 1e-9)
    }

    @Test
    fun repetitions_countFullRotations_withPerRepMetrics() {
        val t = tracker()
        t.feed(*rotation(72.0))
        val s = t.feed(*rotation(70.0), startMs = 10_000)
        assertEquals(2, s.repetitions)
        assertEquals(2, s.completedRepetitions.size)
        assertEquals(listOf(1, 2), s.completedRepetitions.map { it.repetitionNumber })
        assertEquals(72.0, s.completedRepetitions[0].peakRomDeg, 1e-9)
        assertNotNull(s.averageSmoothness)
    }

    @Test
    fun halfMovement_withoutReturn_isNotCounted() {
        val s = tracker().feed(0.0, 20.0, 40.0, 60.0, 72.0, 50.0, 30.0)
        assertEquals(0, s.repetitions)
        assertEquals(LivePhase.TARGET_HIT, s.phase)
    }

    @Test
    fun nonFiniteAngle_isIgnored() {
        val t = tracker()
        val before = t.onAngle(30.0, 0)
        assertEquals(before, t.onAngle(Double.NaN, 20))
        assertEquals(before, t.onAngle(Double.POSITIVE_INFINITY, 40))
    }

    @Test
    fun reset_clearsSession() {
        val t = tracker()
        t.feed(*rotation(72.0))
        t.reset()
        val s = t.snapshot
        assertEquals(0, s.repetitions)
        assertEquals(0.0, s.peakRomDeg, 0.0)
        assertEquals(LivePhase.BASELINE, s.phase)
        assertTrue(s.completedRepetitions.isEmpty())
        assertEquals(1, t.feed(*rotation(72.0), startMs = 50_000).completedRepetitions.first().repetitionNumber)
    }

    @Test
    fun invalidConfig_isRejected() {
        assertThrows(IllegalArgumentException::class.java) { LiveExerciseTracker(0.0, 80.0) }
        assertThrows(IllegalArgumentException::class.java) { LiveExerciseTracker(70.0, 70.0) }
        assertThrows(IllegalArgumentException::class.java) { LiveExerciseTracker(Double.NaN, 80.0) }
    }
}
