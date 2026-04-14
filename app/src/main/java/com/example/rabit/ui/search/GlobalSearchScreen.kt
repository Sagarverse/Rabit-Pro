package com.example.rabit.ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.AccentBlue
import com.example.rabit.ui.theme.AccentGold
import com.example.rabit.ui.theme.BorderColor
import com.example.rabit.ui.theme.Graphite
import com.example.rabit.ui.theme.Obsidian
import com.example.rabit.ui.theme.Platinum
import com.example.rabit.ui.theme.Silver

private data class SearchCommand(
    val id: String,
    val title: String,
    val route: String,
    val keywords: List<String>
)

private data class SearchFeature(
    val route: String,
    val title: String,
    val keywords: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    currentRoute: String,
    availableRoutes: Set<String>,
    availableActionIds: Set<String>,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onExecuteAction: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var quickRunEnabled by remember { mutableStateOf(true) }

    val commands = remember {
        listOf(
            SearchCommand("action_unlock_mac", "Unlock Mac", "automation", listOf("unlock", "mac", "login", "password")),
            SearchCommand("action_lock_screen", "Lock Screen", "automation", listOf("lock", "screen", "security")),
            SearchCommand("action_media_play_pause", "Play / Pause", "media_deck", listOf("media", "music", "play", "pause")),
            SearchCommand("action_media_vol_up", "Volume Up", "media_deck", listOf("media", "volume", "up", "sound")),
            SearchCommand("action_media_vol_down", "Volume Down", "media_deck", listOf("media", "volume", "down", "sound")),
            SearchCommand("action_now_playing", "Refresh Now Playing", "media_deck", listOf("now", "playing", "song", "metadata")),
            SearchCommand("action_wol_send", "Wake Mac", "wake_on_lan", listOf("wake", "wol", "mac", "power")),
            SearchCommand("action_disconnect_keyboard", "Disconnect Keyboard", "keyboard", listOf("disconnect", "keyboard", "hid")),
            SearchCommand("action_web_bridge_toggle", "Toggle Web Bridge", "web_bridge", listOf("web", "bridge", "portal", "toggle"))
        )
    }

    val features = remember {
        listOf(
            SearchFeature("keyboard", "Control Hub", listOf("keyboard", "trackpad", "mouse")),
            SearchFeature("automation", "Automation", listOf("automation", "macro", "shortcut")),
            SearchFeature("injector", "Payload Injector", listOf("injector", "payload", "ducky")),
            SearchFeature("password_manager", "Password Manager", listOf("password", "vault", "biometric")),
            SearchFeature("wake_on_lan", "Wake-on-LAN", listOf("wake", "wol", "magic")),
            SearchFeature("ssh_terminal", "SSH Terminal", listOf("ssh", "terminal", "shell")),
            SearchFeature("media_deck", "Media Deck", listOf("media", "music", "volume")),
            SearchFeature("web_bridge", "Web Bridge", listOf("web", "bridge", "file", "transfer")),
            SearchFeature("assistant", "AI Assistant", listOf("assistant", "ai", "chat")),
            SearchFeature("snippets", "Snippets", listOf("snippet", "text", "template")),
            SearchFeature("settings", "Settings", listOf("settings", "config")),
            SearchFeature("customization", "Customization", listOf("customization", "theme"))
        )
    }

    val q = query.trim().lowercase()
    val matchedCommands = remember(q, commands, availableActionIds) {
        commands.filter { command ->
            availableActionIds.contains(command.id) &&
                (q.isBlank() || command.title.lowercase().contains(q) || command.keywords.any { it.contains(q) })
        }
    }

    val matchedFeatures = remember(q, features, matchedCommands, availableRoutes) {
        val commandRoutes = matchedCommands.map { it.route }.toSet()
        features.filter { feature ->
            availableRoutes.contains(feature.route) &&
                !commandRoutes.contains(feature.route) &&
                (q.isBlank() || feature.title.lowercase().contains(q) || feature.keywords.any { it.contains(q) })
        }
    }

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            TopAppBar(
                title = { Text("Search", color = Platinum) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Platinum)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Obsidian)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text("Search command or feature") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = Platinum,
                    unfocusedTextColor = Platinum
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Quick Run", color = Platinum, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (quickRunEnabled) "ON" else "OFF", color = Silver, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = quickRunEnabled,
                        onCheckedChange = { quickRunEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentBlue,
                            checkedTrackColor = AccentBlue.copy(alpha = 0.4f)
                        )
                    )
                }
            }

            Text(
                text = if (quickRunEnabled) {
                    "Tap a command to execute instantly. Use Open to jump to its page."
                } else {
                    "Tap a result to open its feature page."
                },
                color = Silver,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (matchedCommands.isNotEmpty()) {
                    item("commands_header") {
                        Text("Commands", color = Silver, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    items(matchedCommands, key = { it.id }) { command ->
                        CommandRow(
                            command = command,
                            quickRunEnabled = quickRunEnabled,
                            onRun = { onExecuteAction(command.id) },
                            onOpen = {
                                if (availableRoutes.contains(command.route)) {
                                    onNavigate(command.route)
                                }
                            }
                        )
                    }
                }

                if (matchedFeatures.isNotEmpty()) {
                    item("features_header") {
                        Text("Features", color = Silver, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    items(matchedFeatures, key = { it.route }) { feature ->
                        FeatureRow(
                            feature = feature,
                            currentRoute = currentRoute,
                            onOpen = { onNavigate(feature.route) }
                        )
                    }
                }

                if (matchedCommands.isEmpty() && matchedFeatures.isEmpty()) {
                    item("empty") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = Graphite.copy(alpha = 0.45f),
                            border = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                "No results for '$query'",
                                color = Silver,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandRow(
    command: SearchCommand,
    quickRunEnabled: Boolean,
    onRun: () -> Unit,
    onOpen: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (quickRunEnabled) onRun() else onOpen() },
        shape = RoundedCornerShape(14.dp),
        color = AccentGold.copy(alpha = 0.1f),
        border = BorderStroke(0.5.dp, AccentGold.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Bolt, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(command.title, color = Platinum, fontWeight = FontWeight.SemiBold)
                Text("Action", color = Silver, fontSize = 11.sp)
            }
            TextButton(
                onClick = onOpen,
                colors = ButtonDefaults.textButtonColors(contentColor = AccentBlue)
            ) {
                Text("Open")
            }
        }
    }
}

@Composable
private fun FeatureRow(
    feature: SearchFeature,
    currentRoute: String,
    onOpen: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(14.dp),
        color = if (currentRoute == feature.route) AccentBlue.copy(alpha = 0.16f) else Graphite.copy(alpha = 0.4f),
        border = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(feature.title, color = Platinum, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Silver)
        }
    }
}
