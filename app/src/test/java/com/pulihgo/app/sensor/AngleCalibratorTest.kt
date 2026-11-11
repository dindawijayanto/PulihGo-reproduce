package com.pulihgo.app.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AngleNormalizerTest {
    private val eps = 1e-9

    @Test fun wrapKeepsValuesAlreadyInRange() {
        assertEquals(0.0, AngleNormalizer.wrapDeg180(0.0), eps)
        assertEquals(45.5, AngleNormalizer.wrapDeg180(45.5), eps)
        assertEquals(-179.9, AngleNormalizer.wrapDeg180(-179.9), eps)
        assertEquals(180.0, AngleNormalizer.wrapDeg180(180.0), eps)
    }

    @Test fun wrapMapsBoundaryAndOverflowIntoMinus180To180() {
        assertEquals(180.0, AngleNormalizer.wrapDeg180(-180.0), eps)
        assertEquals(-170.0, AngleNormalizer.wrapDeg180(190.0), eps)
        assertEquals(170.0, AngleNormalizer.wrapDeg180(-190.0), eps)
        assertEquals(-20.0, AngleNormalizer.wrapDeg180(340.0), eps)
        assertEquals(180.0, AngleNormalizer.wrapDeg180(540.0), eps)
        assertEquals(0.0, AngleNormalizer.wrapDeg180(720.0), eps)
        assertEquals(10.0, AngleNormalizer.wrapDeg180(-710.0), eps)
    }

    @Test fun relativeAngleTakesShortWayAcrossPlusMinus180() {
        val offset = Angles(0.0, -170.0, 0.0)
        val rel = AngleNormalizer.relative(Angles(0.0, 160.0, 0.0), offset)
        assertEquals(-30.0, rel.roll, eps)
        assertEquals(30.0, AngleNormalizer.relative(Angles(0.0, -140.0, 0.0), offset).roll, eps)
        assertEquals(-20.0, AngleNormalizer.relative(Angles(0.0, 170.0, 0.0), offset).roll, eps)
    }

    @Test fun circularMeanHandlesTheSeam() {
        assertEquals(180.0, AngleNormalizer.circularMeanDeg(listOf(179.0, -179.0)), 1e-6)
        assertEquals(-175.0, AngleNormalizer.circularMeanDeg(listOf(-170.0, -180.0, 180.0, -170.0)), 1e-6)
        assertEquals(10.0, AngleNormalizer.circularMeanDeg(listOf(5.0, 10.0, 15.0)), 1e-6)
    }

    @Test fun angularDistanceIsShortestPath() {
        assertEquals(2.0, AngleNormalizer.angularDistance(179.0, -179.0), eps)
        assertEquals(180.0, AngleNormalizer.angularDistance(90.0, -90.0), eps)
    }
}

class AngleCalibratorTest {
    private val eps = 1e-6

    private fun feed(calibrator: AngleCalibrator, samples: List<Angles>) =
        samples.map { calibrator.onSample(it) }.last()

    @Test fun startPositionBecomesZero() {
        val c = AngleCalibrator(requiredSamples = 5)
        c.start()
        val result = feed(c, List(5) { Angles(12.0, 33.0, -90.0) })
        assertTrue(result is CalibrationState.Calibrated)
        val rel = c.relative(Angles(12.0, 33.0, -90.0))
        assertEquals(0.0, rel.pitch, eps)
        assertEquals(0.0, rel.roll, eps)
        assertEquals(0.0, rel.yaw, eps)
        assertEquals(60.0, c.relative(Angles(12.0, 93.0, -90.0)).roll, eps)
    }

    @Test fun calibrationAcrossSeamUsesCircularMean() {
        val c = AngleCalibrator(requiredSamples = 4)
        c.start()
        feed(c, listOf(179.0, -179.0, 178.0, -178.0).map { Angles(0.0, it, 0.0) })
        assertEquals(180.0, c.offset!!.roll, eps)
        assertEquals(-1.0, c.relative(Angles(0.0, 179.0, 0.0)).roll, eps)
        assertEquals(1.0, c.relative(Angles(0.0, -179.0, 0.0)).roll, eps)
    }

    @Test fun movementDuringCalibrationIsRejected() {
        val c = AngleCalibrator(requiredSamples = 5, maxSpreadDeg = 4.0)
        c.start()
        val result = feed(c, listOf(0.0, 2.0, 5.0, 10.0, 15.0).map { Angles(0.0, it, 0.0) })
        assertTrue(result is CalibrationState.FailedUnstable)
        assertNull(c.offset)
    }

    @Test fun failedRecalibrationKeepsPreviousOffset() {
        val c = AngleCalibrator(requiredSamples = 3, maxSpreadDeg = 2.0)
        c.start()
        feed(c, List(3) { Angles(0.0, 50.0, 0.0) })
        c.start()
        feed(c, listOf(0.0, 40.0, 80.0).map { Angles(0.0, it, 0.0) })
        assertEquals(10.0, c.relative(Angles(0.0, 60.0, 0.0)).roll, eps)
    }

    @Test fun progressIsReportedWhileCollecting() {
        val c = AngleCalibrator(requiredSamples = 4)
        c.start()
        val state = c.onSample(Angles.ZERO)
        assertEquals(CalibrationState.Collecting(1, 4), state)
    }

    @Test fun samplesIgnoredUnlessCalibrationStarted() {
        val c = AngleCalibrator(requiredSamples = 1)
        assertEquals(CalibrationState.NotCalibrated, c.onSample(Angles(0.0, 30.0, 0.0)))
        assertEquals(-170.0, c.relative(Angles(0.0, 190.0, 0.0)).roll, eps)
    }

    @Test fun resetClearsOffset() {
        val c = AngleCalibrator(requiredSamples = 1, initialOffset = Angles(0.0, 20.0, 0.0))
        assertEquals(10.0, c.relative(Angles(0.0, 30.0, 0.0)).roll, eps)
        c.reset()
        assertEquals(CalibrationState.NotCalibrated, c.state)
        assertEquals(30.0, c.relative(Angles(0.0, 30.0, 0.0)).roll, eps)
    }
}
