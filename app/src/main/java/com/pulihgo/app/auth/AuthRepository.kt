package com.pulihgo.app.auth

import android.content.Context
import com.pulihgo.app.data.model.Role
import com.pulihgo.app.data.model.User

sealed interface LoginResult {
    data class Success(val user: User) : LoginResult
    data class Failure(val message: String) : LoginResult
}

/**
 * Shared role-based demo authentication.
 * Registered Patient accounts are in-memory only, as in Orie's existing UI demo.
 * Do NOT treat this as production user management or persistent registration.
 */
class AuthRepository(private val users: List<User>) {
    private val registeredPatients = mutableListOf<User>()
    private val allUsers: List<User> get() = users + registeredPatients

    fun login(identifier: String, password: String): LoginResult {
        val key = identifier.trim().lowercase()
        if (key.isEmpty() || password.isEmpty()) {
            return LoginResult.Failure("Username/email dan password wajib diisi.")
        }
        val user = allUsers.firstOrNull {
            it.username.lowercase() == key || it.email.lowercase() == key
        }
        return when {
            user == null || user.password != password ->
                LoginResult.Failure("Username/email atau password salah.")
            !user.isActive -> LoginResult.Failure("Akun ini tidak aktif.")
            else -> LoginResult.Success(user)
        }
    }

    fun registerDemoPatient(name: String, email: String, password: String): Boolean {
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isEmpty() || allUsers.any { it.email.equals(normalizedEmail, ignoreCase = true) }) {
            return false
        }
        val index = registeredPatients.size + 1
        registeredPatients += User(
            id = "U_LOCAL_$index",
            username = "demo_registered_$index",
            password = password,
            role = Role.PATIENT,
            fullName = name.trim(),
            email = normalizedEmail,
            isActive = true,
        )
        return true
    }

    fun findActiveUser(userId: String): User? = allUsers.firstOrNull { it.id == userId && it.isActive }

    fun demoAccounts(): List<User> = allUsers.filter { it.isActive }
}

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    var currentUserId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) {
            prefs.edit().apply { if (value == null) remove(KEY_USER_ID) else putString(KEY_USER_ID, value) }.apply()
        }

    private companion object {
        const val PREF_FILE = "pulihgo_auth_session"
        const val KEY_USER_ID = "current_user_id"
    }
}
