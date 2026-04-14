package com.example.rabit.ui.components

import kotlinx.coroutines.launch

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
    featureSnippetsVisible: Boolean = true,
    featureShortcutsVisible: Boolean = true,
    featureWakeOnLanVisible: Boolean = true,
    featureSshTerminalVisible: Boolean = true,
    activeApp: String? = null,
    onBack: (() -> Unit)? = null,
    topBarActions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val drawerScrollState = rememberScrollState()
    var showDrawerScrollHint by remember { mutableStateOf(true) }

    LaunchedEffect(drawerScrollState.value) {
        if (drawerScrollState.value > 0) {
            showDrawerScrollHint = false
        }
    }

    // Main routes accessible from drawer
    val mainRoutes = listOf("main", "keyboard", "web_bridge", "assistant", "settings", "wake_on_lan", "ssh_terminal", "media_deck", "airplay_receiver", "global_search", "automation", "password_manager")
    val isSubPage = currentRoute !in mainRoutes

    val screenTitle = when(currentRoute) {
        "main", "keyboard" -> "CONTROL HUB"
        "web_bridge" -> "WEB BRIDGE"
        "assistant" -> "GENIE AI"
        "settings" -> "SETTINGS"
        "profile" -> "PROFILE"
        "customization" -> "THEME"
        "password_manager" -> "PASSWORD MANAGER"
        "snippets" -> "SNIPPETS"
        "shortcuts" -> "AUTOMATION"
        "wake_on_lan" -> "WAKE ON LAN"
        "ssh_terminal" -> "SSH TERMINAL"
        "media_deck" -> "MEDIA DECK"
        "airplay_receiver" -> "AIRPLAY RX"
        "global_search" -> "GLOBAL SEARCH"
        else -> "HACKIE"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Obsidian,
                drawerContentColor = Platinum,
                drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp),
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
                    .background(AppAtmosphereGradient)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.height(28.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = AccentBlue.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                "HACKIE",
                                color = AccentBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Navigation",
                            color = Silver,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(drawerScrollState)
                        ) {
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

                    DrawerItem(
                        label = "Payload Injector",
                        subLabel = "DuckyScript command injection",
                        icon = Icons.Default.ElectricBolt,
                        isSelected = currentRoute == "injector",
                        onClick = {
                            onNavigate("injector")
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

                if (featureSnippetsVisible) {
                    DrawerItem(
                        label = "Snippets",
                        subLabel = "Saved reusable text blocks",
                        icon = Icons.Default.ContentPaste,
                        isSelected = currentRoute == "snippets",
                        onClick = {
                            onNavigate("snippets")
                            scope.launch { drawerState.close() }
                        }
                    )
                }

                            Spacer(modifier = Modifier.height(12.dp))

                DrawerItem(
                    label = "Password Manager",
                    subLabel = "Biometric + password push settings",
                    icon = Icons.Default.Password,
                    isSelected = currentRoute == "password_manager",
                    onClick = {
                        onNavigate("password_manager")
                        scope.launch { drawerState.close() }
                    }
                )

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

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Obsidian.copy(alpha = 0.8f), Color.Transparent)
                                )
                            )
                    )

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(14.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Obsidian.copy(alpha = 0.8f))
                                    )
                                )
                        )

                        if (showDrawerScrollHint && drawerScrollState.maxValue > 0) {
                            Text(
                                "Scroll for more",
                                color = Silver.copy(alpha = 0.55f),
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = screenTitle,
                            color = Platinum,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
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
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Graphite.copy(alpha = 0.95f),
                        titleContentColor = Platinum
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppAtmosphereGradient)
            ) {
                content(padding)
            }
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
    val tint = if (isSelected) Platinum else Silver.copy(alpha = 0.6f)

    Surface(
        onClick = onClick,
        color = if (isSelected) AccentBlue.copy(alpha = 0.14f) else Color.Transparent,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "$label. $subLabel"
                selected = isSelected
                stateDescription = if (isSelected) "Selected" else "Not selected"
            }
            .border(
                width = 0.8.dp,
                color = if (isSelected) AccentBlue.copy(alpha = 0.45f) else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(label, color = if (isSelected) Platinum else Silver, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(subLabel, color = Silver.copy(alpha = 0.58f), fontSize = 10.sp)
            }
        }
    }
}
