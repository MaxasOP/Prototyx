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
    surfaceContainer = Graphite, surfaceContainerLow = InkElevated,
    surfaceContainerHigh = GraphiteSoft, surfaceContainerHighest = GraphiteSoft,
    secondaryContainer = GraphiteSoft, onSecondaryContainer = Cloud,
    outline = HairlineBright, outlineVariant = Hairline,
    error = Coral, onError = Ink
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF344D35), onPrimary = Color(0xFFFAF8F1),
    primaryContainer = Color(0xFFE4E9DB), onPrimaryContainer = Color(0xFF263B27),
    secondary = Color(0xFF685F4C), onSecondary = Color.White,
    secondaryContainer = Color(0xFFEAE5D9), onSecondaryContainer = Color(0xFF292B25),
    tertiary = Color(0xFF356546), onTertiary = Color.White,
    background = Color(0xFFF6F3EB), onBackground = Color(0xFF252923),
    surface = Color(0xFFF6F3EB), onSurface = Color(0xFF252923),
    surfaceVariant = Color(0xFFECE8DD), onSurfaceVariant = Color(0xFF65695E),
    surfaceContainerLowest = Color(0xFFFCFAF5), surfaceContainerLow = Color(0xFFF2EEE4),
    surfaceContainer = Color(0xFFECE8DD), surfaceContainerHigh = Color(0xFFE6E2D6),
    surfaceContainerHighest = Color(0xFFDEDACD),
    outline = Color(0xFF878B7D), outlineVariant = Color(0xFFD2D3C5),
    error = Color(0xFFA23E31), onError = Color.White
)

val PrototyxShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp), large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun PrototyxTheme(
    darkTheme: Boolean = false,
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
