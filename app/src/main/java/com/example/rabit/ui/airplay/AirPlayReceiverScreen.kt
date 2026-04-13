package com.example.rabit.ui.airplay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

@Composable
fun AirPlayReceiverScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val enabled by viewModel.airPlayReceiverEnabled.collectAsState()
    val status by viewModel.airPlayStatus.collectAsState()
    val wifiAudioStatus by viewModel.wifiAudioStatus.collectAsState()
    val wifiAudioActive by viewModel.wifiAudioStreamActive.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
            .padding(20.dp),
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
                    Text("AirPlay Receiver (Experimental)", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Text(
                    "Advertises Rabit via _raop._tcp so your Mac can discover it in Sound output on the same Wi-Fi.",
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
            }
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Graphite.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Silver)
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text("How to use", color = Platinum, fontWeight = FontWeight.Bold)
                }
                Text("1. Connect Mac and phone to same Wi-Fi.", color = Silver, fontSize = 12.sp)
                Text("2. Enable receiver above.", color = Silver, fontSize = 12.sp)
                Text("3. On Mac: Control Center -> Sound -> select Rabit.", color = Silver, fontSize = 12.sp)
                Text("4. Start playback from Mac.", color = Silver, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Note: This build includes RAOP discovery/service lifecycle scaffold. Full lossless ALAC decode path requires an embedded RAOP/Shairport-compatible engine.",
                    color = Silver.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Fallback Wi-Fi PCM stream: ${if (wifiAudioActive) "ACTIVE" else "IDLE"}",
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

        Button(
            onClick = { if (enabled) viewModel.stopAirPlayReceiver() else viewModel.startAirPlayReceiver() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
        ) {
            Text(if (enabled) "Stop AirPlay Receiver" else "Start AirPlay Receiver")
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
