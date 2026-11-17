package com.pulihgo.app.ui.therapist

import com.pulihgo.app.data.model.Role
import com.pulihgo.app.navigation.AppRoute
import com.pulihgo.app.navigation.RoleRouter
import org.junit.Assert.assertEquals
import org.junit.Test

class TherapistPatientRoutesTest {

    private val therapistRoutes = listOf(
        AppRoute.THERAPIST_HOME,
        AppRoute.THERAPIST_PATIENTS,
        AppRoute.THERAPIST_PATIENT_DETAIL,
        AppRoute.THERAPIST_REPORTS,
    )

    @Test
    fun therapistCanOpenPatientListAndDetail() {
        therapistRoutes.forEach { assertEquals(it, RoleRouter.resolve(it, Role.THERAPIST)) }
    }

    @Test
    fun patientAndGuestAreRedirected() {
        therapistRoutes.forEach {
            assertEquals(AppRoute.PATIENT_HOME, RoleRouter.resolve(it, Role.PATIENT))
            assertEquals(AppRoute.LOGIN, RoleRouter.resolve(it, null))
        }
    }

    @Test
    fun patientSummaryIsPatientOnly() {
        assertEquals(AppRoute.PATIENT_SUMMARY, RoleRouter.resolve(AppRoute.PATIENT_SUMMARY, Role.PATIENT))
        assertEquals(AppRoute.THERAPIST_HOME, RoleRouter.resolve(AppRoute.PATIENT_SUMMARY, Role.THERAPIST))
    }
}
