package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GridCyan,
    onPrimary = GridDarkNavy,
    primaryContainer = GridElectricBlue,
    onPrimaryContainer = Color.White,
    secondary = EnergyGold,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = EnergyAmberLight,
    tertiary = ReliabilityGreen,
    onTertiary = Color.Black,
    background = GridDeepBg,
    onBackground = TextPrimary,
    surface = GridSurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = GridSurfaceCard,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    error = RiskRed
)

private val LightColorScheme = lightColorScheme(
    primary = GridPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = EnergyGold,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = GridSecondaryLight,
    onTertiary = Color.White,
    background = GridBgLight,
    onBackground = TextPrimaryLight,
    surface = GridSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = GridCardLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFCBD5E1),
    error = RiskRed
)

@Composable
fun GridPilotTheme(
    darkTheme: Boolean = true, // Default to high-contrast technical dark theme preferred in grid control rooms
    dynamicColor: Boolean = false, // Keep consistent branding for grid operations
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
