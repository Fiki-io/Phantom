package com.phantom.tube.player.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import com.phantom.tube.MainActivity
import com.phantom.tube.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class PhantomMediaService : Service() {

    private val binder = LocalBinder()
    private var mediaSession: MediaSessionCompat? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentTitle: String = "Phantom Player"
    private var currentChannel: String = ""
    private var isPlaying: Boolean = false

    private var currentDurationMs: Long = 0L

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var currentBitmap: Bitmap? = null
    private var currentThumbnailUrl: String = ""
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    var onPlayAction: (() -> Unit)? = null
    var onPauseAction: (() -> Unit)? = null
    var onNextAction: (() -> Unit)? = null
    var onPreviousAction: (() -> Unit)? = null
    var onSeekAction: ((Long) -> Unit)? = null

    companion object {
        const val CHANNEL_ID = "phantom_media_playback"
        const val NOTIFICATION_ID = 1001
        const val ACTION_PLAY = "com.phantom.tube.ACTION_PLAY"
        const val ACTION_PAUSE = "com.phantom.tube.ACTION_PAUSE"
        const val ACTION_NEXT = "com.phantom.tube.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.phantom.tube.ACTION_PREVIOUS"
        const val ACTION_STOP = "com.phantom.tube.ACTION_STOP"
    }

    inner class LocalBinder : Binder() {
        fun getService(): PhantomMediaService = this@PhantomMediaService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupMediaSession()
        startForegroundCompat(buildNotification())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Phantom Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Playback controls and notifications for Phantom"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "PhantomMediaSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    onPlayAction?.invoke()
                }

                override fun onPause() {
                    onPauseAction?.invoke()
                }

                override fun onSkipToNext() {
                    onNextAction?.invoke()
                }

                override fun onSkipToPrevious() {
                    onPreviousAction?.invoke()
                }

                override fun onSeekTo(pos: Long) {
                    onSeekAction?.invoke(pos)
                }
            })
            isActive = true
        }
    }

    fun updateMediaInfo(
        title: String,
        channel: String,
        durationMs: Long = 0L,
        playing: Boolean,
        thumbnailUrl: String = ""
    ) {
        currentTitle = title
        currentChannel = channel
        isPlaying = playing
        if (durationMs > 0L) {
            currentDurationMs = durationMs
        }

        if (thumbnailUrl.isNotBlank() && thumbnailUrl != currentThumbnailUrl) {
            currentThumbnailUrl = thumbnailUrl
            currentBitmap = null
            loadThumbnail(thumbnailUrl)
        } else {
            applyMetadata(currentBitmap)
            updatePlaybackState(playing)
            startForegroundCompat(buildNotification())
        }
    }

    private fun loadThumbnail(url: String) {
        applyMetadata(null)
        updatePlaybackState(isPlaying)
        startForegroundCompat(buildNotification())

        serviceScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.byteStream()?.use { stream ->
                        val bitmap = BitmapFactory.decodeStream(stream)
                        if (bitmap != null) {
                            withContext(Dispatchers.Main) {
                                if (currentThumbnailUrl == url) {
                                    currentBitmap = bitmap
                                    applyMetadata(bitmap)
                                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                    notificationManager.notify(NOTIFICATION_ID, buildNotification())
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun applyMetadata(bitmap: Bitmap?) {
        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentChannel)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentDurationMs)

        if (bitmap != null) {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap)
        }
        mediaSession?.setMetadata(metadataBuilder.build())
    }

    fun updatePlaybackState(playing: Boolean, currentPositionMs: Long = 0L) {
        isPlaying = playing
        if (playing) {
            acquireWakeLock()
        } else {
            releaseWakeLock()
        }

        val state = if (playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO
            )
            .setState(state, currentPositionMs, 1.0f)
            .build()
        mediaSession?.setPlaybackState(playbackState)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun startForegroundCompat(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_media_previous, "Previous",
            getServicePendingIntent(ACTION_PREVIOUS)
        ).build()

        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_pause, "Pause",
                getServicePendingIntent(ACTION_PAUSE)
            ).build()
        } else {
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_play, "Play",
                getServicePendingIntent(ACTION_PLAY)
            ).build()
        }

        val nextAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_media_next, "Next",
            getServicePendingIntent(ACTION_NEXT)
        ).build()

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentTitle)
            .setContentText(currentChannel)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setOngoing(isPlaying)

        if (currentBitmap != null) {
            builder.setLargeIcon(currentBitmap)
        }

        return builder.build()
    }

    private fun getServicePendingIntent(action: String): PendingIntent {
        val intent = Intent(this, PhantomMediaService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Phantom:PlaybackWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(3 * 60 * 60 * 1000L) // 3 hours safety timeout
            }
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        wakeLock = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> onPlayAction?.invoke()
            ACTION_PAUSE -> onPauseAction?.invoke()
            ACTION_NEXT -> onNextAction?.invoke()
            ACTION_PREVIOUS -> onPreviousAction?.invoke()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        currentBitmap = null
        releaseWakeLock()
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
