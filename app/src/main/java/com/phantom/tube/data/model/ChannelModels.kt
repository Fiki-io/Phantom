package com.phantom.tube.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class ChannelSortChip(
    val title: String,
    val continuationToken: String,
    val isSelected: Boolean = false
)

@Immutable
data class ChannelProfile(
    val id: String,
    val title: String,
    val handle: String = "",
    val avatarUrl: String = "",
    val bannerUrl: String = "",
    val subscriberCountText: String = "",
    val videoCountText: String = "",
    val description: String = "",
    val externalLinksText: String = "",
    val isVerified: Boolean = false,
    val featuredVideo: VideoItem? = null,
    val homeVideos: List<VideoItem> = emptyList(),
    val videos: List<VideoItem> = emptyList(),
    val continuationToken: String? = null,
    val videoTabParams: String? = null,
    val sortChips: List<ChannelSortChip> = emptyList()
)
