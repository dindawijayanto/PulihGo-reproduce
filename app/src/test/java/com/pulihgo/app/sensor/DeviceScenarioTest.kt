package com.pulihgo.app.sensor

import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class DeviceScenarioTest {
    private var clock = 0L

    private fun controller(source: SensorSource = SensorSource.GAME_ROTATION_VECTOR) = CalibratedAngleController(
        source = source,
        updates = { emptyFlow() },
        clockMs = { clock },
        calibrator = AngleCalibrator(requiredSamples = 10, maxSpreadDeg = 4.0),
    )

    private fun rollQuaternion(deg: Double): FloatArray {
        val half = Math.toRadians(deg) / 2
        return floatArrayOf(0f, sin(half).toFloat(), 0f, cos(half).toFloat())
    }

    private fun gravityForRoll(deg: Double): FloatArray {
        val rad = Math.toRadians(deg)
        return floatArrayOf((-sin(rad) * 9.81).toFloat(), 0f, (cos(rad) * 9.81).toFloat())
    }

    private fun CalibratedAngleController.phoneAt(rollDeg: Double, source: SensorSource = SensorSource.GAME_ROTATION_VECTOR): Double {
        clock += 20
        val angles = if (source == SensorSource.ACCELEROMETER) {
            OrientationMath.fromGravity(gravityForRoll(rollDeg))!!
        } else {
            OrientationMath.fromRotationVector(rollQuaternion(rollDeg))
        }
        handle(SensorUpdate.Reading(AngleReading(angles, clock)))
        return state.value.calibrated!!.roll
    }

    private fun CalibratedAngleController.calibrateAt(vararg rolls: Double): CalibrationState {
        calibrate()
        rolls.forEach { phoneAt(it) }
        return state.value.calibration
    }

    private fun CalibratedAngleController.holdStill(rollDeg: Double, jitter: Double = 0.0): CalibrationState =
        calibrateAt(*DoubleArray(10) { rollDeg + if (it % 2 == 0) jitter else -jitter })

    private fun rotation(startDeg: Double, peakDeg: Double, steps: Int = 20): List<Double> =
        (0..steps).map { startDeg + (peakDeg - startDeg) * sin(Math.PI * it / steps) }

    @Test fun restingForearmBecomesZeroDegrees() {
        val c = controller()
        assertTrue(c.holdStill(25.0) is CalibrationState.Calibrated)
        assertEquals(0.0, c.phoneAt(25.0), 0.05)
        assertEquals(40.0, c.phoneAt(65.0), 0.05)
    }

    @Test fun smallHandTremorStillCalibrates() {
        val c = controller()
        assertTrue(c.holdStill(10.0, jitter = 1.5) is CalibrationState.Calibrated)
        assertEquals(0.0, c.phoneAt(10.0), 0.1)
    }

    @Test fun movingArmDuringCalibrationIsRejectedAndKeepsOldZero() {
        val c = controller()
        c.holdStill(0.0)
        val failed = c.calibrateAt(*DoubleArray(10) { it * 3.0 })
        assertTrue(failed is CalibrationState.FailedUnstable)
        assertEquals(30.0, c.phoneAt(30.0), 0.05)
    }

    @Test fun recalibrationMovesTheZeroPoint() {
        val c = controller()
        c.holdStill(0.0)
        assertEquals(-20.0, c.phoneAt(-20.0), 0.05)
        c.holdStill(-20.0)
        assertEquals(0.0, c.phoneAt(-20.0), 0.05)
        assertEquals(50.0, c.phoneAt(30.0), 0.05)
    }

    @Test fun supinationAndPronationHaveOppositeSigns() {
        val c = controller()
        c.holdStill(0.0)
        val supination = c.phoneAt(60.0)
        val pronation = c.phoneAt(-60.0)
        assertEquals(60.0, supination, 0.05)
        assertEquals(-60.0, pronation, 0.05)
    }

    @Test fun angleStaysContinuousAcrossThePlusMinus180Seam() {
        val c = controller()
        c.holdStill(170.0)
        val readings = listOf(175.0, -179.0, -170.0, -150.0).map { c.phoneAt(it) }
        assertEquals(listOf(5.0, 11.0, 20.0, 40.0), readings.map { Math.round(it).toDouble() })
        assertEquals(-20.0, c.phoneAt(150.0), 0.05)
    }

    @Test fun fullSessionShowsBaselineTargetHitAndCeiling() {
        val c = controller()
        c.holdStill(15.0)
        val tracker = LiveExerciseTracker(targetRomDeg = 70.0, ceilingDeg = 80.0)
        val phases = mutableListOf<LivePhase>()

        fun play(angles: List<Double>) = angles.forEach { raw ->
            phases += tracker.onAngle(c.phoneAt(raw), clock).phase
        }

        play(rotation(15.0, 15.0 + 50.0))
        assertEquals(LivePhase.BASELINE, tracker.snapshot.phase)
        assertEquals(1, tracker.snapshot.repetitions)

        play(rotation(15.0, 15.0 + 75.0))
        assertTrue(LivePhase.TARGET_HIT in phases)
        assertEquals(2, tracker.snapshot.repetitions)

        play(rotation(15.0, 15.0 - 90.0))
        assertTrue(LivePhase.CEILING_EXCEEDED in phases)
        assertEquals(3, tracker.snapshot.repetitions)
        assertEquals(90.0, tracker.snapshot.peakRomDeg, 0.5)
        assertEquals(LivePhase.BASELINE, tracker.snapshot.phase)
    }

    @Test fun jitterAroundRestDoesNotCountRepetitions() {
        val c = controller()
        c.holdStill(0.0)
        val tracker = LiveExerciseTracker(targetRomDeg = 70.0, ceilingDeg = 80.0)
        repeat(200) { tracker.onAngle(c.phoneAt(if (it % 2 == 0) 4.0 else -4.0), clock) }
        assertEquals(0, tracker.snapshot.repetitions)
        assertEquals(LivePhase.BASELINE, tracker.snapshot.phase)
    }

    @Test fun accelerometerFallbackGivesTheSameRelativeRoll() {
        val c = controller(SensorSource.ACCELEROMETER)
        c.calibrate()
        repeat(10) { c.phoneAt(20.0, SensorSource.ACCELEROMETER) }
        assertTrue(c.state.value.calibration is CalibrationState.Calibrated)
        assertEquals(45.0, c.phoneAt(65.0, SensorSource.ACCELEROMETER), 0.1)
        assertEquals(-45.0, c.phoneAt(-25.0, SensorSource.ACCELEROMETER), 0.1)
    }

    @Test fun sensorStallDuringExerciseIsReportedThenRecovers() {
        val monitor = SensorHealthMonitor(startupTimeoutMs = 2_000, stallTimeoutMs = 1_000)
        monitor.onStarted(0)
        (1..50).forEach { monitor.onSample(it * 20L) }
        assertTrue(monitor.evaluate(1_010) is SensorHealth.Streaming)
        assertEquals(50.0, monitor.sampleRateHz(), 0.5)
        assertEquals(SensorHealth.Stalled(1_500), monitor.evaluate(2_500))
        monitor.onSample(2_520)
        assertTrue(monitor.evaluate(2_530) is SensorHealth.Streaming)
    }
}
