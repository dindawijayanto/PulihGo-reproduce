package com.pulihgo.app.data.cloud

import com.pulihgo.app.data.session.ExerciseSessionRecord
import com.pulihgo.app.data.session.PatientSessionHistory
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

object CloudSessionMapper {
    fun sessionId(record: ExerciseSessionRecord): String =
        PatientSessionHistory.fromDevice(listOf(record)).first().id

    fun toRow(record: ExerciseSessionRecord, patientId: String): JSONObject = JSONObject().apply {
        put("id", sessionId(record))
        put("patient_id", patientId)
        put("exercise_id", record.exerciseId)
        put("started_at", Instant.ofEpochMilli(record.startedAtEpochMs).toString())
        put("ended_at", Instant.ofEpochMilli(record.endedAtEpochMs).toString())
        put("repetitions", record.repetitions)
        put("peak_rom_deg", record.peakRomDeg)
        put("average_smoothness", record.averageSmoothness ?: JSONObject.NULL)
        put("pain_level", record.painLevel.name)
        put("source", "DEVICE")
        put("repetition_details", JSONArray().apply {
            record.repetitionDetails.forEach { rep ->
                put(JSONObject().apply {
                    put("repetitionNumber", rep.repetitionNumber)
                    put("peakRomDeg", rep.peakRomDeg)
                    put("durationMs", rep.durationMs)
                    put("smoothnessScore", rep.smoothnessScore ?: JSONObject.NULL)
                })
            }
        })
    }

    fun pending(records: List<ExerciseSessionRecord>, syncedIds: Set<String>): List<ExerciseSessionRecord> =
        records.filter { sessionId(it) !in syncedIds }.distinctBy { sessionId(it) }
}
