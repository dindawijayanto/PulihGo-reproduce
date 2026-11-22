package com.pulihgo.app.data.session

import com.pulihgo.app.data.model.Patient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PatientSessionRepositoryTest {
    private val alya = Patient("P001", "U002", "U001", 24, "RIGHT", "Forearm rotation", "")
    private val bima = Patient("P002", "U003", "U001", 31, "LEFT", "Forearm rotation", "")
    private val other = Patient("P003", "U004", "U009", 40, "RIGHT", "Forearm rotation", "")

    private fun record(userId: String, start: Long, reps: Int = 5) = ExerciseSessionRecord(
        patientId = userId,
        exerciseId = "EX001",
        startedAtEpochMs = start,
        endedAtEpochMs = start + 60_000,
        repetitions = reps,
        peakRomDeg = 50.0,
        averageSmoothness = 0.8,
        painLevel = SessionPainLevel.NO_PAIN,
    )

    private val seeded = listOf(
        PatientSession("S-001", record("U002", 1_000_000), SessionSource.SEEDED),
        PatientSession("S-002", record("U003", 2_000_000), SessionSource.SEEDED),
        PatientSession("S-003", record("U004", 3_000_000), SessionSource.SEEDED),
    )
    private val device = mutableMapOf(
        "U002" to listOf(record("U002", 5_000_000, reps = 8)),
    )
    private val repo = PatientSessionRepository(listOf(alya, bima, other), seeded) { device[it].orEmpty() }

    @Test fun patientSessionsMergeSeededAndDeviceNewestFirst() {
        val ids = repo.sessionsForPatient("P001").map { it.id }
        assertEquals(listOf("D-U002-5000000", "S-001"), ids)
        assertEquals(ids, repo.sessionsForUser("U002").map { it.id })
    }

    @Test fun sessionIsFoundByPatientIdAndSessionId() {
        assertEquals(8, repo.session("P001", "D-U002-5000000")?.record?.repetitions)
        assertEquals(SessionSource.SEEDED, repo.session("P001", "S-001")?.source)
    }

    @Test fun sessionOfAnotherPatientIsNotReturned() {
        assertNull(repo.session("P001", "S-002"))
        assertNull(repo.session("P404", "S-001"))
        assertTrue(repo.sessionsForPatient("P404").isEmpty())
    }

    @Test fun newDeviceSessionIsVisibleWithoutRebuildingRepository() {
        device["U003"] = listOf(record("U003", 9_000_000))
        assertEquals("D-U003-9000000", repo.sessionsForPatient("P002").first().id)
    }

    @Test fun therapistSeesOnlyAssignedPatients() {
        assertEquals(listOf("P001", "P002"), repo.patientsOf("U001").map { it.id })
        assertEquals(setOf("U002", "U003"), repo.cohortSessions("U001").keys)
        assertEquals(2, repo.sessionsForTherapist("U001", "P001")?.size)
        assertNull(repo.sessionsForTherapist("U001", "P003"))
    }

    @Test fun therapistLookupChecksOwnershipAndSession() {
        val found = repo.sessionForTherapist("U001", "P001", "D-U002-5000000")
        assertTrue(found is TherapistSessionLookup.Found)
        assertEquals("P001", (found as TherapistSessionLookup.Found).patient.id)
        assertEquals(TherapistSessionLookup.PatientNotAssigned, repo.sessionForTherapist("U001", "P003", "S-003"))
        assertEquals(TherapistSessionLookup.PatientNotAssigned, repo.sessionForTherapist("U001", "P404", "S-001"))
        assertEquals(TherapistSessionLookup.SessionNotFound, repo.sessionForTherapist("U001", "P001", "S-002"))
    }

    @Test fun patientIsResolvedFromUserId() {
        assertEquals("P002", repo.patientForUser("U003")?.id)
        assertNull(repo.patientForUser("U001"))
    }
}
