package com.pulihgo.app.ui.therapist

import com.pulihgo.app.data.model.Patient
import com.pulihgo.app.data.model.Role
import com.pulihgo.app.data.model.User
import com.pulihgo.app.data.session.ExerciseSessionRecord
import com.pulihgo.app.data.session.SessionPainLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TherapistPatientListLogicTest {

    private val users = listOf(
        User("U001", "therapist01", "x", Role.THERAPIST, "Dr. Demo", "t@demo", true),
        User("U002", "patient01", "x", Role.PATIENT, "Alya Putri", "a@demo", true),
        User("U003", "patient02", "x", Role.PATIENT, "Bima Pratama", "b@demo", true),
        User("U004", "patient03", "x", Role.PATIENT, "Citra Other", "c@demo", true),
    )
    private val patients = listOf(
        Patient("P001", "U002", "U001", 24, "RIGHT", "Forearm", ""),
        Patient("P002", "U003", "U001", 31, "LEFT", "Forearm", ""),
        Patient("P003", "U004", "U999", 40, "LEFT", "Forearm", ""),
    )

    private fun record(userId: String, ended: Long, reps: Int, pain: SessionPainLevel) =
        ExerciseSessionRecord(userId, "EX001", ended / 2, ended, reps, 60.0, 0.8, pain)

    private fun items(sessions: Map<String, List<ExerciseSessionRecord>> = emptyMap()) =
        TherapistPatientListLogic.buildItems("U001", patients, users, sessions)

    @Test
    fun onlyAssignedPatientsAreListed() {
        val result = items()
        assertEquals(listOf("P001", "P002"), result.map { it.patientId })
        assertTrue(result.all { it.status == PatientStatus.NO_SESSIONS })
    }

    @Test
    fun sessionsAreMatchedByUserIdAndNeverCrossAttributed() {
        val result = items(
            mapOf(
                "U002" to listOf(record("U002", 2_000, 5, SessionPainLevel.NO_PAIN)),
                // A record under the wrong key must not leak into Bima's row.
                "U003" to listOf(record("U002", 3_000, 9, SessionPainLevel.NO_PAIN)),
            ),
        )
        val alya = result.first { it.patientId == "P001" }
        val bima = result.first { it.patientId == "P002" }
        assertEquals(1, alya.sessionCount)
        assertEquals(5, alya.totalRepetitions)
        assertEquals(0, bima.sessionCount)
        assertNull(bima.lastSession)
    }

    @Test
    fun searchMatchesNameIdAndUsernameCaseInsensitively() {
        val all = items()
        fun ids(q: String) = TherapistPatientListLogic
            .apply(all, q, PatientListFilter.ALL, PatientListSort.NAME_ASC).map { it.patientId }
        assertEquals(listOf("P001"), ids("  alya "))
        assertEquals(listOf("P002"), ids("p002"))
        assertEquals(listOf("P002"), ids("PATIENT02"))
        assertEquals(listOf("P001", "P002"), ids(""))
        assertEquals(emptyList<String>(), ids("zzz"))
    }

    @Test
    fun followUpUsesLatestSessionPainOnly() {
        val result = items(
            mapOf(
                "U002" to listOf(
                    record("U002", 1_000, 3, SessionPainLevel.STOPPED_DUE_TO_PAIN),
                    record("U002", 5_000, 4, SessionPainLevel.NO_PAIN),
                ),
                "U003" to listOf(record("U003", 2_000, 2, SessionPainLevel.MILD_PAIN)),
            ),
        )
        assertEquals(PatientStatus.ON_TRACK, result.first { it.patientId == "P001" }.status)
        val followUp = TherapistPatientListLogic.apply(
            result, "", PatientListFilter.NEEDS_FOLLOW_UP, PatientListSort.NAME_ASC,
        )
        assertEquals(listOf("P002"), followUp.map { it.patientId })
        val active = TherapistPatientListLogic.apply(
            result, "", PatientListFilter.ACTIVE, PatientListSort.NAME_ASC,
        )
        assertEquals(2, active.size)
    }

    @Test
    fun mostRecentSortPutsPatientsWithoutSessionsLast() {
        val result = items(mapOf("U003" to listOf(record("U003", 9_000, 1, SessionPainLevel.NO_PAIN))))
        val sorted = TherapistPatientListLogic.sort(result, PatientListSort.MOST_RECENT_SESSION)
        assertEquals(listOf("P002", "P001"), sorted.map { it.patientId })
    }

    @Test
    fun pagingClampsAndSplitsPages() {
        val many = (1..9).map { i ->
            items().first().copy(patientId = "P%03d".format(i), name = "Patient $i")
        }
        val last = TherapistPatientListLogic.page(many, requestedPage = 99)
        assertEquals(3, last.pageCount)
        assertEquals(2, last.pageIndex)
        assertEquals(1, last.items.size)
        val empty = TherapistPatientListLogic.page(emptyList(), requestedPage = 3)
        assertEquals(1, empty.pageCount)
        assertEquals(0, empty.pageIndex)
        assertTrue(empty.items.isEmpty())
    }

    @Test
    fun initialsAndSessionTimeFormatting() {
        assertEquals("AP", TherapistPatientListLogic.initials("Andi Pratama"))
        assertEquals("AL", TherapistPatientListLogic.initials(" alya "))
        assertEquals("?", TherapistPatientListLogic.initials(""))

        val zone = java.time.ZoneId.of("Asia/Jakarta")
        fun at(day: Int, h: Int, m: Int) =
            java.time.ZonedDateTime.of(2026, 11, day, h, m, 0, 0, zone).toInstant().toEpochMilli()
        val now = at(17, 18, 0)
        assertEquals("Today, 14:20", TherapistPatientListLogic.formatSessionTime(at(17, 14, 20), now, zone))
        assertEquals("Yesterday, 16:45", TherapistPatientListLogic.formatSessionTime(at(16, 16, 45), now, zone))
        assertEquals("Nov 13, 10:15", TherapistPatientListLogic.formatSessionTime(at(13, 10, 15), now, zone))
    }

    @Test
    fun overviewSumsOnlyRealRecordedData() {
        val result = items(
            mapOf(
                "U002" to listOf(
                    record("U002", 1_000, 3, SessionPainLevel.NO_PAIN),
                    record("U002", 2_000, 4, SessionPainLevel.MILD_PAIN),
                ),
            ),
        )
        val overview = TherapistPatientListLogic.overview(result)
        assertEquals(CohortOverview(2, 1, 1, 7), overview)
    }
}
