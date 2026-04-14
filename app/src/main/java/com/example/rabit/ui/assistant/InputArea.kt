package com.example.rabit.ui.assistant

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.components.PushControlBar
import com.example.rabit.ui.theme.*

@Composable
fun PremiumInputArea(viewModel: AssistantViewModel, mainViewModel: MainViewModel) {
    val isPushPaused by mainViewModel.isPushPaused.collectAsState(initial = false)
    val isTextPushing by mainViewModel.isTextPushing.collectAsState(initial = false)
    val messages by viewModel.messages.collectAsState(initial = emptyList())

    Surface(
        color = ChatSurface,
        tonalElevation = 0.dp,
        modifier = Modifier
            .drawBehind {
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            AccentBlue.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1f
                )
            }
    ) {
        Column {
            // Hardware Push Control Bar
            AnimatedVisibility(
                visible = isPushPaused || isTextPushing,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                PushControlBar(
                    isPaused = isPushPaused,
                    onPause = { mainViewModel.pauseTextPush() },
                    onResume = { mainViewModel.resumeTextPush() },
                    onStop = { mainViewModel.stopTextPush() }
                )
            }

            // Auto-Push Toggle
            val autoPush by viewModel.autoPushEnabled.collectAsState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (autoPush) Platinum.copy(alpha = 0.1f) else SoftGrey.copy(alpha = 0.15f),
                    border = BorderStroke(
                        0.5.dp,
                        if (autoPush) Platinum.copy(alpha = 0.25f) else Color.Transparent
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (autoPush) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = null,
                            tint = if (autoPush) Platinum else Silver.copy(alpha = 0.4f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "AUTO PUSH TO MAC",
                            color = if (autoPush) Platinum else Silver.copy(alpha = 0.4f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = autoPush,
                            onCheckedChange = { viewModel.setAutoPushEnabled(it) },
                            modifier = Modifier.scale(0.6f),
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = Platinum,
                                uncheckedTrackColor = SoftGrey
                            )
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                // Quick Prompts Row (visible when chat is active)
                if (messages.isNotEmpty()) {
                    val quickPrompts = listOf("Explain", "Summarize", "Debug", "Refactor", "Translate")
                    Text(
                        "Quick Actions",
                        color = Silver.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)
                    )
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp, start = 16.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(quickPrompts) { promptLabel ->
                            Surface(
                                onClick = {
                                    val prefix = when(promptLabel) {
                                        "Explain" -> "Explain the following:\n\n"
                                        "Summarize" -> "Summarize this:\n\n"
                                        "Debug" -> "Find and fix issues in this code:\n\n"
                                        "Refactor" -> "Refactor this code to be more concise:\n\n"
                                        "Translate" -> "Translate the following to "
                                        else -> ""
                                    }
                                    val current = viewModel.input.value
                                    viewModel.onInputChanged(if (current.isBlank()) prefix else "$prefix$current")
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = Graphite.copy(alpha = 0.45f),
                                border = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = promptLabel,
                                    color = Platinum.copy(alpha = 0.88f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
                PromptInputSection(viewModel)
            }
        }
    }
}
