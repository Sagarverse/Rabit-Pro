package com.example.rabit.ui.pairing

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*
import com.example.rabit.ui.components.*

@SuppressLint("MissingPermission")
@Composable
fun PairingScreen(
    viewModel: MainViewModel, 
    onConnected: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(connectionState) {
        if (connectionState is HidDeviceManager.ConnectionState.Connected) {
            viewModel.stopScanning()
            onConnected()
        }
    }

    Scaffold(
        containerColor = Obsidian
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Bluetooth", 
                    color = Platinum, 
                    fontSize = 34.sp, 
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.requestDiscoverable() },
                        modifier = Modifier.background(SoftGrey, CircleShape)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = "Make Discoverable", tint = AccentGold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.background(SoftGrey, CircleShape)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Platinum)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = { 
                            viewModel.disconnect()
                            try {
                                val packageManager = context.packageManager
                                val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                                if (intent != null) {
                                    val componentName = intent.component
                                    val mainIntent = Intent.makeRestartActivityTask(componentName)
                                    context.startActivity(mainIntent)
                                    Runtime.getRuntime().exit(0)
                                }
                            } catch (e: Exception) {
                                Log.e("PairingScreen", "Restart Error", e)
                            }
                        },
                        modifier = Modifier.background(SoftGrey, CircleShape)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = Platinum)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Radar Animation Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isScanning) {
                    RadarAnimation()
                } else {
                    Icon(
                        Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = Silver.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            // Apple-style toggle row
            PremiumGlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Searching for Devices", color = Platinum, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text("Visible to nearby Macs", color = Silver, fontSize = 13.sp)
                    }
                    Switch(
                        checked = isScanning,
                        onCheckedChange = { 
                            try {
                                if (it) viewModel.startScanning() else viewModel.stopScanning()
                            } catch (e: Exception) {
                                Log.e("PairingScreen", "Toggle Error", e)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SuccessGreen,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = SoftGrey
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            PremiumSectionHeader("OTHER DEVICES")

            PremiumGlassCard(modifier = Modifier.padding(bottom = 24.dp)) {
                Column {
                    val devicesList = scannedDevices.toList()
                    devicesList.forEachIndexed { index, device ->
                        var deviceName by remember(device) { mutableStateOf("Unknown Device") }
                        LaunchedEffect(device) {
                            try {
                                deviceName = device.name ?: "Unknown Device"
                            } catch (e: SecurityException) {
                                deviceName = "Unknown (No Permission)"
                            }
                        }

                        AppleDeviceItem(
                            name = deviceName,
                            status = if (connectionState is HidDeviceManager.ConnectionState.Connecting) "Connecting..." else "Not Connected",
                            onClick = { viewModel.connect(device) }
                        )
                        if (index != devicesList.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 16.dp),
                                thickness = 0.5.dp,
                                color = BorderColor.copy(alpha = 0.5f)
                            )
                        }
                    }
                    if (devicesList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = AccentBlue,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RadarAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val radius by infiniteTransition.animateFloat(
        initialValue = 0.01f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radius"
    )
    val opacity by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "opacity"
    )

    Canvas(modifier = Modifier.size(200.dp)) {
        val currentRadius = (size.minDimension / 2) * radius
        if (currentRadius > 0.5f) {
            // Inner glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AccentBlue.copy(alpha = 0.2f * opacity), Color.Transparent),
                    center = center,
                    radius = (size.minDimension / 2)
                ),
                radius = (size.minDimension / 2),
                center = center
            )
            // Pulse wave
            drawCircle(
                color = AccentBlue.copy(alpha = opacity * 0.5f),
                radius = currentRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
    Icon(
        Icons.Default.Bluetooth,
        contentDescription = null,
        tint = AccentBlue,
        modifier = Modifier.size(48.dp)
    )
}

@Composable
fun AppleDeviceItem(name: String, status: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, color = Platinum, fontSize = 17.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(status, color = Silver, fontSize = 15.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.Bluetooth, 
                contentDescription = null, 
                tint = Silver, 
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
