package com.pulihgo.app.integration

import com.pulihgo.app.data.cloud.CloudSessionMapper
import com.pulihgo.app.data.exercise.Exercise
import com.pulihgo.app.data.session.ExerciseSessionRecord
import com.pulihgo.app.data.session.ExerciseSessionRecordJson
import com.pulihgo.app.data.session.PatientSessionHistory
import com.pulihgo.app.data.session.SessionPainLevel
import com.pulihgo.app.data.therapy.PatientTherapyPlan
import com.pulihgo.app.data.therapy.PatientTherapyPlanCodec
import com.pulihgo.app.quality.PainCheckSessionGate
import com.pulihgo.app.quality.RomSafetyConfig
import com.pulihgo.app.quality.RomSafetyEvaluator
import com.pulihgo.app.quality.RomSafetyLevel
import com.pulihgo.app.sensor.LiveExerciseSnapshot
import com.pulihgo.app.sensor.LiveExerciseTracker
import com.pulihgo.app.sensor.LivePhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PGF-INT-05: cross-module JVM regression tests.
 * These use deterministic synthetic sensor samples; they are NOT device, RLS or
 * clinical safety validation. Manual Android/cloud QA is documented separately.
 */
class PGFINT05FlowTest {
    private fun catalog() = Exercise(
        id = "EX001", name = "Forearm Rotation", description = "QA catalog",
        targetRomDeg = 70.0, targetReps = 10, romCeilingDeg = 80.0,
        requiresPhoneSensor = true, instructions = emptyList(), safetyNotes = emptyList(),
    )

    private fun remoteJson(patient: String = "P001", active: Boolean = true) =
        """[{"patient_id":"$patient","exercise_id":"EX001","target_rom_deg":65,"rom_ceiling_deg":75,"target_reps":8,"sets_per_session":2,"sessions_per_week":4,"is_active":$active,"notes":"Demo only"}]"""

    private fun loadedPlan(): PatientTherapyPlan = PatientTherapyPlanCodec.decode(remoteJson(), "P001").single()

    @Test fun therapistPrescriptionReachesTrackerFrozenPainGateLocalJsonAndCloudRow() {
        val p = loadedPlan()
        val exercise = p.applyTo(catalog())
        assertEquals(65.0, exercise.targetRomDeg, 0.0)
        assertEquals(75.0, exercise.romCeilingDeg, 0.0)
        assertEquals(8, exercise.targetReps)

        val tracker = LiveExerciseTracker(exercise.targetRomDeg, exercise.romCeilingDeg)
        tracker.onAngle(0.0, 1_000L)
        val near = tracker.onAngle(68.0, 2_000L)
        assertEquals(RomSafetyLevel.APPROACHING_LIMIT, near.safetyLevel)
        assertEquals(LivePhase.TARGET_HIT, near.phase)
        tracker.onAngle(0.0, 4_000L)
        val atFinish = tracker.snapshot

        val gate = PainCheckSessionGate("U002", exercise.id, exercise.targetRomDeg, exercise.targetReps)
        assertTrue(gate.beginReview(atFinish, 1_000L, 9_000L))
        var local: ExerciseSessionRecord? = null
        assertTrue(gate.submit(SessionPainLevel.NO_PAIN) { local = it; true })
        val saved = ExerciseSessionRecordJson.decode(ExerciseSessionRecordJson.encode(listOf(local!!))).single()
        assertEquals(local, saved)
        assertEquals(atFinish.repetitions, saved.repetitions)
        assertEquals(65.0, saved.targetRomDeg!!, 0.0)
        assertEquals(8, saved.targetReps)
        val row = CloudSessionMapper.toRow(saved, "P001")
        assertEquals("P001", row.getString("patient_id"))
        assertEquals("EX001", row.getString("exercise_id"))
        assertEquals(65.0, row.getDouble("target_rom_deg"), 0.0)
        assertEquals(8, row.getInt("target_reps"))
        assertEquals(saved.repetitions, row.getInt("repetitions"))
        assertEquals("NO_PAIN", row.getString("pain_level"))
        assertEquals("DEVICE", row.getString("source"))
    }

    @Test fun romWarningsUseTherapistCeilingIncludingNegativeAngles() {
        val p = loadedPlan()
        val e = RomSafetyEvaluator(RomSafetyConfig(p.romCeilingDeg))
        assertEquals(RomSafetyLevel.WITHIN_LIMIT, e.evaluate(64.9)!!.level)
        assertEquals(RomSafetyLevel.APPROACHING_LIMIT, e.evaluate(65.0)!!.level)
        assertEquals(RomSafetyLevel.APPROACHING_LIMIT, e.evaluate(-74.9)!!.level)
        assertEquals(RomSafetyLevel.LIMIT_EXCEEDED, e.evaluate(75.0)!!.level)
        assertEquals(RomSafetyLevel.LIMIT_EXCEEDED, e.evaluate(-76.0)!!.level)
    }

    @Test fun pausedStatusIsPreservedInOfflineCacheWithoutClinicalNotes() {
        val paused = PatientTherapyPlanCodec.decode(remoteJson(active = false), "P001").single()
        val raw = PatientTherapyPlanCodec.encodeCache(listOf(paused))
        val cached = PatientTherapyPlanCodec.decode(raw, "P001").single()
        assertFalse(cached.isActive)
        assertEquals(65.0, cached.targetRomDeg, 0.0)
        assertEquals(8, cached.targetReps)
        assertFalse(raw.contains("Demo only"))
        assertTrue(cached.notes.isEmpty())
    }

    @Test fun mixedPatientResponseIsRejectedAsOneInvalidBatch() {
        val p1 = remoteJson().removePrefix("[").removeSuffix("]")
        val p2 = remoteJson("P002").removePrefix("[").removeSuffix("]")
        assertThrows(IllegalArgumentException::class.java) {
            PatientTherapyPlanCodec.decode("[$p1,$p2]", "P001")
        }
    }

    @Test fun invalidCeilingAndDuplicateRemotePlansFailClosed() {
        assertThrows(IllegalArgumentException::class.java) {
            PatientTherapyPlanCodec.decode(remoteJson().replace("\"rom_ceiling_deg\":75", "\"rom_ceiling_deg\":60"), "P001")
        }
        val single = remoteJson().removePrefix("[").removeSuffix("]")
        assertThrows(IllegalArgumentException::class.java) {
            PatientTherapyPlanCodec.decode("[$single,$single]", "P001")
        }
    }

    @Test fun successfulEmptyResponseHasNoPatientSpecificPrescription() {
        assertTrue(PatientTherapyPlanCodec.decode("[]", "P002").isEmpty())
        assertEquals(70.0, catalog().targetRomDeg, 0.0)
    }

    @Test fun wrongExerciseCannotBorrowAnotherExercisePrescription() {
        assertThrows(IllegalArgumentException::class.java) {
            loadedPlan().applyTo(catalog().copy(id = "EX999"))
        }
    }

    @Test fun painStopBeforeAnyRepetitionMustRemainPainStopInCloud() {
        val gate = PainCheckSessionGate("U002", "EX001", 65.0, 8)
        val zeroReps = LiveExerciseSnapshot(repetitions = 0, peakRomDeg = 0.0)
        assertTrue(gate.beginPainStop(zeroReps, 0L, 8_000L))
        assertFalse(gate.cancelReview())
        assertFalse(gate.submit(SessionPainLevel.NO_PAIN) { true })
        var result: ExerciseSessionRecord? = null
        assertTrue(gate.submit(SessionPainLevel.STOPPED_DUE_TO_PAIN) { result = it; true })
        val row = CloudSessionMapper.toRow(result!!, "P001")
        assertEquals("STOPPED_DUE_TO_PAIN", row.getString("pain_level"))
        assertEquals(0, row.getInt("repetitions"))
        assertEquals(8, row.getInt("target_reps"))
        assertFalse(gate.submit(SessionPainLevel.STOPPED_DUE_TO_PAIN) { true })
    }

    @Test fun localFailureRetainsExactSessionForRetryAndAvoidsDuplicateWrite() {
        val gate = PainCheckSessionGate("U002", "EX001", 65.0, 8)
        assertTrue(gate.beginReview(LiveExerciseSnapshot(repetitions = 2, peakRomDeg = 64.0), 100L, 2_000L))
        var original: ExerciseSessionRecord? = null
        assertFalse(gate.submit(SessionPainLevel.MILD_PAIN) { original = it; false })
        var retry: ExerciseSessionRecord? = null
        assertTrue(gate.submit(SessionPainLevel.MILD_PAIN) { retry = it; true })
        assertEquals(original, retry)
        assertEquals(CloudSessionMapper.sessionId(original!!), CloudSessionMapper.sessionId(retry!!))
        assertFalse(gate.submit(SessionPainLevel.MILD_PAIN) { true })
    }

    @Test fun prescribedEightAndActuallyMeasuredSevenRemainSeparate() {
        val r = ExerciseSessionRecord(
            patientId = "U002", exerciseId = "EX001", startedAtEpochMs = 1_000L,
            endedAtEpochMs = 10_000L, repetitions = 7, peakRomDeg = 67.0,
            averageSmoothness = 0.73, painLevel = SessionPainLevel.NO_PAIN,
            targetRomDeg = 65.0, targetReps = 8,
        )
        val row = CloudSessionMapper.toRow(r, "P001")
        assertEquals(8, row.getInt("target_reps"))
        assertEquals(7, row.getInt("repetitions"))
        assertEquals(65.0, PatientSessionHistory.fromDevice(listOf(r)).single().targetRomDeg!!, 0.0)
    }

    @Test fun historicalSessionWithoutTargetsDoesNotInventValues() {
        val legacy = ExerciseSessionRecord(
            patientId = "U002", exerciseId = "EX001", startedAtEpochMs = 1_000L,
            endedAtEpochMs = 2_000L, repetitions = 0, peakRomDeg = 0.0,
            averageSmoothness = null, painLevel = SessionPainLevel.NO_PAIN,
        )
        val raw = ExerciseSessionRecordJson.encode(listOf(legacy))
        val read = ExerciseSessionRecordJson.decode(raw).single()
        assertNull(read.targetRomDeg)
        assertNull(read.targetReps)
        assertFalse(CloudSessionMapper.toRow(read, "P001").has("target_rom_deg"))
        assertFalse(CloudSessionMapper.toRow(read, "P001").has("target_reps"))
    }
}
