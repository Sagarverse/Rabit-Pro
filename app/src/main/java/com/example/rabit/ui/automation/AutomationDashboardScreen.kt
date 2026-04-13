package com.example.rabit.ui.automation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.domain.model.HidKeyCodes
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*

@Composable
fun AutomationDashboardScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToWakeOnLan: () -> Unit = {},
    onNavigateToSshTerminal: () -> Unit = {}
) {
    val customMacros by viewModel.customMacros.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Graphite.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Silver)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("How to use Automation", color = Platinum, fontWeight = FontWeight.Bold)
                        }
                        Text("• Tap any macro to send keyboard shortcuts directly to your connected Mac.", color = Silver, fontSize = 12.sp)
                        Text("• Requires an active Bluetooth HID connection.", color = Silver, fontSize = 12.sp)
                        Text("• Add custom commands using keys like CMD, ALT, SHIFT.", color = Silver, fontSize = 12.sp)
                    }
                }
            }

            item {
                QuickToolPanel(
                    onWakeOnLan = onNavigateToWakeOnLan,
                    onSshTerminal = onNavigateToSshTerminal
                )
            }

            // ─── SYSTEM CORE ───
            item {
                MacroCategory(
                    title = "SYSTEM CONTROL",
                    icon = Icons.Default.Terminal,
                    macros = listOf(
                        MacroDefinition("Unlock Mac", Icons.Default.LockOpen, SuccessGreen, "UNLOCK_CMD"),
                        MacroDefinition("Lock Mac", Icons.Default.Lock, AccentBlue, "LOCK_CMD"),
                        MacroDefinition("Spotlight", Icons.Default.Search, AccentPurple, "SPOT_CMD"),
                        MacroDefinition("Screen Cap", Icons.Default.Screenshot, AccentTeal, "SHOT_CMD"),
                        MacroDefinition("Mute Mic", Icons.Default.MicOff, ErrorRed, "MUTE_CMD"),
                        MacroDefinition("Sleep Mac", Icons.Default.NightsStay, Silver, "SLEEP_CMD"),
                        MacroDefinition("Sys Info", Icons.Default.Info, AccentBlue, "INFO_CMD"),
                        MacroDefinition("Force Quit", Icons.Default.Cancel, ErrorRed, "FORCE_QUIT_CMD")
                    ),
                    onMacroClick = { handleMacro(it.command, viewModel) }
                )
            }

            // ─── WEB & BROWSER ───
            item {
                MacroCategory(
                    title = "WEB & BROWSER",
                    icon = Icons.Default.Language,
                    macros = listOf(
                        MacroDefinition("New Tab", Icons.Default.Add, AccentBlue, "TAB_CMD"),
                        MacroDefinition("Reload", Icons.Default.Refresh, SuccessGreen, "RELOAD_CMD"),
                        MacroDefinition("History", Icons.Default.History, AccentOrange, "HIST_CMD"),
                        MacroDefinition("Private", Icons.Default.Shield, Silver, "PRIV_CMD"),
                        MacroDefinition("Go Back", Icons.AutoMirrored.Filled.ArrowBack, Silver, "BACK_CMD"),
                        MacroDefinition("FS Mode", Icons.Default.Fullscreen, AccentTeal, "FS_CMD")
                    ),
                    onMacroClick = { handleMacro(it.command, viewModel) }
                )
            }

            // ─── PRODUCTIVITY ───
            item {
                MacroCategory(
                    title = "PRODUCTIVITY",
                    icon = Icons.Default.AutoMode,
                    macros = listOf(
                        MacroDefinition("Mission Ctrl", Icons.Default.GridView, AccentPurple, "MC_CMD"),
                        MacroDefinition("Switch App", Icons.Default.Tab, AccentPink, "SW_CMD"),
                        MacroDefinition("Hide Others", Icons.Default.VisibilityOff, AccentOrange, "HIDE_CMD"),
                        MacroDefinition("Terminal", Icons.Default.Code, Platinum, "TERM_CMD"),
                        MacroDefinition("Open Safari", Icons.Default.Language, AccentBlue, "LAUNCH_SAFARI"),
                        MacroDefinition("Open Spotify", Icons.Default.MusicNote, SuccessGreen, "LAUNCH_SPOTIFY")
                    ),
                    onMacroClick = { handleMacro(it.command, viewModel) }
                )
            }

            // ─── CREATIVE STUDIO ───
            item {
                MacroCategory(
                    title = "CREATIVE STUDIO",
                    icon = Icons.Default.Palette,
                    macros = listOf(
                        MacroDefinition("Play/Pause", Icons.Default.PlayCircle, Platinum, "PLAY_CMD"),
                        MacroDefinition("Zoom In", Icons.Default.ZoomIn, Silver, "ZI_CMD"),
                        MacroDefinition("Zoom Out", Icons.Default.ZoomOut, Silver, "ZO_CMD"),
                        MacroDefinition("Render", Icons.Default.Movie, AccentGold, "RENDER_CMD"),
                        MacroDefinition("Export", Icons.Default.IosShare, AccentTeal, "EXPORT_CMD")
                    ),
                    onMacroClick = { handleMacro(it.command, viewModel) }
                )
            }

            // ─── USER MACROS ───
            item {
                MacroCategory(
                    title = "USER CUSTOM",
                    icon = Icons.Default.SettingsSuggest,
                    macros = customMacros.map { 
                        MacroDefinition(it.name, Icons.Default.Bolt, AccentGold, it.command) 
                    },
                    onMacroClick = { handleMacro(it.command, viewModel) },
                    onDeleteClick = { macro -> 
                        viewModel.deleteCustomMacro(com.example.rabit.ui.CustomMacro(macro.name, macro.command))
                    },
                    onAddClick = { showAddDialog = true }
                )
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var command by remember { mutableStateOf("") }
        val complexExample = remember {
            """
            KEY(CMD+SPACE)
            WAIT(300)
            TEXT(Terminal)
            KEY(ENTER)
            WAIT(700)
            TEXT(whoami)
            KEY(ENTER)
            """.trimIndent()
        }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Graphite,
            title = { Text("Define New Macro", color = Platinum) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Write complex steps with one command per line or use '&&'.", color = Silver, fontSize = 12.sp)
                    Surface(
                        color = SoftGrey.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Supported syntax", color = Platinum, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("KEY(CMD+SPACE)", color = Silver, fontSize = 11.sp)
                            Text("TEXT(hello world)", color = Silver, fontSize = 11.sp)
                            Text("WAIT(500)", color = Silver, fontSize = 11.sp)
                            Text("MEDIA(MUTE)", color = Silver, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Example: Open Terminal and run whoami", color = Platinum, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Text("KEY(CMD+SPACE)", color = AccentGold, fontSize = 11.sp)
                            Text("WAIT(300)", color = AccentGold, fontSize = 11.sp)
                            Text("TEXT(Terminal)", color = AccentGold, fontSize = 11.sp)
                            Text("KEY(ENTER)", color = AccentGold, fontSize = 11.sp)
                            Text("WAIT(700)", color = AccentGold, fontSize = 11.sp)
                            Text("TEXT(whoami)", color = AccentGold, fontSize = 11.sp)
                            Text("KEY(ENTER)", color = AccentGold, fontSize = 11.sp)
                        }
                    }
                    TextButton(
                        onClick = {
                            if (name.isBlank()) name = "Open Terminal + whoami"
                            command = complexExample
                        }
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Insert Example", color = AccentGold, fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Automation Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Platinum
                        )
                    )
                    OutlinedTextField(
                        value = command,
                        onValueChange = { command = it },
                        label = { Text("Command Sequence / Text") },
                        placeholder = { Text("KEY(CMD+SPACE)\nWAIT(300)\nTEXT(Terminal)\nKEY(ENTER)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Platinum
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && command.isNotBlank()) {
                            viewModel.addCustomMacro(name, command)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                ) { Text("Deploy", color = Obsidian, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = Silver) }
            }
        )
    }
}

@Composable
private fun QuickToolPanel(
    onWakeOnLan: () -> Unit,
    onSshTerminal: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "POWER TOOLS",
            color = Platinum.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            fontSize = 12.sp
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Surface(
                onClick = onWakeOnLan,
                color = SoftGrey.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.2f)),
                modifier = Modifier.weight(1f).height(72.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = AccentTeal)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Wake-on-LAN", color = Platinum, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Boot sleeping host", color = Silver, fontSize = 11.sp)
                    }
                }
            }

            Surface(
                onClick = onSshTerminal,
                color = SoftGrey.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.2f)),
                modifier = Modifier.weight(1f).height(72.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = AccentBlue)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("SSH Terminal", color = Platinum, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Run shell commands", color = Silver, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun MacroCategory(
    title: String,
    icon: ImageVector,
    macros: List<MacroDefinition>,
    onMacroClick: (MacroDefinition) -> Unit,
    onDeleteClick: ((MacroDefinition) -> Unit)? = null,
    onAddClick: (() -> Unit)? = null
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Platinum.copy(alpha=0.4f), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    color = Platinum.copy(alpha=0.6f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            if (onAddClick != null) {
                IconButton(onClick = onAddClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = AccentGold, modifier = Modifier.size(18.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Grid of macros
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val chunks = macros.chunked(2)
            chunks.forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    rowItems.forEach { macro ->
                        MacroGridItem(
                            macro = macro, 
                            onClick = { onMacroClick(macro) }, 
                            onDelete = if (onDeleteClick != null) { { onDeleteClick(macro) } } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowItems.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun MacroGridItem(
    macro: MacroDefinition,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = SoftGrey.copy(alpha = 0.12f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.2f)),
        modifier = modifier.height(64.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier.size(36.dp).background(macro.color.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(macro.icon, contentDescription = null, tint = macro.color, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = macro.name,
                    color = Platinum,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
            
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Delete", tint = ErrorRed.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

private fun handleMacro(command: String, viewModel: MainViewModel) {
    when (command) {
        "UNLOCK_CMD" -> viewModel.unlockMac()
        "LOCK_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_CTRL, HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_Q))
        "SPOT_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_SPACE))
        "SHOT_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.MODIFIER_LEFT_SHIFT, HidKeyCodes.KEY_4))
        "MUTE_CMD" -> viewModel.sendSystemShortcut(MainViewModel.SystemShortcut.MUTE)
        "SLEEP_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_ALT, HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_POWER))
        "INFO_CMD" -> {
             viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_SPACE))
             viewModel.sendText("About This Mac")
             viewModel.sendKey(HidKeyCodes.KEY_ENTER)
        }
        "FORCE_QUIT_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.MODIFIER_LEFT_ALT, HidKeyCodes.KEY_ESC))
        "TAB_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_T))
        "RELOAD_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_R))
        "HIST_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_Y))
        "PRIV_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.MODIFIER_LEFT_SHIFT, HidKeyCodes.KEY_N))
        "BACK_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_LEFT_BRACKET))
        "FS_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.MODIFIER_LEFT_CTRL, HidKeyCodes.KEY_F))
        "MC_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_CTRL, HidKeyCodes.KEY_UP))
        "SW_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_TAB))
        "HIDE_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.MODIFIER_LEFT_ALT, HidKeyCodes.KEY_H))
        "TERM_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_CTRL, HidKeyCodes.KEY_GRAVE))
        "PLAY_CMD" -> viewModel.sendSystemShortcut(MainViewModel.SystemShortcut.PLAY_PAUSE)
        "ZI_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_EQUAL))
        "ZO_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_MINUS))
        "RENDER_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_R))
        "EXPORT_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_E))
        "LAUNCH_SAFARI" -> viewModel.launchMacApp("Safari")
        "LAUNCH_SPOTIFY" -> viewModel.launchMacApp("Spotify")
        else -> {
            if (command.contains("&&")) {
                viewModel.sendMacro(command)
            } else {
                viewModel.sendText(command)
            }
        }
    }
}

data class MacroDefinition(val name: String, val icon: ImageVector, val color: Color, val command: String)
