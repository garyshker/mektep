package app.tisimai.mektep.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Mektep color tokens, ported from the web app's design language.
 *
 * The brand constants ([MektepGreen], [MektepRed], …) keep their original names so
 * existing screens keep compiling; their values are aligned to the web palette.
 * New screens should prefer the semantic Material color scheme (MaterialTheme.colorScheme)
 * and the [SubjectPalette] tokens below.
 */

// ---------------------------------------------------------------------------
// Brand (web: --brand / --brand-deep / --brand-tint)
// ---------------------------------------------------------------------------
val MektepGreen = Color(0xFF0E8C6B)       // --brand
val MektepGreenLight = Color(0xFF18B187)  // brighter green used in gradients
val MektepGreenDark = Color(0xFF0A6E54)   // --brand-deep
val BrandTint = Color(0xFFC6EAD8)         // --brand-tint (light pill / container)

// Accents kept for backward-compat with existing screens
val MektepOrange = Color(0xFFE88912)      // warm accent (web math icon)
val MektepRed = Color(0xFFE14B73)         // --wrong / error (web magenta-pink)
val MektepBlue = Color(0xFF5764D8)        // info / english indigo
val MektepPurple = Color(0xFF7C3AED)      // logic purple

// Semantic
val MektepSuccess = Color(0xFF0E8C6B)
val MektepWrong = Color(0xFFE14B73)
val MektepStar = Color(0xFFE8B43A)        // golden star / warning

// ---------------------------------------------------------------------------
// Neutrals — light (web "ink" + backgrounds)
// ---------------------------------------------------------------------------
val MektepBg = Color(0xFFE6F3EC)          // --bg (soft mint)
val MektepBg2 = Color(0xFFF2FAF5)         // --bg-2
val MektepCard = Color(0xFFFFFFFF)        // --card
val MektepCardSoft = Color(0xFFF5FBF7)    // --card-soft
val MektepInk = Color(0xFF0F2A22)         // --ink (primary text)
val MektepInkSoft = Color(0xFF4A6A60)     // --ink-soft (secondary text)
val MektepInkMute = Color(0xFF8AA39A)     // --ink-mute (tertiary text)
val MektepLine = Color(0x140F2A22)        // --line: rgba(15,42,34,.08)

// ---------------------------------------------------------------------------
// Neutrals — dark (teal-tinted, derived to keep the brand mood at night)
// ---------------------------------------------------------------------------
val MektepBgDark = Color(0xFF0E1714)
val MektepSurfaceDark = Color(0xFF14201B)
val MektepSurfaceVariantDark = Color(0xFF1C2B25)
val MektepInkDark = Color(0xFFE2EDE8)
val MektepInkSoftDark = Color(0xFFA6BDB4)
val MektepLineDark = Color(0x1FFFFFFF)
val MektepGreenBright = Color(0xFF34C79E) // primary on dark surfaces

// ---------------------------------------------------------------------------
// Subject colors (web --<subject>-bg / --<subject>-ic pairs)
// Names kept for backward-compat; see [SubjectPalette] for the bg/ic pairs.
// ---------------------------------------------------------------------------
val MathColor = Color(0xFFE88912)
val KazakhColor = Color(0xFFE14B73)
val EnglishColor = Color(0xFF5764D8)
val WorldColor = Color(0xFF2D9D5C)
val LogicColor = Color(0xFF7C3AED)
val MusicColor = Color(0xFFDB2777)

/** A pastel background + saturated icon color pair for a subject tile. */
data class SubjectColors(val container: Color, val content: Color)

/** Maps a subject id to its [SubjectColors]; falls back to the brand pair. */
object SubjectPalette {
    val Math = SubjectColors(Color(0xFFFFE9C8), MathColor)
    val Kazakh = SubjectColors(Color(0xFFFFD8E1), KazakhColor)
    val World = SubjectColors(Color(0xFFCDEBD3), WorldColor)
    val English = SubjectColors(Color(0xFFDEE3FB), EnglishColor)
    val Logic = SubjectColors(Color(0xFFEDE9FB), LogicColor)
    val Music = SubjectColors(Color(0xFFFCE7F3), MusicColor)
    private val brand = SubjectColors(BrandTint, MektepGreen)

    fun forId(id: String?): SubjectColors = when (id?.lowercase()) {
        "math", "mathematics", "matematika" -> Math
        "kazakh", "kaz", "qazaq" -> Kazakh
        "world", "world_studies", "dunietanu" -> World
        "english", "eng" -> English
        "logic", "logika" -> Logic
        "music", "muzyka" -> Music
        else -> brand
    }
}
