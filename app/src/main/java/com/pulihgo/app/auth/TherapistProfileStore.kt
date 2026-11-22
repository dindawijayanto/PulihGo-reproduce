package com.pulihgo.app.auth

import android.content.Context
import com.pulihgo.app.data.model.User
import com.pulihgo.app.ui.therapist.TherapistLanguage
import com.pulihgo.app.ui.therapist.TherapistPreferences
import com.pulihgo.app.ui.therapist.TherapistProfile
import com.pulihgo.app.ui.therapist.TherapistSettingsLogic

class TherapistProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    fun profile(user: User): TherapistProfile {
        val defaults = TherapistSettingsLogic.defaultProfile(user)
        return TherapistProfile(
            fullName = prefs.getString(key(user.id, "fullName"), null) ?: defaults.fullName,
            email = prefs.getString(key(user.id, "email"), null) ?: defaults.email,
            specialization = prefs.getString(key(user.id, "specialization"), null) ?: defaults.specialization,
            affiliation = prefs.getString(key(user.id, "affiliation"), null) ?: defaults.affiliation,
        )
    }

    fun saveProfile(userId: String, profile: TherapistProfile) {
        prefs.edit()
            .putString(key(userId, "fullName"), profile.fullName)
            .putString(key(userId, "email"), profile.email)
            .putString(key(userId, "specialization"), profile.specialization)
            .putString(key(userId, "affiliation"), profile.affiliation)
            .apply()
    }

    fun preferences(userId: String): TherapistPreferences {
        val defaults = TherapistPreferences()
        val language = prefs.getString(key(userId, "language"), null)
            ?.let { name -> TherapistLanguage.entries.firstOrNull { it.name == name } }
        return TherapistPreferences(
            audioAlerts = prefs.getBoolean(key(userId, "audioAlerts"), defaults.audioAlerts),
            autoSync = prefs.getBoolean(key(userId, "autoSync"), defaults.autoSync),
            language = language ?: defaults.language,
        )
    }

    fun savePreferences(userId: String, preferences: TherapistPreferences) {
        prefs.edit()
            .putBoolean(key(userId, "audioAlerts"), preferences.audioAlerts)
            .putBoolean(key(userId, "autoSync"), preferences.autoSync)
            .putString(key(userId, "language"), preferences.language.name)
            .apply()
    }

    private fun key(userId: String, field: String) = "$userId.$field"

    private companion object {
        const val PREF_FILE = "pulihgo_therapist_profile"
    }
}
