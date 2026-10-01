package com.phantom.tube.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class SearchChannelItem(
    val id: String,
    val title: String,
    val handle: String = "",
    val avatarUrl: String = "",
    val subscriberCountText: String = "",
    val videoCountText: String = "",
    val description: String = "",
    val isVerified: Boolean = false
)
