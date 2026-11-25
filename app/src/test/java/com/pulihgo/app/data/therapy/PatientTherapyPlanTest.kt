package com.pulihgo.app.data.therapy

import com.pulihgo.app.data.exercise.Exercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PatientTherapyPlanTest {
    private fun json(patient: String = "P001", target: Double = 60.0, ceiling: Double = 75.0, active: Boolean = true) =
        """[{"patient_id":"$patient","exercise_id":"EX001","target_rom_deg":$target,"rom_ceiling_deg":$ceiling,"target_reps":8,"sets_per_session":2,"sessions_per_week":4,"is_active":$active,"notes":"Take it slowly"}]"""

    private fun catalog() = Exercise("EX001", "Rotation", "Demo", 70.0, 10, 80.0, true, emptyList(), emptyList())

    @Test fun validatesRemoteAndAppliesPatientSpecificTarget() {
        val p = PatientTherapyPlanCodec.decode(json(), "P001").single()
        val applied = p.applyTo(catalog())
        assertEquals(60.0, applied.targetRomDeg, 0.0)
        assertEquals(75.0, applied.romCeilingDeg, 0.0)
        assertEquals(8, applied.targetReps)
        assertEquals(70.0, catalog().targetRomDeg, 0.0)
    }

    @Test fun pausesExerciseAndPreservesStatusInOfflineCache() {
        val p = PatientTherapyPlanCodec.decode(json(active = false), "P001").single()
        assertFalse(p.isActive)
        val cached = PatientTherapyPlanCodec.decode(PatientTherapyPlanCodec.encodeCache(listOf(p)), "P001").single()
        assertFalse(cached.isActive)
        assertTrue(cached.notes.isEmpty())
        assertEquals(2, cached.setsPerSession)
    }

    @Test fun emptyResponseIsValidAndClearsPlanList() {
        assertTrue(PatientTherapyPlanCodec.decode("[]", "P001").isEmpty())
    }

    @Test fun crossPatientResponseIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            PatientTherapyPlanCodec.decode(json(patient = "P002"), "P001")
        }
    }

    @Test fun invalidCeilingCannotOverwriteGoodPlan() {
        assertThrows(IllegalArgumentException::class.java) {
            PatientTherapyPlanCodec.decode(json(ceiling = 55.0), "P001")
        }
    }

    @Test fun duplicatePlansAreRejected() {
        val single = json().removePrefix("[").removeSuffix("]")
        assertThrows(IllegalArgumentException::class.java) {
            PatientTherapyPlanCodec.decode("[$single,$single]", "P001")
        }
    }

    @Test fun missingFieldsAndNonnumericValuesFailClosed() {
        assertThrows(Exception::class.java) {
            PatientTherapyPlanCodec.decode("""[{"patient_id":"P001","exercise_id":"EX001"}]""", "P001")
        }
        assertThrows(Exception::class.java) { PatientTherapyPlanCodec.decode("not json", "P001") }
    }

    @Test fun wrongExerciseIdCannotBeApplied() {
        val p = PatientTherapyPlanCodec.decode(json(), "P001").single()
        assertThrows(IllegalArgumentException::class.java) {
            p.applyTo(catalog().copy(id = "EX999"))
        }
    }
}
