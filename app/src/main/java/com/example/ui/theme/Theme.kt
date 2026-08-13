package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SoraDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = DeepDarkBg,
    primaryContainer = GlassSurfaceVariant,
    onPrimaryContainer = NeonCyan,
    secondary = NeonPurple,
    onSecondary = TextPrimary,
    secondaryContainer = GlassSurface,
    onSecondaryContainer = NeonPurple,
    tertiary = ElectricPink,
    onTertiary = TextPrimary,
    background = DeepDarkBg,
    onBackground = TextPrimary,
    surface = GlassSurface,
    onSurface = TextPrimary,
    surfaceVariant = GlassSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = NeonCyan.copy(alpha = 0.3f),
    error = AccentRed
)

@Composable
fun SoraAiStudioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SoraDarkColorScheme,
        typography = Typography,
        content = content
    )
}

