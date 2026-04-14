package com.example.rabit.ui.assistant

import android.net.Uri
import android.util.Log
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.rabit.ui.theme.*
import com.example.rabit.data.voice.VoiceState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptInputSection(viewModel: AssistantViewModel) {
    val input by viewModel.input.collectAsState()
    var isRecording by remember { mutableStateOf(false) }
    val maxChars = 2000

    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsProvider = remember { AssistantSettingsProvider(context) }
    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState is AssistantUiState.Loading
    val prefs = remember { context.getSharedPreferences("gemini_prefs", android.content.Context.MODE_PRIVATE) }

    val attachedImages by viewModel.attachedImages.collectAsState()
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // Focus state for input glow
    var isFocused by remember { mutableStateOf(false) }
    
    val showCommands by viewModel.showCommands.collectAsState()
    var showSystemModal by remember { mutableStateOf(false) }

    // Send button animation
    val sendButtonScale by animateFloatAsState(
        targetValue = if (input.isNotBlank() && !isLoading) 1f else 0f,
        animationSpec = spring(dampingRatio = AssistantMotion.SPRING_ENTRY_DAMPING, stiffness = 420f),
        label = "sendScale"
    )

    // ── Image Picker ──
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<android.net.Uri> ->
        if (uris.isNotEmpty()) {
            val newList = mutableListOf<Pair<android.net.Uri, String>>()
            for (uri in uris) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()
                    if (bytes != null) {
                        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        newList.add(uri to base64)
                    }
                } catch (e: Exception) {
                    Log.e("PromptInputSection", "Failed to read selected image: $uri", e)
                }
            }
            if (newList.isNotEmpty()) viewModel.attachImages(newList)
            if (uris.isNotEmpty() && newList.isEmpty()) {
                Toast.makeText(context, "Could not attach selected image(s)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Camera Launcher ──
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraUri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(cameraUri!!)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes != null) {
                    val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    viewModel.attachImages(listOf(cameraUri!! to base64))
                }
            } catch (e: Exception) {
                Log.e("PromptInputSection", "Failed to process captured image", e)
                Toast.makeText(context, "Could not attach captured image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── File Picker ──
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { urisList: List<android.net.Uri> ->
        urisList.forEach { uri ->
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                val fileName = cursor?.use { c ->
                    val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (c.moveToFirst() && nameIndex >= 0) c.getString(nameIndex) else "file"
                } ?: "file"
                val inputStream = context.contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.readText() ?: ""
                inputStream?.close()
                viewModel.attachFile(fileName, content)
            } catch (e: Exception) {
                Log.e("PromptInputSection", "Failed to attach file: $uri", e)
                Toast.makeText(context, "Could not attach one of the selected files", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun launchCamera() {
        val authority = "${context.packageName}.fileprovider"
        val photoFile = java.io.File(context.cacheDir, "photos").apply { mkdirs() }
        val file = java.io.File(photoFile, "img_${System.currentTimeMillis()}.jpg")
        val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
        cameraUri = uri
        cameraLauncher.launch(uri)
    }

    // Animated border color for focus
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) AccentBlue.copy(alpha = 0.35f) else BorderColor.copy(alpha = 0.12f),
        animationSpec = tween(AssistantMotion.COLOR_TWEEN),
        label = "borderColor"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        // ── Slash Command Menu ──
        AnimatedVisibility(
            visible = showCommands,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(AssistantMotion.PANEL_ENTER, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(AssistantMotion.PANEL_FADE_IN)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(AssistantMotion.PANEL_EXIT, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(AssistantMotion.PANEL_FADE_OUT)),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Surface(
                color = ChatSurface.copy(alpha = 0.95f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        "QUICK COMMANDS",
                        color = Silver.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 8.dp)
                    )
                    
                    val commands = listOf(
                        Triple(Icons.Default.Code, "/solve") { 
                            viewModel.onInputChanged("Solve this problem and output ONLY the raw code.")
                            launchCamera() // Immediately open the camera for taking problem photo
                        },
                        Triple(Icons.Default.AutoAwesome, "/system") { showSystemModal = true; viewModel.onInputChanged("") },
                        Triple(Icons.Default.Download, "/download") { viewModel.downloadModel(); viewModel.onInputChanged("") },
                        Triple(Icons.AutoMirrored.Filled.Help, "/help") {
                            viewModel.onInputChanged(
                                """
                                Assistant Quick Help

                                Commands:
                                /solve - Capture a problem and generate code-only output.
                                /system - Edit the assistant's system prompt.
                                /download - Download selected local model.
                                /clear - Clear current conversation.

                                Tips:
                                - Use the right tools panel to access Prompt Library, Hardware Monitor, and Macro Genie.
                                - Attach images/documents with the + button for multimodal prompts.
                                - Enable AUTO PUSH TO MAC to auto-send AI replies to your Mac.
                                """.trimIndent()
                            )
                        },
                        Triple(Icons.Default.DeleteSweep, "/clear") { viewModel.clearMessages(); viewModel.onInputChanged("") }
                    )
                    
                    commands.forEach { (icon, cmd, action) ->
                        Surface(
                            onClick = action,
                            color = Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(cmd, color = Platinum, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
        
        // ── Premium Capsule Input ──
        Surface(
            color = InputBarGlass,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                // Attached Media Toolbar
                AnimatedVisibility(
                    visible = attachedImages.isNotEmpty(),
                    enter = expandVertically(animationSpec = tween(AssistantMotion.PANEL_ENTER)) + fadeIn(animationSpec = tween(AssistantMotion.PANEL_FADE_IN)),
                    exit = shrinkVertically(animationSpec = tween(AssistantMotion.PANEL_EXIT)) + fadeOut(animationSpec = tween(AssistantMotion.PANEL_FADE_OUT))
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(attachedImages) { imagePair ->
                            Box {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.2f))
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(imagePair.first).crossfade(true).build(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                // Remove badge
                                Surface(
                                    onClick = { viewModel.removeImage(imagePair.first) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 6.dp, y = (-6).dp)
                                        .size(20.dp),
                                    shape = CircleShape,
                                    color = AccentBlue.copy(alpha = 0.9f)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Attach button
                    IconButton(
                        onClick = { showAttachmentSheet = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Attach",
                            tint = AiOrbGlow.copy(alpha = 0.8f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Text input
                    TextField(
                        value = input,
                        onValueChange = {
                            if (it.length <= maxChars) viewModel.onInputChanged(it)
                            isFocused = true
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                "Message Hackie AI…",
                                color = Silver.copy(alpha = 0.35f),
                                fontSize = 15.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = AiOrbGlow,
                            focusedTextColor = Platinum,
                            unfocusedTextColor = Platinum
                        ),
                        maxLines = 6,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (!isLoading && input.isNotBlank()) {
                                handleSend(viewModel, settingsProvider, context, prefs)
                            }
                        })
                    )

                    // Send / Voice area
                    Box(modifier = Modifier.padding(end = 4.dp)) {
                        // Send button (animated scale in/out)
                        if (sendButtonScale > 0.01f) {
                            IconButton(
                                onClick = { handleSend(viewModel, settingsProvider, context, prefs) },
                                modifier = Modifier
                                    .graphicsLayer(scaleX = sendButtonScale, scaleY = sendButtonScale)
                                    .background(
                                        Brush.linearGradient(listOf(AccentBlue, AccentBlue.copy(alpha = 0.7f))),
                                        CircleShape
                                    )
                                    .size(38.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowUpward,
                                    contentDescription = "Send",
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Voice button (when idle)
                        if (input.isBlank() && !isLoading && sendButtonScale < 0.01f) {
                            val voiceState by viewModel.voiceState.collectAsState()
                            val voiceResult by viewModel.voiceResult.collectAsState()
                            
                            if (voiceResult.isNotBlank() && voiceState == VoiceState.SUCCESS) {
                                viewModel.onInputChanged(voiceResult)
                                viewModel.resetVoiceState()
                            }

                            PulsingVoiceButton(
                                state = voiceState,
                                onClick = { 
                                    if (voiceState == VoiceState.LISTENING) viewModel.stopVoiceRecognition()
                                    else viewModel.startVoiceRecognition()
                                }
                            )
                        }

                        // Loading spinner
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = AiOrbGlow
                            )
                        }
                    }
                }

                // Character count (subtle, when typing)
                AnimatedVisibility(
                    visible = input.length > 100,
                    enter = fadeIn(animationSpec = tween(AssistantMotion.PANEL_FADE_IN)) + expandVertically(animationSpec = tween(AssistantMotion.PANEL_ENTER)),
                    exit = fadeOut(animationSpec = tween(AssistantMotion.PANEL_FADE_OUT)) + shrinkVertically(animationSpec = tween(AssistantMotion.PANEL_EXIT))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            "${input.length}/$maxChars",
                            color = if (input.length > maxChars - 200) AccentBlue.copy(alpha = 0.65f)
                            else Silver.copy(alpha = 0.25f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // ── Bottom Sheet for Attachments ──
        if (showAttachmentSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAttachmentSheet = false },
                containerColor = ChatSurface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = 8.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .background(Silver.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, start = 20.dp, end = 20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Attach Content",
                                color = Platinum,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Share image or document context",
                                color = Silver.copy(alpha = 0.55f),
                                fontSize = 12.sp
                            )
                        }
                        IconButton(onClick = { showAttachmentSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close attachments", tint = Silver)
                        }
                    }

                    AttachmentOption(
                        icon = Icons.Default.CameraAlt,
                        label = "Camera",
                        description = "Take a photo",
                        iconColor = AccentBlue,
                        onClick = {
                            showAttachmentSheet = false
                            launchCamera()
                        }
                    )
                    AttachmentOption(
                        icon = Icons.Default.PhotoLibrary,
                        label = "Gallery",
                        description = "Choose from photos",
                        iconColor = AccentTeal,
                        onClick = {
                            showAttachmentSheet = false
                            imagePickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                    )
                    AttachmentOption(
                        icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                        label = "Documents",
                        description = "Attach text files",
                        iconColor = AccentBlue,
                        onClick = {
                            showAttachmentSheet = false
                            filePickerLauncher.launch(arrayOf("*/*"))
                        }
                    )
                }
            }
        }
        
        if (showSystemModal) {
            SystemPromptModal(viewModel = viewModel, onDismiss = { showSystemModal = false })
        }
    }
}

@Composable
private fun AttachmentOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Graphite.copy(alpha = 0.4f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.32f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(label, color = Platinum, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(description, color = Silver.copy(alpha = 0.5f), fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Silver.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun handleSend(
    viewModel: AssistantViewModel,
    settingsProvider: AssistantSettingsProvider,
    context: android.content.Context,
    prefs: android.content.SharedPreferences
) {
    val currentModel = prefs.getString("selected_model", "gemini-pro-latest") ?: "gemini-pro-latest"
    viewModel.sendPrompt(
        apiKey = settingsProvider.getApiKey() ?: "",
        temperature = viewModel.temperature.value,
        maxTokens = 1024,
        model = currentModel
    )
}
