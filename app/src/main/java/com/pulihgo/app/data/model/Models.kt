package com.pulihgo.app.data.model

/**
 * Role yang dikenal aplikasi. Nilai string-nya harus sama persis dengan field
 * `role` di assets/mock_data/users.json.
 */
enum class Role {
    PATIENT,
    THERAPIST;

    companion object {
        /** Mengembalikan null untuk role yang tidak dikenal, supaya akun rusak tidak bisa login. */
        fun fromValue(value: String): Role? = entries.firstOrNull { it.name == value.trim().uppercase() }
    }
}

/** Satu akun demo dari users.json. */
data class User(
    val id: String,
    val username: String,
    val password: String,
    val role: Role,
    val fullName: String,
    val email: String,
    val isActive: Boolean,
)

/** Profil pasien dari patients.json. `userId` menunjuk ke [User.id], `therapistId` ke therapist. */
data class Patient(
    val id: String,
    val userId: String,
    val therapistId: String,
    val age: Int,
    val affectedArm: String,
    val rehabilitationGoal: String,
    val notes: String,
)
