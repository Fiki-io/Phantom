package com.phantom.tube.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Spesifikasi fisika pegas (Spring Physics) khas iOS untuk fluid micro-interactions.
 */
object IosSpringSpecs {
    // Pegas membal alami untuk tombol & kartu (membal lembut khas iOS)
    val Bouncy = spring<Float>(
        dampingRatio = 0.68f,
        stiffness = 380f
    )

    // Pegas halus untuk sliding tab, sheets, dan transisi layar
    val Gentle = spring<Float>(
        dampingRatio = 0.82f,
        stiffness = 320f
    )

    // Pegas cepat untuk pop / dismiss responsif
    val Snappy = spring<Float>(
        dampingRatio = 0.72f,
        stiffness = 550f
    )
}

/**
 * Modifier efek tekan membal (tactile spring bounce) khas iOS.
 * Saat disentuh: elemen mengecil lembut dengan redaman natural dan haptic tick.
 * Saat dilepas: elemen membal kembali ke ukuran semula secara fluida.
 */
fun Modifier.iosBounceClick(
    scaleDown: Float = 0.96f,
    alphaDown: Float = 0.92f,
    enableHaptic: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = IosSpringSpecs.Bouncy,
        label = "ios_bounce_scale"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isPressed) alphaDown else 1f,
        animationSpec = IosSpringSpecs.Gentle,
        label = "ios_bounce_alpha"
    )

    LaunchedEffect(isPressed) {
        if (isPressed && enableHaptic) {
            try {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } catch (_: Exception) {}
        }
    }

    this
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
            alpha = animatedAlpha
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

/**
 * Efek spring press untuk elemen yang sudah memiliki interactionSource sendiri.
 */
fun Modifier.iosPressEffect(
    interactionSource: MutableInteractionSource,
    scaleDown: Float = 0.96f,
    alphaDown: Float = 0.92f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = IosSpringSpecs.Bouncy,
        label = "ios_press_scale"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isPressed) alphaDown else 1f,
        animationSpec = IosSpringSpecs.Gentle,
        label = "ios_press_alpha"
    )

    this.graphicsLayer {
        scaleX = animatedScale
        scaleY = animatedScale
        alpha = animatedAlpha
    }
}
