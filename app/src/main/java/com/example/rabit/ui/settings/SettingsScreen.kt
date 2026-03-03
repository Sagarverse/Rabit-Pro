package com.example.rabit.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rabit.data.secure.EncryptionManager
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*
import com.example.rabit.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val geminiSettingsViewModel: GeminiSettingsViewModel = viewModel(
        factory = androidx.lifecycle.viewmodel.viewModelFactory {
            addInitializer(GeminiSettingsViewModel::class) {
                GeminiSettingsViewModel(context.applicationContext as android.app.Application)
            }
        }
    )
    
    val autoReconnect by viewModel.autoReconnectEnabled.collectAsState()
    val password by viewModel.unlockPassword.collectAsState()
    val typingSpeed by viewModel.typingSpeed.collectAsState()
    val notificationSync by viewModel.notificationSyncEnabled.collectAsState()
    
    val prefs = remember { context.getSharedPreferences("rabit_prefs", android.content.Context.MODE_PRIVATE) }
    var shakeEnabled by remember { mutableStateOf(prefs.getBoolean("shake_to_control_calls", false)) }
    var macIp by remember { mutableStateOf(prefs.getString("mac_ip", "") ?: "") }

    // Feature 4: Automation
    var dndOnConnect by remember { mutableStateOf(prefs.getBoolean("auto_dnd_on_connect", false)) }
    var wakeLockOnConnect by remember { mutableStateOf(prefs.getBoolean("auto_wake_lock_on_connect", false)) }

    // Feature 5: E2EE
    val encryptionManager = remember { EncryptionManager(context) }
    var e2eeEnabled by remember { mutableStateOf(encryptionManager.isEnabled) }
    var showQrDialog by remember { mutableStateOf(false) }

    var showPasswordDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showMacIpDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Platinum) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Platinum)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Obsidian)
            )
        },
        containerColor = Obsidian
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GeminiApiSettingsSection(viewModel = geminiSettingsViewModel)
            
            PremiumSectionHeader("CONNECTION")
            PremiumGlassCard {
                SettingsToggleItem(
                    title = "Auto Reconnect",
                    subtitle = "Automatically connect to last device",
                    icon = Icons.Default.Sync,
                    checked = autoReconnect,
                    onCheckedChange = { viewModel.setAutoReconnectEnabled(it) }
                )
            }

            PremiumSectionHeader("GESTURES & SECURITY")
            PremiumGlassCard {
                SettingsToggleItem(
                    title = "Shake to Control Calls",
                    subtitle = "Vertical: Answer, Horizontal: Reject",
                    icon = Icons.Default.ScreenRotation,
                    checked = shakeEnabled,
                    onCheckedChange = { 
                        shakeEnabled = it
                        prefs.edit().putBoolean("shake_to_control_calls", it).apply()
                        // Update the service
                        val intent = android.content.Intent(context, com.example.rabit.data.bluetooth.HidService::class.java).apply {
                            action = "UPDATE_SHAKE_SETTINGS"
                            putExtra("enabled", it)
                        }
                        context.startService(intent)
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.5f))
                SettingsClickItem(
                    title = "Typing Speed",
                    subtitle = "Current: $typingSpeed",
                    icon = Icons.Default.Speed,
                    onClick = { showSpeedDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.5f))
                SettingsClickItem(
                    title = "Unlock Password",
                    subtitle = "Current: $password",
                    icon = Icons.Default.Lock,
                    onClick = { showPasswordDialog = true }
                )
            }

            PremiumSectionHeader("NOTIFICATIONS")
            PremiumGlassCard {
                SettingsToggleItem(
                    title = "Notification Sync",
                    subtitle = "Type phone notifications to Mac",
                    icon = Icons.Default.Notifications,
                    checked = notificationSync,
                    onCheckedChange = { viewModel.setNotificationSyncEnabled(it) }
                )
            }

            PremiumSectionHeader("ADVANCED FEATURES")
            PremiumGlassCard {
                SettingsClickItem(
                    title = "Mac IP Address",
                    subtitle = if (macIp.isBlank()) "Tap to set (required for Screen Handoff)" else "Current: $macIp",
                    icon = Icons.Default.Computer,
                    onClick = { showMacIpDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.5f))
                SettingsClickItem(
                    title = "File Receive Server",
                    subtitle = "Running on port 8765 • Send files via curl or Mac companion",
                    icon = Icons.Default.FolderOpen,
                    onClick = { /* info only */ }
                )
            }

            // ─── Feature 4: Automation ───
            PremiumSectionHeader("SMART AUTOMATION")
            PremiumGlassCard {
                SettingsToggleItem(
                    title = "Do Not Disturb on Connect",
                    subtitle = "Silence phone when Mac is connected",
                    icon = Icons.Default.DoNotDisturb,
                    checked = dndOnConnect,
                    onCheckedChange = {
                        dndOnConnect = it
                        prefs.edit().putBoolean("auto_dnd_on_connect", it).apply()
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.5f))
                SettingsToggleItem(
                    title = "Keep Screen Awake",
                    subtitle = "Prevent screen timeout while connected",
                    icon = Icons.Default.BrightnessHigh,
                    checked = wakeLockOnConnect,
                    onCheckedChange = {
                        wakeLockOnConnect = it
                        prefs.edit().putBoolean("auto_wake_lock_on_connect", it).apply()
                    }
                )
            }

            // ─── Feature 5: E2EE ───
            PremiumSectionHeader("SECURITY & ENCRYPTION")
            PremiumGlassCard {
                SettingsToggleItem(
                    title = "End-to-End Encryption",
                    subtitle = "AES-GCM 256-bit • Requires pairing with Mac",
                    icon = Icons.Default.Shield,
                    checked = e2eeEnabled,
                    onCheckedChange = {
                        e2eeEnabled = it
                        encryptionManager.setEnabled(it)
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.5f))
                SettingsClickItem(
                    title = "Pair with Mac (Show QR)",
                    subtitle = if (encryptionManager.isPaired()) "✅ Paired — key exchanged" else "Scan on Mac to exchange keys",
                    icon = Icons.Default.QrCode,
                    onClick = { showQrDialog = true }
                )
            }
        }
    }

    if (showPasswordDialog) {
        var tempPass by remember { mutableStateOf(password) }
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Set Unlock Password") },
            text = {
                OutlinedTextField(
                    value = tempPass,
                    onValueChange = { tempPass = it },
                    label = { Text("Password") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.setUnlockPassword(tempPass)
                    showPasswordDialog = false
                }) {
                    Text("Save")
                }
            }
        )
    }

    if (showSpeedDialog) {
        val speeds = listOf("Too Slow", "Slow", "Normal", "Fast", "Super Fast")
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Typing Speed") },
            text = {
                Column {
                    speeds.forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    viewModel.setTypingSpeed(speed)
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (speed == typingSpeed),
                                onClick = { 
                                    viewModel.setTypingSpeed(speed)
                                    showSpeedDialog = false
                                }
                            )
                            Text(speed, color = Platinum, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Cancel", color = AccentBlue)
                }
            }
        )
    }

    if (showMacIpDialog) {
        var tempIp by remember { mutableStateOf(macIp) }
        AlertDialog(
            onDismissRequest = { showMacIpDialog = false },
            title = { Text("Mac IP Address", color = Platinum) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter your Mac's local IP address.\nFind it in System Settings → Network.",
                        color = Silver, fontSize = 13.sp)
                    OutlinedTextField(
                        value = tempIp,
                        onValueChange = { tempIp = it },
                        label = { Text("e.g. 192.168.1.100") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    macIp = tempIp
                    prefs.edit().putString("mac_ip", tempIp).apply()
                    showMacIpDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showMacIpDialog = false }) {
                    Text("Cancel", color = AccentBlue)
                }
            }
        )
    }

    // Feature 5 – E2EE Pairing dialog
    if (showQrDialog) {
        val myPublicKey = remember { encryptionManager.getPublicKeyBase64() }
        var peerKey by remember { mutableStateOf("") }
        var pairingError by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = { Text("🔐 E2EE Pairing", color = Platinum) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Step 1 — Share your public key with the Mac companion app:",
                        color = Silver, fontSize = 13.sp)
                    Surface(
                        color = Obsidian,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = myPublicKey,
                            modifier = Modifier.padding(8.dp),
                            fontSize = 9.sp,
                            color = AccentBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text("Step 2 — Paste the Mac's public key below:",
                        color = Silver, fontSize = 13.sp)
                    OutlinedTextField(
                        value = peerKey,
                        onValueChange = { peerKey = it; pairingError = "" },
                        label = { Text("Mac's public key (Base64)") },
                        minLines = 3,
                        maxLines = 5
                    )
                    if (pairingError.isNotEmpty()) {
                        Text("❌ $pairingError", color = androidx.compose.ui.graphics.Color.Red, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    try {
                        encryptionManager.acceptPeerPublicKey(peerKey.trim())
                        showQrDialog = false
                    } catch (e: Exception) {
                        pairingError = "Invalid key format. Check and try again."
                    }
                }) { Text("Pair") }
            },
            dismissButton = {
                TextButton(onClick = { showQrDialog = false }) {
                    Text("Cancel", color = AccentBlue)
                }
            }
        )
    }
}



@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            title,
            color = Silver,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Surface(
            color = Graphite,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsToggleItem(title: String, subtitle: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = Platinum, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = Silver, fontSize = 12.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = SuccessGreen)
        )
    }
}

@Composable
fun SettingsClickItem(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, color = Platinum, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Silver, fontSize = 12.sp)
        }
    }
}
