package com.phantom.tube.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "downloads",
    indices = [Index(value = ["downloadedAt"])]
)
data class DownloadEntity(
    @PrimaryKey
    val id: String, // e.g. "${videoId}_${format}_${qualityValue}"
    val videoId: String,
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val format: String, // "MP3" or "MP4"
    val qualityLabel: String, // "320 kbps", "1080p Full HD", etc.
    val fileName: String,
    val fileUri: String, // content:// or file://
    val filePath: String, // display path: e.g. "Music/Phantom/song.mp3"
    val fileSize: Long,
    val downloadedAt: Long = System.currentTimeMillis()
)

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity)

    @Query("SELECT * FROM downloads ORDER BY downloadedAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM downloads WHERE videoId = :videoId)")
    fun isDownloaded(videoId: String): Flow<Boolean>

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM downloads WHERE videoId = :videoId")
    suspend fun deleteByVideoId(videoId: String)

    @Query("DELETE FROM downloads")
    suspend fun clearAll()
}
