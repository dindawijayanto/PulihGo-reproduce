package com.pulihgo.app.auth

import com.pulihgo.app.data.model.Role
import com.pulihgo.app.data.model.User
import com.pulihgo.app.navigation.AppRoute
import com.pulihgo.app.navigation.RoleRouter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TherapistLoginTest {
    private val users = listOf(
        User("U001", "therapist01", "demo123", Role.THERAPIST, "Demo Therapist", "therapist@pulihgo.demo", true),
        User("U002", "patient01", "demo123", Role.PATIENT, "Alya Putri", "alya@pulihgo.demo", true),
        User("U008", "therapist02", "demo123", Role.THERAPIST, "Off Duty", "off@pulihgo.demo", false),
    )
    private val repo = AuthRepository(users)

    @Test fun therapistSignsInWithEmailOrUsername() {
        val byEmail = repo.loginTherapist(" Therapist@PulihGo.demo ", "demo123")
        val byUsername = repo.loginTherapist("therapist01", "demo123")
        assertEquals("U001", (byEmail as LoginResult.Success).user.id)
        assertEquals("U001", (byUsername as LoginResult.Success).user.id)
    }

    @Test fun patientAccountIsRejectedFromTherapistPortal() {
        assertEquals(
            LoginResult.Failure("This portal is for therapist accounts only."),
            repo.loginTherapist("alya@pulihgo.demo", "demo123"),
        )
    }

    @Test fun wrongPasswordAndInactiveTherapistFail() {
        assertTrue(repo.loginTherapist("therapist01", "wrong") is LoginResult.Failure)
        assertEquals(LoginResult.Failure("Akun ini tidak aktif."), repo.loginTherapist("therapist02", "demo123"))
    }

    @Test fun patientLoginStillAcceptsTherapistAccount() {
        assertEquals(Role.THERAPIST, (repo.login("therapist01", "demo123") as LoginResult.Success).user.role)
    }
}

class PasswordChangeTest {
    private val repo = AuthRepository(
        listOf(User("U001", "therapist01", "demo123", Role.THERAPIST, "Demo Therapist", "therapist@pulihgo.demo", true)),
    )

    private fun failure(result: PasswordChangeResult) = (result as PasswordChangeResult.Failure).message

    @Test fun successfulChangeReplacesOldPassword() {
        assertEquals(PasswordChangeResult.Success, repo.changePassword("U001", "demo123", "newpass99", "newpass99"))
        assertTrue(repo.loginTherapist("therapist01", "newpass99") is LoginResult.Success)
        assertTrue(repo.loginTherapist("therapist01", "demo123") is LoginResult.Failure)
    }

    @Test fun wrongCurrentPasswordIsRejected() {
        assertEquals("Current password is incorrect.", failure(repo.changePassword("U001", "nope", "newpass99", "newpass99")))
    }

    @Test fun weakOrMismatchedNewPasswordIsRejected() {
        assertEquals("New password must be at least 8 characters.", failure(repo.changePassword("U001", "demo123", "a1", "a1")))
        assertEquals(
            "New password must contain at least one number or symbol.",
            failure(repo.changePassword("U001", "demo123", "onlyletters", "onlyletters")),
        )
        assertEquals("Passwords do not match.", failure(repo.changePassword("U001", "demo123", "newpass99", "newpass98")))
        assertTrue(repo.loginTherapist("therapist01", "demo123") is LoginResult.Success)
    }

    @Test fun reusingCurrentPasswordIsRejected() {
        repo.changePassword("U001", "demo123", "newpass99", "newpass99")
        assertEquals(
            "New password must be different from the current one.",
            failure(repo.changePassword("U001", "newpass99", "newpass99", "newpass99")),
        )
    }

    @Test fun unknownAccountIsRejected() {
        assertEquals("Account not found.", failure(repo.changePassword("U404", "demo123", "newpass99", "newpass99")))
    }
}

class TherapistRoutesTest {
    @Test fun therapistLoginAndForgotArePublic() {
        assertEquals(AppRoute.THERAPIST_LOGIN, RoleRouter.resolve(AppRoute.THERAPIST_LOGIN, null))
        assertEquals(AppRoute.THERAPIST_FORGOT, RoleRouter.resolve(AppRoute.THERAPIST_FORGOT, null))
    }

    @Test fun signedInUserSkipsTherapistLogin() {
        assertEquals(AppRoute.THERAPIST_HOME, RoleRouter.resolve(AppRoute.THERAPIST_LOGIN, Role.THERAPIST))
        assertEquals(AppRoute.PATIENT_HOME, RoleRouter.resolve(AppRoute.THERAPIST_LOGIN, Role.PATIENT))
    }

    @Test fun settingsIsTherapistOnly() {
        assertEquals(AppRoute.THERAPIST_SETTINGS, RoleRouter.resolve(AppRoute.THERAPIST_SETTINGS, Role.THERAPIST))
        assertEquals(AppRoute.PATIENT_HOME, RoleRouter.resolve(AppRoute.THERAPIST_SETTINGS, Role.PATIENT))
        assertEquals(AppRoute.LOGIN, RoleRouter.resolve(AppRoute.THERAPIST_SETTINGS, null))
    }

    @Test fun logoutReturnsToTheRolesOwnLogin() {
        assertEquals(AppRoute.THERAPIST_LOGIN, RoleRouter.loginFor(Role.THERAPIST))
        assertEquals(AppRoute.LOGIN, RoleRouter.loginFor(Role.PATIENT))
        assertEquals(AppRoute.LOGIN, RoleRouter.loginFor(null))
    }
}
