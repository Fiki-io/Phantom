package com.phantom.tube.data.innertube

import com.phantom.tube.data.model.NextQueue
import com.phantom.tube.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttp
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

    suspend fun fetchFeed(query: String = "trending"): List<VideoItem> = withContext(Dispatchers.IO) {
        return@withContext search(query)
    }

    suspend fun search(query: String): List<VideoItem> = withContext(Dispatchers.IO) {
        try {
            val bodyJson = JSONObject().apply {
                put("context", createClientContext())
                put("query", query)
            }
            val request = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/search?prettyPrint=false")
                .header("Content-Type", "application/json")
                .header("User-Agent", userAgent)
                .post(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext emptyList()
            return@withContext InnerTubeParser.parseSearchResults(responseBody)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
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

    suspend fun fetchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder()
                .url("https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&hl=id&gl=ID&q=$encoded")
                .header("User-Agent", userAgent)
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext emptyList()
            return@withContext InnerTubeParser.parseSuggestions(responseBody)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }
}
