package com.phantom.tube.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.phantom.tube.core.database.SubscriptionEntity
import com.phantom.tube.core.theme.TextMuted
import com.phantom.tube.core.theme.TextPrimary
import com.phantom.tube.core.theme.TextSecondary
import com.phantom.tube.data.model.SearchChannelItem
import com.phantom.tube.data.repository.PhantomRepository
import kotlinx.coroutines.launch

/**
 * Premium YouTube-styled Channel Card displayed at the top of search results
 * when searching for channel names or matching channel creators.
 */
@Composable
fun SearchChannelCard(
    channel: SearchChannelItem,
    repository: PhantomRepository,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val isSubscribed by repository.isSubscribed(channel.id).collectAsState(initial = false)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF161616))
            .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large circular avatar
                AsyncImage(
                    model = channel.avatarUrl,
                    contentDescription = channel.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF262626))
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Title + Verified Check
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = channel.title,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (channel.isVerified) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Terverifikasi",
                                tint = Color(0xFFAAAAAA),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Handle & Subscriber & Video stats
                    val metaLine = buildString {
                        if (channel.handle.isNotBlank()) {
                            append(channel.handle)
                        }
                        if (channel.subscriberCountText.isNotBlank()) {
                            if (isNotEmpty()) append(" • ")
                            append(channel.subscriberCountText)
                        }
                        if (channel.videoCountText.isNotBlank()) {
                            if (isNotEmpty()) append(" • ")
                            append(channel.videoCountText)
                        }
                    }

                    if (metaLine.isNotBlank()) {
                        Text(
                            text = metaLine,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Description preview snippet
                    if (channel.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = channel.description,
                            color = TextMuted,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Subscribe action button matching official YouTube styling
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(
                        if (isSubscribed) Color(0xFF272727) else Color.White
                    )
                    .clickable {
                        scope.launch {
                            if (isSubscribed) {
                                repository.unsubscribe(channel.id)
                            } else {
                                repository.subscribe(
                                    SubscriptionEntity(
                                        channelId = channel.id,
                                        channelTitle = channel.title,
                                        channelHandle = channel.handle,
                                        channelAvatarUrl = channel.avatarUrl,
                                        subscriberCountText = channel.subscriberCountText
                                    )
                                )
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSubscribed) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Disubscribe",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "Subscribe",
                            color = Color.Black,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
