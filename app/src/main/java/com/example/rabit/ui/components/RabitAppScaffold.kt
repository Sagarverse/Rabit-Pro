package com.example.rabit.ui.components

import kotlinx.coroutines.launch

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.R
import com.example.rabit.ui.theme.*

/**
 * RabitAppScaffold — The professional global container for the application.
 * Provides a Slide-out Modal Drawer for unified navigation across all features.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RabitAppScaffold(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isMono = AppThemeMode.isMonochrome

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Obsidian,
                drawerContentColor = Platinum,
                drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp),
                modifier = Modifier.width(320.dp).fillMaxHeight()
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                
                // Drawer Header
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.rabit_logo),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Rabit Pro", color = Platinum, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("Infrastructure v2.0", color = if(isMono) Silver else AccentBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = BorderColor.copy(alpha = 0.2f))
                
                Spacer(modifier = Modifier.height(24.dp))

                // Navigation Items
                DrawerItem(
                    label = "Control Center",
                    subLabel = "Keyboard & Trackpad",
                    icon = Icons.Default.Dvr,
                    selected = currentRoute == "main",
                    onClick = { 
                        onNavigate("main")
                        scope.launch { drawerState.close() }
                    }
                )

                DrawerItem(
                    label = "Web Bridge Hub",
                    subLabel = "File Sharing & Sync",
                    icon = Icons.Default.CloudSync,
                    selected = currentRoute == "web_bridge",
                    onClick = { 
                        onNavigate("web_bridge")
                        scope.launch { drawerState.close() }
                    }
                )

                DrawerItem(
                    label = "Macro Genie",
                    subLabel = "AI Intelligent Macros",
                    icon = Icons.Default.AutoAwesome,
                    selected = currentRoute == "assistant",
                    onClick = { 
                        onNavigate("assistant")
                        scope.launch { drawerState.close() }
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Bottom Footer Items
                DrawerItem(
                    label = "System Settings",
                    subLabel = "Configuration & Sensitivity",
                    icon = Icons.Default.Settings,
                    selected = currentRoute == "settings",
                    onClick = { 
                        onNavigate("settings")
                        scope.launch { drawerState.close() }
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    "© 2026 Rabit Pro • Secured Hub",
                    modifier = Modifier.padding(24.dp),
                    color = Silver.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) {
        Scaffold(
            containerColor = Obsidian,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                when(currentRoute) {
                                    "main" -> Icons.Default.Dvr
                                    "web_bridge" -> Icons.Default.CloudSync
                                    "assistant" -> Icons.Default.AutoAwesome
                                    "settings" -> Icons.Default.Settings
                                    else -> Icons.Default.Hub
                                },
                                contentDescription = null,
                                tint = if(isMono) Platinum else AccentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                when(currentRoute) {
                                    "main" -> "CONTROL HUB"
                                    "web_bridge" -> "FILE BRIDGE"
                                    "assistant" -> "GENIE AI"
                                    "settings" -> "SYSTEM CONFIG"
                                    else -> "RABIT HUB"
                                },
                                color = Platinum,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Sidebar", tint = Platinum)
                        }
                    },
                    actions = {
                        IconButton(onClick = { AppThemeMode.isMonochrome = !AppThemeMode.isMonochrome }) {
                            Icon(
                                if(isMono) Icons.Default.InvertColorsOff else Icons.Default.InvertColors,
                                contentDescription = "Toggle B&W Mode",
                                tint = if(isMono) Silver else AccentBlue
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Obsidian)
                )
            }
        ) { padding ->
            content(padding)
        }
    }
}

@Composable
fun DrawerItem(
    label: String,
    subLabel: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val isMono = AppThemeMode.isMonochrome
    val tint = if (selected) (if(isMono) Platinum else AccentBlue) else Silver.copy(alpha = 0.6f)
    
    Surface(
        onClick = onClick,
        color = if (selected) (if(isMono) Platinum.copy(alpha = 0.05f) else AccentBlue.copy(alpha = 0.05f)) else Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp).fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(label, color = if(selected) Platinum else Silver, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(subLabel, color = Silver.copy(alpha = 0.5f), fontSize = 11.sp)
            }
        }
    }
}
