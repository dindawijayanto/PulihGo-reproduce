package com.pulihgo.app.data.model

enum class Role {
    PATIENT,
    THERAPIST;

    companion object {
        fun fromValue(value: String): Role? = entries.firstOrNull { it.name == value.trim().uppercase() }
    }
}

data class User(
    val id: String,
    val username: String,
    val password: String,
    val role: Role,
    val fullName: String,
    val email: String,
    val isActive: Boolean,
)

data class Patient(
    val id: String,
    val userId: String,
    val therapistId: String,
    val age: Int,
    val affectedArm: String,
    val rehabilitationGoal: String,
    val notes: String,
)
