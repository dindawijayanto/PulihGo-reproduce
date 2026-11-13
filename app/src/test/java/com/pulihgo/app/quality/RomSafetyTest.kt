package com.pulihgo.app.quality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RomSafetyTest {
    private val evaluator = RomSafetyEvaluator(RomSafetyConfig(ceilingDeg = 80.0))

    @Test fun withinLimitBelowWarningThreshold() {
        val result = evaluator.evaluate(69.9)!!
        assertEquals(RomSafetyLevel.WITHIN_LIMIT, result.level)
        assertEquals(69.9, result.currentRomDeg, 0.0001)
    }

    @Test fun warnsAtAndImmediatelyBelowCeiling() {
        assertEquals(RomSafetyLevel.APPROACHING_LIMIT, evaluator.evaluate(70.0)!!.level)
        assertEquals(RomSafetyLevel.APPROACHING_LIMIT, evaluator.evaluate(79.99)!!.level)
    }

    @Test fun marksExactlyAtAndBeyondCeiling() {
        assertEquals(RomSafetyLevel.LIMIT_EXCEEDED, evaluator.evaluate(80.0)!!.level)
        assertEquals(RomSafetyLevel.LIMIT_EXCEEDED, evaluator.evaluate(91.0)!!.level)
    }

    @Test fun negativeRotationUsesAbsoluteRom() {
        assertEquals(RomSafetyLevel.WITHIN_LIMIT, evaluator.evaluate(-69.0)!!.level)
        assertEquals(RomSafetyLevel.APPROACHING_LIMIT, evaluator.evaluate(-75.0)!!.level)
        assertEquals(RomSafetyLevel.LIMIT_EXCEEDED, evaluator.evaluate(-81.0)!!.level)
    }

    @Test fun invalidAndAbsentReadingsAreNotInterpretedAsSafe() {
        assertNull(evaluator.evaluate(null))
        assertNull(evaluator.evaluate(Double.NaN))
        assertNull(evaluator.evaluate(Double.POSITIVE_INFINITY))
        assertNull(evaluator.evaluate(Double.NEGATIVE_INFINITY))
    }

    @Test fun invalidConfigIsRejected() {
        for (ceiling in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            try { RomSafetyConfig(ceilingDeg = ceiling); fail("Expected invalid ceiling to be rejected") }
            catch (_: IllegalArgumentException) { /* expected */ }
        }
        for (margin in listOf(-1.0, 80.0, 90.0, Double.NaN)) {
            try { RomSafetyConfig(80.0, margin); fail("Expected invalid margin to be rejected") }
            catch (_: IllegalArgumentException) { /* expected */ }
        }
    }

    @Test fun configReadsCeilingFromMockExerciseSchema() {
        val json = """{
            "exercises": [
                {"id":"OTHER","defaultRomCeilingDeg":65.0},
                {"id":"EX001","defaultRomCeilingDeg":80.0}
            ]
        }""".trimIndent()
        val config = RomSafetyConfig.fromExercisesJson(json, "EX001")
        assertEquals(80.0, config.ceilingDeg, 0.0)
        assertEquals(70.0, config.warningStartDeg, 0.0)
    }

    @Test fun missingExerciseDoesNotSilentlyUseOtherLimit() {
        val json = """{"exercises":[{"id":"OTHER","defaultRomCeilingDeg":60.0}]}"""
        try {
            RomSafetyConfig.fromExercisesJson(json, "EX001")
            fail("Missing exercise must not use a different exercise's limit")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("EX001"))
        }
    }
}
