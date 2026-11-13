package com.pulihgo.app.data.exercise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseCatalogTest {
    @Test
    fun parsesForearmExerciseAndPreparationInstructions() {
        val exercises = ExerciseParser.parse("""
            {"exercises":[{
              "id":"EX001",
              "name":"Forearm Supination and Pronation",
              "description":"Forearm rotation rehabilitation",
              "defaultTargetRomDeg":70.0,
              "defaultTargetReps":10,
              "defaultRomCeilingDeg":80.0,
              "requiresPhoneSensor":true,
              "instructions":["Keep elbow stable", "Rotate slowly"],
              "safetyNotes":["Stop if pain occurs"]
            }]}
        """.trimIndent())
        assertEquals(1, exercises.size)
        val exercise = exercises.single()
        assertEquals("EX001", exercise.id)
        assertEquals("Forearm Supination and Pronation", exercise.name)
        assertEquals(70.0, exercise.targetRomDeg, 0.0)
        assertEquals(10, exercise.targetReps)
        assertEquals(80.0, exercise.romCeilingDeg, 0.0)
        assertTrue(exercise.requiresPhoneSensor)
        assertEquals(listOf("Keep elbow stable", "Rotate slowly"), exercise.instructions)
        assertEquals(listOf("Stop if pain occurs"), exercise.safetyNotes)
    }

    @Test
    fun skipsIncompleteEntriesAndHandlesMissingExerciseList() {
        assertTrue(ExerciseParser.parse("{}").isEmpty())
        val exercises = ExerciseParser.parse("""{"exercises":[{"id":"", "name":"No ID"}, {"id":"EX2", "name":"Valid"}]}""")
        assertEquals(1, exercises.size)
        assertEquals("EX2", exercises.single().id)
        assertTrue(exercises.single().instructions.isEmpty())
        assertFalse(exercises.single().requiresPhoneSensor)
    }
}
