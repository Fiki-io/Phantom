package com.phantom.tube.core.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass styling modifier for buttons, cards, overlays, and controls.
 * Applies a frosted translucent fill, a refractive multi-stop border gradient,
 * and a subtle specular glass gloss sheen.
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(16.dp),
    borderWidth: Dp = 1.dp,
    tintColor: Color = Color(0xFF181924),
    glassAlpha: Float = 0.55f,
    accentGlow: Color? = null
): Modifier = this
    .clip(shape)
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                tintColor.copy(alpha = glassAlpha),
                tintColor.copy(alpha = (glassAlpha * 0.7f).coerceIn(0f, 1f))
            )
        ),
        shape = shape
    )
    .drawWithContent {
        drawContent()
        // Draw specular liquid glass sheen on top half
        val sheenBrush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.02f),
                Color.Transparent
            ),
            start = Offset(0f, 0f),
            end = Offset(size.width * 0.6f, size.height * 0.6f)
        )
        drawRect(
            brush = sheenBrush,
            size = Size(size.width, size.height * 0.5f)
        )
    }
    .border(
        width = borderWidth,
        brush = Brush.linearGradient(
            colors = if (accentGlow != null) {
                listOf(
                    Color.White.copy(alpha = 0.7f),
                    accentGlow.copy(alpha = 0.5f),
                    Color.White.copy(alpha = 0.15f),
                    accentGlow.copy(alpha = 0.2f)
                )
            } else {
                listOf(
                    Color.White.copy(alpha = 0.55f),
                    Color(0x4000CEC9), // Cyan refraction
                    Color(0x306C5CE7), // Violet refraction
                    Color.White.copy(alpha = 0.12f)
                )
            },
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        ),
        shape = shape
    )

/**
 * Liquid Glass Button variant with slightly higher contrast and gloss for tactile touch.
 */
fun Modifier.liquidGlassButton(
    shape: Shape = RoundedCornerShape(24.dp),
    isPressed: Boolean = false,
    accentColor: Color = NeonCyan
): Modifier = this.liquidGlass(
    shape = shape,
    borderWidth = 1.2.dp,
    tintColor = if (isPressed) Color(0xFF26283C) else Color(0xFF1B1C28),
    glassAlpha = if (isPressed) 0.85f else 0.65f,
    accentGlow = if (isPressed) accentColor else null
)
