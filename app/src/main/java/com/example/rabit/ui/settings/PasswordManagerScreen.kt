package com.example.rabit.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.data.secure.BiometricAuthenticator
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.components.PremiumGlassCard
import com.example.rabit.ui.components.PremiumSectionHeader
import com.example.rabit.ui.theme.AccentBlue
import com.example.rabit.ui.theme.AccentGold
import com.example.rabit.ui.theme.AccentOrange
import com.example.rabit.ui.theme.AccentTeal
import com.example.rabit.ui.theme.BorderColor
import com.example.rabit.ui.theme.Graphite
import com.example.rabit.ui.theme.Obsidian
import com.example.rabit.ui.theme.Platinum
import com.example.rabit.ui.theme.Silver
import com.example.rabit.ui.theme.SuccessGreen

@Composable
fun PasswordManagerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricAuthenticator = remember(activity) { activity?.let { BiometricAuthenticator(it) } }

    val connectionState by viewModel.connectionState.collectAsState()
    val biometricMacAutofillEnabled by viewModel.biometricMacAutofillEnabled.collectAsState()
    val macAutofillPreEnter by viewModel.macAutofillPreEnter.collectAsState()
    val macAutofillPostEnter by viewModel.macAutofillPostEnter.collectAsState()
    val macPassword by viewModel.macPassword.collectAsState()
    val vaultEntries by viewModel.passwordVaultEntries.collectAsState()

    var showPasswordDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showVaultEntryDialog by remember { mutableStateOf(false) }

    Scaffold(containerColor = Obsidian) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PremiumSectionHeader("PASSWORD VAULT")
            PremiumGlassCard {
                Text(
                    text = "Mac unlock secret is stored in encrypted storage on this phone.",
                    color = Silver,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = BorderColor.copy(alpha = 0.4f)
                )

                SettingsToggleItem(
                    title = "Biometric Gate",
                    subtitle = "Require fingerprint/face before autofill",
                    icon = Icons.Default.Fingerprint,
                    iconColor = SuccessGreen,
                    checked = biometricMacAutofillEnabled,
                    onCheckedChange = { viewModel.setBiometricMacAutofillEnabled(it) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = BorderColor.copy(alpha = 0.4f)
                )

                SettingsToggleItem(
                    title = "Press Enter Before Typing",
                    subtitle = "Wake login field before password injection",
                    icon = Icons.Default.Keyboard,
                    iconColor = AccentBlue,
                    checked = macAutofillPreEnter,
                    onCheckedChange = { viewModel.setMacAutofillPreEnter(it) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = BorderColor.copy(alpha = 0.4f)
                )

                SettingsToggleItem(
                    title = "Press Enter After Typing",
                    subtitle = "Auto-submit after password injection",
                    icon = Icons.Default.Key,
                    iconColor = AccentTeal,
                    checked = macAutofillPostEnter,
                    onCheckedChange = { viewModel.setMacAutofillPostEnter(it) }
                )
            }

            PremiumSectionHeader("VAULT ACTIONS")
            PremiumGlassCard {
                Text(
                    text = if (macPassword.isBlank()) "No password stored" else "Password stored securely",
                    color = if (macPassword.isBlank()) AccentOrange else SuccessGreen,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )

                Button(
                    onClick = { showPasswordDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Icon(Icons.Default.Password, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (macPassword.isBlank()) "Set Mac Password" else "Update Mac Password")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val autofillNow = {
                            val error = viewModel.sendStoredMacPasswordToHost()
                            Toast.makeText(context, error ?: "Password sent securely.", Toast.LENGTH_SHORT).show()
                        }

                        if (biometricMacAutofillEnabled) {
                            if (biometricAuthenticator?.isBiometricAvailable() == true) {
                                biometricAuthenticator.authenticate(
                                    title = "Hackie Password Manager",
                                    subtitle = "Authenticate to inject password",
                                    onSuccess = autofillNow,
                                    onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                                )
                            } else {
                                Toast.makeText(context, "Biometric auth is unavailable on this device.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            autofillNow()
                        }
                    },
                    enabled = connectionState is HidDeviceManager.ConnectionState.Connected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null)
                    Text("Authenticate & Push to Mac")
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = AccentGold)
                    Text("Clear Stored Password", color = AccentGold)
                }
            }

            PremiumSectionHeader("APP PASSWORD VAULT")
            PremiumGlassCard {
                Text(
                    text = "Store passwords by app/site name. Tap Push after biometric check to send the selected password to your Mac.",
                    color = Silver,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )

                Button(
                    onClick = { showVaultEntryDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Vault Entry")
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (vaultEntries.isEmpty()) {
                    Text(
                        text = "No app passwords saved yet.",
                        color = Silver.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                } else {
                    vaultEntries.forEach { entry ->
                        PremiumGlassCard {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Text(entry.appName, color = Platinum, fontWeight = FontWeight.Bold)
                                if (entry.username.isNotBlank()) {
                                    Text("User: ${entry.username}", color = Silver, fontSize = 12.sp)
                                }
                                if (entry.notes.isNotBlank()) {
                                    Text(entry.notes, color = Silver.copy(alpha = 0.8f), fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = {
                                            val pushAction = {
                                                val error = viewModel.sendVaultPasswordToHost(entry.id)
                                                Toast.makeText(context, error ?: "Password pushed for ${entry.appName}", Toast.LENGTH_SHORT).show()
                                            }

                                            if (biometricMacAutofillEnabled) {
                                                if (biometricAuthenticator?.isBiometricAvailable() == true) {
                                                    biometricAuthenticator.authenticate(
                                                        title = "Unlock ${entry.appName}",
                                                        subtitle = "Authenticate to push password",
                                                        onSuccess = pushAction,
                                                        onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                                                    )
                                                } else {
                                                    Toast.makeText(context, "Biometric auth is unavailable on this device.", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                pushAction()
                                            }
                                        },
                                        enabled = connectionState is HidDeviceManager.ConnectionState.Connected,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                                    ) {
                                        Icon(Icons.Default.Fingerprint, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Push")
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.deleteVaultEntry(entry.id) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentOrange)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Delete")
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            PremiumSectionHeader("STATUS")
            PremiumGlassCard {
                Text(
                    text = if (connectionState is HidDeviceManager.ConnectionState.Connected) {
                        "Connected to host. Password autofill is available."
                    } else {
                        "Host disconnected. Connect to use password push."
                    },
                    color = Silver,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showPasswordDialog) {
        var tempPass by remember { mutableStateOf(macPassword) }
        var hidden by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            containerColor = Graphite,
            title = { Text("Set Mac Unlock Password", color = Platinum) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tempPass,
                        onValueChange = { tempPass = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (hidden) PasswordVisualTransformation() else VisualTransformation.None,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            TextButton(onClick = { hidden = !hidden }) {
                                Text(if (hidden) "Show" else "Hide")
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Platinum,
                            unfocusedTextColor = Platinum
                        )
                    )
                    Text("Stored locally using encrypted app storage.", color = Silver)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setMacPassword(tempPass)
                        showPasswordDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text("Cancel", color = Silver)
                }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = Graphite,
            title = { Text("Clear Stored Password?", color = Platinum) },
            text = { Text("This removes the saved Mac password from local encrypted storage.", color = Silver) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearMacPassword()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = Silver)
                }
            }
        )
    }

    if (showVaultEntryDialog) {
        var appName by remember { mutableStateOf("") }
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var hidden by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showVaultEntryDialog = false },
            containerColor = Graphite,
            title = { Text("Add Vault Entry", color = Platinum) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = appName,
                        onValueChange = { appName = it },
                        label = { Text("App / Site Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Platinum,
                            unfocusedTextColor = Platinum
                        )
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Platinum,
                            unfocusedTextColor = Platinum
                        )
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (hidden) PasswordVisualTransformation() else VisualTransformation.None,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            TextButton(onClick = { hidden = !hidden }) {
                                Text(if (hidden) "Show" else "Hide")
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Platinum,
                            unfocusedTextColor = Platinum
                        )
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Platinum,
                            unfocusedTextColor = Platinum
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addOrUpdateVaultEntry(appName, username, password, notes)
                        showVaultEntryDialog = false
                    },
                    enabled = appName.isNotBlank() && password.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVaultEntryDialog = false }) {
                    Text("Cancel", color = Silver)
                }
            }
        )
    }
}
