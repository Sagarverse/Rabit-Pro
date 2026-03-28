package com.example.rabit.ui.keyboard

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.example.rabit.ui.components.DarkSkeuoCard
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.domain.model.HidKeyCodes
import com.example.rabit.ui.CustomMacro
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*
import com.example.rabit.ui.assistant.SpeechToTextButton
import com.example.rabit.ui.components.PremiumGlassCard
import com.example.rabit.ui.components.PushControlBar
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun KeyboardScreen(
    viewModel: MainViewModel, 
    onDisconnect: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAssistant: () -> Unit,
    onNavigateToSnippets: () -> Unit = {},
    onNavigateToShortcuts: () -> Unit = {}
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val isPushPaused by viewModel.isPushPaused.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    LaunchedEffect(connectionState) {
        if (connectionState is HidDeviceManager.ConnectionState.Disconnected) {
            onDisconnect()
        }
    }

    Scaffold(
        bottomBar = {
            Column {
                // Push Control Bar — appears when text is being typed
                AnimatedVisibility(
                    visible = isPushPaused || viewModel.isTextPushing.collectAsState().value,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    PushControlBar(
                        isPaused = isPushPaused,
                        onPause = { viewModel.pauseTextPush() },
                        onResume = { viewModel.resumeTextPush() },
                        onStop = { viewModel.stopTextPush() }
                    )
                }
                PremiumBottomBar(pagerState.currentPage, onNavigateToAssistant) { index ->
                    scope.launch { pagerState.animateScrollToPage(index) }
                }
            }
        },
        containerColor = Obsidian
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            PremiumHeader(connectionState, onNavigateToSettings, viewModel::disconnect)

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = true,
                contentPadding = PaddingValues(bottom = 8.dp)
            ) { page ->
                when (page) {
                    0 -> DualKeyboardTab(viewModel)
                    1 -> TrackpadTab(viewModel)
                    2 -> MacroDashboardTab(viewModel)
                    3 -> AdvancedTab(viewModel, onNavigateToSettings, onNavigateToSnippets, onNavigateToShortcuts)
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// TRACKPAD TAB
// ────────────────────────────────────────────────────────────────────────────────

@Composable
fun TrackpadTab(viewModel: MainViewModel) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxSize()) {
        Text("TRACKPAD", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.weight(1f)) {
            // Main trackpad surface (Apple Magic Trackpad style)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { viewModel.resetMouse() },
                            onDragEnd = { viewModel.resetMouse() },
                            onDragCancel = { viewModel.resetMouse() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                viewModel.sendMouseMove(dx = dragAmount.x, dy = dragAmount.y)
                            }
                        )
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        viewModel.sendMouseMove(0f, 0f, buttons = 1)
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            viewModel.sendMouseMove(0f, 0f, buttons = 0)
                        }, 50)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                shape = RoundedCornerShape(32.dp),
                color = SoftGrey.copy(alpha = 0.5f), // Frosted glass look
                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.2f))
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = "Touchpad gesture",
                        tint = Platinum.copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Trackpad", color = Platinum.copy(alpha = 0.4f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Scroll rail
            Surface(
                modifier = Modifier
                    .width(48.dp)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (Math.abs(dragAmount.y) > 0.5f) {
                                    viewModel.sendMouseMove(0f, 0f, wheel = if (dragAmount.y > 0) -1 else 1)
                                }
                            }
                        )
                    },
                shape = RoundedCornerShape(24.dp),
                color = SoftGrey.copy(alpha = 0.4f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.UnfoldMore, contentDescription = "Scroll area", tint = Platinum.copy(alpha = 0.3f))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Mouse buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MouseButton(
                modifier = Modifier.weight(1f),
                text = "LEFT",
                onClick = {
                    viewModel.sendMouseMove(0f, 0f, buttons = 1)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        viewModel.sendMouseMove(0f, 0f, buttons = 0)
                    }, 50)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )
            MouseButton(
                modifier = Modifier.weight(0.6f),
                text = "MID",
                onClick = {
                    viewModel.sendMouseMove(0f, 0f, buttons = 4)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        viewModel.sendMouseMove(0f, 0f, buttons = 0)
                    }, 50)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )
            MouseButton(
                modifier = Modifier.weight(1f),
                text = "RIGHT",
                onClick = {
                    viewModel.sendMouseMove(0f, 0f, buttons = 2)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        viewModel.sendMouseMove(0f, 0f, buttons = 0)
                    }, 50)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// MACRO DASHBOARD TAB
// ────────────────────────────────────────────────────────────────────────────────

@Composable
fun MacroDashboardTab(viewModel: MainViewModel) {
    val customMacros by viewModel.customMacros.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    // Built-in system macros for Mac
    val builtInMacros = remember {
        listOf(
            MacroItem("Lock Mac", Icons.Default.Lock, "LOCK_CMD", AccentBlue),
            MacroItem("Spotlight", Icons.Default.Search, "SPOT_CMD", AccentPurple),
            MacroItem("Screenshot", Icons.Default.CameraAlt, "SHOT_CMD", AccentTeal),
            MacroItem("Mute Mic", Icons.Default.MicOff, "MUTE_CMD", ErrorRed),
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("MACRO DASHBOARD", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            IconButton(onClick = { showAddDialog = true }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Add Macro", tint = AccentGold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item(span = { GridItemSpan(2) }) {
                Text("QUICK ACTIONS", color = Silver.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
            }
            
            items(builtInMacros) { macro ->
                MacroCard(
                    macro = macro,
                    showDelete = false,
                    onClick = { handleMacroExecution(viewModel, macro.command) }
                )
            }

            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("CUSTOM MACROS", color = Silver.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
                    if (customMacros.isEmpty()) {
                        Text("Tap + to add", color = Silver.copy(alpha = 0.3f), fontSize = 10.sp)
                    }
                }
            }

            if (customMacros.isNotEmpty()) {
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

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var command by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Graphite,
            title = { Text("Create Custom Macro", color = Platinum) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Use '&&' to chain multiple commands.", color = Silver, fontSize = 12.sp)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Macro Name") },
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
                        label = { Text("Shell Command / Text") },
                        placeholder = { Text("e.g. git add . && git commit") },
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
                ) { Text("Create", color = Obsidian, fontWeight = FontWeight.Bold) }
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
        "SLEEP_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_CTRL, HidKeyCodes.MODIFIER_LEFT_SHIFT, HidKeyCodes.KEY_F12))
        "SPOT_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_SPACE))
        "SHOT_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.MODIFIER_LEFT_SHIFT, HidKeyCodes.KEY_4))
        "MUTE_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.MODIFIER_LEFT_SHIFT, HidKeyCodes.KEY_A))
        "CAMERA_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.MODIFIER_LEFT_SHIFT, HidKeyCodes.KEY_V))
        "HAND_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.MODIFIER_LEFT_CTRL, HidKeyCodes.KEY_H))
        "SCREEN_CMD" -> viewModel.sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.MODIFIER_LEFT_SHIFT, HidKeyCodes.KEY_E))
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
            // Icon Background
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(macro.color.copy(alpha = 0.15f), CircleShape)
                    .align(Alignment.TopStart),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    macro.icon,
                    contentDescription = "Macro: ${macro.name}",
                    tint = macro.color,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            // Delete Button
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
                        contentDescription = "Delete macro",
                        tint = ErrorRed,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Text
            Column(
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
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

// ────────────────────────────────────────────────────────────────────────────────
// ADVANCED TAB
// ────────────────────────────────────────────────────────────────────────────────

@Composable
fun AdvancedTab(viewModel: MainViewModel, onNavigateToSettings: () -> Unit, onNavigateToSnippets: () -> Unit = {}, onNavigateToShortcuts: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    val currentMedia by viewModel.currentMedia.collectAsState()
    val prefs = remember { context.getSharedPreferences("rabit_prefs", android.content.Context.MODE_PRIVATE) }
    
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedFileUri = uri
        uri?.let {
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                fileName = cursor.getString(nameIndex)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("MEDIA & FILE CONTROLS", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        
        // Media Artwork Sync Card
        if (currentMedia != null) {
            currentMedia?.let { media ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SoftGrey.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(72.dp),
                            color = SoftGrey,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (media.artUrl.isNotBlank()) {
                                AsyncImage(model = media.artUrl, contentDescription = "Album Art", contentScale = ContentScale.Crop)
                            } else {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Silver, modifier = Modifier.padding(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(media.title, color = Platinum, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(media.artist, color = Silver, fontSize = 13.sp, maxLines = 1)
                            if (media.album.isNotBlank()) {
                                Text(media.album, color = Silver.copy(alpha = 0.5f), fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Graphite.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MusicOff, contentDescription = null, tint = Silver.copy(alpha = 0.2f), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("No media playing", color = Silver.copy(alpha = 0.4f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Play music on your Mac to see controls", color = Silver.copy(alpha = 0.25f), fontSize = 12.sp)
                    }
                }
            }
        }

        // Media Controls
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SoftGrey.copy(alpha = 0.5f),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("MEDIA", color = Silver, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MediaIcon(Icons.Default.SkipPrevious) { viewModel.sendConsumerKey(HidKeyCodes.MEDIA_PREVIOUS) }
                    MediaIcon(Icons.Default.VolumeDown) { viewModel.sendConsumerKey(HidKeyCodes.MEDIA_VOL_DOWN) }
                    MediaIcon(Icons.Default.PlayArrow) { viewModel.sendConsumerKey(HidKeyCodes.MEDIA_PLAY_PAUSE) }
                    MediaIcon(Icons.Default.VolumeUp) { viewModel.sendConsumerKey(HidKeyCodes.MEDIA_VOL_UP) }
                    MediaIcon(Icons.Default.SkipNext) { viewModel.sendConsumerKey(HidKeyCodes.MEDIA_NEXT) }
                }
            }
        }

        // File Typer
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SoftGrey.copy(alpha = 0.5f),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("REMOTE FILE TYPER", color = Silver, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { filePicker.launch("*/*") },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Obsidian.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, tint = AccentBlue)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (selectedFileUri == null) "Select text file..." else fileName,
                            color = if (selectedFileUri == null) Silver else Platinum,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                        if (selectedFileUri == null) {
                            Text("Supports .txt, .md, .kt, etc.", color = Silver.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    }
                    if (selectedFileUri != null) {
                        Button(
                            onClick = {
                                selectedFileUri?.let { uri ->
                                    scope.launch {
                                        val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                                            BufferedReader(InputStreamReader(stream)).readText()
                                        }
                                        content?.let { viewModel.sendText(it) }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("TYPE", color = Obsidian, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Quick Links Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallActionCard(modifier = Modifier.weight(1f), title = "SNIPPETS", icon = Icons.Default.TextSnippet, accent = AccentGold) { onNavigateToSnippets() }
            SmallActionCard(modifier = Modifier.weight(1f), title = "SHORTCUTS", icon = Icons.Default.Keyboard, accent = AccentPurple) { onNavigateToShortcuts() }
        }

        // Handoff Quick Action
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = clipboard.primaryClip
                    if (clip != null && clip.itemCount > 0) {
                        val text = clip.getItemAt(0).text?.toString()
                        if (!text.isNullOrBlank() && text.startsWith("http")) {
                            val macIp = prefs.getString("mac_ip", "") ?: ""
                            if (macIp.isNotBlank()) {
                                scope.launch {
                                    try {
                                        val url = java.net.URL("http://$macIp:8766/handoff")
                                        val conn = url.openConnection() as java.net.HttpURLConnection
                                        conn.requestMethod = "POST"
                                        conn.setRequestProperty("Content-Type", "application/json")
                                        conn.doOutput = true
                                        conn.outputStream.write("{\"url\":\"$text\"}".toByteArray())
                                        conn.responseCode
                                        conn.disconnect()
                                        android.widget.Toast.makeText(context, "Sent to Mac!", android.widget.Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Failed: Set Mac IP in Settings", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                android.widget.Toast.makeText(context, "Set Mac IP in Settings first", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            android.widget.Toast.makeText(context, "Copy a URL first", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                },
            color = AccentTeal.copy(alpha = 0.1f),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, AccentTeal.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).background(AccentTeal.copy(alpha=0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text("Handoff to Mac", color = Platinum.copy(alpha=0.9f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Opens clipboard URL on your Mac", color = Silver.copy(alpha=0.6f), fontSize = 12.sp)
                }
            }
        }

        // Action Cards Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallActionCard(modifier = Modifier.weight(1f), title = "UNLOCK", icon = Icons.Default.LockOpen, accent = AccentBlue) { viewModel.unlockMac() }
            SmallActionCard(modifier = Modifier.weight(1f), title = "SETTINGS", icon = Icons.Default.Settings, accent = SuccessGreen) { onNavigateToSettings() }
        }
        
        SmallActionCard(modifier = Modifier.fillMaxWidth(), title = "DISCONNECT", icon = Icons.Default.BluetoothDisabled, accent = ErrorRed) { viewModel.disconnect() }
    }
}

@Composable
fun MediaIcon(icon: ImageVector, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    IconButton(
        onClick = {
            onClick()
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        },
        modifier = Modifier
            .size(48.dp)
            .background(Color.Transparent, CircleShape)
    ) {
        Icon(icon, contentDescription = null, tint = Platinum.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
    }
}

@Composable
fun SmallActionCard(
    modifier: Modifier,
    title: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(110.dp)
            .clickable { onClick() },
        color = SoftGrey.copy(alpha = 0.5f),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.2f))
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            // Icon Background circle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(accent.copy(alpha = 0.15f), CircleShape)
                    .align(Alignment.TopStart),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(18.dp))
            }
            
            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Text(title, color = Platinum.copy(alpha=0.9f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// BOTTOM BAR
// ────────────────────────────────────────────────────────────────────────────────

@Composable
fun PremiumBottomBar(
    selectedTab: Int,
    onNavigateToAssistant: () -> Unit,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp, top = 8.dp), // Floating spacing
        color = Obsidian.copy(alpha = 0.85f),
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                Triple(Icons.Default.Keyboard, "Keys", 0),
                Triple(Icons.Default.Mouse, "Pad", 1),
                Triple(Icons.Default.Dashboard, "Macros", 2),
                Triple(Icons.Default.Tune, "More", 3)
            )

            tabs.forEach { (icon, label, index) ->
                val isSelected = selectedTab == index
                Column(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(index) }
                        .background(if (isSelected) AccentBlue.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = if (isSelected) AccentBlue else Silver.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        label,
                        color = if (isSelected) AccentBlue else Silver.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            // Small glowing separator
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(26.dp)
                    .background(BorderColor.copy(alpha = 0.5f))
            )

            // AI Assistant Button always has a special tint
            Column(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onNavigateToAssistant() }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = "AI Assistant",
                    tint = AccentGold,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text("AI", color = AccentGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// PREMIUM HEADER
// ────────────────────────────────────────────────────────────────────────────────

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
                "RABIT",
                color = Platinum,
                fontSize = 22.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
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

// ────────────────────────────────────────────────────────────────────────────────
// DUAL KEYBOARD TAB
// ────────────────────────────────────────────────────────────────────────────────

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
    var isRecording by remember { mutableStateOf(false) }

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
                SpeechToTextButton(
                    onResult = { result ->
                        viewModel.onVoiceResult(result)
                        val updatedText = textFieldValue.text + (if (textFieldValue.text.isNotEmpty()) " " else "") + result
                        textFieldValue = TextFieldValue(updatedText)
                    },
                    isRecording = isRecording,
                    onRecordingStateChange = { isRecording = it }
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

// ────────────────────────────────────────────────────────────────────────────────
// KEYBOARD LAYOUT
// ────────────────────────────────────────────────────────────────────────────────

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
