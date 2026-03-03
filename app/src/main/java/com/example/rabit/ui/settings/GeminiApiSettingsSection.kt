package com.example.rabit.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

@Composable
fun GeminiApiSettingsSection(viewModel: GeminiSettingsViewModel) {
    val apiKey by viewModel.apiKey.collectAsState()
    val status by viewModel.status.collectAsState()
    val showApiKey by viewModel.showApiKey.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()
    val isOfflineMode by viewModel.isOfflineMode.collectAsState()
    val ggufPath by viewModel.ggufPath.collectAsState()
    
    var isVerifying by remember { mutableStateOf(false) }
    var showModelMenu by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        viewModel.setGgufPath(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Graphite, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("AI CONFIGURATION", color = Silver, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (isOfflineMode) "OFFLINE" else "ONLINE", color = if (isOfflineMode) AccentGold else SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isOfflineMode,
                    onCheckedChange = { viewModel.setOfflineMode(it) },
                    modifier = Modifier.scale(0.7f),
                    colors = SwitchDefaults.colors(checkedTrackColor = AccentGold)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        if (isOfflineMode) {
            Text("Local Model (.bin)", color = Silver, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { filePickerLauncher.launch(arrayOf("*/*")) },
                color = SoftGrey,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = AccentGold, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = ggufPath?.substringAfterLast("/") ?: "Select MediaPipe model (.bin)",
                        color = if (ggufPath != null) Platinum else Silver.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }
            }
            
            if (ggufPath != null) {
                Text(
                    "Model loaded. Note: MediaPipe requires specific .bin format converted from GGUF/LoRA.",
                    color = SuccessGreen,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                Text(
                    "Use converted Gemma 2B or Phi-2 models for best results.",
                    color = WarningYellow,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else {
            Text("Cloud Model", color = Silver, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            Box {
                OutlinedButton(
                    onClick = { showModelMenu = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Platinum)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedModel)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
                DropdownMenu(
                    expanded = showModelMenu,
                    onDismissRequest = { showModelMenu = false },
                    modifier = Modifier.fillMaxWidth(0.8f).background(Graphite)
                ) {
                    availableModels.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model, color = Platinum) },
                            onClick = {
                                viewModel.setSelectedModel(model)
                                showModelMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Gemini API Key", color = Silver, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = { viewModel.onApiKeyChanged(it) },
                label = { Text("Paste API Key") },
                singleLine = true,
                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Row {
                        IconButton(onClick = { viewModel.toggleShowApiKey() }) {
                            Icon(
                                if (showApiKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                        if (apiKey.isNotBlank()) {
                            IconButton(onClick = { viewModel.clearApiKey() }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        isVerifying = true
                        viewModel.verifyApiKey()
                    },
                    enabled = apiKey.isNotBlank() && !isVerifying,
                    colors = ButtonDefaults.buttonColors(containerColor = SoftGrey)
                ) {
                    if (status is ApiKeyStatus.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Platinum)
                    } else {
                        Text("Verify", color = Platinum)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.saveApiKey() },
                    enabled = apiKey.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save")
                }
                Spacer(modifier = Modifier.width(12.dp))
                GeminiApiStatusDot(status)
            }
            
            if (status != ApiKeyStatus.Idle) {
                Spacer(modifier = Modifier.height(8.dp))
                GeminiApiStatusText(status)
            }
        }
    }
}

@Composable
fun GeminiApiStatusDot(status: ApiKeyStatus) {
    val color = when (status) {
        ApiKeyStatus.Connected -> SuccessGreen
        ApiKeyStatus.Invalid -> ErrorRed
        ApiKeyStatus.NetworkError -> WarningYellow
        else -> Color.Gray
    }
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun GeminiApiStatusText(status: ApiKeyStatus) {
    val text: String = when (status) {
        ApiKeyStatus.Connected -> "Connected Successfully"
        ApiKeyStatus.Invalid -> "Invalid Key"
        ApiKeyStatus.NetworkError -> "Network Error"
        ApiKeyStatus.Loading -> "Verifying..."
        else -> ""
    }
    val color: Color = when (status) {
        ApiKeyStatus.Connected -> SuccessGreen
        ApiKeyStatus.Invalid -> ErrorRed
        ApiKeyStatus.NetworkError -> WarningYellow
        ApiKeyStatus.Loading -> Color.Gray
        else -> Color.Transparent
    }
    if (text.isNotBlank()) {
        Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
