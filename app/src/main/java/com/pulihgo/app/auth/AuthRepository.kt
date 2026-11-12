package com.pulihgo.app.auth

import android.content.Context
import com.pulihgo.app.data.model.User

sealed interface LoginResult {
    data class Success(val user: User) : LoginResult
    data class Failure(val message: String) : LoginResult
}

class AuthRepository(private val users: List<User>) {
    fun login(identifier: String, password: String): LoginResult {
        val key = identifier.trim().lowercase()
        if (key.isEmpty() || password.isEmpty()) {
            return LoginResult.Failure("Username/email dan password wajib diisi.")
        }
        val user = users.firstOrNull { it.username.lowercase() == key || it.email.lowercase() == key }
        return when {
            user == null || user.password != password ->
                LoginResult.Failure("Username/email atau password salah.")
            !user.isActive -> LoginResult.Failure("Akun ini tidak aktif.")
            else -> LoginResult.Success(user)
        }
    }

    fun findActiveUser(userId: String): User? = users.firstOrNull { it.id == userId && it.isActive }

    fun demoAccounts(): List<User> = users.filter { it.isActive }
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
