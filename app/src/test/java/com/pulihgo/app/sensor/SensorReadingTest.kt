package com.pulihgo.app.sensor

import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class OrientationMathTest {
    private val eps = 1e-3

    /** Quaternion rotasi [deg] mengelilingi sumbu Y perangkat (sumbu roll Android). */
    private fun rollQuaternion(deg: Double, includeW: Boolean = true): FloatArray {
        val half = Math.toRadians(deg) / 2
        return if (includeW) floatArrayOf(0f, sin(half).toFloat(), 0f, cos(half).toFloat())
        else floatArrayOf(0f, sin(half).toFloat(), 0f)
    }

    @Test fun identityRotationIsZero() {
        val a = OrientationMath.fromRotationVector(floatArrayOf(0f, 0f, 0f, 1f))
        assertEquals(0.0, a.pitch, eps); assertEquals(0.0, a.roll, eps); assertEquals(0.0, a.yaw, eps)
    }

    @Test fun rotationAboutYIsRoll() {
        assertEquals(60.0, OrientationMath.fromRotationVector(rollQuaternion(60.0)).roll, eps)
        assertEquals(-45.0, OrientationMath.fromRotationVector(rollQuaternion(-45.0)).roll, eps)
        assertEquals(170.0, OrientationMath.fromRotationVector(rollQuaternion(170.0)).roll, eps)
        assertEquals(0.0, OrientationMath.fromRotationVector(rollQuaternion(60.0)).pitch, eps)
    }

    @Test fun missingWComponentIsReconstructed() {
        // Beberapa perangkat hanya mengirim 3 nilai rotation vector.
        assertEquals(30.0, OrientationMath.fromRotationVector(rollQuaternion(30.0, includeW = false)).roll, eps)
    }

    @Test fun rotationAboutXIsPitch() {
        val half = Math.toRadians(-20.0) / 2
        val a = OrientationMath.fromRotationVector(floatArrayOf(sin(half).toFloat(), 0f, 0f, cos(half).toFloat()))
        assertEquals(20.0, a.pitch, eps) // konvensi getOrientation: pitch = asin(-R[7])
        assertEquals(0.0, a.roll, eps)
    }

    @Test fun gravityFallbackAgreesWithRotationVector() {
        for (deg in listOf(-150.0, -60.0, 0.0, 35.0, 89.0, 170.0)) {
            val t = Math.toRadians(deg)
            val g = floatArrayOf((-sin(t) * 9.81).toFloat(), 0f, (cos(t) * 9.81).toFloat())
            assertEquals("roll $deg", deg, OrientationMath.fromGravity(g)!!.roll, eps)
        }
    }

    @Test fun gravityFallbackRejectsZeroVector() {
        assertNull(OrientationMath.fromGravity(floatArrayOf(0f, 0f, 0f)))
    }
}

class SensorHealthMonitorTest {

    @Test fun waitingThenNoDataAfterStartupTimeout() {
        val m = SensorHealthMonitor(startupTimeoutMs = 2000, stallTimeoutMs = 1000)
        m.onStarted(0)
        assertEquals(SensorHealth.WaitingFirstSample, m.evaluate(1500))
        assertEquals(SensorHealth.NoData, m.evaluate(2500))
    }

    @Test fun streamingReportsSampleRate() {
        val m = SensorHealthMonitor()
        m.onStarted(0)
        for (t in 0L..1000L step 20) m.onSample(t) // 50 Hz
        val h = m.evaluate(1010)
        assertTrue(h is SensorHealth.Streaming)
        assertEquals(50.0, (h as SensorHealth.Streaming).sampleRateHz, 0.5)
    }

    @Test fun stallIsDetectedAndRecovers() {
        val m = SensorHealthMonitor(stallTimeoutMs = 1000)
        m.onStarted(0)
        m.onSample(100); m.onSample(120)
        assertEquals(SensorHealth.Stalled(1380), m.evaluate(1500))
        m.onSample(1520)
        assertTrue(m.evaluate(1530) is SensorHealth.Streaming)
    }

    @Test fun unavailableAndRegistrationFailureWin() {
        assertEquals(SensorHealth.Unavailable, SensorHealthMonitor().apply { markUnavailable() }.evaluate(0))
        assertEquals(SensorHealth.RegistrationFailed, SensorHealthMonitor().apply { markRegistrationFailed() }.evaluate(0))
    }
}

class CalibratedAngleControllerTest {

    private fun controller(source: SensorSource? = SensorSource.GAME_ROTATION_VECTOR) = CalibratedAngleController(
        source = source,
        updates = { emptyFlow() },
        clockMs = { 0L },
        calibrator = AngleCalibrator(requiredSamples = 3),
    )

    private fun reading(roll: Double, t: Long) =
        SensorUpdate.Reading(AngleReading(Angles(0.0, roll, 0.0), t))

    @Test fun noSensorMeansUnavailable() {
        assertEquals(SensorHealth.Unavailable, controller(source = null).state.value.health)
    }

    @Test fun calibrateThenMeasureRelativeAngle() {
        val c = controller()
        c.handle(reading(-170.0, 0))
        c.calibrate()
        repeat(3) { c.handle(reading(-170.0, 20L * (it + 1))) }
        assertTrue(c.state.value.calibration is CalibrationState.Calibrated)
        c.handle(reading(160.0, 100)) // berputar -30° melewati batas ±180°
        assertEquals(-30.0, c.state.value.calibrated!!.roll, 1e-6)
        assertEquals(160.0, c.state.value.raw!!.roll, 1e-6)
    }

    @Test fun accuracyAndRegistrationFailureAreSurfaced() {
        val c = controller()
        c.handle(SensorUpdate.Accuracy(SensorAccuracy.LOW))
        assertEquals(SensorAccuracy.LOW, c.state.value.accuracy)
        c.handle(SensorUpdate.RegistrationFailed)
        assertEquals(SensorHealth.RegistrationFailed, c.state.value.health)
    }
}
