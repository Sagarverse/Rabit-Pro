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
import androidx.compose.ui.draw.shadow
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
    backgroundColor: Color = Graphite.copy(alpha = 0.72f),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(18.dp), clip = false)
            .clip(RoundedCornerShape(18.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .border(1.dp, BorderStrong.copy(alpha = 0.55f), RoundedCornerShape(18.dp)),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .background(GlassCardGradient)
                .padding(16.dp),
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
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(gradient),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.3.sp
            )
        }
    }
}

@Composable
fun PremiumSectionHeader(title: String) {
    Row(
        modifier = Modifier.padding(start = 10.dp, bottom = 8.dp, top = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 22.dp, height = 4.dp)
                .background(AccentBlue.copy(alpha = 0.8f), RoundedCornerShape(99.dp))
        )
        Text(
            text = title.uppercase(),
            color = Silver.copy(alpha = 0.92f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
    }
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
                        containerColor = AccentBlue.copy(alpha = 0.8f)
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
            .background(backgroundColor.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
            .border(0.8.dp, backgroundColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
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
        DeviceType.WINDOWS -> Triple(Icons.Default.DesktopWindows, "Windows", WindowsDeviceColor)
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
    MAC, ANDROID, WINDOWS, UNKNOWN
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
        lower.contains("windows") || lower.contains("surface") || lower.contains("dell") ||
        lower.contains("hp ") || lower.contains("hp-") || lower.contains("lenovo") || lower.contains("thinkpad") ||
        lower.contains("asus desktop") || lower.contains("acer") || lower.contains("msi") ||
        lower.contains("desktop") || lower.contains("laptop") -> DeviceType.WINDOWS
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

/**
 * Step indicator for the pairing flow: BT On → Scanning → Select → Connected
 */
@Composable
fun ConnectionStepIndicator(
    currentStep: Int, // 0=BT Off, 1=BT On/Ready, 2=Scanning, 3=Connected
    modifier: Modifier = Modifier
) {
    val steps = listOf("Enable BT", "Scan", "Select", "Connected")
    val stepIcons = listOf(
        Icons.Default.Bluetooth,
        Icons.Default.Search,
        Icons.Default.TouchApp,
        Icons.Default.CheckCircle
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Graphite.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .border(0.5.dp, BorderColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, label ->
            val isCompleted = index < currentStep
            val isCurrent = index == currentStep
            val color = when {
                isCompleted -> SuccessGreen
                isCurrent -> AccentBlue
                else -> Silver.copy(alpha = 0.3f)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            color.copy(alpha = if (isCurrent) 0.2f else if (isCompleted) 0.15f else 0.08f),
                            CircleShape
                        )
                        .then(
                            if (isCurrent) Modifier.border(1.5.dp, color, CircleShape) else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        stepIcons[index],
                        contentDescription = label,
                        tint = color,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    label,
                    color = color,
                    fontSize = 9.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }

            // Connector line between steps
            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .height(1.5.dp)
                        .width(16.dp)
                        .background(
                            if (isCompleted) SuccessGreen.copy(alpha = 0.5f) else BorderColor.copy(alpha = 0.2f),
                            RoundedCornerShape(1.dp)
                        )
                )
            }
        }
    }
}

/**
 * Premium device card for the redesigned pairing screen.
 */
@Composable
fun AnimatedDeviceCard(
    name: String,
    deviceType: DeviceType,
    subtitle: String,
    isConnecting: Boolean = false,
    isBonded: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, iconColor) = when (deviceType) {
        DeviceType.MAC -> Icons.Default.Laptop to MacDeviceColor
        DeviceType.ANDROID -> Icons.Default.PhoneAndroid to AndroidDeviceColor
        DeviceType.WINDOWS -> Icons.Default.DesktopWindows to WindowsDeviceColor
        DeviceType.UNKNOWN -> Icons.Default.Devices to UnknownDeviceColor
    }

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isConnecting) iconColor.copy(alpha = 0.06f) else Graphite.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(
            if (isConnecting) 1.dp else 0.5.dp,
            if (isConnecting) iconColor.copy(alpha = 0.4f) else BorderColor.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(iconColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            name,
                            color = Platinum,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        if (isBonded) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AccentGold.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "PAIRED",
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    color = AccentGold,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        subtitle,
                        color = if (isConnecting) iconColor else Silver,
                        fontSize = 12.sp,
                        fontWeight = if (isConnecting) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = iconColor,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Silver.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Quick-connect card for the last connected device. Shows at the top of the pairing screen
 * for instant reconnection.
 */
@Composable
fun QuickConnectCard(
    deviceName: String,
    lastConnectedTime: Long,
    isConnecting: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val deviceType = guessDeviceType(deviceName)
    val (icon, iconColor) = when (deviceType) {
        DeviceType.MAC -> Icons.Default.Laptop to MacDeviceColor
        DeviceType.ANDROID -> Icons.Default.PhoneAndroid to AndroidDeviceColor
        DeviceType.WINDOWS -> Icons.Default.DesktopWindows to WindowsDeviceColor
        DeviceType.UNKNOWN -> Icons.Default.Devices to UnknownDeviceColor
    }

    val timeAgo = remember(lastConnectedTime) {
        val diff = System.currentTimeMillis() - lastConnectedTime
        when {
            diff < 60_000 -> "just now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            else -> "${diff / 86_400_000}d ago"
        }
    }

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = iconColor.copy(alpha = 0.06f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            iconColor.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(iconColor.copy(alpha = 0.2f), iconColor.copy(alpha = 0.05f))
                        ),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    deviceName,
                    color = Platinum,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = Silver.copy(alpha = 0.5f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Last connected $timeAgo",
                        color = Silver.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }
            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = iconColor,
                    strokeWidth = 2.5.dp
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = iconColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Connect",
                            color = iconColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
