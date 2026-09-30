package com.phantom.tube.ui.screens.player

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.IBinder
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.phantom.tube.player.service.PhantomMediaService
import com.phantom.tube.data.innertube.parser.InnerTubeHelpers
import com.phantom.tube.ui.screens.player.components.FloatingMixBar
import com.phantom.tube.ui.screens.player.components.PlayerCommentsSheet
import com.phantom.tube.ui.screens.player.components.PlayerDescriptionSheet
import com.phantom.tube.ui.screens.player.components.PlayerMixSheet
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.phantom.tube.player.PlayerState
import com.phantom.tube.ui.components.PhantomMiniPlayer
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phantom.tube.core.theme.ObsidianDark
import com.phantom.tube.core.theme.TextMuted
import com.phantom.tube.core.theme.TextPrimary
import com.phantom.tube.core.theme.TextSecondary
import com.phantom.tube.core.theme.YouTubeRed
import com.phantom.tube.core.theme.YouTubeSurface
import com.phantom.tube.core.theme.phantomSurface
import com.phantom.tube.data.model.NextQueue
import com.phantom.tube.data.model.SponsorSegment
import com.phantom.tube.data.model.VideoItem
import com.phantom.tube.data.repository.PhantomRepository
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Settings
import com.phantom.tube.player.PhantomGhostSurface
import com.phantom.tube.player.PhantomPlayerBridge
import com.phantom.tube.player.PhantomPlayerController
import com.phantom.tube.ui.components.PhantomIconButton
import com.phantom.tube.ui.components.PhantomScrubber
import com.phantom.tube.ui.components.PhantomVideoCard
import com.phantom.tube.ui.components.PlayerSettingsSheet
import com.phantom.tube.ui.components.SleepTimerOption
import com.phantom.tube.ui.components.SponsorSkipPill
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.phantom.tube.core.database.SubscriptionEntity
import com.phantom.tube.data.model.VideoComment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SeekFeedbackDirection { FORWARD, REWIND }

@OptIn(ExperimentalMaterial3Api::class)
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
    onChannelClick: ((channelId: String, channelTitle: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var playerState by remember { mutableStateOf(PlayerState(videoId = video.id)) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isDeviceLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Sync fullscreen state with physical device orientation
    LaunchedEffect(isDeviceLandscape) {
        if (isDeviceLandscape && !isFullscreen) {
            isFullscreen = true
        } else if (!isDeviceLandscape && isFullscreen) {
            val activity = context as? Activity
            if (activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            isFullscreen = false
        }
    }

    // Hide status bar & navigation bar in landscape / fullscreen (Immersive Sticky Mode)
    DisposableEffect(isFullscreen, isMinimized) {
        val activity = context as? Activity
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (isFullscreen && !isMinimized) {
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val activity = context as? Activity
            val window = activity?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Double-Tap Seek Visual Feedback State
    var seekAnimationSide by remember { mutableStateOf<SeekFeedbackDirection?>(null) }
    var seekAccumulatedSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(seekAnimationSide, seekAccumulatedSeconds) {
        if (seekAnimationSide != null) {
            delay(750L)
            seekAnimationSide = null
            seekAccumulatedSeconds = 0
        }
    }

    // SponsorBlock state
    var sponsorSegments by remember { mutableStateOf<List<SponsorSegment>>(emptyList()) }
    var showSponsorPill by remember { mutableStateOf(false) }
    var lastSkippedSeconds by remember { mutableIntStateOf(0) }
    var lastSkippedFromSec by remember { mutableFloatStateOf(0f) }
    var lastSkippedCategory by remember { mutableStateOf("sponsor") }
    var lastTargetSkipEndSec by remember { mutableFloatStateOf(0f) }
    val skippedSegmentUuids = remember(video.id) { mutableSetOf<String>() }
    val undoneSegmentUuids = remember(video.id) { mutableSetOf<String>() }
    var appliedDefaultsVideoId by remember { mutableStateOf<String?>(null) }

    // YouTube Mix Playlist & Session (Preserving all songs in the Mix)
    var mixPlaylist by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var currentMixIndex by remember { mutableIntStateOf(0) }
    var recommendedVideos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var recContinuationToken by remember { mutableStateOf<String?>(null) }
    var isLoadingMoreRecs by remember { mutableStateOf(false) }
    var canLoadMoreRecs by remember { mutableStateOf(true) }
    var activeAvatarUrl by remember { mutableStateOf(video.channelAvatarUrl) }
    var nextQueueData by remember { mutableStateOf<NextQueue?>(null) }
    var commentsList by remember { mutableStateOf<List<VideoComment>>(emptyList()) }
    var commentsTotalCountText by remember { mutableStateOf("") }
    var commentsContinuationToken by remember { mutableStateOf<String?>(null) }
    var isLoadingComments by remember { mutableStateOf(false) }
    var showDescriptionSheet by remember { mutableStateOf(false) }
    var showCommentsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(video.id) {
        if (video.channelAvatarUrl.isNotBlank()) {
            activeAvatarUrl = video.channelAvatarUrl
        }
        recContinuationToken = null
        canLoadMoreRecs = true
        nextQueueData = null
        commentsList = emptyList()
        commentsTotalCountText = ""
        commentsContinuationToken = null
        isLoadingComments = false
        showDescriptionSheet = false
        showCommentsSheet = false
    }
    var mixTitle by remember { mutableStateOf("") }
    var showMixSheet by remember { mutableStateOf(false) }
    var isInternalNavigation by remember { mutableStateOf(false) }
    var isLoadingQueue by remember { mutableStateOf(false) }

    // Settings States (Speed, Double Tap Seek, Sleep Timer, Loop, Autoplay, Audio-Only)
    var showSettingsSheet by remember { mutableStateOf(false) }
    var doubleTapSeekSeconds by remember { mutableIntStateOf(10) }
    var isLoopEnabled by remember { mutableStateOf(false) }
    var isAutoplayNext by remember { mutableStateOf(true) }
    var isAudioOnly by remember { mutableStateOf(false) }
    var sleepTimerOption by remember { mutableStateOf(SleepTimerOption.OFF) }
    var sleepTimerRemainingSec by remember { mutableStateOf<Int?>(null) }
    var lastRecordedPositionSec by remember { mutableFloatStateOf(0f) }
    val isFavorite by repository.isFavorite(video.id).collectAsState(initial = false)

    var mediaService by remember { mutableStateOf<PhantomMediaService?>(null) }

    val currentVideo by rememberUpdatedState(video)
    val currentOnPlayNextVideo by rememberUpdatedState(onPlayNextVideo)
    val currentOnPlayPreviousVideo by rememberUpdatedState(onPlayPreviousVideo)

    val controller = remember { PhantomPlayerController(context) }

    // Sleep Timer countdown effect
    LaunchedEffect(sleepTimerRemainingSec, playerState.isPlaying) {
        if (sleepTimerRemainingSec != null && (sleepTimerRemainingSec ?: 0) > 0 && playerState.isPlaying) {
            delay(1000L)
            if (playerState.isPlaying) {
                val current = (sleepTimerRemainingSec ?: 0) - 1
                if (current <= 0) {
                    controller.pause()
                    sleepTimerRemainingSec = null
                    sleepTimerOption = SleepTimerOption.OFF
                } else {
                    sleepTimerRemainingSec = current
                }
            }
        }
    }

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
        if (playerState.currentTimeSec > 3f) {
            // Standard media playback: rewinds current track if played past 3 seconds
            controller.seekTo(0f)
            true
        } else if (mixPlaylist.isNotEmpty() && currentMixIndex > 0) {
            val prevIndex = currentMixIndex - 1
            val prevVid = mixPlaylist[prevIndex]
            currentMixIndex = prevIndex
            isInternalNavigation = true
            scope.launch {
                repository.resetWatchPosition(prevVid.id)
            }
            currentOnPlayNextVideo(prevVid)
            true
        } else {
            val handled = currentOnPlayPreviousVideo()
            if (!handled) {
                controller.seekTo(0f)
            }
            handled
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
                        if (appliedDefaultsVideoId != currentVideo.id) {
                            appliedDefaultsVideoId = currentVideo.id
                            val defSpeed = repository.preferences?.defaultSpeed?.value ?: 1.0f
                            if (defSpeed != 1.0f) {
                                playerState = playerState.copy(playbackSpeed = defSpeed)
                                controller.setPlaybackRate(defSpeed)
                            }
                            val defQuality = repository.preferences?.defaultQuality?.value ?: "auto"
                            if (defQuality != "auto") {
                                playerState = playerState.copy(currentQuality = defQuality)
                                controller.setPlaybackQuality(defQuality)
                            }
                        }
                    }
                    2 -> {
                        playerState = playerState.copy(isPlaying = false, isBuffering = false)
                        mediaService?.updatePlaybackState(false, (playerState.currentTimeSec * 1000).toLong())
                        val activeVid = currentVideo
                        val pos = playerState.currentTimeSec
                        val dur = playerState.durationSec
                        if (pos > 1f) {
                            scope.launch {
                                repository.recordWatch(
                                    video = activeVid,
                                    positionMs = (pos * 1000).toLong(),
                                    durationMs = (dur * 1000).toLong()
                                )
                            }
                        }
                    }
                    3 -> playerState = playerState.copy(isBuffering = true)
                    0 -> {
                        playerState = playerState.copy(isPlaying = false, isEnded = true)
                        mediaService?.updatePlaybackState(false, 0L)
                        scope.launch {
                            repository.resetWatchPosition(video.id)
                        }
                        if (isLoopEnabled) {
                            controller.seekTo(0f)
                            controller.play()
                        } else if (sleepTimerOption == SleepTimerOption.END_OF_VIDEO) {
                            controller.pause()
                            sleepTimerOption = SleepTimerOption.OFF
                            sleepTimerRemainingSec = null
                        } else if (isAutoplayNext) {
                            playNext()
                        }
                    }
                }
            },
            onTimeUpdateCallback = { reportingVideoId, current, duration, buffered ->
                // Guard against stale time ticks from previous video during transition
                if (reportingVideoId.isNotBlank() && reportingVideoId != currentVideo.id) {
                    return@PhantomPlayerBridge
                }

                val wasZeroDuration = playerState.durationSec <= 0f && duration > 0f
                playerState = playerState.copy(
                    currentTimeSec = current,
                    durationSec = duration,
                    bufferedFraction = buffered
                )

                if (wasZeroDuration) {
                    mediaService?.updateDuration((duration * 1000).toLong())
                }

                // Throttled watch history recording (every 5 seconds instead of 4 times/sec)
                if (current > 2f && (current - lastRecordedPositionSec >= 5f || current < lastRecordedPositionSec)) {
                    lastRecordedPositionSec = current
                    val activeVid = currentVideo
                    scope.launch {
                        repository.recordWatch(
                            video = activeVid,
                            positionMs = (current * 1000).toLong(),
                            durationMs = (duration * 1000).toLong()
                        )
                    }
                }

                // Check SponsorBlock segments (UUID tracking & valid outro bounds)
                val prefs = repository.preferences
                val sbEnabled = prefs?.sponsorBlockEnabled?.value ?: true
                if (sbEnabled && duration > 0f && current < duration) {
                    val skipSponsor = prefs?.skipSponsor?.value ?: true
                    val skipSelfPromo = prefs?.skipSelfPromo?.value ?: true
                    val skipInteraction = prefs?.skipInteraction?.value ?: true
                    val skipIntro = prefs?.skipIntro?.value ?: true
                    val skipOutro = prefs?.skipOutro?.value ?: true
                    val autoSkip = prefs?.sponsorBlockAutoSkip?.value ?: true

                    sponsorSegments.forEach { seg ->
                        val isCatAllowed = when (seg.category) {
                            "sponsor" -> skipSponsor
                            "selfpromo" -> skipSelfPromo
                            "interaction" -> skipInteraction
                            "intro" -> skipIntro
                            "outro" -> skipOutro
                            else -> true
                        }
                        if (isCatAllowed) {
                            val uuid = if (seg.uuid.isNotBlank()) seg.uuid else "${seg.category}_${seg.startSecond}_${seg.endSecond}"
                            if (uuid !in skippedSegmentUuids && uuid !in undoneSegmentUuids) {
                                // Outro is only valid if video is past half duration and current is past half duration
                                val isOutroValid = if (seg.category == "outro") {
                                    seg.startSecond >= (duration * 0.5f) && current >= (duration * 0.5f)
                                } else true

                                if (isOutroValid && current >= seg.startSecond && current < (seg.endSecond - 0.5f)) {
                                    skippedSegmentUuids.add(uuid)
                                    lastSkippedFromSec = current
                                    lastTargetSkipEndSec = seg.endSecond
                                    lastSkippedSeconds = (seg.endSecond - seg.startSecond).toInt().coerceAtLeast(1)
                                    lastSkippedCategory = seg.category
                                    if (autoSkip) {
                                        controller.seekTo(seg.endSecond)
                                    }
                                    showSponsorPill = true
                                    scope.launch {
                                        delay(4000)
                                        showSponsorPill = false
                                    }
                                }
                            }
                        }
                    }
                }
            },
            onErrorCallback = { errorCode ->
                val msg = when (errorCode) {
                    100 -> "Video tidak tersedia"
                    101, 150 -> "Pemutaran dibatasi pada perangkat ini"
                    152 -> "Format video tidak didukung"
                    2 -> "Parameter tidak valid"
                    5 -> "Gagal memuat video"
                    else -> "Kesalahan pemutaran ($errorCode)"
                }
                playerState = playerState.copy(isBuffering = false, errorCode = msg)
            },
            onQualityChangeCallback = { currentQuality, availableQualities ->
                playerState = playerState.copy(
                    currentQuality = currentQuality,
                    availableQualities = availableQualities
                )
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
                val window = activity?.window
                if (window != null) {
                    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                }
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

    // Enforce Background Playback preference when screen turns off or app leaves foreground
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val isBgAllowed = repository.preferences?.backgroundPlaybackEnabled?.value ?: true
                val activity = context as? Activity
                val inPip = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    activity?.isInPictureInPictureMode == true
                } else false
                if (!isBgAllowed && !inPip) {
                    controller.pause()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
        appliedDefaultsVideoId = null
        sponsorSegments = emptyList()
        lastRecordedPositionSec = 0f
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
            isLoadingQueue = true
            val nextData = repository.getWatchNext(video.id, "RD${video.id}")
            if (nextData != null) {
                nextQueueData = nextData
                if (nextData.currentVideo.channelAvatarUrl.isNotBlank()) {
                    activeAvatarUrl = nextData.currentVideo.channelAvatarUrl
                }
                mediaService?.updateMediaInfo(
                    title = nextData.currentVideo.title.ifBlank { video.title },
                    channel = nextData.currentVideo.channelTitle.ifBlank { video.channelTitle },
                    durationMs = 0L,
                    playing = true,
                    thumbnailUrl = nextData.currentVideo.thumbnailUrl.ifBlank { video.thumbnailUrl }
                )
                recommendedVideos = nextData.recommendations
                recContinuationToken = nextData.recommendationsContinuationToken
                canLoadMoreRecs = true

                if (!fromInternal) {
                    showMixSheet = false
                    mixPlaylist = nextData.mixPlaylist
                    currentMixIndex = if (nextData.mixPlaylist.isNotEmpty()) {
                        val match = nextData.mixPlaylist.indexOfFirst { it.id == video.id }
                        if (match != -1) match else nextData.currentIndex
                    } else 0
                    mixTitle = nextData.playlistTitle.ifBlank { "Mix" }
                } else {
                    // Internal navigation: preserve currentMixIndex and prevent duplicate ID jumps
                    if (currentMixIndex !in mixPlaylist.indices || mixPlaylist[currentMixIndex].id != video.id) {
                        val idx = mixPlaylist.indexOfFirst { it.id == video.id }
                        if (idx != -1) {
                            currentMixIndex = idx
                        }
                    }
                    if (mixPlaylist.isNotEmpty() && currentMixIndex >= mixPlaylist.size - 5) {
                        val existingIds = mixPlaylist.map { it.id }.toSet()
                        val freshItems = nextData.mixPlaylist.filter { it.id !in existingIds }
                        if (freshItems.isNotEmpty()) {
                            mixPlaylist = mixPlaylist + freshItems
                        }
                    }
                }

                if (!nextData.commentsContinuationToken.isNullOrBlank()) {
                    launch {
                        isLoadingComments = true
                        try {
                            val cResult = repository.getComments(nextData.commentsContinuationToken)
                            commentsList = cResult.comments
                            commentsTotalCountText = cResult.totalCountText.ifBlank { nextData.commentsCountText }
                            commentsContinuationToken = cResult.continuationToken
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            isLoadingComments = false
                        }
                    }
                }
            } else {
                if (fromInternal) {
                    if (currentMixIndex !in mixPlaylist.indices || mixPlaylist[currentMixIndex].id != video.id) {
                        val idx = mixPlaylist.indexOfFirst { it.id == video.id }
                        if (idx != -1) {
                            currentMixIndex = idx
                        }
                    }
                }
            }
            isLoadingQueue = false
        }
    }

    val exitFullscreenToPortrait = {
        val activity = context as? Activity
        if (activity != null) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            activity.window.decorView.postDelayed({
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }, 600)
        }
        isFullscreen = false
    }

    BackHandler(enabled = !isMinimized) {
        if (showDescriptionSheet) {
            showDescriptionSheet = false
        } else if (showCommentsSheet) {
            showCommentsSheet = false
        } else if (showSettingsSheet) {
            showSettingsSheet = false
        } else if (isFullscreen) {
            exitFullscreenToPortrait()
        } else if (showMixSheet) {
            showMixSheet = false
        } else {
            onBackClick()
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

    val haptic = LocalHapticFeedback.current
    var hasTriggeredHaptic by remember { mutableStateOf(false) }

    LaunchedEffect(dragOffsetY) {
        val threshold = if (isFullscreen) 80f else 180f
        if (dragOffsetY > threshold && !hasTriggeredHaptic) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            hasTriggeredHaptic = true
        } else if (dragOffsetY == 0f) {
            hasTriggeredHaptic = false
        }
    }

    LaunchedEffect(isMinimized) {
        dragOffsetY = 0f
    }

    val lazyListState = rememberLazyListState()

    val dragModifier = if (!isMinimized) {
        Modifier.pointerInput(isFullscreen) {
            detectVerticalDragGestures(
                onVerticalDrag = { change, dragAmount ->
                    if (dragAmount > 0f || dragOffsetY > 0f) {
                        change.consume()
                        dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                    }
                },
                onDragEnd = {
                    if (isFullscreen) {
                        // Tarik dari atas ke bawah di mode landscape / fullscreen untuk kembali ke portrait (seperti YouTube)
                        if (dragOffsetY > 80f) {
                            dragOffsetY = 0f
                            exitFullscreenToPortrait()
                        } else {
                            dragOffsetY = 0f
                        }
                    } else {
                        // Tarik ke bawah di mode portrait untuk memperkecil ke miniplayer
                        if (dragOffsetY > 220f) {
                            dragOffsetY = 0f
                            onMinimize()
                        } else {
                            dragOffsetY = 0f
                        }
                    }
                },
                onDragCancel = {
                    dragOffsetY = 0f
                }
            )
        }
    } else Modifier

    LaunchedEffect(showCommentsSheet) {
        if (showCommentsSheet) {
            if (commentsList.isEmpty() && !isLoadingComments) {
                val token = commentsContinuationToken ?: nextQueueData?.commentsContinuationToken
                if (!token.isNullOrBlank()) {
                    scope.launch {
                        isLoadingComments = true
                        try {
                            val cResult = repository.getComments(token)
                            commentsList = cResult.comments
                            if (cResult.totalCountText.isNotBlank()) {
                                commentsTotalCountText = cResult.totalCountText
                            } else if (commentsTotalCountText.isBlank()) {
                                commentsTotalCountText = nextQueueData?.commentsCountText ?: ""
                            }
                            commentsContinuationToken = cResult.continuationToken
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            isLoadingComments = false
                        }
                    }
                }
            }
        }
    }

    val loadMoreRecommendations: () -> Unit = {
        if (!isLoadingMoreRecs && !isLoadingQueue && canLoadMoreRecs) {
            scope.launch {
                isLoadingMoreRecs = true
                try {
                    val result = repository.getMoreRecommendations(
                        video = currentVideo,
                        continuation = recContinuationToken
                    )
                    if (result.videos.isNotEmpty()) {
                        val existingIds = (recommendedVideos + mixPlaylist).map { it.id }.toSet()
                        val freshVideos = result.videos.filterNot { it.id in existingIds }
                        if (freshVideos.isNotEmpty()) {
                            recommendedVideos = recommendedVideos + freshVideos
                        }
                    } else {
                        if (result.continuationToken == null) {
                            canLoadMoreRecs = false
                        }
                    }
                    recContinuationToken = result.continuationToken
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isLoadingMoreRecs = false
                }
            }
        }
    }

    // Infinite scroll listener for recommendations list below the video
    LaunchedEffect(lazyListState, recContinuationToken, isLoadingMoreRecs, canLoadMoreRecs) {
        snapshotFlow {
            val layoutInfo = lazyListState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val last = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && last >= total - 3
        }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                if (!isLoadingMoreRecs && !isLoadingQueue && canLoadMoreRecs && recommendedVideos.isNotEmpty()) {
                    loadMoreRecommendations()
                }
            }
    }

    val dragProgress = (animatedDragOffset / 220f).coerceIn(0f, 1f)
    val backdropScrimAlpha = if (isFullscreen) {
        (1f - (animatedDragOffset / 120f)).coerceIn(0f, 0.95f)
    } else {
        (1f - dragProgress).coerceIn(0f, 1f)
    }

    Box(
        modifier = if (isMinimized) {
            modifier
        } else {
            modifier
                .fillMaxSize()
                .drawBehind {
                    if (backdropScrimAlpha > 0f) {
                        drawRect(ObsidianDark.copy(alpha = backdropScrimAlpha))
                    }
                }
        }
    ) {
        // 1. THE SINGLE PERSISTENT VIDEO PLAYER BOX (Always at exact same tree slot)
        val videoCorners = (dragProgress * 16f).dp
        val videoBoxModifier = when {
            isMinimized -> Modifier
                .size(1.dp)
                .alpha(0.01f)
                .align(Alignment.TopStart)
            isFullscreen -> {
                val landscapeDragProgress = (animatedDragOffset / 100f).coerceIn(0f, 1f)
                val landscapeScale = (1f - (animatedDragOffset / 1000f)).coerceIn(0.88f, 1f)
                val landscapeCorners = (landscapeDragProgress * 20f).dp
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = landscapeScale
                        scaleY = landscapeScale
                        translationY = animatedDragOffset
                        shape = RoundedCornerShape(landscapeCorners)
                        clip = landscapeCorners > 0.dp
                        shadowElevation = landscapeDragProgress * 24f
                    }
            }
            else -> Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, animatedDragOffset.roundToInt()) }
                .statusBarsPadding()
                .aspectRatio(16f / 9f)
                .align(Alignment.TopCenter)
                .clip(RoundedCornerShape(videoCorners))
                .shadow(
                    elevation = (dragProgress * 14f).dp,
                    shape = RoundedCornerShape(videoCorners),
                    ambientColor = Color(0x66FF0033),
                    spotColor = Color(0x99000000)
                )
        }

        Box(
            modifier = videoBoxModifier
                .then(dragModifier)
                .background(Color.Black)
                .zIndex(if (!isMinimized) 10f else 0f)
        ) {
            // Layer 0: The Ghost Surface (backed by WebViewAssetLoader)
            PhantomGhostSurface(
                videoId = video.id,
                modifier = Modifier.matchParentSize(),
                controller = controller,
                bridge = bridge
            )

            // Audio-Only Mode AMOLED Overlay
            if (isAudioOnly) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color(0xFF07070A)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .phantomSurface(
                                    shape = CircleShape,
                                    borderWidth = 1.dp,
                                    tintColor = YouTubeSurface,
                                    surfaceAlpha = 0.95f,
                                    accentGlow = Color.Transparent
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = null,
                                tint = YouTubeRed,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = "Mode Audio Saja",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Layar dinonaktifkan untuk menghemat daya",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Layer 1: Transparent Gesture Touch Handler
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(doubleTapSeekSeconds) {
                        detectTapGestures(
                            onTap = {
                                isControlsVisible = !isControlsVisible
                            },
                            onDoubleTap = { offset ->
                                val delta = doubleTapSeekSeconds
                                if (offset.x < size.width / 2) {
                                    seekAccumulatedSeconds = if (seekAnimationSide == SeekFeedbackDirection.REWIND) {
                                        seekAccumulatedSeconds + delta
                                    } else {
                                        delta
                                    }
                                    seekAnimationSide = SeekFeedbackDirection.REWIND
                                    val newTime = (playerState.currentTimeSec - delta.toFloat()).coerceAtLeast(0f)
                                    controller.seekTo(newTime)
                                } else {
                                    seekAccumulatedSeconds = if (seekAnimationSide == SeekFeedbackDirection.FORWARD) {
                                        seekAccumulatedSeconds + delta
                                    } else {
                                        delta
                                    }
                                    seekAnimationSide = SeekFeedbackDirection.FORWARD
                                    val newTime = (playerState.currentTimeSec + delta.toFloat()).coerceAtMost(playerState.durationSec)
                                    controller.seekTo(newTime)
                                }
                            }
                        )
                    }
            )

            // Layer 1.5: Double-Tap Seek Visual Ripple Indicator
            AnimatedVisibility(
                visible = seekAnimationSide == SeekFeedbackDirection.REWIND,
                enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.85f),
                exit = fadeOut(tween(250)),
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.42f)
                    .align(Alignment.CenterStart)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topEndPercent = 100, bottomEndPercent = 100))
                        .background(Color(0x55000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "-${seekAccumulatedSeconds} dtk",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = seekAnimationSide == SeekFeedbackDirection.FORWARD,
                enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.85f),
                exit = fadeOut(tween(250)),
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.42f)
                    .align(Alignment.CenterEnd)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStartPercent = 100, bottomStartPercent = 100))
                        .background(Color(0x55000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "+${seekAccumulatedSeconds} dtk",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Layer 2: Buffering & Error Indicator
            if (playerState.isBuffering) {
                CircularProgressIndicator(
                    color = YouTubeRed,
                    strokeWidth = 3.dp,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                )
            } else if (playerState.errorCode != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .phantomSurface(
                            shape = RoundedCornerShape(16.dp),
                            borderWidth = 1.dp,
                            tintColor = Color(0xFF261010),
                            surfaceAlpha = 0.85f
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
            val isManualSkipMode = !(repository.preferences?.sponsorBlockAutoSkip?.value ?: true)
            SponsorSkipPill(
                visible = showSponsorPill,
                skippedSeconds = lastSkippedSeconds,
                category = lastSkippedCategory,
                isManualMode = isManualSkipMode,
                onSkip = {
                    controller.seekTo(lastTargetSkipEndSec)
                    showSponsorPill = false
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                onUndo = {
                    undoneSegmentUuids.addAll(skippedSegmentUuids)
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
                ) {
                    // Top Bar Controls: Back/Minimize & Speed
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(start = 12.dp, end = 12.dp, top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PhantomIconButton(
                            icon = Icons.Default.ExpandMore,
                            contentDescription = "Perkecil Player",
                            size = 38.dp,
                            iconSize = 24.dp,
                            onClick = {
                                if (isFullscreen) {
                                    exitFullscreenToPortrait()
                                } else {
                                    onMinimize()
                                }
                            }
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Playback Speed Quick Button (cycles 1.0x -> 1.5x -> 2.0x -> 0.5x)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF272727))
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
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Settings Button
                            PhantomIconButton(
                                icon = Icons.Default.Settings,
                                contentDescription = "Pengaturan",
                                size = 36.dp,
                                iconSize = 20.dp,
                                onClick = {
                                    showCommentsSheet = false
                                    showDescriptionSheet = false
                                    showMixSheet = false
                                    showSettingsSheet = true
                                }
                            )
                        }
                    }

                    // Center Row: SkipPrevious, Replay10, Play/Pause, Forward10, SkipNext
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PhantomIconButton(
                            icon = Icons.Default.SkipPrevious,
                            contentDescription = "Video Sebelumnya",
                            size = 42.dp,
                            iconSize = 22.dp,
                            onClick = { playPrevious() }
                        )

                        PhantomIconButton(
                            icon = Icons.Default.Replay10,
                            contentDescription = "Mundur 10 Detik",
                            size = 42.dp,
                            iconSize = 22.dp,
                            onClick = {
                                val newTime = (playerState.currentTimeSec - 10f).coerceAtLeast(0f)
                                controller.seekTo(newTime)
                            }
                        )

                        PhantomIconButton(
                            icon = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                            size = 64.dp,
                            iconSize = 36.dp,
                            onClick = {
                                if (playerState.isPlaying) {
                                    controller.pause()
                                } else {
                                    controller.play()
                                }
                            }
                        )

                        PhantomIconButton(
                            icon = Icons.Default.Forward10,
                            contentDescription = "Maju 10 Detik",
                            size = 42.dp,
                            iconSize = 22.dp,
                            onClick = {
                                val newTime = (playerState.currentTimeSec + 10f).coerceAtMost(playerState.durationSec)
                                controller.seekTo(newTime)
                            }
                        )

                        PhantomIconButton(
                            icon = Icons.Default.SkipNext,
                            contentDescription = "Video Berikutnya",
                            size = 42.dp,
                            iconSize = 22.dp,
                            onClick = { playNext() }
                        )
                    }

                    // Bottom Row: Timestamps + Fullscreen above Scrubber, Scrubber at bottom edge
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, bottom = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x99000000))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${playerState.formattedCurrentTime} / ${playerState.formattedDuration}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            PhantomIconButton(
                                icon = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                size = 36.dp,
                                iconSize = 20.dp,
                                onClick = {
                                    if (isFullscreen) {
                                        exitFullscreenToPortrait()
                                    } else {
                                        val activity = context as? Activity
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                        isFullscreen = true
                                    }
                                }
                            )
                        }

                        // Scrubber with thumb dot at the tip of progress, touching bottom edge
                        PhantomScrubber(
                            progress = playerState.progressFraction,
                            bufferedFraction = playerState.bufferedFraction,
                            showThumb = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp),
                            onSeek = { fraction ->
                                val targetSec = fraction * playerState.durationSec
                                controller.seekTo(targetSec)
                            }
                        )
                    }
                }
            }

            // Layer 5: Persistent Idle Progress Line (When controls are hidden)
            if (!isControlsVisible) {
                PhantomScrubber(
                    progress = playerState.progressFraction,
                    bufferedFraction = playerState.bufferedFraction,
                    showThumb = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .height(2.5.dp)
                )
            }
        }

        // 2. BELOW PLAYER CONTENT (Only shown in portrait full-player mode)
        if (!isMinimized && !isFullscreen) {
            val contentAlpha = (1f - (animatedDragOffset / 90f) * 1.5f).coerceIn(0f, 1f)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, animatedDragOffset.roundToInt()) }
                    .statusBarsPadding()
                    .graphicsLayer {
                        alpha = contentAlpha
                    }
                    .zIndex(1f)
            ) {
                // Spacer reserving the height of the top 16:9 Video Player Box
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                )

                // Dedicated subtle drag handle directly under video player
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(dragModifier)
                        .padding(top = 4.dp, bottom = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 38.dp, height = 4.dp)
                            .background(TextMuted.copy(alpha = 0.35f), RoundedCornerShape(2.dp))
                    )
                }

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                // Video Details Header
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 1. Video Title
                        val activeTitle = nextQueueData?.currentVideo?.title?.ifBlank { video.title } ?: video.title
                        Text(
                            text = activeTitle,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // 2. Sub-info row (Handle • Likes • Views • Date • ...selengkapnya)
                        val activeHandle = nextQueueData?.channelHandle?.let { if (it.startsWith("@")) it else "@$it" }
                            ?: ""
                        val activeViews = InnerTubeHelpers.normalizeViewCount(nextQueueData?.fullViewCountText?.ifBlank { video.viewCountText } ?: video.viewCountText)
                        val activeDate = InnerTubeHelpers.normalizePublishedTime(nextQueueData?.dateText?.ifBlank { video.publishedTimeText } ?: video.publishedTimeText)
                        val activeLikeCount = nextQueueData?.likeCountText ?: ""

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showCommentsSheet = false
                                    showMixSheet = false
                                    showSettingsSheet = false
                                    showDescriptionSheet = true
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                buildAnnotatedString {
                                    if (activeHandle.isNotBlank()) {
                                        append(activeHandle)
                                        append(" • ")
                                    }
                                    if (activeLikeCount.isNotBlank()) {
                                        append("$activeLikeCount suka • ")
                                    }
                                    if (activeViews.isNotBlank()) {
                                        append("$activeViews • ")
                                    }
                                    if (activeDate.isNotBlank()) {
                                        append("$activeDate • ")
                                    }
                                    withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Bold)) {
                                        append("...selengkapnya")
                                    }
                                },
                                color = TextMuted,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3. Channel Row (Avatar, Name, Subscriber count, Subscribe button)
                        val targetChannelId = nextQueueData?.currentVideo?.channelId?.ifBlank { video.channelId } ?: video.channelId
                        val targetChannelTitle = nextQueueData?.currentVideo?.channelTitle?.ifBlank { video.channelTitle } ?: video.channelTitle
                        val isSubscribed by repository.isSubscribed(targetChannelId).collectAsState(initial = false)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            val tId = targetChannelId.ifBlank { targetChannelTitle }
                                            if (tId.isNotBlank()) {
                                                onMinimize()
                                                onChannelClick?.invoke(tId, targetChannelTitle)
                                            }
                                        }
                                    )
                            ) {
                                // Channel Avatar
                                val currentAvatar = nextQueueData?.currentVideo?.channelAvatarUrl?.ifBlank { activeAvatarUrl } ?: activeAvatarUrl
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF2C2D42), Color(0xFF1B1C28))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (currentAvatar.isNotBlank()) {
                                        AsyncImage(
                                            model = currentAvatar,
                                            contentDescription = targetChannelTitle,
                                            modifier = Modifier.matchParentSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = YouTubeRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = targetChannelTitle.ifBlank { "Channel" },
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val subsText = nextQueueData?.channelSubscriberCountText ?: ""
                                    if (subsText.isNotBlank()) {
                                        Text(
                                            text = subsText,
                                            color = TextMuted,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Subscribe Button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSubscribed) Color(0xFF272727) else Color.White)
                                    .clickable {
                                        val currentAvatar = nextQueueData?.currentVideo?.channelAvatarUrl?.ifBlank { activeAvatarUrl } ?: activeAvatarUrl
                                        scope.launch {
                                            if (isSubscribed) {
                                                repository.unsubscribe(targetChannelId)
                                            } else {
                                                repository.subscribe(
                                                    SubscriptionEntity(
                                                        channelId = targetChannelId,
                                                        channelTitle = targetChannelTitle,
                                                        channelHandle = nextQueueData?.channelHandle ?: "",
                                                        channelAvatarUrl = currentAvatar,
                                                        subscriberCountText = nextQueueData?.channelSubscriberCountText ?: ""
                                                    )
                                                )
                                            }
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = if (isSubscribed) "Disubscribe" else "Subscribe",
                                    color = if (isSubscribed) Color.White else Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 4. Action Buttons Row (Like/Dislike total display pill, Bagikan intent pill, Simpan pill)
                        // User instruction:
                        // - Like/Dislike: tampilkan total aja gak usah tambah fitur like asli
                        // - Bagikan: harus punya fungsi
                        // - Gemini titik tiga: gak usah
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Like & Dislike Pill
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF272727))
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ThumbUp,
                                    contentDescription = "Suka",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = activeLikeCount.ifBlank { "Suka" },
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(16.dp)
                                        .background(Color(0x33FFFFFF))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Icon(
                                    imageVector = Icons.Default.ThumbDown,
                                    contentDescription = "Tidak suka",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Bagikan (Share) Pill
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF272727))
                                    .clickable {
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, activeTitle)
                                            putExtra(Intent.EXTRA_TEXT, "$activeTitle\nhttps://youtu.be/${video.id}")
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, "Bagikan")
                                        context.startActivity(shareIntent)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Bagikan",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Bagikan",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Simpan (Favorite) Pill
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF272727))
                                    .clickable {
                                        scope.launch {
                                            repository.toggleFavorite(video, isFavorite)
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Simpan",
                                    tint = if (isFavorite) YouTubeRed else TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isFavorite) "Tersimpan" else "Simpan",
                                    color = if (isFavorite) YouTubeRed else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 5. Comments Preview Card (Screenshot 5)
                        val totalCommentsDisplay = commentsTotalCountText.ifBlank {
                            nextQueueData?.commentsCountText ?: ""
                        }
                        val topCommentSnippet = commentsList.firstOrNull()?.contentText
                            ?: nextQueueData?.topComment?.contentText
                            ?: "Ketuk untuk melihat komentar..."
                        val topCommentAvatar = commentsList.firstOrNull()?.authorAvatarUrl
                            ?: nextQueueData?.topComment?.authorAvatarUrl
                            ?: ""

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF212121))
                                .clickable {
                                    showDescriptionSheet = false
                                    showMixSheet = false
                                    showSettingsSheet = false
                                    showCommentsSheet = true
                                }
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Komentar",
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (totalCommentsDisplay.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = totalCommentsDisplay,
                                            color = TextMuted,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (topCommentAvatar.isNotBlank()) {
                                        AsyncImage(
                                            model = topCommentAvatar,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = topCommentSnippet,
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Section Video Berikutnya
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Berikutnya",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
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
                                color = YouTubeRed,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        items = recommendedVideos,
                        key = { index, item -> "rec_${item.id}_$index" },
                        contentType = { _, _ -> "video_card" }
                    ) { _, item ->
                        PhantomVideoCard(
                            video = item,
                            onChannelClick = { chId ->
                                onMinimize()
                                onChannelClick?.invoke(chId, item.channelTitle)
                            },
                            onClick = {
                                isInternalNavigation = false
                                currentOnPlayNextVideo(item)
                            }
                        )
                    }

                    if (isLoadingMoreRecs) {
                        item(key = "loading_more_recs", contentType = "loader") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(YouTubeSurface)
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = YouTubeRed,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Memuat...",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        // 3. Floating YouTube Mix Bar (Official YouTube mobile bottom dock)
        if (!isMinimized && !isFullscreen && mixPlaylist.isNotEmpty() && !showMixSheet && !showCommentsSheet && !showDescriptionSheet && !showSettingsSheet) {
            val nextVid = if (currentMixIndex < mixPlaylist.lastIndex) mixPlaylist[currentMixIndex + 1] else mixPlaylist.first()
            FloatingMixBar(
                nextVideo = nextVid,
                mixTitle = mixTitle,
                onClick = {
                    showCommentsSheet = false
                    showDescriptionSheet = false
                    showSettingsSheet = false
                    showMixSheet = true
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset { IntOffset(0, animatedDragOffset.roundToInt()) }
                    .graphicsLayer {
                        alpha = (1f - (animatedDragOffset / 90f) * 1.5f).coerceIn(0f, 1f)
                    }
                    .zIndex(4f)
                    .navigationBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
            )
        }

        // 4. FLOATING MIX QUEUE SHEET (Only in full player mode)
        if (!isMinimized) {
            PlayerMixSheet(
                visible = showMixSheet,
                isFullscreen = isFullscreen,
                mixTitle = mixTitle,
                mixPlaylist = mixPlaylist,
                currentMixIndex = currentMixIndex,
                onVideoSelect = { index -> playFromMix(index) },
                onDismiss = { showMixSheet = false },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(5f)
            )
        }

        // 5. PERSISTENT LIQUID GLASS MINIPLAYER (Animated enter & exit)
        AnimatedVisibility(
            visible = isMinimized,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) + fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 76.dp)
                .zIndex(20f)
        ) {
            PhantomMiniPlayer(
                video = video,
                isPlaying = playerState.isPlaying,
                isBuffering = playerState.isBuffering,
                currentTimeSec = playerState.currentTimeSec,
                durationSec = playerState.durationSec,
                onExpand = onExpand,
                onTogglePlayPause = {
                    if (playerState.isPlaying) controller.pause() else controller.play()
                },
                onClose = onClose
            )
        }

        // 6. Liquid Glass Player Settings Sheet (Speed, Double Tap Seek, Sleep Timer, Repeat, Autoplay, Audio-Only)
        if (!isMinimized) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(25f)
            ) {
                PlayerSettingsSheet(
                    visible = showSettingsSheet,
                    isFullscreen = isFullscreen,
                    currentSpeed = playerState.playbackSpeed,
                    currentQuality = playerState.currentQuality,
                    availableQualities = playerState.availableQualities,
                    doubleTapSeekSeconds = doubleTapSeekSeconds,
                    isLoopEnabled = isLoopEnabled,
                    isAutoplayNext = isAutoplayNext,
                    isAudioOnly = isAudioOnly,
                    sleepTimerRemainingSec = sleepTimerRemainingSec,
                    sleepTimerOption = sleepTimerOption,
                    onDismiss = { showSettingsSheet = false },
                    onSpeedSelected = { speed ->
                        playerState = playerState.copy(playbackSpeed = speed)
                        controller.setPlaybackRate(speed)
                    },
                    onQualitySelected = { quality ->
                        playerState = playerState.copy(currentQuality = quality)
                        controller.setPlaybackQuality(quality)
                    },
                    onDoubleTapSeekSelected = { seconds ->
                        doubleTapSeekSeconds = seconds
                    },
                    onSleepTimerSelected = { option ->
                        sleepTimerOption = option
                        sleepTimerRemainingSec = if (option.seconds != null && option.seconds > 0) option.seconds else null
                    },
                    onLoopToggle = { loop ->
                        isLoopEnabled = loop
                        controller.setLoop(loop)
                    },
                    onAutoplayToggle = { isAutoplayNext = it },
                    onAudioOnlyToggle = { isAudioOnly = it }
                )
            }
        }

        // 7. Deskripsi Bottom Sheet (YouTube standard, non-screen-covering)
        val descTargetChannelId = nextQueueData?.currentVideo?.channelId?.ifBlank { video.channelId } ?: video.channelId
        val descTargetChannelTitle = nextQueueData?.currentVideo?.channelTitle?.ifBlank { video.channelTitle } ?: video.channelTitle
        val descCurrentAvatar = nextQueueData?.currentVideo?.channelAvatarUrl?.ifBlank { activeAvatarUrl } ?: activeAvatarUrl
        val isDescSubscribed by repository.isSubscribed(descTargetChannelId).collectAsState(initial = false)

        PlayerDescriptionSheet(
            visible = showDescriptionSheet && !isMinimized,
            isFullscreen = isFullscreen,
            video = video,
            nextQueueData = nextQueueData,
            activeAvatarUrl = descCurrentAvatar,
            isSubscribed = isDescSubscribed,
            onSubscribeClick = {
                scope.launch {
                    if (isDescSubscribed) {
                        repository.unsubscribe(descTargetChannelId)
                    } else {
                        repository.subscribe(
                            SubscriptionEntity(
                                channelId = descTargetChannelId,
                                channelTitle = descTargetChannelTitle,
                                channelHandle = nextQueueData?.channelHandle ?: "",
                                channelAvatarUrl = descCurrentAvatar,
                                subscriberCountText = nextQueueData?.channelSubscriberCountText ?: ""
                            )
                        )
                    }
                }
            },
            onChannelClick = { chId, chTitle ->
                onMinimize()
                onChannelClick?.invoke(chId, chTitle)
            },
            onDismiss = { showDescriptionSheet = false },
            modifier = Modifier
                .fillMaxSize()
                .zIndex(22f)
        )

        // 8. Komentar Bottom Sheet (YouTube standard, non-screen-covering)
        PlayerCommentsSheet(
            visible = showCommentsSheet && !isMinimized,
            isFullscreen = isFullscreen,
            commentsList = commentsList,
            commentsTotalCountText = commentsTotalCountText,
            isLoadingComments = isLoadingComments,
            hasMoreComments = !commentsContinuationToken.isNullOrBlank(),
            onLoadMore = {
                val token = commentsContinuationToken ?: return@PlayerCommentsSheet
                isLoadingComments = true
                scope.launch {
                    try {
                        val res = repository.getComments(token)
                        if (res.comments.isNotEmpty()) {
                            val existingIds = commentsList.map { it.id }.toSet()
                            val fresh = res.comments.filterNot { it.id in existingIds }
                            commentsList = commentsList + fresh
                        }
                        commentsContinuationToken = res.continuationToken
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        isLoadingComments = false
                    }
                }
            },
            onDismiss = { showCommentsSheet = false },
            modifier = Modifier
                .fillMaxSize()
                .zIndex(22f)
        )
    }
}
