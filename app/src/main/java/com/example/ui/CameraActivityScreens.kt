package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Brush
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

    // Smooth laser and pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    val glowBrush = Brush.linearGradient(
        colors = listOf(
            CyberTeal.copy(alpha = pulseAlpha),
            ElectricBlue.copy(alpha = pulseAlpha),
            CyberTeal.copy(alpha = pulseAlpha)
        )
    )

    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpace)
    ) {
        // Upper Viewport Zone (58% Height)
        Box(
            modifier = Modifier
                .weight(0.58f)
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 3.dp,
                    brush = glowBrush,
                    shape = RoundedCornerShape(24.dp)
                )
                .background(Color.Black)
        ) {
            // Live optimized camera feed
            CameraPreview(
                onFacesDetected = { faces, croppedFace ->
                    viewModel.processEnrollmentFrame(faces, croppedFace)
                }
            )

            // Inner sweeping laser line
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val sweepY = maxHeight * offsetY
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .offset(y = sweepY)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    CyberTeal,
                                    ElectricBlue,
                                    CyberTeal,
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // High Precision Oval Indicator
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 180.dp, height = 240.dp)
                        .border(
                            width = 3.dp,
                            color = CyberTeal.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(120.dp)
                        )
                )
            }

            // Top overlay tags (Header metadata inside the viewport, floating unobtrusively)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = null,
                        tint = CyberTeal,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NEW BIOMETRIC ENROLLMENT",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Lower Control & Prompt Zone (42% Height) - Dark, spacious, elegant
        Card(
            modifier = Modifier
                .weight(0.42f)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardSlate.copy(alpha = 0.95f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "REGISTERING PROFILE",
                        color = CyberTeal,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp
                    )

                    Text(
                        text = enrollName.uppercase(),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "ID Ref: $enrollUserId",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Error Message block (Animates into size context)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedVisibility(
                        visible = errorMsg != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        errorMsg?.let { msg ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CrimsonError.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp))
                                    .border(1.dp, CrimsonError.copy(alpha = 0.3f), shape = RoundedCornerShape(10.dp))
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = CrimsonError,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    if (errorMsg == null) {
                        Text(
                            text = "HOLD STEADY INSIDE WINDOW SCANNER",
                            color = TextSilver,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Enrollment Frame Counts Status
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..3) {
                            val active = frameCount >= i
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (active) EmeraldSuccess else Color.DarkGray)
                                    .border(
                                        width = 1.5.dp,
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
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else {
                                    Text(
                                        text = "$i",
                                        color = TextSilver,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Analyzing high precision biometric landmarks ($frameCount/3)",
                        color = TextMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Text("CANCEL ENROLL", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
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

    // Smooth laser and pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Border color switches beautifully based on phase!
    val borderColor = when (authPhase) {
        AuthPhase.LIVENESS_1 -> CyberTeal
        AuthPhase.IDENTIFICATION -> ElectricBlue
        AuthPhase.LIVENESS_2 -> NeonPurple
    }

    val glowBrush = Brush.linearGradient(
        colors = listOf(
            borderColor.copy(alpha = pulseAlpha),
            ElectricBlue.copy(alpha = pulseAlpha),
            borderColor.copy(alpha = pulseAlpha)
        )
    )

    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpace)
    ) {
        // Upper Viewport Zone (58% Height) - Completely unobstructed face viewport
        Box(
            modifier = Modifier
                .weight(0.58f)
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 3.dp,
                    brush = glowBrush,
                    shape = RoundedCornerShape(24.dp)
                )
                .background(Color.Black)
        ) {
            // Live optimized camera feed
            CameraPreview(
                onFacesDetected = { faces, croppedFace ->
                    viewModel.processAuthFrame(faces, croppedFace)
                }
            )

            // Inner sweeping laser line (Sweeps continuously for cinematic feel)
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val sweepY = maxHeight * offsetY
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .offset(y = sweepY)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    borderColor,
                                    ElectricBlue,
                                    borderColor,
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // High Precision Face Guide Oval (Changes color beautifully based on phase!)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 180.dp, height = 240.dp)
                        .border(
                            width = 3.dp,
                            color = borderColor.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(120.dp)
                        )
                )
            }

            // Unobtrusive Header floating inside scanner window
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = borderColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SECURE BIOMETRIC GATE",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Lower Control & Liveness Prompts Zone (42% Height) - Dedicated area so face is NEVER blocked!
        Card(
            modifier = Modifier
                .weight(0.42f)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardSlate.copy(alpha = 0.95f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            if (result == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Multi-Phase Stepper Tracker (Pristine visual progress indicators)
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
                            val completed = when (authPhase) {
                                AuthPhase.LIVENESS_1 -> false
                                AuthPhase.IDENTIFICATION -> phase == AuthPhase.LIVENESS_1
                                AuthPhase.LIVENESS_2 -> phase == AuthPhase.LIVENESS_1 || phase == AuthPhase.IDENTIFICATION
                            }
                            val bg = when {
                                active -> borderColor
                                completed -> EmeraldSuccess
                                else -> Color.DarkGray
                            }
                            val textCol = when {
                                active || completed -> Color.White
                                else -> TextMuted
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(bg, shape = RoundedCornerShape(8.dp))
                                    .padding(vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = textCol,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Active Step title & Animate Challenge Instructs
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = authPhase.title.uppercase(),
                            color = borderColor,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )

                        // Smooth animated slide & fade transition for liveness prompts
                        AnimatedContent(
                            targetState = activeChallenge,
                            transitionSpec = {
                                (fadeIn(animationSpec = spring()) + slideInVertically { it / 2 })
                                    .togetherWith(fadeOut(animationSpec = spring()))
                            },
                            label = "challenge_animation"
                        ) { challenge ->
                            when (authPhase) {
                                AuthPhase.LIVENESS_1, AuthPhase.LIVENESS_2 -> {
                                    challenge?.let { ch ->
                                        Text(
                                            text = "${ch.emoji} ${ch.instruction.uppercase()}",
                                            color = Color.White,
                                            fontSize = 20.sp,
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
                        }

                        Text(
                            text = authPhase.instruction,
                            color = TextSilver.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Countdown Progress Bar
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LinearProgressIndicator(
                            progress = { challengeProgress },
                            color = borderColor,
                            trackColor = Color.DarkGray,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        Text(
                            text = "Liveness prevents printed photos & high-resolution screen spoofing.",
                            color = TextMuted,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth(0.5f)
                    ) {
                        Text("CANCEL", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            } else {
                // Biometrics Result card view
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            when (result) {
                                is VerificationResult.Match -> EmeraldSuccess.copy(alpha = 0.1f)
                                else -> CrimsonError.copy(alpha = 0.1f)
                            },
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    when (val v = result) {
                        is VerificationResult.Match -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "SECURE COSIGN CONFIRMED",
                                    color = EmeraldSuccess,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Biometric Match Confidence: ${(v.score * 100).toInt()}%",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        is VerificationResult.Mismatch -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GppBad,
                                    contentDescription = null,
                                    tint = CrimsonError,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "ACCESS DECLARED INVALID",
                                    color = CrimsonError,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Biometric similarity score is too low: ${(v.score * 100).toInt()}% match",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        is VerificationResult.Error -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = CrimsonError,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "VERIFICATION ERROR",
                                    color = CrimsonError,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = v.message,
                                    color = TextSilver,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        null -> {}
                    }

                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DeepSpace),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) {
                        Text("DONE", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}
