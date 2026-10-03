package com.phantom.tube.player.offline

import android.content.Context
import android.content.Intent
import com.phantom.tube.core.database.DownloadEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manajer terpusat untuk pemutaran audio offline (MP3).
 * Mengatur antrean playlist, lagu aktif, status play/pause, durasi, dan posisi putar.
 * Berkoordinasi langsung dengan [PhantomOfflineAudioService] untuk pemutaran latar belakang.
 */
object OfflineAudioPlayerManager {

    private val _currentTrack = MutableStateFlow<DownloadEntity?>(null)
    val currentTrack: StateFlow<DownloadEntity?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _queue = MutableStateFlow<List<DownloadEntity>>(emptyList())
    val queue: StateFlow<List<DownloadEntity>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isLooping = MutableStateFlow(false)
    val isLooping: StateFlow<Boolean> = _isLooping.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    // Internal state updates called by PhantomOfflineAudioService
    internal fun updateTrack(track: DownloadEntity?, index: Int) {
        _currentTrack.value = track
        _currentIndex.value = index
    }

    internal fun updatePlaybackState(playing: Boolean) {
        _isPlaying.value = playing
    }

    internal fun updatePosition(positionMs: Long) {
        _currentPositionMs.value = positionMs
    }

    internal fun updateDuration(duration: Long) {
        _durationMs.value = duration
    }

    internal fun setQueueList(list: List<DownloadEntity>, startIndex: Int) {
        _queue.value = list
        _currentIndex.value = startIndex
        if (list.isNotEmpty() && startIndex in list.indices) {
            _currentTrack.value = list[startIndex]
        }
    }

    // Public controller commands
    fun playQueue(context: Context, tracks: List<DownloadEntity>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val clampedIndex = startIndex.coerceIn(0, tracks.lastIndex)
        _queue.value = tracks
        _currentIndex.value = clampedIndex
        _currentTrack.value = tracks[clampedIndex]

        val intent = Intent(context, PhantomOfflineAudioService::class.java).apply {
            action = PhantomOfflineAudioService.ACTION_PLAY_QUEUE
            putExtra(PhantomOfflineAudioService.EXTRA_INDEX, clampedIndex)
        }
        startServiceCompat(context, intent)
    }

    fun playTrack(context: Context, track: DownloadEntity) {
        val currentList = _queue.value
        val existingIndex = currentList.indexOfFirst { it.id == track.id }
        if (existingIndex != -1) {
            playQueue(context, currentList, existingIndex)
        } else {
            playQueue(context, listOf(track), 0)
        }
    }

    fun togglePlayPause(context: Context) {
        val intent = Intent(context, PhantomOfflineAudioService::class.java).apply {
            action = PhantomOfflineAudioService.ACTION_TOGGLE_PLAY_PAUSE
        }
        startServiceCompat(context, intent)
    }

    fun playNext(context: Context) {
        val intent = Intent(context, PhantomOfflineAudioService::class.java).apply {
            action = PhantomOfflineAudioService.ACTION_NEXT
        }
        startServiceCompat(context, intent)
    }

    fun playPrevious(context: Context) {
        val intent = Intent(context, PhantomOfflineAudioService::class.java).apply {
            action = PhantomOfflineAudioService.ACTION_PREVIOUS
        }
        startServiceCompat(context, intent)
    }

    fun seekTo(context: Context, positionMs: Long) {
        _currentPositionMs.value = positionMs
        val intent = Intent(context, PhantomOfflineAudioService::class.java).apply {
            action = PhantomOfflineAudioService.ACTION_SEEK
            putExtra(PhantomOfflineAudioService.EXTRA_POSITION_MS, positionMs)
        }
        startServiceCompat(context, intent)
    }

    fun toggleLoop() {
        _isLooping.value = !_isLooping.value
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun stop(context: Context) {
        _isPlaying.value = false
        _currentTrack.value = null
        _queue.value = emptyList()
        try {
            context.stopService(Intent(context, PhantomOfflineAudioService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startServiceCompat(context: Context, intent: Intent) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
