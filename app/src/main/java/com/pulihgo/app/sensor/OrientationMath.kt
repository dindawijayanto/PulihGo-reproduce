package com.pulihgo.app.sensor

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.sqrt

enum class Axis { PITCH, ROLL, YAW }

data class Angles(val pitch: Double, val roll: Double, val yaw: Double) {
    operator fun get(axis: Axis): Double = when (axis) {
        Axis.PITCH -> pitch
        Axis.ROLL -> roll
        Axis.YAW -> yaw
    }

    companion object {
        val ZERO = Angles(0.0, 0.0, 0.0)
    }
}

object OrientationMath {
    private const val RAD_TO_DEG = 180.0 / Math.PI

    fun fromRotationVector(values: FloatArray): Angles {
        require(values.size >= 3) { "Rotation vector needs at least 3 values" }
        val x = values[0].toDouble()
        val y = values[1].toDouble()
        val z = values[2].toDouble()
        val w = if (values.size >= 4) {
            values[3].toDouble()
        } else {
            (1.0 - x * x - y * y - z * z).let { if (it > 0) sqrt(it) else 0.0 }
        }

        val r1 = 2 * x * y - 2 * z * w
        val r4 = 1 - 2 * x * x - 2 * z * z
        val r6 = 2 * x * z - 2 * y * w
        val r7 = 2 * y * z + 2 * x * w
        val r8 = 1 - 2 * x * x - 2 * y * y

        return Angles(
            pitch = asin((-r7).coerceIn(-1.0, 1.0)) * RAD_TO_DEG,
            roll = atan2(-r6, r8) * RAD_TO_DEG,
            yaw = atan2(r1, r4) * RAD_TO_DEG,
        )
    }

    fun fromGravity(values: FloatArray): Angles? {
        require(values.size >= 3) { "Accelerometer needs 3 values" }
        val gx = values[0].toDouble()
        val gy = values[1].toDouble()
        val gz = values[2].toDouble()
        val norm = sqrt(gx * gx + gy * gy + gz * gz)
        if (norm < 1e-3) return null
        return Angles(
            pitch = asin((-gy / norm).coerceIn(-1.0, 1.0)) * RAD_TO_DEG,
            roll = atan2(-gx, gz) * RAD_TO_DEG,
            yaw = 0.0,
        )
    }
}
