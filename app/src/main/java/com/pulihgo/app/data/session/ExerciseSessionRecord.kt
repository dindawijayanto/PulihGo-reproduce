package com.pulihgo.app.data.session

import org.json.JSONArray
import org.json.JSONObject

/** Patient-reported result. STOPPED_DUE_TO_PAIN is distinct from MILD_PAIN. */
enum class SessionPainLevel { NO_PAIN, MILD_PAIN, STOPPED_DUE_TO_PAIN }

/** Values represent genuine calibrated sensor measurements from this session only. */
data class ExerciseSessionRecord(
    val patientId: String,
    val exerciseId: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long,
    val repetitions: Int,
    val peakRomDeg: Double,
    val averageSmoothness: Double?,
    val painLevel: SessionPainLevel,
) {
    val stoppedForPain: Boolean get() = painLevel == SessionPainLevel.STOPPED_DUE_TO_PAIN
    init {
        require(patientId.isNotBlank() && exerciseId.isNotBlank())
        require(startedAtEpochMs > 0 && endedAtEpochMs >= startedAtEpochMs)
        require(repetitions >= 0 && peakRomDeg.isFinite() && peakRomDeg >= 0.0)
        require(averageSmoothness == null || (averageSmoothness.isFinite() && averageSmoothness in 0.0..1.0))
    }
}

/** Pure codec: JVM-unit-testable independently of Android SharedPreferences. */
object ExerciseSessionRecordJson {
    fun encode(sessions: List<ExerciseSessionRecord>): String = JSONArray().apply {
        sessions.forEach { s ->
            put(JSONObject().apply {
                put("patientId", s.patientId)
                put("exerciseId", s.exerciseId)
                put("startedAtEpochMs", s.startedAtEpochMs)
                put("endedAtEpochMs", s.endedAtEpochMs)
                put("repetitions", s.repetitions)
                put("peakRomDeg", s.peakRomDeg)
                put("averageSmoothness", s.averageSmoothness ?: JSONObject.NULL)
                put("painLevel", s.painLevel.name)
            })
        }
    }.toString()

    fun decode(json: String): List<ExerciseSessionRecord> {
        val data = JSONArray(json)
        return (0 until data.length()).mapNotNull { index ->
            runCatching {
                val o = data.getJSONObject(index)
                ExerciseSessionRecord(
                    patientId = o.getString("patientId"),
                    exerciseId = o.getString("exerciseId"),
                    startedAtEpochMs = o.getLong("startedAtEpochMs"),
                    endedAtEpochMs = o.getLong("endedAtEpochMs"),
                    repetitions = o.getInt("repetitions"),
                    peakRomDeg = o.getDouble("peakRomDeg"),
                    averageSmoothness = if (o.isNull("averageSmoothness")) null
                        else o.getDouble("averageSmoothness"),
                    painLevel = SessionPainLevel.valueOf(o.getString("painLevel")),
                )
            }.getOrNull()
        }
    }
}
