package app.tisimai.mektep.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = MektepGreen,
    onPrimary = Color.White,
    primaryContainer = BrandTint,
    onPrimaryContainer = MektepGreenDark,

    secondary = MektepOrange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE9C8),
    onSecondaryContainer = Color(0xFF5D3F00),

    tertiary = MektepBlue,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDEE3FB),
    onTertiaryContainer = Color(0xFF1A2270),

    error = MektepWrong,
    onError = Color.White,
    errorContainer = Color(0xFFFFD8E1),
    onErrorContainer = Color(0xFF5E102A),

    background = MektepBg,
    onBackground = MektepInk,
    surface = MektepCard,
    onSurface = MektepInk,
    surfaceVariant = MektepCardSoft,
    onSurfaceVariant = MektepInkSoft,

    surfaceContainerLowest = Color.White,
    surfaceContainerLow = MektepBg2,
    surfaceContainer = MektepCardSoft,
    surfaceContainerHigh = Color(0xFFEEF6F1),
    surfaceContainerHighest = Color(0xFFE9F3ED),

    outline = MektepInkMute,
    outlineVariant = Color(0xFFD5E5DD),
    scrim = Color.Black,
)

private val DarkColorScheme = darkColorScheme(
    primary = MektepGreenBright,
    onPrimary = Color(0xFF00382A),
    primaryContainer = MektepGreenDark,
    onPrimaryContainer = BrandTint,

    secondary = Color(0xFFF2C879),
    onSecondary = Color(0xFF3E2A00),
    secondaryContainer = Color(0xFF4A3410),
    onSecondaryContainer = Color(0xFFFFE9C8),

    tertiary = Color(0xFFAEB6FF),
    onTertiary = Color(0xFF1A2270),
    tertiaryContainer = Color(0xFF3A41A8),
    onTertiaryContainer = Color(0xFFDEE3FB),

    error = Color(0xFFFF8FAE),
    onError = Color(0xFF5E102A),
    errorContainer = Color(0xFF7A2342),
    onErrorContainer = Color(0xFFFFD8E1),

    background = MektepBgDark,
    onBackground = MektepInkDark,
    surface = MektepSurfaceDark,
    onSurface = MektepInkDark,
    surfaceVariant = MektepSurfaceVariantDark,
    onSurfaceVariant = MektepInkSoftDark,

    surfaceContainerLowest = Color(0xFF0B1310),
    surfaceContainerLow = Color(0xFF131E19),
    surfaceContainer = Color(0xFF17241F),
    surfaceContainerHigh = Color(0xFF1F2E28),
    surfaceContainerHighest = Color(0xFF293833),

    outline = Color(0xFF6E837B),
    outlineVariant = Color(0xFF33433C),
    scrim = Color.Black,
)

@Composable
fun MektepTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Brand identity is intentional — keep dynamic color opt-in only.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MektepTypography,
        shapes = MektepShapes,
        content = content,
    )
}
