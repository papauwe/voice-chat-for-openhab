package de.openhabvoice.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// openHAB-style orange as accent color
private val Orange = Color(0xFFF97316)
private val OrangeContainer = Color(0xFFFFD9C2)
private val OnOrange = Color(0xFF3A1B00)

private val DarkScheme = darkColorScheme(
    primary = Orange,
    onPrimary = OnOrange,
    primaryContainer = Color(0xFF5A2A00),
    onPrimaryContainer = OrangeContainer,
    secondary = Color(0xFF8BD2FF),
    onSecondary = Color(0xFF00293F),
    background = Color(0xFF101418),
    onBackground = Color(0xFFE6E2DC),
    surface = Color(0xFF181D22),
    onSurface = Color(0xFFE6E2DC),
    surfaceVariant = Color(0xFF232A31),
    onSurfaceVariant = Color(0xFFBFC4C9),
    error = Color(0xFFFFB4AB)
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFFC65A00),
    onPrimary = Color.White,
    primaryContainer = OrangeContainer,
    onPrimaryContainer = Color(0xFF3A1B00),
    secondary = Color(0xFF2E6A8E),
    onSecondary = Color.White,
    background = Color(0xFFF8F9FB),
    onBackground = Color(0xFF1A1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFEDEFF3),
    onSurfaceVariant = Color(0xFF45484D),
    error = Color(0xFFBA1A1A)
)

@Composable
fun OpenHabVoiceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        content = content
    )
}
