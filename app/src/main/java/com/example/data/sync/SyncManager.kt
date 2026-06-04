package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.FaceAuthRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

class SyncManager(
    context: Context,
    private val repository: FaceAuthRepository
) {
    private val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val syncScope = CoroutineScope(Dispatchers.IO)

    private val _syncStatus = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncStatus: StateFlow<SyncState> = _syncStatus.asStateFlow()

    init {
        // Guarantee stable device ID registration
        if (prefs.getString("device_id", null) == null) {
            prefs.edit().putString("device_id", UUID.randomUUID().toString()).apply()
        }

        // Register robust connectivity callback to automatically sync when internet is available
        try {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            
            connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    if (isAutoSyncEnabled()) {
                        Log.i("SyncManager", "Network connection active! Initiating background automatic sync.")
                        syncScope.launch {
                            performSync()
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("SyncManager", "Could not register network callback for background sync", e)
        }
    }

    fun isAutoSyncEnabled(): Boolean {
        return prefs.getBoolean("bg_auto_sync", true)
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("bg_auto_sync", enabled).apply()
        // If enabling and network is currently available, trigger sync immediately!
        if (enabled && isNetworkAvailable()) {
            syncScope.launch {
                performSync()
            }
        }
    }

    fun isNetworkAvailable(): Boolean {
        return try {
            val activeNet = connectivityManager.activeNetwork ?: return false
            val cap = connectivityManager.getNetworkCapabilities(activeNet) ?: return false
            cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    fun getDeviceId(): String {
        return prefs.getString("device_id", "DESKTOP-MOCK-DEV") ?: "DESKTOP-MOCK-DEV"
    }

    fun getSyncUrl(): String {
        return prefs.getString("sync_endpoint", "https://api.datalake3.aws/prod/") ?: "https://api.datalake3.aws/prod/"
    }

    fun saveSyncUrl(url: String) {
        var cleanUrl = url.trim()
        if (!cleanUrl.endsWith("/")) {
            cleanUrl += "/"
        }
        prefs.edit().putString("sync_endpoint", cleanUrl).apply()
    }

    /**
     * Executes the secure cloud sync sequence over Retrofit.
     * Marks successfully synced database rows as synced = 1 immediately.
     */
    suspend fun performSync(): SyncResult {
        _syncStatus.value = SyncState.Syncing

        val unsyncedList = repository.getUnsyncedEvents()
        if (unsyncedList.isEmpty()) {
            _syncStatus.value = SyncState.Success(0)
            return SyncResult.Success(0, "No new events to sync.")
        }

        val url = getSyncUrl()
        val deviceId = getDeviceId()

        val jsonEvents = unsyncedList.map {
            SyncEventJson(
                id = it.id,
                type = it.type,
                userId = it.userId,
                name = it.name,
                success = it.success,
                similarity = it.similarity,
                timestamp = it.timestamp
            )
        }

        val payload = SyncPayload(
            deviceId = deviceId,
            appVersion = "1.0.0",
            events = jsonEvents
        )

        try {
            // Build real OkHttpClient & Retrofit instances on the fly to support dynamic URL adjustments!
            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(url)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            val service = retrofit.create(SyncApiService::class.java)
            val response = service.syncEvents(payload)

            if (response.isSuccessful && response.body() != null) {
                val responseBody = response.body()!!
                val idsToMark = unsyncedList.map { it.id }
                repository.markEventsSynced(idsToMark)

                _syncStatus.value = SyncState.Success(idsToMark.size)
                return SyncResult.Success(idsToMark.size, responseBody.message ?: "Sync succeeded")
            } else {
                val errMsg = "Server responded with code ${response.code()}: ${response.errorBody()?.string()}"
                Log.e("SyncManager", errMsg)
                _syncStatus.value = SyncState.Error(errMsg)
                return SyncResult.Failure(errMsg)
            }
        } catch (e: Exception) {
            val errMsg = e.message ?: "Unknown network/concurrency exception during sync."
            Log.e("SyncManager", "Network sync failed", e)
            _syncStatus.value = SyncState.Error(errMsg)
            return SyncResult.Failure(errMsg)
        }
    }
}

sealed interface SyncState {
    object Idle : SyncState
    object Syncing : SyncState
    data class Success(val count: Int) : SyncState
    data class Error(val message: String) : SyncState
}

sealed interface SyncResult {
    data class Success(val count: Int, val message: String) : SyncResult
    data class Failure(val error: String) : SyncResult
}
