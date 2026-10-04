package com.phantom.tube.core.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val DefaultBorderColor = Color.White.copy(alpha = 0.12f)

fun Modifier.phantomSurface(
    shape: Shape = RoundedCornerShape(12.dp),
    borderWidth: Dp = 1.dp,
    tintColor: Color = YouTubeSurface,
    surfaceAlpha: Float = 0.95f,
    accentGlow: Color? = null
): Modifier = this
    .clip(shape)
    .background(
        color = tintColor.copy(alpha = surfaceAlpha),
        shape = shape
    )
    .border(
        width = borderWidth,
        color = accentGlow?.copy(alpha = 0.45f) ?: DefaultBorderColor,
        shape = shape
    )

/**
 * Modifier tombol dengan feedback saat ditekan.
 */
fun Modifier.phantomButtonSurface(
    shape: Shape = RoundedCornerShape(24.dp),
    isPressed: Boolean = false,
    accentColor: Color = YouTubeRed
): Modifier = this.phantomSurface(
    shape = shape,
    borderWidth = 1.dp,
    tintColor = if (isPressed) Color(0xFF333333) else Color(0xFF272727),
    surfaceAlpha = if (isPressed) 1f else 0.9f,
    accentGlow = if (isPressed) accentColor else null
)
