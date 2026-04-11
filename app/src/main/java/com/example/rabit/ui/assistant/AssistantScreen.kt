package com.example.rabit.ui.assistant

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.keyboard.PremiumBottomBar
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
    var showMacroLaunchpad by remember { mutableStateOf(false) }
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
        Scaffold(
            topBar = {
                PremiumChatTopBar(
                    modelName = modelName,
                    isThinking = uiState is AssistantUiState.Loading,
                    connectionState = deviceConnectionState,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onClearChat = { viewModel.clearMessages() },
                    onExportChat = {
                        val fullChat = messages.joinToString("\n\n") { msg ->
                            val role = if (msg.isUser) "You" else "Rabit AI"
                            "$role:\n${msg.content}"
                        }
                        if (fullChat.isNotBlank()) {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, fullChat)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Export Chat"))
                        }
                    },
                    onLaunchpadClick = { showMacroLaunchpad = true },
                    onSettingsClick = onNavigateToSettings
                )
            },
            bottomBar = {
                PremiumBottomBar(
                    selectedTab = -1,
                    onNavigateToAssistant = { /* Already here */ },
                    onTabSelected = { onNavigateToKeyboard() }
                )
            },
            containerColor = ChatSurface
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
    if (showMacroLaunchpad) {
        MacroLaunchpad(
            viewModel = mainViewModel,
            onDismiss = { showMacroLaunchpad = false }
        )
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
                is com.example.rabit.ui.MainViewModel.GenieState.Success -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Executed HID sequence: ${state.macroName}", color = SuccessGreen, fontSize = 14.sp)
                    }
                }
                is com.example.rabit.ui.MainViewModel.GenieState.Error -> {
                    Text(state.message, color = StopRed, fontSize = 14.sp)
                }
                else -> {
                    com.example.rabit.ui.components.VibrantGradientButton(
                        text = "Generate & Run",
                        onClick = { viewModel.generateSmartMacro(intent) },
                        gradient = Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500)))
                    )
                }
            }
        }
    }
}

@Composable
fun AssistantDrawerContent(
    viewModel: AssistantViewModel,
    messageCount: Int,
    onPromptLibraryClick: () -> Unit,
    onHardwareMonitorClick: () -> Unit,
    onMacroGenieClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        com.example.rabit.ui.components.PremiumSectionHeader("Tools")
        
        DrawerItem(
            icon = Icons.Default.AutoAwesome,
            label = "Macro Genie",
            iconTint = AccentGold,
            onClick = onMacroGenieClick
        )
        
        DrawerItem(
            icon = Icons.Default.LibraryBooks,
            label = "Prompt Library",
            onClick = onPromptLibraryClick
        )
        
        DrawerItem(
            icon = Icons.Default.Memory,
            label = "Hardware Monitor",
            onClick = onHardwareMonitorClick
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        com.example.rabit.ui.components.PremiumSectionHeader("Recent Sessions")
        Text("Your chat history will appear here", color = Silver.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(16.dp))
    }
}
