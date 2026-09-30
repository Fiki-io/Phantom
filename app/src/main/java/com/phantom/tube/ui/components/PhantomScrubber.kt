package com.phantom.tube.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
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
 * Scrubber timeline pemutar video:
 * - Unplayed: putih semi-transparan
 * - Buffered: putih sedikit lebih tebal
 * - Played: aksen YouTube Red
 * - Thumb: titik merah saat controls aktif
 */
@Composable
fun PhantomScrubber(
    progress: Float,           // 0.0f to 1.0f
    bufferedFraction: Float,   // 0.0f to 1.0f
    modifier: Modifier = Modifier,
    showThumb: Boolean = true,
    onSeek: (Float) -> Unit = {},
    onScrubbing: (isScrubbing: Boolean, fraction: Float, touchX: Float) -> Unit = { _, _, _ -> }
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var dragTouchX by remember { mutableFloatStateOf(0f) }

    val activeFraction = if (isDragging) dragFraction else progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (showThumb) {
                    Modifier
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                if (size.width > 0) {
                                    val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    onSeek(fraction)
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    if (size.width > 0) {
                                        isDragging = true
                                        dragFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                        dragTouchX = offset.x
                                        onScrubbing(true, dragFraction, offset.x)
                                    }
                                },
                                onDrag = { change, _ ->
                                    if (size.width > 0) {
                                        change.consume()
                                        dragFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                        dragTouchX = change.position.x
                                        onScrubbing(true, dragFraction, change.position.x)
                                    }
                                },
                                onDragEnd = {
                                    isDragging = false
                                    onScrubbing(false, dragFraction, dragTouchX)
                                    onSeek(dragFraction)
                                },
                                onDragCancel = {
                                    isDragging = false
                                    onScrubbing(false, dragFraction, dragTouchX)
                                }
                            )
                        }
                } else Modifier
            )
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas

            val trackHeight = (if (!showThumb) size.height else if (isDragging) 4.5.dp.toPx() else 3.dp.toPx()).coerceAtMost(size.height)
            val thumbRadius = if (isDragging) 8.dp.toPx() else 5.5.dp.toPx()
            val yOffset = if (!showThumb) 0f else (size.height - trackHeight - (thumbRadius / 2f)).coerceAtLeast(0f)
            val corner = CornerRadius(trackHeight / 2, trackHeight / 2)

            // 1. Base Track (unplayed)
            drawRoundRect(
                color = Color.White.copy(alpha = 0.25f),
                topLeft = Offset(0f, yOffset),
                size = Size(size.width, trackHeight),
                cornerRadius = corner
            )

            // 2. Buffered Track
            val bufferWidth = (size.width * bufferedFraction.coerceIn(0f, 1f)).coerceIn(0f, size.width)
            if (bufferWidth > 0f) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.45f),
                    topLeft = Offset(0f, yOffset),
                    size = Size(bufferWidth, trackHeight),
                    cornerRadius = corner
                )
            }

            // 3. Played Progress Track
            val playedWidth = (size.width * activeFraction).coerceIn(0f, size.width)
            if (playedWidth > 0f) {
                drawRoundRect(
                    color = YouTubeRed,
                    topLeft = Offset(0f, yOffset),
                    size = Size(playedWidth, trackHeight),
                    cornerRadius = corner
                )
            }

            // 4. Scrubber Thumb
            if (showThumb) {
                val minX = thumbRadius
                val maxX = (size.width - thumbRadius).coerceAtLeast(minX)
                val thumbX = if (maxX >= minX) playedWidth.coerceIn(minX, maxX) else size.width / 2f
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
