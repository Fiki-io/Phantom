package com.phantom.tube.ui.screens.channel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.phantom.tube.data.innertube.parser.InnerTubeHelpers
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.phantom.tube.core.database.SubscriptionEntity
import com.phantom.tube.core.theme.ObsidianDark
import com.phantom.tube.core.theme.TextMuted
import com.phantom.tube.core.theme.TextPrimary
import com.phantom.tube.core.theme.TextSecondary
import com.phantom.tube.core.theme.YouTubeRed
import com.phantom.tube.data.model.ChannelProfile
import com.phantom.tube.data.model.ChannelSortChip
import com.phantom.tube.data.model.VideoItem
import com.phantom.tube.data.repository.PhantomRepository
import com.phantom.tube.ui.components.PhantomIconButton
import com.phantom.tube.ui.components.PhantomVideoCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelScreen(
    channelId: String,
    initialChannelTitle: String = "",
    repository: PhantomRepository,
    onVideoClick: (VideoItem) -> Unit,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ChannelProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val tabs = listOf("Beranda", "Video")
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })

    var videoTabVideos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var videoContinuationToken by remember { mutableStateOf<String?>(null) }
    var isLoadingMoreVideos by remember { mutableStateOf(false) }

    var sortChips by remember { mutableStateOf<List<ChannelSortChip>>(emptyList()) }
    var selectedSortChip by remember { mutableStateOf("Terbaru") }

    var showDescriptionSheet by remember { mutableStateOf(false) }

    // Check if channel is subscribed
    val isSubscribed by repository.isSubscribed(channelId).collectAsState(initial = false)

    // Load channel data
    fun loadChannelData(sortToken: String? = null) {
        scope.launch {
            if (sortToken != null) {
                isLoadingMoreVideos = true
                val contResult = repository.getChannelVideosContinuation(sortToken)
                val chTitle = profile?.title ?: initialChannelTitle
                val chAvatar = profile?.avatarUrl ?: ""
                videoTabVideos = contResult.videos.map {
                    it.copy(
                        channelTitle = if (it.channelTitle.isNotBlank()) it.channelTitle else chTitle,
                        channelAvatarUrl = if (it.channelAvatarUrl.isNotBlank()) it.channelAvatarUrl else chAvatar,
                        channelId = if (it.channelId.isNotBlank()) it.channelId else channelId
                    )
                }
                videoContinuationToken = contResult.continuationToken
                isLoadingMoreVideos = false
            } else {
                isLoading = true
                val res = repository.getChannel(channelId)
                if (res != null) {
                    profile = res
                    sortChips = if (res.sortChips.isNotEmpty()) {
                        res.sortChips
                    } else {
                        listOf(
                            ChannelSortChip("Terbaru", "", isSelected = true),
                            ChannelSortChip("Populer", ""),
                            ChannelSortChip("Terlama", "")
                        )
                    }

                    // If videos were in main profile response, populate them
                    if (res.videos.isNotEmpty()) {
                        videoTabVideos = res.videos
                        videoContinuationToken = res.continuationToken
                    } else if (!res.videoTabParams.isNullOrBlank()) {
                        // Load video tab specifically
                        val vTabRes = repository.getChannel(channelId, params = res.videoTabParams)
                        if (vTabRes != null && vTabRes.videos.isNotEmpty()) {
                            val chTitle = res.title.ifBlank { initialChannelTitle }
                            val chAvatar = res.avatarUrl
                            videoTabVideos = vTabRes.videos.map {
                                it.copy(
                                    channelTitle = if (it.channelTitle.isNotBlank()) it.channelTitle else chTitle,
                                    channelAvatarUrl = if (it.channelAvatarUrl.isNotBlank()) it.channelAvatarUrl else chAvatar,
                                    channelId = if (it.channelId.isNotBlank()) it.channelId else channelId
                                )
                            }
                            videoContinuationToken = vTabRes.continuationToken
                            if (vTabRes.sortChips.isNotEmpty()) {
                                sortChips = vTabRes.sortChips
                            }
                        }
                    }
                }
                isLoading = false
            }
        }
    }

    fun loadMoreVideos() {
        val token = videoContinuationToken
        if (token.isNullOrBlank() || isLoadingMoreVideos) return
        isLoadingMoreVideos = true
        scope.launch {
            try {
                val nextResult = repository.getChannelVideosContinuation(token)
                if (nextResult.videos.isNotEmpty()) {
                    val currentIds = videoTabVideos.map { it.id }.toSet()
                    val chTitle = profile?.title ?: initialChannelTitle
                    val chAvatar = profile?.avatarUrl ?: ""
                    val uniqueNew = nextResult.videos
                        .filter { it.id !in currentIds }
                        .map {
                            it.copy(
                                channelTitle = if (it.channelTitle.isNotBlank()) it.channelTitle else chTitle,
                                channelAvatarUrl = if (it.channelAvatarUrl.isNotBlank()) it.channelAvatarUrl else chAvatar,
                                channelId = if (it.channelId.isNotBlank()) it.channelId else channelId
                            )
                        }
                    videoTabVideos = videoTabVideos + uniqueNew
                    videoContinuationToken = nextResult.continuationToken
                } else {
                    videoContinuationToken = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingMoreVideos = false
            }
        }
    }

    LaunchedEffect(channelId) {
        loadChannelData()
    }

    val homeListState = rememberLazyListState()
    val videoListState = rememberLazyListState()

    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    var headerOffsetPx by remember { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember(headerHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < 0 && headerHeightPx > 0f) {
                    val newOffset = (headerOffsetPx + delta).coerceIn(-headerHeightPx, 0f)
                    val consumed = newOffset - headerOffsetPx
                    headerOffsetPx = newOffset
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta > 0 && headerHeightPx > 0f) {
                    val newOffset = (headerOffsetPx + delta).coerceIn(-headerHeightPx, 0f)
                    val consumed = newOffset - headerOffsetPx
                    headerOffsetPx = newOffset
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }
        }
    }

    // Pagination for channel videos on scroll in Video tab
    LaunchedEffect(videoListState, videoContinuationToken, isLoadingMoreVideos) {
        snapshotFlow {
            val layoutInfo = videoListState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val last = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && last >= total - 4
        }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                if (!videoContinuationToken.isNullOrBlank() && !isLoadingMoreVideos) {
                    loadMoreVideos()
                }
            }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = profile?.title?.ifBlank { initialChannelTitle } ?: initialChannelTitle,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Cari",
                        tint = TextPrimary
                    )
                }

                IconButton(onClick = { /* Opsi lainnya */ }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu Opsi",
                        tint = TextPrimary
                    )
                }
            }

            if (isLoading && profile == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = YouTubeRed,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                }
            } else {
                val chan = profile
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .clipToBounds()
                        .nestedScroll(nestedScrollConnection)
                ) {
                    val screenHeight = maxHeight
                    val tabRowHeight = 48.dp
                    val pagerHeight = (screenHeight - tabRowHeight).coerceAtLeast(200.dp)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(0, headerOffsetPx.roundToInt()) }
                    ) {
                        // 1. BANNER & CHANNEL PROFILE HEADER
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onSizeChanged {
                                    if (it.height > 0) {
                                        headerHeightPx = it.height.toFloat()
                                    }
                                }
                        ) {
                            // Banner
                            if (chan?.bannerUrl?.isNotBlank() == true) {
                                AsyncImage(
                                    model = chan.bannerUrl,
                                    contentDescription = "Banner ${chan.title}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 5.5f)
                                        .background(Color(0xFF1E1E1E))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(90.dp)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFF282828), Color(0xFF141414))
                                            )
                                        )
                                )
                            }

                            // Channel Profile Info (Exact match to Photo 3)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Large Circular Avatar
                                    AsyncImage(
                                        model = chan?.avatarUrl,
                                        contentDescription = chan?.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF222222))
                                    )

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        // Title + Official Artist / Verified Badge
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = chan?.title ?: initialChannelTitle,
                                                color = TextPrimary,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Spacer(modifier = Modifier.width(6.dp))

                                            Icon(
                                                imageVector = Icons.Default.MusicNote,
                                                contentDescription = "Official Artist Channel",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(3.dp))

                                        // Handle
                                        if (chan?.handle?.isNotBlank() == true) {
                                            Text(
                                                text = chan.handle,
                                                color = TextSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        // Subscribers & Videos count
                                        val statsText = buildString {
                                            if (chan?.subscriberCountText?.isNotBlank() == true) {
                                                append(chan.subscriberCountText)
                                            }
                                            if (chan?.videoCountText?.isNotBlank() == true) {
                                                if (isNotEmpty()) append(" • ")
                                                append(chan.videoCountText)
                                            }
                                        }

                                        if (statsText.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = statsText,
                                                color = TextSecondary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }

                                // Description Preview (Cp official ...selengkapnya)
                                if (chan?.description?.isNotBlank() == true) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = { showDescriptionSheet = true }
                                            ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = chan.description.trim().take(70),
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Text(
                                            text = " ...selengkapnya",
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                // External Links
                                if (chan?.externalLinksText?.isNotBlank() == true) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Link,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = chan.externalLinksText,
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Big SUBSCRIBE Button (Exact Match to Photo 3)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (isSubscribed) Color(0xFF272727) else Color.White
                                        )
                                        .clickable {
                                            scope.launch {
                                                if (isSubscribed) {
                                                    repository.unsubscribe(channelId)
                                                } else {
                                                    repository.subscribe(
                                                        SubscriptionEntity(
                                                            channelId = channelId,
                                                            channelTitle = chan?.title ?: initialChannelTitle,
                                                            channelHandle = chan?.handle ?: "",
                                                            channelAvatarUrl = chan?.avatarUrl ?: "",
                                                            subscriberCountText = chan?.subscriberCountText ?: ""
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
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        } else {
                                            Text(
                                                text = "Subscribe",
                                                color = Color.Black,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. TAB ROW (Beranda, Video) - Animated & Sticky
                        ScrollableTabRow(
                            selectedTabIndex = pagerState.currentPage,
                            containerColor = ObsidianDark,
                            contentColor = TextPrimary,
                            edgePadding = 16.dp,
                            indicator = { tabPositions ->
                                if (pagerState.currentPage < tabPositions.size) {
                                    Box(
                                        modifier = Modifier
                                            .tabIndicatorOffset(tabPositions[pagerState.currentPage])
                                            .fillMaxWidth()
                                            .height(2.5.dp)
                                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                            .background(Color.White)
                                    )
                                }
                            },
                            divider = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(0.5.dp)
                                        .background(Color(0x22FFFFFF))
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(tabRowHeight)
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = pagerState.currentPage == index,
                                    onClick = {
                                        scope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    },
                                    text = {
                                        Text(
                                            text = title,
                                            color = if (pagerState.currentPage == index) TextPrimary else TextSecondary,
                                            fontSize = 14.sp,
                                            fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                )
                            }
                        }

                        // 3. HORIZONTAL PAGER (Swipeable & Fluid Animated Page Switching)
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(pagerHeight)
                        ) { page ->
                            when (page) {
                                0 -> {
                                    // TAB "BERANDA" (Photo 1 & Photo 3)
                                    val featured = chan?.featuredVideo ?: chan?.homeVideos?.firstOrNull() ?: videoTabVideos.firstOrNull()
                                    val homeList = if (chan?.homeVideos?.isNotEmpty() == true) {
                                        chan.homeVideos.filter { it.id != featured?.id }
                                    } else {
                                        videoTabVideos.filter { it.id != featured?.id }
                                    }

                                    LazyColumn(
                                        state = homeListState,
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(bottom = 96.dp)
                                    ) {
                                        if (featured != null) {
                                            // "Untuk Anda" Section
                                            item(key = "for_you_header") {
                                                Text(
                                                    text = "Untuk Anda",
                                                    color = TextPrimary,
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                                )
                                            }

                                            // Featured video (Full width card matching official YouTube UI)
                                            item(key = "featured_for_you_video_${featured.id}") {
                                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                                    PhantomVideoCard(
                                                        video = featured,
                                                        onClick = { onVideoClick(featured) }
                                                    )
                                                }
                                            }
                                        }

                                        if (homeList.isNotEmpty()) {
                                            item(key = "recent_releases_header") {
                                                Text(
                                                    text = "Video Terbaru",
                                                    color = TextPrimary,
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                                )
                                            }

                                            items(homeList, key = { "home_vid_${it.id}" }) { video ->
                                                ChannelVideoHorizontalItem(
                                                    video = video,
                                                    onClick = { onVideoClick(video) }
                                                )
                                            }
                                        }
                                    }
                                }

                                1 -> {
                                    // TAB "VIDEO" (Photo 2)
                                    LazyColumn(
                                        state = videoListState,
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(bottom = 96.dp)
                                    ) {
                                        // Sort Chips: Terbaru, Populer, Terlama
                                        item(key = "video_sort_chips") {
                                            LazyRow(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 10.dp),
                                                contentPadding = PaddingValues(horizontal = 16.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                items(sortChips) { chip ->
                                                    val isSelected = selectedSortChip == chip.title
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(
                                                                if (isSelected) Color.White else Color(0xFF272727)
                                                            )
                                                            .clickable {
                                                                selectedSortChip = chip.title
                                                                if (chip.continuationToken.isNotBlank()) {
                                                                    loadChannelData(sortToken = chip.continuationToken)
                                                                    scope.launch {
                                                                        videoListState.animateScrollToItem(0)
                                                                    }
                                                                }
                                                            }
                                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(
                                                            text = chip.title,
                                                            color = if (isSelected) Color.Black else TextPrimary,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Video List (Horizontal Items exactly matching Photo 2)
                                        items(videoTabVideos, key = { it.id }) { video ->
                                            ChannelVideoHorizontalItem(
                                                video = video,
                                                onClick = { onVideoClick(video) }
                                            )
                                        }

                                        if (isLoadingMoreVideos) {
                                            item(key = "loading_more_videos") {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    CircularProgressIndicator(
                                                        color = YouTubeRed,
                                                        modifier = Modifier.size(24.dp),
                                                        strokeWidth = 2.5.dp
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
            }
        }

        // MODAL BOTTOM SHEET FOR CHANNEL DESCRIPTION (when clicking ...selengkapnya)
        if (showDescriptionSheet) {
            ModalBottomSheet(
                onDismissRequest = { showDescriptionSheet = false },
                containerColor = Color(0xFF1E1E1E),
                contentColor = TextPrimary,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tentang Channel",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showDescriptionSheet = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = profile?.description ?: "Tidak ada deskripsi",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (profile?.externalLinksText?.isNotBlank() == true) {
                        Text(
                            text = "Tautan",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = profile?.externalLinksText ?: "",
                            color = YouTubeRed,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

/**
 * Item video horizontal khusus Channel Page (Persis seperti pada Screenshot 2):
 * - Thumbnail di sisi kiri dengan badge durasi
 * - Judul video, views & waktu rilis di sisi kanan
 * - Tombol opsi ⋮ di ujung kanan
 */
@Composable
fun ChannelVideoHorizontalItem(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Thumbnail 16:9 on Left (Width ~136dp)
        Box(
            modifier = Modifier
                .width(136.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF222222))
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            // Duration badge in bottom right corner (e.g. 4.20)
            if (video.durationText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(
                            color = Color(0xCC000000),
                            shape = RoundedCornerShape(3.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = video.durationText,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Meta on Right
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp)
        ) {
            Text(
                text = video.title,
                color = TextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            val metaText = InnerTubeHelpers.formatVideoMeta(
                channelTitle = "",
                viewCountText = video.viewCountText,
                publishedTimeText = video.publishedTimeText
            )

            if (metaText.isNotBlank()) {
                Text(
                    text = metaText,
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // More options icon
        IconButton(
            onClick = { /* Opsi */ },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Opsi",
                tint = TextSecondary,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}
