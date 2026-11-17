package com.pulihgo.app.ui.therapist

import com.pulihgo.app.data.model.Patient
import com.pulihgo.app.data.model.User
import com.pulihgo.app.data.session.PatientSession
import com.pulihgo.app.data.session.SessionPainLevel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/** Exercise parameters used to judge ROM against the prescription (from exercises.json). */
data class ExerciseTarget(val name: String, val targetRomDeg: Double, val romCeilingDeg: Double, val targetReps: Int = 0)

/** One patient with every session we know about (seeded + device), newest first. */
data class TherapistPatientSessions(
    val patient: Patient,
    val name: String,
    val sessions: List<PatientSession>,
) {
    val latest: PatientSession? get() = sessions.firstOrNull()
}

data class DailyReps(val date: LocalDate, val dayLetter: String, val repetitions: Int, val isToday: Boolean)

enum class ActivityOutcome { COMPLETED, MILD_PAIN, STOPPED_FOR_PAIN }

data class RecentActivityRow(
    val patientId: String,
    val name: String,
    val exerciseName: String,
    val endedAtEpochMs: Long,
    val repetitions: Int,
    val peakRomDeg: Double,
    val outcome: ActivityOutcome,
    val seeded: Boolean,
)

enum class FlagKind { PAIN_STOPPED, MILD_PAIN, CEILING_REACHED }

data class MonitoringFlag(
    val patientId: String,
    val name: String,
    val kind: FlagKind,
    val detail: String,
    val endedAtEpochMs: Long,
)

data class DashboardSummary(
    val assignedPatients: Int,
    val patientsWithSessions: Int,
    val totalSessions: Int,
    val weeklyRepetitions: Int,
    val weeklySessions: Int,
    /** Mean of each patient's latest-session peak ROM; null when no patient has a session. */
    val averageLatestPeakRomDeg: Double?,
    val targetRomDeg: Double?,
    val week: List<DailyReps>,
    val recentActivity: List<RecentActivityRow>,
    val flags: List<MonitoringFlag>,
) {
    /** Figma Screen 01 empty state: nothing assigned or nothing recorded yet. */
    val isEmpty: Boolean get() = totalSessions == 0
}

/**
 * PGF-THR-02 — pure dashboard aggregation. Only uses saved/seeded session results;
 * adherence and schedules are not computed because no prescription schedule exists yet.
 */
object TherapistDashboardLogic {
    const val RECENT_LIMIT = 4

    fun assemble(
        therapistId: String,
        patients: List<Patient>,
        users: List<User>,
        sessionsByUserId: Map<String, List<PatientSession>>,
    ): List<TherapistPatientSessions> {
        val names = users.associate { it.id to it.fullName }
        return patients.filter { it.therapistId == therapistId }.map { p ->
            TherapistPatientSessions(
                patient = p,
                name = names[p.userId]?.takeIf { it.isNotBlank() } ?: p.id,
                sessions = sessionsByUserId[p.userId].orEmpty()
                    .filter { it.record.patientId == p.userId }
                    .sortedByDescending { it.record.endedAtEpochMs },
            )
        }
    }

    fun summarize(
        patients: List<TherapistPatientSessions>,
        exercises: Map<String, ExerciseTarget>,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): DashboardSummary {
        val all = patients.flatMap { it.sessions }
        val weekStart = today.minusDays(6)
        fun dayOf(s: PatientSession) = Instant.ofEpochMilli(s.record.endedAtEpochMs).atZone(zone).toLocalDate()
        val inWeek = all.filter { val d = dayOf(it); !d.isBefore(weekStart) && !d.isAfter(today) }
        val week = (0..6).map { offset ->
            val date = weekStart.plusDays(offset.toLong())
            DailyReps(
                date = date,
                dayLetter = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH),
                repetitions = inWeek.filter { dayOf(it) == date }.sumOf { it.record.repetitions },
                isToday = date == today,
            )
        }
        val latest = patients.mapNotNull { p -> p.latest?.let { p to it } }
        val recent = latest
            .sortedByDescending { it.second.record.endedAtEpochMs }
            .take(RECENT_LIMIT)
            .map { (p, s) ->
                RecentActivityRow(
                    patientId = p.patient.id,
                    name = p.name,
                    exerciseName = exercises[s.record.exerciseId]?.name ?: s.record.exerciseId,
                    endedAtEpochMs = s.record.endedAtEpochMs,
                    repetitions = s.record.repetitions,
                    peakRomDeg = s.record.peakRomDeg,
                    outcome = outcomeOf(s.record.painLevel),
                    seeded = s.source == com.pulihgo.app.data.session.SessionSource.SEEDED,
                )
            }
        val targets = latest.mapNotNull { (_, s) -> s.targetRomDeg ?: exercises[s.record.exerciseId]?.targetRomDeg }.distinct()
        return DashboardSummary(
            assignedPatients = patients.size,
            patientsWithSessions = latest.size,
            totalSessions = all.size,
            weeklyRepetitions = inWeek.sumOf { it.record.repetitions },
            weeklySessions = inWeek.size,
            averageLatestPeakRomDeg = latest.map { it.second.record.peakRomDeg }.takeIf { it.isNotEmpty() }?.average(),
            targetRomDeg = targets.singleOrNull(),
            week = week,
            recentActivity = recent,
            flags = flags(latest, exercises),
        )
    }

    fun outcomeOf(pain: SessionPainLevel) = when (pain) {
        SessionPainLevel.NO_PAIN -> ActivityOutcome.COMPLETED
        SessionPainLevel.MILD_PAIN -> ActivityOutcome.MILD_PAIN
        SessionPainLevel.STOPPED_DUE_TO_PAIN -> ActivityOutcome.STOPPED_FOR_PAIN
    }

    /** Flags look only at each patient's latest session, ordered by severity then recency. */
    private fun flags(
        latest: List<Pair<TherapistPatientSessions, PatientSession>>,
        exercises: Map<String, ExerciseTarget>,
    ): List<MonitoringFlag> = latest.mapNotNull { (p, s) ->
        val r = s.record
        val rom = r.peakRomDeg.toInt()
        val ceiling = exercises[r.exerciseId]?.romCeilingDeg
        when {
            r.painLevel == SessionPainLevel.STOPPED_DUE_TO_PAIN -> MonitoringFlag(
                p.patient.id, p.name, FlagKind.PAIN_STOPPED,
                "Stopped the session due to pain after ${r.repetitions} ${if (r.repetitions == 1) "rep" else "reps"} (peak $rom°).",
                r.endedAtEpochMs,
            )
            r.painLevel == SessionPainLevel.MILD_PAIN -> MonitoringFlag(
                p.patient.id, p.name, FlagKind.MILD_PAIN,
                "Reported mild pain after the last session (peak $rom°).",
                r.endedAtEpochMs,
            )
            ceiling != null && ceiling > 0 && r.peakRomDeg >= ceiling -> MonitoringFlag(
                p.patient.id, p.name, FlagKind.CEILING_REACHED,
                "Peak ROM reached $rom° against a ${ceiling.toInt()}° ceiling.",
                r.endedAtEpochMs,
            )
            else -> null
        }
    }.sortedWith(compareBy<MonitoringFlag> { it.kind.ordinal }.thenByDescending { it.endedAtEpochMs })

    /** "Thursday, October 15, 2026" as in the Figma date pill. */
    fun formatLongDate(date: LocalDate): String =
        date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH))

    /** "18h ago" / "3d ago" / "just now" for flag recency. */
    fun formatAgo(epochMs: Long, nowEpochMs: Long): String {
        val minutes = ((nowEpochMs - epochMs) / 60_000).coerceAtLeast(0)
        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "${minutes}m ago"
            minutes < 60 * 24 -> "${minutes / 60}h ago"
            else -> "${minutes / (60 * 24)}d ago"
        }
    }
}
