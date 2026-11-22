package com.pulihgo.app.ui.therapist

import com.pulihgo.app.data.model.Patient
import com.pulihgo.app.data.session.ExerciseSessionRecord
import com.pulihgo.app.data.session.PatientSession
import com.pulihgo.app.data.session.RepetitionMetric
import com.pulihgo.app.data.session.SessionPainLevel
import com.pulihgo.app.data.session.SessionSource
import com.pulihgo.app.data.session.TherapistSessionLookup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TherapistSessionDetailLogicTest {
    private val patient = Patient("P001", "U002", "U001", 24, "RIGHT", "Forearm rotation", "")
    private val exercises = mapOf("EX001" to ExerciseTarget("Forearm Supination", 60.0, 80.0, 10))

    private fun found(
        reps: List<RepetitionMetric>,
        peak: Double,
        source: SessionSource = SessionSource.DEVICE,
        targetRom: Double? = null,
        exerciseId: String = "EX001",
    ) = TherapistSessionLookup.Found(
        patient,
        PatientSession(
            id = "D-U002-1000",
            record = ExerciseSessionRecord(
                patientId = "U002",
                exerciseId = exerciseId,
                startedAtEpochMs = 1_000,
                endedAtEpochMs = 91_000,
                repetitions = reps.size,
                peakRomDeg = peak,
                averageSmoothness = 0.756,
                painLevel = SessionPainLevel.MILD_PAIN,
                repetitionDetails = reps,
            ),
            source = source,
            targetRomDeg = targetRom,
        ),
    )

    @Test fun detailCarriesSessionMetricsAndTargets() {
        val reps = listOf(
            RepetitionMetric(2, 85.0, 3_000, 0.5),
            RepetitionMetric(1, 62.0, 2_500, 0.9),
            RepetitionMetric(3, 40.0, 2_000, null),
        )
        val d = TherapistSessionDetailLogic.build(found(reps, 85.0), "Alya Putri", exercises)
        assertEquals("P001", d.patientId)
        assertEquals("D-U002-1000", d.sessionId)
        assertEquals("Forearm Supination", d.exerciseName)
        assertEquals(90_000L, d.durationMs)
        assertFalse(d.seeded)
        assertEquals(3, d.repetitions)
        assertEquals(10, d.targetReps)
        assertEquals(76, d.smoothnessPercent)
        assertEquals(RomOutcome.CEILING_EXCEEDED, d.romOutcome)
        assertEquals(listOf(1, 2, 3), d.reps.map { it.number })
        assertEquals(
            listOf(RomOutcome.TARGET_HIT, RomOutcome.CEILING_EXCEEDED, RomOutcome.BELOW_TARGET),
            d.reps.map { it.outcome },
        )
        assertEquals(listOf(90, 50, null), d.reps.map { it.smoothnessPercent })
    }

    @Test fun sessionTargetOverridesExerciseTarget() {
        val d = TherapistSessionDetailLogic.build(
            found(listOf(RepetitionMetric(1, 55.0, 2_000)), 55.0, SessionSource.SEEDED, targetRom = 50.0),
            "Alya Putri",
            exercises,
        )
        assertTrue(d.seeded)
        assertEquals(50.0, d.targetRomDeg!!, 0.0)
        assertEquals(RomOutcome.TARGET_HIT, d.romOutcome)
    }

    @Test fun unknownExerciseFallsBackToIdWithoutTargets() {
        val d = TherapistSessionDetailLogic.build(found(emptyList(), 0.0, exerciseId = "EX999"), "Alya Putri", exercises)
        assertEquals("EX999", d.exerciseName)
        assertNull(d.targetReps)
        assertNull(d.romCeilingDeg)
        assertEquals(RomOutcome.UNKNOWN, d.romOutcome)
        assertTrue(d.reps.isEmpty())
    }

    @Test fun romOutcomeBoundaries() {
        assertEquals(RomOutcome.TARGET_HIT, TherapistSessionDetailLogic.romOutcome(60.0, 60.0, 80.0, true))
        assertEquals(RomOutcome.TARGET_HIT, TherapistSessionDetailLogic.romOutcome(80.0, 60.0, 80.0, true))
        assertEquals(RomOutcome.CEILING_EXCEEDED, TherapistSessionDetailLogic.romOutcome(80.1, 60.0, 80.0, true))
        assertEquals(RomOutcome.BELOW_TARGET, TherapistSessionDetailLogic.romOutcome(59.9, 60.0, 80.0, true))
        assertEquals(RomOutcome.UNKNOWN, TherapistSessionDetailLogic.romOutcome(70.0, null, null, true))
        assertEquals(RomOutcome.UNKNOWN, TherapistSessionDetailLogic.romOutcome(70.0, 60.0, 80.0, false))
    }
}
