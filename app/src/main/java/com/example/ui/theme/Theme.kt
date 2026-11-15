package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ObsidianColorScheme = darkColorScheme(
    primary = SheikahCyan,
    onPrimary = ObsidianVoid,
    primaryContainer = SheikahCyanContainer,
    onPrimaryContainer = SheikahCyan,
    secondary = EdgeAmber,
    onSecondary = ObsidianVoid,
    secondaryContainer = EdgeAmberContainer,
    onSecondaryContainer = EdgeAmber,
    tertiary = EmeraldOptimal,
    onTertiary = ObsidianVoid,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = EmeraldOptimal,
    error = CoralAlert,
    onError = ObsidianVoid,
    errorContainer = CoralContainer,
    onErrorContainer = CoralAlert,
    background = ObsidianVoid,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianElevated,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutline.copy(alpha = 0.55f)
)

private val ForgeShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BOTWRecompNeoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ObsidianColorScheme,
        typography = AppTypography,
        shapes = ForgeShapes,
        content = content
    )
}
