package com.phantom.tube.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Animated gradient shimmer brush for authentic loading skeleton states.
 */
@Composable
fun rememberShimmerBrush(): Brush {
    val shimmerColors = listOf(
        Color(0xFF1E1E1E),
        Color(0xFF2C2C2C),
        Color(0xFF1E1E1E)
    )

    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translation"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(x = translateAnimation.value - 400f, y = translateAnimation.value - 400f),
        end = Offset(x = translateAnimation.value, y = translateAnimation.value)
    )
}

/**
 * Skeleton card matching [PhantomVideoCard] layout while fetching real YouTube InnerTube data.
 */
@Composable
fun VideoCardSkeleton(
    modifier: Modifier = Modifier,
    brush: Brush = rememberShimmerBrush()
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
    ) {
        // Thumbnail skeleton (16:9)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(brush)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Avatar & info row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Avatar circle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(brush)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Title line 1
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Title line 2
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Channel name & metadata line
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(11.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(brush)
                )
            }
        }
    }
}

/**
 * Vertical list of skeleton cards for feed loading states.
 */
@Composable
fun VideoFeedSkeleton(
    count: Int = 3,
    modifier: Modifier = Modifier
) {
    val brush = rememberShimmerBrush()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        repeat(count) {
            VideoCardSkeleton(brush = brush)
        }
    }
}
