package com.phantom.tube

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.phantom.tube.core.crash.PhantomCrashHandler
import com.phantom.tube.core.database.PhantomDatabase
import com.phantom.tube.data.innertube.InnerTubeClient
import com.phantom.tube.data.repository.PhantomRepository
import com.phantom.tube.data.sponsorblock.SponsorBlockClient
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class PhantomApp : Application(), ImageLoaderFactory {

    lateinit var database: PhantomDatabase
        private set

    lateinit var repository: PhantomRepository
        private set

    val sharedHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        PhantomCrashHandler.install(this)
        database = PhantomDatabase.getInstance(this)
        repository = PhantomRepository(
            innerTubeClient = InnerTubeClient(httpClient = sharedHttpClient),
            sponsorBlockClient = SponsorBlockClient(httpClient = sharedHttpClient),
            watchHistoryDao = database.watchHistoryDao(),
            favoriteDao = database.favoriteDao(),
            searchHistoryDao = database.searchHistoryDao()
        )
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
                    .maxSizeBytes(50L * 1024 * 1024) // 50 MB disk cache
                    .build()
            }
            .crossfade(true)
            .build()
    }
}

