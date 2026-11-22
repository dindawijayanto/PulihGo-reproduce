package com.pulihgo.app.ui.therapist

import com.pulihgo.app.data.model.Role
import com.pulihgo.app.data.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TherapistSettingsLogicTest {
    private val therapist = User("U001", "therapist01", "demo123", Role.THERAPIST, "Demo Therapist", "therapist@pulihgo.demo", true)
    private val saved = TherapistSettingsLogic.defaultProfile(therapist)

    @Test fun defaultProfileComesFromDemoAccount() {
        assertEquals("Demo Therapist", saved.fullName)
        assertEquals("therapist@pulihgo.demo", saved.email)
        assertEquals(TherapistSettingsLogic.DEFAULT_SPECIALIZATION, saved.specialization)
        assertEquals(TherapistSettingsLogic.DEFAULT_AFFILIATION, saved.affiliation)
    }

    @Test fun validProfilePasses() {
        assertNull(TherapistSettingsLogic.validate(saved.copy(fullName = "Dr. Carl Manfred")))
    }

    @Test fun invalidFieldsReturnFirstError() {
        assertEquals("Enter a full name (at least 3 characters).", TherapistSettingsLogic.validate(saved.copy(fullName = " a ")))
        assertEquals("Enter a valid professional email.", TherapistSettingsLogic.validate(saved.copy(email = "carl@clinic")))
        assertEquals("Enter a clinical specialization.", TherapistSettingsLogic.validate(saved.copy(specialization = "  ")))
        assertEquals("Enter a clinical affiliation.", TherapistSettingsLogic.validate(saved.copy(affiliation = "")))
    }

    @Test fun normalizeTrimsAndLowercasesEmail() {
        val edited = saved.copy(fullName = "  Carl   Manfred ", email = " Carl@Clinic.ORG ", specialization = " Hand ")
        val normalized = TherapistSettingsLogic.normalize(edited)
        assertEquals("Carl Manfred", normalized.fullName)
        assertEquals("carl@clinic.org", normalized.email)
        assertEquals("Hand", normalized.specialization)
    }

    @Test fun whitespaceOnlyEditsAreNotChanges() {
        assertFalse(TherapistSettingsLogic.hasChanges(saved, saved.copy(fullName = " Demo Therapist ")))
        assertFalse(TherapistSettingsLogic.hasChanges(saved, saved.copy(email = "THERAPIST@pulihgo.demo")))
        assertTrue(TherapistSettingsLogic.hasChanges(saved, saved.copy(affiliation = "RS Pulih")))
    }

    @Test fun initialsHandleOneOrManyWords() {
        assertEquals("CM", TherapistSettingsLogic.initials("Carl Manfred"))
        assertEquals("DT", TherapistSettingsLogic.initials(" demo  therapist x"))
        assertEquals("AL", TherapistSettingsLogic.initials("alya"))
        assertEquals("?", TherapistSettingsLogic.initials("  "))
    }

    @Test fun patientLoadLabelIsPluralised() {
        assertEquals("No assigned patients", TherapistSettingsLogic.patientLoadLabel(0))
        assertEquals("1 Assigned Patient", TherapistSettingsLogic.patientLoadLabel(1))
        assertEquals("8 Assigned Patients", TherapistSettingsLogic.patientLoadLabel(8))
    }
}
