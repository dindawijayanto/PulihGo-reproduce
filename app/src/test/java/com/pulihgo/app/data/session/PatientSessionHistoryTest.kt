package com.pulihgo.app.data.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PatientSessionHistoryTest {
    private val seedJson = """
        {"sessions":[
          {"id":"S001","patientId":"P001","exerciseId":"EX001",
           "startedAt":"2026-11-08T09:00:00+07:00","endedAt":"2026-11-08T09:03:00+07:00",
           "totalReps":3,"targetReps":10,"peakRomDeg":56.0,"targetRomDeg":70.0,"averageSmoothness":0.82,
           "painReported":false,"stopReason":null,"status":"COMPLETED",
           "repetitions":[{"index":1,"peakRomDeg":56.0,"durationMs":2500,"samples":[0,56,0]}]},
          {"id":"S003","patientId":"P002","exerciseId":"EX001",
           "startedAt":"2026-11-07T10:00:00+07:00","endedAt":"2026-11-07T10:02:00+07:00",
           "totalReps":2,"targetReps":10,"peakRomDeg":42.0,"targetRomDeg":70.0,"averageSmoothness":0.71,
           "painReported":true,"stopReason":"PAIN_REPORTED","status":"STOPPED","repetitions":[]},
          {"id":"S999","patientId":"P404","exerciseId":"EX001",
           "startedAt":"2026-11-07T10:00:00+07:00","endedAt":"2026-11-07T10:02:00+07:00",
           "totalReps":1,"peakRomDeg":10.0,"painReported":false,"status":"COMPLETED"}
        ]}
    """.trimIndent()
    private val ids = mapOf("P001" to "U002", "P002" to "U003")

    @Test
    fun seededRowsAreKeyedByUserIdAndUnknownPatientsDropped() {
        val sessions = SeededSessionParser.parse(seedJson, ids)
        assertEquals(listOf("S001", "S003"), sessions.map { it.id })
        assertEquals("U002", sessions[0].record.patientId)
        assertTrue(sessions.all { it.source == SessionSource.SEEDED })
        assertEquals(180_000L, sessions[0].durationMs)
        assertEquals(70.0, sessions[0].targetRomDeg!!, 0.0)
        assertEquals(1, sessions[0].record.repetitionDetails.size)
        assertNull(sessions[0].record.repetitionDetails[0].smoothnessScore)
    }

    @Test
    fun painFlagsMapToSessionPainLevels() {
        val sessions = SeededSessionParser.parse(seedJson, ids)
        assertEquals(SessionPainLevel.NO_PAIN, sessions[0].record.painLevel)
        assertEquals(SessionPainLevel.STOPPED_DUE_TO_PAIN, sessions[1].record.painLevel)
    }

    @Test
    fun mergeKeepsOnlyTheUsersSessionsNewestFirst() {
        val seeded = SeededSessionParser.parse(seedJson, ids)
        val device = listOf(
            ExerciseSessionRecord("U002", "EX001", 2_000_000_000_000, 2_000_000_060_000, 5, 61.0, 0.9, SessionPainLevel.NO_PAIN),
            ExerciseSessionRecord("U003", "EX001", 2_000_000_000_000, 2_000_000_060_000, 5, 61.0, 0.9, SessionPainLevel.NO_PAIN),
        )
        val merged = PatientSessionHistory.merge(seeded, device, "U002")
        assertEquals(2, merged.size)
        assertEquals(SessionSource.DEVICE, merged[0].source)
        assertEquals("S001", merged[1].id)
        assertTrue(merged.all { it.record.patientId == "U002" })
    }

    @Test
    fun malformedSeedYieldsEmptyList() {
        assertEquals(emptyList<PatientSession>(), SeededSessionParser.parse("{}", ids))
    }
}
