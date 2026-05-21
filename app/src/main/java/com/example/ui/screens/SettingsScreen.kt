package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by settingsViewModel.settings.collectAsState()
    val scrollState = rememberScrollState()

    var showFrequencyDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Settings & Rules",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Configure engine thresholds & limits",
                            style = MaterialTheme.typography.bodySmall,
                            color = CyberPrimary,
                            fontWeight = FontWeight.Normal
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
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
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Group 1: General Preferences Card
            SettingsSectionHeader(title = "CLEANUP FREQUENCY & THRESHOLDS")

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0x0FFFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Cleanup Frequency
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFrequencyDialog = true }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = CyberPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Auto Scan Frequency",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = OnDarkBackground
                                )
                                Text(
                                    text = "Automated spatial duplicate background search",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = settings.cleanupFrequency,
                                color = CyberPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Divider(color = Color(0x0AFFFFFF), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                    // Critical Storage Threshold Slider
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = CyberPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Critical Storage Warning",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = OnDarkBackground
                                    )
                                    Text(
                                        text = "Alert when partition fullness exceeds threshold",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Text(
                                text = "${settings.criticalStorageThreshold}%",
                                color = CyberPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Slider(
                            value = settings.criticalStorageThreshold.toFloat(),
                            onValueChange = { settingsViewModel.setCriticalStorageThreshold(it.toInt()) },
                            valueRange = 50f..95f,
                            steps = 9,
                            colors = SliderDefaults.colors(
                                activeTrackColor = CyberPrimary,
                                inactiveTrackColor = DarkSurfaceVariant,
                                thumbColor = CyberPrimary,
                                activeTickColor = DarkBackground,
                                inactiveTickColor = CyberPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("storage_threshold_slider")
                        )
                    }
                }
            }

            // Group 2: AI Scanning Filters
            SettingsSectionHeader(title = "AI SCAN COGNITIVE FILTERS")

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0x0FFFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Scan Photos with AI
                    SettingsToggleRow(
                        icon = Icons.Default.AutoAwesome,
                        title = "Neural Spatial Projection",
                        subtitle = "Identify identical duplicates on custom TFLite layer",
                        checked = settings.scanPhotosAi,
                        onCheckedChange = { settingsViewModel.toggleScanPhotosAi(it) },
                        testTag = "ai_photos_switch"
                    )

                    Divider(color = Color(0x0AFFFFFF), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                    // Scan Blurry Photos (Pro feature)
                    SettingsToggleRow(
                        icon = Icons.Default.BlurOn,
                        title = "Laplacian Focus Clustering",
                        subtitle = "Filter out micro-blur and out-of-focus captures (Pro)",
                        checked = settings.scanBlurryPhotos,
                        onCheckedChange = { isChecked ->
                            // Simple bypass / upgrade requirement
                            if (isChecked) {
                                onNavigateToPaywall()
                                Toast.makeText(context, "Pro subscription required for Laplacian Micro-blur scan", Toast.LENGTH_SHORT).show()
                            } else {
                                settingsViewModel.toggleScanBlurryPhotos(false)
                            }
                        },
                        isProGated = true,
                        testTag = "blurry_photos_switch"
                    )
                }
            }

            // Group 3: Quiet Hours & Automation Conditions
            SettingsSectionHeader(title = "BACKGROUND SCHEDULER RULES")

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0x0FFFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Quiet Hours Start & End
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = null,
                                tint = CyberPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Quiet Scan Lock",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = OnDarkBackground
                                )
                                Text(
                                    text = "Pause background scans during rest cycle",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        
                        // Interactive custom selectors (Simulated for visual layout excellence)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceVariant)
                                    .clickable {
                                        val newStart = (settings.quietHoursStart + 1) % 24
                                        settingsViewModel.setQuietHours(newStart, settings.quietHoursEnd)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "%02d:00".format(settings.quietHoursStart),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberPrimary
                                )
                            }
                            Text(text = "to", color = TextSecondary, fontSize = 12.sp)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceVariant)
                                    .clickable {
                                        val newEnd = (settings.quietHoursEnd + 1) % 24
                                        settingsViewModel.setQuietHours(settings.quietHoursStart, newEnd)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "%02d:00".format(settings.quietHoursEnd),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberPrimary
                                )
                            }
                        }
                    }

                    Divider(color = Color(0x0AFFFFFF), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                    // Wi-Fi Restrictions
                    SettingsToggleRow(
                        icon = Icons.Default.Wifi,
                        title = "Over Wi-Fi Only",
                        subtitle = "Sync AI models and telemetry strictly on Wi-Fi state",
                        checked = settings.wifiOnly,
                        onCheckedChange = { settingsViewModel.toggleWifiOnly(it) },
                        testTag = "wifi_only_switch"
                    )

                    Divider(color = Color(0x0AFFFFFF), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                    // Charger Restrictions
                    SettingsToggleRow(
                        icon = Icons.Default.BatteryChargingFull,
                        title = "Charging Device Only",
                        subtitle = "Run intensive cluster alignments on active charger line",
                        checked = settings.chargingOnly,
                        onCheckedChange = { settingsViewModel.toggleChargingOnly(it) },
                        testTag = "charging_only_switch"
                    )
                }
            }

            // Group 4: Device Hardware Safeguards
            SettingsSectionHeader(title = "HARDWARE SAFEGUARDS")

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0x0FFFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Auto Clean Cache
                    SettingsToggleRow(
                        icon = Icons.Default.Delete,
                        title = "Dynamic Cache Purge",
                        subtitle = "Evict app directories' thumbnail garbage automatically",
                        checked = settings.autoCleanCache,
                        onCheckedChange = { settingsViewModel.toggleAutoCleanCache(it) },
                        testTag = "cache_clean_switch"
                    )

                    Divider(color = Color(0x0AFFFFFF), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                    // Auto Clean APKs
                    SettingsToggleRow(
                        icon = Icons.Default.SettingsApplications,
                        title = "Stale APK Garbage Sweeper",
                        subtitle = "Discard non-installed developer files on identification",
                        checked = settings.autoCleanApks,
                        onCheckedChange = { settingsViewModel.toggleAutoCleanApks(it) },
                        testTag = "apk_clean_switch"
                    )

                    Divider(color = Color(0x0AFFFFFF), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                    // Notifications Toggle
                    SettingsToggleRow(
                        icon = Icons.Default.NotificationsActive,
                        title = "Send Smart Alerts",
                        subtitle = "Push notifications for potential space-saving suggestions",
                        checked = settings.notificationsEnabled,
                        onCheckedChange = { settingsViewModel.toggleNotificationsEnabled(it) },
                        testTag = "notifications_switch"
                    )
                }
            }

            // Bottom Trademark text
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = CyberPrimary.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "On-Device Neural Sanitizer\nVersion 3.4.1 (Stable Release)",
                    fontSize = 11.sp,
                    color = TextSecondary.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }
    }

    // Dialog for Cleanup Frequency
    if (showFrequencyDialog) {
        val options = listOf("Daily", "Weekly", "Monthly", "Never")
        AlertDialog(
            onDismissRequest = { showFrequencyDialog = false },
            title = {
                Text(
                    text = "Select Auto Scan Frequency",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = OnDarkBackground
                )
            },
            containerColor = DarkSurface,
            textContentColor = OnDarkSurface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    settingsViewModel.setCleanupFrequency(option)
                                    showFrequencyDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = option,
                                fontWeight = FontWeight.Medium,
                                color = if (settings.cleanupFrequency == option) CyberPrimary else OnDarkBackground,
                                fontSize = 14.sp
                            )
                            if (settings.cleanupFrequency == option) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = CyberPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFrequencyDialog = false }) {
                    Text("Close", color = CyberPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.5.sp,
        color = OnDarkBackground.copy(alpha = 0.4f),
        modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
    )
}

@Composable
fun SettingsToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isProGated: Boolean = false,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isProGated) ElectricPink else CyberPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = OnDarkBackground
                    )
                    if (isProGated) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(ElectricPink, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PRO",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnElectricPink
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OnCyberPrimary,
                checkedTrackColor = CyberPrimary,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = DarkSurfaceVariant
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
