package com.phantom.tube.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
                val result = repository.getFeedPage(query = categoryToQuery(category))
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
                val result = repository.getFeedPage(continuation = token)
                if (result.videos.isNotEmpty()) {
                    val existingIds = videos.map { it.id }.toSet()
                    val newVideos = result.videos.filterNot { it.id in existingIds }
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

    // Infinite scroll detection
    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 4
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && continuationToken != null && !isLoadingMore && !isLoading && !isRefreshing) {
            loadMore()
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
            AnimatedVisibility(
                visible = isRefreshing,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(10f)
                    .padding(top = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .liquidGlass(
                            shape = RoundedCornerShape(20.dp),
                            borderWidth = 1.dp,
                            glassAlpha = 0.75f,
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

