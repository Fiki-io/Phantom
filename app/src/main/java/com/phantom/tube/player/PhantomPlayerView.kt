package com.phantom.tube.player

import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

class PhantomPlayerController(context: Context) {
    var youTubePlayer: YouTubePlayer? = null
        private set
    private var playerView: YouTubePlayerView? = null
    private var isPlayerReady = false
    private var pendingVideoId: String? = null
    private var pendingStartSeconds: Float = 0f
    var currentVideoId: String? = null
        private set

    fun attachView(view: YouTubePlayerView) {
        this.playerView = view
    }

    fun onReady(player: YouTubePlayer) {
        this.youTubePlayer = player
        this.isPlayerReady = true
        val targetId = pendingVideoId
        if (targetId != null) {
            currentVideoId = targetId
            player.loadVideo(targetId, pendingStartSeconds)
            pendingVideoId = null
            pendingStartSeconds = 0f
        }
    }

    fun loadVideo(videoId: String, startSeconds: Float = 0f) {
        val player = youTubePlayer
        if (player != null && isPlayerReady) {
            currentVideoId = videoId
            player.loadVideo(videoId, startSeconds)
        } else {
            pendingVideoId = videoId
            pendingStartSeconds = startSeconds
        }
    }

    fun play() {
        youTubePlayer?.play()
    }

    fun pause() {
        youTubePlayer?.pause()
    }

    fun seekTo(seconds: Float) {
        youTubePlayer?.seekTo(seconds)
    }

    fun setPlaybackRate(rate: Float) {
        val pbRate = when (rate) {
            0.25f -> PlayerConstants.PlaybackRate.RATE_0_25
            0.5f -> PlayerConstants.PlaybackRate.RATE_0_5
            1.5f -> PlayerConstants.PlaybackRate.RATE_1_5
            2.0f -> PlayerConstants.PlaybackRate.RATE_2
            else -> PlayerConstants.PlaybackRate.RATE_1
        }
        youTubePlayer?.setPlaybackRate(pbRate)
    }

    fun release() {
        try {
            playerView?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        playerView = null
        youTubePlayer = null
        isPlayerReady = false
        pendingVideoId = null
        currentVideoId = null
    }
}

@Composable
fun PhantomGhostSurface(
    videoId: String,
    startSeconds: Float = 0f,
    modifier: Modifier = Modifier,
    controller: PhantomPlayerController,
    onCurrentSecond: (Float) -> Unit,
    onVideoDuration: (Float) -> Unit,
    onLoadedFraction: (Float) -> Unit,
    onStateChange: (PlayerConstants.PlayerState) -> Unit,
    onError: (PlayerConstants.PlayerError) -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            YouTubePlayerView(ctx).apply {
                enableAutomaticInitialization = false
                setBackgroundColor(Color.BLACK)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                controller.attachView(this)

                val iFrameOptions = IFramePlayerOptions.Builder()
                    .controls(0)
                    .rel(0)
                    .ivLoadPolicy(3)
                    .ccLoadPolicy(0)
                    .origin("https://${ctx.packageName}")
                    .build()

                initialize(object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        controller.onReady(youTubePlayer)
                        if (controller.currentVideoId == null) {
                            controller.loadVideo(videoId, startSeconds)
                        }
                    }

                    override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                        onCurrentSecond(second)
                    }

                    override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                        onVideoDuration(duration)
                    }

                    override fun onVideoLoadedFraction(youTubePlayer: YouTubePlayer, loadedFraction: Float) {
                        onLoadedFraction(loadedFraction)
                    }

                    override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                        onStateChange(state)
                    }

                    override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                        onError(error)
                    }
                }, iFrameOptions)
            }
        },
        update = { /* controller maintains internal state */ }
    )
}
