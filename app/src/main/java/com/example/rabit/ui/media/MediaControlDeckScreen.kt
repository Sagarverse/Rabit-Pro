package com.example.rabit.ui.media

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.AccentBlue
import com.example.rabit.ui.theme.BorderColor
import com.example.rabit.ui.theme.Graphite
import com.example.rabit.ui.theme.Obsidian
import com.example.rabit.ui.theme.Platinum
import com.example.rabit.ui.theme.Silver
import com.example.rabit.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaControlDeckScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val title by viewModel.nowPlayingTitle.collectAsState()
    val artist by viewModel.nowPlayingArtist.collectAsState()
    val album by viewModel.nowPlayingAlbum.collectAsState()
    val artworkBase64 by viewModel.nowPlayingArtworkBase64.collectAsState()
    val p2pStatus by viewModel.p2pStatus.collectAsState("Disconnected")
    val connectionState by viewModel.connectionState.collectAsState()
    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        contentVisible = true
    }

    val artwork = remember(artworkBase64) {
        try {
            artworkBase64?.let {
                val bytes = Base64.decode(it, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        } catch (_: Exception) {
            null
        }
    }

    Scaffold(
        containerColor = Obsidian
    ) { padding ->
    AnimatedVisibility(
        visible = contentVisible,
        enter = fadeIn(animationSpec = tween(320)) + slideInVertically(initialOffsetY = { it / 14 }, animationSpec = tween(320))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Obsidian)
                .padding(20.dp)
        ) {
        Surface(
            color = Graphite.copy(alpha = 0.45f),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (artwork != null) {
                    Image(
                        bitmap = artwork,
                        contentDescription = "Album art",
                        modifier = Modifier.size(180.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .background(Graphite, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Silver, modifier = Modifier.size(60.dp))
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text(title, color = Platinum, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(artist, color = Silver, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (album.isNotBlank()) {
                    Text(album, color = Silver.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Bluetooth: ${if (connectionState is HidDeviceManager.ConnectionState.Connected) "Connected" else "Disconnected"} • P2P: $p2pStatus",
                    color = Silver.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Graphite.copy(alpha = 0.55f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (connectionState is HidDeviceManager.ConnectionState.Connected) "Hardware Link Ready" else "Connect via Bluetooth for full control",
                        color = if (connectionState is HidDeviceManager.ConnectionState.Connected) SuccessGreen else Silver,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.requestNowPlayingFromHost() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue.copy(alpha = 0.85f))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Refresh Now Playing")
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaControlButton(icon = Icons.Default.Remove, label = "Vol -", onClick = { viewModel.sendMediaVolumeDown() })
            MediaControlButton(icon = Icons.Default.FastRewind, label = "Prev", onClick = { viewModel.sendMediaPreviousTrack() })
            Surface(
                shape = CircleShape,
                color = AccentBlue,
                modifier = Modifier.size(84.dp)
            ) {
                IconButton(onClick = { viewModel.sendMediaPlayPause() }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play/Pause", tint = Platinum, modifier = Modifier.size(42.dp))
                }
            }
            MediaControlButton(icon = Icons.Default.FastForward, label = "Next", onClick = { viewModel.sendMediaNextTrack() })
            MediaControlButton(icon = Icons.Default.Add, label = "Vol +", onClick = { viewModel.sendMediaVolumeUp() })
        }

        Spacer(modifier = Modifier.height(18.dp))

        Surface(
            color = Graphite.copy(alpha = 0.45f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("How it works", color = Platinum, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text("• Volume & Playback controls work instantly via standard Bluetooth.", color = Silver, fontSize = 12.sp)
                Text("• For live metadata/artwork, run the desktop helper on your Mac:\n   python3 desktop-helper/rabit_desktop_helper.py", color = Silver, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Companion metadata format: {\"type\":\"NOW_PLAYING\",\"title\":\"...\",\"artist\":\"...\",\"album\":\"...\",\"artworkBase64\":\"...\"}",
                    color = Silver.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
        }
    }
    }
}

@Composable
private fun MediaControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = CircleShape, color = Graphite, modifier = Modifier.size(58.dp)) {
            IconButton(onClick = onClick) {
                Icon(icon, contentDescription = label, tint = Platinum)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Silver, fontSize = 11.sp)
    }
}
