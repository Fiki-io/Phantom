package com.phantom.tube.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phantom.tube.core.theme.ObsidianDark
import com.phantom.tube.core.theme.TextMuted
import com.phantom.tube.core.theme.TextPrimary
import com.phantom.tube.core.theme.TextSecondary
import com.phantom.tube.core.theme.YouTubeRed
import com.phantom.tube.core.theme.YouTubeSurface
import com.phantom.tube.data.repository.PhantomRepository
import com.phantom.tube.data.settings.PhantomPreferences
import com.phantom.tube.ui.components.PhantomIconButton
import kotlinx.coroutines.launch

private data class RegionOption(val code: String, val name: String, val flag: String)

private val REGION_OPTIONS = listOf(
    RegionOption("ID", "Indonesia", "🇮🇩"),
    RegionOption("GLOBAL", "Global / Internasional", "🌐"),
    RegionOption("US", "Amerika Serikat", "🇺🇸"),
    RegionOption("JP", "Jepang", "🇯🇵"),
    RegionOption("KR", "Korea Selatan", "🇰🇷"),
    RegionOption("GB", "Inggris (UK)", "🇬🇧")
)

private data class QualityOption(val code: String, val label: String)

private val QUALITY_OPTIONS = listOf(
    QualityOption("auto", "Otomatis (Disarankan)"),
    QualityOption("hd1080", "1080p (Full HD)"),
    QualityOption("hd720", "720p (HD)"),
    QualityOption("large", "480p"),
    QualityOption("medium", "360p"),
    QualityOption("small", "240p")
)

private val SPEED_OPTIONS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

@Composable
fun SettingsScreen(
    preferences: PhantomPreferences,
    repository: PhantomRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Preferences states
    val defaultQuality by preferences.defaultQuality.collectAsState()
    val defaultSpeed by preferences.defaultSpeed.collectAsState()
    val bgPlayback by preferences.backgroundPlaybackEnabled.collectAsState()
    val autoPip by preferences.autoPipEnabled.collectAsState()

    val sbEnabled by preferences.sponsorBlockEnabled.collectAsState()
    val sbAutoSkip by preferences.sponsorBlockAutoSkip.collectAsState()
    val skipSponsor by preferences.skipSponsor.collectAsState()
    val skipSelfPromo by preferences.skipSelfPromo.collectAsState()
    val skipInteraction by preferences.skipInteraction.collectAsState()
    val skipIntro by preferences.skipIntro.collectAsState()
    val skipOutro by preferences.skipOutro.collectAsState()

    val contentCountry by preferences.contentCountry.collectAsState()
    val pauseWatchHistory by preferences.pauseWatchHistory.collectAsState()

    // Cache state
    var cacheSizeBytes by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        cacheSizeBytes = preferences.getCacheSizeBytes(context)
    }

    // Dialog states
    var showQualityDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showRegionDialog by remember { mutableStateOf(false) }
    var showClearWatchHistoryDialog by remember { mutableStateOf(false) }
    var showClearSearchHistoryDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDark)
    ) {
        // TOP APP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PhantomIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Kembali",
                size = 40.dp,
                iconSize = 22.dp,
                onClick = onBackClick
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Pengaturan",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // CONTENT LIST
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {

            // 1. PEMUTARAN & KUALITAS VIDEO
            SettingsSectionHeader(title = "Pemutaran & Video")

            SettingsCard {
                val currentQualityLabel = QUALITY_OPTIONS.firstOrNull { it.code == defaultQuality }?.label ?: defaultQuality
                SettingsItemClickable(
                    icon = Icons.Default.HighQuality,
                    title = "Kualitas Default Video",
                    subtitle = currentQualityLabel,
                    onClick = { showQualityDialog = true }
                )

                SettingsDivider()

                val currentSpeedLabel = if (defaultSpeed == 1.0f) "1.0x (Normal)" else "${defaultSpeed}x"
                SettingsItemClickable(
                    icon = Icons.Default.Speed,
                    title = "Kecepatan Putar Default",
                    subtitle = currentSpeedLabel,
                    onClick = { showSpeedDialog = true }
                )

                SettingsDivider()

                SettingsItemSwitch(
                    icon = Icons.Default.SurroundSound,
                    title = "Putar di Latar Belakang",
                    subtitle = "Audio terus berputar saat aplikasi diminimalkan atau layar mati",
                    checked = bgPlayback,
                    onCheckedChange = { preferences.setBackgroundPlaybackEnabled(it) }
                )

                SettingsDivider()

                SettingsItemSwitch(
                    icon = Icons.Default.PictureInPicture,
                    title = "PiP Otomatis",
                    subtitle = "Buka Picture-in-Picture secara otomatis saat menekan tombol Home",
                    checked = autoPip,
                    onCheckedChange = { preferences.setAutoPipEnabled(it) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. SPONSORBLOCK
            SettingsSectionHeader(title = "SponsorBlock (Lewati Segmen)")

            SettingsCard {
                SettingsItemSwitch(
                    icon = Icons.Default.Security,
                    title = "Aktifkan SponsorBlock",
                    subtitle = "Melewati iklan berbayar, promosi diri, dan intro secara pintar",
                    checked = sbEnabled,
                    onCheckedChange = { preferences.setSponsorBlockEnabled(it) }
                )

                AnimatedVisibility(visible = sbEnabled) {
                    Column {
                        SettingsDivider()

                        SettingsItemSwitch(
                            icon = Icons.Default.PlayArrow,
                            title = "Lewati Secara Otomatis",
                            subtitle = if (sbAutoSkip) "Segmen langsung di-skip tanpa konfirmasi" else "Tampilkan tombol lewati manual",
                            checked = sbAutoSkip,
                            onCheckedChange = { preferences.setSponsorBlockAutoSkip(it) }
                        )

                        SettingsDivider()

                        Text(
                            text = "Kategori yang di-skip:",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        SettingsSubItemSwitch(
                            title = "Sponsor / Iklan Sponsor",
                            checked = skipSponsor,
                            onCheckedChange = { preferences.setSkipSponsor(it) }
                        )

                        SettingsSubItemSwitch(
                            title = "Promosi Pribadi (Self-Promo)",
                            checked = skipSelfPromo,
                            onCheckedChange = { preferences.setSkipSelfPromo(it) }
                        )

                        SettingsSubItemSwitch(
                            title = "Interaksi (Pengingat Like & Subscribe)",
                            checked = skipInteraction,
                            onCheckedChange = { preferences.setSkipInteraction(it) }
                        )

                        SettingsSubItemSwitch(
                            title = "Intro / Cuplikan Pembuka",
                            checked = skipIntro,
                            onCheckedChange = { preferences.setSkipIntro(it) }
                        )

                        SettingsSubItemSwitch(
                            title = "Outro / Kartu Akhir",
                            checked = skipOutro,
                            onCheckedChange = { preferences.setSkipOutro(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. KONTEN & WILAYAH
            SettingsSectionHeader(title = "Konten & Wilayah")

            SettingsCard {
                val currentRegion = REGION_OPTIONS.firstOrNull { it.code == contentCountry }
                val regionLabel = if (currentRegion != null) "${currentRegion.flag} ${currentRegion.name}" else contentCountry
                SettingsItemClickable(
                    icon = Icons.Default.Language,
                    title = "Wilayah Rekomendasi & Trending",
                    subtitle = regionLabel,
                    onClick = { showRegionDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. PRIVASI & PENYIMPANAN
            SettingsSectionHeader(title = "Privasi & Penyimpanan")

            SettingsCard {
                SettingsItemSwitch(
                    icon = Icons.Default.PauseCircleOutline,
                    title = "Jeda Riwayat Tontonan",
                    subtitle = "Video baru yang ditonton tidak akan disimpan ke riwayat lokal",
                    checked = pauseWatchHistory,
                    onCheckedChange = { preferences.setPauseWatchHistory(it) }
                )

                SettingsDivider()

                SettingsItemClickable(
                    icon = Icons.Default.History,
                    title = "Hapus Riwayat Tontonan",
                    subtitle = "Bersihkan seluruh video yang pernah ditonton",
                    onClick = { showClearWatchHistoryDialog = true }
                )

                SettingsDivider()

                SettingsItemClickable(
                    icon = Icons.Default.DeleteOutline,
                    title = "Hapus Riwayat Pencarian",
                    subtitle = "Bersihkan semua rekaman kata kunci pencarian",
                    onClick = { showClearSearchHistoryDialog = true }
                )

                SettingsDivider()

                val formattedCache = formatFileSize(cacheSizeBytes)
                SettingsItemClickable(
                    icon = Icons.Default.CleaningServices,
                    title = "Bersihkan Cache Aplikasi",
                    subtitle = "Ukuran cache saat ini: $formattedCache",
                    onClick = { showClearCacheDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. TENTANG PHANTOM
            SettingsSectionHeader(title = "Tentang Aplikasi")

            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(YouTubeRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Phantom Tube",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Versi 2.0-clean (Client-Side InnerTube)",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                SettingsDivider()

                SettingsItemClickable(
                    icon = Icons.Default.OpenInNew,
                    title = "Repositori GitHub",
                    subtitle = "Fiki-io/Phantom • Proyek Sumber Terbuka",
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Fiki-io/Phantom"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Tidak dapat membuka peramban", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                SettingsDivider()

                SettingsItemClickable(
                    icon = Icons.Default.Info,
                    title = "Arsitektur Privasi",
                    subtitle = "Bebas pelacak, tanpa Google Play Services, pemrosesan data murni di perangkat Anda.",
                    onClick = {}
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    // ==========================================
    // DIALOGS & PICKERS
    // ==========================================

    // 1. Quality Picker Dialog
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text(text = "Kualitas Default Video", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    QUALITY_OPTIONS.forEach { opt ->
                        val isSelected = opt.code == defaultQuality
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    preferences.setDefaultQuality(opt.code)
                                    showQualityDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    preferences.setDefaultQuality(opt.code)
                                    showQualityDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = YouTubeRed)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = opt.label,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("Tutup", color = YouTubeRed)
                }
            },
            containerColor = YouTubeSurface
        )
    }

    // 2. Speed Picker Dialog
    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text(text = "Kecepatan Putar Default", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    SPEED_OPTIONS.forEach { speed ->
                        val isSelected = defaultSpeed == speed
                        val speedText = if (speed == 1.0f) "1.0x (Normal)" else "${speed}x"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    preferences.setDefaultSpeed(speed)
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    preferences.setDefaultSpeed(speed)
                                    showSpeedDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = YouTubeRed)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = speedText,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Tutup", color = YouTubeRed)
                }
            },
            containerColor = YouTubeSurface
        )
    }

    // 3. Region Picker Dialog
    if (showRegionDialog) {
        AlertDialog(
            onDismissRequest = { showRegionDialog = false },
            title = { Text(text = "Wilayah Rekomendasi", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    REGION_OPTIONS.forEach { reg ->
                        val isSelected = reg.code == contentCountry
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    preferences.setContentCountry(reg.code)
                                    showRegionDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    preferences.setContentCountry(reg.code)
                                    showRegionDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = YouTubeRed)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${reg.flag}  ${reg.name}",
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRegionDialog = false }) {
                    Text("Tutup", color = YouTubeRed)
                }
            },
            containerColor = YouTubeSurface
        )
    }

    // 4. Clear Watch History Dialog
    if (showClearWatchHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearWatchHistoryDialog = false },
            title = { Text(text = "Hapus Riwayat Tontonan?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Semua riwayat video yang pernah Anda tonton akan dihapus dari penyimpanan lokal.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.clearWatchHistory()
                            Toast.makeText(context, "Riwayat tontonan berhasil dibersihkan", Toast.LENGTH_SHORT).show()
                            showClearWatchHistoryDialog = false
                        }
                    }
                ) {
                    Text("Hapus", color = YouTubeRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearWatchHistoryDialog = false }) {
                    Text("Batal", color = TextSecondary)
                }
            },
            containerColor = YouTubeSurface
        )
    }

    // 5. Clear Search History Dialog
    if (showClearSearchHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearSearchHistoryDialog = false },
            title = { Text(text = "Hapus Riwayat Pencarian?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Semua saran kata kunci pencarian yang tersimpan akan dihapus.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.clearSearchHistory()
                            Toast.makeText(context, "Riwayat pencarian berhasil dibersihkan", Toast.LENGTH_SHORT).show()
                            showClearSearchHistoryDialog = false
                        }
                    }
                ) {
                    Text("Hapus", color = YouTubeRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearSearchHistoryDialog = false }) {
                    Text("Batal", color = TextSecondary)
                }
            },
            containerColor = YouTubeSurface
        )
    }

    // 6. Clear Cache Dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text(text = "Bersihkan Cache?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Menghapus thumbnail dan memori cache sementara (${formatFileSize(cacheSizeBytes)}). Aplikasi akan mengunduh ulang gambar sesuai kebutuhan.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        preferences.clearAppCache(context)
                        cacheSizeBytes = preferences.getCacheSizeBytes(context)
                        Toast.makeText(context, "Cache aplikasi berhasil dibersihkan", Toast.LENGTH_SHORT).show()
                        showClearCacheDialog = false
                    }
                ) {
                    Text("Bersihkan", color = YouTubeRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Batal", color = TextSecondary)
                }
            },
            containerColor = YouTubeSurface
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = YouTubeRed,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF161616))
            .border(1.dp, Color(0xFF262626), RoundedCornerShape(16.dp))
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFF222222))
    )
}

@Composable
private fun SettingsItemClickable(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF222222)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SettingsItemSwitch(
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF222222)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) YouTubeRed else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = YouTubeRed,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = Color(0xFF2B2B2B)
            )
        )
    }
}

@Composable
private fun SettingsSubItemSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(start = 28.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = if (checked) TextPrimary else TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = YouTubeRed,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = Color(0xFF2B2B2B)
            )
        )
    }
}

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val kb = size / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format("%.2f GB", gb)
        mb >= 1.0 -> String.format("%.1f MB", mb)
        kb >= 1.0 -> String.format("%.1f KB", kb)
        else -> "$size B"
    }
}
