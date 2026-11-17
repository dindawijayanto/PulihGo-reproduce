package com.pulihgo.app.ui.summary

import com.pulihgo.app.data.session.PatientSession
import com.pulihgo.app.data.session.SessionPainLevel

/** Patient progress across every recorded session (seeded history + this device). */
data class ProgressSnapshot(
    val totalSessions: Int,
    val totalRepetitions: Int,
    val bestPeakRomDeg: Double?,
    /** Peak ROM of the oldest session that has at least one repetition. */
    val firstPeakRomDeg: Double?,
    /** Peak ROM of the newest session that has at least one repetition. */
    val latestPeakRomDeg: Double?,
    val painReportedSessions: Int,
    /** Sessions with at least one repetition (the ones ROM progress is based on). */
    val sessionsWithRepetitions: Int,
) {
    /** Latest minus first peak ROM; needs two sessions with repetitions to mean anything. */
    val romChangeDeg: Double?
        get() = if (sessionsWithRepetitions > 1 && firstPeakRomDeg != null && latestPeakRomDeg != null) latestPeakRomDeg - firstPeakRomDeg else null
}

/** PGF-PRO-03 — pure helpers for the Session History list and the progress card. */
object SessionHistoryLogic {
    /** Newest first; ties broken by id so the order is stable. */
    fun ordered(history: List<PatientSession>): List<PatientSession> =
        history.sortedWith(compareByDescending<PatientSession> { it.record.endedAtEpochMs }.thenBy { it.id })

    /** The session the report should show: the selected one if it still exists, else the newest. */
    fun selected(history: List<PatientSession>, selectedId: String?): PatientSession? {
        val sorted = ordered(history)
        return sorted.firstOrNull { it.id == selectedId } ?: sorted.firstOrNull()
    }

    fun progress(history: List<PatientSession>): ProgressSnapshot {
        val withReps = ordered(history).filter { it.record.repetitions > 0 }
        return ProgressSnapshot(
            totalSessions = history.size,
            totalRepetitions = history.sumOf { it.record.repetitions },
            bestPeakRomDeg = withReps.maxOfOrNull { it.record.peakRomDeg },
            firstPeakRomDeg = withReps.lastOrNull()?.record?.peakRomDeg,
            latestPeakRomDeg = withReps.firstOrNull()?.record?.peakRomDeg,
            painReportedSessions = history.count { it.record.painLevel != SessionPainLevel.NO_PAIN },
            sessionsWithRepetitions = withReps.size,
        )
    }

    /** Oldest → newest peak ROM values for the trend sparkline (sessions with reps only). */
    fun romTrend(history: List<PatientSession>, limit: Int = 10): List<Double> =
        ordered(history).filter { it.record.repetitions > 0 }.take(limit).reversed().map { it.record.peakRomDeg }
}
