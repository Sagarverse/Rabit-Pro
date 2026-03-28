package com.example.rabit.ui.pairing

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextAlign
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

    // Check if Bluetooth is enabled
    val bluetoothAdapter = remember { BluetoothAdapter.getDefaultAdapter() }
    var isBluetoothEnabled by remember { mutableStateOf(bluetoothAdapter?.isEnabled == true) }

    // Periodically check BT state
    LaunchedEffect(Unit) {
        while (true) {
            isBluetoothEnabled = bluetoothAdapter?.isEnabled == true
            kotlinx.coroutines.delay(1000)
        }
    }

    LaunchedEffect(connectionState) {
        if (connectionState is HidDeviceManager.ConnectionState.Connected) {
            viewModel.stopScanning()
            onConnected()
        }
    }

    Scaffold(containerColor = Obsidian) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── Header ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "RABIT PRO",
                        color = Platinum,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Connect to Mac or Android",
                        color = Silver,
                        fontSize = 13.sp
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.requestDiscoverable() },
                        modifier = Modifier
                            .size(40.dp)
                            .background(SoftGrey, CircleShape)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = "Discoverable", tint = AccentGold, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .size(40.dp)
                            .background(SoftGrey, CircleShape)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Platinum, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Bluetooth Toggle ──
            PremiumGlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SettingsIconBadge(
                            icon = Icons.Default.Bluetooth,
                            backgroundColor = AccentBlue
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                "Bluetooth",
                                color = Platinum,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (!isBluetoothEnabled) "Tap to enable" 
                                else if (isScanning) "Scanning…"
                                else "Ready to connect",
                                color = if (!isBluetoothEnabled) AccentOrange else Silver,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Switch(
                        checked = isBluetoothEnabled && isScanning,
                        onCheckedChange = { shouldEnable ->
                            try {
                                if (!isBluetoothEnabled) {
                                    // Open system Bluetooth settings to let user enable BT
                                    val btIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                                    btIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(btIntent)
                                } else if (shouldEnable) {
                                    viewModel.startScanning()
                                } else {
                                    viewModel.stopScanning()
                                }
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

            Spacer(modifier = Modifier.height(24.dp))

            // ── Radar Animation Section ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isScanning) {
                    RadarAnimation()
                } else if (!isBluetoothEnabled) {
                    // BT off state
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.BluetoothDisabled,
                            contentDescription = "Bluetooth off",
                            tint = Silver.copy(alpha = 0.25f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Bluetooth is off",
                            color = Silver.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Toggle the switch above to begin",
                            color = Silver.copy(alpha = 0.3f),
                            fontSize = 12.sp
                        )
                    }
                } else {
                    // Idle state
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Bluetooth,
                            contentDescription = "Bluetooth idle",
                            tint = Silver.copy(alpha = 0.2f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Ready to scan",
                            color = Silver.copy(alpha = 0.4f),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Compatibility Note ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AccentBlue.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Rabit connects to Mac & Android devices only. Windows pairing is not supported.",
                        color = AccentBlue.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Saved Devices ──
            val savedDevices by viewModel.savedDevices.collectAsState()
            if (savedDevices.isNotEmpty() && !isScanning) {
                PremiumSectionHeader("RECENTLY CONNECTED")
                PremiumGlassCard {
                    savedDevices.take(3).forEachIndexed { index, saved ->
                        val deviceType = guessDeviceType(saved.name)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Try to find matching device in scanned devices
                                    val matching = scannedDevices.firstOrNull { 
                                        try { it.name == saved.name } catch (e: SecurityException) { false }
                                    }
                                    if (matching != null) viewModel.connect(matching)
                                    else viewModel.startScanning()
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DeviceTypeBadge(deviceType)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(saved.name, color = Platinum, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            }
                            Icon(Icons.Default.History, contentDescription = null, tint = Silver.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
                        }
                        if (index != savedDevices.take(3).size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(start = 52.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.3f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Devices Section with Rescan ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PremiumSectionHeader("AVAILABLE DEVICES")
                if (!isScanning && isBluetoothEnabled) {
                    TextButton(
                        onClick = { viewModel.startScanning() },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Scan Again", color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            PremiumGlassCard(modifier = Modifier.weight(1f)) {
                val devicesList = scannedDevices.toList()
                if (devicesList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = AccentBlue,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    "Searching for devices…",
                                    color = Silver,
                                    fontSize = 14.sp
                                )
                            } else {
                                Icon(
                                    Icons.Default.DevicesOther,
                                    contentDescription = "No devices",
                                    tint = Silver.copy(alpha = 0.2f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    "No devices found",
                                    color = Silver.copy(alpha = 0.5f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Enable scanning to discover nearby devices",
                                    color = Silver.copy(alpha = 0.3f),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    Column {
                        devicesList.forEachIndexed { index, device ->
                            var deviceName by remember(device) { mutableStateOf("Unknown Device") }
                            LaunchedEffect(device) {
                                try {
                                    deviceName = device.name ?: "Unknown Device"
                                } catch (e: SecurityException) {
                                    deviceName = "Unknown (No Permission)"
                                }
                            }

                            val deviceType = guessDeviceType(deviceName)

                            DeviceItem(
                                name = deviceName,
                                deviceType = deviceType,
                                isConnecting = connectionState is HidDeviceManager.ConnectionState.Connecting,
                                onClick = { viewModel.connect(device) }
                            )
                            if (index != devicesList.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 52.dp),
                                    thickness = 0.5.dp,
                                    color = BorderColor.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// Device Item
// ────────────────────────────────────────────────────────────────────────────────

@Composable
fun DeviceItem(
    name: String,
    deviceType: DeviceType,
    isConnecting: Boolean,
    onClick: () -> Unit
) {
    val (icon, iconColor) = when (deviceType) {
        DeviceType.MAC -> Icons.Default.Laptop to MacDeviceColor
        DeviceType.ANDROID -> Icons.Default.PhoneAndroid to AndroidDeviceColor
        DeviceType.UNKNOWN -> Icons.Default.Devices to UnknownDeviceColor
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(name, color = Platinum, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(
                    if (isConnecting) "Connecting…" else "Tap to connect",
                    color = if (isConnecting) AccentBlue else Silver,
                    fontSize = 12.sp
                )
            }
        }
        if (isConnecting) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = AccentBlue,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Silver.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// Radar Animation
// ────────────────────────────────────────────────────────────────────────────────

@Composable
fun RadarAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")

    // Multiple rings for depth
    val ring1 by infiniteTransition.animateFloat(
        initialValue = 0.01f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "ring1"
    )
    val ring2 by infiniteTransition.animateFloat(
        initialValue = 0.01f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, 700, easing = LinearEasing), RepeatMode.Restart),
        label = "ring2"
    )
    val ring3 by infiniteTransition.animateFloat(
        initialValue = 0.01f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, 1400, easing = LinearEasing), RepeatMode.Restart),
        label = "ring3"
    )

    Canvas(modifier = Modifier.size(180.dp)) {
        val maxRadius = size.minDimension / 2

        // Ambient glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(AccentBlue.copy(alpha = 0.12f), Color.Transparent),
                center = center,
                radius = maxRadius
            ),
            radius = maxRadius,
            center = center
        )

        // Pulse rings
        listOf(ring1, ring2, ring3).forEach { progress ->
            val r = maxRadius * progress
            if (r > 0.5f) {
                val alpha = (1f - progress) * 0.4f
                drawCircle(
                    color = AccentBlue.copy(alpha = alpha),
                    radius = r,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }
    }

    // Center bluetooth icon
    Icon(
        Icons.Default.Bluetooth,
        contentDescription = null,
        tint = AccentBlue,
        modifier = Modifier.size(40.dp)
    )
}
