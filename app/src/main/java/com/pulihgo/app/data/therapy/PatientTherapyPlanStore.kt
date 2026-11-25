package com.pulihgo.app.data.therapy

import android.content.Context

/** Offline-only backup. Each record is scoped to BOTH local user and cloud patient ID. */
class PatientTherapyPlanStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "pulihgo_patient_therapy_plan_cache_v1", Context.MODE_PRIVATE,
    )

    private fun key(userId: String, patientId: String) = "user_${userId}_patient_$patientId"

    fun read(userId: String, patientId: String): List<PatientTherapyPlan> = runCatching {
        PatientTherapyPlanCodec.decode(prefs.getString(key(userId, patientId), "[]") ?: "[]", patientId)
    }.getOrDefault(emptyList())

    /** Even an empty cached list means a successful cloud check occurred earlier. */
    fun hasSnapshot(userId: String, patientId: String): Boolean = prefs.contains(key(userId, patientId))

    /** A successful empty cloud response clears stale plans; failures must never call this. */
    fun replace(userId: String, patientId: String, plans: List<PatientTherapyPlan>): Boolean {
        if (userId.isBlank() || patientId.isBlank() || plans.any { it.patientId != patientId }) return false
        return prefs.edit().putString(key(userId, patientId), PatientTherapyPlanCodec.encodeCache(plans)).commit()
    }
}
