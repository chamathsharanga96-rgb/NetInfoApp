package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = ProfPrimary,
    onPrimary = ProfOnPrimary,
    primaryContainer = ProfPrimaryContainer,
    onPrimaryContainer = ProfOnPrimaryContainer,
    secondary = TechTurquoise,
    onSecondary = Color.White,
    tertiary = ProfSecondary,
    onTertiary = ProfOnSecondary,
    background = ProfBackground,
    onBackground = ProfOnBackground,
    surface = ProfSurface,
    onSurface = ProfOnSurface,
    surfaceVariant = ProfSurfaceVariant,
    onSurfaceVariant = ProfOnSurfaceVariant,
    outline = ProfOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = ProfDarkPrimary,
    onPrimary = ProfDarkOnPrimary,
    primaryContainer = ProfDarkPrimaryContainer,
    onPrimaryContainer = ProfDarkOnPrimaryContainer,
    secondary = TechTurquoise,
    onSecondary = Color.White,
    tertiary = Color(0xFFBAC7DB),
    onTertiary = Color(0xFF243140),
    background = ProfDarkBackground,
    onBackground = ProfDarkOnBackground,
    surface = ProfDarkSurface,
    onSurface = ProfDarkOnSurface,
    surfaceVariant = ProfDarkSurfaceVariant,
    onSurfaceVariant = ProfDarkOnSurfaceVariant,
    outline = ProfDarkOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Follow system theme preference natively
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
