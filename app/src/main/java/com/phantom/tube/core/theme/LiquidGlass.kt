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

private val SheenColors = listOf(
    Color.White.copy(alpha = 0.05f),
    Color.White.copy(alpha = 0.01f),
    Color.Transparent
)

private val DefaultBorderColors = listOf(
    Color.White.copy(alpha = 0.16f),
    Color.White.copy(alpha = 0.06f),
    Color.White.copy(alpha = 0.04f),
    Color.White.copy(alpha = 0.10f)
)

/**
 * Styling modifier for sleek dark surfaces, buttons, and sheets.
 * Provides a clean YouTube Dark fill with subtle top-to-bottom shading and crisp borders.
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(16.dp),
    borderWidth: Dp = 1.dp,
    tintColor: Color = Color(0xFF212121),
    glassAlpha: Float = 0.95f,
    accentGlow: Color? = null
): Modifier = this
    .clip(shape)
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                tintColor.copy(alpha = glassAlpha),
                tintColor.copy(alpha = (glassAlpha * 0.9f).coerceIn(0f, 1f))
            )
        ),
        shape = shape
    )
    .drawWithContent {
        drawContent()
        // Subtle sheen highlight on top half
        val sheenBrush = Brush.linearGradient(
            colors = SheenColors,
            start = Offset.Zero,
            end = Offset(size.width * 0.5f, size.height * 0.5f)
        )
        drawRect(
            brush = sheenBrush,
            size = Size(size.width, size.height * 0.4f)
        )
    }
    .border(
        width = borderWidth,
        brush = Brush.linearGradient(
            colors = if (accentGlow != null) {
                listOf(
                    accentGlow.copy(alpha = 0.6f),
                    accentGlow.copy(alpha = 0.2f),
                    Color.White.copy(alpha = 0.1f)
                )
            } else {
                DefaultBorderColors
            },
            start = Offset.Zero,
            end = Offset.Infinite
        ),
        shape = shape
    )

/**
 * Clean button modifier with responsive press feedback.
 */
fun Modifier.liquidGlassButton(
    shape: Shape = RoundedCornerShape(24.dp),
    isPressed: Boolean = false,
    accentColor: Color = YouTubeRed
): Modifier = this.liquidGlass(
    shape = shape,
    borderWidth = 1.dp,
    tintColor = if (isPressed) Color(0xFF333333) else Color(0xFF272727),
    glassAlpha = if (isPressed) 1f else 0.9f,
    accentGlow = if (isPressed) accentColor else null
)
