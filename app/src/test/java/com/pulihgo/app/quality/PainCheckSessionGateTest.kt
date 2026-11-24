package com.pulihgo.app.quality

import com.pulihgo.app.data.session.ExerciseSessionRecord
import com.pulihgo.app.data.session.RepetitionMetric
import com.pulihgo.app.data.session.SessionPainLevel
import com.pulihgo.app.sensor.LiveExerciseSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PainCheckSessionGateTest {
    private fun gate() = PainCheckSessionGate("U002", "EX001")
    private fun snapshot(reps: Int = 2) = LiveExerciseSnapshot(
        repetitions = reps,
        peakRomDeg = if (reps > 0) 74.0 else 0.0,
        averageSmoothness = if (reps > 0) 0.81 else null,
        completedRepetitions = if (reps > 0) listOf(
            RepetitionMetric(1, 71.0, 2200L, 0.8),
            RepetitionMetric(2, 74.0, 2400L, 0.82),
        ) else emptyList(),
    )

    @Test fun noPainRecordsFrozenMeasurementsAndFinishTimestamp() {
        val g = gate()
        assertTrue(g.beginReview(snapshot(), 1000L, 5000L))
        val records = mutableListOf<ExerciseSessionRecord>()
        assertTrue(g.submit(SessionPainLevel.NO_PAIN) { records.add(it); true })
        val rec = records.single()
        assertEquals(1000L, rec.startedAtEpochMs)
        assertEquals(5000L, rec.endedAtEpochMs)
        assertEquals(2, rec.repetitions)
        assertEquals(74.0, rec.peakRomDeg, 0.0)
        assertEquals(2, rec.repetitionDetails.size)
        assertEquals(SessionPainLevel.NO_PAIN, rec.painLevel)
        assertTrue(g.isSaved)
    }

    @Test fun secondSubmissionCannotWriteDuplicate() {
        val g = gate()
        assertTrue(g.beginReview(snapshot(), 1000L, 3000L))
        var writes = 0
        assertTrue(g.submit(SessionPainLevel.MILD_PAIN) { writes++; true })
        assertFalse(g.submit(SessionPainLevel.MILD_PAIN) { writes++; true })
        assertFalse(g.beginReview(snapshot(), 1000L, 4000L))
        assertEquals(1, writes)
    }

    @Test fun failedCommitCanRetryWithExactlySameSessionIdentityAndValues() {
        val g = gate()
        assertTrue(g.beginReview(snapshot(), 1000L, 3000L))
        var first: ExerciseSessionRecord? = null
        assertFalse(g.submit(SessionPainLevel.MILD_PAIN) { first = it; false })
        assertFalse(g.isSaved)
        var second: ExerciseSessionRecord? = null
        assertTrue(g.submit(SessionPainLevel.MILD_PAIN) { second = it; true })
        assertEquals(first, second)
    }

    @Test fun thrownPersistenceErrorIsRetryable() {
        val g = gate()
        g.beginReview(snapshot(), 1000L, 3000L)
        assertFalse(g.submit(SessionPainLevel.NO_PAIN) { throw IllegalStateException("storage") })
        assertTrue(g.submit(SessionPainLevel.NO_PAIN) { true })
    }

    @Test fun returningToSessionDiscardsOldSnapshotAndAllowsFreshFinish() {
        val g = gate()
        g.beginReview(snapshot(), 1000L, 3000L)
        assertTrue(g.isFrozen)
        assertTrue(g.cancelReview())
        assertFalse(g.isFrozen)
        assertTrue(g.beginReview(snapshot(reps = 0), 1000L, 9000L))
        var recorded: ExerciseSessionRecord? = null
        assertTrue(g.submit(SessionPainLevel.NO_PAIN) { recorded = it; true })
        assertEquals(0, recorded!!.repetitions)
        assertEquals(9000L, recorded!!.endedAtEpochMs)
    }

    @Test fun emergencyPainStopWorksBeforeAnyMovementAndCannotBeRelabeled() {
        val g = gate()
        assertTrue(g.beginPainStop(snapshot(reps = 0), 0L, 2500L))
        assertTrue(g.isStopPending)
        assertFalse(g.cancelReview())
        assertFalse(g.submit(SessionPainLevel.NO_PAIN) { true })
        var recorded: ExerciseSessionRecord? = null
        assertTrue(g.submit(SessionPainLevel.STOPPED_DUE_TO_PAIN) { recorded = it; true })
        assertEquals(2500L, recorded!!.startedAtEpochMs)
        assertEquals(2500L, recorded!!.endedAtEpochMs)
        assertEquals(0, recorded!!.repetitions)
        assertTrue(recorded!!.stoppedForPain)
    }

    @Test fun emergencyPainStopRetryKeepsStopFlag() {
        val g = gate()
        g.beginPainStop(snapshot(), 1000L, 2500L)
        assertFalse(g.submit(SessionPainLevel.STOPPED_DUE_TO_PAIN) { false })
        assertFalse(g.submit(SessionPainLevel.MILD_PAIN) { true })
        assertTrue(g.submit(SessionPainLevel.STOPPED_DUE_TO_PAIN) { true })
    }

    @Test fun invalidIdentityAndClockAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { PainCheckSessionGate("", "EX001") }
        assertThrows(IllegalArgumentException::class.java) { PainCheckSessionGate("U002", "") }
        assertFalse(gate().beginReview(snapshot(), 1000L, 0L))
    }
}
