package com.phantom.tube.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.phantom.tube.core.theme.NeonCyan
import com.phantom.tube.core.theme.NeonViolet

@Composable
fun LiquidGlassScrubber(
    progress: Float,           // 0.0f to 1.0f
    bufferedFraction: Float,   // 0.0f to 1.0f
    modifier: Modifier = Modifier,
    onSeek: (Float) -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val activeFraction = if (isDragging) dragFraction else progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek(fraction)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        isDragging = false
                        onSeek(dragFraction)
                    },
                    onDragCancel = {
                        isDragging = false
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val trackHeight = if (isDragging) 6.dp.toPx() else 4.dp.toPx()
            val yOffset = (size.height - trackHeight) / 2f
            val corner = CornerRadius(trackHeight / 2, trackHeight / 2)

            // 1. Base Glass Track (unplayed)
            drawRoundRect(
                color = Color.White.copy(alpha = 0.15f),
                topLeft = Offset(0f, yOffset),
                size = Size(size.width, trackHeight),
                cornerRadius = corner
            )

            // 2. Buffered Track
            val bufferWidth = (size.width * bufferedFraction.coerceIn(0f, 1f))
            if (bufferWidth > 0) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.35f),
                    topLeft = Offset(0f, yOffset),
                    size = Size(bufferWidth, trackHeight),
                    cornerRadius = corner
                )
            }

            // 3. Played Progress Track with Liquid Gradient
            val playedWidth = size.width * activeFraction
            if (playedWidth > 0) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(NeonViolet, NeonCyan)
                    ),
                    topLeft = Offset(0f, yOffset),
                    size = Size(playedWidth, trackHeight),
                    cornerRadius = corner
                )
            }

            // 4. Glow Scrubber Thumb
            val thumbRadius = if (isDragging) 9.dp.toPx() else 6.dp.toPx()
            val thumbX = playedWidth.coerceIn(thumbRadius, size.width - thumbRadius)
            val thumbY = size.height / 2f

            // Outer Glow
            drawCircle(
                color = NeonCyan.copy(alpha = 0.35f),
                radius = thumbRadius * 1.8f,
                center = Offset(thumbX, thumbY)
            )

            // Inner Glass Thumb
            drawCircle(
                color = Color.White,
                radius = thumbRadius,
                center = Offset(thumbX, thumbY)
            )
        }
    }
}
