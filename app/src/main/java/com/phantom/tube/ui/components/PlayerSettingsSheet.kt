package com.phantom.tube.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phantom.tube.core.theme.TextMuted
import com.phantom.tube.core.theme.TextPrimary
import com.phantom.tube.core.theme.TextSecondary
import com.phantom.tube.core.theme.YouTubeRed
import kotlin.math.roundToInt

enum class SettingsSheetPage {
    MAIN,
    QUALITY,
    DOUBLE_TAP_SEEK,
    SPEED,
    SLEEP_TIMER
}

data class VideoQualityOption(
    val code: String,
    val label: String
)

val ALL_STANDARD_QUALITIES = listOf(
    VideoQualityOption("auto", "Otomatis (Disarankan)"),
    VideoQualityOption("highres", "Kualitas Tertinggi (4K / 2K)"),
    VideoQualityOption("hd2160", "2160p (4K UHD)"),
    VideoQualityOption("hd1440", "1440p (QHD)"),
    VideoQualityOption("hd1080", "1080p (Full HD)"),
    VideoQualityOption("hd720", "720p (HD)"),
    VideoQualityOption("large", "480p"),
    VideoQualityOption("medium", "360p"),
    VideoQualityOption("small", "240p"),
    VideoQualityOption("tiny", "144p (Hemat Kuota)")
)

enum class SleepTimerOption(val label: String, val seconds: Int?) {
    OFF("Nonaktif", null),
    MINUTES_15("15 Menit", 15 * 60),
    MINUTES_30("30 Menit", 30 * 60),
    MINUTES_45("45 Menit", 45 * 60),
    MINUTES_60("60 Menit", 60 * 60),
    END_OF_VIDEO("Di akhir video", -1)
}

val AVAILABLE_SEEK_DURATIONS = listOf(5, 10, 15, 20, 30)

val AVAILABLE_SPEEDS = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

/**
 * Standard YouTube Player Settings Bottom Sheet.
 * Clean, flat list UI without bulky card boxes, with transparent backdrop so the video
 * continues playing unobstructed at the top of the screen.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PlayerSettingsSheet(
    visible: Boolean,
    isFullscreen: Boolean,
    currentSpeed: Float,
    currentQuality: String = "auto",
    availableQualities: List<String> = emptyList(),
    doubleTapSeekSeconds: Int,
    isLoopEnabled: Boolean,
    isAutoplayNext: Boolean,
    isAudioOnly: Boolean,
    sleepTimerRemainingSec: Int?,
    sleepTimerOption: SleepTimerOption,
    onDismiss: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onQualitySelected: (String) -> Unit = {},
    onDoubleTapSeekSelected: (Int) -> Unit,
    onSleepTimerSelected: (SleepTimerOption) -> Unit,
    onLoopToggle: (Boolean) -> Unit,
    onAutoplayToggle: (Boolean) -> Unit,
    onAudioOnlyToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPage by remember { mutableStateOf(SettingsSheetPage.MAIN) }
    var sheetOffsetY by remember { mutableFloatStateOf(0f) }
    val animatedOffsetY by animateFloatAsState(
        targetValue = sheetOffsetY,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "settings_sheet_offset"
    )

    LaunchedEffect(visible) {
        if (visible) {
            currentPage = SettingsSheetPage.MAIN
            sheetOffsetY = 0f
        }
    }

    val density = LocalDensity.current
    val dismissThresholdPx = remember(density) { with(density) { 90.dp.toPx() } }

    val handleDragModifier = Modifier.pointerInput(Unit) {
        detectVerticalDragGestures(
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                sheetOffsetY = (sheetOffsetY + dragAmount).coerceAtLeast(0f)
            },
            onDragEnd = {
                if (sheetOffsetY > dismissThresholdPx) {
                    onDismiss()
                } else {
                    sheetOffsetY = 0f
                }
            },
            onDragCancel = {
                sheetOffsetY = 0f
            }
        )
    }

    val nestedScrollConnection = remember(dismissThresholdPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (sheetOffsetY > 0f && available.y < 0f) {
                    val consumed = available.y.coerceAtLeast(-sheetOffsetY)
                    sheetOffsetY += consumed
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f) {
                    sheetOffsetY = (sheetOffsetY + available.y).coerceAtLeast(0f)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (sheetOffsetY > dismissThresholdPx || available.y > 800f) {
                    onDismiss()
                } else {
                    sheetOffsetY = 0f
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (sheetOffsetY > dismissThresholdPx || available.y > 800f) {
                    onDismiss()
                } else {
                    sheetOffsetY = 0f
                }
                return Velocity.Zero
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it },
        modifier = modifier.fillMaxSize()
    ) {
        // Scrim background: tap empty area to dismiss. Transparent so top video is unobstructed!
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
        ) {
            val sheetModifier = if (isFullscreen) {
                Modifier
                    .fillMaxHeight(0.88f)
                    .width(380.dp)
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.70f)
                    .align(Alignment.BottomCenter)
            }

            val sheetShape = if (isFullscreen) RoundedCornerShape(16.dp) else RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            Box(
                modifier = sheetModifier
                    .offset { IntOffset(0, animatedOffsetY.roundToInt()) }
                    .clip(sheetShape)
                    .background(Color(0xFF212121))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
                    .nestedScroll(nestedScrollConnection)
                    .navigationBarsPadding()
                    .padding(top = 8.dp, bottom = 12.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (!isFullscreen) {
                        // Drag Handle
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(handleDragModifier)
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 38.dp, height = 4.dp)
                                    .background(Color.White.copy(alpha = 0.35f), RoundedCornerShape(2.dp))
                            )
                        }
                    }

                    AnimatedContent(
                        targetState = currentPage,
                        transitionSpec = {
                            if (targetState != SettingsSheetPage.MAIN) {
                                slideInHorizontally { it } + fadeIn() with
                                        slideOutHorizontally { -it } + fadeOut()
                            } else {
                                slideInHorizontally { -it } + fadeIn() with
                                        slideOutHorizontally { it } + fadeOut()
                            }
                        },
                        label = "settings_page_transition"
                    ) { page ->
                        when (page) {
                            SettingsSheetPage.MAIN -> {
                                MainSettingsContent(
                                    currentSpeed = currentSpeed,
                                    currentQuality = currentQuality,
                                    doubleTapSeekSeconds = doubleTapSeekSeconds,
                                    isLoopEnabled = isLoopEnabled,
                                    isAutoplayNext = isAutoplayNext,
                                    isAudioOnly = isAudioOnly,
                                    sleepTimerRemainingSec = sleepTimerRemainingSec,
                                    sleepTimerOption = sleepTimerOption,
                                    onNavigate = { currentPage = it },
                                    onLoopToggle = onLoopToggle,
                                    onAutoplayToggle = onAutoplayToggle,
                                    onAudioOnlyToggle = onAudioOnlyToggle,
                                    onClose = onDismiss
                                )
                            }
                            SettingsSheetPage.QUALITY -> {
                                QualitySettingsContent(
                                    currentQuality = currentQuality,
                                    availableQualities = availableQualities,
                                    onSelectQuality = {
                                        onQualitySelected(it)
                                        currentPage = SettingsSheetPage.MAIN
                                    },
                                    onBack = { currentPage = SettingsSheetPage.MAIN }
                                )
                            }
                            SettingsSheetPage.DOUBLE_TAP_SEEK -> {
                                DoubleTapSeekSettingsContent(
                                    currentSeconds = doubleTapSeekSeconds,
                                    onSelectSeconds = {
                                        onDoubleTapSeekSelected(it)
                                        currentPage = SettingsSheetPage.MAIN
                                    },
                                    onBack = { currentPage = SettingsSheetPage.MAIN }
                                )
                            }
                            SettingsSheetPage.SPEED -> {
                                SpeedSettingsContent(
                                    currentSpeed = currentSpeed,
                                    onSelectSpeed = {
                                        onSpeedSelected(it)
                                        currentPage = SettingsSheetPage.MAIN
                                    },
                                    onBack = { currentPage = SettingsSheetPage.MAIN }
                                )
                            }
                            SettingsSheetPage.SLEEP_TIMER -> {
                                SleepTimerSettingsContent(
                                    currentOption = sleepTimerOption,
                                    remainingSec = sleepTimerRemainingSec,
                                    onSelectOption = {
                                        onSleepTimerSelected(it)
                                        currentPage = SettingsSheetPage.MAIN
                                    },
                                    onBack = { currentPage = SettingsSheetPage.MAIN }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainSettingsContent(
    currentSpeed: Float,
    currentQuality: String,
    doubleTapSeekSeconds: Int,
    isLoopEnabled: Boolean,
    isAutoplayNext: Boolean,
    isAudioOnly: Boolean,
    sleepTimerRemainingSec: Int?,
    sleepTimerOption: SleepTimerOption,
    onNavigate: (SettingsSheetPage) -> Unit,
    onLoopToggle: (Boolean) -> Unit,
    onAutoplayToggle: (Boolean) -> Unit,
    onAudioOnlyToggle: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Setelan",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Tutup",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Divider(
            color = Color(0xFF2E2E2E),
            thickness = 1.dp,
            modifier = Modifier.padding(bottom = 2.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            val qualitySubtitle = when (currentQuality) {
                "auto" -> "Otomatis"
                "highres" -> "2160p / 1440p"
                "hd2160" -> "2160p (4K)"
                "hd1440" -> "1440p (2K)"
                "hd1080" -> "1080p (FHD)"
                "hd720" -> "720p (HD)"
                "large" -> "480p"
                "medium" -> "360p"
                "small" -> "240p"
                "tiny" -> "144p"
                else -> currentQuality
            }
            item {
                SettingsNavigationRow(
                    icon = Icons.Default.HighQuality,
                    title = "Kualitas Video",
                    subtitle = qualitySubtitle,
                    onClick = { onNavigate(SettingsSheetPage.QUALITY) }
                )
            }

            val speedLabel = if (currentSpeed == 1.0f) "1.0x (Normal)" else "${currentSpeed}x"
            item {
                SettingsNavigationRow(
                    icon = Icons.Default.Speed,
                    title = "Kecepatan Pemutaran",
                    subtitle = speedLabel,
                    onClick = { onNavigate(SettingsSheetPage.SPEED) }
                )
            }

            val sleepSubtitle = if (sleepTimerRemainingSec != null && sleepTimerRemainingSec > 0) {
                "Aktif (${formatSeconds(sleepTimerRemainingSec.toLong())} tersisa)"
            } else {
                sleepTimerOption.label
            }
            item {
                SettingsNavigationRow(
                    icon = Icons.Default.Bedtime,
                    title = "Timer tidur",
                    subtitle = sleepSubtitle,
                    onClick = { onNavigate(SettingsSheetPage.SLEEP_TIMER) }
                )
            }

            item {
                SettingsNavigationRow(
                    icon = Icons.Default.FastForward,
                    title = "Ketuk dua kali untuk mencari",
                    subtitle = "$doubleTapSeekSeconds Detik",
                    onClick = { onNavigate(SettingsSheetPage.DOUBLE_TAP_SEEK) }
                )
            }

            item {
                Divider(
                    color = Color(0xFF2E2E2E),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            item {
                SettingsSwitchRow(
                    icon = Icons.Default.Repeat,
                    title = "Putar ulang video",
                    subtitle = "Putar video secara terus-menerus",
                    checked = isLoopEnabled,
                    onCheckedChange = onLoopToggle
                )
            }

            item {
                SettingsSwitchRow(
                    icon = Icons.Default.PlayCircle,
                    title = "Putar otomatis",
                    subtitle = "Putar video berikutnya secara otomatis",
                    checked = isAutoplayNext,
                    onCheckedChange = onAutoplayToggle
                )
            }

            item {
                SettingsSwitchRow(
                    icon = Icons.Default.Headphones,
                    title = "Mode audio saja",
                    subtitle = "Matikan video untuk menghemat daya",
                    checked = isAudioOnly,
                    onCheckedChange = onAudioOnlyToggle
                )
            }
        }
    }
}

@Composable
private fun DoubleTapSeekSettingsContent(
    currentSeconds: Int,
    onSelectSeconds: (Int) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Kembali",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Ketuk dua kali untuk mencari",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Divider(
            color = Color(0xFF2E2E2E),
            thickness = 1.dp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(AVAILABLE_SEEK_DURATIONS) { seconds ->
                val isSelected = seconds == currentSeconds
                val label = if (seconds == 10) "$seconds Detik (Bawaan)" else "$seconds Detik"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectSeconds(seconds) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) YouTubeRed else TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Terpilih",
                            tint = YouTubeRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedSettingsContent(
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Kembali",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Kecepatan Pemutaran",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Divider(
            color = Color(0xFF2E2E2E),
            thickness = 1.dp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(AVAILABLE_SPEEDS) { speed ->
                val isSelected = (speed == currentSpeed)
                val label = if (speed == 1.0f) "1.0x (Normal)" else "${speed}x"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectSpeed(speed) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) YouTubeRed else TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Terpilih",
                            tint = YouTubeRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepTimerSettingsContent(
    currentOption: SleepTimerOption,
    remainingSec: Int?,
    onSelectOption: (SleepTimerOption) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Kembali",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Timer tidur",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Divider(
            color = Color(0xFF2E2E2E),
            thickness = 1.dp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        if (remainingSec != null && remainingSec > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2C2C2C))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Timer tidur aktif",
                            color = YouTubeRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Berhenti dalam ${formatSeconds(remainingSec.toLong())}",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = { onSelectOption(SleepTimerOption.OFF) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Batalkan Timer",
                            tint = YouTubeRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(SleepTimerOption.values()) { option ->
                val isSelected = option == currentOption
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectOption(option) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = option.label,
                        color = if (isSelected) YouTubeRed else TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Terpilih",
                            tint = YouTubeRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = YouTubeRed,
                uncheckedThumbColor = Color(0xFF9E9E9E),
                uncheckedTrackColor = Color(0xFF383838),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun QualitySettingsContent(
    currentQuality: String,
    availableQualities: List<String>,
    onSelectQuality: (String) -> Unit,
    onBack: () -> Unit
) {
    val options = remember(availableQualities) {
        if (availableQualities.isNotEmpty()) {
            val list = mutableListOf(VideoQualityOption("auto", "Otomatis (Disarankan)"))
            val availableSet = availableQualities.toSet()
            ALL_STANDARD_QUALITIES.filter { it.code != "auto" && availableSet.contains(it.code) }.forEach {
                list.add(it)
            }
            if (list.size == 1) {
                ALL_STANDARD_QUALITIES
            } else {
                list
            }
        } else {
            ALL_STANDARD_QUALITIES
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Kembali",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Kualitas Video",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Divider(
            color = Color(0xFF2E2E2E),
            thickness = 1.dp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(options) { opt ->
                val isSelected = (opt.code == currentQuality)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectQuality(opt.code) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = opt.label,
                        color = if (isSelected) YouTubeRed else TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Terpilih",
                            tint = YouTubeRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatSeconds(totalSec: Long): String {
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
