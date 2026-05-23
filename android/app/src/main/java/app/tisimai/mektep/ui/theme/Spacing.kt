package app.tisimai.mektep.ui.theme

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** 4dp-based spacing scale used across the redesign. */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 28.dp
    val xxxl = 36.dp

    /** Standard screen edge padding. */
    val screen = 20.dp
    /** Default inner padding for cards. */
    val card = 20.dp
}

/** Soft elevation tokens (the web uses very gentle shadows). */
object MektepElevation {
    val card = 2.dp
    val raised = 6.dp
    val modal = 12.dp
}

/** Brand and surface gradients ported from the web's radial/linear backgrounds. */
object MektepGradients {

    /** Hero "continue lesson" gradient: brand green with a warm corner glow. */
    val brandHero: Brush
        get() = Brush.linearGradient(
            colors = listOf(MektepGreen, MektepGreenDark),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
        )

    /** Brand circle/badge fill. */
    val brandMark: Brush
        get() = Brush.linearGradient(listOf(MektepGreenLight, MektepGreenDark))

    /** Subtle full-screen background wash; pass the theme background as [base]. */
    @Composable
    @ReadOnlyComposable
    fun screenWash(base: Color): Brush = Brush.verticalGradient(
        colors = listOf(base, base),
    )
}

/** Convenience modifier applying the screen background color. */
fun Modifier.screenBackground(color: Color): Modifier = this.background(color)
