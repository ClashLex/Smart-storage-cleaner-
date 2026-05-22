package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.example.domain.JunkItem
import com.example.data.database.PhotoEmbedding
import com.example.ui.theme.*
import com.example.ui.viewmodel.CleanerViewModel
import com.example.ui.viewmodel.ScanUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JunkCleanerScreen(
    initialCategory: String,
    cleanerViewModel: CleanerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scanState by cleanerViewModel.scanState.collectAsState()

    // 4 Category Tabs
    val categories = listOf("WhatsApp", "Blurry Media", "Old APKs", "App Cache")
    var selectedTabIndex by remember {
        mutableStateOf(categories.indexOf(initialCategory).coerceAtLeast(0))
    }
    val activeCategory = categories[selectedTabIndex]

    // Observe respective category details
    val whatsappItems by cleanerViewModel.whatsappItems.collectAsState()
    val apkItems by cleanerViewModel.apkItems.collectAsState()
    val cacheItems by cleanerViewModel.cacheItems.collectAsState()
    val blurryPhotos by cleanerViewModel.blurryPhotos.collectAsState()

    // Manage separate checked sets or inline checked properties depending on type
    // Blurry photos use the shared selection flow as they are PhotoEmbeddings
    val selectedBlurryUris by cleanerViewModel.selectedUris.collectAsState()

    // Space statistics
    val whatsappSize = whatsappItems.sumOf { it.size }
    val apkSize = apkItems.sumOf { it.size }
    val cacheSize = cacheItems.sumOf { it.size }
    val blurrySize = blurryPhotos.sumOf { it.fileSize }

    val activeListSize = when (activeCategory) {
        "WhatsApp" -> whatsappItems.size
        "Blurry Media" -> blurryPhotos.size
        "Old APKs" -> apkItems.size
        "App Cache" -> cacheItems.size
        else -> 0
    }

    val activeSelectedCount = when (activeCategory) {
        "WhatsApp" -> whatsappItems.count { it.checked }
        "Blurry Media" -> selectedBlurryUris.size
        "Old APKs" -> apkItems.count { it.checked }
        "App Cache" -> cacheItems.count { it.checked }
        else -> 0
    }

    val activeSelectedSize = when (activeCategory) {
        "WhatsApp" -> whatsappItems.filter { it.checked }.sumOf { it.size }
        "Blurry Media" -> blurryPhotos.filter { selectedBlurryUris.contains(it.uri) }.sumOf { it.fileSize }
        "Old APKs" -> apkItems.filter { it.checked }.sumOf { it.size }
        "App Cache" -> cacheItems.filter { it.checked }.sumOf { it.size }
        else -> 0L
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Junk & Space Purge",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "On-Device Neural Sanitation",
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
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (activeCategory != "Blurry Media" && activeListSize > 0) {
                        TextButton(
                            onClick = {
                                val allChecked = when (activeCategory) {
                                    "WhatsApp" -> whatsappItems.all { it.checked }
                                    "Old APKs" -> apkItems.all { it.checked }
                                    "App Cache" -> cacheItems.all { it.checked }
                                    else -> false
                                }
                                cleanerViewModel.selectAllJunk(activeCategory, !allChecked)
                            }
                        ) {
                            val allChecked = when (activeCategory) {
                                "WhatsApp" -> whatsappItems.all { it.checked }
                                "Old APKs" -> apkItems.all { it.checked }
                                "App Cache" -> cacheItems.all { it.checked }
                                else -> false
                            }
                            Text(
                                text = if (allChecked) "Deselect All" else "Select All",
                                color = CyberPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
        bottomBar = {
            if (activeCategory == "Blurry Media" && scanState is ScanUiState.Scanned && selectedBlurryUris.isNotEmpty()) {
                BottomActionBar(
                    selectedCount = selectedBlurryUris.size,
                    savingsBytes = activeSelectedSize,
                    buttonText = "Purge Blurry Images",
                    testTag = "purge_blurry_button",
                    onAction = {
                        val count = selectedBlurryUris.size
                        val sizeStr = formatFileSize(activeSelectedSize)
                        cleanerViewModel.deleteSelectedDuplicates {
                            Toast.makeText(context, "Purged $count blurry media ($sizeStr saved)!", Toast.LENGTH_LONG).show()
                            onNavigateBack()
                        }
                    }
                )
            } else if (activeCategory != "Blurry Media" && activeSelectedCount > 0) {
                BottomActionBar(
                    selectedCount = activeSelectedCount,
                    savingsBytes = activeSelectedSize,
                    buttonText = "Purge Selected",
                    testTag = "purge_junk_button",
                    onAction = {
                        val count = activeSelectedCount
                        val sizeStr = formatFileSize(activeSelectedSize)
                        when (activeCategory) {
                            "WhatsApp" -> {
                                val ids = whatsappItems.filter { it.checked }.map { it.id }
                                cleanerViewModel.deleteWhatsAppItems(ids)
                            }
                            "Old APKs" -> {
                                val ids = apkItems.filter { it.checked }.map { it.id }
                                cleanerViewModel.deleteApkItems(ids)
                            }
                            "App Cache" -> {
                                val ids = cacheItems.filter { it.checked }.map { it.id }
                                cleanerViewModel.deleteCacheItems(ids)
                            }
                        }
                        Toast.makeText(context, "Cleared $count items ($sizeStr reclaimed)!", Toast.LENGTH_LONG).show()
                        onNavigateBack()
                    }
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category Slider Row
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = DarkBackground,
                contentColor = CyberPrimary,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = CyberPrimary
                    )
                },
                divider = { HorizontalDivider(color = Color(0x11FFFFFF)) }
            ) {
                categories.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selectedTabIndex == index) CyberPrimary.copy(alpha = 0.15f)
                                            else DarkSurfaceVariant
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = when (title) {
                                            "WhatsApp" -> formatSizeShort(whatsappSize)
                                            "Blurry Media" -> formatSizeShort(blurrySize)
                                            "Old APKs" -> formatSizeShort(apkSize)
                                            "App Cache" -> formatSizeShort(cacheSize)
                                            else -> "0B"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedTabIndex == index) CyberPrimary else TextSecondary
                                    )
                                }
                            }
                        },
                        selectedContentColor = CyberPrimary,
                        unselectedContentColor = TextSecondary,
                        modifier = Modifier.testTag("tab_$title")
                    )
                }
            }

            // Central content depending on active tab
            Spacer(modifier = Modifier.height(12.dp))

            when (activeCategory) {
                "Blurry Media" -> {
                    if (scanState is ScanUiState.Unscanned) {
                        FullScreenMessagePlaceholder(
                            icon = Icons.Default.ImageNotSupported,
                            title = "Unscanned Library",
                            description = "Blurry Media analysis requires a complete local storage scan. Go back and trigger a safe diagnostic cleanup scan.",
                            buttonText = "Perform Dashboard Scan",
                            onAction = { cleanerViewModel.startScan() }
                        )
                    } else if (scanState is ScanUiState.Scanning) {
                        FullProgressScanningView(progress = (scanState as ScanUiState.Scanning).progress)
                    } else if (blurryPhotos.isEmpty()) {
                        FullScreenMessagePlaceholder(
                            icon = Icons.Default.CheckCircle,
                            iconColor = KeeperGreen,
                            title = "No Blurry Files!",
                            description = "Excellent focus across your library! On-device camera matches indicate zero blurred images are taking up space.",
                            showButton = false
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 120.dp, start = 16.dp, end = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                InfoHeaderCard(
                                    emoji = "🎞️",
                                    title = "Laplacian Focus Analytics",
                                    subtitle = "Found ${blurryPhotos.size} micro-blur files. Out-of-focus images can be cleared instantly.",
                                    savingsString = formatFileSize(blurrySize)
                                )
                            }
                            items(blurryPhotos, key = { it.uri }) { photo ->
                                BlurryPhotoListItem(
                                    photo = photo,
                                    isSelected = selectedBlurryUris.contains(photo.uri),
                                    onToggle = { cleanerViewModel.toggleSelection(photo.uri) }
                                )
                            }
                        }
                    }
                }
                else -> {
                    val activeList = when (activeCategory) {
                        "WhatsApp" -> whatsappItems
                        "Old APKs" -> apkItems
                        "App Cache" -> cacheItems
                        else -> emptyList()
                    }

                    if (activeList.isEmpty()) {
                        FullScreenMessagePlaceholder(
                            icon = Icons.Default.OfflinePin,
                            iconColor = KeeperGreen,
                            title = "Storage Cleaned & Optimized",
                            description = "All space wasters in this category have been sanitized. Your active partition is fully optimized.",
                            showButton = false
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 120.dp, start = 16.dp, end = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                InfoHeaderCard(
                                    emoji = when (activeCategory) {
                                        "WhatsApp" -> "💬"
                                        "Old APKs" -> "⚙️"
                                        "App Cache" -> "🗑️"
                                        else -> "📦"
                                    },
                                    title = when (activeCategory) {
                                        "WhatsApp" -> "WhatsApp Storage"
                                        "Old APKs" -> "Old App Installers"
                                        "App Cache" -> "Sthumbs & Cache Partition"
                                        else -> "Residual Junk"
                                    },
                                    subtitle = when (activeCategory) {
                                        "WhatsApp" -> "Stale videos, duplicate images & chat voice-note archives on disk."
                                        "Old APKs" -> "Leftover package installers (.apk) from manual code builds or browser downloads."
                                        "App Cache" -> "Application thumbnail streams, temporary JSON caching feeds & render scraps."
                                        else -> "Miscellaneous stale files."
                                    },
                                    savingsString = formatFileSize(when (activeCategory) {
                                        "WhatsApp" -> whatsappSize
                                        "Old APKs" -> apkSize
                                        "App Cache" -> cacheSize
                                        else -> 0L
                                    })
                                )
                            }

                            items(activeList, key = { it.id }) { item ->
                                JunkListItem(
                                    item = item,
                                    onToggle = {
                                        when (activeCategory) {
                                            "WhatsApp" -> cleanerViewModel.toggleWhatsAppItem(item.id)
                                            "Old APKs" -> cleanerViewModel.toggleApkItem(item.id)
                                            "App Cache" -> cleanerViewModel.toggleCacheItem(item.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoHeaderCard(
    emoji: String,
    title: String,
    subtitle: String,
    savingsString: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0x0FFFFFFF)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(CyberPrimary.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 28.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = OnDarkBackground
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(ElectricPink, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Potential Savings: $savingsString",
                        color = ElectricPink,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun JunkListItem(
    item: JunkItem,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface.copy(alpha = 0.2f))
            .border(1.dp, DarkSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable { onToggle() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(DarkSurfaceVariant, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (item.name.endsWith(".mp4") || item.name.endsWith(".zip")) Icons.AutoMirrored.Filled.InsertDriveFile
                else if (item.name.endsWith(".apk")) Icons.Default.SettingsApplications
                else Icons.Default.FolderOpen,
                contentDescription = null,
                tint = CyberPrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = OnDarkBackground,
                maxLines = 1
            )
            Text(
                text = item.detail,
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = "${item.dateString} • ${formatFileSize(item.size)}",
                fontSize = 10.sp,
                color = TextSecondary.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 1.dp)
            )
        }

        Checkbox(
            checked = item.checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = CyberPrimary,
                uncheckedColor = TextSecondary,
                checkmarkColor = OnCyberPrimary
            ),
            modifier = Modifier.testTag("junk_select_${item.id}")
        )
    }
}

@Composable
fun BlurryPhotoListItem(
    photo: PhotoEmbedding,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface.copy(alpha = 0.2f))
            .border(1.dp, DarkSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable { onToggle() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(DarkSurfaceVariant, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.BlurOn,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x99000000))
                    .align(Alignment.BottomCenter)
                    .padding(vertical = 1.dp)
            ) {
                Text(
                    text = "${photo.width}x${photo.height}",
                    fontSize = 8.sp,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = photo.fileName,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = OnDarkBackground,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(Color(0x33FF5252), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "BLURY SCORE %.1f".format(photo.blurScore),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = ErrorRed
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatFileSize(photo.fileSize),
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = CyberPrimary,
                uncheckedColor = TextSecondary,
                checkmarkColor = OnCyberPrimary
            ),
            modifier = Modifier.testTag("blurry_select_${photo.uri}")
        )
    }
}

@Composable
fun BottomActionBar(
    selectedCount: Int,
    savingsBytes: Long,
    buttonText: String,
    testTag: String,
    onAction: () -> Unit
) {
    Surface(
        color = DarkSurface,
        tonalElevation = 8.dp,
        border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$selectedCount files selected",
                        color = OnDarkBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Saves ${formatFileSize(savingsBytes)}",
                        color = ElectricPink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberPrimary,
                        contentColor = OnCyberPrimary
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .height(50.dp)
                        .testTag(testTag),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = buttonText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FullScreenMessagePlaceholder(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color = TextSecondary,
    title: String,
    description: String,
    showButton: Boolean = true,
    buttonText: String = "",
    onAction: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = OnDarkBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            if (showButton) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberPrimary,
                        contentColor = OnCyberPrimary
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(text = buttonText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FullProgressScanningView(progress: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            CircularProgressIndicator(
                color = CyberPrimary,
                strokeWidth = 6.dp,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Aligning Cognitive Layers...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OnDarkBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Processing perceptual Laplacian blur scores ($progress%)",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progress / 100f },
                color = CyberPrimary,
                trackColor = DarkSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
        }
    }
}

private fun formatSizeShort(bytes: Long): String {
    if (bytes <= 0) return "0B"
    if (bytes >= 1024 * 1024 * 1024) {
        return "${"%.1f".format(bytes.toDouble() / (1024 * 1024 * 1024))}G"
    } else if (bytes >= 1024 * 1024) {
        return "${"%.0f".format(bytes.toDouble() / (1024 * 1024))}M"
    } else if (bytes >= 1024) {
        return "${"%.0f".format(bytes.toDouble() / 1024)}k"
    }
    return "${bytes}B"
}
