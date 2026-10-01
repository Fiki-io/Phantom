package com.phantom.tube

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.phantom.tube.core.crash.PhantomCrashHandler
import com.phantom.tube.core.database.PhantomDatabase
import com.phantom.tube.core.security.PhantomNative
import com.phantom.tube.data.innertube.InnerTubeClient
import com.phantom.tube.data.repository.PhantomRepository
import com.phantom.tube.data.sponsorblock.SponsorBlockClient
import com.phantom.tube.data.settings.PhantomPreferences
import com.phantom.tube.data.innertube.cache.ChannelAvatarCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class PhantomApp : Application(), ImageLoaderFactory {

    lateinit var database: PhantomDatabase
        private set

    lateinit var preferences: PhantomPreferences
        private set

    lateinit var repository: PhantomRepository
        private set

    val sharedHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool(16, 5, TimeUnit.MINUTES))
            .cache(Cache(cacheDir.resolve("http_cache"), 40L * 1024 * 1024))
            .build()
    }

    val updateManager: com.phantom.tube.core.update.UpdateManager by lazy {
        com.phantom.tube.core.update.UpdateManager(this, sharedHttpClient)
    }

    override fun onCreate() {
        super.onCreate()
        PhantomCrashHandler.install(this)
        PhantomNative.verifySecurity(this)
        database = PhantomDatabase.getInstance(this)
        preferences = PhantomPreferences(this)
        val innerTube = InnerTubeClient(httpClient = sharedHttpClient)
        ChannelAvatarCache.init(innerTube)
        repository = PhantomRepository(
            innerTubeClient = innerTube,
            sponsorBlockClient = SponsorBlockClient(httpClient = sharedHttpClient),
            watchHistoryDao = database.watchHistoryDao(),
            favoriteDao = database.favoriteDao(),
            searchHistoryDao = database.searchHistoryDao(),
            subscriptionDao = database.subscriptionDao(),
            preferences = preferences
        )

        // Preload avatar cache with subscribed channels from local DB
        CoroutineScope(Dispatchers.IO).launch {
            try {
                database.subscriptionDao().getAllSubscriptions().collect { subs ->
                    subs.forEach { sub ->
                        if (sub.channelAvatarUrl.isNotBlank()) {
                            ChannelAvatarCache.put(
                                sub.channelId,
                                sub.channelTitle,
                                sub.channelAvatarUrl
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore background DB error
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(sharedHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(80L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false) // Aggressive disk caching for smooth thumbnail scrolling
            .allowHardware(true)        // Direct GPU rendering of bitmap textures
            .crossfade(150)             // Fast and smooth crossfade
            .build()
    }
}
