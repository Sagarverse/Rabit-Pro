package com.example.rabit.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val biometricEnabled by viewModel.biometricLockEnabled.collectAsState()
    val shakeToDisconnect by viewModel.shakeToDisconnectEnabled.collectAsState()
    val stealthMode by viewModel.stealthModeEnabled.collectAsState()
    val dynamicTheme by viewModel.dynamicThemeEnabled.collectAsState()
    val autoReconnect by viewModel.autoReconnectEnabled.collectAsState()
    val password by viewModel.unlockPassword.collectAsState()

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

            // ─── Visual Engine ───
            PremiumSectionHeader("VISUAL ENGINE")
            PremiumGlassCard {
                SettingsToggleItem(
                    title = "Dynamic Color Engine",
                    subtitle = "Allow UI to use the premium accent palette",
                    icon = Icons.Default.AutoAwesome,
                    iconColor = AccentTeal,
                    checked = dynamicTheme,
                    onCheckedChange = { viewModel.setDynamicThemeEnabled(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))
                SettingsToggleItem(
                    title = "Glassmorphism Illumination",
                    subtitle = "Subtle glows behind premium cards",
                    icon = Icons.Default.BlurOn,
                    iconColor = AccentBlue,
                    checked = dynamicTheme, // Tied to dynamic theme for pro feel
                    onCheckedChange = { /* Tied to dynamic theme */ }
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
                Button(onClick = { viewModel.setUnlockPassword(tempPass); showPasswordDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showPasswordDialog = false }) { Text("Cancel", color = Silver) } }
        )
    }
}
