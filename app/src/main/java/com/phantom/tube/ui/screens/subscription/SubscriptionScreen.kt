package com.phantom.tube.ui.screens.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.phantom.tube.core.theme.ObsidianDark
import com.phantom.tube.core.theme.TextMuted
import com.phantom.tube.core.theme.TextPrimary
import com.phantom.tube.core.theme.TextSecondary
import com.phantom.tube.core.theme.YouTubeRed
import com.phantom.tube.data.model.VideoItem
import com.phantom.tube.data.repository.PhantomRepository
import com.phantom.tube.ui.components.PhantomVideoCard
import kotlinx.coroutines.launch

@Composable
fun SubscriptionScreen(
    repository: PhantomRepository,
    onVideoClick: (VideoItem) -> Unit,
    onChannelClick: (channelId: String, channelTitle: String) -> Unit,
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subscriptions by repository.getSubscriptions().collectAsState(initial = emptyList())
    var feedVideos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var isLoadingFeed by remember { mutableStateOf(false) }

    // Fetch latest videos from subscribed channels
    LaunchedEffect(subscriptions) {
        if (subscriptions.isNotEmpty()) {
            isLoadingFeed = true
            val collectedVideos = mutableListOf<VideoItem>()
            // Query top 5 recent subscriptions for their latest videos
            subscriptions.take(5).forEach { sub ->
                try {
                    val chanProfile = repository.getChannel(sub.channelId)
                    if (chanProfile != null && chanProfile.videos.isNotEmpty()) {
                        collectedVideos.addAll(chanProfile.videos.take(6))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            feedVideos = collectedVideos.distinctBy { it.id }
            isLoadingFeed = false
        } else {
            feedVideos = emptyList()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .statusBarsPadding()
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Subscriptions,
                contentDescription = null,
                tint = YouTubeRed,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Subscription",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (subscriptions.isEmpty()) {
            // EMPTY STATE
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Subscriptions,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(64.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Belum Ada Channel yang Dilanggan",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Jelajahi video dan subscribe channel favoritmu untuk melihat video terbarunya di sini tanpa butuh akun Google.",
                        color = TextSecondary,
                        fontSize = 13.5.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onExploreClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YouTubeRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Jelajahi Beranda",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                // 1. Horizontal Avatars Row of Subscribed Channels
                item(key = "subscribed_channels_row") {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(subscriptions, key = { it.channelId }) { sub ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(64.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { onChannelClick(sub.channelId, sub.channelTitle) }
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF222222))
                                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                ) {
                                    AsyncImage(
                                        model = sub.channelAvatarUrl,
                                        contentDescription = sub.channelTitle,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.matchParentSize()
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = sub.channelTitle,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // 2. Section divider / header
                item(key = "feed_section_header") {
                    Text(
                        text = "Video Terbaru",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }

                // 3. Loading indicator or Feed items
                if (isLoadingFeed && feedVideos.isEmpty()) {
                    item(key = "feed_loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = YouTubeRed,
                                modifier = Modifier.size(30.dp),
                                strokeWidth = 2.5.dp
                            )
                        }
                    }
                } else {
                    itemsIndexed(feedVideos, key = { index, v -> "sub_vid_${v.id}_$index" }) { _, video ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            PhantomVideoCard(
                                video = video,
                                onChannelClick = { chId ->
                                    onChannelClick(chId.ifBlank { video.channelTitle }, video.channelTitle)
                                },
                                onClick = { onVideoClick(video) }
                            )
                        }
                    }
                }
            }
        }
    }
}
