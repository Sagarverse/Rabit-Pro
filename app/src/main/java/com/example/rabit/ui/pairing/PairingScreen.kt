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
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*
import com.example.rabit.ui.components.*

@SuppressLint("MissingPermission")
@Composable
fun PairingScreen(
    viewModel: MainViewModel, 
    onConnected: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAssistant: () -> Unit,
    onNavigateToWebBridge: () -> Unit
) {
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val savedDevices by viewModel.savedDevices.collectAsState()
    val context = LocalContext.current

    // Check if Bluetooth is enabled
    val bluetoothAdapter = remember { BluetoothAdapter.getDefaultAdapter() }
    var isBluetoothEnabled by remember { mutableStateOf(bluetoothAdapter?.isEnabled == true) }

    // Use a single connecting device state for per-device spinners
    var connectingDeviceAddress by remember { mutableStateOf<String?>(null) }
    var connectionError by remember { mutableStateOf<String?>(null) }

    // Auto-start scanning when screen opens and BT is enabled
    LaunchedEffect(Unit) {
        if (bluetoothAdapter?.isEnabled == true) {
            viewModel.startScanning()
        }
    }

    // Periodically check BT state
    LaunchedEffect(Unit) {
        while (true) {
            val wasEnabled = isBluetoothEnabled
            isBluetoothEnabled = bluetoothAdapter?.isEnabled == true
            
            // Auto-scan instantly when BT turns on
            if (!wasEnabled && isBluetoothEnabled) {
                viewModel.startScanning()
            }
            kotlinx.coroutines.delay(1000)
        }
    }

    LaunchedEffect(connectionState) {
        when (connectionState) {
            is HidDeviceManager.ConnectionState.Connected -> {
                viewModel.stopScanning()
                connectingDeviceAddress = null
                connectionError = null
                onConnected()
            }
            is HidDeviceManager.ConnectionState.Disconnected -> {
                if (connectingDeviceAddress != null) {
                    val deviceName = try {
                        bluetoothAdapter?.bondedDevices?.find { it.address == connectingDeviceAddress }?.name ?: ""
                    } catch (e: Exception) { "" }
                    val deviceType = com.example.rabit.ui.components.guessDeviceType(deviceName)
                    connectionError = when (deviceType) {
                        com.example.rabit.ui.components.DeviceType.WINDOWS -> 
                            "Connection failed. For Windows: Go to Settings > Bluetooth > Add device on your PC, then tap this device again."
                        com.example.rabit.ui.components.DeviceType.MAC ->
                            "Connection failed. On your Mac, go to System Settings > Bluetooth and ensure it's discoverable."
                        else -> "Connection failed. Ensure the device is nearby, discoverable, and Bluetooth is enabled."
                    }
                    connectingDeviceAddress = null
                }
            }
            else -> {}
        }
    }

    // Determine current step for the stepper
    val currentStep = remember(isBluetoothEnabled, isScanning, connectionState) {
        when {
            !isBluetoothEnabled -> 0 // BT Off
            connectionState is HidDeviceManager.ConnectionState.Connected -> 3 // Connected
            isScanning -> 1 // Scanning
            else -> 2 // Select
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Obsidian,
                drawerContentColor = Platinum,
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                // Drawer Header
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "RABIT PRO",
                        color = Platinum,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "Ultimate HID Control",
                        color = AccentBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Divider(color = BorderColor.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(modifier = Modifier.height(16.dp))

                // Drawer Items
                NavigationDrawerItem(
                    label = { Text("Connect Device", fontWeight = FontWeight.SemiBold) },
                    selected = true,
                    onClick = { scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Bluetooth, contentDescription = null) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = AccentBlue.copy(alpha = 0.15f),
                        selectedIconColor = AccentBlue,
                        selectedTextColor = AccentBlue,
                        unselectedContainerColor = Color.Transparent,
                        unselectedIconColor = Silver,
                        unselectedTextColor = Silver
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Web Bridge", fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        onNavigateToWebBridge()
                    },
                    icon = { Icon(Icons.Default.Cloud, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedIconColor = Silver, unselectedTextColor = Silver)
                )

                NavigationDrawerItem(
                    label = { Text("Chat Assistant", fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        onNavigateToAssistant()
                    },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedIconColor = Silver, unselectedTextColor = Silver)
                )

                NavigationDrawerItem(
                    label = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedIconColor = Silver, unselectedTextColor = Silver)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    "v2.4.0-pro",
                    modifier = Modifier.padding(24.dp),
                    color = Silver.copy(alpha = 0.3f),
                    fontSize = 10.sp
                )
            }
        }
    ) {
        Scaffold(containerColor = Obsidian) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // ── Hero Banner ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PremiumDarkGradient)
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(SoftGrey.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Platinum, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        "RABIT PRO",
                                        color = Platinum,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 2.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "Pairing Interface",
                                        color = AccentBlue,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            IconButton(
                                onClick = onNavigateToSettings,
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(SoftGrey.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Platinum, modifier = Modifier.size(20.dp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        ConnectionStepIndicator(currentStep = currentStep)
                    }
                }

                // ── Main Content Area ──
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(top = 20.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    
                    // ── Error Banner ──
                    if (connectionError != null) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = ErrorRed.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, ErrorRed.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            connectionError!!,
                                            color = ErrorRed,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = { connectionError = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = ErrorRed)
                                    }
                                }
                            }
                        }
                    }

                if (!isBluetoothEnabled) {
                    // ── BT Off State ──
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = AccentBlue.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .background(AccentBlue.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.BluetoothDisabled, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(30.dp))
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Bluetooth is Disabled",
                                    color = Platinum,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Rabit requires Bluetooth to connect and act as a keyboard and mouse for your Mac.",
                                    color = Silver,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { viewModel.requestEnableBluetooth(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Enable Bluetooth", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    // ── BT On State ──

                    // ── Quick Connect (Last Device) ──
                    if (savedDevices.isNotEmpty() && connectionState !is HidDeviceManager.ConnectionState.Connected) {
                        val lastDevice = savedDevices.first()
                        item {
                            Text(
                                "QUICK CONNECT",
                                color = Silver,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            com.example.rabit.ui.components.QuickConnectCard(
                                deviceName = lastDevice.name,
                                lastConnectedTime = lastDevice.lastConnected,
                                isConnecting = connectingDeviceAddress != null,
                                onClick = {
                                    val bondedDevice = try {
                                        bluetoothAdapter?.bondedDevices?.find { it.name == lastDevice.name }
                                    } catch (e: Exception) { null }
                                    if (bondedDevice != null) {
                                        connectingDeviceAddress = bondedDevice.address
                                        connectionError = null
                                        viewModel.connectWithRetry(bondedDevice)
                                    } else {
                                        connectionError = "Device '${lastDevice.name}' is no longer bonded. Please pair again."
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "KNOWN WORKSTATIONS",
                                color = Silver,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Get bonded devices from adapter
                    val bondedDevices = try {
                        bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }

                    if (bondedDevices.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Graphite.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    "No paired devices found",
                                    color = Silver,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(bondedDevices, key = { it.address }) { device ->
                            val deviceName = try { device.name ?: "Unknown Device" } catch(e: Exception) { "Unknown" }
                            val deviceType = guessDeviceType(deviceName)
                            val isThisDeviceConnecting = connectingDeviceAddress == device.address

                            AnimatedDeviceCard(
                                name = deviceName,
                                deviceType = deviceType,
                                subtitle = if (isThisDeviceConnecting) "Connecting..." else "Tap to connect",
                                isConnecting = isThisDeviceConnecting,
                                isBonded = true,
                                onClick = {
                                    connectingDeviceAddress = device.address
                                    connectionError = null
                                    viewModel.connect(device)
                                }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "NEARBY DISCOVERIES",
                                color = Silver,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            
                            if (isScanning) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), color = AccentBlue, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Scanning", color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            } else {
                                TextButton(
                                    onClick = { viewModel.startScanning() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Scan Again", color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Filter out already bonded devices from scanned list
                    val bondedAddresses = bondedDevices.map { it.address }.toSet()
                    val newDevices = scannedDevices.filter { it.address !in bondedAddresses }

                    if (newDevices.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth().height(160.dp),
                                color = Graphite.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.2f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isScanning) {
                                        RadarAnimationSmall()
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.BluetoothSearching, contentDescription = null, tint = Silver.copy(alpha = 0.3f), modifier = Modifier.size(32.dp))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("No new devices found", color = Silver.copy(alpha = 0.6f), fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(newDevices, key = { it.address }) { device ->
                            val deviceName = try { device.name ?: "Unknown Device" } catch(e: Exception) { "Unknown" }
                            val deviceType = guessDeviceType(deviceName)
                            val isThisDeviceConnecting = connectingDeviceAddress == device.address

                            AnimatedDeviceCard(
                                name = deviceName,
                                deviceType = deviceType,
                                subtitle = if (isThisDeviceConnecting) "Pairing & Connecting..." else "New device",
                                isConnecting = isThisDeviceConnecting,
                                isBonded = false,
                                onClick = {
                                    // Stop scanning before attempting to connect for better success rate
                                    viewModel.stopScanning() 
                                    connectingDeviceAddress = device.address
                                    connectionError = null
                                    viewModel.connect(device)
                                }
                            )
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        // Make Mac discoverable hint
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.requestDiscoverable() },
                            color = SoftGrey.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(36.dp).background(AccentGold.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Don't see your Mac?", color = Platinum, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Tap here to make Rabit discoverable", color = Silver, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        // Skip to Offline AI Assistant
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { onNavigateToAssistant() },
                            color = AiViolet.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, AiViolet.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(36.dp).background(AiViolet.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ChatBubble, contentDescription = null, tint = AiViolet, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Skip to Chat Assistant", color = Platinum, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Use offline AI mode without connecting", color = Silver, fontSize = 12.sp)
                                }
                                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = AiViolet, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                }
            }
        }
    }
}
}

// ────────────────────────────────────────────────────────────────────────────────
// Radar Animation
// ────────────────────────────────────────────────────────────────────────────────

@Composable
fun RadarAnimationSmall() {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val ring1 by infiniteTransition.animateFloat(
        initialValue = 0.01f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "ring1"
    )
    val ring2 by infiniteTransition.animateFloat(
        initialValue = 0.01f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, 1000, easing = LinearEasing), RepeatMode.Restart),
        label = "ring2"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val maxRadius = size.minDimension / 2
            
            // Ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AccentBlue.copy(alpha = 0.15f), Color.Transparent),
                    center = center,
                    radius = maxRadius
                ),
                radius = maxRadius,
                center = center
            )

            listOf(ring1, ring2).forEach { progress ->
                val r = maxRadius * progress
                if (r > 0.5f) {
                    val alpha = (1f - progress) * 0.5f
                    drawCircle(
                        color = AccentBlue.copy(alpha = alpha),
                        radius = r,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
        
        Icon(
            Icons.Default.Bluetooth,
            contentDescription = null,
            tint = AccentBlue,
            modifier = Modifier.size(24.dp)
        )
    }
}
