package com.pulihgo.app.auth

import com.pulihgo.app.data.MockDataParser
import com.pulihgo.app.data.model.Role
import com.pulihgo.app.data.model.User
import com.pulihgo.app.navigation.AppRoute
import com.pulihgo.app.navigation.RoleRouter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MockDataParserTest {
    private fun asset(name: String) = File("src/main/assets/mock_data/$name").readText()

    @Test fun seededUsersParseWithRoles() {
        val users = MockDataParser.parseUsers(asset("users.json"))
        assertEquals(3, users.size)
        assertEquals(Role.THERAPIST, users.first { it.username == "therapist01" }.role)
        assertEquals(2, users.count { it.role == Role.PATIENT })
    }

    @Test fun seededPatientsLinkToUsers() {
        val users = MockDataParser.parseUsers(asset("users.json")).associateBy { it.id }
        val patients = MockDataParser.parsePatients(asset("patients.json"))
        assertTrue(patients.isNotEmpty())
        patients.forEach {
            assertEquals(Role.PATIENT, users.getValue(it.userId).role)
            assertEquals(Role.THERAPIST, users.getValue(it.therapistId).role)
        }
    }

    @Test fun unknownRoleIsSkipped() {
        val json = """{"users":[{"id":"X","username":"x","password":"p","role":"ADMIN","fullName":"X","email":"x@x"}]}"""
        assertTrue(MockDataParser.parseUsers(json).isEmpty())
    }
}

class AuthRepositoryTest {
    private val users = listOf(
        User("U001", "therapist01", "demo123", Role.THERAPIST, "Demo Therapist", "therapist@pulihgo.demo", true),
        User("U002", "patient01", "demo123", Role.PATIENT, "Alya Putri", "alya@pulihgo.demo", true),
        User("U009", "inactive01", "demo123", Role.PATIENT, "Off", "off@pulihgo.demo", false),
    )
    private val repo = AuthRepository(users)

    private fun roleOf(result: LoginResult) = (result as? LoginResult.Success)?.user?.role

    @Test fun eachRoleLogsInWithOwnAccount() {
        assertEquals(Role.THERAPIST, roleOf(repo.login("therapist01", "demo123")))
        assertEquals(Role.PATIENT, roleOf(repo.login("patient01", "demo123")))
    }

    @Test fun emailAndCaseInsensitiveIdentifierWork() {
        assertEquals(Role.PATIENT, roleOf(repo.login("  ALYA@pulihgo.demo ", "demo123")))
        assertEquals(Role.THERAPIST, roleOf(repo.login("Therapist01", "demo123")))
    }

    @Test fun wrongPasswordUnknownUserAndBlankFieldsFail() {
        assertTrue(repo.login("patient01", "DEMO123") is LoginResult.Failure)
        assertTrue(repo.login("nobody", "demo123") is LoginResult.Failure)
        assertTrue(repo.login("", "") is LoginResult.Failure)
    }

    @Test fun inactiveAccountCannotLoginOrRestore() {
        assertEquals(LoginResult.Failure("Akun ini tidak aktif."), repo.login("inactive01", "demo123"))
        assertNull(repo.findActiveUser("U009"))
        assertEquals("Alya Putri", repo.findActiveUser("U002")?.fullName)
    }
}

class RoleRouterTest {
    @Test fun loginLandsOnRoleHome() {
        assertEquals(AppRoute.PATIENT_HOME, RoleRouter.homeFor(Role.PATIENT))
        assertEquals(AppRoute.THERAPIST_HOME, RoleRouter.homeFor(Role.THERAPIST))
    }

    @Test fun guestIsSentToLogin() {
        AppRoute.entries.filterNot { it.isPublic }.forEach {
            assertEquals(AppRoute.LOGIN, RoleRouter.resolve(it, null))
        }
        assertEquals(AppRoute.LOGIN, RoleRouter.resolve(AppRoute.LOGIN, null))
    }

    @Test fun roleCannotOpenOtherRolesScreens() {
        assertEquals(AppRoute.PATIENT_HOME, RoleRouter.resolve(AppRoute.THERAPIST_HOME, Role.PATIENT))
        assertEquals(AppRoute.THERAPIST_HOME, RoleRouter.resolve(AppRoute.PATIENT_HOME, Role.THERAPIST))
        assertEquals(AppRoute.THERAPIST_HOME, RoleRouter.resolve(AppRoute.SENSOR_CHECK, Role.THERAPIST))
        assertFalse(RoleRouter.canAccess(AppRoute.THERAPIST_HOME, Role.PATIENT))
    }

    @Test fun loggedInUserSkipsLoginAndOpensAllowedScreens() {
        assertEquals(AppRoute.PATIENT_HOME, RoleRouter.resolve(AppRoute.LOGIN, Role.PATIENT))
        assertEquals(AppRoute.SENSOR_CHECK, RoleRouter.resolve(AppRoute.SENSOR_CHECK, Role.PATIENT))
    }

    @Test fun everyProtectedRouteBelongsToSomeRole() {
        AppRoute.entries.filterNot { it.isPublic }.forEach { route ->
            assertTrue(route.name, Role.entries.any { RoleRouter.canAccess(route, it) })
        }
    }
}
