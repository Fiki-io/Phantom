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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.QueueMusic
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

    // Playback Queue & History Session (YouTube Mix)
    var upNextQueue by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var recommendedVideos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var mixTitle by remember { mutableStateOf("") }
    var isMixExpanded by remember { mutableStateOf(false) }
    val history = remember { mutableListOf<VideoItem>() }
    val playedVideoIds = remember { mutableSetOf<String>() }
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
        if (upNextQueue.isNotEmpty()) {
            val nextVid = upNextQueue.first()
            upNextQueue = upNextQueue.drop(1)
            history.add(currentVideo)
            playedVideoIds.add(nextVid.id)
            isInternalNavigation = true
            currentOnPlayNextVideo(nextVid)
        } else {
            scope.launch {
                isLoadingQueue = true
                val nextData = repository.getWatchNext(currentVideo.id, "RD${currentVideo.id}")
                val candidates = nextData?.mixQueue?.filter {
                    it.id != currentVideo.id && it.id !in playedVideoIds
                }?.distinctBy { it.id } ?: nextData?.mixQueue?.filter { it.id != currentVideo.id }
                if (!candidates.isNullOrEmpty()) {
                    val nextVid = candidates.first()
                    upNextQueue = candidates.drop(1)
                    history.add(currentVideo)
                    playedVideoIds.add(nextVid.id)
                    isInternalNavigation = true
                    currentOnPlayNextVideo(nextVid)
                }
                isLoadingQueue = false
            }
        }
    }

    val playPrevious: () -> Boolean = {
        if (history.isNotEmpty()) {
            val prevVid = history.removeAt(history.lastIndex)
            upNextQueue = (listOf(currentVideo) + upNextQueue).distinctBy { it.id }
            isInternalNavigation = true
            currentOnPlayNextVideo(prevVid)
            true
        } else {
            controller.seekTo(0f)
            false
        }
    }

    val playFromQueue: (VideoItem) -> Unit = { targetVideo ->
        val targetIndex = upNextQueue.indexOfFirst { it.id == targetVideo.id }
        if (targetIndex != -1) {
            upNextQueue = upNextQueue.filterIndexed { index, _ -> index > targetIndex }
        }
        history.add(currentVideo)
        playedVideoIds.add(targetVideo.id)
        isInternalNavigation = true
        currentOnPlayNextVideo(targetVideo)
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
        } else {
            playedVideoIds.clear()
        }
        playedVideoIds.add(video.id)

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
                isMixExpanded = false
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
                    val freshItems = nextData.mixQueue.filter {
                        it.id != video.id && it.id !in playedVideoIds
                    }.distinctBy { it.id }
                    upNextQueue = if (freshItems.isNotEmpty()) {
                        freshItems
                    } else {
                        nextData.mixQueue.filter { it.id != video.id }.distinctBy { it.id }
                    }
                    recommendedVideos = nextData.recommendations
                    mixTitle = nextData.playlistTitle.ifBlank { "YouTube Mix" }
                }
                isLoadingQueue = false
            } else {
                if (upNextQueue.size < 5) {
                    val nextData = repository.getWatchNext(video.id, "RD${video.id}")
                    if (nextData != null && nextData.mixQueue.isNotEmpty()) {
                        val existingIds = upNextQueue.map { it.id }.toSet()
                        val freshItems = nextData.mixQueue.filter {
                            it.id !in playedVideoIds && it.id !in existingIds && it.id != video.id
                        }.distinctBy { it.id }
                        upNextQueue = upNextQueue + freshItems
                    }
                }
            }
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
                    }
                }

                // 2. YouTube Mix Card (Mirip YouTube Asli dengan tombol Lihat Antrean)
                if (mixTitle.isNotBlank() || upNextQueue.isNotEmpty()) {
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
                                .clickable { isMixExpanded = !isMixExpanded }
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
                                                text = "Mix resmi YouTube • ${upNextQueue.size + 1} video",
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
                                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (isMixExpanded) "Tutup" else "Antrean",
                                            color = NeonCyan,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = if (isMixExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                if (!isMixExpanded && upNextQueue.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Berikutnya: ${upNextQueue.first().title}",
                                        color = NeonCyan,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // If isMixExpanded is true: show the entire Mix Queue list (Foto 1)
                    if (isMixExpanded) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .liquidGlass(
                                        shape = RoundedCornerShape(12.dp),
                                        borderWidth = 1.dp,
                                        tintColor = Color(0xFF0C2738),
                                        glassAlpha = 0.9f
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "▶",
                                        color = NeonCyan,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = video.title,
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "Sedang Diputar • ${video.channelTitle}",
                                            color = NeonCyan,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        items(upNextQueue, key = { "mix_" + it.id }) { item ->
                            LiquidGlassVideoCard(
                                video = item,
                                onClick = { playFromQueue(item) }
                            )
                        }
                    }
                }

                // 3. Section Rekomendasi Video (Foto 2)
                item {
                    Spacer(modifier = Modifier.height(6.dp))
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
                    items(recommendedVideos, key = { "rec_" + it.id }) { item ->
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
}
