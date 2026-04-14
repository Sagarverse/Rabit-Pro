package com.example.rabit.ui.assistant

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.data.voice.VoiceState
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*
import kotlinx.coroutines.launch

// ════════════════════════════════════════════════════════════════════
// ── Main Assistant Screen (Slim Orchestrator)
// ════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel,
    mainViewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToKeyboard: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val modelName by viewModel.selectedModelName.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val context = LocalContext.current
    val leftDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var rightPanelOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = rightPanelOpen || leftDrawerState.isOpen) {
        when {
            rightPanelOpen -> rightPanelOpen = false
            leftDrawerState.isOpen -> scope.launch { leftDrawerState.close() }
        }
    }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val deviceConnectionState by viewModel.deviceConnectionState.collectAsState()
    val featureWebBridgeVisible by mainViewModel.featureWebBridgeVisible.collectAsState()
    val featureAutomationVisible by mainViewModel.featureAutomationVisible.collectAsState()
    val featureAssistantVisible by mainViewModel.featureAssistantVisible.collectAsState()
    val featureSnippetsVisible by mainViewModel.featureSnippetsVisible.collectAsState()
    val featureWakeOnLanVisible by mainViewModel.featureWakeOnLanVisible.collectAsState()
    val featureSshTerminalVisible by mainViewModel.featureSshTerminalVisible.collectAsState()

    var showPromptLibrary by remember { mutableStateOf(false) }
    var showHardwareMonitor by remember { mutableStateOf(false) }
    var showMacroGenie by remember { mutableStateOf(false) }

    // Auto-Push logic — fires haptic when pushing
    LaunchedEffect(uiState) {
        if (uiState is AssistantUiState.Success && viewModel.autoPushEnabled.value) {
            val resp = (uiState as AssistantUiState.Success).response
            if (resp.text.isNotBlank()) {
                performHapticDoubleTap(context)
                mainViewModel.sendText(resp.text)
            }
        }
        // Haptic on AI response arrival
        if (uiState is AssistantUiState.Success) {
            performHapticConfirm(context)
        }
    }

    // Haptic tick when AI starts thinking
    LaunchedEffect(uiState) {
        if (uiState is AssistantUiState.Loading) {
            performHapticTick(context)
        }
    }

    ModalNavigationDrawer(
        drawerState = leftDrawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = ChatSurface,
                modifier = Modifier.width(300.dp)
            ) {
                AssistantLeftPanelContent(
                    featureWebBridgeVisible = featureWebBridgeVisible,
                    featureAutomationVisible = featureAutomationVisible,
                    featureAssistantVisible = featureAssistantVisible,
                    featureSnippetsVisible = featureSnippetsVisible,
                    featureWakeOnLanVisible = featureWakeOnLanVisible,
                    featureSshTerminalVisible = featureSshTerminalVisible,
                    onNavigate = {
                        onNavigate(it)
                        scope.launch { leftDrawerState.close() }
                    }
                )
            }
        }
    ) {
        val hidConnectionState by mainViewModel.connectionState.collectAsState()

        Scaffold(
            containerColor = ChatSurface,
            topBar = {
                PremiumChatTopBar(
                    modelName = modelName,
                    isThinking = uiState is AssistantUiState.Loading,
                    connectionState = hidConnectionState,
                    onLeftPanelClick = { scope.launch { leftDrawerState.open() } },
                    onRightPanelClick = { rightPanelOpen = true },
                    onClearChat = { viewModel.clearConversation() },
                    onNewChat = { viewModel.clearConversation() },
                    onExportChat = { viewModel.exportChatHistory(context) },
                    onSettingsClick = { onNavigateToSettings() }
                )
            }
        ) { padding ->
            val modelLoadState by viewModel.modelLoadState.collectAsState()
            val modelCopyProgress by viewModel.modelCopyProgress.collectAsState()
            val modelLastError by viewModel.modelLastError.collectAsState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Model Status (Visible during loading or errors)
                LocalModelStatusBar(
                    state = modelLoadState,
                    progress = modelCopyProgress,
                    error = modelLastError,
                    onDismiss = { viewModel.clearModelError() }
                )

                Box(modifier = Modifier.weight(1f)) {
                    if (messages.isEmpty()) {
                        PremiumWelcomeScreen(viewModel)
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(messages, key = { it.id }) { message ->
                                AnimatedMessageEntry(message, viewModel, mainViewModel)
                            }
                        }

                        // Scroll to bottom FAB
                        val isScrolledUp = listState.canScrollForward
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = 16.dp)
                        ) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isScrolledUp,
                                enter = fadeIn(animationSpec = tween(AssistantMotion.FAB_FADE_IN)) + scaleIn(animationSpec = spring(dampingRatio = AssistantMotion.SPRING_ENTRY_DAMPING, stiffness = AssistantMotion.SPRING_ENTRY_STIFFNESS)),
                                exit = fadeOut(animationSpec = tween(AssistantMotion.FAB_FADE_OUT)) + scaleOut(animationSpec = spring(dampingRatio = AssistantMotion.SPRING_EXIT_DAMPING, stiffness = AssistantMotion.SPRING_EXIT_STIFFNESS))
                            ) {
                                SmallFloatingActionButton(
                                    onClick = { scope.launch { listState.animateScrollToItem(messages.size - 1) } },
                                    containerColor = AccentBlue.copy(alpha = 0.5f),
                                    contentColor = Platinum,
                                    shape = CircleShape,
                                    modifier = Modifier.border(0.5.dp, BorderColor.copy(alpha = 0.4f), CircleShape)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Scroll to Bottom", modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(1.dp))

                // Sticky Bottom Input Area
                PremiumInputArea(viewModel, mainViewModel)
            }
        }

        AnimatedVisibility(
            visible = rightPanelOpen,
            enter = fadeIn(tween(AssistantMotion.PANEL_FADE_IN)) + slideInHorizontally(initialOffsetX = { it / 3 }, animationSpec = tween(AssistantMotion.PANEL_ENTER, easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(AssistantMotion.PANEL_FADE_OUT)) + slideOutHorizontally(targetOffsetX = { it / 3 }, animationSpec = tween(AssistantMotion.PANEL_EXIT, easing = FastOutSlowInEasing))
        ) {
            val blocker = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable { rightPanelOpen = false }
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.weight(1f))
                    Column(
                        modifier = Modifier
                            .width(320.dp)
                            .fillMaxHeight()
                            .background(GlassCardGradient)
                            .border(0.8.dp, BorderStrong.copy(alpha = 0.5f), RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                            .clickable(
                                interactionSource = blocker,
                                indication = null
                            ) { }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Assistant Tools",
                                    color = Platinum,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "Prompts, diagnostics, and macro utilities",
                                    color = Silver.copy(alpha = 0.72f),
                                    fontSize = 11.sp
                                )
                            }
                            IconButton(onClick = { rightPanelOpen = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close tools panel", tint = Silver)
                            }
                        }
                        HorizontalDivider(color = BorderColor.copy(alpha = 0.22f))
                        AssistantDrawerContent(
                            viewModel = viewModel,
                            messageCount = messages.size,
                            onPromptLibraryClick = { showPromptLibrary = true; rightPanelOpen = false },
                            onHardwareMonitorClick = { showHardwareMonitor = true; rightPanelOpen = false },
                            onMacroGenieClick = { showMacroGenie = true; rightPanelOpen = false }
                        )
                    }
                }
            }
        }
    }

    if (showPromptLibrary) {
        PromptLibraryModal(
            onDismiss = { showPromptLibrary = false },
            onSelectPrompt = { prompt ->
                viewModel.onInputChanged(prompt)
                showPromptLibrary = false
            }
        )
    }
    if (showHardwareMonitor) {
        HardwareMonitorModal(onDismiss = { showHardwareMonitor = false })
    }

    if (showMacroGenie) {
        MacroGenieModal(
            viewModel = mainViewModel,
            onDismiss = { showMacroGenie = false }
        )
    }
}

@Composable
private fun AssistantLeftPanelContent(
    featureWebBridgeVisible: Boolean,
    featureAutomationVisible: Boolean,
    featureAssistantVisible: Boolean,
    featureSnippetsVisible: Boolean,
    featureWakeOnLanVisible: Boolean,
    featureSshTerminalVisible: Boolean,
    onNavigate: (String) -> Unit
) {
    val scrollState = androidx.compose.foundation.rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
            Text("Navigation", color = Silver, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        AssistantNavDrawerItem(
            label = "Control Hub",
            subLabel = "Keyboard & Trackpad",
            icon = Icons.AutoMirrored.Filled.Dvr,
            isSelected = false,
            onClick = { onNavigate("keyboard") }
        )

        if (featureWebBridgeVisible) {
            AssistantNavDrawerItem(
                label = "Web Bridge Hub",
                subLabel = "File Sharing & Sync",
                icon = Icons.Default.CloudSync,
                isSelected = false,
                onClick = { onNavigate("web_bridge") }
            )
        }

        if (featureAutomationVisible) {
            AssistantNavDrawerItem(
                label = "Automation Hub",
                subLabel = "Macros & Quick Actions",
                icon = Icons.Default.Bolt,
                isSelected = false,
                onClick = { onNavigate("automation") }
            )

            AssistantNavDrawerItem(
                label = "Payload Injector",
                subLabel = "DuckyScript command injection",
                icon = Icons.Default.ElectricBolt,
                isSelected = false,
                onClick = { onNavigate("injector") }
            )
        }

        AssistantNavDrawerItem(
            label = "Media Control Deck",
            subLabel = "Now Playing & Transport",
            icon = Icons.Default.MusicNote,
            isSelected = false,
            onClick = { onNavigate("media_deck") }
        )

        AssistantNavDrawerItem(
            label = "AirPlay Receiver",
            subLabel = "Wi-Fi audio target (experimental)",
            icon = Icons.Default.Speaker,
            isSelected = false,
            onClick = { onNavigate("airplay_receiver") }
        )

        if (featureWakeOnLanVisible) {
            AssistantNavDrawerItem(
                label = "Wake-on-LAN",
                subLabel = "Boot Sleeping Mac/PC",
                icon = Icons.Default.PowerSettingsNew,
                isSelected = false,
                onClick = { onNavigate("wake_on_lan") }
            )
        }

        if (featureSshTerminalVisible) {
            AssistantNavDrawerItem(
                label = "SSH Terminal",
                subLabel = "Native secure shell",
                icon = Icons.Default.Terminal,
                isSelected = false,
                onClick = { onNavigate("ssh_terminal") }
            )
        }

        if (featureAssistantVisible) {
            AssistantNavDrawerItem(
                label = "AI Assistant",
                subLabel = "Smart Control & Logic",
                icon = Icons.Default.AutoAwesome,
                isSelected = true,
                onClick = { onNavigate("assistant") }
            )
        }

        if (featureSnippetsVisible) {
            AssistantNavDrawerItem(
                label = "Snippets",
                subLabel = "Saved reusable text blocks",
                icon = Icons.Default.ContentPaste,
                isSelected = false,
                onClick = { onNavigate("snippets") }
            )
        }

        AssistantNavDrawerItem(
            label = "Password Manager",
            subLabel = "Biometric + password push settings",
            icon = Icons.Default.Password,
            isSelected = false,
            onClick = { onNavigate("password_manager") }
        )

        AssistantNavDrawerItem(
            label = "System Settings",
            subLabel = "Configuration & Sensitivity",
            icon = Icons.Default.Settings,
            isSelected = false,
            onClick = { onNavigate("settings") }
        )
    }
}

@Composable
private fun AssistantNavDrawerItem(
    label: String,
    subLabel: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) AccentBlue.copy(alpha = 0.18f) else Color.Transparent,
        border = BorderStroke(
            0.5.dp,
            if (isSelected) AccentBlue.copy(alpha = 0.35f) else BorderColor.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) AccentBlue else Silver,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(label, color = Platinum, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subLabel, color = Silver.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// ── Utility
// ════════════════════════════════════════════════════════════════════

fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        else -> "${diff / 86_400_000}d ago"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroGenieModal(
    viewModel: com.example.rabit.ui.MainViewModel,
    onDismiss: () -> Unit
) {
    var intent by remember { mutableStateOf("") }
    val genieState by viewModel.genieState.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ChatSurface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Silver.copy(alpha = 0.3f))
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "SMART MACRO GENIE",
                        color = AccentBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Generate and execute HID macros from natural language.",
                        color = Silver.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close macro genie", tint = Silver)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Graphite.copy(alpha = 0.45f),
                border = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Macro Intent",
                        color = Silver.copy(alpha = 0.65f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = intent,
                        onValueChange = { intent = it },
                        placeholder = { Text("e.g. 'Mute Zoom' or 'New Window'", color = Silver.copy(alpha = 0.5f)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        trailingIcon = {
                            val voiceState by viewModel.voiceState.collectAsState()
                            val voiceResult by viewModel.voiceResult.collectAsState()

                            if (voiceResult.isNotBlank() && voiceState == VoiceState.SUCCESS) {
                                intent = voiceResult
                                viewModel.resetVoiceState()
                            }

                            PulsingVoiceButton(
                                state = voiceState,
                                onClick = {
                                    if (voiceState == VoiceState.LISTENING) viewModel.stopVoiceRecognition()
                                    else viewModel.startVoiceRecognition()
                                }
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Graphite.copy(alpha = 0.5f),
                            unfocusedContainerColor = Graphite.copy(alpha = 0.3f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Platinum,
                            unfocusedTextColor = Platinum
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (val state = genieState) {
                is com.example.rabit.ui.MainViewModel.GenieState.Thinking -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AccentBlue, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("AI is brewing your macro...", color = Silver, fontSize = 14.sp)
                    }
                }
                is com.example.rabit.ui.MainViewModel.GenieState.Executing -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(state.currentStep, color = Platinum, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                        
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = AccentBlue,
                            trackColor = Graphite
                        )
                        
                        TextButton(
                            onClick = { viewModel.cancelMacro() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("ABORT SEQUENCE", color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                is com.example.rabit.ui.MainViewModel.GenieState.Success -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Macro Complete: ${state.macroName}", color = AccentBlue, fontSize = 14.sp)
                    }
                }
                is com.example.rabit.ui.MainViewModel.GenieState.Error -> {
                    Column {
                        Text(state.message, color = StopRed, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        com.example.rabit.ui.components.VibrantGradientButton(
                            text = "Try Again",
                            onClick = { viewModel.generateSmartMacro(intent) },
                            gradient = Brush.linearGradient(listOf(Color(0xFF2F3136), Color(0xFF17191D)))
                        )
                    }
                }
                else -> {
                    com.example.rabit.ui.components.VibrantGradientButton(
                        text = "Summon Genie",
                        onClick = { viewModel.generateSmartMacro(intent) },
                        gradient = Brush.linearGradient(listOf(Color(0xFF0A84FF), Color(0xFF0D6EDB)))
                    )
                }
            }
        }
    }
}


@Composable
fun PulsingVoiceButton(
    state: com.example.rabit.data.voice.VoiceState,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voicePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state == com.example.rabit.data.voice.VoiceState.LISTENING) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = AssistantMotion.PULSE_DOT, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (state == com.example.rabit.data.voice.VoiceState.LISTENING) 0.8f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = AssistantMotion.PULSE_DOT, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(contentAlignment = Alignment.Center) {
        if (state == com.example.rabit.data.voice.VoiceState.LISTENING) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                    .background(AccentBlue.copy(alpha = pulseAlpha), CircleShape)
            )
        }
        
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (state == com.example.rabit.data.voice.VoiceState.LISTENING) AccentBlue.copy(alpha = 0.2f) 
                    else Color.Transparent, 
                    CircleShape
                )
        ) {
            Icon(
                if (state == com.example.rabit.data.voice.VoiceState.LISTENING) Icons.Default.Mic 
                else Icons.Default.MicNone,
                contentDescription = "Voice Input",
                tint = if (state == com.example.rabit.data.voice.VoiceState.LISTENING) AccentBlue else Silver,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
