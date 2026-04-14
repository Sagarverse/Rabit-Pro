package com.example.rabit.ui.airplay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.AccentBlue
import com.example.rabit.ui.theme.Graphite
import com.example.rabit.ui.theme.Obsidian
import com.example.rabit.ui.theme.Platinum
import com.example.rabit.ui.theme.Silver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirPlayReceiverScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val enabled by viewModel.airPlayReceiverEnabled.collectAsState()
    val status by viewModel.airPlayStatus.collectAsState()
    val diagnostics by viewModel.airPlayStatusLog.collectAsState()
    val nativeReadiness by viewModel.airPlayNativeReadiness.collectAsState()
    val handshakeStage by viewModel.airPlayHandshakeStage.collectAsState()
    val lastRtspMethod by viewModel.airPlayLastRtspMethod.collectAsState()
    val serverPorts by viewModel.airPlayServerPorts.collectAsState()
    val clientPorts by viewModel.airPlayClientPorts.collectAsState()
    val packetStats by viewModel.airPlayPacketStats.collectAsState()
    val alacCapability by viewModel.airPlayAlacCapability.collectAsState()
    val wifiAudioStatus by viewModel.wifiAudioStatus.collectAsState()
    val wifiAudioActive by viewModel.wifiAudioStreamActive.collectAsState()
    val localIp by viewModel.localIp.collectAsState()
    val webBridgeRunning by viewModel.isWebBridgeRunning.collectAsState()
    val webBridgePin by viewModel.webBridgePin.collectAsState()
    val webBridgeSelfTestStatus by viewModel.webBridgeSelfTestStatus.collectAsState()
    val webBridgeSelfTestInProgress by viewModel.webBridgeSelfTestInProgress.collectAsState()
    val autoFallbackEnabled by viewModel.airPlayAutoFallbackEnabled.collectAsState()
    val codecFallbackRequired =
        !status.contains("ALAC decode active", ignoreCase = true) && (
            status.contains("Unsupported RAOP codec", ignoreCase = true) ||
                status.contains("ALAC stream detected", ignoreCase = true) ||
                status.contains("AirPlay fallback required", ignoreCase = true)
            )
    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        contentVisible = true
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Graphite.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Speaker, contentDescription = null, tint = AccentBlue)
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text("AirPlay Discovery Preview", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Text(
                    "Advertises Hackie via _raop._tcp so your Mac can discover it in Sound output on the same Wi-Fi.",
                    color = Silver,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Receiver", color = Platinum, fontWeight = FontWeight.SemiBold)
                        Text(if (enabled) "Running" else "Stopped", color = Silver, fontSize = 12.sp)
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { if (it) viewModel.startAirPlayReceiver() else viewModel.stopAirPlayReceiver() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Platinum, checkedTrackColor = AccentBlue)
                    )
                }
                Text("Status: $status", color = Silver.copy(alpha = 0.8f), fontSize = 12.sp)
                Text(
                    "Native readiness: $nativeReadiness",
                    color = when (nativeReadiness) {
                        "MEDIUM" -> AccentBlue
                        "LOW-MEDIUM" -> Platinum
                        else -> Silver
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Experimental mode: discovery and native RAOP playback are enabled with best-effort ALAC decode.",
                    color = Silver.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                Text(
                    "Readiness increases as ANNOUNCE/SETUP/RECORD, decode activation, and RTP packet delivery are detected.",
                    color = Silver.copy(alpha = 0.72f),
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
                Text(
                    "ALAC decoder: $alacCapability",
                    color = if (alacCapability.startsWith("AVAILABLE")) AccentBlue else Silver,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Auto fallback", color = Platinum, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("Start Web Bridge automatically when native decode is unavailable", color = Silver, fontSize = 10.sp)
                    }
                    Switch(
                        checked = autoFallbackEnabled,
                        onCheckedChange = { viewModel.setAirPlayAutoFallbackEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Platinum, checkedTrackColor = AccentBlue)
                    )
                }
                if (codecFallbackRequired) {
                    Text(
                        if (webBridgeRunning) {
                            "Sender codec is not fully supported here. Web Bridge fallback is active for phone playback."
                        } else {
                            "Sender codec is not fully supported here. Start Web Bridge fallback below for phone speaker playback."
                        },
                        color = Platinum,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (codecFallbackRequired || webBridgeRunning || wifiAudioActive) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Graphite.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Silver)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Fallback", color = Platinum, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Use Web Bridge when native decode is unavailable.",
                        color = Silver,
                        fontSize = 12.sp
                    )
                    Button(
                        onClick = { if (!webBridgeRunning) viewModel.startWebBridge() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Text(if (webBridgeRunning) "Web Bridge Running" else "Start Web Bridge")
                    }
                    Text(
                        "Host: $localIp   PIN: $webBridgePin",
                        color = Silver,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Button(
                        onClick = { viewModel.runWebBridgeConnectivitySelfTest() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = webBridgeRunning && !webBridgeSelfTestInProgress,
                        colors = ButtonDefaults.buttonColors(containerColor = Graphite)
                    ) {
                        Text(if (webBridgeSelfTestInProgress) "Running Self-Test..." else "Run Bridge Self-Test")
                    }
                    Text(
                        "Self-test: $webBridgeSelfTestStatus",
                        color = Silver.copy(alpha = 0.88f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Text(
                        "Fallback stream: ${if (wifiAudioActive) "ACTIVE" else "IDLE"}",
                        color = if (wifiAudioActive) AccentBlue else Silver,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Status: $wifiAudioStatus",
                        color = Silver.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Graphite.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Playback Controls", color = Platinum, fontWeight = FontWeight.Bold)
                Text(
                    "Control host media directly from here while AirPlay is connected.",
                    color = Silver,
                    fontSize = 11.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.sendMediaPreviousTrack() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Graphite)
                    ) {
                        Icon(Icons.Default.KeyboardDoubleArrowLeft, contentDescription = null)
                    }
                    Button(
                        onClick = { viewModel.sendMediaPlayPause() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                    }
                    Button(
                        onClick = { viewModel.sendMediaNextTrack() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Graphite)
                    ) {
                        Icon(Icons.Default.KeyboardDoubleArrowRight, contentDescription = null)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.sendMediaVolumeDown() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Graphite)
                    ) {
                        Icon(Icons.Default.VolumeDown, contentDescription = null)
                        Spacer(modifier = Modifier.padding(2.dp))
                        Text("Vol -")
                    }
                    Button(
                        onClick = { viewModel.sendMediaVolumeUp() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Graphite)
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null)
                        Spacer(modifier = Modifier.padding(2.dp))
                        Text("Vol +")
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Graphite.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Session Trace", color = Platinum, fontWeight = FontWeight.Bold)
                Text(
                    "Handshake stage: $handshakeStage",
                    color = when (handshakeStage) {
                        "STREAMING" -> AccentBlue
                        "RECORDING", "SETUP", "ANNOUNCED" -> Platinum
                        "STALLED", "FAILED" -> Silver.copy(alpha = 0.9f)
                        else -> Silver
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text("Last RTSP method: $lastRtspMethod", color = Silver, fontSize = 11.sp)
                Text("Server ports: $serverPorts", color = Silver, fontSize = 11.sp)
                Text("Client ports: $clientPorts", color = Silver, fontSize = 11.sp)
                Text("RTP stats: $packetStats", color = Silver, fontSize = 11.sp)
            }
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Graphite.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Discovery Diagnostics", color = Platinum, fontWeight = FontWeight.Bold)
                Text("If Hackie is not visible in Mac AirPlay output, use Restart then re-open macOS output list.", color = Silver, fontSize = 11.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.restartAirPlayReceiver() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.padding(3.dp))
                        Text("Restart")
                    }

                    Button(
                        onClick = { viewModel.refreshAirPlayDecoderCapability() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Graphite)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(modifier = Modifier.padding(3.dp))
                        Text("Probe Decoder")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Button(
                        onClick = { viewModel.clearAirPlayStatusLog() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Graphite)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null)
                        Spacer(modifier = Modifier.padding(3.dp))
                        Text("Clear Log")
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp, max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(diagnostics) { line ->
                        Text(line, color = Silver, fontSize = 11.sp)
                    }
                }
            }
        }

        Button(
            onClick = { if (enabled) viewModel.stopAirPlayReceiver() else viewModel.startAirPlayReceiver() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
        ) {
            Text(if (enabled) "Stop Discovery Preview" else "Start Discovery Preview")
        }

        Button(
            onClick = { viewModel.playAirPlayTestTone() },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(containerColor = Graphite)
        ) {
            Text("Play Receiver Test Tone")
        }
        }
    }
    }
}
