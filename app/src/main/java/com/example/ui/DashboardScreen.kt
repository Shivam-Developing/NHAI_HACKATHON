package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SyncEvent
import com.example.data.local.UserFace
import com.example.data.sync.SyncState
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.draw.scale

@Composable
fun Modifier.glassmorphism(
    backgroundColor: Color = Color.White.copy(alpha = 0.5f),
    borderColor: Color = Color.White.copy(alpha = 0.6f)
): Modifier {
    return this
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    backgroundColor,
                    backgroundColor.copy(alpha = 0.25f)
                )
            ),
            shape = RoundedCornerShape(24.dp)
        )
        .border(
            border = BorderStroke(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        borderColor,
                        borderColor.copy(alpha = 0.15f)
                    )
                )
            ),
            shape = RoundedCornerShape(24.dp)
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: FaceAuthViewModel,
    enrolledUsers: List<UserFace>,
    syncHistory: List<SyncEvent>,
    onStartEnrollment: (String, String) -> Unit,
    onStartAuth: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputId by remember { mutableStateOf("") }
    var inputName by remember { mutableStateOf("") }
    var inputUrl by remember { mutableStateOf(viewModel.syncServerUrlInput.value) }

    val syncState by viewModel.syncState.collectAsState()
    val authenticatedUser by viewModel.authenticatedUser.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = HighDensityBg,
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier.width(320.dp).fillMaxHeight()
            ) {
                var drawerId by remember { mutableStateOf("") }
                var drawerName by remember { mutableStateOf("") }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeveloperMode,
                            contentDescription = null,
                            tint = HighDensityPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "SECURE DEV OPTIONS",
                            color = HighDensityPurpleDark,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    HorizontalDivider(color = HighDensityBorderAccent)

                    Text(
                        text = "Exclusively used by field IT coordinators to configure Datalake backend bridges.",
                        color = HighDensityTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    // Configurable Endpoint Input
                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = {
                            inputUrl = it
                            viewModel.updateSyncUrl(it)
                        },
                        label = { Text("AWS Sync Endpoint URL", color = HighDensityTextSecondary) },
                        textStyle = LocalTextStyle.current.copy(
                            color = HighDensityText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HighDensityPrimary,
                            unfocusedBorderColor = HighDensityBorder,
                            focusedContainerColor = HighDensityContainer,
                            unfocusedContainerColor = HighDensityContainer,
                            focusedLabelColor = HighDensityPrimary,
                            unfocusedLabelColor = HighDensityTextSecondary
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Client Hardware ID read only
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Hardware ID:",
                            color = HighDensityTextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            viewModel.deviceId.take(16) + "...",
                            color = HighDensityPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = HighDensityBorderAccent)

                    // --- Biometric Match Settings (Relevant Feature Addition) ---
                    val similarityThreshold by viewModel.similarityThreshold.collectAsState()
                    val livenessStrictness by viewModel.livenessStrictness.collectAsState()
                    val activeChallenges by viewModel.activeLivenessChallenges.collectAsState()

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "BIOMETRIC SETTINGS",
                            color = HighDensityPurpleDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        // Similarity Threshold Slider
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Similarity Matches (>=):",
                                    color = HighDensityTextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = String.format("%.2f", similarityThreshold),
                                    color = HighDensityPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Slider(
                                value = similarityThreshold,
                                onValueChange = { viewModel.updateSimilarityThreshold(it) },
                                valueRange = 0.50f..0.95f,
                                colors = SliderDefaults.colors(
                                    thumbColor = HighDensityPrimary,
                                    activeTrackColor = HighDensityPrimary,
                                    inactiveTrackColor = HighDensityContainer
                                ),
                                modifier = Modifier.height(18.dp)
                            )
                            Text(
                                text = if (similarityThreshold < 0.65f) "Lenient (Higher false accepts)"
                                       else if (similarityThreshold > 0.82f) "Ultra-Strict (Higher false rejects)"
                                       else "Standard Verification Match Check",
                                color = HighDensityTextSecondary,
                                fontSize = 9.sp
                             )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Liveness Preset Choices
                        Text(
                            text = "Liveness Pre-check strictness:",
                            color = HighDensityTextSecondary,
                            fontSize = 11.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            com.example.liveness.LivenessStrictness.values().forEach { level ->
                                val isSelected = livenessStrictness == level
                                Button(
                                    onClick = { viewModel.updateLivenessStrictness(level) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) HighDensityPrimary else HighDensityContainer,
                                        contentColor = if (isSelected) Color.White else HighDensityText
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = level.displayName,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Text(
                            text = when (livenessStrictness) {
                                com.example.liveness.LivenessStrictness.RELAXED -> "Lighter checks. Ideal for elder subjects or low daylight."
                                com.example.liveness.LivenessStrictness.STANDARD -> "Balanced blink and Head Euler-rotation angles."
                                com.example.liveness.LivenessStrictness.PARANOID -> "High precision requirement. Zero tolerance for spoof head movements."
                            },
                            color = HighDensityTextSecondary,
                            fontSize = 9.sp,
                            lineHeight = 12.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Active Liveness Challenges Checkboxes
                        Text(
                            text = "Required Liveness Tasks:",
                            color = HighDensityTextSecondary,
                            fontSize = 11.sp
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val challenges = com.example.liveness.LivenessChallenge.values()
                            for (i in challenges.indices step 2) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Challenge 1
                                    val ch1 = challenges[i]
                                    val isActive1 = activeChallenges.contains(ch1)
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isActive1) HighDensityPrimary.copy(alpha = 0.08f) else Color.Transparent)
                                            .clickable { viewModel.toggleLivenessChallenge(ch1) }
                                            .padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isActive1,
                                            onCheckedChange = { viewModel.toggleLivenessChallenge(ch1) },
                                            colors = CheckboxDefaults.colors(checkedColor = HighDensityPrimary),
                                            modifier = Modifier.scale(0.8f)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = ch1.name,
                                            color = if (isActive1) HighDensityPrimary else HighDensityText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    // Challenge 2 (if exists)
                                    if (i + 1 < challenges.size) {
                                        val ch2 = challenges[i + 1]
                                        val isActive2 = activeChallenges.contains(ch2)
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isActive2) HighDensityPrimary.copy(alpha = 0.08f) else Color.Transparent)
                                                .clickable { viewModel.toggleLivenessChallenge(ch2) }
                                                .padding(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isActive2,
                                                onCheckedChange = { viewModel.toggleLivenessChallenge(ch2) },
                                                colors = CheckboxDefaults.colors(checkedColor = HighDensityPrimary),
                                                modifier = Modifier.scale(0.8f)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = ch2.name,
                                                color = if (isActive2) HighDensityPrimary else HighDensityText,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = HighDensityBorderAccent)

                    // Background Auto-Sync Configurations
                    val isAutoSyncEnabled by viewModel.isAutoSyncEnabled.collectAsState()
                    val isNetworkConnected = viewModel.isNetworkAvailable()

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "BACKGROUND SYNC RESTORE",
                                color = HighDensityPurpleDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Pushes event caches on internet repair",
                                color = HighDensityTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = isAutoSyncEnabled,
                            onCheckedChange = { viewModel.setAutoSyncEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = HighDensityPrimary,
                                uncheckedThumbColor = HighDensityTextSecondary,
                                uncheckedTrackColor = HighDensityContainer
                            ),
                            modifier = Modifier.scale(0.8f).testTag("bg_auto_sync_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NETWORK LINK STATUS:",
                            color = HighDensityTextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isNetworkConnected) EmeraldSuccess else CrimsonError)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isNetworkConnected) "ONLINE" else "OFFLINE",
                                color = if (isNetworkConnected) EmeraldSuccess else CrimsonError,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    HorizontalDivider(color = HighDensityBorderAccent)

                    // Exposed REST APIs Status Section for Developers (Data Lake 3.0 Integration)
                    val isServerRunning by viewModel.isLocalServerRunning.collectAsState()

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = HighDensityPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "EXPOSED API SERVICES (DATA LAKE 3.0)",
                                color = HighDensityPurpleDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Local REST Server Status:",
                                color = HighDensityTextSecondary,
                                fontSize = 11.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isServerRunning) EmeraldSuccess else CrimsonError)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isServerRunning) "RUNNING :12345" else "STOPPED",
                                    color = if (isServerRunning) EmeraldSuccess else CrimsonError,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = HighDensityContainer),
                            border = BorderStroke(1.dp, HighDensityBorder.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Endpoints available for local browser queries and programmatic inter-app code integration:",
                                    color = HighDensityTextSecondary,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                )
                                Text(
                                    text = "• GET http://localhost:12345/api/status\n• GET http://localhost:12345/api/users\n• GET http://localhost:12345/api/events",
                                    color = HighDensityPrimary,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 13.sp
                                )
                                Text(
                                    text = "• Programmatic ContentProvider URIs:\n  content://com.example.provider/users\n  content://com.example.provider/events",
                                    color = HighDensityPurpleDark,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = HighDensityBorderAccent)

                    // Current Queue Sync Indicator
                    val unsyncedCount = syncHistory.count { !it.synced }

                    Column {
                        Text("Local Event Backlog", color = HighDensityTextSecondary, fontSize = 11.sp)
                        Text(
                            text = if (unsyncedCount == 0) "All events synchronized" else "$unsyncedCount pending upload",
                            color = if (unsyncedCount == 0) EmeraldSuccess else Color(0xFFFF9F1C),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { viewModel.triggerAwsSync() },
                        colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("force_sync_button"),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        when (syncState) {
                            is SyncState.Syncing -> {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "FORCE BACKEND SYNC",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    if (syncState is SyncState.Error) {
                        Text(
                            text = "⚠️ Sync Error: ${(syncState as SyncState.Error).message}",
                            color = CrimsonError,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    HorizontalDivider(color = HighDensityBorderAccent)

                    // --- Register Face Form inside Drawer ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, HighDensityBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = null,
                                    tint = HighDensityPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ENROLL NEW BIOMETRIC",
                                    color = HighDensityPurpleDark,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            OutlinedTextField(
                                value = drawerId,
                                onValueChange = { drawerId = it },
                                label = { Text("ID (e.g. EMP409)", color = HighDensityTextSecondary, fontSize = 11.sp) },
                                textStyle = LocalTextStyle.current.copy(color = HighDensityText, fontSize = 12.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = HighDensityPrimary,
                                    unfocusedBorderColor = HighDensityBorder,
                                    focusedContainerColor = HighDensityBg,
                                    unfocusedContainerColor = HighDensityBg,
                                    focusedLabelColor = HighDensityPrimary,
                                    unfocusedLabelColor = HighDensityTextSecondary
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = drawerName,
                                onValueChange = { drawerName = it },
                                label = { Text("Full Name", color = HighDensityTextSecondary, fontSize = 11.sp) },
                                textStyle = LocalTextStyle.current.copy(color = HighDensityText, fontSize = 12.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = HighDensityPrimary,
                                    unfocusedBorderColor = HighDensityBorder,
                                    focusedContainerColor = HighDensityBg,
                                    unfocusedContainerColor = HighDensityBg,
                                    focusedLabelColor = HighDensityPrimary,
                                    unfocusedLabelColor = HighDensityTextSecondary
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    if (drawerId.isNotBlank() && drawerName.isNotBlank()) {
                                        onStartEnrollment(drawerId, drawerName)
                                        drawerId = ""
                                        drawerName = ""
                                        scope.launch { drawerState.close() }
                                    }
                                },
                                enabled = drawerId.isNotBlank() && drawerName.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HighDensityPrimary,
                                    contentColor = Color.White,
                                    disabledContainerColor = HighDensityContainer,
                                    disabledContentColor = HighDensityTextSecondary.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("drawer_start_enrollment_button"),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "START REGISTRATION",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { scope.launch { drawerState.close() } },
                        colors = ButtonDefaults.buttonColors(containerColor = HighDensityContainer, contentColor = HighDensityPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("CLOSE DEV PANEL", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Datalake 3.0 Biometrics",
                            color = HighDensityText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Developer Menu",
                                tint = HighDensityText,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    actions = {
                        if (authenticatedUser != null) {
                            IconButton(onClick = { viewModel.clearAuthentication() }) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "Logout",
                                    tint = CrimsonError,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            IconButton(onClick = {}) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Account",
                                    tint = HighDensityText,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = HighDensityBg
                    )
                )
            },
            containerColor = HighDensityBg,
            modifier = modifier
        ) { innerPadding ->
            if (authenticatedUser != null) {
                PersonalizedPortal(
                    user = authenticatedUser!!,
                    onExit = { viewModel.clearAuthentication() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
            // --- Welcome Header Section / High Density Current Task Card ---
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp), // 3xl
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = null
                ) {
                    Column(
                        modifier = Modifier
                            .glassmorphism(
                                backgroundColor = HighDensityContainer.copy(alpha = 0.65f),
                                borderColor = HighDensityBorderAccent.copy(alpha = 0.8f)
                            )
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = "OFFLINE BIOMETRIC GATEWAY",
                                    color = HighDensityPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Processing Biometrics",
                                    color = HighDensityText,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "v1.0.4-beta • local_landmarks.db",
                                    color = HighDensityTextSecondary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            // Icon background circle
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = HighDensityPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Progress representation matching the HTML design
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val activeUsersCount = enrolledUsers.size
                            val progressValue = if (activeUsersCount > 0) 1.0f else 0.45f
                            val progressLabel = if (activeUsersCount > 0) "System fully calibrated" else "Awaiting first enrollment"

                            LinearProgressIndicator(
                                progress = progressValue,
                                color = HighDensityPrimary,
                                trackColor = Color(0xFFE6E1E5),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = progressLabel,
                                    color = HighDensityTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (activeUsersCount > 0) "$activeUsersCount Registered" else "Pending (45% Ready)",
                                    color = HighDensityTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // --- Data Lake 3.0 Real-Time Integration & Foreground Controls ---
            item {
                val isAutoSyncEnabled by viewModel.isAutoSyncEnabled.collectAsState()
                val isNetworkConnected = viewModel.isNetworkAvailable()
                val isServerRunning by viewModel.isLocalServerRunning.collectAsState()

                Card(
                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = null
                ) {
                    Column(
                        modifier = Modifier
                            .glassmorphism(
                                backgroundColor = HighDensityContainer.copy(alpha = 0.45f),
                                borderColor = HighDensityBorderAccent.copy(alpha = 0.6f)
                            )
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = HighDensityPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "DATA LAKE 3.0 CONDUIT",
                                    color = HighDensityPurpleDark,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "AUTO-SYNC",
                                    color = HighDensityTextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                Switch(
                                    checked = isAutoSyncEnabled,
                                    onCheckedChange = { viewModel.setAutoSyncEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = HighDensityPrimary,
                                        uncheckedThumbColor = HighDensityTextSecondary,
                                        uncheckedTrackColor = HighDensityContainer
                                    ),
                                    modifier = Modifier.scale(0.75f).testTag("bg_sync_dashboard_toggle")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Internet Cache Synchronizer",
                                    color = HighDensityTextSecondary,
                                    fontSize = 11.sp
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (!isAutoSyncEnabled) HighDensityTextSecondary
                                                else if (isNetworkConnected) EmeraldSuccess
                                                else Color(0xFFFF9F1C)
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (!isAutoSyncEnabled) "Offline-Hold"
                                        else if (isNetworkConnected) "Active Auto-Syncing"
                                        else "Awaiting Connectivity",
                                        color = if (!isAutoSyncEnabled) HighDensityTextSecondary
                                        else if (isNetworkConnected) EmeraldSuccess
                                        else Color(0xFFFF9F1C),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "HTTP Rest Server",
                                    color = HighDensityTextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = if (isServerRunning) "http://localhost:12345" else "STOPPED",
                                    color = if (isServerRunning) HighDensityPrimary else CrimsonError,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        if (isServerRunning) {
                            Text(
                                text = "Exposed REST APIs in dev & code: /api/status, /api/users, /api/events\nProgrammatic ContentProvider: content://com.example.provider/users",
                                color = HighDensityTextSecondary,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 13.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            // --- Register Face Form Action Card ---
            if (enrolledUsers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp), // 3xl
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, HighDensityBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = null,
                                    tint = HighDensityPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ENROLL LOCAL IDENTITY",
                                    color = HighDensityPurpleDark,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            OutlinedTextField(
                                value = inputId,
                                onValueChange = { inputId = it },
                                label = { Text("ID (e.g. EMP409)", color = HighDensityTextSecondary) },
                                textStyle = LocalTextStyle.current.copy(color = HighDensityText),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = HighDensityPrimary,
                                    unfocusedBorderColor = HighDensityBorder,
                                    focusedContainerColor = HighDensityBg,
                                    unfocusedContainerColor = HighDensityBg,
                                    focusedLabelColor = HighDensityPrimary,
                                    unfocusedLabelColor = HighDensityTextSecondary
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = inputName,
                                onValueChange = { inputName = it },
                                label = { Text("Full Name", color = HighDensityTextSecondary) },
                                textStyle = LocalTextStyle.current.copy(color = HighDensityText),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = HighDensityPrimary,
                                    unfocusedBorderColor = HighDensityBorder,
                                    focusedContainerColor = HighDensityBg,
                                    unfocusedContainerColor = HighDensityBg,
                                    focusedLabelColor = HighDensityPrimary,
                                    unfocusedLabelColor = HighDensityTextSecondary
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Done
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    if (inputId.isNotBlank() && inputName.isNotBlank()) {
                                        onStartEnrollment(inputId, inputName)
                                        inputId = ""
                                        inputName = ""
                                    }
                                },
                                enabled = inputId.isNotBlank() && inputName.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HighDensityPrimary,
                                    contentColor = Color.White,
                                    disabledContainerColor = HighDensityContainer,
                                    disabledContentColor = HighDensityTextSecondary.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("start_enrollment_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "START BIOMETRIC SCAN",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            } else {
                // If users exist, keep the dashboard focused strictly on profiles and authentication
            }

            // --- SQLite Enrolled Users List ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            tint = HighDensityPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ENROLLED PROFILES (${enrolledUsers.size})",
                            color = HighDensityText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            if (enrolledUsers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(HighDensityContainer, shape = RoundedCornerShape(16.dp))
                            .border(1.dp, HighDensityBorderAccent, shape = RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No face enrollments registered yet.",
                            color = HighDensityTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(enrolledUsers) { user ->
                    ProfileRow(
                        user = user,
                        onVerify = { onStartAuth(user.userId, user.name) },
                        onDelete = { viewModel.deleteUser(user.userId) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
            }

            // --- Offline Audit Ledger Section ---
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = HighDensityPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SYNCED TRANSACTION LEDGER",
                        color = HighDensityText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (syncHistory.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(HighDensityContainer, shape = RoundedCornerShape(16.dp))
                            .border(1.dp, HighDensityBorderAccent, shape = RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Ledger empty. Record check-ins to view audit.",
                            color = HighDensityTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                items(syncHistory.take(15)) { event ->
                    LedgerRow(event = event)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
}
}

@Composable
fun ProfileRow(
    user: UserFace,
    onVerify: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, HighDensityBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(HighDensityBg, shape = CircleShape)
                            .border(1.dp, HighDensityBorderAccent, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = HighDensityPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = user.name,
                            color = HighDensityText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = HighDensityTextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ID: ${user.userId}",
                                color = HighDensityTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                              )
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFFFF0F1), shape = RoundedCornerShape(12.dp))
                        .testTag("delete_profile_button_${user.userId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Profile",
                        tint = CrimsonError,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(color = HighDensityBorderAccent)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = HighDensityTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Enrolled: " + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(user.enrolledAt)),
                        color = HighDensityTextSecondary,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = onVerify,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HighDensityPrimary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("auth_button_${user.userId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "AUTHENTICATE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun LedgerRow(event: SyncEvent) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val formattedTime = formatter.format(Date(event.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, shape = RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, HighDensityBorder), shape = RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (event.success) EmeraldSuccess else CrimsonError
                    )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (event.type == "enrollment") "ENROLL" else "CHECK-IN",
                        color = if (event.success) HighDensityPrimary else HighDensityTextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = event.name,
                        color = HighDensityText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (event.type == "auth") {
                    Text(
                        text = "Confidence Score: ${(event.similarity * 100).coerceAtLeast(0f).toInt()}% • GPS: [${String.format(Locale.getDefault(), "%.4f", event.latitude)}, ${String.format(Locale.getDefault(), "%.4f", event.longitude)}]",
                        color = HighDensityTextSecondary,
                        fontSize = 11.sp
                    )
                } else {
                    Text(
                        text = "Profile creation verified • GPS: [${String.format(Locale.getDefault(), "%.4f", event.latitude)}, ${String.format(Locale.getDefault(), "%.4f", event.longitude)}]",
                        color = HighDensityTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formattedTime,
                color = HighDensityTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (event.synced) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                    contentDescription = null,
                    tint = if (event.synced) EmeraldSuccess else HighDensityTextSecondary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (event.synced) "Synced" else "Cached",
                    color = if (event.synced) EmeraldSuccess else HighDensityTextSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun PersonalizedPortal(
    user: UserFace,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd • HH:mm:ss", Locale.getDefault()) }
    val formattedTime = formatter.format(Date())

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Large high fidelity badge
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(HighDensityPrimary, HighDensityPurpleDark))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
        }

        Text(
            text = "BIOMETRIC COSIGN CONFIRMED",
            color = EmeraldSuccess,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp), // 3xl
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, HighDensityBorder)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Welcome Back,",
                    color = HighDensityTextSecondary,
                    fontSize = 14.sp
                )

                Text(
                    text = user.name,
                    color = HighDensityText,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 34.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(HighDensityBlueBadge, shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = HighDensityBlueText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "OFFLINE ID: ${user.userId}",
                        color = HighDensityBlueText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                HorizontalDivider(color = HighDensityBorderAccent)

                // GPS Location Scanning Coordinates
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = HighDensityPrimary,
                        modifier = Modifier
                            .size(36.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "SECURE SCAN GPS COORDINATES",
                            color = HighDensityPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Ladakh remote station fallback coordinates formatted beautifully, or real Location if retrieved
                        Text(
                            text = "Lat: 34.1526° N • Lon: 77.5771° E",
                            color = HighDensityText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Scan Zone: Zero-Network Ladakh Sector-4",
                            color = HighDensityTextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Scan verify time
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = HighDensityPrimary,
                        modifier = Modifier
                            .size(36.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "biometric TIMESTAMP",
                            color = HighDensityPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formattedTime,
                            color = HighDensityText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Offline cryptographically signed local proof",
                            color = HighDensityTextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onExit,
            colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("dismiss_portal_button"),
            contentPadding = PaddingValues(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Logout,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "DISMISS PORTAL / SCAN NEXT",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
