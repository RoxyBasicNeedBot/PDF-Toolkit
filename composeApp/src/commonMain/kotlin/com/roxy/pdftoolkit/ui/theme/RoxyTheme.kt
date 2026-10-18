package com.roxy.pdftoolkit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.roxy.pdftoolkit.ui.glass.RoxyDeviceTier

val LocalRoxyDeviceTier = staticCompositionLocalOf { RoxyDeviceTier.TIER_3_FULL_AGSL }
val LocalRoxyIsDarkTheme = staticCompositionLocalOf { true }

private val RoxyDarkColorScheme = darkColorScheme(
    primary = RoxyColor.Primary,
    onPrimary = Color.White,
    primaryContainer = RoxyColor.PrimaryVariant,
    onPrimaryContainer = Color.White,
    secondary = RoxyColor.Secondary,
    onSecondary = Color.Black,
    background = RoxyColor.BackgroundDark,
    onBackground = RoxyColor.TextPrimary,
    surface = RoxyColor.SurfaceDark,
    onSurface = RoxyColor.TextPrimary,
    surfaceVariant = RoxyColor.SurfaceContainerDark,
    onSurfaceVariant = RoxyColor.TextSecondary
)

private val RoxyLightColorScheme = lightColorScheme(
    primary = RoxyColor.Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF3730A3),
    secondary = RoxyColor.Secondary,
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569)
)

@Composable
fun RoxyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    deviceTier: RoxyDeviceTier = RoxyDeviceTier.TIER_3_FULL_AGSL,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) RoxyDarkColorScheme else RoxyLightColorScheme

    CompositionLocalProvider(
        LocalRoxyDeviceTier provides deviceTier,
        LocalRoxyIsDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = RoxyTypography,
            content = content
        )
    }
}
