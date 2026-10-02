package com.phantom.tube.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.phantom.tube.core.theme.TextPrimary
import com.phantom.tube.core.theme.TextSecondary
import com.phantom.tube.core.theme.YouTubeRed
import com.phantom.tube.data.innertube.cache.ChannelAvatarCache
import com.phantom.tube.data.innertube.parser.InnerTubeHelpers
import com.phantom.tube.data.model.VideoItem

/**
 * Kartu item video standar:
 * - Thumbnail 16:9 dengan durasi
 * - Indikator progres tonton riil
 * - Avatar channel
 * - Judul video, nama channel, dan info penayangan
 * - Tombol opsi
 */
@Composable
fun PhantomVideoCard(
    video: VideoItem,
    modifier: Modifier = Modifier,
    progressFraction: Float = 0f,
    onChannelClick: ((String) -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .iosBounceClick(
                scaleDown = 0.975f,
                alphaDown = 0.95f,
                onClick = onClick
            )
            .padding(bottom = 12.dp)
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF181818))
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            // Durasi video atau Badge Playlist
            if (video.isPlaylist || !video.playlistId.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 8.dp, bottom = 8.dp)
                        .background(
                            color = Color(0xCC000000),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QueueMusic,
                            contentDescription = "Playlist",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = video.durationText.ifBlank { "Playlist" },
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            } else if (video.durationText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 8.dp, bottom = 8.dp)
                        .background(
                            color = Color(0xCC000000),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = video.durationText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Real Watch Progress Bar from SQLite history
            if (progressFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color(0x66000000))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progressFraction.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(YouTubeRed)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Info: Avatar + Detail
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            val targetChannelId = video.channelId.ifBlank { video.channelTitle }
            var avatarUrl by remember(video.id, video.channelAvatarUrl) {
                val clean = InnerTubeHelpers.normalizeUrl(video.channelAvatarUrl)
                mutableStateOf(clean.ifBlank { ChannelAvatarCache.get(video.channelId, video.channelTitle) })
            }

            LaunchedEffect(video.id, video.channelId, video.channelTitle, avatarUrl) {
                if (avatarUrl.isBlank() && video.channelId.isNotBlank() && video.channelId.startsWith("UC")) {
                    val resolved = ChannelAvatarCache.resolveAvatar(video.channelId, video.channelTitle)
                    if (!resolved.isNullOrBlank()) {
                        avatarUrl = resolved
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF272727))
                    .then(
                        if (onChannelClick != null && targetChannelId.isNotBlank()) {
                            Modifier.iosBounceClick(scaleDown = 0.90f) { onChannelClick(targetChannelId) }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = video.channelTitle,
                        modifier = Modifier
                            .matchParentSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else if (video.isPlaylist || !video.playlistId.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color(0xFF272727), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Mix",
                            tint = YouTubeRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    val initials = video.channelTitle.trim().take(1).uppercase()
                    if (initials.isNotBlank()) {
                        Text(
                            text = initials,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = YouTubeRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(3.dp))

                val metaString = InnerTubeHelpers.formatVideoMeta(
                    channelTitle = video.channelTitle,
                    viewCountText = video.viewCountText,
                    publishedTimeText = video.publishedTimeText
                )

                Text(
                    text = metaString,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = { onMoreClick?.invoke() },
                modifier = Modifier
                    .size(32.dp)
                    .padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Opsi",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
