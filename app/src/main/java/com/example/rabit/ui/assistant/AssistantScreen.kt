package com.example.rabit.ui.assistant

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
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
    onNavigateToKeyboard: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val modelName by viewModel.selectedModelName.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val deviceConnectionState by viewModel.deviceConnectionState.collectAsState()

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
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = ChatSurface,
                modifier = Modifier.width(300.dp)
            ) {
                AssistantDrawerContent(
                    viewModel = viewModel,
                    messageCount = messages.size,
                    onPromptLibraryClick = { showPromptLibrary = true; scope.launch { drawerState.close() } },
                    onHardwareMonitorClick = { showHardwareMonitor = true; scope.launch { drawerState.close() } },
                    onMacroGenieClick = { showMacroGenie = true; scope.launch { drawerState.close() } }
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
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onClearChat = { viewModel.clearConversation() },
                    onNewChat = { viewModel.clearConversation() },
                    onExportChat = { viewModel.exportChatHistory(context) },
                    onLaunchpadClick = { /* Not needed in Pro navigation */ },
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
                            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                                enter = fadeIn(animationSpec = tween(300)) + scaleIn(animationSpec = spring(dampingRatio = 0.6f, stiffness = 200f)),
                                exit = fadeOut(animationSpec = tween(200)) + scaleOut(animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f))
                            ) {
                                SmallFloatingActionButton(
                                    onClick = { scope.launch { listState.animateScrollToItem(messages.size - 1) } },
                                    containerColor = AiViolet.copy(alpha = 0.5f),
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
            Text(
                "SMART MACRO GENIE",
                color = AccentGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Describe what you want to do on your Mac, and Rabit AI will generate the HID sequence.",
                color = Silver,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(24.dp))

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

            Spacer(modifier = Modifier.height(24.dp))

            when (val state = genieState) {
                is com.example.rabit.ui.MainViewModel.GenieState.Thinking -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AccentGold, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("AI is brewing your macro...", color = Silver, fontSize = 14.sp)
                    }
                }
                is com.example.rabit.ui.MainViewModel.GenieState.Executing -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = AccentGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(state.currentStep, color = Platinum, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                        
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = AccentGold,
                            trackColor = Graphite
                        )
                        
                        TextButton(
                            onClick = { viewModel.cancelMacro() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("ABORT SEQUENCE", color = ErrorRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                is com.example.rabit.ui.MainViewModel.GenieState.Success -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Macro Complete: ${state.macroName}", color = SuccessGreen, fontSize = 14.sp)
                    }
                }
                is com.example.rabit.ui.MainViewModel.GenieState.Error -> {
                    Column {
                        Text(state.message, color = StopRed, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        com.example.rabit.ui.components.VibrantGradientButton(
                            text = "Try Again",
                            onClick = { viewModel.generateSmartMacro(intent) },
                            gradient = Brush.linearGradient(listOf(Color(0xFF888888), Color(0xFF444444)))
                        )
                    }
                }
                else -> {
                    com.example.rabit.ui.components.VibrantGradientButton(
                        text = "Summon Genie",
                        onClick = { viewModel.generateSmartMacro(intent) },
                        gradient = Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500)))
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
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (state == com.example.rabit.data.voice.VoiceState.LISTENING) 0.8f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
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
                    .background(AccentGold.copy(alpha = pulseAlpha), CircleShape)
            )
        }
        
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (state == com.example.rabit.data.voice.VoiceState.LISTENING) AccentGold.copy(alpha = 0.2f) 
                    else Color.Transparent, 
                    CircleShape
                )
        ) {
            Icon(
                if (state == com.example.rabit.data.voice.VoiceState.LISTENING) Icons.Default.Mic 
                else Icons.Default.MicNone,
                contentDescription = "Voice Input",
                tint = if (state == com.example.rabit.data.voice.VoiceState.LISTENING) AccentGold else Silver,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
