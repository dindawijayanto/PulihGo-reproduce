package com.pulihgo.app.ui.summary

import com.pulihgo.app.data.session.ExerciseSessionRecord
import com.pulihgo.app.data.session.PatientSession
import com.pulihgo.app.data.session.RepetitionMetric
import com.pulihgo.app.data.session.SessionPainLevel
import com.pulihgo.app.data.session.SessionSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class SessionSummaryLogicTest {
    private fun session(reps: Int, details: List<RepetitionMetric> = emptyList(), pain: SessionPainLevel = SessionPainLevel.NO_PAIN) =
        PatientSession(
            id = "s",
            record = ExerciseSessionRecord("U002", "EX001", 1_000_000, 1_016_000, reps, details.maxOfOrNull { it.peakRomDeg } ?: 0.0, null, pain, details),
            source = SessionSource.DEVICE,
        )

    @Test
    fun stateFollowsFigmaVariants() {
        assertEquals(SummaryState.EMPTY, SessionSummaryLogic.stateOf(null))
        assertEquals(SummaryState.NO_REPETITIONS, SessionSummaryLogic.stateOf(session(0)))
        assertEquals(SummaryState.COMPLETED, SessionSummaryLogic.stateOf(session(3)))
    }

    @Test
    fun chartScalesRomAgainstCeilingAndKeepsRepOrder() {
        val s = session(
            2,
            listOf(
                RepetitionMetric(2, 60.0, 2000, 0.9),
                RepetitionMetric(1, 40.0, 2000, null),
            ),
        )
        val points = SessionSummaryLogic.chartPoints(s, romCeilingDeg = 80.0)
        assertEquals(listOf(1, 2), points.map { it.repetition })
        assertEquals(0.5f, points[0].romFraction, 0.001f)
        assertEquals(0.75f, points[1].romFraction, 0.001f)
        assertTrue(SessionSummaryLogic.hasSmoothness(points))
    }

    @Test
    fun chartNeverExceedsTopWhenRomPassesCeiling() {
        val points = SessionSummaryLogic.chartPoints(session(1, listOf(RepetitionMetric(1, 95.0, 1000, null))), 80.0)
        assertEquals(1f, points.single().romFraction, 0.0f)
        assertFalse(SessionSummaryLogic.hasSmoothness(points))
    }

    @Test
    fun noRepetitionDetailsMeansNoChart() {
        assertTrue(SessionSummaryLogic.chartPoints(session(4), 80.0).isEmpty())
    }

    @Test
    fun durationAndDateFormatting() {
        assertEquals("16s", SessionSummaryLogic.formatDuration(16_000))
        assertEquals("3m 30s", SessionSummaryLogic.formatDuration(210_000))
        assertEquals("4m", SessionSummaryLogic.formatDuration(240_000))
        assertEquals("1h 02m", SessionSummaryLogic.formatDuration(3_720_000))
        // 2026-10-15T07:51Z
        assertEquals("Thu 15 Oct, 07:51", SessionSummaryLogic.formatSessionDate(1_792_050_660_000, ZoneOffset.UTC))
    }

    @Test
    fun comfortLabelsMatchPainCheck() {
        assertEquals("No pain", SessionSummaryLogic.comfort(SessionPainLevel.NO_PAIN).text)
        assertEquals(ComfortTone.CAUTION, SessionSummaryLogic.comfort(SessionPainLevel.MILD_PAIN).tone)
        assertEquals(ComfortTone.STOP, SessionSummaryLogic.comfort(SessionPainLevel.STOPPED_DUE_TO_PAIN).tone)
    }
}
