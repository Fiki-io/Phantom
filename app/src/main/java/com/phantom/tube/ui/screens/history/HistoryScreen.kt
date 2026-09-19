package com.phantom.tube.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.phantom.tube.core.theme.NeonCyan
import com.phantom.tube.core.theme.ObsidianDark
import com.phantom.tube.core.theme.TextMuted
import com.phantom.tube.core.theme.TextPrimary
import com.phantom.tube.core.theme.TextSecondary
import com.phantom.tube.core.theme.liquidGlass
import com.phantom.tube.data.model.VideoItem
import com.phantom.tube.data.repository.PhantomRepository
import com.phantom.tube.ui.components.LiquidGlassIconButton
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(
    repository: PhantomRepository,
    onVideoClick: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val historyList by repository.getWatchHistory().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Riwayat Tontonan",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (historyList.isNotEmpty()) {
                LiquidGlassIconButton(
                    icon = Icons.Default.DeleteSweep,
                    contentDescription = "Hapus Riwayat",
                    size = 38.dp,
                    iconSize = 20.dp,
                    onClick = {
                        scope.launch { repository.clearHistory() }
                    }
                )
            }
        }

        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Belum ada riwayat tontonan",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(historyList, key = { index, entry -> "hist_${entry.videoId}_$index" }) { _, entry ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                shape = RoundedCornerShape(16.dp),
                                borderWidth = 0.8.dp,
                                glassAlpha = 0.45f
                            )
                            .clickable {
                                onVideoClick(
                                    VideoItem(
                                        id = entry.videoId,
                                        title = entry.title,
                                        channelTitle = entry.channelTitle,
                                        thumbnailUrl = entry.thumbnailUrl,
                                        durationText = entry.durationText
                                    )
                                )
                            }
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Thumbnail with Resume Progress Indicator
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(68.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF141520))
                            ) {
                                AsyncImage(
                                    model = entry.thumbnailUrl,
                                    contentDescription = entry.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )

                                if (entry.progressFraction > 0f) {
                                    LinearProgressIndicator(
                                        progress = { entry.progressFraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .align(Alignment.BottomCenter),
                                        color = NeonCyan,
                                        trackColor = Color.White.copy(alpha = 0.3f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.title,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = entry.channelTitle,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
