package com.pulihgo.app.ui.summary

import com.pulihgo.app.data.session.ExerciseSessionRecord
import com.pulihgo.app.data.session.PatientSession
import com.pulihgo.app.data.session.SessionPainLevel
import com.pulihgo.app.data.session.SessionSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionHistoryLogicTest {
    private fun s(id: String, ended: Long, reps: Int, rom: Double, pain: SessionPainLevel = SessionPainLevel.NO_PAIN, source: SessionSource = SessionSource.DEVICE) =
        PatientSession(id, ExerciseSessionRecord("U002", "EX001", ended - 1000, ended, reps, rom, null, pain), source)

    private val history = listOf(
        s("S002", 1_000_000, 2, 65.0, source = SessionSource.SEEDED),
        s("S001", 2_000_000, 3, 56.0, source = SessionSource.SEEDED),
        s("D-1", 3_000_000, 0, 0.0, SessionPainLevel.MILD_PAIN),
        s("D-2", 4_000_000, 6, 62.0),
    )

    @Test
    fun orderedIsNewestFirst() {
        assertEquals(listOf("D-2", "D-1", "S001", "S002"), SessionHistoryLogic.ordered(history).map { it.id })
    }

    @Test
    fun selectedFallsBackToNewestWhenMissing() {
        assertEquals("S001", SessionHistoryLogic.selected(history, "S001")!!.id)
        assertEquals("D-2", SessionHistoryLogic.selected(history, "gone")!!.id)
        assertEquals("D-2", SessionHistoryLogic.selected(history, null)!!.id)
        assertNull(SessionHistoryLogic.selected(emptyList(), null))
    }

    @Test
    fun progressIgnoresZeroRepSessionsForRom() {
        val p = SessionHistoryLogic.progress(history)
        assertEquals(4, p.totalSessions)
        assertEquals(11, p.totalRepetitions)
        assertEquals(65.0, p.bestPeakRomDeg!!, 0.0)
        assertEquals(65.0, p.firstPeakRomDeg!!, 0.0)
        assertEquals(62.0, p.latestPeakRomDeg!!, 0.0)
        assertEquals(-3.0, p.romChangeDeg!!, 0.0)
        assertEquals(1, p.painReportedSessions)
        assertEquals(3, p.sessionsWithRepetitions)
    }

    @Test
    fun romChangeNeedsTwoSessionsWithReps() {
        val p = SessionHistoryLogic.progress(listOf(s("a", 10_000, 3, 50.0), s("b", 20_000, 0, 0.0)))
        assertNull(p.romChangeDeg)
        assertNull(SessionHistoryLogic.progress(emptyList()).bestPeakRomDeg)
    }

    @Test
    fun romTrendIsOldestToNewest() {
        assertEquals(listOf(65.0, 56.0, 62.0), SessionHistoryLogic.romTrend(history))
        assertEquals(listOf(56.0, 62.0), SessionHistoryLogic.romTrend(history, limit = 2))
    }
}
