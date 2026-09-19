package com.phantom.tube.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entry: WatchHistoryEntity)

    @Query("SELECT * FROM watch_history ORDER BY watchedAt DESC")
    fun getAllHistory(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history ORDER BY watchedAt DESC LIMIT :limit")
    suspend fun getRecentWatched(limit: Int = 10): List<WatchHistoryEntity>

    @Query("SELECT * FROM watch_history WHERE videoId = :videoId LIMIT 1")
    suspend fun getEntry(videoId: String): WatchHistoryEntity?

    @Query("SELECT lastPositionMs FROM watch_history WHERE videoId = :videoId LIMIT 1")
    suspend fun getLastPosition(videoId: String): Long?

    @Query("DELETE FROM watch_history WHERE videoId = :videoId")
    suspend fun delete(videoId: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearAll()
}
