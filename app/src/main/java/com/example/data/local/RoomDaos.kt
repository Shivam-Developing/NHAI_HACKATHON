package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserFaceDao {
    @Query("SELECT * FROM users_embeddings")
    fun getAllUsersFlow(): Flow<List<UserFace>>

    @Query("SELECT * FROM users_embeddings")
    suspend fun getAllUsers(): List<UserFace>

    @Query("SELECT * FROM users_embeddings WHERE userId = :userId LIMIT 1")
    suspend fun getUserFace(userId: String): UserFace?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserFace(userFace: UserFace)

    @Query("DELETE FROM users_embeddings WHERE userId = :userId")
    suspend fun deleteUserFace(userId: String)
}

@Dao
interface SyncEventDao {
    @Query("SELECT * FROM sync_events_queue ORDER BY timestamp DESC")
    fun getHistoryFlow(): Flow<List<SyncEvent>>

    @Query("SELECT * FROM sync_events_queue ORDER BY timestamp DESC")
    suspend fun getAllEvents(): List<SyncEvent>

    @Query("SELECT * FROM sync_events_queue WHERE synced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedEvents(): List<SyncEvent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: SyncEvent)

    @Query("UPDATE sync_events_queue SET synced = 1 WHERE id IN (:ids)")
    suspend fun markEventsSynced(ids: List<String>)

    @Query("DELETE FROM sync_events_queue WHERE synced = 1")
    suspend fun clearSyncedEvents()
}
