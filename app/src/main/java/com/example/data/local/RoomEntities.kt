package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "users_embeddings")
data class UserFace(
    @PrimaryKey
    val userId: String,
    val name: String,
    val embedding: FloatArray,
    val enrolledAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as UserFace
        if (userId != other.userId) return false
        if (name != other.name) return false
        if (!embedding.contentEquals(other.embedding)) return false
        return enrolledAt == other.enrolledAt
    }

    override fun hashCode(): Int {
        var result = userId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + enrolledAt.hashCode()
        return result
    }
}

@Entity(tableName = "sync_events_queue")
data class SyncEvent(
    @PrimaryKey
    val id: String,
    val type: String, // "enrollment" or "auth"
    val userId: String,
    val name: String,
    val success: Boolean,
    val similarity: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val synced: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

class RoomConverters {
    @TypeConverter
    fun fromFloatArray(array: FloatArray?): String? {
        return array?.joinToString(",")
    }

    @TypeConverter
    fun toFloatArray(string: String?): FloatArray? {
        if (string.isNullOrEmpty()) return null
        return string.split(",").map { it.toFloat() }.toFloatArray()
    }
}
