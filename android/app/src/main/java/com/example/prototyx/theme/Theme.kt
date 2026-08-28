package com.example.prototyx.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5)
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
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
