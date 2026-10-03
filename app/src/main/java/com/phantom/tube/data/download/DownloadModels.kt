package com.phantom.tube.data.download

data class AvailableQuality(
    val id: String,
    val label: String,       // e.g. "1080p Full HD", "720p HD", "320 kbps"
    val sizeDescription: String,  // e.g. "MP4 • Full HD", "MP3 • Studio Quality"
    val formatType: String,  // "video" or "audio"
    val qualityValue: String // "1080", "720", "360", "240", "144", "320", "256", "128"
)

data class DownloadResult(
    val url: String,
    val filename: String,
    val mimeType: String
)

sealed class DownloadState {
    object Idle : DownloadState()
    data class Converting(val qualityLabel: String) : DownloadState()
    data class Downloading(
        val qualityLabel: String,
        val percent: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : DownloadState()
    data class Success(val fileName: String, val filePath: String, val fileUri: String) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

data class DownloadedFileInfo(
    val fileName: String,
    val fileUri: String,
    val filePath: String,
    val fileSize: Long,
    val mimeType: String
)
