package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color(0xFF031628),
    primaryContainer = Color(0xFF0C2B45),
    onPrimaryContainer = Color(0xFFBCE7FF),
    secondary = ElectricViolet,
    onSecondary = Color(0xFF141738),
    secondaryContainer = Color(0xFF262A5A),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = EmeraldSuccess,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = DarkSurfaceBorder,
    error = CrimsonDanger,
    onError = Color.White
)

@Composable
fun AgentCoreTheme(
    darkTheme: Boolean = true, // Default to sleek AI Operations dark mode
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
