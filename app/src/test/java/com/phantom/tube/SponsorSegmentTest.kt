package com.phantom.tube

import com.phantom.tube.core.database.WatchHistoryEntity
import com.phantom.tube.data.model.SponsorSegment
import org.junit.Assert.assertEquals
import org.junit.Test

class SponsorSegmentTest {

    @Test
    fun testSponsorSegmentDuration() {
        val segment = SponsorSegment(
            category = "sponsor",
            startSecond = 45.5f,
            endSecond = 95.5f
        )
        assertEquals(50.0f, segment.durationSeconds, 0.01f)
    }

    @Test
    fun testWatchHistoryProgress() {
        val entry = WatchHistoryEntity(
            videoId = "vid1",
            title = "Test",
            channelTitle = "Channel",
            thumbnailUrl = "thumb",
            lastPositionMs = 30000L,
            durationMs = 60000L
        )
        assertEquals(0.5f, entry.progressFraction, 0.01f)
    }
}
