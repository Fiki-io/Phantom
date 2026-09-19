package com.phantom.tube.data.model

data class FeedResult(
    val videos: List<VideoItem> = emptyList(),
    val continuationToken: String? = null
)
