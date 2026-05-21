package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CyberPrimary,
    onPrimary = OnCyberPrimary,
    secondary = QuantumBlue,
    onSecondary = OnQuantumBlue,
    tertiary = ElectricPink,
    onTertiary = OnElectricPink,
    background = DarkBackground,
    onBackground = OnDarkBackground,
    surface = DarkSurface,
    onSurface = OnDarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkSurface
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    // Force modern dark theme throughout for a premium cyber cleaner vibe
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
