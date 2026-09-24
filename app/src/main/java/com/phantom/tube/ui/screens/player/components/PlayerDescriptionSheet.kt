package com.phantom.tube.ui.screens.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.phantom.tube.core.theme.TextMuted
import com.phantom.tube.core.theme.TextPrimary
import com.phantom.tube.core.theme.TextSecondary
import com.phantom.tube.data.model.NextQueue
import com.phantom.tube.data.model.VideoItem
import kotlin.math.roundToInt

@Composable
fun PlayerDescriptionSheet(
    visible: Boolean,
    isFullscreen: Boolean,
    video: VideoItem,
    nextQueueData: NextQueue?,
    activeAvatarUrl: String,
    isSubscribed: Boolean,
    onSubscribeClick: () -> Unit,
    onChannelClick: (String, String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var descSheetOffsetY by remember { mutableFloatStateOf(0f) }
    val animatedDescSheetOffsetY by animateFloatAsState(
        targetValue = descSheetOffsetY,
        animationSpec = tween(durationMillis = 200),
        label = "desc_sheet_offset"
    )

    LaunchedEffect(visible) {
        if (visible) {
            descSheetOffsetY = 0f
        }
    }

    val descHandleDragModifier = Modifier.pointerInput(Unit) {
        detectVerticalDragGestures(
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                descSheetOffsetY = (descSheetOffsetY + dragAmount).coerceAtLeast(0f)
            },
            onDragEnd = {
                if (descSheetOffsetY > 140f) {
                    onDismiss()
                } else {
                    descSheetOffsetY = 0f
                }
            },
            onDragCancel = {
                descSheetOffsetY = 0f
            }
        )
    }

    AnimatedVisibility(
        visible = visible && !isFullscreen,
        enter = fadeIn(animationSpec = tween(180)) + slideInVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) { it },
        exit = fadeOut(animationSpec = tween(100)) + slideOutVertically(animationSpec = tween(150, easing = FastOutLinearInEasing)) { it },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.72f)
                    .align(Alignment.BottomCenter)
                    .offset { IntOffset(0, animatedDescSheetOffsetY.roundToInt()) }
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(Color(0xFF212121))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                ) {
                    // Drag handle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(descHandleDragModifier)
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 38.dp, height = 4.dp)
                                .background(Color.White.copy(alpha = 0.35f), RoundedCornerShape(2.dp))
                        )
                    }

                    // Header: Deskripsi + Close button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(descHandleDragModifier),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Deskripsi",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .height(1.dp)
                            .background(Color(0xFF2E2E2E))
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Video Title
                        item {
                            Text(
                                text = nextQueueData?.currentVideo?.title?.ifBlank { video.title } ?: video.title,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 22.sp
                            )
                        }

                        // 2. Channel info
                        item {
                            val targetChannelId = nextQueueData?.currentVideo?.channelId?.ifBlank { video.channelId } ?: video.channelId
                            val targetChannelTitle = nextQueueData?.currentVideo?.channelTitle?.ifBlank { video.channelTitle } ?: video.channelTitle
                            val currentAvatar = nextQueueData?.currentVideo?.channelAvatarUrl?.ifBlank { activeAvatarUrl } ?: activeAvatarUrl

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onChannelClick(targetChannelId, targetChannelTitle) }
                                ) {
                                    if (currentAvatar.isNotBlank()) {
                                        AsyncImage(
                                            model = currentAvatar,
                                            contentDescription = targetChannelTitle,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                    }
                                    Column {
                                        Text(
                                            text = targetChannelTitle,
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        val subs = nextQueueData?.channelSubscriberCountText ?: ""
                                        if (subs.isNotBlank()) {
                                            Text(
                                                text = subs,
                                                color = TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(if (isSubscribed) Color(0xFF272727) else Color.White)
                                        .clickable(onClick = onSubscribeClick)
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = if (isSubscribed) "Disubscribe" else "Subscribe",
                                        color = if (isSubscribed) Color.White else Color.Black,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // 3. Stats Row
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = nextQueueData?.likeCountText?.ifBlank { "-" } ?: "-",
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(text = "Suka", color = TextMuted, fontSize = 11.sp)
                                }

                                Box(modifier = Modifier.height(28.dp).width(1.dp).background(Color(0xFF333333)))

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = nextQueueData?.fullViewCountText?.ifBlank { video.viewCountText } ?: "-",
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(text = "Penayangan", color = TextMuted, fontSize = 11.sp)
                                }

                                Box(modifier = Modifier.height(28.dp).width(1.dp).background(Color(0xFF333333)))

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = nextQueueData?.dateText?.ifBlank { video.publishedTimeText } ?: "-",
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(text = "Tanggal", color = TextMuted, fontSize = 11.sp)
                                }
                            }
                        }

                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color(0xFF272727))
                            )
                        }

                        // 4. Description Content
                        item {
                            val desc = nextQueueData?.description?.ifBlank { "Tidak ada deskripsi." }
                                ?: "Tidak ada deskripsi."
                            Text(
                                text = desc,
                                color = TextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
