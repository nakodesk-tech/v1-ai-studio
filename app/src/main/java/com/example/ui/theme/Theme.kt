package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MintPrimary,
    onPrimary = Color.White,
    secondary = SoftBlueIcon,
    background = Color(0xFF121A15),
    surface = Color(0xFF1A2620),
    onBackground = TextLightOnDark,
    onSurface = TextLightOnDark
)

private val LightColorScheme = lightColorScheme(
    primary = ForestDarkGreen,
    onPrimary = Color.White,
    primaryContainer = SoftGreenBg,
    onPrimaryContainer = ForestDarkGreen,
    secondary = SoftBlueIcon,
    onSecondary = Color.White,
    secondaryContainer = SoftBlueBg,
    onSecondaryContainer = SoftBlueIcon,
    tertiary = SoftOrangeIcon,
    tertiaryContainer = SoftOrangeBg,
    background = MintBackground,
    onBackground = TextDarkPrimary,
    surface = SurfaceWhite,
    onSurface = TextDarkPrimary,
    surfaceVariant = MintBackground,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceCardBorder
)

@Composable
fun EduDataTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    EduDataTheme(darkTheme = darkTheme, content = content)
}

