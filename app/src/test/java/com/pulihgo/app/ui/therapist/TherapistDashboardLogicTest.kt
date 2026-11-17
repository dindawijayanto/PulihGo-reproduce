package com.pulihgo.app.ui.therapist

import com.pulihgo.app.data.model.Patient
import com.pulihgo.app.data.model.Role
import com.pulihgo.app.data.model.User
import com.pulihgo.app.data.session.ExerciseSessionRecord
import com.pulihgo.app.data.session.PatientSession
import com.pulihgo.app.data.session.SessionPainLevel
import com.pulihgo.app.data.session.SessionSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class TherapistDashboardLogicTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 11, 17)
    private fun at(date: LocalDate, hour: Int = 9) = date.atTime(hour, 0).toInstant(zone).toEpochMilli()

    private val users = listOf(
        User("U001", "t", "x", Role.THERAPIST, "Demo Therapist", "t@x", true),
        User("U002", "a", "x", Role.PATIENT, "Alya Putri", "a@x", true),
        User("U003", "b", "x", Role.PATIENT, "Bima Pratama", "b@x", true),
        User("U009", "o", "x", Role.PATIENT, "Other Patient", "o@x", true),
    )
    private val patients = listOf(
        Patient("P001", "U002", "U001", 24, "RIGHT", "Forearm", ""),
        Patient("P002", "U003", "U001", 31, "LEFT", "Forearm", ""),
        Patient("P009", "U009", "U777", 40, "LEFT", "Forearm", ""),
    )
    private val exercises = mapOf("EX001" to ExerciseTarget("Forearm Supination and Pronation", 70.0, 80.0))

    private fun session(uid: String, ended: Long, reps: Int, rom: Double, pain: SessionPainLevel, source: SessionSource = SessionSource.DEVICE) =
        PatientSession(
            id = "$uid-$ended",
            record = ExerciseSessionRecord(uid, "EX001", ended - 60_000, ended, reps, rom, 0.8, pain),
            source = source,
        )

    @Test
    fun onlyAssignedPatientsAreIncluded() {
        val cohort = TherapistDashboardLogic.assemble("U001", patients, users, emptyMap())
        assertEquals(listOf("P001", "P002"), cohort.map { it.patient.id })
        assertEquals("Alya Putri", cohort[0].name)
    }

    @Test
    fun emptyCohortShowsEmptyState() {
        val cohort = TherapistDashboardLogic.assemble("U001", patients, users, emptyMap())
        val s = TherapistDashboardLogic.summarize(cohort, exercises, today, zone)
        assertTrue(s.isEmpty)
        assertEquals(2, s.assignedPatients)
        assertEquals(0, s.patientsWithSessions)
        assertNull(s.averageLatestPeakRomDeg)
        assertTrue(s.flags.isEmpty())
        assertEquals(7, s.week.size)
        assertTrue(s.week.last().isToday)
    }

    @Test
    fun weeklyDoseCountsOnlyTheRollingSevenDays() {
        val sessions = mapOf(
            "U002" to listOf(
                session("U002", at(today), 12, 62.0, SessionPainLevel.NO_PAIN),
                session("U002", at(today.minusDays(6)), 5, 50.0, SessionPainLevel.NO_PAIN),
                session("U002", at(today.minusDays(7)), 99, 40.0, SessionPainLevel.NO_PAIN, SessionSource.SEEDED),
            ),
        )
        val cohort = TherapistDashboardLogic.assemble("U001", patients, users, sessions)
        val s = TherapistDashboardLogic.summarize(cohort, exercises, today, zone)
        assertFalse(s.isEmpty)
        assertEquals(17, s.weeklyRepetitions)
        assertEquals(2, s.weeklySessions)
        assertEquals(3, s.totalSessions)
        assertEquals(12, s.week.last().repetitions)
        assertEquals(5, s.week.first().repetitions)
        assertEquals(1, s.patientsWithSessions)
    }

    @Test
    fun averagePeakRomUsesLatestSessionPerPatient() {
        val sessions = mapOf(
            "U002" to listOf(
                session("U002", at(today), 1, 60.0, SessionPainLevel.NO_PAIN),
                session("U002", at(today.minusDays(1)), 1, 10.0, SessionPainLevel.NO_PAIN),
            ),
            "U003" to listOf(session("U003", at(today.minusDays(2)), 1, 40.0, SessionPainLevel.NO_PAIN)),
        )
        val s = TherapistDashboardLogic.summarize(
            TherapistDashboardLogic.assemble("U001", patients, users, sessions), exercises, today, zone,
        )
        assertEquals(50.0, s.averageLatestPeakRomDeg!!, 0.001)
        assertEquals(70.0, s.targetRomDeg!!, 0.0)
    }

    @Test
    fun recentActivityIsNewestFirstAndMarksSeeded() {
        val sessions = mapOf(
            "U002" to listOf(session("U002", at(today.minusDays(3)), 3, 56.0, SessionPainLevel.NO_PAIN, SessionSource.SEEDED)),
            "U003" to listOf(session("U003", at(today), 4, 48.0, SessionPainLevel.STOPPED_DUE_TO_PAIN)),
        )
        val s = TherapistDashboardLogic.summarize(
            TherapistDashboardLogic.assemble("U001", patients, users, sessions), exercises, today, zone,
        )
        assertEquals(listOf("P002", "P001"), s.recentActivity.map { it.patientId })
        assertEquals(ActivityOutcome.STOPPED_FOR_PAIN, s.recentActivity[0].outcome)
        assertTrue(s.recentActivity[1].seeded)
        assertEquals("Forearm Supination and Pronation", s.recentActivity[0].exerciseName)
    }

    @Test
    fun flagsComeFromLatestSessionOnlyAndAreOrderedBySeverity() {
        val sessions = mapOf(
            // Older pain is superseded by a newer session at the ROM ceiling.
            "U002" to listOf(
                session("U002", at(today), 6, 82.0, SessionPainLevel.NO_PAIN),
                session("U002", at(today.minusDays(1)), 2, 30.0, SessionPainLevel.STOPPED_DUE_TO_PAIN),
            ),
            "U003" to listOf(session("U003", at(today.minusDays(1)), 4, 48.0, SessionPainLevel.STOPPED_DUE_TO_PAIN)),
        )
        val s = TherapistDashboardLogic.summarize(
            TherapistDashboardLogic.assemble("U001", patients, users, sessions), exercises, today, zone,
        )
        assertEquals(listOf(FlagKind.PAIN_STOPPED, FlagKind.CEILING_REACHED), s.flags.map { it.kind })
        assertEquals(listOf("P002", "P001"), s.flags.map { it.patientId })
    }

    @Test
    fun formattingHelpers() {
        assertEquals("Tuesday, November 17, 2026", TherapistDashboardLogic.formatLongDate(today))
        assertEquals("just now", TherapistDashboardLogic.formatAgo(1_000, 1_000))
        assertEquals("18h ago", TherapistDashboardLogic.formatAgo(0, 18 * 3_600_000L))
        assertEquals("2d ago", TherapistDashboardLogic.formatAgo(0, 50 * 3_600_000L))
    }
}
