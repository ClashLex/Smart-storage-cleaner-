package com.example.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ChevronRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.viewmodel.CleanerViewModel
import com.example.ui.viewmodel.ScanUiState

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
    val duplicateGroups by cleanerViewModel.duplicateGroups.collectAsState()
    val blurryPhotos by cleanerViewModel.blurryPhotos.collectAsState()
    val whatsappItems by cleanerViewModel.whatsappItems.collectAsState()
    val apkItems by cleanerViewModel.apkItems.collectAsState()
    val cacheItems by cleanerViewModel.cacheItems.collectAsState()
    val storageStats by cleanerViewModel.storageStats.collectAsState()
    val totalReclaimedBytes by cleanerViewModel.totalReclaimedBytes.collectAsState()
    val duplicateCount = duplicateGroups.sumOf { it.duplicates.size }
    val duplicateBytes = duplicateGroups.sumOf { group -> group.duplicates.sumOf { it.fileSize } }
    val usedPercent = storageStats.usedPercentage.toInt().coerceIn(0, 100)
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Storage", style = MaterialTheme.typography.titleLarge)
                        Text("Smart Cleaner", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToPaywall, modifier = Modifier.testTag("premium_upgrade_button")) {
                        Icon(Icons.Default.Star, contentDescription = "Upgrade")
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Settings") }, leadingIcon = { Icon(Icons.Default.Settings, null) }, onClick = { showMenu = false; onNavigateToSettings() })
                        DropdownMenuItem(text = { Text("Sign out") }, onClick = { showMenu = false; onLogout() })
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = true, onClick = { }, icon = { Icon(Icons.Default.Storage, null) }, label = { Text("Storage") })
                NavigationBarItem(selected = false, onClick = onNavigateToDuplicates, icon = { Icon(Icons.Default.Image, null) }, label = { Text("Review") })
                NavigationBarItem(selected = false, onClick = onNavigateToSettings, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
            }
        }
    ) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(2.dp)) }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("storage_hero_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Available storage", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatBytes(storageStats.availableBytes), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("$usedPercent%", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                                Text("used", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        LinearProgressIndicator(progress = { usedPercent / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Used " + formatBytes(storageStats.usedBytes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Total " + formatBytes(storageStats.totalBytes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                Text("Cleanup overview", style = MaterialTheme.typography.titleLarge)
                Text(if (scanState is ScanUiState.Scanned) "Review the results before removing anything." else "Scan your device to find items you may want to review.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (scanState is ScanUiState.Scanning) {
                item {
                    val progress = (scanState as? ScanUiState.Scanning)?.progress ?: 0
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(progress = { progress / 100f }, modifier = Modifier.size(32.dp))
                            Column(Modifier.padding(start = 16.dp)) {
                                Text("Scanning storage", style = MaterialTheme.typography.titleMedium)
                                Text("$progress% complete", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            if (scanState is ScanUiState.Scanned) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text("Scan complete", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("$duplicateCount possible duplicates · " + formatBytes(duplicateBytes) + " to review", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Icon(Icons.AutoMirrored.Filled.ChevronRight, contentDescription = "Review results", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
            item { Text("Categories", style = MaterialTheme.typography.titleLarge) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryRow("WhatsApp media", formatBytes(whatsappItems.sumOf { it.size }), Icons.Default.Folder) { onNavigateToJunk("WhatsApp") }
                    CategoryRow("App installers", formatBytes(apkItems.sumOf { it.size }), Icons.Default.CleaningServices) { onNavigateToJunk("Old APKs") }
                    CategoryRow("App cache", formatBytes(cacheItems.sumOf { it.size }), Icons.Default.Storage) { onNavigateToJunk("App Cache") }
                    CategoryRow("Blurry photos", if (scanState is ScanUiState.Scanned) blurryPhotos.size.toString() + " items" else "Not scanned", Icons.Default.VideoLibrary) { onNavigateToJunk("Blurry Media") }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Privacy by design", style = MaterialTheme.typography.titleMedium)
                        Text("Your scan runs on this device. Review every suggested item before cleanup.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        if (scanState is ScanUiState.Scanned) onNavigateToDuplicates()
                        else if (hasPermissions(context)) cleanerViewModel.startScan()
                        else onNavigateToPermission()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).testTag("begin_cleanup_button"),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Icon(Icons.Default.Dashboard, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (scanState is ScanUiState.Scanned) "Review scan results" else "Scan storage")
                }
            }
            item {
                if (totalReclaimedBytes > 0) {
                    Text(formatBytes(totalReclaimedBytes) + " reclaimed so far", modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CategoryRow(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), shape = MaterialTheme.shapes.large) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ChevronRight, contentDescription = "Open " + title, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
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