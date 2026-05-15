package app.tisimai.mektep.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// Mektep brand colors
val MektepGreen = Color(0xFF0E8C6B)
val MektepGreenLight = Color(0xFF4CAF8B)
val MektepGreenDark = Color(0xFF06644C)
val MektepOrange = Color(0xFFFF9800)
val MektepRed = Color(0xFFE53935)
val MektepBlue = Color(0xFF2196F3)
val MektepPurple = Color(0xFF9C27B0)

// Subject colors
val MathColor = Color(0xFF4CAF50)
val KazakhColor = Color(0xFF2196F3)
val EnglishColor = Color(0xFFFF9800)
val WorldColor = Color(0xFF9C27B0)

private val LightColorScheme = lightColorScheme(
    primary = MektepGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8F0D8),
    onPrimaryContainer = MektepGreenDark,
    secondary = MektepOrange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE0B2),
    onSecondaryContainer = Color(0xFF5D3F00),
    tertiary = MektepBlue,
    error = MektepRed,
    background = Color(0xFFF8FAF8),
    onBackground = Color(0xFF1A1C1A),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1A),
    surfaceVariant = Color(0xFFF0F4F0),
)

private val DarkColorScheme = darkColorScheme(
    primary = MektepGreenLight,
    onPrimary = Color(0xFF003822),
    primaryContainer = MektepGreenDark,
    onPrimaryContainer = Color(0xFFB8F0D8),
    secondary = Color(0xFFFFCC80),
    onSecondary = Color(0xFF3E2700),
    tertiary = Color(0xFF90CAF9),
    error = Color(0xFFEF9A9A),
    background = Color(0xFF1A1C1A),
    onBackground = Color(0xFFE2E3DE),
    surface = Color(0xFF1A1C1A),
    onSurface = Color(0xFFE2E3DE),
)

@Composable
fun MektepTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
