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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phantom.tube.core.theme.NeonCyan
import com.phantom.tube.core.theme.ObsidianDark
import com.phantom.tube.core.theme.TextMuted
import com.phantom.tube.core.theme.TextPrimary
import com.phantom.tube.core.theme.TextSecondary
import com.phantom.tube.core.theme.liquidGlass
import com.phantom.tube.data.model.VideoItem
import com.phantom.tube.data.repository.PhantomRepository
import com.phantom.tube.ui.components.LiquidGlassIconButton
import com.phantom.tube.ui.components.LiquidGlassVideoCard
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var isSearching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var debounceJob by remember { mutableStateOf<Job?>(null) }

    fun executeSearch(query: String) {
        if (query.isBlank()) return
        keyboardController?.hide()
        scope.launch {
            isSearching = true
            hasSearched = true
            suggestions = emptyList()
            try {
                searchResults = repository.search(query)
            } catch (e: Exception) {
                searchResults = emptyList()
            } finally {
                isSearching = false
            }
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank() && !hasSearched) {
            debounceJob?.cancel()
            debounceJob = scope.launch {
                delay(300)
                suggestions = repository.getSuggestions(searchQuery)
            }
        } else if (searchQuery.isBlank()) {
            suggestions = emptyList()
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
        // Search Input Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiquidGlassIconButton(
                icon = Icons.Default.ArrowBack,
                contentDescription = "Kembali",
                size = 40.dp,
                iconSize = 20.dp,
                onClick = onBackClick
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Glass Search Bar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .liquidGlass(
                        shape = RoundedCornerShape(24.dp),
                        borderWidth = 1.dp,
                        glassAlpha = 0.65f,
                        accentGlow = NeonCyan
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = NeonCyan,
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
                        cursorBrush = SolidColor(NeonCyan),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { executeSearch(searchQuery) }),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Cari di YouTube...",
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
                                }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Content: Suggestions or Results
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isSearching -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = NeonCyan,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Mencari video...",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
                suggestions.isNotEmpty() && !hasSearched -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(suggestions) { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .liquidGlass(
                                        shape = RoundedCornerShape(12.dp),
                                        borderWidth = 0.5.dp,
                                        glassAlpha = 0.35f
                                    )
                                    .clickable {
                                        searchQuery = suggestion
                                        executeSearch(suggestion)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = suggestion,
                                        color = TextPrimary,
                                        fontSize = 14.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.NorthWest,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
                hasSearched && searchResults.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(searchResults, key = { index, video -> "search_${video.id}_$index" }) { _, video ->
                            LiquidGlassVideoCard(
                                video = video,
                                onClick = { onVideoClick(video) }
                            )
                        }
                    }
                }
                hasSearched && searchResults.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Tidak ada hasil ditemukan",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Ketik judul atau kata kunci untuk mencari",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
