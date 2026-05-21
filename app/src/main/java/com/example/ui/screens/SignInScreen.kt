package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.AuthViewModel

@Composable
fun SignInScreen(
    authViewModel: AuthViewModel,
    onSignInSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by authViewModel.uiState.collectAsState()
    val userSession by authViewModel.userSession.collectAsState()

    var showSandboxDialog by remember { mutableStateOf(false) }
    var sandboxEmail by remember { mutableStateOf("") }
    var sandboxName by remember { mutableStateOf("") }

    // Navigation trigger on login success
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success || userSession?.userId != null) {
            onSignInSuccess()
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Error) {
            Toast.makeText(context, (uiState as AuthUiState.Error).message, Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Futuristic grid element in background using canvas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val brush = Brush.radialGradient(
                        colors = listOf(QuantumBlue.copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.15f),
                        radius = size.width * 0.8f
                    )
                    drawCircle(
                        brush = brush,
                        center = Offset(size.width * 0.5f, size.height * 0.15f)
                    )
                }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // APP HERO BLOCK
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                // Outer glowing circle base
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(
                            Brush.linearGradient(listOf(CyberPrimary, QuantumBlue)),
                            CircleShape
                        )
                        .padding(3.dp)
                        .background(DarkBackground, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(QuantumBlue.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, CyberPrimary.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Shield Guard AI Logo",
                            modifier = Modifier.size(36.dp),
                            tint = CyberPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "SMART CLEANER",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Text(
                    text = "Smart Storage Cleaner AI",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = OnDarkBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.txtHeight())
                )

                Text(
                    text = "Privacy-first on-device intelligence. Never uploads photos or personal media to cloud servers. Zero data leaves your phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp)
                )
            }

            // MIDDLE FEATURE HIGHLIGHT CARDS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FeatureRow(
                    icon = Icons.Default.CheckCircle,
                    iconColor = CyberPrimary,
                    title = "On-Device AI Scans",
                    subtitle = "Embeddings-based model runs completely on phone to group matches."
                )
                FeatureRow(
                    icon = Icons.Default.PhotoLibrary,
                    iconColor = QuantumBlue,
                    title = "Deep Photo Analysis",
                    subtitle = "Identifies duplicate structures, screenshots, and blurry files securely."
                )
                FeatureRow(
                    icon = Icons.Default.Lock,
                    iconColor = ElectricPink,
                    title = "Tethered Local Authority",
                    subtitle = "No deletes without explicit confirmation. No random ads or scripts."
                )
            }

            // CONTROLS & AUTH BUTTONS
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(
                        color = CyberPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Authenticating secure framework...",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberPrimary
                    )
                } else {
                    // Standard Google Sign In UI Button
                    Button(
                        onClick = { authViewModel.signInWithGoogle(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_signin_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = QuantumBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Google Icon",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continue with Google",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    // Divider with label
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = DarkSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "DEVELOPER SANDBOX BYPASS",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 16.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = DarkSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }

                    // Direct Demo Developer Bypass Button
                    OutlinedButton(
                        onClick = { showSandboxDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("dev_sandbox_bypass_button"),
                        border = BorderStroke(1.dp, CyberPrimary.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CyberPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "Sandbox",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Launch Sandbox Mock User",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = "Protected with hardware-level security & Credential Manager verification. Powered by TFLite MobileNet v3.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)
                )
            }
        }
    }

    // Interactive developer user creation dialog
    if (showSandboxDialog) {
        AlertDialog(
            onDismissRequest = { showSandboxDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Dev Mode",
                        tint = CyberPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "AI Developer Sandbox", color = OnDarkBackground)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Sign-in without Google Play Services. Enter metadata values to load state.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = sandboxName,
                        onValueChange = { sandboxName = it },
                        label = { Text("Display Name") },
                        placeholder = { Text("Ansil Muhammed") },
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberPrimary,
                            unfocusedBorderColor = DarkSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("sandbox_name_input")
                    )
                    OutlinedTextField(
                        value = sandboxEmail,
                        onValueChange = { sandboxEmail = it },
                        label = { Text("Email Address") },
                        placeholder = { Text("ansil@gmail.com") },
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberPrimary,
                            unfocusedBorderColor = DarkSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("sandbox_email_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSandboxDialog = false
                        val email = sandboxEmail.trim().ifEmpty { "ansilmuhammed919@gmail.com" }
                        val name = sandboxName.trim().ifEmpty { "Ansil Muhammed" }
                        authViewModel.simulateSignInForTesting(email, name)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary, contentColor = OnCyberPrimary),
                    modifier = Modifier.testTag("sandbox_confirm_button")
                ) {
                    Text("Unlock & Sync")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSandboxDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface,
            tonalElevation = 6.dp
        )
    }
}

@Composable
fun FeatureRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.4f))
            .border(1.dp, DarkSurfaceVariant.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconColor.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = OnDarkBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

// Inline helper for sizing safely
private fun Int.txtHeight(): androidx.compose.ui.unit.Dp = this.dp
