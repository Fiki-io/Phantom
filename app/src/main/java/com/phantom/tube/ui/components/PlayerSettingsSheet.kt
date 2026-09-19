package com.phantom.tube.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phantom.tube.core.theme.GlassBorderLight
import com.phantom.tube.core.theme.NeonCyan
import com.phantom.tube.core.theme.NeonPink
import com.phantom.tube.core.theme.ObsidianSurfaceLight
import com.phantom.tube.core.theme.TextMuted
import com.phantom.tube.core.theme.TextPrimary
import com.phantom.tube.core.theme.TextSecondary
import com.phantom.tube.core.theme.liquidGlass

enum class SettingsSheetPage {
    MAIN,
    QUALITY,
    SPEED,
    SLEEP_TIMER
}

enum class SleepTimerOption(val label: String, val seconds: Int?) {
    OFF("Nonaktif", null),
    MINUTES_15("15 Menit", 15 * 60),
    MINUTES_30("30 Menit", 30 * 60),
    MINUTES_45("45 Menit", 45 * 60),
    MINUTES_60("60 Menit", 60 * 60),
    END_OF_VIDEO("Akhir Video Ini", -1)
}

data class VideoQualityOption(val code: String, val label: String)

val AVAILABLE_QUALITIES = listOf(
    VideoQualityOption("auto", "Otomatis"),
    VideoQualityOption("hd1080", "1080p (FHD)"),
    VideoQualityOption("hd720", "720p (HD)"),
    VideoQualityOption("large", "480p"),
    VideoQualityOption("medium", "360p"),
    VideoQualityOption("small", "240p"),
    VideoQualityOption("tiny", "144p")
)

val AVAILABLE_SPEEDS = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PlayerSettingsSheet(
    visible: Boolean,
    isFullscreen: Boolean,
    currentQuality: String,
    currentSpeed: Float,
    isLoopEnabled: Boolean,
    isAutoplayNext: Boolean,
    isAudioOnly: Boolean,
    sleepTimerRemainingSec: Int?,
    sleepTimerOption: SleepTimerOption,
    onDismiss: () -> Unit,
    onQualitySelected: (String) -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onSleepTimerSelected: (SleepTimerOption) -> Unit,
    onLoopToggle: (Boolean) -> Unit,
    onAutoplayToggle: (Boolean) -> Unit,
    onAudioOnlyToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPage by remember { mutableStateOf(SettingsSheetPage.MAIN) }

    LaunchedEffect(visible) {
        if (visible) {
            currentPage = SettingsSheetPage.MAIN
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it },
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { onDismiss() }
        ) {
            val sheetModifier = if (isFullscreen) {
                Modifier
                    .fillMaxHeight(0.88f)
                    .width(420.dp)
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.68f)
                    .align(Alignment.BottomCenter)
            }

            Box(
                modifier = sheetModifier
                    .liquidGlass(
                        shape = if (isFullscreen) RoundedCornerShape(24.dp) else RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                        borderWidth = 1.dp,
                        tintColor = Color(0xFF0F121C),
                        glassAlpha = 0.96f
                    )
                    .clickable(enabled = false) {}
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (!isFullscreen) {
                        // Drag Handle
                        Box(
                            modifier = Modifier
                                .size(width = 44.dp, height = 4.dp)
                                .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                                .align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
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
                                    currentQuality = currentQuality,
                                    currentSpeed = currentSpeed,
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
                                    onSelectQuality = {
                                        onQualitySelected(it)
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
    currentQuality: String,
    currentSpeed: Float,
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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pengaturan Pemutar",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            LiquidGlassIconButton(
                icon = Icons.Default.Close,
                contentDescription = "Tutup",
                size = 36.dp,
                iconSize = 18.dp,
                onClick = onClose
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val qualityLabel = AVAILABLE_QUALITIES.find { it.code == currentQuality }?.label ?: "Otomatis"
            item {
                SettingsNavigationRow(
                    icon = Icons.Default.HighQuality,
                    title = "Kualitas Video",
                    subtitle = qualityLabel,
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
                    title = "Pengatur Waktu Tidur",
                    subtitle = sleepSubtitle,
                    onClick = { onNavigate(SettingsSheetPage.SLEEP_TIMER) }
                )
            }

            item {
                Divider(
                    color = Color.White.copy(alpha = 0.08f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                SettingsSwitchRow(
                    icon = Icons.Default.Repeat,
                    title = "Ulangi Video Ini (Loop)",
                    subtitle = "Otomatis memutar kembali video yang sama dari awal",
                    checked = isLoopEnabled,
                    onCheckedChange = onLoopToggle
                )
            }

            item {
                SettingsSwitchRow(
                    icon = Icons.Default.PlayCircle,
                    title = "Putar Otomatis",
                    subtitle = "Lanjut putar video rekomendasi berikutnya saat video selesai",
                    checked = isAutoplayNext,
                    onCheckedChange = onAutoplayToggle
                )
            }

            item {
                SettingsSwitchRow(
                    icon = Icons.Default.Headphones,
                    title = "Mode Audio Only",
                    subtitle = "Tampilan visual AMOLED hitam pekat untuk menghemat daya",
                    checked = isAudioOnly,
                    onCheckedChange = onAudioOnlyToggle
                )
            }
        }
    }
}

@Composable
private fun QualitySettingsContent(
    currentQuality: String,
    onSelectQuality: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiquidGlassIconButton(
                icon = Icons.Default.ArrowBack,
                contentDescription = "Kembali",
                size = 36.dp,
                iconSize = 18.dp,
                onClick = onBack
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Pilih Kualitas Video",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "Kualitas terbaik akan diprioritaskan saat koneksi Anda stabil.",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 6.dp, top = 6.dp, bottom = 12.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(AVAILABLE_QUALITIES) { option ->
                val isSelected = option.code == currentQuality || (currentQuality.isBlank() && option.code == "auto")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(14.dp),
                            borderWidth = if (isSelected) 1.2.dp else 0.6.dp,
                            glassAlpha = if (isSelected) 0.8f else 0.35f,
                            accentGlow = if (isSelected) NeonCyan else null
                        )
                        .clickable { onSelectQuality(option.code) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option.label,
                            color = if (isSelected) NeonCyan else TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Terpilih",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
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
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiquidGlassIconButton(
                icon = Icons.Default.ArrowBack,
                contentDescription = "Kembali",
                size = 36.dp,
                iconSize = 18.dp,
                onClick = onBack
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Kecepatan Pemutaran",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(AVAILABLE_SPEEDS) { speed ->
                val isSelected = (speed == currentSpeed)
                val label = if (speed == 1.0f) "1.0x (Normal)" else "${speed}x"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(14.dp),
                            borderWidth = if (isSelected) 1.2.dp else 0.6.dp,
                            glassAlpha = if (isSelected) 0.8f else 0.35f,
                            accentGlow = if (isSelected) NeonCyan else null
                        )
                        .clickable { onSelectSpeed(speed) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) NeonCyan else TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Terpilih",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
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
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiquidGlassIconButton(
                icon = Icons.Default.ArrowBack,
                contentDescription = "Kembali",
                size = 36.dp,
                iconSize = 18.dp,
                onClick = onBack
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Pengatur Waktu Tidur",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (remainingSec != null && remainingSec > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(14.dp),
                        borderWidth = 1.dp,
                        tintColor = Color(0xFF1B2338),
                        glassAlpha = 0.85f,
                        accentGlow = NeonCyan
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Timer Berjalan",
                            color = NeonCyan,
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
                    LiquidGlassIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Batalkan Timer",
                        size = 32.dp,
                        iconSize = 16.dp,
                        tint = NeonPink,
                        onClick = { onSelectOption(SleepTimerOption.OFF) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(SleepTimerOption.values()) { option ->
                val isSelected = option == currentOption
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(14.dp),
                            borderWidth = if (isSelected) 1.2.dp else 0.6.dp,
                            glassAlpha = if (isSelected) 0.8f else 0.35f,
                            accentGlow = if (isSelected) NeonCyan else null
                        )
                        .clickable { onSelectOption(option) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option.label,
                            color = if (isSelected) NeonCyan else TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Terpilih",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(14.dp),
                borderWidth = 0.6.dp,
                glassAlpha = 0.35f
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(14.dp),
                borderWidth = 0.6.dp,
                glassAlpha = 0.35f
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) NeonCyan else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = NeonCyan,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = ObsidianSurfaceLight,
                    uncheckedBorderColor = Color.White.copy(alpha = 0.15f)
                )
            )
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
