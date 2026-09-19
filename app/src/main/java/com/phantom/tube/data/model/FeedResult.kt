package com.phantom.tube.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class FeedResult(
    val videos: List<VideoItem> = emptyList(),
    val continuationToken: String? = null
)
