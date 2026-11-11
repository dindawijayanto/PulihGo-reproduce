package com.pulihgo.app.data

import android.content.Context
import com.pulihgo.app.data.model.Patient
import com.pulihgo.app.data.model.Role
import com.pulihgo.app.data.model.User
import org.json.JSONObject

/**
 * Membaca seeded dummy data (PGF-ALL-03) dari `app/src/main/assets/mock_data/`.
 * File harus berada di folder assets supaya ikut ter-package ke dalam APK.
 */
class MockDataSource(private val context: Context) {

    fun loadUsers(): List<User> = MockDataParser.parseUsers(readAsset("mock_data/users.json"))

    fun loadPatients(): List<Patient> = MockDataParser.parsePatients(readAsset("mock_data/patients.json"))

    private fun readAsset(path: String): String =
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
}

/** Parser murni (tanpa Context) supaya bisa diuji dengan unit test JVM. */
object MockDataParser {

    /** Entri dengan role yang tidak dikenal dilewati, tidak membuat aplikasi crash. */
    fun parseUsers(json: String): List<User> {
        val array = JSONObject(json).getJSONArray("users")
        return (0 until array.length()).mapNotNull { index ->
            val item = array.getJSONObject(index)
            val role = Role.fromValue(item.getString("role")) ?: return@mapNotNull null
            User(
                id = item.getString("id"),
                username = item.getString("username"),
                password = item.getString("password"),
                role = role,
                fullName = item.getString("fullName"),
                email = item.getString("email"),
                isActive = item.optBoolean("isActive", true),
            )
        }
    }

    fun parsePatients(json: String): List<Patient> {
        val array = JSONObject(json).getJSONArray("patients")
        return (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            Patient(
                id = item.getString("id"),
                userId = item.getString("userId"),
                therapistId = item.getString("therapistId"),
                age = item.getInt("age"),
                affectedArm = item.getString("affectedArm"),
                rehabilitationGoal = item.getString("rehabilitationGoal"),
                notes = item.optString("notes", ""),
            )
        }
    }
}
