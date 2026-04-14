package com.example.rabit.ui.automation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.AccentBlue
import com.example.rabit.ui.theme.BorderColor
import com.example.rabit.ui.theme.ErrorRed
import com.example.rabit.ui.theme.Graphite
import com.example.rabit.ui.theme.Obsidian
import com.example.rabit.ui.theme.Platinum
import com.example.rabit.ui.theme.Silver
import com.example.rabit.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WakeOnLanScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val mac by viewModel.wolMacAddress.collectAsState()
    val broadcast by viewModel.wolBroadcastIp.collectAsState()
    val port by viewModel.wolPort.collectAsState()
    val status by viewModel.wolStatus.collectAsState()

    var macInput by remember(mac) { mutableStateOf(mac) }
    var broadcastInput by remember(broadcast) { mutableStateOf(broadcast) }
    var portInput by remember(port) { mutableStateOf(port.toString()) }
    var contentVisible by remember { mutableStateOf(false) }
    val statusColor = when {
        status.startsWith("Success") -> SuccessGreen
        status.startsWith("Failed") -> ErrorRed
        else -> Silver
    }

    LaunchedEffect(Unit) {
        contentVisible = true
    }

    AnimatedVisibility(
        visible = contentVisible,
        enter = fadeIn(animationSpec = tween(320)) + slideInVertically(initialOffsetY = { it / 14 }, animationSpec = tween(320))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Graphite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Wake-on-LAN Setup (Step by Step)", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("1. Enable Wake-on-LAN in BIOS/UEFI on the target computer.", color = Silver, fontSize = 12.sp)
                Text("2. macOS: enable Wake for network access in Energy settings.", color = Silver, fontSize = 12.sp)
                Text("3. Windows: network adapter > Power Management > allow wake by magic packet.", color = Silver, fontSize = 12.sp)
                Text("4. Find the target MAC address from system network settings.", color = Silver, fontSize = 12.sp)
                Text("5. Enter MAC exactly (AA:BB:CC:DD:EE:FF), keep Port as 9 unless your network requires another.", color = Silver, fontSize = 12.sp)
                Text("6. Use broadcast IP 255.255.255.255 or your subnet broadcast (for example 192.168.1.255).", color = Silver, fontSize = 12.sp)
                Text("7. Keep phone and target on the same LAN for local wake tests first.", color = Silver, fontSize = 12.sp)
                Text("8. Tap Wake Mac and verify status below. If it fails, router/AP may block broadcast packets.", color = Silver, fontSize = 12.sp)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Graphite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Wake-on-LAN", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(
                    "Send a magic packet to power on your Mac/PC from sleep or shutdown.",
                    color = Silver,
                    fontSize = 13.sp
                )

                OutlinedTextField(
                    value = macInput,
                    onValueChange = {
                        macInput = it
                        viewModel.setWolMacAddress(it)
                    },
                    label = { Text("Target MAC Address") },
                    placeholder = { Text("AA:BB:CC:DD:EE:FF") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = Platinum
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = broadcastInput,
                    onValueChange = {
                        broadcastInput = it
                        viewModel.setWolBroadcastIp(it)
                    },
                    label = { Text("Broadcast IP") },
                    placeholder = { Text("255.255.255.255") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = Platinum
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = portInput,
                    onValueChange = {
                        portInput = it
                        it.toIntOrNull()?.let(viewModel::setWolPort)
                    },
                    label = { Text("Port") },
                    placeholder = { Text("9") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = Platinum
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { viewModel.sendWakeOnLan() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Wake Mac", color = Color.White)
                }

                Text("Status: $status", color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Tip: If your Mac is already awake, the packet is still sent but you may not see any visible change.",
                    color = Silver,
                    fontSize = 11.sp
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Obsidian),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Setup Notes", color = Platinum, fontWeight = FontWeight.SemiBold)
                Text("1. If wake works once and then fails, disable deep sleep / hibernate on target machine.", color = Silver, fontSize = 12.sp)
                Text("2. Test with both 255.255.255.255 and subnet broadcast IP.", color = Silver, fontSize = 12.sp)
                Text("3. Use Ethernet on target machine for most reliable WOL behavior.", color = Silver, fontSize = 12.sp)
                Icon(Icons.Default.WifiTethering, contentDescription = null, tint = AccentBlue)
            }
        }
        }
    }
}
