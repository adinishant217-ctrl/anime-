package com.anistream.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.anistream.app.domain.model.ThemeMode

private val MidnightScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = Color(0xFF0B1020),
    primaryContainer = AccentCyanDark,
    onPrimaryContainer = MidnightText,
    secondary = AccentPink,
    onSecondary = Color(0xFF0B1020),
    tertiary = AccentViolet,
    onTertiary = MidnightText,
    background = MidnightBackground,
    onBackground = MidnightText,
    surface = MidnightSurface,
    onSurface = MidnightText,
    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = MidnightTextMuted,
    outline = MidnightOutline,
    error = ErrorRed,
    onError = Color(0xFF0B1020)
)

private val DarkScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = DarkBackground,
    primaryContainer = AccentCyanDark,
    onPrimaryContainer = DarkText,
    secondary = AccentPink,
    onSecondary = DarkBackground,
    tertiary = AccentViolet,
    onTertiary = DarkText,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextMuted,
    outline = DarkOutline,
    error = ErrorRed,
    onError = DarkBackground
)

private val AmoledScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = AmoledBackground,
    primaryContainer = AccentCyanDark,
    onPrimaryContainer = AmoledText,
    secondary = AccentPink,
    onSecondary = AmoledBackground,
    tertiary = AccentViolet,
    onTertiary = AmoledText,
    background = AmoledBackground,
    onBackground = AmoledText,
    surface = AmoledSurface,
    onSurface = AmoledText,
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = AmoledTextMuted,
    outline = AmoledOutline,
    error = ErrorRed,
    onError = AmoledBackground
)

private val AniStreamShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AniStreamTheme(
    themeMode: ThemeMode = ThemeMode.MIDNIGHT,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.MIDNIGHT -> MidnightScheme
        ThemeMode.DARK -> DarkScheme
        ThemeMode.AMOLED -> AmoledScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AniStreamTypography,
        shapes = AniStreamShapes,
        content = content
    )
}
