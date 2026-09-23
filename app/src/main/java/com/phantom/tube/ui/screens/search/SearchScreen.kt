package com.phantom.tube.ui.screens.search

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phantom.tube.core.theme.ObsidianDark
import com.phantom.tube.core.theme.TextMuted
import com.phantom.tube.core.theme.TextPrimary
import com.phantom.tube.core.theme.TextSecondary
import com.phantom.tube.core.theme.YouTubeRed
import com.phantom.tube.core.theme.YouTubeSurface
import com.phantom.tube.data.model.VideoItem
import com.phantom.tube.data.repository.PhantomRepository
import com.phantom.tube.ui.components.PhantomIconButton
import com.phantom.tube.ui.components.PhantomVideoCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class SearchSuggestionItem(
    val query: String,
    val isHistory: Boolean
)

@Composable
fun SearchScreen(
    repository: PhantomRepository,
    onVideoClick: (VideoItem) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var searchResults by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var searchContinuationToken by remember { mutableStateOf<String?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var isFetchingSuggestions by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    val searchListState = rememberLazyListState()

    val searchHistory by repository.getSearchHistory().collectAsState(initial = emptyList())

    // Instant local history matching (0ms latency, always reactive while typing)
    val matchingHistory = remember(searchQuery, searchHistory) {
        val trimmed = searchQuery.trim()
        if (trimmed.isBlank()) emptyList()
        else searchHistory.filter { it.query.contains(trimmed, ignoreCase = true) }
    }

    // Unified hybrid suggestions: matching local search history first, then live YouTube suggestions
    val combinedSuggestions = remember(matchingHistory, suggestions, searchQuery) {
        val trimmed = searchQuery.trim()
        if (trimmed.isBlank()) {
            emptyList()
        } else {
            val list = mutableListOf<SearchSuggestionItem>()
            val seen = mutableSetOf<String>()

            // 1. Matching history queries first
            matchingHistory.forEach { item ->
                val q = item.query.trim()
                if (q.isNotEmpty() && seen.add(q.lowercase())) {
                    list.add(SearchSuggestionItem(query = q, isHistory = true))
                }
            }

            // 2. YouTube network suggestions
            suggestions.forEach { sugg ->
                val q = sugg.trim()
                if (q.isNotEmpty() && seen.add(q.lowercase())) {
                    list.add(SearchSuggestionItem(query = q, isHistory = false))
                }
            }

            list
        }
    }

    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    fun executeSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        keyboardController?.hide()
        scope.launch {
            repository.saveSearchQuery(trimmed)
            isSearching = true
            hasSearched = true
            suggestions = emptyList()
            searchContinuationToken = null
            try {
                val pageResult = repository.searchPage(query = trimmed)
                searchResults = pageResult.videos
                searchContinuationToken = pageResult.continuationToken
            } catch (e: Exception) {
                searchResults = emptyList()
                searchContinuationToken = null
            } finally {
                isSearching = false
            }
        }
    }

    fun loadMoreSearch() {
        val token = searchContinuationToken ?: return
        if (isLoadingMore || isSearching) return
        scope.launch {
            isLoadingMore = true
            try {
                val pageResult = repository.searchPage(continuation = token)
                if (pageResult.videos.isNotEmpty()) {
                    val existingIds = searchResults.map { it.id }.toSet()
                    val newVideos = pageResult.videos.filterNot { it.id in existingIds }
                    searchResults = searchResults + newVideos
                }
                searchContinuationToken = pageResult.continuationToken
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingMore = false
            }
        }
    }

    // Infinite scroll listener: detects when user scrolls near the end of search results
    LaunchedEffect(searchListState, searchContinuationToken, isLoadingMore, searchResults.size) {
        snapshotFlow {
            val layoutInfo = searchListState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val last = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total to last
        }.collect { (total, last) ->
            if (total > 0 && last >= total - 3 && searchContinuationToken != null && !isLoadingMore && !isSearching) {
                loadMoreSearch()
            }
        }
    }

    LaunchedEffect(searchQuery, hasSearched) {
        val trimmed = searchQuery.trim()
        if (trimmed.isNotBlank() && !hasSearched) {
            isFetchingSuggestions = true
            delay(120) // Fast 120ms debounce for responsive typing
            try {
                val fetched = repository.getSuggestions(trimmed)
                if (fetched.isNotEmpty()) {
                    suggestions = fetched
                }
            } catch (e: Exception) {
                // Keep suggestions if network glitches
            } finally {
                isFetchingSuggestions = false
            }
        } else if (trimmed.isBlank()) {
            suggestions = emptyList()
            isFetchingSuggestions = false
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .statusBarsPadding()
    ) {
        // YouTube Search Input Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PhantomIconButton(
                icon = Icons.Default.ArrowBack,
                contentDescription = "Kembali",
                size = 38.dp,
                iconSize = 20.dp,
                onClick = onBackClick
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Standard YouTube Search Bar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(YouTubeSurface)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            hasSearched = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(YouTubeRed),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { executeSearch(searchQuery) }),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Telusuri YouTube...",
                                    color = TextMuted,
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Hapus",
                            tint = TextSecondary,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    searchQuery = ""
                                    suggestions = emptyList()
                                    hasSearched = false
                                    searchResults = emptyList()
                                }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Content: Loading, History, Suggestions, or Results
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isSearching -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = YouTubeRed,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Memuat...",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
                // 2. Actively typing (searchQuery is not blank) -> Show Hybrid Suggestions (Instant local history + Live YouTube suggestions)
                !hasSearched && searchQuery.isNotBlank() -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (isFetchingSuggestions && combinedSuggestions.isEmpty()) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp),
                                color = YouTubeRed,
                                trackColor = Color.Transparent
                            )
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (combinedSuggestions.isEmpty()) {
                                item(key = "direct_search") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                executeSearch(searchQuery)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = YouTubeRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Text(
                                            text = "Telusuri \"${searchQuery.trim()}\"",
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            } else {
                                itemsIndexed(
                                    items = combinedSuggestions,
                                    key = { index, item -> "${item.isHistory}_${item.query}_$index" },
                                    contentType = { _, _ -> "suggestion_item" }
                                ) { _, item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                searchQuery = item.query
                                                executeSearch(item.query)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (item.isHistory) Icons.Default.History else Icons.Default.Search,
                                            contentDescription = if (item.isHistory) "Riwayat" else "Saran",
                                            tint = if (item.isHistory) TextSecondary else TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Text(
                                            text = item.query,
                                            color = if (item.isHistory) TextPrimary else TextPrimary.copy(alpha = 0.95f),
                                            fontSize = 14.sp,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.NorthWest,
                                            contentDescription = "Gunakan kueri",
                                            tint = TextMuted,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable {
                                                    searchQuery = item.query
                                                }
                                        )
                                        if (item.isHistory) {
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Hapus kueri",
                                                tint = TextMuted,
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable {
                                                        scope.launch {
                                                            repository.deleteSearchQuery(item.query)
                                                        }
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                hasSearched && searchResults.isNotEmpty() -> {
                    LazyColumn(
                        state = searchListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(
                            items = searchResults,
                            key = { index, video -> "search_${video.id}_$index" },
                            contentType = { _, _ -> "video_card" }
                        ) { _, video ->
                            PhantomVideoCard(
                                video = video,
                                onClick = { onVideoClick(video) }
                            )
                        }

                        if (isLoadingMore) {
                            item(key = "loading_more_search", contentType = "loader") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(YouTubeSurface)
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            color = YouTubeRed,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Memuat...",
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                hasSearched && searchResults.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Tidak ada hasil untuk \"${searchQuery.trim()}\"",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    }
                }
                searchQuery.isBlank() && searchHistory.isNotEmpty() -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Riwayat Penelusuran",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "Hapus semua",
                                color = YouTubeRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .clickable {
                                        scope.launch {
                                            repository.clearSearchHistory()
                                        }
                                    }
                                    .padding(4.dp)
                            )
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(
                                items = searchHistory,
                                key = { it.query },
                                contentType = { "history_item" }
                            ) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            searchQuery = item.query
                                            executeSearch(item.query)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = item.query,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.NorthWest,
                                        contentDescription = "Masukkan ke pencarian",
                                        tint = TextMuted,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable {
                                                searchQuery = item.query
                                            }
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Hapus",
                                        tint = TextMuted,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable {
                                                scope.launch {
                                                    repository.deleteSearchQuery(item.query)
                                                }
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = TextMuted.copy(alpha = 0.35f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Telusuri video",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

