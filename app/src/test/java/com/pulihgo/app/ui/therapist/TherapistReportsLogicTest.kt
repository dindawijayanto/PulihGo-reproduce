package com.pulihgo.app.ui.therapist

import com.pulihgo.app.data.model.Patient
import com.pulihgo.app.data.session.ExerciseSessionRecord
import com.pulihgo.app.data.session.PatientSession
import com.pulihgo.app.data.session.SessionPainLevel
import com.pulihgo.app.data.session.SessionSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class TherapistReportsLogicTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 11, 17)
    private fun at(date: LocalDate, hour: Int = 9) = date.atTime(hour, 0).toInstant(zone).toEpochMilli()
    private val exercises = mapOf("EX001" to ExerciseTarget("Forearm Supination and Pronation", 70.0, 80.0, 10))

    private fun session(id: String, date: LocalDate, reps: Int, rom: Double, pain: SessionPainLevel = SessionPainLevel.NO_PAIN, smooth: Double? = null, seeded: Boolean = false) =
        PatientSession(
            id = id,
            record = ExerciseSessionRecord("U002", "EX001", at(date) - 180_000, at(date), reps, rom, smooth, pain),
            source = if (seeded) SessionSource.SEEDED else SessionSource.DEVICE,
            targetRomDeg = if (seeded) 70.0 else null,
            targetReps = if (seeded) 10 else null,
        )

    private fun patient(vararg sessions: PatientSession) = TherapistPatientSessions(
        patient = Patient("P001", "U002", "U001", 24, "RIGHT", "Forearm supination and pronation", ""),
        name = "Alya Putri",
        sessions = sessions.sortedByDescending { it.record.endedAtEpochMs },
    )

    private val alya = patient(
        session("S001", today.minusDays(9), 3, 56.0, seeded = true),
        session("D1", today.minusDays(2), 8, 55.0, SessionPainLevel.MILD_PAIN, smooth = 0.78),
        session("D2", today, 6, 62.0, smooth = 0.84),
        session("D3", today.minusDays(1), 0, 0.0, SessionPainLevel.STOPPED_DUE_TO_PAIN),
    )

    @Test
    fun lastSevenDaysExcludesOlderSessions() {
        val r = TherapistReportsLogic.build(alya, ReportRange.LAST_7_DAYS, exercises, today, zone)
        assertEquals(listOf("D2", "D3", "D1"), r.rows.map { it.id })
        assertEquals(1, r.sessionsOutsideRange)
        assertEquals(3, r.kpis.sessions)
        assertEquals(14, r.kpis.totalRepetitions)
        assertEquals(62.0, r.kpis.bestPeakRomDeg!!, 0.0)
        assertEquals(58.5, r.kpis.averagePeakRomDeg!!, 0.001)
        assertEquals(0.81, r.kpis.averageSmoothness!!, 0.001)
        assertEquals(2, r.kpis.painReportedSessions)
        assertTrue(r.rangeLabel.startsWith("Last 7 Days (11 Nov"))
    }

    @Test
    fun allTimeIncludesSeededHistoryAndTrendIsOldestFirst() {
        val r = TherapistReportsLogic.build(alya, ReportRange.ALL_TIME, exercises, today, zone)
        assertEquals(4, r.rows.size)
        assertEquals(0, r.sessionsOutsideRange)
        assertEquals(listOf(56.0, 55.0, 62.0), r.romTrend.map { it.peakRomDeg })
        assertEquals("8 Nov", r.romTrend.first().label)
        assertTrue(r.rows.last().seeded)
        assertEquals("All Time", r.rangeLabel)
    }

    @Test
    fun targetsComeFromSeedOrExercise() {
        val r = TherapistReportsLogic.build(alya, ReportRange.ALL_TIME, exercises, today, zone)
        assertEquals(70.0, r.targetRomDeg!!, 0.0)
        assertEquals(80.0, r.romCeilingDeg!!, 0.0)
        assertEquals(10, r.targetRepsPerSession)
        val deviceOnly = patient(session("D9", today, 4, 40.0))
        assertEquals(10, TherapistReportsLogic.build(deviceOnly, ReportRange.ALL_TIME, exercises, today, zone).targetRepsPerSession)
    }

    @Test
    fun weekBarsAreTheRollingSevenDays() {
        val r = TherapistReportsLogic.build(alya, ReportRange.ALL_TIME, exercises, today, zone)
        assertEquals(7, r.week.size)
        assertEquals(today, r.week.last().date)
        assertEquals(6, r.week.last().repetitions)
        assertEquals(8, r.week[4].repetitions)
        assertEquals(14, r.week.sumOf { it.repetitions })
    }

    @Test
    fun statusMapping() {
        val r = TherapistReportsLogic.build(alya, ReportRange.ALL_TIME, exercises, today, zone)
        val byId = r.rows.associateBy { it.id }
        assertEquals(ReportSessionStatus.COMPLETED, byId.getValue("D2").status)
        assertEquals(ReportSessionStatus.STOPPED_FOR_PAIN, byId.getValue("D3").status)
        assertEquals(ReportSessionStatus.MILD_PAIN, byId.getValue("D1").status)
        assertEquals(ReportSessionStatus.NO_REPETITIONS, TherapistReportsLogic.statusOf(session("z", today, 0, 0.0)))
        assertEquals(ReportSessionStatus.MILD_PAIN, TherapistReportsLogic.statusOf(session("y", today, 0, 0.0, SessionPainLevel.MILD_PAIN)))
    }

    @Test
    fun emptyPatientHasNoKpiValues() {
        val r = TherapistReportsLogic.build(patient(), ReportRange.LAST_30_DAYS, exercises, today, zone)
        assertTrue(r.rows.isEmpty())
        assertNull(r.kpis.bestPeakRomDeg)
        assertNull(r.kpis.averageSmoothness)
        assertTrue(r.romTrend.isEmpty())
    }
}
