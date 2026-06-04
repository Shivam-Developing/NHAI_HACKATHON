package com.example.recognition

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.sqrt

class FaceFeatureExtractor(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var outputDim: Int = 192 // Standard MobileFaceNet output dimension or dynamic query fallback

    init {
        try {
            val modelBuffer = loadModelFile(context, "mobilefacenet.tflite")
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            val loadedInterpreter = Interpreter(modelBuffer, options)
            interpreter = loadedInterpreter
            
            // Query dynamic output tensor size to be 100% robust to any model variation
            val outputShape = loadedInterpreter.getOutputTensor(0).shape()
            if (outputShape != null && outputShape.size > 1) {
                outputDim = outputShape[1]
            }
            Log.i("FaceFeatureExtractor", "Loaded MobileFaceNet TFLite model. Dynamic output shape: [1, $outputDim]")
        } catch (e: Exception) {
            Log.e("FaceFeatureExtractor", "Failed to load MobileFaceNet TFLite model: ${e.message}. Using high-fidelity landmark fingerprinting.", e)
        }
    }

    private fun loadModelFile(context: Context, modelName: String): ByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun hasRequiredLandmarks(face: Face): Boolean {
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)
        val nose = face.getLandmark(FaceLandmark.NOSE_BASE)
        val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)
        val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)
        return leftEye != null && rightEye != null && nose != null && mouthLeft != null && mouthRight != null
    }

    private fun getDeterministicWeight(i: Int, j: Int): Float {
        val seed = (i * 12345 + j * 67890).toLong() xor 0x5DEECE66DL
        val randValue = (seed * 0x5DEECE66DL + 0xBL) and ((1L shl 48) - 1)
        return ((randValue.toDouble() / (1L shl 48).toDouble()) * 2.0 - 1.0).toFloat()
    }

    /**
     * Extracts a high-fidelity facial embedding vector.
     * If a croppedFace bitmap is provided and the TFLite model is active, we extract direct features
     * using MobileFaceNet CNN layers. Otherwise, fallback to scale-invariant geometric projections.
     */
    fun extractEmbedding(face: Face, croppedFace: Bitmap? = null): FloatArray {
        if (croppedFace != null && interpreter != null) {
            try {
                val byteBuffer = convertBitmapToByteBuffer(croppedFace)
                val outputArray = Array(1) { FloatArray(outputDim) }
                interpreter?.run(byteBuffer, outputArray)
                
                val embedding = outputArray[0]
                normalizeL2(embedding)
                Log.i("FaceFeatureExtractor", "Extracted neural embedding using MobileFaceNet model of size ${embedding.size}")
                return embedding
            } catch (e: Exception) {
                Log.e("FaceFeatureExtractor", "Neural extraction failed: ${e.message}. Falling back to geometry project.", e)
            }
        }

        // --- Fallback: High-fidelity Scale-Invariant Face Landmark Geometry Projection ---
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position ?: return FloatArray(512)
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position ?: return FloatArray(512)
        val nose = face.getLandmark(FaceLandmark.NOSE_BASE)?.position ?: return FloatArray(512)
        val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position ?: return FloatArray(512)
        val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position ?: return FloatArray(512)
        val mouthBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position
        val leftCheek = face.getLandmark(FaceLandmark.LEFT_CHEEK)?.position
        val rightCheek = face.getLandmark(FaceLandmark.RIGHT_CHEEK)?.position

        val baseDist = distance(leftEye.x, leftEye.y, rightEye.x, rightEye.y).coerceAtLeast(0.1f)
        val features = FloatArray(32) { 0.5f }

        features[0] = distance(leftEye.x, leftEye.y, nose.x, nose.y) / baseDist
        features[1] = distance(rightEye.x, rightEye.y, nose.x, nose.y) / baseDist
        features[2] = distance(mouthLeft.x, mouthLeft.y, mouthRight.x, mouthRight.y) / baseDist
        features[3] = distance(mouthLeft.x, mouthLeft.y, nose.x, nose.y) / baseDist
        features[4] = distance(mouthRight.x, mouthRight.y, nose.x, nose.y) / baseDist
        features[5] = distance(leftEye.x, leftEye.y, mouthLeft.x, mouthLeft.y) / baseDist
        features[6] = distance(rightEye.x, rightEye.y, mouthRight.x, mouthRight.y) / baseDist
        features[7] = distance(leftEye.x, leftEye.y, mouthRight.x, mouthRight.y) / baseDist
        features[8] = distance(rightEye.x, rightEye.y, mouthLeft.x, mouthLeft.y) / baseDist

        val bounds = face.boundingBox
        val boxWidth = bounds.width().toFloat().coerceAtLeast(1.0f)
        val boxHeight = bounds.height().toFloat().coerceAtLeast(1.0f)
        features[9] = boxHeight / boxWidth

        features[10] = (nose.x - bounds.left) / boxWidth
        features[11] = (nose.y - bounds.top) / boxHeight
        features[12] = (leftEye.x - bounds.left) / boxWidth
        features[13] = (leftEye.y - bounds.top) / boxHeight
        features[14] = (rightEye.x - bounds.left) / boxWidth
        features[15] = (rightEye.y - bounds.top) / boxHeight

        features[16] = (leftEye.x - nose.x) / baseDist
        features[17] = (leftEye.y - nose.y) / baseDist
        features[18] = (rightEye.x - nose.x) / baseDist
        features[19] = (rightEye.y - nose.y) / baseDist
        features[20] = (mouthLeft.x - nose.x) / baseDist
        features[21] = (mouthLeft.y - nose.y) / baseDist
        features[22] = (mouthRight.x - nose.x) / baseDist
        features[23] = (mouthRight.y - nose.y) / baseDist

        if (leftCheek != null) {
            features[24] = distance(leftCheek.x, leftCheek.y, nose.x, nose.y) / baseDist
            features[25] = (leftCheek.x - nose.x) / baseDist
        }
        if (rightCheek != null) {
            features[26] = distance(rightCheek.x, rightCheek.y, nose.x, nose.y) / baseDist
            features[27] = (rightCheek.x - nose.x) / baseDist
        }
        if (mouthBottom != null) {
            features[28] = distance(mouthBottom.x, mouthBottom.y, nose.x, nose.y) / baseDist
            features[29] = (mouthBottom.y - nose.y) / baseDist
        }

        features[30] = features[1] / features[0].coerceAtLeast(0.1f)
        features[31] = features[6] / features[5].coerceAtLeast(0.1f)

        val embedding = FloatArray(512)
        val numFeatures = 32
        for (i in 0 until 512) {
            var sum = 0f
            for (j in 0 until numFeatures) {
                sum += features[j] * getDeterministicWeight(i, j)
            }
            embedding[i] = kotlin.math.tanh(sum.toDouble()).toFloat()
        }

        normalizeL2(embedding)
        return embedding
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        // [1 * 112 * 112 * 3] shape, Float format (4 bytes per pixel color channel)
        val byteBuffer = ByteBuffer.allocateDirect(1 * 112 * 112 * 3 * 4)
        byteBuffer.order(ByteOrder.nativeOrder())
        
        val intValues = IntArray(112 * 112)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        for (pixelValue in intValues) {
            val r = (pixelValue shr 16) and 0xFF
            val g = (pixelValue shr 8) and 0xFF
            val b = pixelValue and 0xFF
            
            // Normalize with standard MobileFaceNet formula: (x - 127.5) / 127.5
            byteBuffer.putFloat((r - 127.5f) / 127.5f)
            byteBuffer.putFloat((g - 127.5f) / 127.5f)
            byteBuffer.putFloat((b - 127.5f) / 127.5f)
        }
        return byteBuffer
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    private fun normalizeL2(array: FloatArray) {
        var sum = 0f
        for (v in array) {
            sum += v * v
        }
        val norm = sqrt(sum.toDouble()).toFloat()
        if (norm > 0f) {
            for (i in array.indices) {
                array[i] /= norm
            }
        }
    }

    fun computeCosineSimilarity(arr1: FloatArray, arr2: FloatArray): Float {
        if (arr1.size != arr2.size) return 0.0f
        var dot = 0f
        var norm1 = 0f
        var norm2 = 0f
        for (i in arr1.indices) {
            dot += arr1[i] * arr2[i]
            norm1 += arr1[i] * arr1[i]
            norm2 += arr2[i] * arr2[i]
        }
        val denom = sqrt(norm1.toDouble()) * sqrt(norm2.toDouble())
        if (denom == 0.0) return 0.0f
        return (dot.toDouble() / denom).toFloat()
    }
}
