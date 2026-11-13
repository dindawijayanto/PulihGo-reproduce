package com.pulihgo.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Shared application palette. Patient Home, account screens, onboarding and
// feature handoffs must all read the same darkTheme state from MainActivity.
private val PulihTeal = Color(0xFF0D7C7C)
private val LightPalette = lightColorScheme(
    primary = PulihTeal,
    onPrimary = Color.White,
    secondary = Color(0xFF159F73),
    onSecondary = Color.White,
    background = Color(0xFFF9F9F7),
    onBackground = Color(0xFF1A1E21),
    surface = Color.White,
    onSurface = Color(0xFF1A1E21),
    surfaceVariant = Color(0xFFEAF8F7),
    onSurfaceVariant = Color(0xFF64717A),
    outline = Color(0xFFE4EAEB)
)
private val DarkPalette = darkColorScheme(
    primary = PulihTeal,
    onPrimary = Color.White,
    secondary = Color(0xFF159F73),
    onSecondary = Color.White,
    background = Color(0xFF111717),
    onBackground = Color(0xFFF2F7F6),
    surface = Color(0xFF202727),
    onSurface = Color(0xFFF2F7F6),
    surfaceVariant = Color(0xFF1B3C39),
    onSurfaceVariant = Color(0xFFB9C6C6),
    outline = Color(0xFF3A4949)
)

@Composable
fun PulihGoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkPalette
        else -> LightPalette
    }
    MaterialTheme(colorScheme = colors, typography = Typography) {
        // Preserve the shared Plus Jakarta Sans family for Text without a style.
        ProvideTextStyle(value = Typography.bodyLarge) { content() }
    }
}
