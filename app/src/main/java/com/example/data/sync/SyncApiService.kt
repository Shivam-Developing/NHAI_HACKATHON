package com.example.data.sync

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class SyncEventJson(
    val id: String,
    val type: String, // "enrollment" or "auth"
    val userId: String,
    val name: String,
    val success: Boolean,
    val similarity: Float,
    val timestamp: Long
)

data class SyncPayload(
    val deviceId: String,
    val appVersion: String,
    val events: List<SyncEventJson>
)

data class SyncResponse(
    val syncedCount: Int,
    val status: String,
    val message: String? = null
)

interface SyncApiService {
    @POST("sync")
    suspend fun syncEvents(
        @Body payload: SyncPayload
    ): Response<SyncResponse>
}
