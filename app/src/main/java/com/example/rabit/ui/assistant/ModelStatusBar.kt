package com.example.rabit.ui.assistant

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

@Composable
fun LocalModelStatusBar(
    state: com.example.rabit.data.gemini.ModelLoadState,
    progress: Float,
    error: String?,
    onDismiss: () -> Unit = {}
) {
    AnimatedVisibility(
        visible = state != com.example.rabit.data.gemini.ModelLoadState.IDLE && state != com.example.rabit.data.gemini.ModelLoadState.READY,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Surface(
            color = if (state == com.example.rabit.data.gemini.ModelLoadState.ERROR)
                AccentBlue.copy(alpha = 0.08f) else Graphite.copy(alpha = 0.5f),
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(
                0.5.dp,
                if (state == com.example.rabit.data.gemini.ModelLoadState.ERROR)
                    AccentBlue.copy(alpha = 0.25f) else BorderColor.copy(alpha = 0.1f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (state) {
                        com.example.rabit.data.gemini.ModelLoadState.DOWNLOADING -> {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Platinum)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Downloading AI Model (~1.1GB)…", color = Platinum, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        com.example.rabit.data.gemini.ModelLoadState.COPYING -> {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Silver)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Copying Model to Internal Storage…", color = Platinum, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        com.example.rabit.data.gemini.ModelLoadState.INITIALIZING -> {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = AiOrbGlow)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Waking up Local LLM…", color = Platinum, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        com.example.rabit.data.gemini.ModelLoadState.ERROR -> {
                            val isNetworkError = error?.contains("Network", ignoreCase = true) == true || 
                                               error?.contains("DNS", ignoreCase = true) == true ||
                                               error?.contains("resolve", ignoreCase = true) == true
                            
                            Icon(
                                imageVector = if (isNetworkError) Icons.Default.WifiOff else Icons.Default.ErrorOutline, 
                                contentDescription = null, 
                                tint = AccentBlue, 
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                if (isNetworkError) "Network Connectivity Issue" else "Model Engine Failure", 
                                color = AccentBlue, 
                                fontSize = 13.sp, 
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Silver, modifier = Modifier.size(18.dp))
                            }
                        }
                        else -> {}
                    }
                }
                if (state == com.example.rabit.data.gemini.ModelLoadState.COPYING || state == com.example.rabit.data.gemini.ModelLoadState.DOWNLOADING) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = Platinum,
                        trackColor = SoftGrey.copy(alpha = 0.3f)
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        color = Silver.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (state == com.example.rabit.data.gemini.ModelLoadState.ERROR && error != null) {
                    Text(
                        text = error,
                        color = AccentBlue.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
