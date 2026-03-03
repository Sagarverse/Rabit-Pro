package com.example.rabit.ui.assistant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

@Composable
fun PromptInputSection(viewModel: AssistantViewModel) {
    val input by viewModel.input.collectAsState()
    val systemInstruction by viewModel.systemInstruction.collectAsState()
    var isRecording by remember { mutableStateOf(false) }
    var showSystemPrompt by remember { mutableStateOf(false) }
    val charCount = input.length
    val maxChars = 2000

    val context = LocalContext.current
    val settingsProvider = remember { AssistantSettingsProvider(context) }
    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState is AssistantUiState.Loading
    var selectedLength by remember { mutableStateOf("Medium") }
    var temperature by remember { mutableStateOf(0.7f) }

    val prefs = remember { context.getSharedPreferences("gemini_prefs", android.content.Context.MODE_PRIVATE) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // System Instructions (Predefined commands)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { showSystemPrompt = !showSystemPrompt },
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    if (showSystemPrompt) Icons.Default.KeyboardArrowUp else Icons.Default.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = AccentGold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "SYSTEM INSTRUCTIONS", 
                    color = AccentGold, 
                    fontSize = 11.sp, 
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            if (systemInstruction.isNotBlank()) {
                Text("Active", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        AnimatedVisibility(visible = showSystemPrompt) {
            OutlinedTextField(
                value = systemInstruction,
                onValueChange = { viewModel.onSystemInstructionChanged(it) },
                label = { Text("Predefined commands (e.g. 'Reply in 2 sentences only')") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold,
                    unfocusedBorderColor = BorderColor
                ),
                maxLines = 3
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = {
                    if (it.length <= maxChars) viewModel.onInputChanged(it)
                },
                label = { Text("Type your prompt...") },
                modifier = Modifier.weight(1f),
                singleLine = false,
                maxLines = 4,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { /* handled by send button */ })
            )
            IconButton(onClick = { viewModel.clearInput() }, enabled = input.isNotBlank() && !isLoading) {
                Icon(Icons.Default.Clear, contentDescription = "Clear")
            }
            SpeechToTextButton(
                onResult = { viewModel.onInputChanged(it) },
                isRecording = isRecording,
                onRecordingStateChange = { isRecording = it }
            )
            IconButton(
                onClick = {
                    if (!isLoading && input.isNotBlank()) {
                        val maxTokens = when (selectedLength) {
                            "Short" -> 256
                            "Medium" -> 512
                            "Detailed" -> 1024
                            else -> 512
                        }
                        val currentModel = prefs.getString("selected_model", "gemini-pro-latest") ?: "gemini-pro-latest"
                        viewModel.sendPrompt(
                            apiKey = settingsProvider.getApiKey() ?: "",
                            temperature = temperature,
                            maxTokens = maxTokens,
                            model = currentModel
                        )
                    }
                },
                enabled = input.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PromptTemplateDropdown { template ->
                    if (template.isNotBlank()) viewModel.onInputChanged(template)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Length:", color = Silver, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(4.dp))
                var expanded by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { expanded = true }) {
                    Text(selectedLength, fontSize = 13.sp)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("Short", "Medium", "Detailed").forEach { label ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                selectedLength = label
                                expanded = false
                            }
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Temp:", color = Silver, fontSize = 11.sp)
                Slider(
                    value = temperature,
                    onValueChange = { temperature = it },
                    valueRange = 0f..1f,
                    steps = 8,
                    modifier = Modifier.width(80.dp)
                )
                Text(String.format("%.1f", temperature), color = Silver, fontSize = 11.sp)
            }
            Text("$charCount/$maxChars", color = Silver, fontSize = 11.sp)
        }
    }
}
