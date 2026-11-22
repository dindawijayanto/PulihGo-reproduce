package com.pulihgo.app.ui.therapist

import com.pulihgo.app.data.model.User
import com.pulihgo.app.ui.account.AccountValidation

data class TherapistProfile(
    val fullName: String,
    val email: String,
    val specialization: String,
    val affiliation: String,
)

enum class TherapistLanguage(val label: String) {
    ENGLISH("English (Clinical)"),
    INDONESIAN("Bahasa Indonesia"),
}

data class TherapistPreferences(
    val audioAlerts: Boolean = true,
    val autoSync: Boolean = true,
    val language: TherapistLanguage = TherapistLanguage.ENGLISH,
)

object TherapistSettingsLogic {
    const val DEFAULT_SPECIALIZATION = "Upper Limb Rehabilitation"
    const val DEFAULT_AFFILIATION = "PulihGo Digital Tele-Rehab Hub"

    fun defaultProfile(user: User) = TherapistProfile(
        fullName = user.fullName,
        email = user.email,
        specialization = DEFAULT_SPECIALIZATION,
        affiliation = DEFAULT_AFFILIATION,
    )

    fun normalize(profile: TherapistProfile) = TherapistProfile(
        fullName = profile.fullName.trim().replace(Regex("\\s+"), " "),
        email = profile.email.trim().lowercase(),
        specialization = profile.specialization.trim(),
        affiliation = profile.affiliation.trim(),
    )

    fun validate(profile: TherapistProfile): String? {
        val p = normalize(profile)
        return when {
            p.fullName.length < 3 -> "Enter a full name (at least 3 characters)."
            !AccountValidation.isValidEmail(p.email) -> "Enter a valid professional email."
            p.specialization.isEmpty() -> "Enter a clinical specialization."
            p.affiliation.isEmpty() -> "Enter a clinical affiliation."
            else -> null
        }
    }

    fun hasChanges(saved: TherapistProfile, edited: TherapistProfile): Boolean = normalize(edited) != saved

    fun initials(name: String): String {
        val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when {
            words.isEmpty() -> "?"
            words.size == 1 -> words[0].take(2).uppercase()
            else -> "${words[0].first()}${words[1].first()}".uppercase()
        }
    }

    fun patientLoadLabel(count: Int): String = when (count) {
        0 -> "No assigned patients"
        1 -> "1 Assigned Patient"
        else -> "$count Assigned Patients"
    }
}
