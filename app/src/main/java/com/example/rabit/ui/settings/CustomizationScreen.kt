package com.example.rabit.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.data.secure.BiometricAuthenticator
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*
import com.example.rabit.ui.components.*

/**
 * CustomizationScreen - The professional "Control Center" for Rabit Pro users.
 * 
 * Provides granular toggles for security, haptics, and visual dynamics, 
 * ensuring the app feels perfectly tailored to each professional's workflow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricAuthenticator = remember(activity) { activity?.let { BiometricAuthenticator(it) } }

    val biometricEnabled by viewModel.biometricLockEnabled.collectAsState()
    val biometricMacAutofillEnabled by viewModel.biometricMacAutofillEnabled.collectAsState()
    val macAutofillPreEnter by viewModel.macAutofillPreEnter.collectAsState()
    val macAutofillPostEnter by viewModel.macAutofillPostEnter.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val shakeToDisconnect by viewModel.shakeToDisconnectEnabled.collectAsState()
    val stealthMode by viewModel.stealthModeEnabled.collectAsState()
    val autoReconnect by viewModel.autoReconnectEnabled.collectAsState()
    val password by viewModel.macPassword.collectAsState()
    val featureWebBridgeVisible by viewModel.featureWebBridgeVisible.collectAsState()
    val featureAutomationVisible by viewModel.featureAutomationVisible.collectAsState()
    val featureAssistantVisible by viewModel.featureAssistantVisible.collectAsState()
    val featureSnippetsVisible by viewModel.featureSnippetsVisible.collectAsState()
    val featureShortcutsVisible by viewModel.featureShortcutsVisible.collectAsState()
    val featureWakeOnLanVisible by viewModel.featureWakeOnLanVisible.collectAsState()
    val featureSshTerminalVisible by viewModel.featureSshTerminalVisible.collectAsState()

    var showPasswordDialog by remember { mutableStateOf(false) }

    Scaffold(
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
            // ─── Security Section ───
            PremiumSectionHeader("SECURITY & ACCESS")
            PremiumGlassCard {
                SettingsToggleItem(
                    title = "Biometric Lock",
                    subtitle = "Require fingerprint or face ID to open app",
                    icon = Icons.Default.Fingerprint,
                    iconColor = SuccessGreen,
                    checked = biometricEnabled,
                    onCheckedChange = { viewModel.setBiometricLockEnabled(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsToggleItem(
                    title = "Stealth History",
                    subtitle = "Auto-clear session data on app exit",
                    icon = Icons.Default.VisibilityOff,
                    iconColor = AccentOrange,
                    checked = stealthMode,
                    onCheckedChange = { viewModel.setStealthModeEnabled(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsClickItem(
                    title = "Mac Unlock Password",
                    subtitle = "Unlock Mac: ••••",
                    icon = Icons.Default.Lock,
                    iconColor = AccentGold,
                    onClick = { showPasswordDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsToggleItem(
                    title = "Biometric Password Autofill",
                    subtitle = "Require fingerprint/face before typing Mac password",
                    icon = Icons.Default.Password,
                    iconColor = AccentTeal,
                    checked = biometricMacAutofillEnabled,
                    onCheckedChange = { viewModel.setBiometricMacAutofillEnabled(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsToggleItem(
                    title = "Press Enter Before Typing",
                    subtitle = "Wake login field before sending password",
                    icon = Icons.Default.Keyboard,
                    iconColor = AccentBlue,
                    checked = macAutofillPreEnter,
                    onCheckedChange = { viewModel.setMacAutofillPreEnter(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsToggleItem(
                    title = "Press Enter After Typing",
                    subtitle = "Submit credentials after autofill",
                    icon = Icons.Default.Key,
                    iconColor = SuccessGreen,
                    checked = macAutofillPostEnter,
                    onCheckedChange = { viewModel.setMacAutofillPostEnter(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (connectionState is HidDeviceManager.ConnectionState.Connected) "Mac connection detected" else "Connect to Mac to use autofill",
                        color = Silver,
                        fontSize = 12.sp
                    )
                    Button(
                        onClick = {
                            val dispatchAutofill = {
                                val error = viewModel.sendStoredMacPasswordToHost()
                                val message = error ?: "Password sent securely via HID."
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }

                            if (biometricMacAutofillEnabled) {
                                if (biometricAuthenticator?.isBiometricAvailable() == true) {
                                    biometricAuthenticator.authenticate(
                                        title = "Hackie Mac Autofill",
                                        subtitle = "Authenticate to type your Mac password",
                                        onSuccess = dispatchAutofill,
                                        onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                                    )
                                } else {
                                    Toast.makeText(context, "Biometric authentication is not available on this device.", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                dispatchAutofill()
                            }
                        },
                        enabled = connectionState is HidDeviceManager.ConnectionState.Connected,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Authenticate & Autofill Now", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ─── Interaction & Gestures ───
            PremiumSectionHeader("INTERACTION")
            PremiumGlassCard {
                SettingsToggleItem(
                    title = "Shake to Disconnect",
                    subtitle = "Physically shake phone to end Mac connection",
                    icon = Icons.Default.Vibration,
                    iconColor = AccentPink,
                    checked = shakeToDisconnect,
                    onCheckedChange = { viewModel.setShakeToDisconnectEnabled(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsToggleItem(
                    title = "Auto Reconnect",
                    subtitle = "Handled in main settings, but here for control",
                    icon = Icons.Default.Sync,
                    iconColor = AccentBlue,
                    checked = autoReconnect,
                    onCheckedChange = { viewModel.setAutoReconnectEnabled(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                
                // Haptic Tactile Engine
                val currentHaptic by viewModel.hapticPreset.collectAsState()
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Dns, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Tactile Engine", color = Platinum, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Soft", "Mechanical", "Sharp").forEach { preset ->
                            val isSelected = currentHaptic == preset
                            Surface(
                                onClick = { viewModel.setHapticPreset(preset) },
                                color = if (isSelected) AccentTeal.copy(alpha = 0.15f) else Graphite.copy(alpha = 0.3f),
                                contentColor = if (isSelected) AccentTeal else Silver,
                                shape = RoundedCornerShape(8.dp),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, AccentTeal.copy(alpha = 0.5f)) else null,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = preset,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // ── Voice & Speech Section ──
            PremiumSectionHeader("VOICE & SPEECH ENGINE")
            PremiumGlassCard {
                val ttsPitch by viewModel.ttsPitch.collectAsState()
                val ttsSpeechRate by viewModel.ttsSpeechRate.collectAsState()
                
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = AccentPink, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Vocal Pitch", color = Platinum, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = ttsPitch,
                        onValueChange = { viewModel.setTtsPitch(it) },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = AccentPink, activeTrackColor = AccentPink.copy(alpha = 0.5f))
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Speech Rate", color = Platinum, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = ttsSpeechRate,
                        onValueChange = { viewModel.setTtsSpeechRate(it) },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = AccentBlue, activeTrackColor = AccentBlue.copy(alpha = 0.5f))
                    )
                }
            }

            PremiumSectionHeader("FEATURE VISIBILITY")
            PremiumGlassCard {
                SettingsToggleItem(
                    title = "Show Web Bridge",
                    subtitle = "Browser portal and file sync pages",
                    icon = Icons.Default.CloudSync,
                    iconColor = AccentBlue,
                    checked = featureWebBridgeVisible,
                    onCheckedChange = { viewModel.setFeatureWebBridgeVisible(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsToggleItem(
                    title = "Show Automation + Shortcuts",
                    subtitle = "Macro dashboard, shortcut guide, and quick tools",
                    icon = Icons.Default.Bolt,
                    iconColor = AccentGold,
                    checked = featureAutomationVisible,
                    onCheckedChange = { viewModel.setFeatureAutomationVisible(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsToggleItem(
                    title = "Show AI Assistant",
                    subtitle = "Chat assistant screen and tools",
                    icon = Icons.Default.AutoAwesome,
                    iconColor = AccentTeal,
                    checked = featureAssistantVisible,
                    onCheckedChange = { viewModel.setFeatureAssistantVisible(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsToggleItem(
                    title = "Show Snippets",
                    subtitle = "Saved text snippets screen",
                    icon = Icons.AutoMirrored.Filled.Notes,
                    iconColor = AccentPurple,
                    checked = featureSnippetsVisible,
                    onCheckedChange = { viewModel.setFeatureSnippetsVisible(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsToggleItem(
                    title = "Show Wake-on-LAN",
                    subtitle = "Network power-on tool",
                    icon = Icons.Default.PowerSettingsNew,
                    iconColor = SuccessGreen,
                    checked = featureWakeOnLanVisible,
                    onCheckedChange = { viewModel.setFeatureWakeOnLanVisible(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsToggleItem(
                    title = "Show SSH Terminal",
                    subtitle = "Native secure shell screen",
                    icon = Icons.Default.Terminal,
                    iconColor = AccentOrange,
                    checked = featureSshTerminalVisible,
                    onCheckedChange = { viewModel.setFeatureSshTerminalVisible(it) }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

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
                Button(onClick = { viewModel.setMacPassword(tempPass); showPasswordDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showPasswordDialog = false }) { Text("Cancel", color = Silver) } }
        )
    }
}
