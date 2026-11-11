package com.pulihgo.app.auth

import android.content.Context
import com.pulihgo.app.data.model.User

/** Hasil percobaan login. Pesan error ditampilkan apa adanya di layar login. */
sealed interface LoginResult {
    data class Success(val user: User) : LoginResult
    data class Failure(val message: String) : LoginResult
}

/**
 * Demo authentication berdasarkan role (PGF-ALL-04).
 *
 * - Sumber akun: assets/mock_data/users.json (PGF-ALL-03).
 * - Identifier boleh berupa username ATAU email (case-insensitive), sehingga
 *   LoginScreen Orie yang berbasis email tetap bisa memakai repository ini.
 * - Password dibandingkan apa adanya (case-sensitive).
 * - Ini BUKAN autentikasi produksi: tidak ada hashing dan tidak ada token.
 */
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

    /** Dipakai untuk memulihkan sesi login dari [SessionStore] saat aplikasi dibuka ulang. */
    fun findActiveUser(userId: String): User? = users.firstOrNull { it.id == userId && it.isActive }

    /** Akun demo yang ditampilkan sebagai petunjuk di layar login. */
    fun demoAccounts(): List<User> = users.filter { it.isActive }
}

/**
 * Menyimpan HANYA id user yang sedang login (bukan password) supaya aplikasi
 * langsung membuka Home sesuai role setelah ditutup lalu dibuka lagi.
 */
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
