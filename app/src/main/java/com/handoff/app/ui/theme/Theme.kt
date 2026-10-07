package com.handoff.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = AccentSage,
    onPrimary = Color.White,
    primaryContainer = SageContainer,
    onPrimaryContainer = SageOnContainer,
    secondary = TextSecondary,
    onSecondary = Color.White,
    secondaryContainer = SurfaceSecondary,
    onSecondaryContainer = TextPrimary,
    tertiary = ClaudeOrange,
    onTertiary = Color.White,
    tertiaryContainer = ClaudeTint,
    onTertiaryContainer = Color(0xFF5C3221),
    background = CreamBackground,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceSecondary,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = SurfaceWhite,
    surfaceContainerLow = SurfaceWhite,
    surfaceContainerLowest = SurfaceWhite,
    surfaceContainerHigh = SurfaceSecondary,
    surfaceContainerHighest = SurfaceSecondary,
    outline = Hairline,
    outlineVariant = Hairline,
    error = AccentError,
    onError = Color.White,
    errorContainer = ErrorContainer,
    onErrorContainer = ErrorOnContainer,
    inverseSurface = DarkSurface,
    inverseOnSurface = DarkTextPrimary,
    inversePrimary = DarkAccentSage,
    surfaceTint = Color.Transparent,
    scrim = Color(0x521B1816),
)

private val DarkColors = darkColorScheme(
    primary = DarkAccentSage,
    onPrimary = Color(0xFF16261D),
    primaryContainer = DarkSageContainer,
    onPrimaryContainer = DarkSageOnContainer,
    secondary = DarkTextSecondary,
    onSecondary = Color(0xFF1B1816),
    secondaryContainer = DarkSurfaceSecondary,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = DarkAccentSage,
    onTertiary = Color(0xFF16261D),
    tertiaryContainer = DarkClaudeTint,
    onTertiaryContainer = Color(0xFFF3CDBD),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceSecondary,
    onSurfaceVariant = DarkTextSecondary,
    surfaceContainer = DarkSurface,
    surfaceContainerLow = DarkSurface,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerHigh = DarkSurfaceSecondary,
    surfaceContainerHighest = DarkSurfaceSecondary,
    outline = DarkHairline,
    outlineVariant = DarkHairline,
    error = DarkError,
    onError = Color(0xFF3B1511),
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkErrorOnContainer,
    inverseSurface = SurfaceWhite,
    inverseOnSurface = TextPrimary,
    inversePrimary = AccentSage,
    surfaceTint = Color.Transparent,
    scrim = Color(0x66100D0B),
)

/** App theme: warm iOS-style skin on top of Material3, Inter type, custom shapes. */
@Composable
fun HandoffTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = HandoffTypography,
        shapes = HandoffShapes,
        content = content,
    )
}
