package com.pulihgo.app.data.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseSessionRecordTest {
    private fun record(painLevel: SessionPainLevel) = ExerciseSessionRecord(
        patientId = "PATIENT_01", exerciseId = "EX001", startedAtEpochMs = 1000,
        endedAtEpochMs = 3000, repetitions = 6, peakRomDeg = 72.2,
        averageSmoothness = 0.63, painLevel = painLevel,
    )

    @Test fun allThreeOutcomesRoundTripWithGenuineSessionNumbers() {
        val input = SessionPainLevel.entries.map(::record)
        val output = ExerciseSessionRecordJson.decode(ExerciseSessionRecordJson.encode(input))
        assertEquals(input, output)
        assertEquals(3, output.size)
        assertTrue(output.last().stoppedForPain)
        assertFalse(output.first().stoppedForPain)
    }

    @Test fun zeroRepetitionsAndMissingSmoothnessStayDistinctFromZeroScore() {
        val input = record(SessionPainLevel.NO_PAIN).copy(repetitions = 0, averageSmoothness = null)
        val decoded = ExerciseSessionRecordJson.decode(ExerciseSessionRecordJson.encode(listOf(input))).single()
        assertEquals(0, decoded.repetitions)
        assertNull(decoded.averageSmoothness)
        assertEquals(72.2, decoded.peakRomDeg, 1e-9)
    }

    @Test fun malformedRecordDoesNotBecomeFictitiousSafeSession() {
        val bad = """[{"patientId":"P1","exerciseId":"EX001","startedAtEpochMs":10,"endedAtEpochMs":20,"repetitions":1,"peakRomDeg":70,"averageSmoothness":0.9,"painLevel":"NOT_A_LEVEL"}]"""
        assertTrue(ExerciseSessionRecordJson.decode(bad).isEmpty())
    }
}
