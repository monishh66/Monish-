package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CinemaColorScheme = darkColorScheme(
    primary = CinemaCrimson,
    onPrimary = Color.White,
    primaryContainer = CinemaCrimsonHover,
    onPrimaryContainer = Color.White,
    secondary = CinemaAmber,
    onSecondary = Color.Black,
    secondaryContainer = CinemaSurfaceVariant,
    onSecondaryContainer = CinemaAmber,
    tertiary = CinemaCyan,
    onTertiary = Color.Black,
    background = CinemaBackground,
    onBackground = TextWhite,
    surface = CinemaSurface,
    onSurface = TextWhite,
    surfaceVariant = CinemaSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor,
    error = CinemaCrimson
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Always enforce premium cinema theme
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = CinemaColorScheme,
        typography = Typography,
        content = content
    )
}
