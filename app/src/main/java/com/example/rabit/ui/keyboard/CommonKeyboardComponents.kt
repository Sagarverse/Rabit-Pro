package com.example.rabit.ui.keyboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.domain.model.RemoteFile
import com.example.rabit.domain.model.Workstation
import com.example.rabit.ui.theme.*

@Composable
fun PremiumKey(label: String, modifier: Modifier, accent: Color, onPress: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isPressed) SoftGrey else KeyBackground)
            .border(
                1.dp,
                if (isPressed) accent.copy(alpha = 0.5f) else BorderColor,
                RoundedCornerShape(10.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null) { onPress() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isPressed) accent else accent.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MouseButton(modifier: Modifier, text: String, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "mouseBtn")

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    }
                )
            },
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SoftGrey.copy(alpha = 0.6f)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.3f)),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text, color = Platinum.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun WorkstationCard(
    workstation: Workstation,
    connectionState: HidDeviceManager.ConnectionState,
    onConnect: (Workstation) -> Unit
) {
    val isConnecting = connectionState is HidDeviceManager.ConnectionState.Connecting
    val isConnected = (connectionState as? HidDeviceManager.ConnectionState.Connected)?.deviceName == workstation.name

    Surface(
        modifier = Modifier
            .width(160.dp)
            .clickable(!isConnecting && !isConnected) { onConnect(workstation) },
        color = if (isConnected) AccentBlue.copy(alpha = 0.1f) else SoftGrey.copy(alpha = 0.4f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isConnected) AccentBlue.copy(alpha = 0.5f) else BorderColor.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(if (isConnected) AccentBlue.copy(alpha = 0.2f) else Silver.copy(alpha = 0.05f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (workstation.name.contains("Mac", true)) Icons.Default.LaptopMac else Icons.Default.Computer,
                        contentDescription = null,
                        tint = if (isConnected) AccentBlue else Silver.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (isConnected) {
                    Box(modifier = Modifier.size(8.dp).background(SuccessGreen, CircleShape))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                workstation.name,
                color = if (isConnected) AccentBlue else Platinum,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Text(
                workstation.address.take(12) + "...",
                color = Silver.copy(alpha = 0.4f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun RemoteFileCard(file: RemoteFile, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val icon = if (file.isFolder) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile
        val tint = if (file.isFolder) AccentBlue else Platinum.copy(alpha = 0.6f)
        
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(tint.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            file.name,
            color = Platinum,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SmallActionCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        color = SoftGrey.copy(alpha = 0.5f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(28.dp).background(accent.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, color = Platinum, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}
