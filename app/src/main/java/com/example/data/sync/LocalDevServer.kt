package com.example.data.sync

import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.UserFace
import com.example.data.local.SyncEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

class LocalDevServer(private val db: AppDatabase) {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.IO)

    fun start(port: Int = 12345) {
        if (isRunning) return
        isRunning = true
        scope.launch {
            try {
                serverSocket = ServerSocket(port).apply {
                    reuseAddress = true
                }
                Log.i("LocalDevServer", "Local Dev socket-based HTTP Server started on 0.0.0.0:$port")
                while (isRunning) {
                    val socket = serverSocket?.accept() ?: break
                    scope.launch {
                        handleClient(socket)
                    }
                }
            } catch (e: Exception) {
                if (isRunning) {
                    Log.e("LocalDevServer", "Failed or stopped Local Dev Server listener", e)
                }
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // ignore
        }
        serverSocket = null
    }

    fun isRunning(): Boolean {
        return isRunning && serverSocket != null && !serverSocket!!.isClosed
    }

    private suspend fun handleClient(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val firstLine = reader.readLine()
            if (firstLine.isNullOrBlank()) {
                return
            }
            
            val parts = firstLine.split(" ")
            if (parts.size < 2) {
                sendResponse(socket, 400, "Bad Request", "text/plain")
                return
            }
            
            val method = parts[0]
            val path = parts[1]
            
            if (method != "GET" && method != "OPTIONS") {
                sendResponse(socket, 405, "Method Not Allowed", "text/plain")
                return
            }

            if (method == "OPTIONS") {
                // Return standard headers with empty body to handle preflights properly
                sendResponse(socket, 200, "", "text/plain")
                return
            }
            
            when {
                path.startsWith("/api/users") -> {
                    val users = db.userFaceDao().getAllUsers()
                    val json = buildString {
                        append("[")
                        users.forEachIndexed { index, user ->
                            append("{")
                            append("\"userId\":\"${escapeJson(user.userId)}\",")
                            append("\"name\":\"${escapeJson(user.name)}\",")
                            append("\"enrolledAt\":${user.enrolledAt},")
                            append("\"embedding\":[")
                            append(user.embedding.joinToString(","))
                            append("]")
                            append("}")
                            if (index < users.size - 1) append(",")
                        }
                        append("]")
                    }
                    sendResponse(socket, 200, json, "application/json")
                }
                path.startsWith("/api/events") -> {
                    val events = db.syncEventDao().getAllEvents()
                    val json = buildString {
                        append("[")
                        events.forEachIndexed { index, event ->
                            append("{")
                            append("\"id\":\"${escapeJson(event.id)}\",")
                            append("\"type\":\"${escapeJson(event.type)}\",")
                            append("\"userId\":\"${escapeJson(event.userId)}\",")
                            append("\"name\":\"${escapeJson(event.name)}\",")
                            append("\"success\":${event.success},")
                            append("\"similarity\":${event.similarity},")
                            append("\"timestamp\":${event.timestamp},")
                            append("\"synced\":${event.synced},")
                            append("\"latitude\":${event.latitude},")
                            append("\"longitude\":${event.longitude}")
                            append("}")
                            if (index < events.size - 1) append(",")
                        }
                        append("]")
                    }
                    sendResponse(socket, 200, json, "application/json")
                }
                path.startsWith("/api/status") -> {
                    val osName = System.getProperty("os.name") ?: "Android"
                    val arch = System.getProperty("os.arch") ?: "unknown"
                    val json = """
                        {
                            "status": "ONLINE",
                            "component": "Biometric Field Auth Server",
                            "datalakeCompatible": "v3.0",
                            "systemTime": ${System.currentTimeMillis()},
                            "os": "$osName",
                            "architecture": "$arch"
                        }
                    """.trimIndent()
                    sendResponse(socket, 200, json, "application/json")
                }
                else -> {
                    sendResponse(socket, 404, "Not Found", "text/plain")
                }
            }
        } catch (e: Exception) {
            Log.e("LocalDevServer", "Error handling socket request", e)
        } finally {
            try {
                socket.close()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun sendResponse(socket: Socket, statusCode: Int, body: String, contentType: String) {
        try {
            val os: OutputStream = socket.getOutputStream()
            val bytes = body.toByteArray()
            val statusString = when (statusCode) {
                200 -> "200 OK"
                400 -> "400 Bad Request"
                404 -> "404 Not Found"
                405 -> "405 Method Not Allowed"
                else -> "$statusCode Internal Error"
            }
            
            val headers = buildString {
                append("HTTP/1.1 ").append(statusString).append("\r\n")
                append("Content-Type: ").append(contentType).append("; charset=UTF-8\r\n")
                append("Content-Length: ").append(bytes.size).append("\r\n")
                append("Access-Control-Allow-Origin: *\r\n")
                append("Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n")
                append("Access-Control-Allow-Headers: Content-Type,Authorization\r\n")
                append("Connection: close\r\n")
                append("\r\n")
            }
            
            os.write(headers.toByteArray())
            if (bytes.isNotEmpty()) {
                os.write(bytes)
            }
            os.flush()
        } catch (e: Exception) {
            Log.e("LocalDevServer", "Failed to send HTTP response", e)
        }
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
    }
}
