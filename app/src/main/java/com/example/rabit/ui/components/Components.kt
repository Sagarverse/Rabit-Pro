package com.example.rabit.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

@Composable
fun PremiumGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .border(0.5.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        color = Graphite.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun VibrantGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradient: Brush = PremiumBlueGradient
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(),
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(gradient),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PremiumSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = Silver,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp, top = 24.dp)
    )
}

/**
 * Floating control bar for pause/resume/stop during text push operations.
 * Appears at the bottom of the screen when text is being pushed over Bluetooth.
 */
@Composable
fun PushControlBar(
    isPaused: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pushPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (isPaused) PausedAmber.copy(alpha = 0.15f) else AccentBlue.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPaused) PausedAmber.copy(alpha = 0.4f) else AccentBlue.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Animated typing indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (isPaused) PausedAmber else SuccessGreen.copy(alpha = pulseAlpha),
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    if (isPaused) "PAUSED" else "TYPING…",
                    color = if (isPaused) PausedAmber else Platinum,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Pause / Resume button
                FilledIconButton(
                    onClick = { if (isPaused) onResume() else onPause() },
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isPaused) SuccessGreen else PausedAmber
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Stop button
                FilledIconButton(
                    onClick = onStop,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = StopRed
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Icon with colored background circle — used in settings for iOS-style icon indicators.
 */
@Composable
fun SettingsIconBadge(
    icon: ImageVector,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    iconTint: Color = Color.White
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .background(backgroundColor, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Device type badge with icon and label for the pairing screen.
 */
@Composable
fun DeviceTypeBadge(
    deviceType: DeviceType,
    modifier: Modifier = Modifier
) {
    val (icon, label, color) = when (deviceType) {
        DeviceType.MAC -> Triple(Icons.Default.Laptop, "Mac", MacDeviceColor)
        DeviceType.ANDROID -> Triple(Icons.Default.PhoneAndroid, "Android", AndroidDeviceColor)
        DeviceType.UNKNOWN -> Triple(Icons.Default.Devices, "Device", UnknownDeviceColor)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

enum class DeviceType {
    MAC, ANDROID, UNKNOWN
}

/**
 * Determine device type from Bluetooth device name heuristics.
 */
fun guessDeviceType(name: String): DeviceType {
    val lower = name.lowercase()
    return when {
        lower.contains("macbook") || lower.contains("imac") || lower.contains("mac pro") ||
        lower.contains("mac mini") || lower.contains("mac studio") || lower.contains("apple") -> DeviceType.MAC
        lower.contains("android") || lower.contains("galaxy") || lower.contains("pixel") ||
        lower.contains("samsung") || lower.contains("oneplus") || lower.contains("xiaomi") ||
        lower.contains("redmi") || lower.contains("oppo") || lower.contains("vivo") ||
        lower.contains("realme") || lower.contains("poco") || lower.contains("motorola") ||
        lower.contains("huawei") || lower.contains("nokia") || lower.contains("lg") ||
        lower.contains("sony") || lower.contains("asus") || lower.contains("zte") -> DeviceType.ANDROID
        // Filter out known Windows indicators
        lower.contains("windows") || lower.contains("surface") || lower.contains("dell") ||
        lower.contains("hp ") || lower.contains("lenovo") || lower.contains("thinkpad") ||
        lower.contains("asus desktop") -> DeviceType.UNKNOWN
        else -> DeviceType.UNKNOWN
    }
}

/**
 * Connection quality indicator pill that shows signal quality.
 */
@Composable
fun ConnectionQualityIndicator(
    isConnected: Boolean,
    deviceName: String = ""
) {
    if (!isConnected) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .background(SuccessGreen.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .border(0.5.dp, SuccessGreen.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(SuccessGreen, CircleShape)
        )
        Text(
            if (deviceName.isNotBlank()) deviceName else "Connected",
            color = SuccessGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
