package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TanPlayColorScheme = darkColorScheme(
    primary = HighClarityWhite,
    onPrimary = ObsidianBase,
    primaryContainer = ElectricLime,
    onPrimaryContainer = OnElectricLime,
    inversePrimary = ElectricLimeDim,
    secondary = SlateSecondary,
    onSecondary = ObsidianBase,
    secondaryContainer = ObsidianContainerHigh,
    onSecondaryContainer = SlateSecondary,
    tertiary = EmeraldTertiary,
    onTertiary = OnEmeraldTertiary,
    tertiaryContainer = ObsidianContainerHigh,
    onTertiaryContainer = EmeraldTertiaryDim,
    background = ObsidianBase,
    onBackground = HighClarityWhite,
    surface = ObsidianSurface,
    onSurface = HighClarityWhite,
    surfaceVariant = ObsidianContainerHighest,
    onSurfaceVariant = MutedSlate,
    surfaceTint = ElectricLime,
    outline = BorderDivider,
    outlineVariant = BorderDivider,
    error = LiveRed,
    onError = Color.White,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

@Composable
fun TanPlayTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TanPlayColorScheme,
        typography = Typography,
        content = content
    )
}
