package com.phantom.tube.data.repository

import com.phantom.tube.core.database.FavoriteDao
import com.phantom.tube.core.database.FavoriteEntity
import com.phantom.tube.core.database.SearchHistoryDao
import com.phantom.tube.core.database.SearchHistoryEntity
import com.phantom.tube.core.database.WatchHistoryDao
import com.phantom.tube.core.database.WatchHistoryEntity
import com.phantom.tube.data.innertube.InnerTubeClient
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
    private val searchHistoryDao: SearchHistoryDao
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

    suspend fun getSuggestions(query: String): List<String> {
        return innerTubeClient.fetchSuggestions(query)
    }

    suspend fun getWatchNext(videoId: String, playlistId: String? = null): NextQueue? {
        return innerTubeClient.fetchWatchNext(videoId, playlistId)
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
        return watchHistoryDao.getLastPosition(videoId) ?: 0L
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
}
