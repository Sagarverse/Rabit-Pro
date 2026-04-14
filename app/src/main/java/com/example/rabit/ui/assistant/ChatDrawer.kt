package com.example.rabit.ui.assistant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*
import com.example.rabit.data.repository.ChatSession

@Composable
fun AssistantDrawerContent(
    viewModel: AssistantViewModel, 
    messageCount: Int = 0,
    onPromptLibraryClick: () -> Unit,
    onHardwareMonitorClick: () -> Unit,
    onMacroGenieClick: () -> Unit
) {
    val chatSessions by viewModel.chatSessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val downloadedModels by viewModel.downloadedModels.collectAsState()
    val activeOfflineModel by viewModel.activeOfflineModel.collectAsState()
    val modelStorageUsage by viewModel.modelStorageUsageMb.collectAsState()
    val temperature by viewModel.temperature.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp)
    ) {

        Spacer(modifier = Modifier.height(24.dp))

        // ── Chat History Section ──
        ChatHistorySection(
            sessions = chatSessions,
            currentId = currentSessionId,
            onSessionSelect = { viewModel.loadChatSession(it) },
            onSessionDelete = { viewModel.deleteChatSession(it) }
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = BorderColor.copy(alpha = 0.12f))
        Spacer(modifier = Modifier.height(24.dp))

        // ── Model Manager Section ──
        ModelManagerSection(
            availableModels = viewModel.availableModels,
            downloadedModels = downloadedModels,
            activeModel = activeOfflineModel,
            storageUsage = modelStorageUsage,
            onDownload = { viewModel.downloadSpecificModel(it) },
            onDelete = { viewModel.deleteOfflineModel(it) },
            onSelect = { viewModel.selectOfflineModel(it) }
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = BorderColor.copy(alpha = 0.12f))
        Spacer(modifier = Modifier.height(24.dp))

        // ── Quick Tools ──
        Text(
            "QUICK TOOLS",
            color = Silver.copy(alpha = 0.35f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            DrawerItem(Icons.Default.AutoAwesome, "Macro Genie", AccentBlue, onClick = onMacroGenieClick)
            DrawerItem(Icons.AutoMirrored.Filled.LibraryBooks, "Prompt Library", Silver, onClick = onPromptLibraryClick)
            DrawerItem(Icons.Default.Memory, "Hardware Monitor", Silver, onClick = onHardwareMonitorClick)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Temperature Slider ──
        Text(
            "MODEL TEMPERATURE",
            color = Silver.copy(alpha = 0.35f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            color = Color.Transparent,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.2f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (temperature < 0.4f) "Precise" else if (temperature > 0.7f) "Creative" else "Balanced",
                        color = Platinum,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        String.format("%.2f", temperature),
                        color = Platinum,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = temperature,
                    onValueChange = { viewModel.updateTemperature(it) },
                    valueRange = 0f..1f,
                    steps = 10,
                    colors = SliderDefaults.colors(
                        thumbColor = Platinum,
                        activeTrackColor = Platinum.copy(alpha = 0.7f),
                        inactiveTrackColor = SoftGrey.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.height(30.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── New Session Button ──
        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
            Button(
                onClick = { viewModel.clearConversation() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(listOf(AccentBlue, AccentBlue.copy(alpha = 0.7f))),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("NEW SESSION", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ════════════════════════════════════════════════════════════════════
// ── Chat History Section
// ════════════════════════════════════════════════════════════════════

@Composable
fun ChatHistorySection(
    sessions: List<ChatSession>,
    currentId: String?,
    onSessionSelect: (String) -> Unit,
    onSessionDelete: (String) -> Unit
) {
    Column {
        Text(
            "CHATS",
            color = Silver.copy(alpha = 0.35f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (sessions.isEmpty()) {
            Text(
                "No previous chats",
                color = Silver.copy(alpha = 0.3f),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        } else {
            sessions.take(10).forEach { session ->
                val isSelected = session.id == currentId
                Surface(
                    onClick = { onSessionSelect(session.id) },
                    color = if (isSelected) AccentBlue.copy(alpha = 0.1f) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isSelected) Icons.Default.ChatBubble else Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = if (isSelected) AccentBlue else Silver.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                session.title,
                                color = if (isSelected) Platinum else Silver.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${session.messageCount} msgs · ${formatRelativeTime(session.lastModified)}",
                                color = Silver.copy(alpha = 0.4f),
                                fontSize = 11.sp
                            )
                        }
                        IconButton(
                            onClick = { onSessionDelete(session.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline, 
                                contentDescription = "Delete", 
                                tint = if (isSelected) AccentBlue.copy(alpha = 0.8f) else Silver.copy(alpha = 0.2f), 
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// ── Model Manager Section
// ════════════════════════════════════════════════════════════════════

@Composable
fun ModelManagerSection(
    availableModels: List<com.example.rabit.data.gemini.ModelInfo>,
    downloadedModels: List<com.example.rabit.data.gemini.ModelInfo>,
    activeModel: com.example.rabit.data.gemini.ModelInfo?,
    storageUsage: Long,
    onDownload: (com.example.rabit.data.gemini.ModelInfo) -> Unit,
    onDelete: (com.example.rabit.data.gemini.ModelInfo) -> Unit,
    onSelect: (com.example.rabit.data.gemini.ModelInfo) -> Unit,
    onImportLocal: (() -> Unit)? = null // Expose import file picker
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "OFFLINE MODELS",
                color = Silver.copy(alpha = 0.35f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            if (storageUsage > 0) {
                Text(
                    "${storageUsage} MB",
                    color = Silver.copy(alpha = 0.25f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        availableModels.forEach { model ->
            val isDownloaded = downloadedModels.any { it.id == model.id }
            val isActive = activeModel?.id == model.id

            Surface(
                onClick = { if (isDownloaded) onSelect(model) },
                color = if (isActive) AccentBlue.copy(alpha = 0.1f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                border = if (isActive) BorderStroke(0.5.dp, AccentBlue.copy(alpha = 0.3f)) else null
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isActive) AccentBlue.copy(alpha = 0.2f) else SoftGrey.copy(alpha = 0.1f),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (model.isGpu) Icons.Default.Bolt else Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = if (isActive) AccentBlue else Silver.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            model.name,
                            color = if (isActive) Platinum else Silver.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (isDownloaded) "Ready · ${model.sizeLabel}" else "${model.description} · ${model.sizeLabel}",
                            color = Silver.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (!isDownloaded) {
                        Row {
                            // Import local file button
                            if (onImportLocal != null) {
                                IconButton(onClick = { onImportLocal() }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.UploadFile, contentDescription = "Import", tint = Silver, modifier = Modifier.size(20.dp))
                                }
                            }
                            // Browser / Download button
                            IconButton(
                                onClick = { 
                                    // Because Gemma models are gated on Kaggle/HF (yielding HTTP 401), redirect to browser
                                    val i = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.kaggle.com/models/google/gemma/frameworks/tfLite"))
                                    context.startActivity(i)
                                }, 
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Get Model", tint = AccentBlue, modifier = Modifier.size(20.dp))
                            }
                        }
                    } else if (isActive) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = Platinum, modifier = Modifier.size(20.dp))
                    } else {
                        IconButton(onClick = { onDelete(model) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Silver.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// ── Drawer Item
// ════════════════════════════════════════════════════════════════════

@Composable
fun DrawerItem(
    icon: ImageVector,
    label: String,
    iconTint: Color = Silver,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(iconTint.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(label, color = Platinum, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Silver.copy(alpha = 0.2f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
