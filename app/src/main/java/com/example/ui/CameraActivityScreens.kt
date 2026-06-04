package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.CameraPreview
import com.example.ui.theme.*

@Composable
fun EnrollmentScreen(
    viewModel: FaceAuthViewModel,
    onBack: () -> Unit
) {
    val enrollUserId by viewModel.enrollUserId.collectAsState()
    val enrollName by viewModel.enrollName.collectAsState()
    val frameCount by viewModel.enrollFrameCount.collectAsState()
    val errorMsg by viewModel.enrollmentError.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpace)
    ) {
        // Live camera feed bounding
        CameraPreview(
            onFacesDetected = { faces, croppedFace ->
                viewModel.processEnrollmentFrame(faces, croppedFace)
            }
        )

        // Oval guidance overlay
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 240.dp, height = 300.dp)
                    .border(width = 3.dp, color = HighDensityPrimary, shape = RoundedCornerShape(150.dp))
            )
        }

        // Header metadata
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(HighDensityPrimary.copy(alpha = 0.9f), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "BIOMETRIC PROFILE ENROLL",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Registering: $enrollName ($enrollUserId)",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(HighDensityPrimary.copy(alpha = 0.9f), shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        // Action controls / error warning overlays
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 40.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            errorMsg?.let { msg ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = CrimsonError.copy(alpha = 0.9f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxWidth()
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSlate.copy(alpha = 0.9f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "HOLD STEADY INSIDE GUIDE",
                        color = TextSilver,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Frame capture step indicator circles
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..3) {
                            val active = frameCount >= i
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (active) EmeraldSuccess else Color.DarkGray)
                                    .border(
                                        width = 1.dp,
                                        color = if (active) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (active) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                } else {
                                    Text(
                                        text = "$i",
                                        color = TextSilver,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Capturing standard high quality facial landmarks ($frameCount/3)...",
                        color = TextMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(0.5f)
                    ) {
                        Text("CANCEL", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun VerificationScreen(
    viewModel: FaceAuthViewModel,
    onBack: () -> Unit
) {
    val authUserId by viewModel.authUserId.collectAsState()
    val authUserName by viewModel.authUserName.collectAsState()
    val activeChallenge by viewModel.activeChallenge.collectAsState()
    val challengeProgress by viewModel.challengeProgress.collectAsState()
    val result by viewModel.verificationResult.collectAsState()
    val authPhase by viewModel.currentAuthPhase.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpace)
    ) {
        // Standard Camera view
        CameraPreview(
            onFacesDetected = { faces, croppedFace ->
                viewModel.processAuthFrame(faces, croppedFace)
            }
        )

        // Oval face locator guidelines
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 240.dp, height = 300.dp)
                    .border(width = 3.dp, color = if (authPhase == AuthPhase.IDENTIFICATION) ElectricBlue else HighDensityPrimary, shape = RoundedCornerShape(150.dp))
            )
        }

        // Header Info Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(HighDensityPrimary.copy(alpha = 0.9f), shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "BIOMETRIC AUTHENTICATION",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Authenticating: $authUserName",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(HighDensityPrimary.copy(alpha = 0.9f), shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        // Bottom active challenge prompts & results
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 40.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (result == null) {
                // If checking liveness: display current prompt + count bar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSlate.copy(alpha = 0.95f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Multi-Phase Stepper View
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                AuthPhase.LIVENESS_1 to "1. Liveness",
                                AuthPhase.IDENTIFICATION to "2. Identity",
                                AuthPhase.LIVENESS_2 to "3. Confirm"
                            ).forEach { (phase, label) ->
                                val active = authPhase == phase
                                val completed = when(authPhase) {
                                    AuthPhase.LIVENESS_1 -> false
                                    AuthPhase.IDENTIFICATION -> phase == AuthPhase.LIVENESS_1
                                    AuthPhase.LIVENESS_2 -> phase == AuthPhase.LIVENESS_1 || phase == AuthPhase.IDENTIFICATION
                                }
                                val bg = when {
                                    active -> HighDensityPrimary
                                    completed -> EmeraldSuccess
                                    else -> Color.DarkGray
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(bg, shape = RoundedCornerShape(8.dp))
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = authPhase.title.uppercase(),
                            color = ElectricBlue,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )

                        when (authPhase) {
                            AuthPhase.LIVENESS_1, AuthPhase.LIVENESS_2 -> {
                                activeChallenge?.let { challenge ->
                                    Text(
                                        text = "${challenge.emoji} ${challenge.instruction.uppercase()}",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            AuthPhase.IDENTIFICATION -> {
                                Text(
                                    text = "👁️ LOOK DIRECTLY AT THE LENS",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Text(
                            text = authPhase.instruction,
                            color = TextSilver.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Countdown Progress Bar
                        LinearProgressIndicator(
                            progress = challengeProgress,
                            color = ElectricBlue,
                            trackColor = Color.DarkGray,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )

                        Text(
                            text = "Liveness prevents printed photo & screen spoof attacks.",
                            color = TextMuted,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Displays haptic results overlay (verification successfully confirmed OR rejected)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (result) {
                            is VerificationResult.Match -> EmeraldSuccess.copy(alpha = 0.95f)
                            else -> CrimsonError.copy(alpha = 0.95f)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (val v = result) {
                            is VerificationResult.Match -> {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "BIOMETRIC COSIGN CONFIRMED",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Similarity Match Confidence: ${(v.score * 100).toInt()}%",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            is VerificationResult.Mismatch -> {
                                Icon(
                                    imageVector = Icons.Default.GppBad,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "ACCESS REJECTED",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Biometric score too low: ${(v.score * 100).toInt()}% match",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            is VerificationResult.Error -> {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "AUTHENTICATION ERROR",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = v.message,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                            null -> {}
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DeepSpace),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(0.6f)
                        ) {
                            Text("DONE", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
