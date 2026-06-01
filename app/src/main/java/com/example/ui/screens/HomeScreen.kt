package com.example.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.*
import com.example.ui.viewmodel.CleanerViewModel
import com.example.ui.viewmodel.ScanUiState
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    cleanerViewModel: CleanerViewModel,
    onNavigateToDuplicates: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToJunk: (String) -> Unit,
    onNavigateToPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scanState by cleanerViewModel.scanState.collectAsState()
    val allPhotos by cleanerViewModel.allPhotos.collectAsState()
    val duplicateGroups by cleanerViewModel.duplicateGroups.collectAsState()
    val blurryPhotos by cleanerViewModel.blurryPhotos.collectAsState()

    val totalReclaimedBytes by cleanerViewModel.totalReclaimedBytes.collectAsState()
    val whatsappItems by cleanerViewModel.whatsappItems.collectAsState()
    val apkItems by cleanerViewModel.apkItems.collectAsState()
    val cacheItems by cleanerViewModel.cacheItems.collectAsState()

    val whatsappSize = whatsappItems.sumOf { it.size }
    val apkSize = apkItems.sumOf { it.size }
    val cacheSize = cacheItems.sumOf { it.size }

    val storageStats by cleanerViewModel.storageStats.collectAsState()

    val totalCapacity = storageStats.totalBytes
    val currentUsedBytes = storageStats.usedBytes
    val currentAvailableBytes = storageStats.availableBytes
    val usedPercentage = (storageStats.usedPercentage).toInt().coerceIn(0, 100)

    val duplicateBytesSize = duplicateGroups.flatMap { it.duplicates }.sumOf { it.fileSize }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Smart Cleaner",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            color = CyberPrimary,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "PRIVACY-FIRST AI",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = OnDarkBackground.copy(alpha = 0.5f),
                            letterSpacing = 1.5.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToPaywall,
                        modifier = Modifier.testTag("premium_upgrade_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0x33B2D183), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Premium Upgrade",
                                tint = CyberPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = OnDarkBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = OnDarkBackground
                )
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Storage Hero Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("storage_hero_card"),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, Color(0x0FFFFFFF))
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Subtle glowing ambient light in upper-right corner
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .align(Alignment.TopEnd)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(CyberPrimary.copy(alpha = 0.12f), Color.Transparent)
                                )
                            )
                    )

                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Available Storage",
                                    fontSize = 14.sp,
                                    color = OnDarkBackground.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = formatBytes(currentAvailableBytes),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Light,
                                        color = CyberPrimary
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "USED OF ${formatBytes(totalCapacity)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnDarkBackground.copy(alpha = 0.4f),
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$usedPercentage% Full",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnDarkBackground
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Modern progress bar
                        LinearProgressIndicator(
                            progress = { usedPercentage / 100f },
                            color = CyberPrimary,
                            trackColor = DarkSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(CyberPrimary, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "System & Media",
                                    fontSize = 12.sp,
                                    color = OnDarkBackground.copy(alpha = 0.6f)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(OnDarkBackground.copy(alpha = 0.2f), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Waste Cache",
                                    fontSize = 12.sp,
                                    color = OnDarkBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Scan Status Overlay / Recommendation Banner
            AnimatedVisibility(
                visible = scanState is ScanUiState.Scanning,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val progress = (scanState as? ScanUiState.Scanning)?.progress ?: 0
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    border = BorderStroke(1.dp, CyberPrimary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(CyberPrimary.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { progress / 100f },
                                    color = CyberPrimary,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Analyzing Spatial Patterns...",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnDarkBackground
                                )
                                Text(
                                    text = "Running on-device CNN embeddings ($progress%)",
                                    fontSize = 11.sp,
                                    color = OnDarkBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // AI Recommendation Banner (only visible if scanned)
            AnimatedVisibility(
                visible = scanState is ScanUiState.Scanned,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(CyberPrimary, ElectricPink)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clickable { onNavigateToDuplicates() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0x22000000), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "✨", fontSize = 24.sp)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Optimization Ready",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F110E),
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (duplicateGroups.isNotEmpty()) {
                                "Found ${duplicateGroups.flatMap { it.duplicates }.size} duplicates. Save ${formatFileSize(duplicateBytesSize)}"
                            } else {
                                "No duplicates found! Your library is optimized."
                            },
                            color = Color(0xFF0F110E).copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0x11000000), CircleShape)
                            .border(1.dp, Color(0x33000000), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Optimize",
                            tint = Color(0xFF0F110E)
                        )
                    }
                }
            }

            // 3. Space Wasters Grid
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOP WASTERS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = OnDarkBackground.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "View All",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberPrimary,
                        modifier = Modifier.clickable {
                            if (scanState is ScanUiState.Scanned) {
                                onNavigateToDuplicates()
                            } else {
                                Toast.makeText(context, "Please run cleanup scan first!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // WhatsApp
                    WasterGridCell(
                        modifier = Modifier.weight(1f),
                        emoji = "💬",
                        category = "WhatsApp",
                        savings = formatFileSize(whatsappSize),
                        backgroundTint = Color(0xFF2E7D32),
                        onClick = { onNavigateToJunk("WhatsApp") }
                    )

                    // Blurry Media (Dynamic from scannings!)
                    WasterGridCell(
                        modifier = Modifier.weight(1f),
                        emoji = "🎞️",
                        category = "Blurry Media",
                        savings = if (scanState is ScanUiState.Scanned) {
                            formatFileSize(blurryPhotos.sumOf { it.fileSize })
                        } else {
                            "2.8 GB"
                        },
                        backgroundTint = Color(0xFF1565C0),
                        onClick = { onNavigateToJunk("Blurry Media") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Old APKs
                    WasterGridCell(
                        modifier = Modifier.weight(1f),
                        emoji = "⚙️",
                        category = "Old APKs",
                        savings = formatFileSize(apkSize),
                        backgroundTint = Color(0xFFE65100),
                        onClick = { onNavigateToJunk("Old APKs") }
                    )

                    // App Cache
                    WasterGridCell(
                        modifier = Modifier.weight(1f),
                        emoji = "🗑️",
                        category = "App Cache",
                        savings = formatFileSize(cacheSize),
                        backgroundTint = Color(0xFF6A1B9A),
                        onClick = { onNavigateToJunk("App Cache") }
                    )
                }
            }

            // 4. Bottom Main Performance Cleanup trigger button
            Button(
                onClick = {
                    if (scanState is ScanUiState.Scanned) {
                        onNavigateToDuplicates()
                    } else {
                        // Check if system storage permissions are granted before commencing scan
                        if (hasPermissions(context)) {
                            cleanerViewModel.startScan()
                        } else {
                            onNavigateToPermission()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .testTag("begin_cleanup_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberPrimary,
                    contentColor = OnCyberPrimary
                ),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0x22FFFFFF))
            ) {
                Icon(
                    imageVector = if (scanState is ScanUiState.Scanned) Icons.Default.DoneAll else Icons.Default.OfflineBolt,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (scanState is ScanUiState.Scanned) "View Duplicate Clusters" else "Begin Safe Cleanup Scan",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            }

            // Engine feedback indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = KeeperGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "On-Device Neural Model Match: 100% Private",
                    style = MaterialTheme.typography.bodySmall,
                    color = KeeperGreen,
                    fontWeight = FontWeight.Medium
                )
            }

            TextButton(
                onClick = onLogout,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Sign Out Secure Session", color = OnDarkBackground.copy(alpha = 0.4f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun WasterGridCell(
    modifier: Modifier = Modifier,
    emoji: String,
    category: String,
    savings: String,
    backgroundTint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxHeight()
            .testTag("waster_card_$category")
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, Color(0x3BFFFFFF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(backgroundTint.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 18.sp)
            }

            Column {
                Text(
                    text = category,
                    fontSize = 13.sp,
                    color = OnDarkBackground.copy(alpha = 0.5f),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = savings,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnDarkBackground
                )
            }
        }
    }
}

private fun formatDecimal(value: Double): String {
    val df = DecimalFormat("0.0")
    return df.format(value)
}

fun hasPermissions(context: Context): Boolean {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
    } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}
