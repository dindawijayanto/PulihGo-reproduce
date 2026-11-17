package com.pulihgo.app.ui.therapist

import com.pulihgo.app.data.session.PatientSession
import com.pulihgo.app.data.session.SessionPainLevel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class ReportRange(val label: String, val days: Long?) {
    LAST_7_DAYS("Last 7 Days", 7),
    LAST_30_DAYS("Last 30 Days", 30),
    ALL_TIME("All Time", null),
}

data class ReportKpis(
    val sessions: Int,
    val totalRepetitions: Int,
    val bestPeakRomDeg: Double?,
    val averagePeakRomDeg: Double?,
    /** Mean of the sessions that measured smoothness; null when none did. */
    val averageSmoothness: Double?,
    val painReportedSessions: Int,
)

data class RomTrendPoint(val label: String, val peakRomDeg: Double)

enum class ReportSessionStatus { COMPLETED, MILD_PAIN, STOPPED_FOR_PAIN, NO_REPETITIONS }

data class ReportSessionRow(
    val id: String,
    val dateLabel: String,
    val timeLabel: String,
    val exerciseName: String,
    val repetitions: Int,
    val peakRomDeg: Double,
    val durationMs: Long,
    val status: ReportSessionStatus,
    val seeded: Boolean,
)

data class PatientReport(
    val patientId: String,
    val name: String,
    val goal: String,
    val range: ReportRange,
    val rangeLabel: String,
    val kpis: ReportKpis,
    /** Sessions with repetitions in the range, oldest → newest. */
    val romTrend: List<RomTrendPoint>,
    val targetRomDeg: Double?,
    val romCeilingDeg: Double?,
    /** Always the rolling 7 days ending today, as in Figma's "Weekly Rep Distribution". */
    val week: List<DailyReps>,
    val targetRepsPerSession: Int?,
    /** Newest first. */
    val rows: List<ReportSessionRow>,
    /** Sessions recorded for this patient outside the selected range. */
    val sessionsOutsideRange: Int,
)

/**
 * PGF-THR-06 — per-patient report data for the Reports UI. KPI definitions here are simple
 * aggregates of saved sessions; PGF-THR-07 (Erza) owns the final KPI calculations.
 */
object TherapistReportsLogic {
    private val dayLabel = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val rowDate = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    private val rowTime = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

    fun rangeStart(range: ReportRange, today: LocalDate): LocalDate? = range.days?.let { today.minusDays(it - 1) }

    fun rangeLabel(range: ReportRange, today: LocalDate): String {
        val start = rangeStart(range, today) ?: return range.label
        return "${range.label} (${start.format(dayLabel)} – ${today.format(dayLabel)})"
    }

    fun statusOf(s: PatientSession): ReportSessionStatus = when {
        // Pain feedback outranks the repetition count so the therapist never loses it.
        s.record.painLevel == SessionPainLevel.STOPPED_DUE_TO_PAIN -> ReportSessionStatus.STOPPED_FOR_PAIN
        s.record.painLevel == SessionPainLevel.MILD_PAIN -> ReportSessionStatus.MILD_PAIN
        s.record.repetitions == 0 -> ReportSessionStatus.NO_REPETITIONS
        else -> ReportSessionStatus.COMPLETED
    }

    fun build(
        patient: TherapistPatientSessions,
        range: ReportRange,
        exercises: Map<String, ExerciseTarget>,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): PatientReport {
        fun dateOf(s: PatientSession) = Instant.ofEpochMilli(s.record.endedAtEpochMs).atZone(zone).toLocalDate()
        val start = rangeStart(range, today)
        val all = patient.sessions.sortedByDescending { it.record.endedAtEpochMs }
        val inRange = all.filter { s -> val d = dateOf(s); (start == null || !d.isBefore(start)) && !d.isAfter(today) }
        val withReps = inRange.filter { it.record.repetitions > 0 }
        val smooth = inRange.mapNotNull { it.record.averageSmoothness }
        val exerciseIds = all.map { it.record.exerciseId }.distinct()
        val exercise = exerciseIds.singleOrNull()?.let { exercises[it] }
        val weekStart = today.minusDays(6)
        val week = (0..6).map { offset ->
            val date = weekStart.plusDays(offset.toLong())
            DailyReps(
                date = date,
                dayLetter = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                repetitions = all.filter { dateOf(it) == date }.sumOf { it.record.repetitions },
                isToday = date == today,
            )
        }
        return PatientReport(
            patientId = patient.patient.id,
            name = patient.name,
            goal = patient.patient.rehabilitationGoal,
            range = range,
            rangeLabel = rangeLabel(range, today),
            kpis = ReportKpis(
                sessions = inRange.size,
                totalRepetitions = inRange.sumOf { it.record.repetitions },
                bestPeakRomDeg = withReps.maxOfOrNull { it.record.peakRomDeg },
                averagePeakRomDeg = withReps.map { it.record.peakRomDeg }.takeIf { it.isNotEmpty() }?.average(),
                averageSmoothness = smooth.takeIf { it.isNotEmpty() }?.average(),
                painReportedSessions = inRange.count { it.record.painLevel != SessionPainLevel.NO_PAIN },
            ),
            romTrend = withReps.reversed().map { RomTrendPoint(dateOf(it).format(dayLabel), it.record.peakRomDeg) },
            targetRomDeg = all.firstNotNullOfOrNull { it.targetRomDeg } ?: exercise?.targetRomDeg,
            romCeilingDeg = exercise?.romCeilingDeg,
            week = week,
            targetRepsPerSession = all.firstNotNullOfOrNull { it.targetReps } ?: exercise?.targetReps?.takeIf { it > 0 },
            rows = inRange.map { s ->
                val zdt = Instant.ofEpochMilli(s.record.startedAtEpochMs).atZone(zone)
                ReportSessionRow(
                    id = s.id,
                    dateLabel = zdt.format(rowDate),
                    timeLabel = zdt.format(rowTime),
                    exerciseName = exercises[s.record.exerciseId]?.name ?: s.record.exerciseId,
                    repetitions = s.record.repetitions,
                    peakRomDeg = s.record.peakRomDeg,
                    durationMs = s.durationMs,
                    status = statusOf(s),
                    seeded = s.source == com.pulihgo.app.data.session.SessionSource.SEEDED,
                )
            },
            sessionsOutsideRange = all.size - inRange.size,
        )
    }
}
