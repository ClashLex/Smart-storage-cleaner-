package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ServiceLocator
import com.example.ui.theme.*
import com.example.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    authViewModel: AuthViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userSession by authViewModel.userSession.collectAsState()
    val isPremium = userSession?.isPremium ?: false
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var selectedTier by remember { mutableStateOf("cleaner_pro_annual") } // "cleaner_pro_monthly", "cleaner_pro_annual", "cleaner_lifetime"
    var isProcessingPurchase by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Cleaner Pro", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("paywall_close_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    // Sandbox debug reset mechanism
                    if (isPremium) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    ServiceLocator.userPreferencesRepository.updatePremiumStatus(false, 0L)
                                    Toast.makeText(context, "Sandbox Profile Reset back to FREE level!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Reset Free Tier", color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Image/Icon Display Box with Gradient Background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(CyberPrimary.copy(alpha = 0.25f), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = ElectricPink,
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "UNLEASH FULL POTENTIAL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = CyberPrimary
                    )
                }
            }

            AnimatedContent(
                targetState = isPremium,
                label = "paywall_content_animation"
            ) { premiumActive ->
                if (premiumActive) {
                    // Premium Active Screen Layout
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(32.dp),
                            border = BorderStroke(1.dp, CyberPrimary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(CyberPrimary.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Success",
                                        tint = CyberPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = "Premium Active",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 24.sp,
                                    color = OnDarkBackground,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "All pro filters, automated scans, and Laplacian clustering layers are fully unlocked.",
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberPrimary,
                                contentColor = OnCyberPrimary
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text("Return to Storage Dashboard", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                } else {
                    // Free Tier - Pricing & Upgrade Grid
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Features Checklist
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PaywallFeatureRow(icon = Icons.Default.BlurOn, text = "Laplacian Micro-blur Cluster Filter")
                            PaywallFeatureRow(icon = Icons.Default.Update, text = "Auto scheduled quiet hours sweeps")
                            PaywallFeatureRow(icon = Icons.Default.AllInclusive, text = "Infinite matching & space scanning")
                            PaywallFeatureRow(icon = Icons.Default.CloudSync, text = "Cloud preference sync across items")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Tiers list Selection Grid
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Monthly
                            PaywallTierCard(
                                productId = "cleaner_pro_monthly",
                                title = "Monthly Pro",
                                price = "$1.99 / mo",
                                badgeText = null,
                                selected = selectedTier == "cleaner_pro_monthly",
                                onSelect = { selectedTier = "cleaner_pro_monthly" }
                            )

                            // Annual
                            PaywallTierCard(
                                productId = "cleaner_pro_annual",
                                title = "Annual Pro",
                                price = "$9.99 / yr",
                                badgeText = "BEST SAVINGS • 50% OFF",
                                selected = selectedTier == "cleaner_pro_annual",
                                onSelect = { selectedTier = "cleaner_pro_annual" }
                            )

                            // Lifetime
                            PaywallTierCard(
                                productId = "cleaner_lifetime",
                                title = "Forever Premium",
                                price = "$19.99 Single Buy",
                                badgeText = "ULTIMATE ACCESS",
                                selected = selectedTier == "cleaner_lifetime",
                                onSelect = { selectedTier = "cleaner_lifetime" }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Checkout Button
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (isProcessingPurchase) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(color = CyberPrimary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Authorizing sandbox token...", color = OnDarkBackground, fontSize = 14.sp)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        isProcessingPurchase = true
                                        authViewModel.purchasePremium(selectedTier) { success ->
                                            isProcessingPurchase = false
                                            if (success) {
                                                Toast.makeText(
                                                    context,
                                                    "Successfully upgraded! Play sandbox token verified.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Upgrade aborted. Please retry checkout.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyberPrimary,
                                        contentColor = OnCyberPrimary
                                    ),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("subscribe_premium_button")
                                ) {
                                    Icon(imageVector = Icons.Default.OfflineBolt, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Upgrade to Pro now", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }

                        // Sandbox simulation label
                        Text(
                            text = "Play Billing v6 Sandbox Mode Enabled",
                            fontSize = 11.sp,
                            color = TextSecondary.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PaywallFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CyberPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = OnDarkBackground
        )
    }
}

@Composable
fun PaywallTierCard(
    productId: String,
    title: String,
    price: String,
    badgeText: String?,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) DarkSurfaceVariant else DarkSurface
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) CyberPrimary else Color(0x11FFFFFF)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("tier_$productId")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (badgeText != null) {
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(CyberPrimary, ElectricPink)
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = OnCyberPrimary,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selected,
                        onClick = onSelect,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = CyberPrimary,
                            unselectedColor = TextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = OnDarkBackground
                    )
                }

                Text(
                    text = price,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (selected) CyberPrimary else OnDarkBackground
                )
            }
        }
    }
}
