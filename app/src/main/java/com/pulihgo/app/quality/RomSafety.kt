package com.pulihgo.app.quality

import org.json.JSONObject
import kotlin.math.abs

/** A movement warning threshold, not a clinical safety assessment. */
data class RomSafetyConfig(
    val ceilingDeg: Double,
    val warningMarginDeg: Double = 10.0,
) {
    init {
        require(ceilingDeg.isFinite() && ceilingDeg > 0.0) {
            "ROM ceiling must be finite and greater than 0"
        }
        require(warningMarginDeg.isFinite() && warningMarginDeg >= 0.0 && warningMarginDeg < ceilingDeg) {
            "Warning margin must be finite and between 0 and ceiling"
        }
    }

    val warningStartDeg: Double get() = ceilingDeg - warningMarginDeg

    companion object {
        /** Reads the ceiling for the selected exercise from the existing asset schema. */
        fun fromExercisesJson(json: String, exerciseId: String): RomSafetyConfig {
            val exercises = JSONObject(json).getJSONArray("exercises")
            for (index in 0 until exercises.length()) {
                val exercise = exercises.getJSONObject(index)
                if (exercise.getString("id") == exerciseId) {
                    return RomSafetyConfig(ceilingDeg = exercise.getDouble("defaultRomCeilingDeg"))
                }
            }
            throw IllegalArgumentException("Exercise not found: $exerciseId")
        }
    }
}

enum class RomSafetyLevel {
    WITHIN_LIMIT,
    APPROACHING_LIMIT,
    LIMIT_EXCEEDED,
}

data class RomSafetyStatus(
    val level: RomSafetyLevel,
    val currentRomDeg: Double,
    val ceilingDeg: Double,
    val warningStartDeg: Double,
)

/** Evaluates absolute calibrated ROM for both positive and negative forearm rotation. */
class RomSafetyEvaluator(private val config: RomSafetyConfig) {
    fun evaluate(calibratedAngleDeg: Double?): RomSafetyStatus? {
        if (calibratedAngleDeg == null || !calibratedAngleDeg.isFinite()) return null

        val romDeg = abs(calibratedAngleDeg)
        val level = when {
            romDeg >= config.ceilingDeg -> RomSafetyLevel.LIMIT_EXCEEDED
            romDeg >= config.warningStartDeg -> RomSafetyLevel.APPROACHING_LIMIT
            else -> RomSafetyLevel.WITHIN_LIMIT
        }
        return RomSafetyStatus(level, romDeg, config.ceilingDeg, config.warningStartDeg)
    }
}
