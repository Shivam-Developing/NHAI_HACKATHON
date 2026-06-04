package com.example.liveness

import com.google.mlkit.vision.face.Face

enum class LivenessChallenge(val instruction: String, val emoji: String) {
    BLINK("Blink your eyes", "👁️"),
    SMILE("Smile nicely", "😊"),
    TURN_LEFT("Turn your head left", "⬅️"),
    TURN_RIGHT("Turn your head right", "➡️")
}

class LivenessDetector {
    private var currentChallenge: LivenessChallenge = LivenessChallenge.BLINK
    private var startTimeMillis: Long = 0
    private val timeoutDurationMillis: Long = 7000

    fun startNewChallenge(): LivenessChallenge {
        val challenges = LivenessChallenge.values()
        currentChallenge = challenges.random()
        startTimeMillis = System.currentTimeMillis()
        return currentChallenge
    }

    fun startNewChallengeExcluding(exclude: LivenessChallenge?): LivenessChallenge {
        val challenges = LivenessChallenge.values().filter { it != exclude }
        currentChallenge = if (challenges.isNotEmpty()) challenges.random() else LivenessChallenge.values().random()
        startTimeMillis = System.currentTimeMillis()
        return currentChallenge
    }

    fun resetChallengeTimer() {
        startTimeMillis = System.currentTimeMillis()
    }

    fun getCurrentChallenge(): LivenessChallenge = currentChallenge

    fun getInstructionText(): String {
        return "${currentChallenge.emoji} ${currentChallenge.instruction}"
    }

    fun isTimedOut(): Boolean {
        return (System.currentTimeMillis() - startTimeMillis) > timeoutDurationMillis
    }

    fun getRemainingTimePercent(): Float {
        val elapsed = System.currentTimeMillis() - startTimeMillis
        return (1.0f - (elapsed.toFloat() / timeoutDurationMillis.toFloat())).coerceIn(0.0f, 1.0f)
    }

    /**
     * Evaluates whether the user passes the active challenge using ML Kit Face coordinates.
     */
    fun evaluateFace(face: Face): Boolean {
        return when (currentChallenge) {
            LivenessChallenge.BLINK -> {
                val leftOpen = face.leftEyeOpenProbability ?: 1.0f
                val rightOpen = face.rightEyeOpenProbability ?: 1.0f
                leftOpen < 0.35f && rightOpen < 0.35f
            }
            LivenessChallenge.SMILE -> {
                val smileProb = face.smilingProbability ?: 0.0f
                smileProb > 0.60f
            }
            LivenessChallenge.TURN_LEFT -> {
                val yaw = face.headEulerAngleY // Left is positive Y yaw
                yaw > 18.0f
            }
            LivenessChallenge.TURN_RIGHT -> {
                val yaw = face.headEulerAngleY // Right is negative Y yaw
                yaw < -18.0f
            }
        }
    }

    /**
     * Checks if the face is in high quality, aligned frontward, and eyes open for secure enrollment.
     */
    fun isFaceSuitableForEnrollment(face: Face): Boolean {
        val yaw = face.headEulerAngleY // horizontal rotation
        val pitch = face.headEulerAngleX // vertical rotation (nod)
        val roll = face.headEulerAngleZ // tilt rotation

        val leftEye = face.leftEyeOpenProbability ?: 1.0f
        val rightEye = face.rightEyeOpenProbability ?: 1.0f

        val aligned = Math.abs(yaw) < 12.0f && Math.abs(pitch) < 12.0f && Math.abs(roll) < 12.0f
        val eyesOpen = leftEye > 0.75f && rightEye > 0.75f

        return aligned && eyesOpen
    }
}
