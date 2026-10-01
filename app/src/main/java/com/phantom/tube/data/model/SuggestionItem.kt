package com.phantom.tube.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class SuggestionItem(
    val query: String,
    val channelId: String? = null,
    val channelTitle: String? = null,
    val channelHandle: String? = null,
    val channelAvatarUrl: String? = null
) {
    val isChannel: Boolean get() = !channelId.isNullOrBlank()
}
