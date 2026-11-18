package de.openhabvoice.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import de.openhabvoice.data.AppTheme

// openHAB-style orange as default accent color
private val Orange = Color(0xFFF97316)
private val OrangeContainer = Color(0xFFFFD9C2)
private val OnOrange = Color(0xFF3A1B00)

/**
 * Builds a dark scheme with the shared neutral surfaces and the given
 * accent (primary/secondary) colors.
 */
private fun darkAccentScheme(
    primary: Color,
    onPrimary: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color,
    secondary: Color,
    onSecondary: Color
) = darkColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    secondary = secondary,
    onSecondary = onSecondary,
    background = Color(0xFF101418),
    onBackground = Color(0xFFE6E2DC),
    surface = Color(0xFF181D22),
    onSurface = Color(0xFFE6E2DC),
    surfaceVariant = Color(0xFF232A31),
    onSurfaceVariant = Color(0xFFBFC4C9),
    error = Color(0xFFFFB4AB)
)

/**
 * Builds a light scheme with the shared neutral surfaces and the given
 * accent (primary/secondary) colors.
 */
private fun lightAccentScheme(
    primary: Color,
    onPrimary: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color,
    secondary: Color,
    onSecondary: Color
) = lightColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    secondary = secondary,
    onSecondary = onSecondary,
    background = Color(0xFFF8F9FB),
    onBackground = Color(0xFF1A1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFEDEFF3),
    onSurfaceVariant = Color(0xFF45484D),
    error = Color(0xFFBA1A1A)
)

// Hand-tuned accent palettes per AppTheme (dark and light)
private val DarkSchemes = mapOf(
    AppTheme.OPENHAB to darkAccentScheme(
        primary = Orange,
        onPrimary = OnOrange,
        primaryContainer = Color(0xFF5A2A00),
        onPrimaryContainer = OrangeContainer,
        secondary = Color(0xFF8BD2FF),
        onSecondary = Color(0xFF00293F)
    ),
    AppTheme.BLUE to darkAccentScheme(
        primary = Color(0xFF9ECAFF),
        onPrimary = Color(0xFF003258),
        primaryContainer = Color(0xFF00497D),
        onPrimaryContainer = Color(0xFFD1E4FF),
        secondary = Color(0xFFBBC7DB),
        onSecondary = Color(0xFF253140)
    ),
    AppTheme.GREEN to darkAccentScheme(
        primary = Color(0xFF9CD67D),
        onPrimary = Color(0xFF0E3900),
        primaryContainer = Color(0xFF24500B),
        onPrimaryContainer = Color(0xFFB7F397),
        secondary = Color(0xFFBBC9B4),
        onSecondary = Color(0xFF253424)
    ),
    AppTheme.VIOLET to darkAccentScheme(
        primary = Color(0xFFD0BCFF),
        onPrimary = Color(0xFF381E72),
        primaryContainer = Color(0xFF4F378B),
        onPrimaryContainer = Color(0xFFEADDFF),
        secondary = Color(0xFFCCC2DC),
        onSecondary = Color(0xFF332D41)
    )
)

private val LightSchemes = mapOf(
    AppTheme.OPENHAB to lightAccentScheme(
        primary = Color(0xFFC65A00),
        onPrimary = Color.White,
        primaryContainer = OrangeContainer,
        onPrimaryContainer = Color(0xFF3A1B00),
        secondary = Color(0xFF2E6A8E),
        onSecondary = Color.White
    ),
    AppTheme.BLUE to lightAccentScheme(
        primary = Color(0xFF0061A4),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD1E4FF),
        onPrimaryContainer = Color(0xFF001D36),
        secondary = Color(0xFF535F70),
        onSecondary = Color.White
    ),
    AppTheme.GREEN to lightAccentScheme(
        primary = Color(0xFF386A20),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFB7F397),
        onPrimaryContainer = Color(0xFF072100),
        secondary = Color(0xFF52634F),
        onSecondary = Color.White
    ),
    AppTheme.VIOLET to lightAccentScheme(
        primary = Color(0xFF6750A4),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFEADDFF),
        onPrimaryContainer = Color(0xFF21005D),
        secondary = Color(0xFF625B71),
        onSecondary = Color.White
    )
)

@Composable
fun OpenHabVoiceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: AppTheme = AppTheme.OPENHAB,
    content: @Composable () -> Unit
) {
    val colorScheme = when (accent) {
        AppTheme.DYNAMIC ->
            // Material You is only available from Android 12 (S); on older
            // systems (e.g. FireOS) fall back to the openHAB scheme.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else if (darkTheme) {
                DarkSchemes.getValue(AppTheme.OPENHAB)
            } else {
                LightSchemes.getValue(AppTheme.OPENHAB)
            }
        else ->
            if (darkTheme) DarkSchemes.getValue(accent) else LightSchemes.getValue(accent)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
