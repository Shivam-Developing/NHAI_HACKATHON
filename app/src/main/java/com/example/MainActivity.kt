package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.DashboardScreen
import com.example.ui.EnrollmentScreen
import com.example.ui.FaceAuthViewModel
import com.example.ui.Screen
import com.example.ui.VerificationScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.HighDensityBg
import com.example.ui.theme.HighDensityPrimary
import com.example.ui.theme.HighDensityText
import com.example.ui.theme.HighDensityTextSecondary
import com.example.ui.theme.HighDensityContainer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = HighDensityBg
                ) { innerPadding ->
                    FaceAuthApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun FaceAuthApp(
    modifier: Modifier = Modifier,
    viewModel: FaceAuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsState()
    val enrolledUsers by viewModel.enrolledUsers.collectAsState()
    val syncHistory by viewModel.syncHistory.collectAsState()

    // Camera and Location permission tracking
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: hasCameraPermission
        hasLocationPermission = (permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false) ||
                                (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false)
    }

    // Temporary storage for launching actions after permission checks
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val handleCameraActionRequired: (() -> Unit) -> Unit = { actionToRun ->
        if (hasCameraPermission && hasLocationPermission) {
            actionToRun()
        } else {
            pendingAction = actionToRun
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(hasCameraPermission, hasLocationPermission) {
        if (hasCameraPermission && hasLocationPermission) {
            pendingAction?.let {
                it()
                pendingAction = null
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = HighDensityBg
    ) {
        when (currentScreen) {
            is Screen.Dashboard -> {
                DashboardScreen(
                    viewModel = viewModel,
                    enrolledUsers = enrolledUsers,
                    syncHistory = syncHistory,
                    onStartEnrollment = { uid, uname ->
                        handleCameraActionRequired {
                            viewModel.initiateEnrollment(uid, uname)
                        }
                    },
                    onStartAuth = { uid, uname ->
                        handleCameraActionRequired {
                            viewModel.selectUserForAuth(uid, uname)
                        }
                    }
                )
            }
            is Screen.EnrollmentScanner -> {
                if (hasCameraPermission) {
                    EnrollmentScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateTo(Screen.Dashboard) }
                    )
                } else {
                    PermissionRequestView(
                        onRequestPermission = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        onCancel = { viewModel.navigateTo(Screen.Dashboard) }
                    )
                }
            }
            is Screen.AuthScanner -> {
                if (hasCameraPermission) {
                    VerificationScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateTo(Screen.Dashboard) }
                    )
                } else {
                    PermissionRequestView(
                        onRequestPermission = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        onCancel = { viewModel.navigateTo(Screen.Dashboard) }
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionRequestView(
    onRequestPermission: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HighDensityBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.LockOpen,
                contentDescription = null,
                tint = HighDensityPrimary,
                modifier = Modifier.size(64.dp)
            )

            Text(
                text = "CAMERA PERMISSION REQUIRED",
                color = HighDensityText,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Biometric scanning and liveness verification are fully offline operations but require immediate access to the camera hardware to track face coordinate configurations.",
                color = HighDensityTextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary, contentColor = Color.White),
                shape = RoundedCornerShape(24.dp), // round-full pill
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("GRANT CAMERA PERMISSION", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }

            TextButton(onClick = onCancel) {
                Text("CANCEL ACTION", color = HighDensityTextSecondary, fontSize = 13.sp)
            }
        }
    }
}
