package com.pulihgo.app.ui.therapist

import com.pulihgo.app.data.model.Patient
import com.pulihgo.app.data.model.User
import com.pulihgo.app.data.session.ExerciseSessionRecord
import com.pulihgo.app.data.session.SessionPainLevel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * PGF-THR-03 — pure (Android-free) model and rules for the therapist Patient List.
 *
 * Data honesty rules:
 * - Only seeded patients assigned to the therapist are listed; nothing is invented.
 * - Session metrics come from [ExerciseSessionRecord]s (seeded history + this device).
 *   Records are keyed by the patient's *user* id (see AppNavigation SENSOR_CHECK),
 *   so callers must group them by [Patient.userId].
 * - "Needs follow-up" only means the latest saved session carried a pain report.
 *   It is a review prompt, not a clinical diagnosis.
 * - Adherence and scheduled reviews are NOT computed: there is no schedule data yet.
 */
enum class PatientStatus { NO_SESSIONS, ON_TRACK, NEEDS_FOLLOW_UP }

enum class PatientListFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    NEEDS_FOLLOW_UP("Needs Follow-up"),
}

enum class PatientListSort(val label: String) {
    MOST_RECENT_SESSION("Most Recent Session"),
    NAME_ASC("Name (A–Z)"),
}

data class LastSessionSummary(
    val endedAtEpochMs: Long,
    val repetitions: Int,
    val peakRomDeg: Double,
    val averageSmoothness: Double?,
    val painLevel: SessionPainLevel,
)

data class PatientListItem(
    val patientId: String,
    val userId: String,
    val name: String,
    val username: String,
    val age: Int,
    val affectedArm: String,
    val rehabilitationGoal: String,
    val sessionCount: Int,
    val totalRepetitions: Int,
    val lastSession: LastSessionSummary?,
) {
    val status: PatientStatus
        get() = when {
            lastSession == null -> PatientStatus.NO_SESSIONS
            lastSession.painLevel != SessionPainLevel.NO_PAIN -> PatientStatus.NEEDS_FOLLOW_UP
            else -> PatientStatus.ON_TRACK
        }
}

data class CohortOverview(
    val totalPatients: Int,
    val activePatients: Int,
    val needsFollowUp: Int,
    val totalRepetitions: Int,
)

data class PatientPage(
    val items: List<PatientListItem>,
    val pageIndex: Int,
    val pageCount: Int,
    val totalMatches: Int,
)

object TherapistPatientListLogic {
    const val PAGE_SIZE = 4

    /** Builds list rows for patients assigned to [therapistId] only. */
    fun buildItems(
        therapistId: String,
        patients: List<Patient>,
        users: List<User>,
        sessionsByUserId: Map<String, List<ExerciseSessionRecord>>,
    ): List<PatientListItem> {
        val usersById = users.associateBy { it.id }
        return patients
            .filter { it.therapistId == therapistId }
            .map { patient ->
                val user = usersById[patient.userId]
                // Defensive: never attribute another patient's records to this row.
                val sessions = sessionsByUserId[patient.userId].orEmpty()
                    .filter { it.patientId == patient.userId }
                val latest = sessions.maxByOrNull { it.endedAtEpochMs }
                PatientListItem(
                    patientId = patient.id,
                    userId = patient.userId,
                    name = user?.fullName?.takeIf { it.isNotBlank() } ?: patient.id,
                    username = user?.username.orEmpty(),
                    age = patient.age,
                    affectedArm = patient.affectedArm,
                    rehabilitationGoal = patient.rehabilitationGoal,
                    sessionCount = sessions.size,
                    totalRepetitions = sessions.sumOf { it.repetitions },
                    lastSession = latest?.let {
                        LastSessionSummary(
                            endedAtEpochMs = it.endedAtEpochMs,
                            repetitions = it.repetitions,
                            peakRomDeg = it.peakRomDeg,
                            averageSmoothness = it.averageSmoothness,
                            painLevel = it.painLevel,
                        )
                    },
                )
            }
    }

    fun matchesQuery(item: PatientListItem, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return item.name.contains(q, ignoreCase = true) ||
            item.patientId.contains(q, ignoreCase = true) ||
            (item.username.isNotEmpty() && item.username.contains(q, ignoreCase = true))
    }

    fun matchesFilter(item: PatientListItem, filter: PatientListFilter): Boolean = when (filter) {
        PatientListFilter.ALL -> true
        PatientListFilter.ACTIVE -> item.sessionCount > 0
        PatientListFilter.NEEDS_FOLLOW_UP -> item.status == PatientStatus.NEEDS_FOLLOW_UP
    }

    fun sort(items: List<PatientListItem>, sort: PatientListSort): List<PatientListItem> = when (sort) {
        // Patients without sessions go last; ties fall back to name then id for stability.
        PatientListSort.MOST_RECENT_SESSION -> items.sortedWith(
            compareByDescending<PatientListItem> { it.lastSession?.endedAtEpochMs ?: Long.MIN_VALUE }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                .thenBy { it.patientId },
        )
        PatientListSort.NAME_ASC -> items.sortedWith(
            compareBy(String.CASE_INSENSITIVE_ORDER) { it: PatientListItem -> it.name }
                .thenBy { it.patientId },
        )
    }

    fun apply(
        items: List<PatientListItem>,
        query: String,
        filter: PatientListFilter,
        sort: PatientListSort,
    ): List<PatientListItem> =
        sort(items.filter { matchesQuery(it, query) && matchesFilter(it, filter) }, sort)

    /** Clamps [requestedPage] into range so a narrowed search never shows an empty page. */
    fun page(items: List<PatientListItem>, requestedPage: Int, pageSize: Int = PAGE_SIZE): PatientPage {
        require(pageSize > 0)
        val pageCount = maxOf(1, (items.size + pageSize - 1) / pageSize)
        val index = requestedPage.coerceIn(0, pageCount - 1)
        return PatientPage(
            items = items.drop(index * pageSize).take(pageSize),
            pageIndex = index,
            pageCount = pageCount,
            totalMatches = items.size,
        )
    }

    /** "AP" for "Andi Pratama"; falls back to the first two letters of a single word. */
    fun initials(name: String): String {
        val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when {
            words.isEmpty() -> "?"
            words.size == 1 -> words[0].take(2).uppercase()
            else -> (words[0].take(1) + words[1].take(1)).uppercase()
        }
    }

    /** Figma "Today, 14:20" / "Yesterday, 16:45" / "Oct 13, 10:15" in the device zone. */
    fun formatSessionTime(epochMs: Long, nowEpochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val time = Instant.ofEpochMilli(epochMs).atZone(zone)
        val today = Instant.ofEpochMilli(nowEpochMs).atZone(zone).toLocalDate()
        val clock = time.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH))
        return when (time.toLocalDate()) {
            today -> "Today, $clock"
            today.minusDays(1) -> "Yesterday, $clock"
            else -> time.format(DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.ENGLISH))
        }
    }

    fun overview(items: List<PatientListItem>): CohortOverview = CohortOverview(
        totalPatients = items.size,
        activePatients = items.count { it.sessionCount > 0 },
        needsFollowUp = items.count { it.status == PatientStatus.NEEDS_FOLLOW_UP },
        totalRepetitions = items.sumOf { it.totalRepetitions },
    )
}
