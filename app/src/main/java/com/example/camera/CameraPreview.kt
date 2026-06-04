package com.example.camera

import android.annotation.SuppressLint
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executors

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CameraPreview(
    onFacesDetected: (List<Face>, android.graphics.Bitmap?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Executor for parsing camera frame analyses
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Face detector options enabling full classifications (smile, eye blink) and landmarks
    val detectorOptions = remember {
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.12f)
            .build()
    }

    val faceDetector = remember(detectorOptions) {
        FaceDetection.getClient(detectorOptions)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            try {
                faceDetector.close()
            } catch (e: Exception) {
                Log.e("CameraPreview", "Error closing face detector: ${e.message}")
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                // Request FRONT camera for selfie biometrics
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()

                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }

                // Setup analysis setting backpressure strategy to keep UI perfectly fluid
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy: ImageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )

                        faceDetector.process(image)
                            .addOnSuccessListener { faces ->
                                var croppedFace: android.graphics.Bitmap? = null
                                if (faces.isNotEmpty()) {
                                    croppedFace = cropFace(imageProxy, faces.first())
                                }
                                onFacesDetected(faces, croppedFace)
                            }
                            .addOnFailureListener { e ->
                                Log.e("CameraPreview", "Face detection fails: ${e.message}")
                            }
                            .addOnCompleteListener {
                                // MUST close the image proxy to request the next frame!
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                try {
                    // Unbind everything first before rebinding
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (exc: Exception) {
                    Log.e("CameraPreview", "Error initializing CameraX lifecycle binding: ${exc.message}", exc)
                }

            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier.fillMaxSize()
    )
}

private fun cropFace(imageProxy: ImageProxy, face: Face): android.graphics.Bitmap? {
    try {
        val originalBitmap = imageProxy.toBitmap() ?: return null
        val rotation = imageProxy.imageInfo.rotationDegrees

        // Rotate the entire source bitmap to upright orientation first so its dimensions and coordinates
        // align perfectly with the ML Kit coordinates (which are based on the rotated InputImage).
        val rotatedBitmap = if (rotation != 0) {
            val matrix = android.graphics.Matrix()
            matrix.postRotate(rotation.toFloat())
            android.graphics.Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
        } else {
            originalBitmap
        }

        val box = face.boundingBox
        val left = box.left.coerceIn(0, rotatedBitmap.width - 1)
        val top = box.top.coerceIn(0, rotatedBitmap.height - 1)
        val width = box.width().coerceAtMost(rotatedBitmap.width - left).coerceAtLeast(1)
        val height = box.height().coerceAtMost(rotatedBitmap.height - top).coerceAtLeast(1)

        val cropped = android.graphics.Bitmap.createBitmap(rotatedBitmap, left, top, width, height)

        // Recycle rotatedBitmap if it was newly created and different from originalBitmap
        if (rotatedBitmap !== originalBitmap) {
            rotatedBitmap.recycle()
        }
        originalBitmap.recycle()

        return android.graphics.Bitmap.createScaledBitmap(cropped, 112, 112, true)
    } catch (e: Exception) {
        Log.e("CameraPreview", "Error cropping face: ${e.message}", e)
        return null
    }
}
