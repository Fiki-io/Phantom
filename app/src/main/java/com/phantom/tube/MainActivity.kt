package com.phantom.tube

import android.Manifest
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.phantom.tube.core.theme.ObsidianDark
import com.phantom.tube.core.theme.PhantomTheme
import com.phantom.tube.data.model.VideoItem
import com.phantom.tube.ui.components.LiquidGlassBottomNav
import com.phantom.tube.ui.components.NavTab
import com.phantom.tube.ui.screens.history.HistoryScreen
import com.phantom.tube.ui.screens.home.HomeScreen
import com.phantom.tube.ui.screens.library.LibraryScreen
import com.phantom.tube.ui.screens.player.PlayerScreen
import com.phantom.tube.ui.screens.search.SearchScreen

class MainActivity : ComponentActivity() {

    private var isInPipMode by mutableStateOf(false)
    private var activeVideo by mutableStateOf<VideoItem?>(null)
    private var isPlayerMinimized by mutableStateOf(false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Handled notification permission response
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()

        val app = application as PhantomApp
        val repository = app.repository

        setContent {
            PhantomTheme {
                var currentTab by remember { mutableStateOf(NavTab.HOME) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ObsidianDark)
                ) {
                    // 1. BASE SCREEN TABS (Always active in background)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                bottom = when {
                                    activeVideo != null && isPlayerMinimized -> 142.dp
                                    !isInPipMode -> 80.dp
                                    else -> 0.dp
                                }
                            )
                    ) {
                        Crossfade(targetState = currentTab, label = "tab_crossfade") { tab ->
                            when (tab) {
                                NavTab.HOME -> HomeScreen(
                                    repository = repository,
                                    onVideoClick = { video ->
                                        activeVideo = video
                                        isPlayerMinimized = false
                                    },
                                    onSearchClick = { currentTab = NavTab.SEARCH }
                                )
                                NavTab.SEARCH -> SearchScreen(
                                    repository = repository,
                                    onVideoClick = { video ->
                                        activeVideo = video
                                        isPlayerMinimized = false
                                    },
                                    onBackClick = { currentTab = NavTab.HOME }
                                )
                                NavTab.HISTORY -> HistoryScreen(
                                    repository = repository,
                                    onVideoClick = { video ->
                                        activeVideo = video
                                        isPlayerMinimized = false
                                    }
                                )
                                NavTab.LIBRARY -> LibraryScreen(
                                    repository = repository,
                                    onVideoClick = { video ->
                                        activeVideo = video
                                        isPlayerMinimized = false
                                    }
                                )
                            }
                        }
                    }

                    // 2. LIQUID GLASS BOTTOM NAVIGATION BAR
                    if (!isInPipMode && (activeVideo == null || isPlayerMinimized)) {
                        LiquidGlassBottomNav(
                            currentTab = currentTab,
                            onTabSelected = { currentTab = it },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }

                    // 3. PERSISTENT PLAYER (Full screen OR Miniplayer)
                    if (activeVideo != null) {
                        PlayerScreen(
                            video = activeVideo!!,
                            repository = repository,
                            isMinimized = isPlayerMinimized,
                            onMinimize = {
                                isPlayerMinimized = true
                            },
                            onExpand = {
                                isPlayerMinimized = false
                            },
                            onClose = {
                                activeVideo = null
                                isPlayerMinimized = false
                            },
                            onPlayNextVideo = { nextVideo ->
                                activeVideo = nextVideo
                            },
                            modifier = if (isPlayerMinimized) {
                                Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .padding(bottom = 76.dp)
                            } else {
                                Modifier.fillMaxSize()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Enter Picture-in-Picture only if a video is currently active
        if (activeVideo != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                // Ignore if not supported on specific hardware
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }
}
