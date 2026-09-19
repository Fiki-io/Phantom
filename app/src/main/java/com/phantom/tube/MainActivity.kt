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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                    if (activeVideo != null) {
                        PlayerScreen(
                            video = activeVideo!!,
                            repository = repository,
                            onBackClick = { activeVideo = null },
                            onPlayNextVideo = { nextVideo ->
                                activeVideo = nextVideo
                            }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Crossfade(targetState = currentTab, label = "tab_crossfade") { tab ->
                                when (tab) {
                                    NavTab.HOME -> HomeScreen(
                                        repository = repository,
                                        onVideoClick = { activeVideo = it },
                                        onSearchClick = { currentTab = NavTab.SEARCH }
                                    )
                                    NavTab.SEARCH -> SearchScreen(
                                        repository = repository,
                                        onVideoClick = { activeVideo = it },
                                        onBackClick = { currentTab = NavTab.HOME }
                                    )
                                    NavTab.HISTORY -> HistoryScreen(
                                        repository = repository,
                                        onVideoClick = { activeVideo = it }
                                    )
                                    NavTab.LIBRARY -> LibraryScreen(
                                        repository = repository,
                                        onVideoClick = { activeVideo = it }
                                    )
                                }
                            }

                            if (!isInPipMode) {
                                LiquidGlassBottomNav(
                                    currentTab = currentTab,
                                    onTabSelected = { currentTab = it },
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                )
                            }
                        }
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
