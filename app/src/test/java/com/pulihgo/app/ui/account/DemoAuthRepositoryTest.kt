package com.pulihgo.app.ui.account

import org.junit.Assert.*
import org.junit.Test

class DemoAuthRepositoryTest {
    @Test fun demoAccountSignsIn() {
        val repo = DemoAuthRepository()
        assertEquals("Budi Santoso", repo.login("PATIENT@PULIHGO.DEMO", "Demo12345!")?.name)
        assertNull(repo.login(DemoAuthRepository.DEMO_EMAIL, "wrong"))
    }

    @Test fun demoRegistrationCanSignIn() {
        val repo = DemoAuthRepository()
        assertTrue(repo.register("Test User", "test@example.com", "password123"))
        assertFalse(repo.register("Other", "TEST@example.com", "password123"))
        assertEquals("Test User", repo.login("test@example.com", "password123")?.name)
    }

    @Test fun registrationValidation() {
        assertNotNull(AccountValidation.validateRegistration("A", "bad", "123", "123", false))
        assertEquals("Passwords do not match.", AccountValidation.validateRegistration("Test User", "test@example.com", "password123", "abc12345", true))
        assertNull(AccountValidation.validateRegistration("Test User", "test@example.com", "password123", "password123", true))
        assertEquals("Password must contain at least one number or symbol.",
            AccountValidation.validateRegistration("Test User", "test@example.com", "onlyletters", "onlyletters", true))
        assertTrue(AccountValidation.containsNumberOrSymbol("pass12345"))
        assertTrue(AccountValidation.containsNumberOrSymbol("password!"))
        assertFalse(AccountValidation.containsNumberOrSymbol("lettersOnly"))
    }
}
