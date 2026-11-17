package com.pulihgo.app.ui.summary

import com.pulihgo.app.data.session.PatientSession
import com.pulihgo.app.data.session.SessionPainLevel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Figma Summary states: 08 Empty, 15 No Repetitions, 21 Completed / Repetition Chart. */
enum class SummaryState { EMPTY, NO_REPETITIONS, COMPLETED }

/** One chart column: ROM and smoothness normalised to 0..1 for drawing. */
data class RepetitionPoint(
    val repetition: Int,
    val peakRomDeg: Double,
    val romFraction: Float,
    val smoothness: Double?,
)

enum class ComfortTone { GOOD, CAUTION, STOP }

data class ComfortLabel(val text: String, val tone: ComfortTone)

object SessionSummaryLogic {
    fun stateOf(session: PatientSession?): SummaryState = when {
        session == null -> SummaryState.EMPTY
        session.record.repetitions == 0 -> SummaryState.NO_REPETITIONS
        else -> SummaryState.COMPLETED
    }

    /**
     * ROM is scaled against the larger of the exercise ceiling and the best repetition so
     * the line never leaves the chart; smoothness is already 0..1.
     */
    fun chartPoints(session: PatientSession, romCeilingDeg: Double?): List<RepetitionPoint> {
        val reps = session.record.repetitionDetails.sortedBy { it.repetitionNumber }
        if (reps.isEmpty()) return emptyList()
        val scale = maxOf(romCeilingDeg ?: 0.0, reps.maxOf { it.peakRomDeg }, 1.0)
        return reps.map {
            RepetitionPoint(
                repetition = it.repetitionNumber,
                peakRomDeg = it.peakRomDeg,
                romFraction = (it.peakRomDeg / scale).toFloat().coerceIn(0f, 1f),
                smoothness = it.smoothnessScore,
            )
        }
    }

    fun hasSmoothness(points: List<RepetitionPoint>) = points.any { it.smoothness != null }

    /** "16s", "3m 30s", "1h 02m". */
    fun formatDuration(durationMs: Long): String {
        val total = (durationMs / 1000).coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return when {
            h > 0 -> String.format(Locale.US, "%dh %02dm", h, m)
            m > 0 -> if (s == 0L) "${m}m" else "${m}m ${s}s"
            else -> "${s}s"
        }
    }

    /** Figma "Thu 15 Oct, 07:51". */
    fun formatSessionDate(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMs).atZone(zone).format(DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.ENGLISH))

    /** Joint comfort as reported by the patient after the session (Pain Check). */
    fun comfort(pain: SessionPainLevel): ComfortLabel = when (pain) {
        SessionPainLevel.NO_PAIN -> ComfortLabel("No pain", ComfortTone.GOOD)
        SessionPainLevel.MILD_PAIN -> ComfortLabel("Mild discomfort", ComfortTone.CAUTION)
        SessionPainLevel.STOPPED_DUE_TO_PAIN -> ComfortLabel("Stopped: pain", ComfortTone.STOP)
    }
}
