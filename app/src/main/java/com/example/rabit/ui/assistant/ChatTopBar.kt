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
    onMenuClick: () -> Unit,
    onClearChat: () -> Unit,
    onExportChat: () -> Unit,
    onLaunchpadClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbPulse")
    val orbAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isThinking) 800 else 2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "orbAlpha"
    )
    val orbColor by animateColorAsState(
        targetValue = if (isThinking) AccentGold else AiOrbGlow,
        animationSpec = tween(400),
        label = "orbColor"
    )
    val statusColor by animateColorAsState(
        targetValue = if (isThinking) AccentGold.copy(alpha = 0.8f) else Platinum.copy(alpha = 0.7f),
        animationSpec = tween(400),
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
                        // Animated AI Orb
                        Box(
                            modifier = Modifier.size(34.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Outer glow
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .alpha(orbAlpha * 0.35f)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(orbColor, Color.Transparent)
                                        ),
                                        CircleShape
                                    )
                            )
                            // Inner orb
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(orbColor, orbColor.copy(alpha = 0.6f))
                                        ),
                                        CircleShape
                                    )
                                    .border(1.dp, orbColor.copy(alpha = orbAlpha * 0.5f), CircleShape)
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
                                            if (isThinking) AccentGold
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
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Platinum)
                    }
                },
                actions = {
                    IconButton(onClick = onLaunchpadClick) {
                        Icon(Icons.Default.RocketLaunch, contentDescription = "Launchpad", tint = AccentGold.copy(alpha = 0.9f))
                    }
                    IconButton(onClick = onExportChat) {
                        Icon(Icons.Default.IosShare, contentDescription = "Export Chat", tint = Silver.copy(alpha = 0.8f))
                    }
                    IconButton(onClick = onClearChat) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Chat", tint = Silver.copy(alpha = 0.6f))
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Silver.copy(alpha = 0.8f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    }
}
