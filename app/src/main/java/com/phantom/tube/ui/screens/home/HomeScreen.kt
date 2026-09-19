package com.phantom.tube.ui.screens.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.phantom.tube.core.theme.NeonCyan
import com.phantom.tube.core.theme.ObsidianDark
import com.phantom.tube.core.theme.TextMuted
import com.phantom.tube.core.theme.TextPrimary
import com.phantom.tube.core.theme.TextSecondary
import com.phantom.tube.core.theme.liquidGlass
import com.phantom.tube.data.model.VideoItem
import com.phantom.tube.data.repository.PhantomRepository
import com.phantom.tube.ui.components.LiquidGlassIconButton
import com.phantom.tube.ui.components.LiquidGlassTopBar
import com.phantom.tube.ui.components.LiquidGlassVideoCard
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    repository: PhantomRepository,
    onVideoClick: (VideoItem) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf("Semua", "Trending", "Musik", "Gaming", "Berita", "Podcast", "Teknologi", "Animasi")
    var selectedCategory by remember { mutableStateOf("Semua") }
    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var continuationToken by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun categoryToQuery(category: String): String = when (category) {
        "Semua" -> "trending indonesia"
        "Trending" -> "viral indonesia hari ini"
        "Musik" -> "lagu indonesia hits terbaru"
        "Gaming" -> "gaming trending indonesia"
        "Berita" -> "berita terkini hari ini"
        "Podcast" -> "podcast indonesia"
        "Teknologi" -> "gadget teknologi indonesia"
        "Animasi" -> "animasi indonesia anime"
        else -> category
    }

    fun loadFeed(category: String, isRefresh: Boolean = false) {
        scope.launch {
            if (isRefresh) {
                isRefreshing = true
            } else {
                isLoading = true
            }
            hasError = false
            try {
                val result = if (category == "Semua") {
                    repository.getHomeRecommendations(historyIndex = 0)
                } else {
                    repository.getFeedPage(query = categoryToQuery(category))
                }
                videos = result.videos
                continuationToken = result.continuationToken
                hasError = result.videos.isEmpty()
                if (isRefresh) {
                    listState.scrollToItem(0)
                }
            } catch (e: Exception) {
                if (!isRefresh) {
                    hasError = true
                }
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    fun loadMore() {
        val token = continuationToken ?: return
        if (isLoadingMore || isLoading || isRefreshing) return
        scope.launch {
            isLoadingMore = true
            try {
                val result = if (selectedCategory == "Semua") {
                    if (token.startsWith("history_")) {
                        val nextIdx = token.substringAfter("history_").toIntOrNull() ?: 1
                        repository.getHomeRecommendations(historyIndex = nextIdx)
                    } else {
                        repository.getHomeRecommendations(continuation = token)
                    }
                } else {
                    repository.getFeedPage(continuation = token)
                }

                if (result.videos.isNotEmpty()) {
                    val existingIds = videos.map { v -> v.id }.toSet()
                    val newVideos = result.videos.filterNot { v -> v.id in existingIds }
                    videos = videos + newVideos
                }
                continuationToken = result.continuationToken
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingMore = false
            }
        }
    }

    LaunchedEffect(selectedCategory) {
        loadFeed(selectedCategory)
    }

    // Automatic infinite scroll engine: monitors scroll state on every frame
    LaunchedEffect(listState, continuationToken, isLoadingMore, videos.size) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val last = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total to last
        }.collect { (total, last) ->
            if (total > 0 && last >= total - 3 && continuationToken != null && !isLoadingMore && !isLoading && !isRefreshing) {
                loadMore()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDark)
    ) {
        LiquidGlassTopBar(
            selectedCategory = selectedCategory,
            categories = categories,
            onCategorySelected = { selectedCategory = it },
            onSearchClick = onSearchClick,
            onRefreshClick = { loadFeed(selectedCategory, isRefresh = true) }
        )

        Box(modifier = Modifier.fillMaxSize()) {
            // Live refresh pill indicator
            if (isRefreshing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .zIndex(10f)
                        .padding(top = 8.dp)
                        .liquidGlass(
                            shape = RoundedCornerShape(20.dp),
                            borderWidth = 1.dp,
                            glassAlpha = 0.85f,
                            accentGlow = NeonCyan
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    CircularProgressIndicator(
                        color = NeonCyan,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Menyegarkan Beranda...",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = NeonCyan,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Memuat video terbaru...",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
                hasError && videos.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Gagal memuat feed video",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LiquidGlassIconButton(
                            icon = Icons.Default.Refresh,
                            contentDescription = "Coba Lagi",
                            onClick = { loadFeed(selectedCategory) }
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(videos, key = { index, video -> "home_${video.id}_$index" }) { _, video ->
                            LiquidGlassVideoCard(
                                video = video,
                                onClick = { onVideoClick(video) }
                            )
                        }

                        // Infinite scroll loader at bottom
                        if (isLoadingMore) {
                            item(key = "loading_more_indicator") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .liquidGlass(
                                                shape = RoundedCornerShape(16.dp),
                                                borderWidth = 0.5.dp,
                                                glassAlpha = 0.45f
                                            )
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            color = NeonCyan,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Memuat lebih banyak video...",
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        if (continuationToken == null && videos.isNotEmpty()) {
                            item(key = "end_of_feed_notice") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Semua video telah ditampilkan",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

