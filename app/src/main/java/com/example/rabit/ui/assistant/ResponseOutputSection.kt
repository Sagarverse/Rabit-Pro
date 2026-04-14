package com.example.rabit.ui.assistant

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.domain.model.gemini.GeminiResponse
import com.example.rabit.ui.theme.*

@Composable
fun ResponseOutputSection(uiState: AssistantUiState, viewModel: AssistantViewModel) {
    val autoPush by viewModel.autoPushEnabled.collectAsState()
    val context = LocalContext.current
    val hidManager = remember { BluetoothHidServiceProvider.getInstance(context) }

    LaunchedEffect(uiState) {
        if (uiState is AssistantUiState.Success && autoPush) {
            hidManager.sendText(uiState.response.text)
        }
    }

    AnimatedContent(
        targetState = uiState,
        transitionSpec = {
            fadeIn() + slideInVertically { it / 4 } togetherWith fadeOut()
        },
        label = "responseTransition"
    ) { state ->
        when (state) {
            is AssistantUiState.Idle -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AccentBlue.copy(alpha = 0.3f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Ask something to Gemini…",
                            color = Silver.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
            is AssistantUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = AccentBlue,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Thinking…", color = Silver, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            is AssistantUiState.Success -> {
                GeminiResponseBox(state.response, viewModel)
            }
            is AssistantUiState.Error -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AccentBlue.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(state.message, color = AccentBlue, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun GeminiResponseBox(response: GeminiResponse, viewModel: AssistantViewModel) {
    val context = LocalContext.current
    val autoPush by viewModel.autoPushEnabled.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val hidManager = remember { BluetoothHidServiceProvider.getInstance(context) }

    LaunchedEffect(response.text) {
        if (response.text.isNotBlank()) {
            AssistantNotifier.showNotification(context, response.text)
            AssistantNotifier.vibrate(context)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Graphite,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Response header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("GEMINI", color = AccentBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // USER PROMPT CONTEXT
            if (response.promptText.isNotBlank() || response.attachedImageUris.isNotEmpty()) {
                Surface(
                    color = Obsidian.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        if (response.attachedImageUris.isNotEmpty()) {
                            Row(
                                modifier = Modifier.padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                response.attachedImageUris.forEach { uri ->
                                    Box {
                                        coil.compose.AsyncImage(
                                            model = coil.request.ImageRequest.Builder(context).data(uri).crossfade(true).build(),
                                            contentDescription = null,
                                            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                        if (response.promptText.isNotBlank()) {
                            Text(
                                text = response.promptText,
                                color = Silver.copy(alpha = 0.8f),
                                fontSize = 13.sp,
                                maxLines = 3,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Response text area
            Box(
                modifier = Modifier
                    .heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                com.example.rabit.ui.components.MarkdownText(text = response.text, color = Platinum, fontSize = 15f)
            }
            
            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(thickness = 0.5.dp, color = BorderColor.copy(alpha = 0.4f))

            Spacer(modifier = Modifier.height(10.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Copy
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Gemini Response", response.text)
                        clipboard.setPrimaryClip(clip)
                    }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Silver, modifier = Modifier.size(18.dp))
                    }
                    // Share
                    IconButton(onClick = {
                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, response.text)
                            type = "text/plain"
                        }
                        context.startActivity(android.content.Intent.createChooser(sendIntent, null))
                    }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Silver, modifier = Modifier.size(18.dp))
                    }
                    // Speak
                    IconButton(onClick = { viewModel.speakText(response.text) }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            if (isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Play Aloud",
                            tint = if (isSpeaking) AccentBlue else Silver,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Auto-Push Toggle with Label
                    Row(
                        modifier = Modifier
                            .background(SoftGrey.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("AUTO", color = Silver.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = autoPush,
                            onCheckedChange = { viewModel.setAutoPushEnabled(it) },
                            modifier = Modifier.scale(0.65f),
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = AccentBlue,
                                checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                                uncheckedThumbColor = androidx.compose.ui.graphics.Color.White,
                                uncheckedTrackColor = SoftGrey
                            )
                        )
                    }

                    // Push Button
                    Button(
                        onClick = { hidManager.sendText(response.text) },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(10.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PUSH", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
