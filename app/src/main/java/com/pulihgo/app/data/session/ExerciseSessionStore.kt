package com.pulihgo.app.data.session

import android.content.Context

/** Device-local persistence; no backend or patient-wide server sync is implied. */
class ExerciseSessionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "pulihgo_exercise_sessions_v1", Context.MODE_PRIVATE,
    )

    private fun key(patientId: String) = "patient_" + patientId

    fun read(patientId: String): List<ExerciseSessionRecord> =
        runCatching {
            val data = prefs.getString(key(patientId), "[]") ?: "[]"
            ExerciseSessionRecordJson.decode(data).filter { it.patientId == patientId }
        }.getOrDefault(emptyList())

    /** Returns false if the OS fails to persist; do NOT show a success banner then. */
    fun append(record: ExerciseSessionRecord): Boolean = runCatching {
        val raw = prefs.getString(key(record.patientId), "[]") ?: "[]"
        val existing = ExerciseSessionRecordJson.decode(raw)
            .filter { it.patientId == record.patientId }
        prefs.edit().putString(
            key(record.patientId), ExerciseSessionRecordJson.encode(existing + record),
        ).commit()
    }.getOrDefault(false)
}
