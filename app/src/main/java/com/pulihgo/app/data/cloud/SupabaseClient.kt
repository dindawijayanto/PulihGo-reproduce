package com.pulihgo.app.data.cloud

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class SupabaseConfig(val url: String, val publishableKey: String) {
    val isConfigured: Boolean get() = url.startsWith("https://") && publishableKey.isNotBlank()
}

data class CloudTokens(val accessToken: String, val refreshToken: String)

class CloudHttpException(val code: Int, message: String) : IOException(message)

class SupabaseClient(private val config: SupabaseConfig) {
    fun signIn(email: String, password: String): CloudTokens =
        tokens(post("/auth/v1/token?grant_type=password", JSONObject().put("email", email).put("password", password).toString(), null))

    fun refresh(refreshToken: String): CloudTokens =
        tokens(post("/auth/v1/token?grant_type=refresh_token", JSONObject().put("refresh_token", refreshToken).toString(), null))

    fun insertSessions(accessToken: String, rows: JSONArray) {
        post(
            "/rest/v1/exercise_sessions?on_conflict=id",
            rows.toString(),
            accessToken,
            mapOf("Prefer" to "resolution=ignore-duplicates,return=minimal"),
        )
    }

    private fun tokens(body: String): CloudTokens {
        val json = JSONObject(body)
        return CloudTokens(json.getString("access_token"), json.getString("refresh_token"))
    }

    private fun post(path: String, body: String, accessToken: String?, headers: Map<String, String> = emptyMap()): String {
        val connection = URL(config.url.trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty("apikey", config.publishableKey)
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer ${accessToken ?: config.publishableKey}")
            headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw CloudHttpException(code, response.take(300))
            return response
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000
    }
}
