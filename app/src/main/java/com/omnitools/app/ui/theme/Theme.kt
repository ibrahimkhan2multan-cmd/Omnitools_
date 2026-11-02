package com.omnitools.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

val OmniBlue = Color(0xFF4F8CFF)
val OmniViolet = Color(0xFF8B6CFF)
val OmniNavy = Color(0xFF0E1220)
val OmniSurface = Color(0xFF171C30)
val OmniSurfaceHigh = Color(0xFF1F2540)

private val DarkColors = darkColorScheme(
    primary = OmniBlue,
    onPrimary = Color(0xFF05122E),
    secondary = OmniViolet,
    onSecondary = Color(0xFF14083D),
    tertiary = Color(0xFF58D6B0),
    background = OmniNavy,
    onBackground = Color(0xFFE8EBF7),
    surface = OmniSurface,
    onSurface = Color(0xFFE8EBF7),
    surfaceVariant = OmniSurfaceHigh,
    onSurfaceVariant = Color(0xFFB4BAD4),
    outline = Color(0xFF3A4163),
    error = Color(0xFFFF6B7A),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F6BE0),
    onPrimary = Color.White,
    secondary = Color(0xFF6A4BE0),
    onSecondary = Color.White,
    tertiary = Color(0xFF14937A),
    background = Color(0xFFF6F7FC),
    onBackground = Color(0xFF12162A),
    surface = Color.White,
    onSurface = Color(0xFF12162A),
    surfaceVariant = Color(0xFFE9ECF8),
    onSurfaceVariant = Color(0xFF454B66),
    outline = Color(0xFFB9BED6),
    error = Color(0xFFB3261E),
)

private val OmniShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
)

/** The app's color scheme for dark or light mode (also used for the previews on the first-run screen). */
fun omniColorScheme(dark: Boolean): ColorScheme = if (dark) DarkColors else LightColors

/** themeMode: "system", "dark" or "light". */
@Composable
fun OmniToolsTheme(themeMode: String = "system", content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }

    // Make status/navigation bar icons readable on the chosen background, regardless of the phone's own setting.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
        }
    }

    MaterialTheme(
        colorScheme = omniColorScheme(dark),
        shapes = OmniShapes,
        content = content,
    )
}
