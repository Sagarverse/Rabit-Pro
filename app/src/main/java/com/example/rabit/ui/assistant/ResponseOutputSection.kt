package com.example.rabit.ui.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
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

    when (uiState) {
        is AssistantUiState.Idle -> {
            Text("Ask something to Gemini...", color = Silver, fontSize = 15.sp, modifier = Modifier.padding(8.dp))
        }
        is AssistantUiState.Loading -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Thinking...", color = Silver, fontSize = 15.sp)
            }
        }
        is AssistantUiState.Success -> {
            GeminiResponseBox(uiState.response, viewModel)
        }
        is AssistantUiState.Error -> {
            Text("Error: ${uiState.message}", color = ErrorRed, fontSize = 15.sp, modifier = Modifier.padding(8.dp))
        }
    }
}

@Composable
fun GeminiResponseBox(response: GeminiResponse, viewModel: AssistantViewModel) {
    val context = LocalContext.current
    val autoPush by viewModel.autoPushEnabled.collectAsState()
    val hidManager = remember { BluetoothHidServiceProvider.getInstance(context) }

    LaunchedEffect(response.text) {
        if (response.text.isNotBlank()) {
            AssistantNotifier.showNotification(context, response.text)
            AssistantNotifier.playSound(context)
            AssistantNotifier.vibrate(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Graphite, shape = MaterialTheme.shapes.medium)
            .padding(12.dp)
    ) {
        Box(modifier = Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState())) {
            Text(response.text, color = Platinum, fontSize = 15.sp)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                IconButton(onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Gemini Response", response.text)
                    clipboard.setPrimaryClip(clip)
                }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = {
                    val sendIntent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_TEXT, response.text)
                        type = "text/plain"
                    }
                    context.startActivity(android.content.Intent.createChooser(sendIntent, null))
                }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(20.dp))
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Auto Push", color = Silver, fontSize = 12.sp)
                Switch(
                    checked = autoPush,
                    onCheckedChange = { viewModel.setAutoPushEnabled(it) },
                    modifier = Modifier.scale(0.7f),
                    colors = SwitchDefaults.colors(checkedTrackColor = SuccessGreen)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = { hidManager.sendText(response.text) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Push", fontSize = 12.sp)
                }
            }
        }
    }
}
