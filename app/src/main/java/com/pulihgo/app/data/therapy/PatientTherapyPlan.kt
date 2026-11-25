package com.pulihgo.app.data.therapy

import com.pulihgo.app.data.exercise.Exercise
import org.json.JSONArray
import org.json.JSONObject

/** Server prescription for one real Supabase patient and one catalog exercise. */
data class PatientTherapyPlan(
    val patientId: String,
    val exerciseId: String,
    val targetRomDeg: Double,
    val romCeilingDeg: Double,
    val targetReps: Int,
    val setsPerSession: Int,
    val sessionsPerWeek: Int,
    val isActive: Boolean,
    val notes: String = "",
) {
    init {
        require(patientId.isNotBlank() && exerciseId.isNotBlank())
        require(targetRomDeg.isFinite() && targetRomDeg > 0 && targetRomDeg < 180)
        require(romCeilingDeg.isFinite() && romCeilingDeg > targetRomDeg && romCeilingDeg <= 180)
        require(targetReps in 1..100 && setsPerSession in 1..10 && sessionsPerWeek in 1..7)
        require(notes.length <= 500)
    }

    /** Target and ceiling are applied together; session start freezes this Exercise copy. */
    fun applyTo(exercise: Exercise): Exercise {
        require(exercise.id == exerciseId)
        return exercise.copy(
            targetRomDeg = targetRomDeg,
            romCeilingDeg = romCeilingDeg,
            targetReps = targetReps,
        )
    }
}

/** Reject any malformed or cross-patient payload as a whole; never use partial prescriptions. */
object PatientTherapyPlanCodec {
    fun decode(json: String, expectedPatientId: String): List<PatientTherapyPlan> {
        require(expectedPatientId.isNotBlank())
        val array = JSONArray(json)
        val rows = (0 until array.length()).map { index ->
            val row = array.getJSONObject(index)
            PatientTherapyPlan(
                patientId = row.getString("patient_id"),
                exerciseId = row.getString("exercise_id"),
                targetRomDeg = row.getDouble("target_rom_deg"),
                romCeilingDeg = row.getDouble("rom_ceiling_deg"),
                targetReps = row.getInt("target_reps"),
                setsPerSession = row.getInt("sets_per_session"),
                sessionsPerWeek = row.getInt("sessions_per_week"),
                isActive = row.getBoolean("is_active"),
                notes = row.optString("notes", ""),
            ).also { require(it.patientId == expectedPatientId) }
        }
        require(rows.map { it.exerciseId }.distinct().size == rows.size) { "Duplicate therapy plans" }
        return rows
    }

    /** Offline cache deliberately excludes free-text clinical notes. */
    fun encodeCache(plans: List<PatientTherapyPlan>): String = JSONArray().apply {
        plans.forEach { p ->
            put(JSONObject().apply {
                put("patient_id", p.patientId)
                put("exercise_id", p.exerciseId)
                put("target_rom_deg", p.targetRomDeg)
                put("rom_ceiling_deg", p.romCeilingDeg)
                put("target_reps", p.targetReps)
                put("sets_per_session", p.setsPerSession)
                put("sessions_per_week", p.sessionsPerWeek)
                put("is_active", p.isActive)
            })
        }
    }.toString()
}
