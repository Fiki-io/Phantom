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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.phantom.tube.core.theme.YouTubeRed

/**
 * YouTube Red Scrubber:
 * - Unplayed: semi-transparent gray/white
 * - Buffered: semi-transparent white
 * - Played: YouTube Red track
 * - Thumb: YouTube Red circle at the end of progress (when showThumb = true)
 * - Idle Mode (showThumb = false): Persistent thin progress line without thumb dot
 */
@Composable
fun LiquidGlassScrubber(
    progress: Float,           // 0.0f to 1.0f
    bufferedFraction: Float,   // 0.0f to 1.0f
    modifier: Modifier = Modifier,
    showThumb: Boolean = true,
    onSeek: (Float) -> Unit = {}
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val activeFraction = if (isDragging) dragFraction else progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (showThumb) {
                    Modifier
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
                } else Modifier
            )
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val trackHeight = if (!showThumb) size.height else if (isDragging) 4.5.dp.toPx() else 3.dp.toPx()
            val thumbRadius = if (isDragging) 8.dp.toPx() else 5.5.dp.toPx()
            val yOffset = if (!showThumb) 0f else size.height - trackHeight - (thumbRadius / 2f)
            val corner = CornerRadius(trackHeight / 2, trackHeight / 2)

            // 1. Base Track (unplayed)
            drawRoundRect(
                color = Color.White.copy(alpha = 0.25f),
                topLeft = Offset(0f, yOffset),
                size = Size(size.width, trackHeight),
                cornerRadius = corner
            )

            // 2. Buffered Track
            val bufferWidth = (size.width * bufferedFraction.coerceIn(0f, 1f))
            if (bufferWidth > 0) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.45f),
                    topLeft = Offset(0f, yOffset),
                    size = Size(bufferWidth, trackHeight),
                    cornerRadius = corner
                )
            }

            // 3. Played Progress Track in YouTube Red
            val playedWidth = (size.width * activeFraction).coerceIn(0f, size.width)
            if (playedWidth > 0) {
                drawRoundRect(
                    color = YouTubeRed,
                    topLeft = Offset(0f, yOffset),
                    size = Size(playedWidth, trackHeight),
                    cornerRadius = corner
                )
            }

            // 4. YouTube Red Scrubber Thumb (Only shown in active controls mode)
            if (showThumb) {
                val thumbX = playedWidth.coerceIn(thumbRadius, size.width - thumbRadius)
                val thumbY = yOffset + trackHeight / 2f

                drawCircle(
                    color = YouTubeRed,
                    radius = thumbRadius,
                    center = Offset(thumbX, thumbY)
                )
            }
        }
    }
}
