package com.phantom.tube.data.repository

import com.phantom.tube.core.database.FavoriteDao
import com.phantom.tube.core.database.FavoriteEntity
import com.phantom.tube.core.database.SearchHistoryDao
import com.phantom.tube.core.database.SearchHistoryEntity
import com.phantom.tube.core.database.SubscriptionDao
import com.phantom.tube.core.database.SubscriptionEntity
import com.phantom.tube.core.database.WatchHistoryDao
import com.phantom.tube.core.database.WatchHistoryEntity
import com.phantom.tube.data.innertube.InnerTubeClient
import com.phantom.tube.data.model.ChannelProfile
import com.phantom.tube.data.model.CommentsResult
import com.phantom.tube.data.model.FeedResult
import com.phantom.tube.data.model.NextQueue
import com.phantom.tube.data.model.SponsorSegment
import com.phantom.tube.data.model.VideoItem
import com.phantom.tube.data.sponsorblock.SponsorBlockClient
import kotlinx.coroutines.flow.Flow

class PhantomRepository(
    private val innerTubeClient: InnerTubeClient = InnerTubeClient(),
    private val sponsorBlockClient: SponsorBlockClient = SponsorBlockClient(),
    private val watchHistoryDao: WatchHistoryDao,
    private val favoriteDao: FavoriteDao,
    private val searchHistoryDao: SearchHistoryDao,
    private val subscriptionDao: SubscriptionDao
) {
    suspend fun getHomeRecommendations(
        historyIndex: Int = 0,
        continuation: String? = null
    ): FeedResult {
        // If we have a direct continuation token from YouTube, load next page
        if (!continuation.isNullOrBlank() && !continuation.startsWith("history_")) {
            val pageResult = innerTubeClient.fetchFeedPage(continuation = continuation)
            if (pageResult.videos.isNotEmpty()) {
                return pageResult
            }
        }

        // Check local watch history for personalized YouTube algorithmic recommendations
        val recentWatched = watchHistoryDao.getRecentWatched(limit = 10)
        if (recentWatched.isNotEmpty() && historyIndex < recentWatched.size) {
            val targetVideo = recentWatched[historyIndex]
            val nextQueue = innerTubeClient.fetchWatchNext(targetVideo.videoId)
            val recs = nextQueue?.recommendations ?: emptyList()
            if (recs.isNotEmpty()) {
                val nextToken = if (historyIndex + 1 < recentWatched.size) {
                    "history_${historyIndex + 1}"
                } else {
                    null
                }
                return FeedResult(videos = recs, continuationToken = nextToken)
            }
        }

        // Fallback for new users (no watch history yet) or end of history:
        return innerTubeClient.fetchFeedPage(query = "trending indonesia", continuation = continuation)
    }

    suspend fun getFeedPage(query: String? = null, continuation: String? = null): FeedResult {
        return innerTubeClient.fetchFeedPage(query, continuation)
    }

    suspend fun getFeed(query: String = "trending"): List<VideoItem> {
        return innerTubeClient.fetchFeed(query)
    }

    suspend fun search(query: String): List<VideoItem> {
        return innerTubeClient.search(query)
    }

    suspend fun searchPage(query: String? = null, continuation: String? = null): FeedResult {
        return innerTubeClient.searchPage(query, continuation)
    }

    suspend fun getSuggestions(query: String): List<String> {
        return innerTubeClient.fetchSuggestions(query)
    }

    suspend fun getWatchNext(videoId: String, playlistId: String? = null): NextQueue? {
        return innerTubeClient.fetchWatchNext(videoId, playlistId)
    }

    suspend fun getComments(continuationToken: String): CommentsResult {
        return innerTubeClient.fetchComments(continuationToken)
    }

    suspend fun getMoreRecommendations(
        video: VideoItem,
        continuation: String? = null
    ): FeedResult {
        // 1. Try YouTube official watch next continuation if available
        if (!continuation.isNullOrBlank() && !continuation.startsWith("search_fallback_")) {
            val result = innerTubeClient.fetchWatchNextContinuation(continuation, currentVideoId = video.id)
            if (result.videos.isNotEmpty()) {
                return result
            }
        }

        // 2. Intelligent related content fallback
        val searchQuery = "${video.title} ${video.channelTitle}".trim().ifBlank { video.title }
        val searchContinuation = if (continuation?.startsWith("search_fallback_") == true) {
            continuation.removePrefix("search_fallback_")
        } else null

        val feedResult = innerTubeClient.fetchFeedPage(query = searchQuery, continuation = searchContinuation)
        val filtered = feedResult.videos.filter { it.id != video.id }
        val nextToken = feedResult.continuationToken?.let { "search_fallback_$it" }
        return FeedResult(videos = filtered, continuationToken = nextToken)
    }

    suspend fun getSponsorSegments(videoId: String): List<SponsorSegment> {
        return sponsorBlockClient.getSkipSegments(videoId)
    }

    // Search History
    fun getSearchHistory(limit: Int = 25): Flow<List<SearchHistoryEntity>> {
        return searchHistoryDao.getRecentHistory(limit)
    }

    suspend fun saveSearchQuery(query: String) {
        if (query.isNotBlank()) {
            searchHistoryDao.insertOrUpdate(SearchHistoryEntity(query = query.trim()))
        }
    }

    suspend fun deleteSearchQuery(query: String) {
        searchHistoryDao.delete(query)
    }

    suspend fun clearSearchHistory() {
        searchHistoryDao.clearAll()
    }

    // Watch History
    fun getWatchHistory(): Flow<List<WatchHistoryEntity>> {
        return watchHistoryDao.getAllHistory()
    }

    suspend fun recordWatch(
        video: VideoItem,
        positionMs: Long = 0L,
        durationMs: Long = 0L
    ) {
        watchHistoryDao.insertOrUpdate(
            WatchHistoryEntity(
                videoId = video.id,
                title = video.title,
                channelTitle = video.channelTitle,
                channelId = video.channelId,
                thumbnailUrl = video.thumbnailUrl,
                durationText = video.durationText,
                lastPositionMs = positionMs,
                durationMs = durationMs,
                watchedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getLastPosition(videoId: String): Long {
        val entry = watchHistoryDao.getEntry(videoId) ?: return 0L
        val duration = entry.durationMs
        val lastPos = entry.lastPositionMs
        // If video was finished, or within 5s of end, or >= 95% watched, or < 4s: start fresh at 0
        if (duration > 0L) {
            if (lastPos >= duration - 5000L || (lastPos.toFloat() / duration.toFloat()) >= 0.95f) {
                return 0L
            }
        }
        if (lastPos < 4000L) {
            return 0L
        }
        return lastPos
    }

    suspend fun resetWatchPosition(videoId: String) {
        watchHistoryDao.getEntry(videoId)?.let { entry ->
            watchHistoryDao.insertOrUpdate(entry.copy(lastPositionMs = 0L))
        }
    }

    suspend fun deleteHistoryItem(videoId: String) {
        watchHistoryDao.delete(videoId)
    }

    suspend fun clearHistory() {
        watchHistoryDao.clearAll()
    }

    // Favorites
    fun getFavorites(): Flow<List<FavoriteEntity>> {
        return favoriteDao.getAllFavorites()
    }

    fun isFavorite(videoId: String): Flow<Boolean> {
        return favoriteDao.isFavorite(videoId)
    }

    suspend fun toggleFavorite(video: VideoItem, isFav: Boolean) {
        if (isFav) {
            favoriteDao.delete(video.id)
        } else {
            favoriteDao.insert(
                FavoriteEntity(
                    videoId = video.id,
                    title = video.title,
                    channelTitle = video.channelTitle,
                    thumbnailUrl = video.thumbnailUrl,
                    durationText = video.durationText
                )
            )
        }
    }

    // Subscriptions
    fun getSubscriptions(): Flow<List<SubscriptionEntity>> {
        return subscriptionDao.getAllSubscriptions()
    }

    fun isSubscribed(channelId: String): Flow<Boolean> {
        return subscriptionDao.isSubscribed(channelId)
    }

    suspend fun subscribe(subscription: SubscriptionEntity) {
        subscriptionDao.insert(subscription)
    }

    suspend fun unsubscribe(channelId: String) {
        subscriptionDao.delete(channelId)
    }

    // Channel profile & videos
    suspend fun getChannel(
        channelId: String,
        params: String? = null,
        continuation: String? = null
    ): ChannelProfile? {
        return innerTubeClient.fetchChannel(channelId = channelId, params = params, continuation = continuation)
    }

    suspend fun getChannelVideosContinuation(continuation: String): FeedResult {
        return innerTubeClient.fetchChannelVideosContinuation(continuation)
    }
}
