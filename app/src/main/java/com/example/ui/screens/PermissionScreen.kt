package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PermissionScreen(
    onPermissionsGranted: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Choose appropriate permissions based on API version
    val permissionsList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        listOf(
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VIDEO,
            android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VIDEO
        )
    } else {
        listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val permissionState = rememberMultiplePermissionsState(permissions = permissionsList)

    val hasEssentialPermission = remember(permissionState.permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissionState.permissions.any {
                (it.permission == android.Manifest.permission.READ_MEDIA_IMAGES && it.status.isGranted) ||
                (it.permission == android.Manifest.permission.READ_MEDIA_VIDEO && it.status.isGranted) ||
                (it.permission == android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED && it.status.isGranted)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionState.permissions.any {
                (it.permission == android.Manifest.permission.READ_MEDIA_IMAGES && it.status.isGranted) ||
                (it.permission == android.Manifest.permission.READ_MEDIA_VIDEO && it.status.isGranted)
            }
        } else {
            permissionState.permissions.any {
                it.permission == android.Manifest.permission.READ_EXTERNAL_STORAGE && it.status.isGranted
            }
        }
    }

    // Trigger completion callback immediately when permissions are fully or partially granted
    LaunchedEffect(hasEssentialPermission) {
        if (hasEssentialPermission) {
            onPermissionsGranted()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Access Required",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("permission_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = OnDarkBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = OnDarkBackground,
                    navigationIconContentColor = OnDarkBackground
                )
            )
        },
        containerColor = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header Visuals with Glowing Ring
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(CyberPrimary.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        CyberPrimary.copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Storage Access Icon",
                            tint = CyberPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Grant Photo Library Access",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = OnDarkBackground,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "To locate duplicates and blurry pictures, Smart Cleaner requires permission to scan photos on this device's internal storage.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Privacy Indicator Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, Color(0x12FFFFFF)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Security Guard Icon",
                            tint = CyberPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "100% Secure & On-Device",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = OnDarkBackground
                            )
                            Text(
                                text = "No personal images are ever processed or uploaded off your phone.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Rationale Section
                val anyDenied = permissionState.permissions.any { !it.status.isGranted }
                val showRationale = permissionState.shouldShowRationale

                AnimatedVisibility(
                    visible = anyDenied,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (showRationale) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                                border = BorderStroke(1.dp, CyberPrimary.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Info Rationale",
                                        tint = CyberPrimary,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Smart Cleaner utilizes machine learning locally. Without permission, the app cannot scan or save space.",
                                        fontSize = 11.sp,
                                        color = OnDarkBackground,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        // Action Buttons based on authorization state
                        if (!showRationale && anyDenied && permissionState.permissions.any { it.status.shouldShowRationale == false }) {
                            // If user checked "Don't ask again" or permanently denied, prompt them to open settings
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.fromParts("package", context.packageName, null)
                                        )
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Fallback settings launcher
                                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberPrimary,
                                    contentColor = OnCyberPrimary
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("open_settings_button")
                            ) {
                                Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Open App Settings",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        } else {
                            Button(
                                onClick = { permissionState.launchMultiplePermissionRequest() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberPrimary,
                                    contentColor = OnCyberPrimary
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("grant_permission_button")
                            ) {
                                Text(
                                    text = "Grant Dynamic Access",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
