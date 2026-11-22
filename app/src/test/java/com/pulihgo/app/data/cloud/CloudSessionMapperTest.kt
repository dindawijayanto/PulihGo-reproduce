package com.pulihgo.app.data.cloud

import com.pulihgo.app.data.session.ExerciseSessionRecord
import com.pulihgo.app.data.session.RepetitionMetric
import com.pulihgo.app.data.session.SessionPainLevel
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudSessionMapperTest {
    private fun record(start: Long, smoothness: Double? = 0.75, reps: List<RepetitionMetric> = emptyList()) = ExerciseSessionRecord(
        patientId = "U002",
        exerciseId = "EX001",
        startedAtEpochMs = start,
        endedAtEpochMs = start + 90_000,
        repetitions = reps.size,
        peakRomDeg = 64.5,
        averageSmoothness = smoothness,
        painLevel = SessionPainLevel.MILD_PAIN,
        repetitionDetails = reps,
    )

    @Test fun sessionIdMatchesLocalHistoryId() {
        assertEquals("D-U002-1731900000000", CloudSessionMapper.sessionId(record(1_731_900_000_000)))
    }

    @Test fun rowUsesCloudPatientIdAndIsoTimes() {
        val row = CloudSessionMapper.toRow(
            record(1_731_900_000_000, reps = listOf(RepetitionMetric(1, 64.5, 2_000, 0.8), RepetitionMetric(2, 60.0, 2_100, null))),
            "P001",
        )
        assertEquals("D-U002-1731900000000", row.getString("id"))
        assertEquals("P001", row.getString("patient_id"))
        assertEquals("EX001", row.getString("exercise_id"))
        assertEquals("2024-11-18T03:20:00Z", row.getString("started_at"))
        assertEquals("2024-11-18T03:21:30Z", row.getString("ended_at"))
        assertEquals(2, row.getInt("repetitions"))
        assertEquals(64.5, row.getDouble("peak_rom_deg"), 0.0)
        assertEquals(0.75, row.getDouble("average_smoothness"), 0.0)
        assertEquals("MILD_PAIN", row.getString("pain_level"))
        assertEquals("DEVICE", row.getString("source"))
        val reps = row.getJSONArray("repetition_details")
        assertEquals(2, reps.length())
        assertEquals(1, reps.getJSONObject(0).getInt("repetitionNumber"))
        assertEquals(0.8, reps.getJSONObject(0).getDouble("smoothnessScore"), 0.0)
        assertTrue(reps.getJSONObject(1).isNull("smoothnessScore"))
    }

    @Test fun missingSmoothnessIsJsonNull() {
        val row = CloudSessionMapper.toRow(record(1_000, smoothness = null), "P001")
        assertTrue(row.isNull("average_smoothness"))
        assertFalse(row.has("target_reps"))
        assertEquals(JSONObject.NULL, row.get("average_smoothness"))
    }

    @Test fun pendingSkipsSyncedAndDuplicateSessions() {
        val a = record(1_000)
        val b = record(2_000)
        val pending = CloudSessionMapper.pending(listOf(a, b, b), setOf("D-U002-1000"))
        assertEquals(listOf("D-U002-2000"), pending.map(CloudSessionMapper::sessionId))
        assertTrue(CloudSessionMapper.pending(listOf(a), setOf("D-U002-1000")).isEmpty())
    }

    @Test fun configRequiresHttpsUrlAndKey() {
        assertTrue(SupabaseConfig("https://x.supabase.co", "sb_publishable_x").isConfigured)
        assertFalse(SupabaseConfig("", "sb_publishable_x").isConfigured)
        assertFalse(SupabaseConfig("https://x.supabase.co", "").isConfigured)
    }
}
