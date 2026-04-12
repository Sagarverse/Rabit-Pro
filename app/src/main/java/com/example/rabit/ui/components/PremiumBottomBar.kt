package com.example.rabit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

/**
 * PremiumBottomBar - The iconic "App Dock" for Rabit Pro.
 * 
 * Provides high-speed navigation between the main control modules:
 * Keys, Trackpad, Macros, and AI Assistant.
 */
@Composable
fun PremiumBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    // Current "Tab" index mapping
    // 0: Keyboard/Pad (main)
    // 1: Web Bridge
    // 2: Macros (actually part of keyboard screen, but highlighted separately)
    // -1: AI Assistant
    
    val selectedIndex = when {
        currentRoute == "keyboard" || currentRoute == "main" -> 0
        currentRoute == "web_bridge" -> 1
        currentRoute == "assistant" -> -1
        else -> -2 // Neutral
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Graphite,
        tonalElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Main Tabs
            DockItem(
                icon = Icons.Default.Keyboard,
                label = "HUB",
                isSelected = selectedIndex == 0,
                activeColor = AccentBlue,
                onClick = { onNavigate("keyboard") }
            )

            DockItem(
                icon = Icons.Default.Cloud,
                label = "BRIDGE",
                isSelected = selectedIndex == 1,
                activeColor = AccentBlue,
                onClick = { onNavigate("web_bridge") }
            )

            // Vertical divider
            Box(
                modifier = Modifier
                    .width(0.5.dp)
                    .height(24.dp)
                    .background(BorderColor.copy(alpha = 0.4f))
            )

            // AI Assistant tab (special gold styling)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onNavigate("assistant") }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (selectedIndex == -1) AccentGold.copy(alpha = 0.12f) else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "AI Assistant",
                        tint = if (selectedIndex == -1) AccentGold else AccentGold.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "AI",
                    color = if (selectedIndex == -1) AccentGold else AccentGold.copy(alpha = 0.6f),
                    fontSize = 9.sp,
                    fontWeight = if (selectedIndex == -1) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun RowScope.DockItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    if (isSelected) activeColor.copy(alpha = 0.12f) else Color.Transparent,
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isSelected) activeColor else Silver.copy(alpha = 0.45f),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            label,
            color = if (isSelected) activeColor else Silver.copy(alpha = 0.45f),
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
