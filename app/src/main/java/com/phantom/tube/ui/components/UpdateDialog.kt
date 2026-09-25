package com.phantom.tube.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.phantom.tube.core.theme.CrimsonPrimary
import com.phantom.tube.core.theme.ObsidianSurface
import com.phantom.tube.core.theme.TextMuted
import com.phantom.tube.core.theme.TextPrimary
import com.phantom.tube.core.theme.TextSecondary
import com.phantom.tube.core.update.AppUpdateInfo

sealed class UpdateDialogState {
    data class Available(val info: AppUpdateInfo) : UpdateDialogState()
    data class Downloading(val info: AppUpdateInfo, val progress: Float) : UpdateDialogState()
    data class PermissionRequired(val onOpenSettings: () -> Unit) : UpdateDialogState()
    data class Error(val message: String, val onRetry: () -> Unit) : UpdateDialogState()
}

@Composable
fun UpdateDialog(
    state: UpdateDialogState,
    onDismiss: () -> Unit,
    onStartDownload: (AppUpdateInfo) -> Unit
) {
    val isCancellable = state !is UpdateDialogState.Downloading

    Dialog(
        onDismissRequest = {
            if (isCancellable) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = isCancellable,
            dismissOnClickOutside = isCancellable
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = ObsidianSurface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(CrimsonPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state is UpdateDialogState.Downloading) Icons.Default.Download else Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (state) {
                    is UpdateDialogState.Available -> {
                        Text(
                            text = "Pembaruan Tersedia",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Versi ${state.info.versionName} kini telah dirilis",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Changelog Box
                        if (state.info.changelog.isNotBlank()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 160.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF141416)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(14.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = "Catatan Pembaruan:",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = CrimsonPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = state.info.changelog,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextPrimary,
                                            lineHeight = 18.sp
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("Nanti", color = TextMuted)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onStartDownload(state.info) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CrimsonPrimary,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Perbarui Sekarang", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    is UpdateDialogState.Downloading -> {
                        Text(
                            text = "Mengunduh Pembaruan...",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val percent = (state.progress * 100).toInt()
                        Text(
                            text = if (state.progress > 0f) "$percent% selesai" else "Menyiapkan unduhan...",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        if (state.progress > 0f) {
                            LinearProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = CrimsonPrimary,
                                trackColor = Color(0xFF26262B)
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = CrimsonPrimary,
                                trackColor = Color(0xFF26262B)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Aplikasi akan otomatis menginstal setelah unduhan selesai.",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                        )
                    }

                    is UpdateDialogState.PermissionRequired -> {
                        Text(
                            text = "Izin Instalasi Diperlukan",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Untuk memperbarui Phantom secara otomatis, izinkan instalasi aplikasi dari sumber ini pada pengaturan perangkat Anda.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("Batal", color = TextMuted)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = state.onOpenSettings,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CrimsonPrimary,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Buka Pengaturan", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    is UpdateDialogState.Error -> {
                        Text(
                            text = "Pembaruan Gagal",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("Tutup", color = TextMuted)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = state.onRetry,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CrimsonPrimary,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Coba Lagi", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
