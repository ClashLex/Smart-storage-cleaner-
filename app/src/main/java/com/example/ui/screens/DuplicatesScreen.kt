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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.data.DuplicateGroup
import com.example.data.database.PhotoEmbedding
import com.example.ui.theme.*
import com.example.ui.viewmodel.CleanerViewModel
import com.example.ui.viewmodel.ScanUiState
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatesScreen(
    onNavigateBack: () -> Unit,
    cleanerViewModel: CleanerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scanState by cleanerViewModel.scanState.collectAsState()
    val duplicateGroups by cleanerViewModel.duplicateGroups.collectAsState()
    val selectedUris by cleanerViewModel.selectedUris.collectAsState()

    val totalPotentialSavings = duplicateGroups.sumOf { it.potentialSavings }
    val selectedSavings = duplicateGroups.flatMap { g ->
        g.duplicates.filter { d -> selectedUris.contains(d.uri) }
    }.sumOf { it.fileSize }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI Duplicate Finder",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "TFLite Spatial Clustering",
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
                    if (duplicateGroups.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                if (selectedUris.size == duplicateGroups.flatMap { it.duplicates }.size) {
                                    cleanerViewModel.selectNone()
                                } else {
                                    cleanerViewModel.selectAll()
                                }
                            }
                        ) {
                            Text(
                                text = if (selectedUris.size == duplicateGroups.flatMap { it.duplicates }.size) "Deselect All" else "Select All",
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
            if (duplicateGroups.isNotEmpty() && selectedUris.isNotEmpty()) {
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
                                    text = "${selectedUris.size} files selected",
                                    color = OnDarkBackground,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Reclaiming ${formatFileSize(selectedSavings)}",
                                    color = ElectricPink,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = {
                                    val count = selectedUris.size
                                    val sizeStr = formatFileSize(selectedSavings)
                                    cleanerViewModel.deleteSelectedDuplicates {
                                        Toast.makeText(
                                            context,
                                            "Cleaned $count duplicates ($sizeStr saved)!",
                                            Toast.LENGTH_LONG
                                        ).onSuccess {
                                            onNavigateBack()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberPrimary,
                                    contentColor = OnCyberPrimary
                                ),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .height(50.dp)
                                    .testTag("delete_selected_button"),
                                border = BorderStroke(1.dp, Color(0x33FFFFFF))
                            ) {
                                Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Delete duplicates", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        when (scanState) {
            is ScanUiState.Unscanned -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ImageNotSupported,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Unscanned Local Storage",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = OnDarkBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Run a smart optimizer scan from the home library to find clusters of identical photo entries safely.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { cleanerViewModel.startScan() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberPrimary,
                                contentColor = OnCyberPrimary
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Scan Media Library Now", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            is ScanUiState.Scanning -> {
                val progress = (scanState as ScanUiState.Scanning).progress
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        CircularProgressIndicator(
                            color = CyberPrimary,
                            strokeWidth = 6.dp,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Analyzing Spatial Signatures...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnDarkBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Extracting perceptual dHash profiles and continuous embedding weights on-device ($progress%)",
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
            is ScanUiState.Scanned -> {
                if (duplicateGroups.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = KeeperGreen,
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Zero Duplicates Found!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = OnDarkBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your media library is pristine. All duplicates and blurry variants are fully optimized.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            TextButton(onClick = onNavigateBack) {
                                Text("Go Back to Dashboard", color = CyberPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(bottom = 120.dp, start = 16.dp, end = 16.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = CyberPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "${duplicateGroups.size} Duplicate Clusters Detected",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = OnDarkBackground
                                        )
                                        Text(
                                            text = "Safely clearing unrecommended copies can salvage up to ${formatFileSize(totalPotentialSavings)} on disk.",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        items(duplicateGroups, key = { it.groupId }) { group ->
                            DuplicateGroupCard(
                                group = group,
                                selectedUris = selectedUris,
                                onToggle = { uri -> cleanerViewModel.toggleSelection(uri) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DuplicateGroupCard(
    group: DuplicateGroup,
    selectedUris: Set<String>,
    onToggle: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, Color(0x0FFFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Group header metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0x1AB2D183), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterNone,
                            contentDescription = null,
                            tint = CyberPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Match Group (${1 + group.duplicates.size})",
                        fontWeight = FontWeight.Bold,
                        color = OnDarkBackground,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = "Savings: ${formatFileSize(group.potentialSavings)}",
                    fontWeight = FontWeight.Bold,
                    color = ElectricPink,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. The suggested KEEPER (Best quality, usually compressed-source reference or crispest)
            PhotoRowItem(
                photo = group.keeper,
                isKeeper = true,
                isSelected = false,
                onToggle = {},
                subtitle = "Keeper • Best Focus"
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0x0AFFFFFF), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // 2. The duplicates selected for safe clearance
            group.duplicates.forEachIndexed { index, duplicate ->
                PhotoRowItem(
                    photo = duplicate,
                    isKeeper = false,
                    isSelected = selectedUris.contains(duplicate.uri),
                    onToggle = { onToggle(duplicate.uri) },
                    subtitle = "Duplicate #${index + 1} (${formatBytesDifferencePercentage(group.keeper.fileSize, duplicate.fileSize)})"
                )
                if (index < group.duplicates.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun PhotoRowItem(
    photo: PhotoEmbedding,
    isKeeper: Boolean,
    isSelected: Boolean,
    onToggle: () -> Unit,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !isKeeper) { onToggle() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail card representation
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
                .border(1.dp, if (isKeeper) CyberPrimary else Color.Transparent, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isKeeper) Icons.Default.CheckCircle else Icons.Default.Filter,
                contentDescription = null,
                tint = if (isKeeper) CyberPrimary else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
            // Dimensions indicator bubble
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x99000000))
                    .align(Alignment.BottomCenter)
                    .padding(vertical = 2.dp)
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

        // Info details
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = photo.fileName,
                    fontWeight = FontWeight.Bold,
                    color = OnDarkBackground,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                if (isKeeper) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(CyberPrimary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "BEST",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = OnCyberPrimary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$subtitle • ${formatFileSize(photo.fileSize)}",
                color = TextSecondary,
                fontSize = 11.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BlurOn,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "L-Blur score: ${"%.1f".format(photo.blurScore)}",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }

        if (!isKeeper) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = CyberPrimary,
                    uncheckedColor = TextSecondary,
                    checkmarkColor = OnCyberPrimary
                ),
                modifier = Modifier.testTag("duplicate_select_${photo.uri}")
            )
        } else {
            IconButton(onClick = {}, enabled = false) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Keeper lock",
                    tint = KeeperGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val df = DecimalFormat("#,##0.00")
    if (bytes >= 1024 * 1024 * 1024) {
        return "${df.format(bytes.toDouble() / (1024 * 1024 * 1024))} GB"
    } else if (bytes >= 1024 * 1024) {
        return "${df.format(bytes.toDouble() / (1024 * 1024))} MB"
    } else if (bytes >= 1024) {
        return "${df.format(bytes.toDouble() / 1024)} KB"
    }
    return "$bytes Bytes"
}

fun formatBytesDifferencePercentage(keeperBytes: Long, duplicateBytes: Long): String {
    val diff = duplicateBytes - keeperBytes
    return if (diff == 0L) {
        "identical size"
    } else if (diff < 0L) {
        "compressed ${"%.0f".format((diff.toDouble().coerceAtLeast(-duplicateBytes.toDouble()) / keeperBytes.toDouble()) * -100)}%"
    } else {
        "larger ${"%.0f".format((diff.toDouble() / keeperBytes.toDouble()) * 100)}%"
    }
}

// Safely execute Toast functions without compiler Kotlin warnings
private inline fun <T> T.onSuccess(action: (T) -> Unit): T {
    action(this)
    return this
}
