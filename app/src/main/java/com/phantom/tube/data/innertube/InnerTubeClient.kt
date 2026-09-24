package com.phantom.tube.data.innertube

import com.phantom.tube.data.model.ChannelProfile
import com.phantom.tube.data.model.FeedResult
import com.phantom.tube.data.model.NextQueue
import com.phantom.tube.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class InnerTubeClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private fun createClientContext(hl: String = "id", gl: String = "ID"): JSONObject {
        val client = JSONObject().apply {
            put("clientName", "WEB")
            put("clientVersion", "2.20240901.00.00")
            put("hl", hl)
            put("gl", gl)
        }
        return JSONObject().apply {
            put("client", client)
        }
    }

    suspend fun fetchFeedPage(
        query: String? = null,
        continuation: String? = null
    ): FeedResult = withContext(Dispatchers.IO) {
        try {
            val bodyJson = JSONObject().apply {
                put("context", createClientContext())
                if (!continuation.isNullOrBlank()) {
                    put("continuation", continuation)
                } else {
                    put("query", query ?: "trending indonesia")
                }
            }
            val request = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/search?prettyPrint=false")
                .header("Content-Type", "application/json")
                .header("User-Agent", userAgent)
                .post(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext FeedResult()
            return@withContext InnerTubeParser.parseFeedWithContinuation(responseBody)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext FeedResult()
        }
    }

    suspend fun fetchFeed(query: String = "trending"): List<VideoItem> = withContext(Dispatchers.IO) {
        return@withContext fetchFeedPage(query = query).videos
    }

    suspend fun search(query: String): List<VideoItem> = withContext(Dispatchers.IO) {
        return@withContext fetchFeedPage(query = query).videos
    }

    suspend fun searchPage(
        query: String? = null,
        continuation: String? = null
    ): FeedResult = withContext(Dispatchers.IO) {
        return@withContext fetchFeedPage(query = query, continuation = continuation)
    }

    suspend fun fetchWatchNext(videoId: String, playlistId: String? = null): NextQueue? = withContext(Dispatchers.IO) {
        try {
            val bodyJson = JSONObject().apply {
                put("context", createClientContext())
                put("videoId", videoId)
                val targetPlaylistId = if (!playlistId.isNullOrBlank()) playlistId else "RD$videoId"
                put("playlistId", targetPlaylistId)
            }
            val request = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/next?prettyPrint=false")
                .header("Content-Type", "application/json")
                .header("User-Agent", userAgent)
                .post(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext null
            return@withContext InnerTubeParser.parseWatchNext(responseBody, videoId)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun fetchWatchNextContinuation(
        continuation: String,
        currentVideoId: String = ""
    ): FeedResult = withContext(Dispatchers.IO) {
        try {
            val bodyJson = JSONObject().apply {
                put("context", createClientContext())
                put("continuation", continuation)
            }
            val request = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/next?prettyPrint=false")
                .header("Content-Type", "application/json")
                .header("User-Agent", userAgent)
                .post(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext FeedResult()
            return@withContext InnerTubeParser.parseWatchNextContinuation(responseBody, currentVideoId)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext FeedResult()
        }
    }

    suspend fun fetchChannel(
        channelId: String,
        params: String? = null,
        continuation: String? = null
    ): ChannelProfile? = withContext(Dispatchers.IO) {
        try {
            var targetChannelId = channelId.trim()
            // If channelId is not a standard UC id and not a continuation, resolve by search
            if (continuation.isNullOrBlank() && !targetChannelId.startsWith("UC") && targetChannelId.isNotBlank()) {
                val searchRes = fetchFeedPage(query = targetChannelId)
                val foundId = searchRes.videos.firstOrNull { it.channelId.startsWith("UC") }?.channelId
                if (!foundId.isNullOrBlank()) {
                    targetChannelId = foundId
                }
            }

            val bodyJson = JSONObject().apply {
                put("context", createClientContext())
                if (!continuation.isNullOrBlank()) {
                    put("continuation", continuation)
                } else {
                    put("browseId", targetChannelId)
                    if (!params.isNullOrBlank()) {
                        put("params", params)
                    }
                }
            }

            val request = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/browse?prettyPrint=false")
                .header("Content-Type", "application/json")
                .header("User-Agent", userAgent)
                .post(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext null
            return@withContext InnerTubeParser.parseChannelPage(responseBody, targetChannelId)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun fetchChannelVideosContinuation(
        continuation: String
    ): FeedResult = withContext(Dispatchers.IO) {
        try {
            val bodyJson = JSONObject().apply {
                put("context", createClientContext())
                put("continuation", continuation)
            }

            val request = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/browse?prettyPrint=false")
                .header("Content-Type", "application/json")
                .header("User-Agent", userAgent)
                .post(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext FeedResult()
            return@withContext InnerTubeParser.parseChannelContinuation(responseBody)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext FeedResult()
        }
    }

    suspend fun fetchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()
        try {
            val encoded = java.net.URLEncoder.encode(trimmed, "UTF-8")

            // Endpoints list to try in order:
            // 1. YouTube firefox client (clean JSON array, high speed, YouTube specific)
            // 2. YouTube official client (JSONP window.google.ac.h format)
            // 3. Google suggestion fallback (client=firefox, ds=yt)
            val endpoints = listOf(
                "https://suggestqueries-clients6.youtube.com/complete/search?client=firefox&ds=yt&hl=id&gl=ID&q=$encoded",
                "https://suggestqueries-clients6.youtube.com/complete/search?client=youtube&ds=yt&hl=id&gl=ID&q=$encoded",
                "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&hl=id&gl=ID&q=$encoded"
            )

            for (url in endpoints) {
                try {
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", userAgent)
                        .header("Accept", "*/*")
                        .get()
                        .build()

                    val response = httpClient.newCall(request).execute()
                    response.use { resp ->
                        if (resp.isSuccessful) {
                            val bodyBytes = resp.body?.bytes()
                            if (bodyBytes != null && bodyBytes.isNotEmpty()) {
                                val parsed = InnerTubeParser.parseSuggestions(String(bodyBytes, Charsets.UTF_8))
                                if (parsed.isNotEmpty()) {
                                    return@withContext parsed
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Try next fallback endpoint
                }
            }

            emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
