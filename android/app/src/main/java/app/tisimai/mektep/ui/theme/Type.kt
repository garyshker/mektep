package app.tisimai.mektep.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import app.tisimai.mektep.R

/**
 * Nunito, pulled at runtime via the Google Fonts downloadable-font provider
 * (Play Services). The certificate array lives in res/values/font_certs.xml.
 */
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val nunito = GoogleFont("Nunito")

val NunitoFamily = FontFamily(
    Font(googleFont = nunito, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = nunito, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = nunito, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = nunito, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = nunito, fontProvider = provider, weight = FontWeight.ExtraBold),
    Font(googleFont = nunito, fontProvider = provider, weight = FontWeight.Black),
)

/**
 * Type scale mirroring the web app: tight, heavy display/headlines (800–900),
 * comfortable body (400–500), and bold uppercase-friendly labels (800).
 */
val MektepTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = NunitoFamily, fontWeight = FontWeight.Black,
        fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-0.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = NunitoFamily, fontWeight = FontWeight.Black,
        fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.25).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = NunitoFamily, fontWeight = FontWeight.Black,
        fontSize = 28.sp, lineHeight = 34.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = NunitoFamily, fontWeight = FontWeight.Black,
        fontSize = 26.sp, lineHeight = 32.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = NunitoFamily, fontWeight = FontWeight.ExtraBold,
        fontSize = 23.sp, lineHeight = 28.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = NunitoFamily, fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp, lineHeight = 26.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = NunitoFamily, fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp, lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = NunitoFamily, fontWeight = FontWeight.ExtraBold,
        fontSize = 17.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = NunitoFamily, fontWeight = FontWeight.Bold,
        fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = NunitoFamily, fontWeight = FontWeight.Medium,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = NunitoFamily, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.15.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = NunitoFamily, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.2.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = NunitoFamily, fontWeight = FontWeight.ExtraBold,
        fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.3.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = NunitoFamily, fontWeight = FontWeight.ExtraBold,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = NunitoFamily, fontWeight = FontWeight.ExtraBold,
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.8.sp,
    ),
)
