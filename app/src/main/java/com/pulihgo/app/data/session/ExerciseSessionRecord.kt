package com.pulihgo.app.data.session

import org.json.JSONArray
import org.json.JSONObject

/** Patient-reported result. STOPPED_DUE_TO_PAIN is distinct from MILD_PAIN. */
enum class SessionPainLevel { NO_PAIN, MILD_PAIN, STOPPED_DUE_TO_PAIN }

/** Repetition event metric for fine-grained exercise analysis across modules. */
data class RepetitionMetric(
    val repetitionNumber: Int,
    val peakRomDeg: Double,
    val durationMs: Long,
    val smoothnessScore: Double? = null,
) {
    init {
        require(repetitionNumber > 0)
        require(peakRomDeg.isFinite() && peakRomDeg >= 0.0)
        require(durationMs >= 0)
        require(smoothnessScore == null || (smoothnessScore.isFinite() && smoothnessScore in 0.0..1.0))
    }
}

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
    val repetitionDetails: List<RepetitionMetric> = emptyList(),
    /** Exact prescription snapshot for this session; absent on historical local records. */
    val targetRomDeg: Double? = null,
    val targetReps: Int? = null,
) {
    val stoppedForPain: Boolean get() = painLevel == SessionPainLevel.STOPPED_DUE_TO_PAIN
    init {
        require(patientId.isNotBlank() && exerciseId.isNotBlank())
        require(startedAtEpochMs > 0 && endedAtEpochMs >= startedAtEpochMs)
        require(repetitions >= 0 && peakRomDeg.isFinite() && peakRomDeg >= 0.0)
        require(averageSmoothness == null || (averageSmoothness.isFinite() && averageSmoothness in 0.0..1.0))
        require(targetRomDeg == null || (targetRomDeg.isFinite() && targetRomDeg > 0 && targetRomDeg < 180))
        require(targetReps == null || targetReps in 1..100)
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
                if (s.targetRomDeg != null) put("targetRomDeg", s.targetRomDeg)
                if (s.targetReps != null) put("targetReps", s.targetReps)
                put("repetitionDetails", JSONArray().apply {
                    s.repetitionDetails.forEach { rep ->
                        put(JSONObject().apply {
                            put("repetitionNumber", rep.repetitionNumber)
                            put("peakRomDeg", rep.peakRomDeg)
                            put("durationMs", rep.durationMs)
                            put("smoothnessScore", rep.smoothnessScore ?: JSONObject.NULL)
                        })
                    }
                })
            })
        }
    }.toString()

    fun decode(json: String): List<ExerciseSessionRecord> {
        val data = JSONArray(json)
        return (0 until data.length()).mapNotNull { index ->
            runCatching {
                val o = data.getJSONObject(index)
                val details = if (o.has("repetitionDetails")) {
                    val detailsArr = o.getJSONArray("repetitionDetails")
                    (0 until detailsArr.length()).mapNotNull { rIdx ->
                        runCatching {
                            val rObj = detailsArr.getJSONObject(rIdx)
                            RepetitionMetric(
                                repetitionNumber = rObj.getInt("repetitionNumber"),
                                peakRomDeg = rObj.getDouble("peakRomDeg"),
                                durationMs = rObj.getLong("durationMs"),
                                smoothnessScore = if (rObj.isNull("smoothnessScore")) null else rObj.getDouble("smoothnessScore"),
                            )
                        }.getOrNull()
                    }
                } else emptyList()

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
                    repetitionDetails = details,
                    targetRomDeg = if (o.has("targetRomDeg") && !o.isNull("targetRomDeg")) o.getDouble("targetRomDeg") else null,
                    targetReps = if (o.has("targetReps") && !o.isNull("targetReps")) o.getInt("targetReps") else null,
                )
            }.getOrNull()
        }
    }
}
