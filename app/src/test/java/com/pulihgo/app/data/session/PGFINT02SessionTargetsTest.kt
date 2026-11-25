package com.pulihgo.app.data.session

import com.pulihgo.app.data.cloud.CloudSessionMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PGFINT02SessionTargetsTest {
    private fun record() = ExerciseSessionRecord(
        patientId = "U002", exerciseId = "EX001", startedAtEpochMs = 1_000L, endedAtEpochMs = 4_000L,
        repetitions = 8, peakRomDeg = 65.0, averageSmoothness = 0.8, painLevel = SessionPainLevel.NO_PAIN,
        targetRomDeg = 60.0, targetReps = 8,
    )

    @Test fun sessionTargetsSurviveLocalJsonAndCloudMapping() {
        val original = record()
        val saved = ExerciseSessionRecordJson.decode(ExerciseSessionRecordJson.encode(listOf(original))).single()
        assertEquals(original, saved)
        val cloud = CloudSessionMapper.toRow(saved, "P001")
        assertEquals(60.0, cloud.getDouble("target_rom_deg"), 0.0)
        assertEquals(8, cloud.getInt("target_reps"))
    }

    @Test fun deviceHistoryUsesSessionSnapshotNotCurrentCatalog() {
        val session = PatientSessionHistory.fromDevice(listOf(record())).single()
        assertEquals(60.0, session.targetRomDeg!!, 0.0)
        assertEquals(8, session.targetReps)
    }

    @Test fun legacyRecordHasNoInventedTargets() {
        val legacy = record().copy(targetRomDeg = null, targetReps = null)
        val decoded = ExerciseSessionRecordJson.decode(ExerciseSessionRecordJson.encode(listOf(legacy))).single()
        assertNull(decoded.targetRomDeg)
        assertNull(decoded.targetReps)
        val row = CloudSessionMapper.toRow(decoded, "P001")
        assertFalse(row.has("target_rom_deg"))
        assertFalse(row.has("target_reps"))
    }

    @Test fun impossibleTargetValuesRejected() {
        assertTrue(runCatching { record().copy(targetRomDeg = 200.0) }.isFailure)
        assertTrue(runCatching { record().copy(targetReps = -1) }.isFailure)
    }
}
