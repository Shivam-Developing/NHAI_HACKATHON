package com.example.data.local

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import kotlinx.coroutines.runBlocking

class FaceAuthContentProvider : ContentProvider() {
    private lateinit var database: AppDatabase

    override fun onCreate(): Boolean {
        val ctx = context ?: return false
        database = AppDatabase.getDatabase(ctx)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        return when (uriMatcher.match(uri)) {
            USERS -> {
                val cursor = MatrixCursor(arrayOf("userId", "name", "enrolledAt", "embedding"))
                runBlocking {
                    try {
                        val users = database.userFaceDao().getAllUsers()
                        users.forEach { user ->
                            cursor.addRow(arrayOf(
                                user.userId,
                                user.name,
                                user.enrolledAt,
                                user.embedding.joinToString(",")
                            ))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                cursor
            }
            EVENTS -> {
                val cursor = MatrixCursor(arrayOf("id", "type", "userId", "name", "success", "similarity", "timestamp", "synced", "latitude", "longitude"))
                runBlocking {
                    try {
                        val events = database.syncEventDao().getAllEvents()
                        events.forEach { event ->
                            cursor.addRow(arrayOf(
                                event.id,
                                event.type,
                                event.userId,
                                event.name,
                                if (event.success) 1 else 0,
                                event.similarity,
                                event.timestamp,
                                if (event.synced) 1 else 0,
                                event.latitude,
                                event.longitude
                            ))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                cursor
            }
            else -> null
        }
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            USERS -> "vnd.android.cursor.dir/com.example.provider.users"
            EVENTS -> "vnd.android.cursor.dir/com.example.provider.events"
            else -> null
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    companion object {
        private const val AUTHORITY = "com.example.provider"
        private const val USERS = 1
        private const val EVENTS = 2

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "users", USERS)
            addURI(AUTHORITY, "events", EVENTS)
        }
    }
}
