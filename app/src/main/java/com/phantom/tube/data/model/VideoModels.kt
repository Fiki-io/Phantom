package com.phantom.tube.data.model

data class VideoItem(
    val id: String,
    val title: String,
    val channelTitle: String,
    val channelId: String = "",
    val thumbnailUrl: String,
    val channelAvatarUrl: String = "",
    val viewCountText: String = "",
    val publishedTimeText: String = "",
    val durationText: String = "",
    val durationSeconds: Long = 0L
)

data class VideoDetail(
    val id: String,
    val title: String,
    val description: String = "",
    val channelTitle: String = "",
    val channelId: String = "",
    val channelAvatarUrl: String = "",
    val viewCountText: String = "",
    val likeCountText: String = "",
    val subscriberCountText: String = "",
    val publishedDate: String = "",
    val relatedVideos: List<VideoItem> = emptyList()
)

data class NextQueue(
    val currentVideo: VideoItem,
    val mixPlaylist: List<VideoItem> = emptyList(),
    val recommendations: List<VideoItem> = emptyList(),
    val playlistTitle: String = "",
    val currentIndex: Int = 0
) {
    val upNext: List<VideoItem> get() = if (currentIndex < mixPlaylist.lastIndex) mixPlaylist.subList(currentIndex + 1, mixPlaylist.size) else emptyList()
    val mixQueue: List<VideoItem> get() = upNext
}

data class SponsorSegment(
    val category: String,
    val startSecond: Float,
    val endSecond: Float,
    val uuid: String = ""
) {
    val durationSeconds: Float get() = (endSecond - startSecond).coerceAtLeast(0f)
}
