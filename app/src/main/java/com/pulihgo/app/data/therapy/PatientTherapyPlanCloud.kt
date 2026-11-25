package com.pulihgo.app.data.therapy

import com.pulihgo.app.data.cloud.CloudHttpException
import com.pulihgo.app.data.cloud.SupabaseConfig
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Read-only PostgREST client. No service-role key, no direct write, RLS remains authoritative. */
class PatientTherapyPlanCloud(private val config: SupabaseConfig) {
    suspend fun readForPatient(patientId: String, accessToken: String): List<PatientTherapyPlan> =
        withContext(Dispatchers.IO) {
            require(config.isConfigured && patientId.isNotBlank() && accessToken.isNotBlank())
            val encodedId = URLEncoder.encode(patientId, "UTF-8")
            val path = "/rest/v1/therapy_plans?patient_id=eq.$encodedId" +
                "&select=patient_id,exercise_id,target_rom_deg,rom_ceiling_deg,target_reps," +
                "sets_per_session,sessions_per_week,notes,is_active&order=exercise_id.asc"
            val connection = URL(config.url.trimEnd('/') + path).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                connection.setRequestProperty("apikey", config.publishableKey)
                connection.setRequestProperty("Authorization", "Bearer $accessToken")
                connection.setRequestProperty("Accept", "application/json")
                val status = connection.responseCode
                val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (status !in 200..299) throw CloudHttpException(status, body.take(300))
                PatientTherapyPlanCodec.decode(body, patientId)
            } finally {
                connection.disconnect()
            }
        }
}
