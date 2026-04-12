package com.example.rabit.ui.settings

import android.widget.Toast
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToCustomization: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
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
    val autoPush by viewModel.autoPushEnabled.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val trackpadSensitivity by viewModel.trackpadSensitivity.collectAsState()
    
    val prefs = remember { context.getSharedPreferences("rabit_prefs", android.content.Context.MODE_PRIVATE) }


    var dndOnConnect by remember { mutableStateOf(prefs.getBoolean("auto_dnd_on_connect", false)) }
    var wakeLockOnConnect by remember { mutableStateOf(prefs.getBoolean("auto_wake_lock_on_connect", false)) }

    val encryptionManager = remember { EncryptionManager(context) }
    var e2eeEnabled by remember { mutableStateOf(encryptionManager.isEnabled) }

    var showPasswordDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Obsidian
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ─── Pro Status Card ───
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                shape = RoundedCornerShape(20.dp),
                color = AccentGold.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, Brush.linearGradient(listOf(AccentGold.copy(alpha = 0.5f), Color.Transparent)))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).background(AccentGold.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = AccentGold, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("PRO LICENSE", color = Platinum, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Lifetime Professional License", color = AccentGold, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            GeminiApiSettingsSection(viewModel = geminiSettingsViewModel)
            
            // ─── Appearance ───
            PremiumSectionHeader("APPEARANCE")
            PremiumGlassCard {
                SettingsClickItem(
                    title = "Advanced Customization",
                    subtitle = "Personalize colors, security & gestures",
                    icon = Icons.Default.Tune,
                    iconColor = AccentBlue,
                    onClick = onNavigateToCustomization
                )
            }
            
            // ─── Connection ───
            PremiumSectionHeader("CONNECTION")
            PremiumGlassCard {
                SettingsToggleItem(
                    title = "Auto Reconnect",
                    subtitle = "Automatically connect to last device",
                    icon = Icons.Default.Sync,
                    iconColor = AccentBlue,
                    checked = autoReconnect,
                    onCheckedChange = { viewModel.setAutoReconnectEnabled(it) }
                )
            }

            // ─── Input & Controls ───
            PremiumSectionHeader("INPUT & CONTROLS")
            PremiumGlassCard {
                SettingsClickItem(
                    title = "Typing Speed",
                    subtitle = "Current: $typingSpeed",
                    icon = Icons.Default.Speed,
                    iconColor = AccentPurple,
                    onClick = { showSpeedDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsClickItem(
                    title = "Mac Unlock Password",
                    subtitle = "Mac unlock: ••••",
                    icon = Icons.Default.Lock,
                    iconColor = AccentOrange,
                    onClick = { showPasswordDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsToggleItem(
                    title = "Haptic Feedback",
                    subtitle = "Vibrate on key press & button taps",
                    icon = Icons.Default.Vibration,
                    iconColor = AccentPink,
                    checked = vibrationEnabled,
                    onCheckedChange = { viewModel.setVibrationEnabled(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                // Trackpad Sensitivity
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsIconBadge(icon = Icons.Default.TouchApp, backgroundColor = AccentTeal)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Trackpad Sensitivity", color = Platinum, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Text("Adjust cursor speed: ${String.format("%.1f", trackpadSensitivity)}x", color = Silver, fontSize = 12.sp)
                        }
                    }
                    Slider(
                        value = trackpadSensitivity,
                        onValueChange = { viewModel.setTrackpadSensitivity(it) },
                        valueRange = 0.5f..3.0f,
                        steps = 4,
                        modifier = Modifier.padding(start = 46.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = AccentTeal,
                            activeTrackColor = AccentTeal
                        )
                    )
                }
            }

            // ─── Clipboard & Sync ───
            PremiumSectionHeader("CLIPBOARD & SYNC")
            PremiumGlassCard {
                SettingsToggleItem(
                    title = "Clipboard Auto-Push",
                    subtitle = "Automatically push copied text to Mac",
                    icon = Icons.Default.ContentPasteGo,
                    iconColor = SuccessGreen,
                    checked = autoPush,
                    onCheckedChange = { viewModel.setAutoPushEnabled(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsToggleItem(
                    title = "Notification Sync",
                    subtitle = "Type phone notifications to Mac",
                    icon = Icons.Default.Notifications,
                    iconColor = AccentPink,
                    checked = notificationSync,
                    onCheckedChange = { viewModel.setNotificationSyncEnabled(it) }
                )
            }



            // ─── Macros ───
            PremiumSectionHeader("MACROS")
            PremiumGlassCard {
                SettingsClickItem(
                    title = "Export Macros",
                    subtitle = "Copy all custom macros as JSON",
                    icon = Icons.Default.Upload,
                    iconColor = AccentBlue,
                    onClick = {
                        val json = viewModel.exportMacrosJson()
                        clipboardManager.setText(AnnotatedString(json))
                        Toast.makeText(context, "Macros copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsClickItem(
                    title = "Import Macros",
                    subtitle = "Paste JSON to import macros",
                    icon = Icons.Default.Download,
                    iconColor = SuccessGreen,
                    onClick = { showImportDialog = true }
                )
            }

            // ─── Automation ───
            PremiumSectionHeader("SMART AUTOMATION")
            PremiumGlassCard {
                SettingsToggleItem(
                    title = "Do Not Disturb on Connect",
                    subtitle = "Silence phone when Mac is connected",
                    icon = Icons.Default.DoNotDisturb,
                    iconColor = ErrorRed,
                    checked = dndOnConnect,
                    onCheckedChange = {
                        dndOnConnect = it
                        prefs.edit().putBoolean("auto_dnd_on_connect", it).apply()
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsToggleItem(
                    title = "Keep Screen Awake",
                    subtitle = "Prevent screen timeout while connected",
                    icon = Icons.Default.BrightnessHigh,
                    iconColor = WarningYellow,
                    checked = wakeLockOnConnect,
                    onCheckedChange = {
                        wakeLockOnConnect = it
                        prefs.edit().putBoolean("auto_wake_lock_on_connect", it).apply()
                    }
                )
            }

            // ─── Security & Encryption ───
            PremiumSectionHeader("ENCRYPTION")
            PremiumGlassCard {
                SettingsToggleItem(
                    title = "End-to-End Encryption",
                    subtitle = "AES-GCM 256-bit • Requires Mac pairing",
                    icon = Icons.Default.Shield,
                    iconColor = SuccessGreen,
                    checked = e2eeEnabled,
                    onCheckedChange = {
                        e2eeEnabled = it
                        encryptionManager.setEnabled(it)
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsClickItem(
                    title = "Pair with Mac (Show QR)",
                    subtitle = if (encryptionManager.isPaired()) "✅ Paired — key exchanged" else "Scan on Mac to exchange keys",
                    icon = Icons.Default.QrCode,
                    iconColor = AccentBlue,
                    onClick = { showQrDialog = true }
                )
            }

            // ─── About ───
            PremiumSectionHeader("ABOUT")
            PremiumGlassCard {
                SettingsClickItem(
                    title = "About Developer",
                    subtitle = "Sagar M • Bengaluru, India",
                    icon = Icons.Default.Person,
                    iconColor = Platinum,
                    onClick = onNavigateToProfile
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsClickItem(
                    title = "Version Info",
                    subtitle = "v1.5.0-pro (Stable Build)",
                    icon = Icons.Default.Info,
                    iconColor = Silver,
                    onClick = { /* Could show a changelog */ }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // ─── Dialogs ───

    if (showPasswordDialog) {
        var tempPass by remember { mutableStateOf(password) }
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            containerColor = Graphite,
            title = { Text("Set Unlock Password", color = Platinum) },
            text = {
                OutlinedTextField(
                    value = tempPass,
                    onValueChange = { tempPass = it },
                    label = { Text("Password") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentBlue, unfocusedBorderColor = BorderColor, focusedTextColor = Platinum)
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.setUnlockPassword(tempPass); showPasswordDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showPasswordDialog = false }) { Text("Cancel", color = Silver) } }
        )
    }

    if (showSpeedDialog) {
        val speeds = listOf("Too Slow", "Slow", "Normal", "Fast", "Super Fast")
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            containerColor = Graphite,
            title = { Text("Typing Speed", color = Platinum) },
            text = {
                Column {
                    speeds.forEach { speed ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.setTypingSpeed(speed); showSpeedDialog = false }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = (speed == typingSpeed), onClick = { viewModel.setTypingSpeed(speed); showSpeedDialog = false }, colors = RadioButtonDefaults.colors(selectedColor = AccentBlue))
                            Text(speed, color = Platinum, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSpeedDialog = false }) { Text("Cancel", color = Silver) } }
        )
    }



    if (showQrDialog) {
        val myPublicKey = remember { encryptionManager.getPublicKeyBase64() }
        var peerKey by remember { mutableStateOf("") }
        var pairingError by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            containerColor = Graphite,
            title = { Text("🔐 E2EE Pairing", color = Platinum) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Step 1 — Share your public key:", color = Silver, fontSize = 13.sp)
                    Surface(color = Obsidian, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(myPublicKey, modifier = Modifier.padding(8.dp), fontSize = 9.sp, color = AccentBlue, fontWeight = FontWeight.Bold)
                    }
                    Text("Step 2 — Paste the Mac's public key:", color = Silver, fontSize = 13.sp)
                    OutlinedTextField(
                        value = peerKey,
                        onValueChange = { peerKey = it; pairingError = "" },
                        label = { Text("Mac's public key (Base64)") },
                        minLines = 3, maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentBlue, unfocusedBorderColor = BorderColor, focusedTextColor = Platinum)
                    )
                    if (pairingError.isNotEmpty()) { Text("❌ $pairingError", color = ErrorRed, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                Button(onClick = {
                    try { encryptionManager.acceptPeerPublicKey(peerKey.trim()); showQrDialog = false }
                    catch (e: Exception) { pairingError = "Invalid key format." }
                }, colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)) { Text("Pair") }
            },
            dismissButton = { TextButton(onClick = { showQrDialog = false }) { Text("Cancel", color = Silver) } }
        )
    }

    if (showImportDialog) {
        var importJson by remember { mutableStateOf("") }
        var importError by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = Graphite,
            title = { Text("Import Macros", color = Platinum) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste the exported macro JSON below:", color = Silver, fontSize = 13.sp)
                    OutlinedTextField(
                        value = importJson,
                        onValueChange = { importJson = it; importError = "" },
                        label = { Text("JSON content") },
                        minLines = 4, maxLines = 8,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SuccessGreen, unfocusedBorderColor = BorderColor, focusedTextColor = Platinum)
                    )
                    if (importError.isNotEmpty()) { Text("❌ $importError", color = ErrorRed, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (viewModel.importMacrosJson(importJson)) {
                        Toast.makeText(context, "Macros imported!", Toast.LENGTH_SHORT).show()
                        showImportDialog = false
                    } else {
                        importError = "Invalid JSON format."
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)) { Text("Import", color = Obsidian) }
            },
            dismissButton = { TextButton(onClick = { showImportDialog = false }) { Text("Cancel", color = Silver) } }
        )
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// Settings Item Components
// ────────────────────────────────────────────────────────────────────────────────

@Composable
fun SettingsToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color = AccentBlue,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            SettingsIconBadge(icon = icon, backgroundColor = iconColor)
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(title, color = Platinum, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = Silver, fontSize = 12.sp)
            }
        }
        Switch(
            checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = SuccessGreen, checkedThumbColor = Color.White, uncheckedThumbColor = Color.White, uncheckedTrackColor = SoftGrey)
        )
    }
}

@Composable
fun SettingsClickItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color = AccentBlue,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconBadge(icon = icon, backgroundColor = iconColor)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Platinum, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Silver, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Silver.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
    }
}

@Composable
fun ContactItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Silver, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(text, color = Platinum, fontSize = 14.sp)
    }
}

@Composable
fun SocialIconButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(8.dp)
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = SoftGrey.copy(alpha = 0.3f),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.2f))
        ) {
            Icon(icon, contentDescription = label, tint = Platinum, modifier = Modifier.padding(10.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Silver, fontSize = 10.sp)
    }
}
