package com.pulihgo.app.quality

import com.pulihgo.app.detection.AngleSample
import com.pulihgo.app.detection.Repetition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MovementSmoothnessTest {
    private fun rep(number: Int, angles: List<Double>, timestamps: List<Long>? = null): Repetition {
        val times = timestamps ?: angles.indices.map { it * 20L }
        val samples = angles.mapIndexed { i, value -> AngleSample(times[i], value) }
        return Repetition(number, angles.maxOf { kotlin.math.abs(it) }, times.last() - times.first(), samples)
    }

    @Test fun controlledMonotonicOutAndBackIsNearOne() {
        val result = MovementSmoothnessEvaluator.evaluate(
            rep(1, listOf(30.0, 40.0, 50.0, 60.0, 70.0, 60.0, 50.0, 40.0, 30.0, 20.0, 10.0)),
        )!!
        assertEquals(1.0, result.value, 1e-9)
        assertEquals(1.0, result.pathEfficiency, 1e-9)
    }

    @Test fun noisyReversalsHaveLowerSmoothnessThanControlledMovement() {
        val controlled = MovementSmoothnessEvaluator.evaluate(
            rep(1, listOf(30.0, 40.0, 50.0, 60.0, 70.0, 60.0, 50.0, 40.0, 30.0, 20.0, 10.0)),
        )!!
        val erratic = MovementSmoothnessEvaluator.evaluate(
            rep(2, listOf(30.0, 58.0, 35.0, 72.0, 33.0, 66.0, 29.0, 49.0, 20.0, 14.0, 5.0)),
        )!!
        assertTrue(erratic.value < controlled.value)
        assertTrue(erratic.value in 0.0..1.0)
        assertTrue(erratic.pathEfficiency < 1.0)
    }

    @Test fun supportsNegativeAnglesAndNonuniformTimestamps() {
        val score = MovementSmoothnessEvaluator.evaluate(rep(
            3,
            listOf(-30.0, -40.0, -65.0, -70.0, -55.0, -35.0, -10.0),
            listOf(0L, 20L, 50L, 75L, 100L, 130L, 160L),
        ))!!
        assertTrue(score.value in 0.0..1.0)
    }

    @Test fun rejectsInvalidIncompleteOrStationarySamples() {
        assertNull(MovementSmoothnessEvaluator.evaluate(rep(1, listOf(30.0, 40.0, 10.0))))
        assertNull(MovementSmoothnessEvaluator.evaluate(rep(1, listOf(30.0, 30.0, 30.0, 30.0, 30.0))))
        assertNull(MovementSmoothnessEvaluator.evaluate(rep(1, listOf(30.0, Double.NaN, 60.0, 40.0, 10.0))))
        assertNull(MovementSmoothnessEvaluator.evaluate(rep(
            1, listOf(30.0, 45.0, 60.0, 40.0, 10.0), listOf(0, 20, 20, 60, 80),
        )))
    }

    @Test fun trackerExposesImmutableSessionSummaryAndResets() {
        val tracker = SessionSmoothnessTracker()
        val sample = listOf(30.0, 40.0, 50.0, 60.0, 70.0, 60.0, 50.0, 40.0, 30.0, 20.0, 10.0)
        assertNull(tracker.snapshot().averageScore)
        tracker.record(rep(1, sample))
        tracker.record(rep(2, sample.map { -it }))
        tracker.record(rep(2, sample.map { -it })) // dedupe same repetition number
        assertEquals(2, tracker.snapshot().evaluatedRepetitions)
        assertEquals(1.0, tracker.snapshot().averageScore!!, 1e-9)
        tracker.reset()
        assertTrue(tracker.snapshot().byRepetition.isEmpty())
        assertNull(tracker.snapshot().averageScore)
    }
}
