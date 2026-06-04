package com.example.data

import com.example.data.local.SyncEvent
import com.example.data.local.SyncEventDao
import com.example.data.local.UserFace
import com.example.data.local.UserFaceDao
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class FaceAuthRepository(
    private val userFaceDao: UserFaceDao,
    private val syncEventDao: SyncEventDao
) {
    val allUsersFlow: Flow<List<UserFace>> = userFaceDao.getAllUsersFlow()
    val historyFlow: Flow<List<SyncEvent>> = syncEventDao.getHistoryFlow()

    suspend fun getUserFace(userId: String): UserFace? {
        return userFaceDao.getUserFace(userId)
    }

    suspend fun saveUserFace(userId: String, name: String, embedding: FloatArray) {
        val userFace = UserFace(
            userId = userId,
            name = name,
            embedding = embedding,
            enrolledAt = System.currentTimeMillis()
        )
        userFaceDao.insertUserFace(userFace)
    }

    suspend fun deleteUserFace(userId: String) {
        userFaceDao.deleteUserFace(userId)
    }

    suspend fun logEnrollmentEvent(userId: String, name: String, latitude: Double = 0.0, longitude: Double = 0.0) {
        val event = SyncEvent(
            id = UUID.randomUUID().toString(),
            type = "enrollment",
            userId = userId,
            name = name,
            success = true,
            similarity = 1.0f,
            timestamp = System.currentTimeMillis(),
            synced = false,
            latitude = latitude,
            longitude = longitude
        )
        syncEventDao.insertEvent(event)
    }

    suspend fun logAuthEvent(userId: String, name: String, success: Boolean, similarity: Float, latitude: Double = 0.0, longitude: Double = 0.0) {
        val event = SyncEvent(
            id = UUID.randomUUID().toString(),
            type = "auth",
            userId = userId,
            name = name,
            success = success,
            similarity = similarity,
            timestamp = System.currentTimeMillis(),
            synced = false,
            latitude = latitude,
            longitude = longitude
        )
        syncEventDao.insertEvent(event)
    }

    suspend fun getUnsyncedEvents(): List<SyncEvent> {
        return syncEventDao.getUnsyncedEvents()
    }

    suspend fun markEventsSynced(ids: List<String>) {
        if (ids.isNotEmpty()) {
            syncEventDao.markEventsSynced(ids)
        }
    }

    suspend fun clearSyncedEvents() {
        syncEventDao.clearSyncedEvents()
    }
}
