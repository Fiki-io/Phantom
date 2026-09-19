package com.phantom.tube.ui.screens.player

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.IBinder
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import com.phantom.tube.player.service.PhantomMediaService
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.runtime.mutableFloatStateOf
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.phantom.tube.ui.components.LiquidGlassMiniPlayer
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
import androidx.compose.runtime.rememberUpdatedState
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
import com.phantom.tube.core.theme.NeonPurple
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
    isMinimized: Boolean = false,
    onMinimize: () -> Unit = {},
    onExpand: () -> Unit = {},
    onClose: () -> Unit = {},
    onBackClick: () -> Unit = onMinimize,
    onPlayNextVideo: (VideoItem) -> Unit,
    onPlayPreviousVideo: () -> Boolean = { false },
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

    // YouTube Mix Playlist & Session (Preserving all songs in the Mix)
    var mixPlaylist by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var currentMixIndex by remember { mutableIntStateOf(0) }
    var recommendedVideos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var mixTitle by remember { mutableStateOf("") }
    var showMixSheet by remember { mutableStateOf(false) }
    var isInternalNavigation by remember { mutableStateOf(false) }
    var isLoadingQueue by remember { mutableStateOf(false) }

    // Favorites
    val isFavorite by repository.isFavorite(video.id).collectAsState(initial = false)

    var mediaService by remember { mutableStateOf<PhantomMediaService?>(null) }

    val currentVideo by rememberUpdatedState(video)
    val currentOnPlayNextVideo by rememberUpdatedState(onPlayNextVideo)
    val currentOnPlayPreviousVideo by rememberUpdatedState(onPlayPreviousVideo)

    val controller = remember { PhantomPlayerController(context) }

    val playNext: () -> Unit = {
        if (mixPlaylist.isNotEmpty()) {
            if (currentMixIndex < mixPlaylist.lastIndex) {
                val nextIndex = currentMixIndex + 1
                val nextVid = mixPlaylist[nextIndex]
                currentMixIndex = nextIndex
                isInternalNavigation = true
                currentOnPlayNextVideo(nextVid)
            } else {
                // At the end of playlist, ask YouTube for continuation of the Mix!
                scope.launch {
                    isLoadingQueue = true
                    val nextData = repository.getWatchNext(currentVideo.id, "RD${currentVideo.id}")
                    if (nextData != null && nextData.mixPlaylist.isNotEmpty()) {
                        val existingIds = mixPlaylist.map { it.id }.toSet()
                        val freshItems = nextData.mixPlaylist.filter { it.id !in existingIds }
                        if (freshItems.isNotEmpty()) {
                            val nextIndex = mixPlaylist.size
                            mixPlaylist = mixPlaylist + freshItems
                            currentMixIndex = nextIndex
                            isInternalNavigation = true
                            currentOnPlayNextVideo(mixPlaylist[nextIndex])
                        }
                    }
                    isLoadingQueue = false
                }
            }
        }
    }

    val playPrevious: () -> Boolean = {
        if (mixPlaylist.isNotEmpty() && currentMixIndex > 0) {
            val prevIndex = currentMixIndex - 1
            val prevVid = mixPlaylist[prevIndex]
            currentMixIndex = prevIndex
            isInternalNavigation = true
            currentOnPlayNextVideo(prevVid)
            true
        } else {
            controller.seekTo(0f)
            false
        }
    }

    val playFromMix: (Int) -> Unit = { targetIndex ->
        if (targetIndex in mixPlaylist.indices) {
            currentMixIndex = targetIndex
            isInternalNavigation = true
            currentOnPlayNextVideo(mixPlaylist[targetIndex])
        }
    }

    val bridge = remember {
        PhantomPlayerBridge(
            onReadyCallback = {
                controller.onReady()
            },
            onStateChangeCallback = { state ->
                // 1 = PLAYING, 2 = PAUSED, 3 = BUFFERING, 0 = ENDED
                when (state) {
                    1 -> {
                        playerState = playerState.copy(isPlaying = true, isBuffering = false, isEnded = false, errorCode = null)
                        mediaService?.updatePlaybackState(true, (playerState.currentTimeSec * 1000).toLong())
                    }
                    2 -> {
                        playerState = playerState.copy(isPlaying = false, isBuffering = false)
                        mediaService?.updatePlaybackState(false, (playerState.currentTimeSec * 1000).toLong())
                    }
                    3 -> playerState = playerState.copy(isBuffering = true)
                    0 -> {
                        playerState = playerState.copy(isPlaying = false, isEnded = true)
                        mediaService?.updatePlaybackState(false, (playerState.currentTimeSec * 1000).toLong())
                        playNext()
                    }
                }
            },
            onTimeUpdateCallback = { current, duration, buffered ->
                val wasZeroDuration = playerState.durationSec <= 0f && duration > 0f
                playerState = playerState.copy(
                    currentTimeSec = current,
                    durationSec = duration,
                    bufferedFraction = buffered
                )

                if (wasZeroDuration) {
                    mediaService?.updateDuration((duration * 1000).toLong())
                }

                // Record watch history
                if (current > 2f) {
                    val activeVid = currentVideo
                    scope.launch {
                        repository.recordWatch(
                            video = activeVid,
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
                val msg = when (errorCode) {
                    100 -> "Video tidak ditemukan (100)"
                    101, 150 -> "Pemilik video membatasi pemutaran di aplikasi lain (150)"
                    152 -> "Pemutaran dibatasi oleh YouTube (Error 152)"
                    2 -> "Parameter request tidak valid (2)"
                    5 -> "Kesalahan pemutar HTML5 (5)"
                    else -> "Error pemutaran ($errorCode)"
                }
                playerState = playerState.copy(isBuffering = false, errorCode = msg)
            }
        )
    }

    // Maintain persistent PhantomMediaService connection for the entire playback session
    DisposableEffect(Unit) {
        val serviceIntent = Intent(context, PhantomMediaService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val service = (binder as? PhantomMediaService.LocalBinder)?.getService()
                service?.apply {
                    onPlayAction = { controller.play() }
                    onPauseAction = { controller.pause() }
                    onNextAction = { playNext() }
                    onPreviousAction = { playPrevious() }
                    onSeekAction = { posMs ->
                        controller.seekTo(posMs / 1000f)
                    }
                }
                mediaService = service
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                mediaService = null
            }
        }

        try {
            context.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val activity = context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (isFullscreen) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            controller.release()
            try {
                context.unbindService(connection)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                context.stopService(Intent(context, PhantomMediaService::class.java))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Auto-hide controls timer
    LaunchedEffect(isControlsVisible, playerState.isPlaying) {
        if (isControlsVisible && playerState.isPlaying) {
            delay(4000)
            isControlsVisible = false
        }
    }

    // Load new video into engine & immediately sync notification
    LaunchedEffect(video.id, mediaService) {
        playerState = PlayerState(videoId = video.id)
        sponsorSegments = emptyList()
        val fromInternal = isInternalNavigation
        if (fromInternal) {
            isInternalNavigation = false
        }

        val lastPos = repository.getLastPosition(video.id)
        controller.loadVideo(video.id, lastPos / 1000f)

        mediaService?.updateMediaInfo(
            title = video.title,
            channel = video.channelTitle,
            durationMs = 0L,
            playing = true,
            thumbnailUrl = video.thumbnailUrl
        )

        launch {
            sponsorSegments = repository.getSponsorSegments(video.id)
        }

        launch {
            if (!fromInternal) {
                isLoadingQueue = true
                showMixSheet = false
                val nextData = repository.getWatchNext(video.id, "RD${video.id}")
                if (nextData != null) {
                    if (video.title.isBlank() || video.title == "Video" || video.channelTitle.isBlank()) {
                        mediaService?.updateMediaInfo(
                            title = nextData.currentVideo.title,
                            channel = nextData.currentVideo.channelTitle,
                            durationMs = 0L,
                            playing = true,
                            thumbnailUrl = nextData.currentVideo.thumbnailUrl
                        )
                    }
                    mixPlaylist = nextData.mixPlaylist
                    currentMixIndex = if (nextData.mixPlaylist.isNotEmpty()) {
                        val match = nextData.mixPlaylist.indexOfFirst { it.id == video.id }
                        if (match != -1) match else nextData.currentIndex
                    } else 0
                    recommendedVideos = nextData.recommendations
                    mixTitle = nextData.playlistTitle.ifBlank { "YouTube Mix" }
                }
                isLoadingQueue = false
            } else {
                val idx = mixPlaylist.indexOfFirst { it.id == video.id }
                if (idx != -1) {
                    currentMixIndex = idx
                }
                if (mixPlaylist.isNotEmpty() && currentMixIndex >= mixPlaylist.size - 5) {
                    val nextData = repository.getWatchNext(video.id, "RD${video.id}")
                    if (nextData != null && nextData.mixPlaylist.isNotEmpty()) {
                        val existingIds = mixPlaylist.map { it.id }.toSet()
                        val freshItems = nextData.mixPlaylist.filter { it.id !in existingIds }
                        if (freshItems.isNotEmpty()) {
                            mixPlaylist = mixPlaylist + freshItems
                        }
                    }
                }
            }
        }
    }

    BackHandler(enabled = !isMinimized) {
        if (isFullscreen) {
            val activity = context as? Activity
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            isFullscreen = false
        } else if (showMixSheet) {
            showMixSheet = false
        } else {
            onMinimize()
        }
    }

    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val animatedDragOffset by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "drag_minimize_offset"
    )

    LaunchedEffect(isMinimized) {
        dragOffsetY = 0f
    }

    val lazyListState = rememberLazyListState()
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (dragOffsetY > 0f && available.y < 0f) {
                    val consumed = available.y.coerceAtLeast(-dragOffsetY)
                    dragOffsetY += consumed
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f && !isMinimized && !isFullscreen && lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0) {
                    dragOffsetY = (dragOffsetY + available.y * 0.7f).coerceAtLeast(0f)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (dragOffsetY > 0f) {
                    if (dragOffsetY > 160f || available.y > 800f) {
                        dragOffsetY = 0f
                        onMinimize()
                    } else {
                        dragOffsetY = 0f
                    }
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    val dragModifier = if (!isMinimized && !isFullscreen) {
        Modifier.pointerInput(Unit) {
            detectVerticalDragGestures(
                onVerticalDrag = { change, dragAmount ->
                    if (dragAmount > 0f || dragOffsetY > 0f) {
                        change.consume()
                        dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                    }
                },
                onDragEnd = {
                    if (dragOffsetY > 160f) {
                        dragOffsetY = 0f
                        onMinimize()
                    } else {
                        dragOffsetY = 0f
                    }
                },
                onDragCancel = {
                    dragOffsetY = 0f
                }
            )
        }
    } else Modifier

    Box(
        modifier = if (isMinimized) {
            modifier
        } else {
            modifier
                .fillMaxSize()
                .background(ObsidianDark.copy(alpha = (1f - (animatedDragOffset / 1500f)).coerceIn(0.6f, 1f)))
                .offset { IntOffset(0, animatedDragOffset.roundToInt()) }
        }
    ) {
        // 1. THE SINGLE PERSISTENT VIDEO PLAYER BOX (Always at exact same tree slot)
        val videoBoxModifier = when {
            isMinimized -> Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .offset(y = (-3000).dp)
                .align(Alignment.TopCenter)
            isFullscreen -> Modifier.fillMaxSize()
            else -> Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .aspectRatio(16f / 9f)
                .align(Alignment.TopCenter)
        }

        Box(
            modifier = videoBoxModifier
                .then(dragModifier)
                .background(Color.Black)
                .zIndex(if (!isMinimized) 1f else 0f)
        ) {
            // Layer 0: The Ghost Surface (backed by WebViewAssetLoader)
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
                        text = playerState.errorCode ?: "Terjadi kesalahan pemutaran",
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
                    // Top Bar Controls: Back/Minimize & Speed
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiquidGlassIconButton(
                            icon = Icons.Default.ExpandMore,
                            contentDescription = "Perkecil Player",
                            size = 38.dp,
                            iconSize = 24.dp,
                            onClick = {
                                if (isFullscreen) {
                                    val activity = context as? Activity
                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                    isFullscreen = false
                                } else {
                                    onMinimize()
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
                                            1.0f -> 1.5f
                                            1.5f -> 2.0f
                                            2.0f -> 0.5f
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

                    // Center Row: SkipPrevious, Replay10, Play/Pause, Forward10, SkipNext
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiquidGlassIconButton(
                            icon = Icons.Default.SkipPrevious,
                            contentDescription = "Video Sebelumnya",
                            size = 42.dp,
                            iconSize = 22.dp,
                            onClick = { playPrevious() }
                        )

                        LiquidGlassIconButton(
                            icon = Icons.Default.Replay10,
                            contentDescription = "Mundur 10 Detik",
                            size = 42.dp,
                            iconSize = 22.dp,
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
                            size = 42.dp,
                            iconSize = 22.dp,
                            onClick = {
                                val newTime = (playerState.currentTimeSec + 10f).coerceAtMost(playerState.durationSec)
                                controller.seekTo(newTime)
                            }
                        )

                        LiquidGlassIconButton(
                            icon = Icons.Default.SkipNext,
                            contentDescription = "Video Berikutnya",
                            size = 42.dp,
                            iconSize = 22.dp,
                            onClick = { playNext() }
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

        // 2. BELOW PLAYER CONTENT (Only shown in portrait full-player mode)
        if (!isMinimized && !isFullscreen) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Spacer reserving the height of the top 16:9 Video Player Box
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                )

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                // Video Details Header
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(dragModifier)
                    ) {
                        // Subtle drag down indicator
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 10.dp)
                                .size(width = 38.dp, height = 4.dp)
                                .background(TextMuted.copy(alpha = 0.35f), RoundedCornerShape(2.dp))
                        )
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
                    }
                }

                // 2. YouTube Mix Banner Card (Membuka antrean mengambang)
                if (mixPlaylist.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(
                                    shape = RoundedCornerShape(16.dp),
                                    borderWidth = 1.dp,
                                    tintColor = Color(0xFF0F172A),
                                    glassAlpha = 0.85f
                                )
                                .clickable { showMixSheet = true }
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(NeonCyan.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.QueueMusic,
                                                contentDescription = null,
                                                tint = NeonCyan,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = mixTitle.ifBlank { "YouTube Mix" },
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "Lagu ${currentMixIndex + 1} dari ${mixPlaylist.size} • Mix resmi YouTube",
                                                color = TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Button Lihat Antrean
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .background(NeonCyan.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Lihat Antrean",
                                            color = NeonCyan,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (currentMixIndex < mixPlaylist.lastIndex) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Berikutnya: ${mixPlaylist[currentMixIndex + 1].title}",
                                        color = NeonCyan,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Section Rekomendasi Video (Foto 2: Bersih & Terpisah dari Mix)
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Rekomendasi Video",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isLoadingQueue && recommendedVideos.isEmpty()) {
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
                    itemsIndexed(recommendedVideos, key = { index, item -> "rec_${item.id}_$index" }) { _, item ->
                        LiquidGlassVideoCard(
                            video = item,
                            onClick = {
                                isInternalNavigation = false
                                currentOnPlayNextVideo(item)
                            }
                        )
                    }
                }
            }
        }
    }

        // 3. FLOATING MIX QUEUE SHEET (Only in full player mode)
        if (!isMinimized) {
            AnimatedVisibility(
                visible = showMixSheet && !isFullscreen,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.fillMaxSize()
            ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable { showMixSheet = false }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.72f)
                    .align(Alignment.BottomCenter)
                    .liquidGlass(
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        borderWidth = 1.dp,
                        tintColor = Color(0xFF0E1726),
                        glassAlpha = 0.96f
                    )
                    .clickable(enabled = false) {}
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Drag Handle
                    Box(
                        modifier = Modifier
                            .size(width = 42.dp, height = 4.dp)
                            .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                            .align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mixTitle.ifBlank { "YouTube Mix" },
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "Lagu ${currentMixIndex + 1} dari ${mixPlaylist.size} • Mix resmi YouTube",
                                color = NeonCyan,
                                fontSize = 12.sp
                            )
                        }

                        LiquidGlassIconButton(
                            icon = Icons.Default.Close,
                            contentDescription = "Tutup",
                            size = 36.dp,
                            iconSize = 18.dp,
                            onClick = { showMixSheet = false }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mix Playlist List (Semua lagu lengkap, tidak ada yang di-hide)
                    val listState = rememberLazyListState()
                    LaunchedEffect(showMixSheet) {
                        if (showMixSheet && currentMixIndex in mixPlaylist.indices) {
                            listState.animateScrollToItem(currentMixIndex)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        itemsIndexed(mixPlaylist, key = { index, item -> "mix_${item.id}_$index" }) { index, item ->
                            val isCurrent = index == currentMixIndex
                            MixPlaylistItemCard(
                                index = index + 1,
                                video = item,
                                isCurrent = isCurrent,
                                onClick = {
                                    playFromMix(index)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

        // 4. PERSISTENT LIQUID GLASS MINIPLAYER (When player is minimized)
        if (isMinimized) {
            LiquidGlassMiniPlayer(
                video = video,
                isPlaying = playerState.isPlaying,
                isBuffering = playerState.isBuffering,
                currentTimeSec = playerState.currentTimeSec,
                durationSec = playerState.durationSec,
                onExpand = onExpand,
                onTogglePlayPause = {
                    if (playerState.isPlaying) controller.pause() else controller.play()
                },
                onClose = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .zIndex(2f)
            )
        }
    }
}

@Composable
fun MixPlaylistItemCard(
    index: Int,
    video: VideoItem,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(14.dp),
                borderWidth = if (isCurrent) 1.5.dp else 0.8.dp,
                tintColor = if (isCurrent) Color(0xFF0F3246) else Color(0xFF14151F),
                glassAlpha = if (isCurrent) 0.85f else 0.45f
            )
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index number or Playing Indicator
            Box(
                modifier = Modifier.width(28.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrent) {
                    Text(
                        text = "▶",
                        color = NeonCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "$index",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Thumbnail with duration
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF101018))
            ) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                if (video.durationText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = video.durationText,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Title and Channel Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    color = if (isCurrent) NeonCyan else TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (isCurrent) "Sedang Diputar • ${video.channelTitle}" else video.channelTitle,
                    color = if (isCurrent) NeonCyan.copy(alpha = 0.8f) else TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
