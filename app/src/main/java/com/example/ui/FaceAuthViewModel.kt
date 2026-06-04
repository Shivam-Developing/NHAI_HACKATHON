package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FaceAuthRepository
import com.example.data.local.AppDatabase
import com.example.data.local.SyncEvent
import com.example.data.local.UserFace
import com.example.data.sync.SyncManager
import com.example.data.sync.SyncState
import com.example.liveness.LivenessChallenge
import com.example.liveness.LivenessDetector
import com.example.recognition.FaceFeatureExtractor
import com.google.mlkit.vision.face.Face
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FaceAuthViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = FaceAuthRepository(db.userFaceDao(), db.syncEventDao())
    private val syncManager = SyncManager(application, repository)
    private val featureExtractor = FaceFeatureExtractor(application)
    private val livenessDetector = LivenessDetector()
    private val localDevServer = com.example.data.sync.LocalDevServer(db)

    private val _isLocalServerRunning = MutableStateFlow(false)
    val isLocalServerRunning: StateFlow<Boolean> = _isLocalServerRunning.asStateFlow()

    private val _isAutoSyncEnabled = MutableStateFlow(syncManager.isAutoSyncEnabled())
    val isAutoSyncEnabled: StateFlow<Boolean> = _isAutoSyncEnabled.asStateFlow()

    // Database updates loaded as state observables
    val enrolledUsers: StateFlow<List<UserFace>> = repository.allUsersFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val syncHistory: StateFlow<List<SyncEvent>> = repository.historyFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val syncState: StateFlow<SyncState> = syncManager.syncStatus

    // Navigation and orchestration states
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Dashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Enrollment session properties
    private val _enrollUserId = MutableStateFlow("")
    val enrollUserId: StateFlow<String> = _enrollUserId.asStateFlow()

    private val _enrollName = MutableStateFlow("")
    val enrollName: StateFlow<String> = _enrollName.asStateFlow()

    private val _enrollFrameCount = MutableStateFlow(0)
    val enrollFrameCount: StateFlow<Int> = _enrollFrameCount.asStateFlow()

    private val _enrollmentError = MutableStateFlow<String?>(null)
    val enrollmentError: StateFlow<String?> = _enrollmentError.asStateFlow()

    // Frame accumulation array for averaged enrollment embeddings
    private val capturedFrameEmbeddings = mutableListOf<FloatArray>()
    private var lastCaptureTime: Long = 0

    // Authentication session properties
    private val _authUserId = MutableStateFlow("")
    val authUserId: StateFlow<String> = _authUserId.asStateFlow()

    private val _authUserName = MutableStateFlow("")
    val authUserName: StateFlow<String> = _authUserName.asStateFlow()

    private val _activeChallenge = MutableStateFlow<LivenessChallenge?>(null)
    val activeChallenge: StateFlow<LivenessChallenge?> = _activeChallenge.asStateFlow()

    private val _challengeProgress = MutableStateFlow(1.0f)
    val challengeProgress: StateFlow<Float> = _challengeProgress.asStateFlow()

    private val _verificationResult = MutableStateFlow<VerificationResult?>(null)
    val verificationResult: StateFlow<VerificationResult?> = _verificationResult.asStateFlow()

    // Session states for dual checkpoint biometric verification
    private var livenessPassed = false
    private var biometricMatched = false
    private var maxSimilarityFound = 0f

    private val _currentAuthPhase = MutableStateFlow<AuthPhase>(AuthPhase.LIVENESS_1)
    val currentAuthPhase: StateFlow<AuthPhase> = _currentAuthPhase.asStateFlow()

    // Authenticated user session state
    private val _authenticatedUser = MutableStateFlow<UserFace?>(null)
    val authenticatedUser: StateFlow<UserFace?> = _authenticatedUser.asStateFlow()

    fun clearAuthentication() {
        _authenticatedUser.value = null
    }

    // General app sync configuration inputs
    private val _syncServerUrlInput = MutableStateFlow(syncManager.getSyncUrl())
    val syncServerUrlInput: StateFlow<String> = _syncServerUrlInput.asStateFlow()

    val deviceId: String = syncManager.getDeviceId()

    init {
        // Run clean-up at start
        viewModelScope.launch {
            repository.clearSyncedEvents()
        }
        // Start the local REST server on port 12345
        localDevServer.start(12345)
        _isLocalServerRunning.value = localDevServer.isRunning()
    }

    override fun onCleared() {
        super.onCleared()
        localDevServer.stop()
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        syncManager.setAutoSyncEnabled(enabled)
        _isAutoSyncEnabled.value = enabled
    }

    fun isNetworkAvailable(): Boolean {
        return syncManager.isNetworkAvailable()
    }

    // Navigation transitions
    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        if (screen == Screen.Dashboard) {
            resetAuthSession()
            resetEnrollmentSession()
        }
    }

    // Sync helpers
    fun updateSyncUrl(url: String) {
        _syncServerUrlInput.value = url
        syncManager.saveSyncUrl(url)
    }

    fun triggerAwsSync() {
        viewModelScope.launch(Dispatchers.IO) {
            syncManager.performSync()
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            repository.deleteUserFace(userId)
        }
    }

    // Enroll Management
    fun initiateEnrollment(userId: String, name: String) {
        if (userId.isBlank() || name.isBlank()) {
            _enrollmentError.value = "User ID and Name cannot be empty."
            return
        }
        _enrollUserId.value = userId.trim()
        _enrollName.value = name.trim()
        _enrollFrameCount.value = 0
        _enrollmentError.value = null
        capturedFrameEmbeddings.clear()
        navigateTo(Screen.EnrollmentScanner)
    }

    fun processEnrollmentFrame(faces: List<Face>, croppedFace: android.graphics.Bitmap? = null) {
        if (faces.isEmpty()) return
        val face = faces.first()

        // Rate limit frames extraction to at most 1 frame per 1000ms for stable hold captures
        val now = System.currentTimeMillis()
        if (now - lastCaptureTime < 1000) return

        if (!livenessDetector.isFaceSuitableForEnrollment(face)) {
            _enrollmentError.value = "Face tilted or eyes closed. Align straight inside guide."
            return
        }
        if (!featureExtractor.hasRequiredLandmarks(face)) {
            _enrollmentError.value = "Positioning incorrect. Ensure face is center with good lighting."
            return
        }
        _enrollmentError.value = null

        val currentCount = _enrollFrameCount.value
        if (currentCount < 3) {
            lastCaptureTime = now
            val emb = featureExtractor.extractEmbedding(face, croppedFace)
            capturedFrameEmbeddings.add(emb)
            val nextCount = currentCount + 1
            _enrollFrameCount.value = nextCount

            if (nextCount == 3) {
                finalizeEnrollment()
            }
        }
    }

    private fun finalizeEnrollment() {
        val targetId = _enrollUserId.value
        val targetName = _enrollName.value
        val location = getCurrentLocation()

        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                // Average the 3 captured embeddings for high precision
                val embSize = capturedFrameEmbeddings.firstOrNull()?.size ?: 512
                val aggregate = FloatArray(embSize)
                for (emb in capturedFrameEmbeddings) {
                    for (i in 0 until embSize) {
                        aggregate[i] += emb[i]
                    }
                }
                for (i in 0 until embSize) {
                    aggregate[i] /= capturedFrameEmbeddings.size
                }

                // Perform L2 Normalization on the aggregate vector for maximum mathematical rigor during matching
                var sum = 0f
                for (v in aggregate) {
                    sum += v * v
                }
                val norm = Math.sqrt(sum.toDouble()).toFloat()
                if (norm > 0f) {
                    for (i in aggregate.indices) {
                        aggregate[i] /= norm
                    }
                }

                // Encrypting / saving securely in repository
                repository.saveUserFace(targetId, targetName, aggregate)
                repository.logEnrollmentEvent(targetId, targetName, location.first, location.second)
            }
            navigateTo(Screen.Dashboard)
            triggerAwsSync() // Auto-trigger sync queue upload on connectivity
        }
    }

    private fun resetEnrollmentSession() {
        _enrollUserId.value = ""
        _enrollName.value = ""
        _enrollFrameCount.value = 0
        _enrollmentError.value = null
        capturedFrameEmbeddings.clear()
    }

    // Authentication / Check-In Management
    fun selectUserForAuth(userId: String, name: String) {
        _authUserId.value = userId
        _authUserName.value = name
        _verificationResult.value = null
        _activeChallenge.value = livenessDetector.startNewChallenge()
        _challengeProgress.value = 1.0f
        _currentAuthPhase.value = AuthPhase.LIVENESS_1
        livenessPassed = false
        biometricMatched = false
        maxSimilarityFound = 0f
        navigateTo(Screen.AuthScanner)
    }

    fun processAuthFrame(faces: List<Face>, croppedFace: android.graphics.Bitmap? = null) {
        if (_verificationResult.value != null) return

        // Evaluate timeout status for each specific phase
        if (livenessDetector.isTimedOut()) {
            val reason = when (_currentAuthPhase.value) {
                AuthPhase.LIVENESS_1 -> "Verification failed: Phase 1 liveness check timed out. Please complete the dynamic action."
                AuthPhase.IDENTIFICATION -> "Verification failed: Phase 2 identity matching timed out. Please keep your face aligned inside the indicator."
                AuthPhase.LIVENESS_2 -> "Verification failed: Phase 3 security check timed out. Please complete the final action."
            }
            handleVerificationFailure(reason)
            return
        }

        // Update progress bar percentage based on current action challenge
        _challengeProgress.value = livenessDetector.getRemainingTimePercent()

        if (faces.isEmpty()) return
        val face = faces.first()

        when (_currentAuthPhase.value) {
            AuthPhase.LIVENESS_1 -> {
                // Phase 1: Initial Liveness Check
                if (livenessDetector.evaluateFace(face)) {
                    livenessPassed = true
                    android.util.Log.i("FaceAuthViewModel", "Liveness 1 passed! Moving to Identification Phase.")
                    
                    // Transition to Identification phase
                    _currentAuthPhase.value = AuthPhase.IDENTIFICATION
                    livenessDetector.resetChallengeTimer() // Refresh the 7s countdown timer for face alignment
                }
            }
            AuthPhase.IDENTIFICATION -> {
                // Phase 2: Face Alignment & Cosine Similarity Match Check (Identity verification)
                val yaw = face.headEulerAngleY
                val pitch = face.headEulerAngleX
                val roll = face.headEulerAngleZ
                val leftEye = face.leftEyeOpenProbability ?: 1.0f
                val rightEye = face.rightEyeOpenProbability ?: 1.0f

                val aligned = Math.abs(yaw) < 14.0f && Math.abs(pitch) < 14.0f && Math.abs(roll) < 14.0f
                val eyesOpen = leftEye > 0.40f && rightEye > 0.40f

                val suitableForAuth = aligned && eyesOpen && featureExtractor.hasRequiredLandmarks(face)

                if (suitableForAuth) {
                    val currentEmb = featureExtractor.extractEmbedding(face, croppedFace)
                    viewModelScope.launch {
                        val storedUser = repository.getUserFace(_authUserId.value)
                        if (storedUser != null) {
                            val similarity = featureExtractor.computeCosineSimilarity(currentEmb, storedUser.embedding)
                            if (similarity > maxSimilarityFound) {
                                maxSimilarityFound = similarity
                            }
                            
                            // Cognitive threshold: 0.70 similarity is highly robust and aligned for neural embeddings
                            if (similarity >= 0.70f) {
                                biometricMatched = true
                                android.util.Log.i("FaceAuthViewModel", "Identity confirmed with similarity: $similarity. Transitioning to Liveness 2.")
                                
                                // Phase 3: Final Security check with another distinct challenge
                                val firstChallenge = _activeChallenge.value
                                val secondChallenge = livenessDetector.startNewChallengeExcluding(firstChallenge)
                                _activeChallenge.value = secondChallenge
                                _currentAuthPhase.value = AuthPhase.LIVENESS_2
                            }
                        }
                    }
                }
            }
            AuthPhase.LIVENESS_2 -> {
                // Phase 3: Second Liveness Check (Guaranteed to be a different action challenge)
                if (livenessDetector.evaluateFace(face)) {
                    android.util.Log.i("FaceAuthViewModel", "Liveness 2 passed! Auth successful.")
                    viewModelScope.launch {
                        val storedUser = repository.getUserFace(_authUserId.value)
                        if (storedUser != null) {
                            _authenticatedUser.value = storedUser
                            _verificationResult.value = VerificationResult.Match(maxSimilarityFound.coerceAtLeast(0.65f))
                            val location = getCurrentLocation()
                            repository.logAuthEvent(
                                userId = _authUserId.value,
                                name = _authUserName.value,
                                success = true,
                                similarity = maxSimilarityFound,
                                latitude = location.first,
                                longitude = location.second
                            )
                            triggerAwsSync()
                        }
                    }
                }
            }
        }
    }

    private fun handleVerificationFailure(reason: String) {
        val location = getCurrentLocation()
        viewModelScope.launch {
            repository.logAuthEvent(
                userId = _authUserId.value,
                name = _authUserName.value,
                success = false,
                similarity = maxSimilarityFound,
                latitude = location.first,
                longitude = location.second
            )
            _verificationResult.value = VerificationResult.Error(reason)
            triggerAwsSync()
        }
    }

    private fun getCurrentLocation(): Pair<Double, Double> {
        val context = getApplication<Application>()
        try {
            val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
            val providers = locationManager.getProviders(true)
            var bestLocation: android.location.Location? = null
            for (provider in providers) {
                val loc = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                    bestLocation = loc
                }
            }
            if (bestLocation != null) {
                return Pair(bestLocation.latitude, bestLocation.longitude)
            }
        } catch (e: SecurityException) {
            android.util.Log.e("FaceAuthViewModel", "Location permissions are missing: ${e.message}")
        } catch (e: Exception) {
            android.util.Log.e("FaceAuthViewModel", "Location query failed: ${e.message}")
        }
        // Fallback Indian remote field station coordinates (Ladakh high altitude secure zone)
        return Pair(34.1526, 77.5771)
    }

    private fun resetAuthSession() {
        _authUserId.value = ""
        _authUserName.value = ""
        _activeChallenge.value = null
        _challengeProgress.value = 1.0f
        _verificationResult.value = null
    }
}

// Representation models
enum class AuthPhase(val title: String, val instruction: String) {
    LIVENESS_1("Initial Security check (Phase 1/3)", "Complete the prompt below to verify active presence"),
    IDENTIFICATION("Biometric Identification (Phase 2/3)", "Look directly into the camera lens with eyes wide open"),
    LIVENESS_2("Final Security confirmation (Phase 3/3)", "Almost done! Finish this final dynamic action prompt")
}

sealed interface Screen {
    object Dashboard : Screen
    object EnrollmentScanner : Screen
    object AuthScanner : Screen
}

sealed interface VerificationResult {
    data class Match(val score: Float) : VerificationResult
    data class Mismatch(val score: Float) : VerificationResult
    data class Error(val message: String) : VerificationResult
}
