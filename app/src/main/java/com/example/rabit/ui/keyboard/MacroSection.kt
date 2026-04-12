package com.example.rabit.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*
import com.example.rabit.ui.CustomMacro
import com.example.rabit.domain.model.HidKeyCodes

/**
 * MacroSection - The professional "Macro Dashboard".
 * 
 * Provides quick access to system commands and user-defined automation strings.
 */
@Composable
fun MacroSection(
    viewModel: MainViewModel,
    showSystemOnly: Boolean = true
) {
    val customMacros by viewModel.customMacros.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    // Built-in system macros — the core infrastructure toolkit
    val builtInMacros = remember {
        listOf(
            MacroItem("Lock Workstation", Icons.Default.Lock, "LOCK_CMD", AccentBlue),
            MacroItem("Spotlight Search", Icons.Default.Search, "SPOT_CMD", AccentPurple),
            MacroItem("System Shot", Icons.Default.CameraAlt, "SHOT_CMD", AccentTeal),
            MacroItem("Toggle Audio", Icons.Default.MicOff, "MUTE_CMD", ErrorRed),
            MacroItem("Sleep Mac", Icons.Default.NightsStay, "SLEEP_CMD", Silver),
            MacroItem("System Info", Icons.Default.Info, "INFO_CMD", AccentBlue),
            MacroItem("Force Quit", Icons.Default.Cancel, "FORCE_QUIT_CMD", ErrorRed),
            MacroItem("Search Google", Icons.Default.Language, "BROWSER_CMD", AccentGold),
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (showSystemOnly) "SYSTEM COMMANDS" else "USER AUTOMATIONS", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            if (!showSystemOnly) {
                IconButton(onClick = { showAddDialog = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Add Macro", tint = AccentGold)
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (showSystemOnly) {
                items(builtInMacros) { macro ->
                    MacroCard(
                        macro = macro,
                        showDelete = false,
                        onClick = { handleMacroExecution(viewModel, macro.command) }
                    )
                }
            } else {
                if (customMacros.isEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Gesture, null, tint = Silver.copy(alpha = 0.2f), modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(16.dp))
                                Text("No custom macros yet", color = Silver.copy(alpha = 0.4f), fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    items(customMacros.map { MacroItem(it.name, Icons.Default.Terminal, it.command, AccentGold) }) { macro ->
                        MacroCard(
                            macro = macro,
                            showDelete = true,
                            onDelete = { viewModel.deleteCustomMacro(CustomMacro(macro.name, macro.command)) },
                            onClick = { handleMacroExecution(viewModel, macro.command) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var command by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Graphite,
            title = { Text("Define New Macro", color = Platinum) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Chain multiple commands using '&&'.", color = Silver, fontSize = 12.sp)
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
                        placeholder = { Text("e.g. brew update && brew upgrade") },
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

private fun handleMacroExecution(viewModel: MainViewModel, command: String) {
    when(command) {
        "LOCK_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_CTRL, HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_Q))
        "SPOT_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_SPACE))
        "SHOT_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.MODIFIER_LEFT_SHIFT, HidKeyCodes.KEY_4))
        "MUTE_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.MODIFIER_LEFT_SHIFT, HidKeyCodes.KEY_A))
        "SLEEP_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_ALT, HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_POWER)) // Mac Sleep: Cmd+Opt+Power
        "INFO_CMD" -> {
             viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_SPACE))
             viewModel.sendText("About This Mac")
             viewModel.sendKey(HidKeyCodes.KEY_ENTER)
        }
        "FORCE_QUIT_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.MODIFIER_LEFT_ALT, HidKeyCodes.KEY_ESC))
        "BROWSER_CMD" -> {
             viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_SPACE))
             viewModel.sendText("https://google.com")
             viewModel.sendKey(HidKeyCodes.KEY_ENTER)
        }
        else -> {
            if (command.contains("&&")) {
                viewModel.sendMacro(command)
            } else {
                viewModel.sendText(command)
                viewModel.sendKey(HidKeyCodes.KEY_ENTER)
            }
        }
    }
}

data class MacroItem(val name: String, val icon: ImageVector, val command: String, val color: Color)

@Composable
fun MacroCard(
    macro: MacroItem,
    showDelete: Boolean = false,
    modifier: Modifier = Modifier,
    onDelete: () -> Unit = {},
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, label = "macroCardScale")

    Surface(
        color = SoftGrey.copy(alpha = 0.5f),
        shape = RoundedCornerShape(26.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.2f)),
        modifier = modifier
            .height(120.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                        onClick()
                    }
                )
            }
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(macro.color.copy(alpha = 0.15f), CircleShape)
                    .align(Alignment.TopStart),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    macro.icon,
                    contentDescription = null,
                    tint = macro.color,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (showDelete) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .background(ErrorRed.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = ErrorRed,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Text(
                    macro.name,
                    color = Platinum,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}
