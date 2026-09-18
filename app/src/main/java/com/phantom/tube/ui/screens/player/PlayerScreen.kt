package com.phantom.tube.ui.screens.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phantom.tube.core.theme.NeonAmber
import com.phantom.tube.core.theme.NeonCyan
import com.phantom.tube.core.theme.NeonPink
import com.phantom.tube.core.theme.NeonViolet
import com.phantom.tube.core.theme.ObsidianDark
import com.phantom.tube.core.theme.TextMuted
import com.phantom.tube.core.theme.TextPrimary
import com.phantom.tube.core.theme.TextSecondary
import com.phantom.tube.core.theme.liquidGlass
import com.phantom.tube.data.model.NextQueue
import com.phantom.tube.data.model.SponsorSegment
import com.phantom.tube.data.model.VideoItem
import com.phantom.tube.data.repository.PhantomRepository
import com.phantom.tube.player.PhantomGhostSurface
import com.phantom.tube.player.PhantomPlayerBridge
import com.phantom.tube.player.PhantomPlayerController
import com.phantom.tube.player.PlayerState
import com.phantom.tube.ui.components.LiquidGlassIconButton
import com.phantom.tube.ui.components.LiquidGlassScrubber
import com.phantom.tube.ui.components.LiquidGlassVideoCard
import com.phantom.tube.ui.components.SponsorSkipPill
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    video: VideoItem,
    repository: PhantomRepository,
    onBackClick: () -> Unit,
    onPlayNextVideo: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var playerState by remember { mutableStateOf(PlayerState(videoId = video.id)) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }

    // SponsorBlock state
    var sponsorSegments by remember { mutableStateOf<List<SponsorSegment>>(emptyList()) }
    var showSponsorPill by remember { mutableStateOf(false) }
    var lastSkippedSeconds by remember { mutableIntStateOf(0) }
    var lastSkippedFromSec by remember { mutableFloatStateOf(0f) }

    // Up Next Queue
    var nextQueue by remember { mutableStateOf<NextQueue?>(null) }
    var isLoadingQueue by remember { mutableStateOf(true) }

    // Favorites
    val isFavorite by repository.isFavorite(video.id).collectAsState(initial = false)

    val controller = remember { PhantomPlayerController(context) }

    val bridge = remember {
        PhantomPlayerBridge(
            onReadyCallback = {
                controller.markBridgeReady()
                scope.launch {
                    val lastPos = repository.getLastPosition(video.id)
                    controller.loadVideo(video.id, (lastPos / 1000f))
                }
            },
            onStateChangeCallback = { state ->
                // 1 = PLAYING, 2 = PAUSED, 3 = BUFFERING, 0 = ENDED
                when (state) {
                    1 -> playerState = playerState.copy(isPlaying = true, isBuffering = false, isEnded = false)
                    2 -> playerState = playerState.copy(isPlaying = false, isBuffering = false)
                    3 -> playerState = playerState.copy(isBuffering = true)
                    0 -> {
                        playerState = playerState.copy(isPlaying = false, isEnded = true)
                        // Autoplay next video from queue
                        nextQueue?.upNext?.firstOrNull()?.let { nextVid ->
                            onPlayNextVideo(nextVid)
                        }
                    }
                }
            },
            onTimeUpdateCallback = { current, duration, buffered ->
                playerState = playerState.copy(
                    currentTimeSec = current,
                    durationSec = duration,
                    bufferedFraction = buffered
                )

                // Record watch history
                if (current > 2f) {
                    scope.launch {
                        repository.recordWatch(
                            video = video,
                            positionMs = (current * 1000).toLong(),
                            durationMs = (duration * 1000).toLong()
                        )
                    }
                }

                // Check SponsorBlock segments
                sponsorSegments.forEach { seg ->
                    if (current >= seg.startSecond && current < (seg.startSecond + 1.5f)) {
                        lastSkippedFromSec = current
                        lastSkippedSeconds = (seg.endSecond - seg.startSecond).toInt()
                        controller.seekTo(seg.endSecond)
                        showSponsorPill = true
                        scope.launch {
                            delay(4000)
                            showSponsorPill = false
                        }
                    }
                }
            },
            onErrorCallback = { errorCode ->
                playerState = playerState.copy(isBuffering = false, errorCode = errorCode)
            }
        )
    }

    // Keep screen on during playback
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (isFullscreen) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            controller.release()
        }
    }

    // Auto-hide controls timer
    LaunchedEffect(isControlsVisible, playerState.isPlaying) {
        if (isControlsVisible && playerState.isPlaying) {
            delay(4000)
            isControlsVisible = false
        }
    }

    // Load SponsorBlock segments & Watch Next Queue
    LaunchedEffect(video.id) {
        scope.launch {
            sponsorSegments = repository.getSponsorSegments(video.id)
        }
        scope.launch {
            isLoadingQueue = true
            nextQueue = repository.getWatchNext(video.id)
            isLoadingQueue = false
        }
    }

    BackHandler {
        if (isFullscreen) {
            val activity = context as? Activity
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            isFullscreen = false
        } else {
            onBackClick()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDark)
    ) {
        // VIDEO PLAYER SECTION (16:9 or Match Parent in Fullscreen)
        Box(
            modifier = if (isFullscreen) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .aspectRatio(16f / 9f)
            }
                .background(Color.Black)
        ) {
            // Layer 0: The Ghost WebView (pure video rendering)
            PhantomGhostSurface(
                videoId = video.id,
                modifier = Modifier.matchParentSize(),
                controller = controller,
                bridge = bridge
            )

            // Layer 1: Transparent Gesture Touch Handler
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                isControlsVisible = !isControlsVisible
                            },
                            onDoubleTap = { offset ->
                                if (offset.x < size.width / 2) {
                                    // Seek back 10s
                                    val newTime = (playerState.currentTimeSec - 10f).coerceAtLeast(0f)
                                    controller.seekTo(newTime)
                                } else {
                                    // Seek forward 10s
                                    val newTime = (playerState.currentTimeSec + 10f).coerceAtMost(playerState.durationSec)
                                    controller.seekTo(newTime)
                                }
                            }
                        )
                    }
            )

            // Layer 2: Buffering & Error Indicator
            if (playerState.isBuffering) {
                CircularProgressIndicator(
                    color = NeonCyan,
                    strokeWidth = 3.dp,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                )
            } else if (playerState.errorCode != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .liquidGlass(
                            shape = RoundedCornerShape(16.dp),
                            borderWidth = 1.dp,
                            tintColor = Color(0xFF261010),
                            glassAlpha = 0.85f
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Video tidak dapat diputar (Error ${playerState.errorCode})",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Layer 3: Floating Sponsor Skip Pill
            SponsorSkipPill(
                visible = showSponsorPill,
                skippedSeconds = lastSkippedSeconds,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                onUndo = {
                    controller.seekTo(lastSkippedFromSec)
                    showSponsorPill = false
                }
            )

            // Layer 4: 100% Native Liquid Glass Controls Overlay
            androidx.compose.animation.AnimatedVisibility(
                visible = isControlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.matchParentSize()
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.65f),
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                        .padding(12.dp)
                ) {
                    // Top Bar Controls: Back & Speed
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiquidGlassIconButton(
                            icon = Icons.Default.ArrowBack,
                            contentDescription = "Kembali",
                            size = 38.dp,
                            iconSize = 20.dp,
                            onClick = {
                                if (isFullscreen) {
                                    val activity = context as? Activity
                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                    isFullscreen = false
                                } else {
                                    onBackClick()
                                }
                            }
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Playback Speed Button (cycles 1.0x -> 1.25x -> 1.5x -> 2.0x -> 0.75x)
                            Box(
                                modifier = Modifier
                                    .liquidGlass(
                                        shape = RoundedCornerShape(14.dp),
                                        borderWidth = 0.8.dp,
                                        glassAlpha = 0.6f
                                    )
                                    .clickable {
                                        val nextSpeed = when (playerState.playbackSpeed) {
                                            1.0f -> 1.25f
                                            1.25f -> 1.5f
                                            1.5f -> 2.0f
                                            2.0f -> 0.75f
                                            else -> 1.0f
                                        }
                                        playerState = playerState.copy(playbackSpeed = nextSpeed)
                                        controller.setPlaybackRate(nextSpeed)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${playerState.playbackSpeed}x",
                                    color = NeonCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Center Row: Replay10, Play/Pause, Forward10
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiquidGlassIconButton(
                            icon = Icons.Default.Replay10,
                            contentDescription = "Mundur 10 Detik",
                            size = 46.dp,
                            iconSize = 24.dp,
                            onClick = {
                                val newTime = (playerState.currentTimeSec - 10f).coerceAtLeast(0f)
                                controller.seekTo(newTime)
                            }
                        )

                        LiquidGlassIconButton(
                            icon = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                            size = 64.dp,
                            iconSize = 36.dp,
                            accentGlow = NeonCyan,
                            onClick = {
                                if (playerState.isPlaying) {
                                    controller.pause()
                                } else {
                                    controller.play()
                                }
                            }
                        )

                        LiquidGlassIconButton(
                            icon = Icons.Default.Forward10,
                            contentDescription = "Maju 10 Detik",
                            size = 46.dp,
                            iconSize = 24.dp,
                            onClick = {
                                val newTime = (playerState.currentTimeSec + 10f).coerceAtMost(playerState.durationSec)
                                controller.seekTo(newTime)
                            }
                        )
                    }

                    // Bottom Row: Scrubber, Timestamps, Fullscreen
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    ) {
                        LiquidGlassScrubber(
                            progress = playerState.progressFraction,
                            bufferedFraction = playerState.bufferedFraction,
                            onSeek = { fraction ->
                                val targetSec = fraction * playerState.durationSec
                                controller.seekTo(targetSec)
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${playerState.formattedCurrentTime} / ${playerState.formattedDuration}",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LiquidGlassIconButton(
                                    icon = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    size = 36.dp,
                                    iconSize = 20.dp,
                                    onClick = {
                                        val activity = context as? Activity
                                        if (isFullscreen) {
                                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                            isFullscreen = false
                                        } else {
                                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                            isFullscreen = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // BELOW PLAYER CONTENT (Only shown in portrait mode)
        if (!isFullscreen) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Video Details Header
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = video.title,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = video.channelTitle,
                                    color = NeonCyan,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (video.viewCountText.isNotBlank()) {
                                    Text(
                                        text = video.viewCountText,
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Favorite Action Button
                            LiquidGlassIconButton(
                                icon = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Simpan",
                                size = 42.dp,
                                iconSize = 22.dp,
                                tint = if (isFavorite) NeonPink else TextPrimary,
                                onClick = {
                                    scope.launch {
                                        repository.toggleFavorite(video, isFavorite)
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Up Next Header
                        Text(
                            text = "Video Berikutnya",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Up Next Queue Items
                if (isLoadingQueue) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = NeonCyan,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                } else {
                    nextQueue?.upNext?.let { queueList ->
                        items(queueList, key = { it.id }) { item ->
                            LiquidGlassVideoCard(
                                video = item,
                                onClick = { onPlayNextVideo(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}
