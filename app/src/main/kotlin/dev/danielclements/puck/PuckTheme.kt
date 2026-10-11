package dev.danielclements.puck

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ---- Fallback schemes (API < 31): the charcoal/blue palette as full M3 schemes ----

val PuckFallbackDark = darkColorScheme(
    primary = Color(0xFFAAC7FF), onPrimary = Color(0xFF0A305F),
    primaryContainer = Color(0xFF284777), onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFFBEC6DC), onSecondary = Color(0xFF283141),
    secondaryContainer = Color(0xFF3A4356), onSecondaryContainer = Color(0xFFDAE2F9),
    tertiary = Color(0xFFDCBCE0), onTertiary = Color(0xFF3F2844),
    tertiaryContainer = Color(0xFF563E5C), onTertiaryContainer = Color(0xFFF9D8FD),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121316), onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF121316), onSurface = Color(0xFFE3E2E6),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099), outlineVariant = Color(0xFF44474F),
    surfaceContainerLowest = Color(0xFF0C0D10),
    surfaceContainerLow = Color(0xFF1A1B1F),
    surfaceContainer = Color(0xFF1F2025),
    surfaceContainerHigh = Color(0xFF2A2C31),
    surfaceContainerHighest = Color(0xFF34363B),
)

val PuckFallbackLight = lightColorScheme(
    primary = Color(0xFF3D5F90), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E3FF), onPrimaryContainer = Color(0xFF001B3D),
    secondary = Color(0xFF565F71), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDAE2F9), onSecondaryContainer = Color(0xFF131C2B),
    tertiary = Color(0xFF705575), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFAD8FD), onTertiaryContainer = Color(0xFF29132E),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF9F9FF), onBackground = Color(0xFF1A1B20),
    surface = Color(0xFFF9F9FF), onSurface = Color(0xFF1A1B20),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFF74777F), outlineVariant = Color(0xFFC4C6D0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEEEFF6),
    surfaceContainer = Color(0xFFE8E9F0),
    surfaceContainerHigh = Color(0xFFE4E5EC),
    surfaceContainerHighest = Color(0xFFE1E2EA),
)

// ---- Derived tones (DESIGN_SPEC.md §2) ----
//
// These don't map onto a single M3 ColorScheme role on their own — they're a
// fixed recipe over the scheme's surfaceContainer tiers, so a dynamic (Material
// You) scheme gets the same "disc system" depth language as the fallback one.

@Immutable
data class PuckExtendedColors(
    val tray: Color,
    val card: Color,
    val raised: Color,
    val well: Color,
    val edge: Color,
    val shade: Color,
)

fun ColorScheme.puckExtended(dark: Boolean) = PuckExtendedColors(
    tray = surfaceContainerLow,
    card = surfaceContainer,
    raised = if (dark) surfaceContainerHigh else surfaceContainerLowest,
    well = if (dark) surfaceContainerLowest else surfaceContainerHighest,
    edge = if (dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.95f),
    shade = if (dark) Color.Black.copy(alpha = 0.55f) else Color(0xFF1A2440).copy(alpha = 0.16f),
)

val LocalPuckColors = staticCompositionLocalOf { PuckFallbackDark.puckExtended(true) }

object Puck {
    val colors: PuckExtendedColors
        @Composable get() = LocalPuckColors.current
}

/**
 * Shared theme for every screen in the app, including the widget
 * configuration activity — not just MainActivity's.
 */
@Composable
fun PuckTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (dark) PuckFallbackDark else PuckFallbackLight
    }
    CompositionLocalProvider(LocalPuckColors provides colorScheme.puckExtended(dark)) {
        MaterialTheme(colorScheme = colorScheme, typography = PuckTypography, content = content)
    }
}
