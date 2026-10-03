package com.phantom.tube.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [WatchHistoryEntity::class, FavoriteEntity::class, SearchHistoryEntity::class, SubscriptionEntity::class, DownloadEntity::class],
    version = 5,
    exportSchema = false
)
abstract class PhantomDatabase : RoomDatabase() {
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile
        private var INSTANCE: PhantomDatabase? = null

        fun getInstance(context: Context): PhantomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PhantomDatabase::class.java,
                    "phantom_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
