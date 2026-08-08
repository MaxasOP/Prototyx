package com.example.prototyx.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SurfaceWhite,
    onPrimary = TextCharcoal,
    secondary = PaleBlue,
    onSecondary = PaleBlueText,
    background = TextCharcoal,
    surface = TextCharcoal,
    onBackground = SurfaceWhite,
    onSurface = SurfaceWhite
)

private val LightColorScheme = lightColorScheme(
    primary = TextCharcoal,
    onPrimary = SurfaceWhite,
    secondary = PaleBlue,
    onSecondary = PaleBlueText,
    tertiary = PaleGreen,
    onTertiary = PaleGreenText,
    background = BackgroundWarm,
    surface = SurfaceWhite,
    onBackground = TextCharcoal,
    onSurface = TextCharcoal,
    surfaceVariant = SurfaceWhite,
    onSurfaceVariant = TextMuted,
    outline = BorderLight
)

@Composable
fun PrototyxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
