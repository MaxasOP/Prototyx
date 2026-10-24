package com.example.prototyx.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SignalBright, onPrimary = Ink,
    primaryContainer = Color(0xFF153A2D), onPrimaryContainer = Color(0xFFB7F1D6),
    secondary = Cyan, onSecondary = Ink,
    tertiary = Mint, onTertiary = Ink,
    background = Ink, onBackground = Cloud,
    surface = InkElevated, onSurface = Cloud,
    surfaceVariant = Graphite, onSurfaceVariant = Mist,
    outline = HairlineBright, outlineVariant = Hairline,
    error = Coral, onError = Ink
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF176B4E), onPrimary = Color.White,
    secondary = Color(0xFF006781),
    background = Color(0xFFF4F5F7), onBackground = Color(0xFF111318),
    surface = Color.White, onSurface = Color(0xFF111318),
    surfaceVariant = Color(0xFFEBEDF1), onSurfaceVariant = Color(0xFF5E6673),
    outline = Color(0xFFD7DAE0), error = Color(0xFFB42335)
)

val PrototyxShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun PrototyxTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = colorScheme.background.toArgb()
        window.navigationBarColor = colorScheme.background.toArgb()
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = PrototyxShapes,
        content = content
    )
}
