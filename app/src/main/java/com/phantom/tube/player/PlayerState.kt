package com.phantom.tube.player

data class PlayerState(
    val videoId: String = "",
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isEnded: Boolean = false,
    val currentTimeSec: Float = 0f,
    val durationSec: Float = 0f,
    val bufferedFraction: Float = 0f,
    val playbackSpeed: Float = 1.0f,
    val currentQuality: String = "auto",
    val errorCode: String? = null
) {
    val progressFraction: Float
        get() = if (durationSec > 0f) (currentTimeSec / durationSec).coerceIn(0f, 1f) else 0f

    val formattedCurrentTime: String
        get() = formatSeconds(currentTimeSec.toLong())

    val formattedDuration: String
        get() = formatSeconds(durationSec.toLong())

    private fun formatSeconds(totalSec: Long): String {
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
