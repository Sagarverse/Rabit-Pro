package com.example.rabit.ui.assistant

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumChatTopBar(
    modelName: String,
    isThinking: Boolean,
    connectionState: com.example.rabit.data.bluetooth.HidDeviceManager.ConnectionState,
    onLeftPanelClick: () -> Unit,
    onRightPanelClick: () -> Unit,
    onClearChat: () -> Unit,
    onNewChat: () -> Unit,
    onExportChat: () -> Unit,
    onSettingsClick: () -> Unit
) {
    var showMoreMenu by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "orbPulse")
    val orbAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isThinking) AssistantMotion.PULSE_FAST else AssistantMotion.PULSE_IDLE, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "orbAlpha"
    )
    val orbColor by animateColorAsState(
        targetValue = if (isThinking) AccentBlue else AiOrbGlow,
        animationSpec = tween(AssistantMotion.COLOR_TWEEN),
        label = "orbColor"
    )
    val statusColor by animateColorAsState(
        targetValue = if (isThinking) AccentBlue.copy(alpha = 0.9f) else Platinum.copy(alpha = 0.7f),
        animationSpec = tween(AssistantMotion.COLOR_TWEEN),
        label = "statusColor"
    )

    Surface(
        modifier = Modifier.statusBarsPadding(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(ChatSurface, ChatSurface.copy(alpha = 0.95f), Color.Transparent)
                    )
                )
        ) {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Model indicator with subtle pulse while thinking
                        Box(
                            modifier = Modifier
                                .size(34.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (modelName.contains("Gemini")) "G" else "M",
                                color = orbColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                modifier = Modifier.alpha(if (isThinking) orbAlpha else 1f)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                modelName,
                                color = Platinum,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (isThinking) AccentBlue
                                            else if (connectionState is com.example.rabit.data.bluetooth.HidDeviceManager.ConnectionState.Connected) SuccessGreen
                                            else Silver,
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    when {
                                        isThinking -> "Generating…"
                                        connectionState is com.example.rabit.data.bluetooth.HidDeviceManager.ConnectionState.Connected -> "Ring: Connected"
                                        else -> "Ring: Disconnected"
                                    },
                                    color = statusColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onLeftPanelClick,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .background(Graphite.copy(alpha = 0.45f), CircleShape)
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Platinum, modifier = Modifier.size(18.dp))
                    }
                },
                actions = {
                    IconButton(
                        onClick = onRightPanelClick,
                        modifier = Modifier.background(Graphite.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Open right panel", tint = AccentBlue, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onNewChat,
                        modifier = Modifier.background(Graphite.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Chat", tint = Platinum, modifier = Modifier.size(18.dp))
                    }
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More actions", tint = Silver.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            containerColor = Graphite.copy(alpha = 0.95f)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export Chat", color = Platinum) },
                                leadingIcon = { Icon(Icons.Default.IosShare, contentDescription = null, tint = Silver.copy(alpha = 0.9f)) },
                                onClick = {
                                    showMoreMenu = false
                                    onExportChat()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear Chat", color = Platinum) },
                                leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Silver.copy(alpha = 0.85f)) },
                                onClick = {
                                    showMoreMenu = false
                                    onClearChat()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings", color = Platinum) },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = Silver.copy(alpha = 0.9f)) },
                                onClick = {
                                    showMoreMenu = false
                                    onSettingsClick()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    }
}
