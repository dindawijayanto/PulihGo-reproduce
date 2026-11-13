package com.pulihgo.app.data.exercise

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Data latihan demo dari assets; parameter tidak di-hardcode pada UI. */
data class Exercise(
    val id: String,
    val name: String,
    val description: String,
    val targetRomDeg: Double,
    val targetReps: Int,
    val romCeilingDeg: Double,
    val requiresPhoneSensor: Boolean,
    val instructions: List<String>,
    val safetyNotes: List<String>,
)

class ExerciseCatalog(private val context: Context) {
    fun loadExercises(): List<Exercise> = context.assets.open("mock_data/exercises.json")
        .bufferedReader(Charsets.UTF_8).use { ExerciseParser.parse(it.readText()) }
}

/** Pure JSON parser sehingga mudah diuji tanpa Context / Android runtime. */
object ExerciseParser {
    fun parse(json: String): List<Exercise> {
        val entries = JSONObject(json).optJSONArray("exercises") ?: return emptyList()
        return (0 until entries.length()).mapNotNull { index ->
            val item = entries.optJSONObject(index) ?: return@mapNotNull null
            val id = item.optString("id").trim()
            val name = item.optString("name").trim()
            if (id.isEmpty() || name.isEmpty()) return@mapNotNull null
            Exercise(
                id = id,
                name = name,
                description = item.optString("description"),
                targetRomDeg = item.optDouble("defaultTargetRomDeg", 0.0),
                targetReps = item.optInt("defaultTargetReps", 0),
                romCeilingDeg = item.optDouble("defaultRomCeilingDeg", 0.0),
                requiresPhoneSensor = item.optBoolean("requiresPhoneSensor", false),
                instructions = item.optJSONArray("instructions").toStringList(),
                safetyNotes = item.optJSONArray("safetyNotes").toStringList(),
            )
        }
    }
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { i ->
        optString(i).trim().takeIf { it.isNotBlank() }
    }
}
