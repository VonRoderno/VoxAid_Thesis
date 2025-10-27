package com.voxaid.core.design.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * VoxAid Material3 theme.
 * Provides light and dark color schemes optimized for emergency scenarios.
 */

private val LightColorScheme = lightColorScheme(
    primary = VoxAidBlue,
    onPrimary = Color.White,
    primaryContainer = VoxAidBlueLight,
    onPrimaryContainer = VoxAidBlueDark,

    secondary = VoxAidOrange,
    onSecondary = Color.White,
    secondaryContainer = VoxAidOrangeLight,
    onSecondaryContainer = VoxAidOrangeDark,

    tertiary = VoxAidGreen,
    onTertiary = Color.White,

    error = VoxAidRed,
    onError = Color.White,
    errorContainer = VoxAidRedLight,
    onErrorContainer = VoxAidRedDark,

    background = VoxAidBackgroundLight,
    onBackground = VoxAidTextDark,

    surface = Color.White,
    onSurface = VoxAidTextDark,
    surfaceVariant = VoxAidGray100,
    onSurfaceVariant = VoxAidGray700,

    outline = VoxAidGray300,
    outlineVariant = VoxAidGray200
)

private val DarkColorScheme = darkColorScheme(
    primary = VoxAidBlueLight,
    onPrimary = VoxAidBlueDark,
    primaryContainer = VoxAidBlueDark,
    onPrimaryContainer = VoxAidBlueLight,

    secondary = VoxAidOrangeLight,
    onSecondary = VoxAidOrangeDark,
    secondaryContainer = VoxAidOrangeDark,
    onSecondaryContainer = VoxAidOrangeLight,

    tertiary = VoxAidGreenLight,
    onTertiary = VoxAidGreenDark,

    error = VoxAidRedLight,
    onError = VoxAidRedDark,
    errorContainer = VoxAidRedDark,
    onErrorContainer = VoxAidRedLight,

    background = VoxAidBackgroundDark,
    onBackground = VoxAidTextLight,

    surface = VoxAidSurfaceDark,
    onSurface = VoxAidTextLight,
    surfaceVariant = VoxAidGray800,
    onSurfaceVariant = VoxAidGray300,

    outline = VoxAidGray600,
    outlineVariant = VoxAidGray700
)

@Composable
fun VoxAidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VoxAidTypography,
        content = content
    )
}