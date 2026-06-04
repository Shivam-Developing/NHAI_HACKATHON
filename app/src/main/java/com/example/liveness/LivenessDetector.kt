package com.example.liveness

import com.google.mlkit.vision.face.Face

enum class LivenessChallenge(val instruction: String, val emoji: String) {
    BLINK("Blink your eyes", "👁️"),
    SMILE("Smile nicely", "😊"),
    TURN_LEFT("Turn your head left", "⬅️"),
    TURN_RIGHT("Turn your head right", "➡️")
}

enum class LivenessStrictness(val displayName: String, val description: String) {
    RELAXED("Relaxed", "More lenient thresholds suited for low light environments"),
    STANDARD("Standard", "Balanced security and biometric convenience (Default)"),
    PARANOID("Paranoid", "Highly strict requirements for maximum secure validation")
}

class LivenessDetector {
    private var currentChallenge: LivenessChallenge = LivenessChallenge.BLINK
    private var startTimeMillis: Long = 0
    private val timeoutDurationMillis: Long = 7000

    private var activeChallenges: Set<LivenessChallenge> = LivenessChallenge.values().toSet()
    private var strictness: LivenessStrictness = LivenessStrictness.STANDARD

    fun setActiveChallenges(challenges: Set<LivenessChallenge>) {
        activeChallenges = challenges.ifEmpty { setOf(LivenessChallenge.BLINK) }
    }

    fun setStrictness(level: LivenessStrictness) {
        strictness = level
    }

    fun getStrictness(): LivenessStrictness = strictness

    fun getActiveChallenges(): Set<LivenessChallenge> = activeChallenges

    fun startNewChallenge(): LivenessChallenge {
        val challenges = activeChallenges.toList()
        currentChallenge = challenges.random()
        startTimeMillis = System.currentTimeMillis()
        return currentChallenge
    }

    fun startNewChallengeExcluding(exclude: LivenessChallenge?): LivenessChallenge {
        val challenges = activeChallenges.filter { it != exclude }
        currentChallenge = if (challenges.isNotEmpty()) challenges.random() else activeChallenges.random()
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
                val blinkThreshold = when (strictness) {
                    LivenessStrictness.RELAXED -> 0.45f
                    LivenessStrictness.STANDARD -> 0.35f
                    LivenessStrictness.PARANOID -> 0.22f
                }
                leftOpen < blinkThreshold && rightOpen < blinkThreshold
            }
            LivenessChallenge.SMILE -> {
                val smileProb = face.smilingProbability ?: 0.0f
                val smileThreshold = when (strictness) {
                    LivenessStrictness.RELAXED -> 0.45f
                    LivenessStrictness.STANDARD -> 0.60f
                    LivenessStrictness.PARANOID -> 0.78f
                }
                smileProb > smileThreshold
            }
            LivenessChallenge.TURN_LEFT -> {
                val yaw = face.headEulerAngleY // Left is positive Y yaw
                val yawThreshold = when (strictness) {
                    LivenessStrictness.RELAXED -> 12.0f
                    LivenessStrictness.STANDARD -> 18.0f
                    LivenessStrictness.PARANOID -> 24.0f
                }
                yaw > yawThreshold
            }
            LivenessChallenge.TURN_RIGHT -> {
                val yaw = face.headEulerAngleY // Right is negative Y yaw
                val yawThreshold = when (strictness) {
                    LivenessStrictness.RELAXED -> -12.0f
                    LivenessStrictness.STANDARD -> -18.0f
                    LivenessStrictness.PARANOID -> -24.0f
                }
                yaw < yawThreshold
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

        val (alignThreshold, eyeThreshold) = when (strictness) {
            LivenessStrictness.RELAXED -> Pair(18.0f, 0.60f)
            LivenessStrictness.STANDARD -> Pair(12.0f, 0.75f)
            LivenessStrictness.PARANOID -> Pair(8.0f, 0.88f)
        }

        val aligned = Math.abs(yaw) < alignThreshold && Math.abs(pitch) < alignThreshold && Math.abs(roll) < alignThreshold
        val eyesOpen = leftEye > eyeThreshold && rightEye > eyeThreshold

        return aligned && eyesOpen
    }
}
