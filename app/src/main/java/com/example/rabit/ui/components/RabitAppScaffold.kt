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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sagar.rabit.R
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
    featureWebBridgeVisible: Boolean = true,
    featureAutomationVisible: Boolean = true,
    featureAssistantVisible: Boolean = true,
    featureWakeOnLanVisible: Boolean = true,
    featureSshTerminalVisible: Boolean = true,
    activeApp: String? = null,
    onBack: (() -> Unit)? = null,
    topBarActions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isMono = AppThemeMode.isMonochrome

    // Main routes accessible from drawer
    val mainRoutes = listOf("main", "keyboard", "web_bridge", "assistant", "settings", "wake_on_lan", "ssh_terminal", "media_deck", "airplay_receiver")
    val isSubPage = currentRoute !in mainRoutes

    val screenTitle = when(currentRoute) {
        "main", "keyboard" -> "CONTROL HUB"
        "web_bridge" -> "WEB BRIDGE"
        "assistant" -> "GENIE AI"
        "settings" -> "SETTINGS"
        "profile" -> "PROFILE"
        "customization" -> "THEME"
        "snippets" -> "SNIPPETS"
        "shortcuts" -> "GUIDE"
        "wake_on_lan" -> "WAKE ON LAN"
        "ssh_terminal" -> "SSH TERMINAL"
        "media_deck" -> "MEDIA DECK"
        "airplay_receiver" -> "AIRPLAY RX"
        else -> "RABIT PRO"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Obsidian,
                drawerContentColor = Platinum,
                drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp),
                modifier = Modifier.width(320.dp).fillMaxHeight().background(Obsidian)
            ) {
                Spacer(modifier = Modifier.height(64.dp))
                
                // Navigation Items
                DrawerItem(
                    label = "Control Hub",
                    subLabel = "Keyboard & Trackpad",
                    icon = Icons.AutoMirrored.Filled.Dvr,
                    isSelected = currentRoute == "main" || currentRoute == "keyboard",
                    onClick = { 
                        onNavigate("keyboard")
                        scope.launch { drawerState.close() }
                    }
                )

                if (featureWebBridgeVisible) {
                    DrawerItem(
                        label = "Web Bridge Hub",
                        subLabel = "File Sharing & Sync",
                        icon = Icons.Default.CloudSync,
                        isSelected = currentRoute == "web_bridge",
                        onClick = {
                            onNavigate("web_bridge")
                            scope.launch { drawerState.close() }
                        }
                    )
                }

                if (featureAutomationVisible) {
                    DrawerItem(
                        label = "Automation Hub",
                        subLabel = "Macros & Quick Actions",
                        icon = Icons.Default.Bolt,
                        isSelected = currentRoute == "automation",
                        onClick = {
                            onNavigate("automation")
                            scope.launch { drawerState.close() }
                        }
                    )
                }

                DrawerItem(
                    label = "Media Control Deck",
                    subLabel = "Now Playing & Transport",
                    icon = Icons.Default.MusicNote,
                    isSelected = currentRoute == "media_deck",
                    onClick = {
                        onNavigate("media_deck")
                        scope.launch { drawerState.close() }
                    }
                )

                DrawerItem(
                    label = "AirPlay Receiver",
                    subLabel = "Wi-Fi audio target (experimental)",
                    icon = Icons.Default.Speaker,
                    isSelected = currentRoute == "airplay_receiver",
                    onClick = {
                        onNavigate("airplay_receiver")
                        scope.launch { drawerState.close() }
                    }
                )

                if (featureWakeOnLanVisible) {
                    DrawerItem(
                        label = "Wake-on-LAN",
                        subLabel = "Boot Sleeping Mac/PC",
                        icon = Icons.Default.PowerSettingsNew,
                        isSelected = currentRoute == "wake_on_lan",
                        onClick = {
                            onNavigate("wake_on_lan")
                            scope.launch { drawerState.close() }
                        }
                    )
                }

                if (featureSshTerminalVisible) {
                    DrawerItem(
                        label = "SSH Terminal",
                        subLabel = "Native secure shell",
                        icon = Icons.Default.Terminal,
                        isSelected = currentRoute == "ssh_terminal",
                        onClick = {
                            onNavigate("ssh_terminal")
                            scope.launch { drawerState.close() }
                        }
                    )
                }

                if (featureAssistantVisible) {
                    DrawerItem(
                        label = "AI Assistant",
                        subLabel = "Smart Control & Logic",
                        icon = Icons.Default.AutoAwesome,
                        isSelected = currentRoute == "assistant",
                        onClick = {
                            onNavigate("assistant")
                            scope.launch { drawerState.close() }
                        }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom Footer Items
                DrawerItem(
                    label = "System Settings",
                    subLabel = "Configuration & Sensitivity",
                    icon = Icons.Default.Settings,
                    isSelected = currentRoute == "settings",
                    onClick = { 
                        onNavigate("settings")
                        scope.launch { drawerState.close() }
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    ) {
        Scaffold(
            containerColor = Obsidian,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = screenTitle,
                                color = Platinum,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "PRO v2.5",
                                    color = AccentBlue.copy(alpha = 0.6f),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                if (activeApp != null) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        color = Silver.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = activeApp.uppercase(),
                                            color = SuccessGreen,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        if (isSubPage && onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back", tint = Platinum)
                            }
                        } else {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Open navigation menu", tint = Platinum)
                            }
                        }
                    },
                    actions = {
                        topBarActions()
                        IconButton(onClick = { AppThemeMode.isMonochrome = !AppThemeMode.isMonochrome }) {
                            Icon(
                                if(isMono) Icons.Default.InvertColorsOff else Icons.Default.InvertColors,
                                contentDescription = if (isMono) "Disable monochrome theme" else "Enable monochrome theme",
                                tint = Platinum.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Obsidian,
                        titleContentColor = Platinum
                    )
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
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isMono = AppThemeMode.isMonochrome
    // If mono is on, use Platinum for selection. Otherwise AccentBlue.
    // BUT if the system background ever leaks to white, we'd need dark. 
    // Since we force Obsidian, Platinum/AccentBlue is always safe on Obsidian.
    val tint = if (isSelected) (if(isMono) Platinum else AccentBlue) else Silver.copy(alpha = 0.6f)
    
    Surface(
        onClick = onClick,
        color = if (isSelected) (if(isMono) Platinum.copy(alpha = 0.05f) else AccentBlue.copy(alpha = 0.05f)) else Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "$label. $subLabel"
                selected = isSelected
                stateDescription = if (isSelected) "Selected" else "Not selected"
            }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(label, color = if(isSelected) Platinum else Silver, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(subLabel, color = Silver.copy(alpha = 0.5f), fontSize = 11.sp)
            }
        }
    }
}
