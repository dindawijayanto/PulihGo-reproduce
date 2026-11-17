package com.pulihgo.app.data.session

import android.content.Context
import org.json.JSONObject
import java.time.OffsetDateTime

/** Where a session result came from, so screens can label seeded demo history honestly. */
enum class SessionSource { SEEDED, DEVICE }

/**
 * One exercise session as shown in Progress, History, Dashboard and Reports.
 * [record.patientId] is always the patient's **user id** (e.g. U002), matching how
 * LiveExerciseScreen saves device sessions.
 */
data class PatientSession(
    val id: String,
    val record: ExerciseSessionRecord,
    val source: SessionSource,
    val targetReps: Int? = null,
    val targetRomDeg: Double? = null,
) {
    val durationMs: Long get() = record.endedAtEpochMs - record.startedAtEpochMs
}

/**
 * Parses the seeded history in `assets/mock_data/sessions.json` (PGF-ALL-03).
 * Seed rows reference Patient ids (P001); they are re-keyed to user ids so they
 * line up with device-saved sessions. Rows for unknown patients are dropped.
 */
object SeededSessionParser {
    fun parse(json: String, userIdByPatientId: Map<String, String>): List<PatientSession> {
        val array = JSONObject(json).optJSONArray("sessions") ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            runCatching {
                val o = array.getJSONObject(index)
                val userId = userIdByPatientId[o.getString("patientId")] ?: return@runCatching null
                val reps = o.optJSONArray("repetitions")
                val details = if (reps == null) emptyList() else (0 until reps.length()).map { r ->
                    val rep = reps.getJSONObject(r)
                    RepetitionMetric(
                        repetitionNumber = rep.getInt("index"),
                        peakRomDeg = rep.getDouble("peakRomDeg"),
                        durationMs = rep.getLong("durationMs"),
                        smoothnessScore = null,
                    )
                }
                val painReported = o.optBoolean("painReported", false)
                val stopped = o.optString("status") == "STOPPED" || o.optString("stopReason") == "PAIN_REPORTED"
                PatientSession(
                    id = o.getString("id"),
                    record = ExerciseSessionRecord(
                        patientId = userId,
                        exerciseId = o.getString("exerciseId"),
                        startedAtEpochMs = OffsetDateTime.parse(o.getString("startedAt")).toInstant().toEpochMilli(),
                        endedAtEpochMs = OffsetDateTime.parse(o.getString("endedAt")).toInstant().toEpochMilli(),
                        repetitions = o.getInt("totalReps"),
                        peakRomDeg = o.getDouble("peakRomDeg"),
                        averageSmoothness = if (o.isNull("averageSmoothness")) null else o.getDouble("averageSmoothness"),
                        painLevel = when {
                            !painReported -> SessionPainLevel.NO_PAIN
                            stopped -> SessionPainLevel.STOPPED_DUE_TO_PAIN
                            else -> SessionPainLevel.MILD_PAIN
                        },
                        repetitionDetails = details,
                    ),
                    source = SessionSource.SEEDED,
                    targetReps = o.optInt("targetReps").takeIf { it > 0 },
                    targetRomDeg = o.optDouble("targetRomDeg").takeIf { it.isFinite() && it > 0 },
                )
            }.getOrNull()
        }
    }
}

object PatientSessionHistory {
    /** Device sessions get a stable id from their user id and start time. */
    fun fromDevice(records: List<ExerciseSessionRecord>): List<PatientSession> =
        records.map { PatientSession(id = "D-${it.patientId}-${it.startedAtEpochMs}", record = it, source = SessionSource.DEVICE) }

    /** All sessions for one user, newest first. */
    fun merge(seeded: List<PatientSession>, device: List<ExerciseSessionRecord>, userId: String): List<PatientSession> =
        (seeded.filter { it.record.patientId == userId } + fromDevice(device.filter { it.patientId == userId }))
            .sortedWith(compareByDescending<PatientSession> { it.record.endedAtEpochMs }.thenBy { it.id })
}

/** Reads the seeded history asset; returns an empty list if it is missing or malformed. */
class SeededSessionSource(private val context: Context) {
    fun load(userIdByPatientId: Map<String, String>): List<PatientSession> = runCatching {
        val json = context.assets.open("mock_data/sessions.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        SeededSessionParser.parse(json, userIdByPatientId)
    }.getOrDefault(emptyList())
}
