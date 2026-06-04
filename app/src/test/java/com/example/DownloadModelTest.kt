package com.example

import org.junit.Test
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class DownloadModelTest {
    @Test
    fun downloadMobileFaceNet() {
        println("CURRENT WORKING DIRECTORY = ${System.getProperty("user.dir")}")
        val baseDir = File(System.getProperty("user.dir"))
        
        // Find the main folder
        var srcMainDir = File(baseDir, "app/src/main")
        if (!srcMainDir.exists()) {
            srcMainDir = File(baseDir, "src/main")
        }
        if (!srcMainDir.exists()) {
            var parent = baseDir
            while (parent != null && !srcMainDir.exists()) {
                val check = File(parent, "app/src/main")
                if (check.exists()) {
                    srcMainDir = check
                    break
                }
                val checkDirect = File(parent, "src/main")
                if (checkDirect.exists()) {
                    srcMainDir = checkDirect
                    break
                }
                parent = parent.parentFile
            }
        }
        
        assert(srcMainDir.exists()) { "Could not find src/main folder in ${baseDir.absolutePath}" }
        
        val assetsDir = File(srcMainDir, "assets")
        if (!assetsDir.exists()) {
            val created = assetsDir.mkdirs()
            println("Created assets directory: $created inside ${assetsDir.absolutePath}")
        }
        
        val targetFile = File(assetsDir, "mobilefacenet.tflite")
        println("Target model path: ${targetFile.absolutePath}")
        
        val candidates = listOf(
            "https://raw.githubusercontent.com/MCarlomagno/FaceRecognitionAuth/master/assets/mobilefacenet.tflite",
            "https://raw.githubusercontent.com/ngtrphuong/facerecognition/master/assets/mobilefacenet.tflite",
            "https://raw.githubusercontent.com/shubham0204/Face_Recognition_with_TensorFlow_Lite-Android/main/app/src/main/assets/mobilefacenet.tflite",
            "https://raw.githubusercontent.com/shubham0204/Face_Recognition_with_TensorFlow_Lite-Android/master/app/src/main/assets/mobilefacenet.tflite"
        )
        
        var downloaded = false
        var lastError: Exception? = null
        
        for (urlString in candidates) {
            try {
                println("Trying directly from: $urlString")
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 30000
                conn.readTimeout = 30000
                conn.requestMethod = "GET"
                
                val responseCode = conn.responseCode
                println("Response code for $urlString -> $responseCode")
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val contentLength = conn.contentLengthLong
                    println("Content length: $contentLength")
                    BufferedInputStream(conn.getInputStream()).use { input ->
                        FileOutputStream(targetFile).use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var totalBytes = 0L
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                totalBytes += bytesRead
                            }
                            println("Success! Downloaded $totalBytes bytes of mobilefacenet.tflite")
                        }
                    }
                    downloaded = true
                    break
                }
            } catch (e: Exception) {
                println("Failed to download from $urlString due to: ${e.message}")
                lastError = e
            }
        }
        
        if (!downloaded) {
            throw lastError ?: RuntimeException("Could not download mobilefacenet.tflite from any candidate URL.")
        }
        
        assert(targetFile.exists() && targetFile.length() > 100000) { "Download succeeded but file is missing or too small: ${targetFile.length()} bytes" }
    }
}
