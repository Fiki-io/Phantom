package com.phantom.tube.data.innertube.cache

import com.phantom.tube.data.innertube.InnerTubeClient
import com.phantom.tube.data.innertube.parser.InnerTubeHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance in-memory cache and background resolver for YouTube channel avatars.
 * Guarantees that video cards in Home, Search, Channel, and Related feeds always display
 * the authentic channel logo even when raw feed snippets omit the avatar payload.
 */
object ChannelAvatarCache {

    private val cacheById = ConcurrentHashMap<String, String>()
    private val cacheByTitle = ConcurrentHashMap<String, String>()
    private val inFlight = ConcurrentHashMap.newKeySet<String>()

    private var client: InnerTubeClient? = null

    fun init(innerTubeClient: InnerTubeClient) {
        client = innerTubeClient
    }

    fun put(channelId: String?, channelTitle: String?, avatarUrl: String?) {
        val clean = InnerTubeHelpers.normalizeUrl(avatarUrl)
        if (clean.isBlank()) return

        if (!channelId.isNullOrBlank()) {
            cacheById[channelId] = clean
        }
        if (!channelTitle.isNullOrBlank()) {
            val key = channelTitle.trim().lowercase()
            if (key.isNotBlank()) {
                cacheByTitle[key] = clean
            }
        }
    }

    fun get(channelId: String?, channelTitle: String?): String {
        if (!channelId.isNullOrBlank()) {
            cacheById[channelId]?.let { if (it.isNotBlank()) return it }
        }
        if (!channelTitle.isNullOrBlank()) {
            val key = channelTitle.trim().lowercase()
            if (key.isNotBlank()) {
                cacheByTitle[key]?.let { if (it.isNotBlank()) return it }
            }
        }
        return ""
    }

    suspend fun resolveAvatar(channelId: String, channelTitle: String): String? = withContext(Dispatchers.IO) {
        val cached = get(channelId, channelTitle)
        if (cached.isNotBlank()) return@withContext cached

        if (channelId.isBlank() || !channelId.startsWith("UC")) {
            return@withContext null
        }

        val c = client ?: return@withContext null
        if (!inFlight.add(channelId)) {
            return@withContext get(channelId, channelTitle).ifBlank { null }
        }

        try {
            val profile = c.fetchChannel(channelId = channelId)
            val avatar = InnerTubeHelpers.normalizeUrl(profile?.avatarUrl)
            if (avatar.isNotBlank()) {
                put(channelId, channelTitle, avatar)
                val profTitle = profile?.title
                if (!profTitle.isNullOrBlank()) {
                    put(channelId, profTitle, avatar)
                }
                return@withContext avatar
            }
        } catch (e: Exception) {
            // Silently ignore network hiccups; avoid crashing UI
        } finally {
            inFlight.remove(channelId)
        }

        return@withContext get(channelId, channelTitle).ifBlank { null }
    }
}
