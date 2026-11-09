package com.pulihgo.app.ui.account

/**
 * Sprint demo-only credentials. This class MUST be replaced by the team's role-aware
 * repository (PGF-ALL-03 / PGF-ALL-04). No password or token is written to disk.
 * Accounts created here are intentionally lost when the app process dies.
 */
class DemoAuthRepository {
    data class Patient(val name: String, val email: String)
    private data class Account(val patient: Patient, val password: String)

    companion object {
        const val DEMO_EMAIL = "patient@pulihgo.demo"
        const val DEMO_PASSWORD = "Demo12345!"
    }

    private val accounts = mutableMapOf(
        DEMO_EMAIL to Account(Patient("Budi Santoso", DEMO_EMAIL), DEMO_PASSWORD)
    )

    fun login(email: String, password: String): Patient? {
        val key = email.trim().lowercase()
        return accounts[key]?.takeIf { it.password == password }?.patient
    }

    fun register(name: String, email: String, password: String): Boolean {
        val key = email.trim().lowercase()
        if (accounts.containsKey(key)) return false
        accounts[key] = Account(Patient(name.trim(), key), password)
        return true
    }
}

object AccountValidation {
    private val emailPattern = Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")

    fun isValidEmail(value: String): Boolean = emailPattern.matches(value.trim())

    // Same password requirement as the Register screen's hint card.
    fun containsNumberOrSymbol(value: String): Boolean = value.any { character ->
        character.isDigit() || (!character.isLetterOrDigit() && !character.isWhitespace())
    }

    fun validateRegistration(
        name: String,
        email: String,
        password: String,
        confirmation: String,
        acceptedTerms: Boolean
    ): String? = when {
        name.trim().length < 3 -> "Enter your full name (at least 3 characters)."
        !isValidEmail(email) -> "Enter a valid email address."
        password.length < 8 -> "Password must be at least 8 characters."
        !containsNumberOrSymbol(password) -> "Password must contain at least one number or symbol."
        password != confirmation -> "Passwords do not match."
        !acceptedTerms -> "Please accept the Terms of Service and Privacy Policy."
        else -> null
    }
}
