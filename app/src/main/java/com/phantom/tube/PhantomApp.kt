package com.phantom.tube

import android.app.Application
import com.phantom.tube.core.crash.PhantomCrashHandler
import com.phantom.tube.core.database.PhantomDatabase
import com.phantom.tube.data.innertube.InnerTubeClient
import com.phantom.tube.data.repository.PhantomRepository
import com.phantom.tube.data.sponsorblock.SponsorBlockClient

class PhantomApp : Application() {

    lateinit var database: PhantomDatabase
        private set

    lateinit var repository: PhantomRepository
        private set

    override fun onCreate() {
        super.onCreate()
        PhantomCrashHandler.install(this)
        database = PhantomDatabase.getInstance(this)
        repository = PhantomRepository(
            innerTubeClient = InnerTubeClient(),
            sponsorBlockClient = SponsorBlockClient(),
            watchHistoryDao = database.watchHistoryDao(),
            favoriteDao = database.favoriteDao(),
            searchHistoryDao = database.searchHistoryDao()
        )
    }
}
