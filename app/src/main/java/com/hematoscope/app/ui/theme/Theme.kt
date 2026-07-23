package com.hematoscope.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Brand palette: deep haematology crimson with a violet nuclear accent.
private val Crimson = Color(0xFFB4123A)
private val CrimsonDeep = Color(0xFF7A0C26)
private val NucleusViolet = Color(0xFF6B1E86)
private val CytoPink = Color(0xFFF4C6D2)

private val LightColors = lightColorScheme(
    primary = Crimson,
    onPrimary = Color.White,
    primaryContainer = CytoPink,
    onPrimaryContainer = CrimsonDeep,
    secondary = NucleusViolet,
    onSecondary = Color.White,
    tertiary = Color(0xFF4CAF7D),
    background = Color(0xFFFFF8F8),
    surface = Color(0xFFFFFBFB),
    surfaceVariant = Color(0xFFF3E1E5),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB2BF),
    onPrimary = Color(0xFF5F1122),
    primaryContainer = Color(0xFF8A1130),
    onPrimaryContainer = CytoPink,
    secondary = Color(0xFFE0B6F0),
    onSecondary = Color(0xFF3B1050),
    tertiary = Color(0xFF9FD8B8),
    background = Color(0xFF1A1113),
    surface = Color(0xFF1A1113),
    surfaceVariant = Color(0xFF52434A),
    error = Color(0xFFFFB4AB)
)

private val AppTypography = Typography()

@Composable
fun HematoScopeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
}
