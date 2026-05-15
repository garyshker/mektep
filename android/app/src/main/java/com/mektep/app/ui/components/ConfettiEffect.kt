package com.mektep.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.sin
import kotlin.random.Random

data class ConfettiParticle(
    val x: Float,
    val speed: Float,
    val delay: Float,
    val rotation: Float,
    val color: Color,
    val size: Float
)

@Composable
fun ConfettiEffect(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isActive) return

    val colors = listOf(
        Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFF9800),
        Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFFFFEB3B)
    )

    val particles = remember {
        (0 until 60).map {
            ConfettiParticle(
                x = Random.nextFloat(),
                speed = 0.3f + Random.nextFloat() * 0.7f,
                delay = Random.nextFloat() * 0.6f,
                rotation = Random.nextFloat() * 360f,
                color = colors.random(),
                size = 6f + Random.nextFloat() * 10f
            )
        }
    }

    val progress by rememberInfiniteTransition(label = "confetti").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "confettiProgress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            val adjustedProgress = ((progress - p.delay + 1f) % 1f)
            val y = adjustedProgress * size.height * 1.2f
            val x = p.x * size.width + sin(adjustedProgress * 6f) * 30f
            val alpha = (1f - adjustedProgress).coerceIn(0f, 1f)

            rotate(p.rotation + adjustedProgress * 720f, pivot = Offset(x, y)) {
                drawRect(
                    color = p.color.copy(alpha = alpha),
                    topLeft = Offset(x - p.size / 2, y - p.size / 2),
                    size = Size(p.size, p.size * 0.6f)
                )
            }
        }
    }
}
