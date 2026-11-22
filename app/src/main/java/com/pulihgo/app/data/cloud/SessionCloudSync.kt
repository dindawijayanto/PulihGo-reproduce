package com.pulihgo.app.data.cloud

import android.content.Context
import com.pulihgo.app.data.model.User
import com.pulihgo.app.data.session.ExerciseSessionRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException

sealed interface CloudSyncResult {
    data class Synced(val uploaded: Int) : CloudSyncResult
    data object UpToDate : CloudSyncResult
    data object NotConnected : CloudSyncResult
    data object NotConfigured : CloudSyncResult
    data class Offline(val pending: Int) : CloudSyncResult
    data class Failed(val pending: Int, val reason: String) : CloudSyncResult
}

class SessionCloudSync(
    context: Context,
    private val config: SupabaseConfig,
    private val patientIdForUser: (userId: String) -> String?,
    private val readDevice: (userId: String) -> List<ExerciseSessionRecord>,
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
    private val client = SupabaseClient(config)
    private val mutex = Mutex()

    suspend fun connect(user: User, password: String): Boolean = withContext(Dispatchers.IO) {
        if (!config.isConfigured) return@withContext false
        runCatching { client.signIn(user.email, password) }
            .onSuccess { prefs.edit().putString(tokenKey(user.id), it.refreshToken).apply() }
            .isSuccess
    }

    fun disconnect(userId: String) {
        prefs.edit().remove(tokenKey(userId)).apply()
    }

    fun pendingCount(userId: String): Int = CloudSessionMapper.pending(readDevice(userId), syncedIds(userId)).size

    suspend fun syncPending(userId: String): CloudSyncResult = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (!config.isConfigured) return@withContext CloudSyncResult.NotConfigured
            val pending = CloudSessionMapper.pending(readDevice(userId), syncedIds(userId))
            if (pending.isEmpty()) return@withContext CloudSyncResult.UpToDate
            val refreshToken = prefs.getString(tokenKey(userId), null) ?: return@withContext CloudSyncResult.NotConnected
            val patientId = patientIdForUser(userId) ?: return@withContext CloudSyncResult.NotConnected
            try {
                val tokens = try {
                    client.refresh(refreshToken)
                } catch (e: CloudHttpException) {
                    if (e.code in 400..499) {
                        disconnect(userId)
                        return@withContext CloudSyncResult.NotConnected
                    }
                    throw e
                }
                prefs.edit().putString(tokenKey(userId), tokens.refreshToken).apply()
                val rows = JSONArray().apply { pending.forEach { put(CloudSessionMapper.toRow(it, patientId)) } }
                client.insertSessions(tokens.accessToken, rows)
                markSynced(userId, pending.map(CloudSessionMapper::sessionId))
                CloudSyncResult.Synced(pending.size)
            } catch (e: CloudHttpException) {
                CloudSyncResult.Failed(pending.size, "HTTP ${e.code}")
            } catch (e: IOException) {
                CloudSyncResult.Offline(pending.size)
            } catch (e: Exception) {
                CloudSyncResult.Failed(pending.size, e.javaClass.simpleName)
            }
        }
    }

    private fun syncedIds(userId: String): Set<String> = prefs.getStringSet(syncedKey(userId), emptySet()).orEmpty()

    private fun markSynced(userId: String, ids: List<String>) {
        prefs.edit().putStringSet(syncedKey(userId), syncedIds(userId) + ids).apply()
    }

    private fun tokenKey(userId: String) = "refresh_$userId"
    private fun syncedKey(userId: String) = "synced_$userId"

    private companion object {
        const val PREF_FILE = "pulihgo_cloud_sync"
    }
}
