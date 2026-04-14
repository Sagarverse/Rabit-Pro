package com.example.rabit.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.data.voice.VoiceState
import com.example.rabit.domain.model.HidKeyCodes
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.assistant.PulsingVoiceButton
import com.example.rabit.ui.theme.*

/**
 * KeyboardScreen - The professional "Infrastructure Hub" remote interface.
 * 
 * Manages high-level navigation between input modules (Keyboard, Trackpad, Macros, Hub).
 * Features lifecycle-aware state management for background sensors (Air Mouse).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardScreen(
    viewModel: MainViewModel,
    onDisconnect: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAssistant: () -> Unit,
    onNavigateToSnippets: () -> Unit = {},
    onNavigateToAutomation: () -> Unit = {},
    onNavigateToWebBridge: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val connectionState by viewModel.connectionState.collectAsState()
    var wasConnected by remember { mutableStateOf(false) }

    LaunchedEffect(connectionState) {
        when (connectionState) {
            is HidDeviceManager.ConnectionState.Connected -> {
                wasConnected = true
            }
            is HidDeviceManager.ConnectionState.Disconnected -> {
                if (wasConnected) {
                    wasConnected = false
                    onDisconnect()
                }
            }
            else -> Unit
        }
    }

    // Scoping: Disable Pad operations (Air Mouse) when leaving the "PAD" tab or screen
    LaunchedEffect(selectedTab) {
        if (selectedTab != 1) {
            viewModel.setAirMouseEnabled(false)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.setAirMouseEnabled(false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // High-Precision Header
        PremiumHeader(
            connectionState = connectionState,
            onNavigateToSettings = onNavigateToSettings,
            onDisconnect = onDisconnect
        )

        // Module Switcher (TabRow alternative for extreme responsiveness)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .background(SoftGrey.copy(alpha=0.3f), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf(
                "INPUT" to Icons.Default.Keyboard,
                "PAD" to Icons.Default.TouchApp,
                "HUB" to Icons.Default.FolderOpen
            )
            
            tabs.forEachIndexed { index, (label, icon) ->
                val active = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) Graphite else Color.Transparent)
                        .clickable { selectedTab = index },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = if (active) AccentBlue else Silver.copy(alpha=0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            label,
                            color = if (active) Platinum else Silver.copy(alpha=0.5f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Dynamic Module Container
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> DualKeyboardTab(viewModel)
                1 -> TrackpadSection(viewModel)
                2 -> FileHubSection(viewModel, onNavigateToSnippets, onNavigateToAutomation, onNavigateToWebBridge)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun PremiumHeader(
    connectionState: HidDeviceManager.ConnectionState,
    onNavigateToSettings: () -> Unit,
    onDisconnect: () -> Unit
) {
    val deviceName = (connectionState as? HidDeviceManager.ConnectionState.Connected)?.deviceName ?: "OFFLINE"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "CONTROL DECK",
                color = Platinum,
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "Hi pookie, I am Hackie. Let's cause productive chaos.",
                color = AccentBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            if (deviceName != "OFFLINE") SuccessGreen else ErrorRed,
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    deviceName.uppercase(),
                    color = Silver,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .size(40.dp)
                    .background(SoftGrey, CircleShape)
                    .border(1.dp, BorderColor, CircleShape)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Platinum, modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = onDisconnect,
                modifier = Modifier
                    .size(40.dp)
                    .background(SoftGrey, CircleShape)
                    .border(1.dp, BorderColor, CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Disconnect", tint = Platinum, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun DualKeyboardTab(viewModel: MainViewModel) {
    var isSystemMode by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(SoftGrey, RoundedCornerShape(20.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                .padding(4.dp)
        ) {
            listOf("CUSTOM" to false, "SYSTEM" to true).forEach { (label, system) ->
                val active = isSystemMode == system
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (active) Graphite else Color.Transparent)
                        .clickable { isSystemMode = system },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (active) AccentBlue else Silver,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isSystemMode) {
            MinimalSystemInput(viewModel)
        } else {
            val activeModifiers by viewModel.activeModifiers.collectAsState()

            PremiumKeyboardLayout(
                activeModifiers = activeModifiers,
                onModifierClick = { mod -> viewModel.toggleModifier(mod) },
                onKeyPress = { code -> viewModel.sendKey(code) }
            )
        }
    }
}

@Composable
fun MinimalSystemInput(viewModel: MainViewModel) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var batchText by remember { mutableStateOf("") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                val oldText = textFieldValue.text
                val newText = newValue.text

                if (newText.length < oldText.length) {
                    repeat(oldText.length - newText.length) {
                        viewModel.sendKey(HidKeyCodes.KEY_BACKSPACE)
                    }
                } else if (newText.length > oldText.length) {
                    val diff = newText.substring(oldText.length)
                    viewModel.sendText(diff)
                }

                textFieldValue = newValue
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            placeholder = { Text("Native Keyboard Input...", color = Silver.copy(alpha = 0.3f)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = BorderColor,
                focusedTextColor = Platinum,
                cursorColor = AccentBlue
            ),
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            trailingIcon = {
                val voiceState by viewModel.voiceState.collectAsState()
                val voiceResult by viewModel.voiceResult.collectAsState()
                
                LaunchedEffect(voiceResult, voiceState) {
                    if (voiceResult.isNotBlank() && voiceState == VoiceState.SUCCESS) {
                        val updatedText = textFieldValue.text + (if (textFieldValue.text.isNotEmpty()) " " else "") + voiceResult
                        textFieldValue = TextFieldValue(updatedText)
                        viewModel.resetVoiceState()
                    }
                }

                PulsingVoiceButton(
                    state = voiceState,
                    onClick = {
                        if (voiceState == VoiceState.LISTENING) {
                            viewModel.stopVoiceRecognition()
                        } else {
                            viewModel.startVoiceRecognition()
                        }
                    }
                )
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text("BATCH SENDER", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = batchText,
            onValueChange = { batchText = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
            placeholder = { Text("Type long text and hit SEND", color = Silver.copy(alpha = 0.3f)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SuccessGreen,
                unfocusedBorderColor = BorderColor,
                focusedTextColor = Platinum,
                cursorColor = SuccessGreen
            ),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { viewModel.sendText(batchText); batchText = "" }) {
                Text("SEND TO MAC", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = { textFieldValue = TextFieldValue(""); batchText = "" }) {
                Text("RESET", color = Silver, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PremiumKeyboardLayout(
    activeModifiers: Byte,
    onModifierClick: (Byte) -> Unit,
    onKeyPress: (Byte) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val rows = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
        listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
        listOf("Z", "X", "C", "V", "B", "N", "M", "Bksp"),
        listOf("Ctrl", "Opt", "Cmd", "Space", "Enter")
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { label ->
                    val code = when(label) {
                        "Bksp" -> HidKeyCodes.KEY_BACKSPACE
                        "Space" -> HidKeyCodes.KEY_SPACE
                        "Enter" -> HidKeyCodes.KEY_ENTER
                        "Ctrl" -> HidKeyCodes.MODIFIER_LEFT_CTRL
                        "Opt" -> HidKeyCodes.MODIFIER_LEFT_ALT
                        "Cmd" -> HidKeyCodes.MODIFIER_LEFT_GUI
                        else -> {
                            val char = label[0].lowercaseChar()
                            HidKeyCodes.getHidCode(char).keyCode
                        }
                    }
                    val isMod = label in listOf("Ctrl", "Opt", "Cmd")
                    val isSelected = isMod && ((activeModifiers.toInt() and code.toInt()) != 0)

                    PremiumKey(
                        label = label,
                        modifier = Modifier.weight(if (label == "Space") 2.5f else if (label.length > 1) 1.3f else 1f),
                        accent = if (isSelected) SuccessGreen else if (isMod) AccentBlue else Platinum,
                        onPress = {
                            if (isMod) onModifierClick(code) else onKeyPress(code)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    )
                }
            }
        }
    }
}
