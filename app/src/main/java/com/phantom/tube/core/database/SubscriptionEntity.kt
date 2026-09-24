package com.phantom.tube.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey
    val channelId: String,
    val channelTitle: String,
    val channelHandle: String = "",
    val channelAvatarUrl: String = "",
    val subscriberCountText: String = "",
    val subscribedAt: Long = System.currentTimeMillis()
)

@Dao
interface SubscriptionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subscription: SubscriptionEntity)

    @Query("SELECT * FROM subscriptions ORDER BY subscribedAt DESC")
    fun getAllSubscriptions(): Flow<List<SubscriptionEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM subscriptions WHERE channelId = :channelId)")
    fun isSubscribed(channelId: String): Flow<Boolean>

    @Query("DELETE FROM subscriptions WHERE channelId = :channelId")
    suspend fun delete(channelId: String)

    @Query("SELECT COUNT(*) FROM subscriptions")
    fun getSubscriptionCount(): Flow<Int>
}
