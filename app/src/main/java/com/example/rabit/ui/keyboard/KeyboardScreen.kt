package com.example.rabit.ui.keyboard

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.domain.model.HidKeyCodes
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun KeyboardScreen(
    viewModel: MainViewModel, 
    onDisconnect: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAssistant: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 5 })
    val scope = rememberCoroutineScope()

    LaunchedEffect(connectionState) {
        if (connectionState is HidDeviceManager.ConnectionState.Disconnected) {
            onDisconnect()
        }
    }

    Scaffold(
        bottomBar = {
            PremiumBottomBar(pagerState.currentPage, onNavigateToAssistant) { index ->
                scope.launch { pagerState.animateScrollToPage(index) }
            }
        },
        containerColor = Obsidian
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            PremiumHeader(connectionState, onNavigateToSettings, viewModel::disconnect)

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = true,
                contentPadding = PaddingValues(bottom = 16.dp)
            ) { page ->
                when (page) {
                    0 -> DualKeyboardTab(viewModel)
                    1 -> TrackpadTab(viewModel)
                    2 -> RemoteTextTab(viewModel)
                    3 -> FilesTab(viewModel)
                    4 -> AdvancedTab(viewModel, onNavigateToSettings)
                }
            }
        }
    }
}

@Composable
fun TrackpadTab(viewModel: MainViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("TRACKPAD", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(32.dp))
                    .background(Graphite)
                    .border(1.dp, BorderColor, RoundedCornerShape(32.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                viewModel.sendMouseMove(
                                    dx = dragAmount.x,
                                    dy = dragAmount.y
                                )
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
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = Silver.copy(alpha = 0.2f), modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Swipe to move • Tap to click", color = Silver.copy(alpha = 0.4f), fontSize = 12.sp)
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SoftGrey)
                    .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (Math.abs(dragAmount.y) > 1) {
                                    viewModel.sendMouseMove(0f, 0f, wheel = if (dragAmount.y > 0) -1 else 1)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.UnfoldMore, contentDescription = null, tint = Silver.copy(alpha = 0.3f))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().height(80.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MouseButton(
                modifier = Modifier.weight(1f),
                text = "LEFT",
                onClick = { 
                    viewModel.sendMouseMove(0f, 0f, buttons = 1)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ viewModel.sendMouseMove(0f, 0f, buttons = 0) }, 50)
                }
            )
            MouseButton(
                modifier = Modifier.weight(0.6f),
                text = "MID",
                onClick = { 
                    viewModel.sendMouseMove(0f, 0f, buttons = 4)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ viewModel.sendMouseMove(0f, 0f, buttons = 0) }, 50)
                }
            )
            MouseButton(
                modifier = Modifier.weight(1f),
                text = "RIGHT",
                onClick = { 
                    viewModel.sendMouseMove(0f, 0f, buttons = 2)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ viewModel.sendMouseMove(0f, 0f, buttons = 0) }, 50)
                }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun MouseButton(modifier: Modifier, text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SoftGrey),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text, color = Platinum, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FilesTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    
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

    Column(modifier = Modifier.fillMaxSize()) {
        Text("FILES", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(24.dp))
        
        Surface(
            color = Graphite,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().weight(1f),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    if (selectedFileUri == null) Icons.Default.CloudUpload else Icons.Default.InsertDriveFile, 
                    contentDescription = null, 
                    tint = if (selectedFileUri == null) AccentBlue else SuccessGreen, 
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    if (selectedFileUri == null) "Remote File Sharing" else fileName, 
                    color = Platinum, 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (selectedFileUri == null) "Pick a text file to type its contents to your Mac." else "Ready to type file contents via Bluetooth.",
                    color = Silver,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { filePicker.launch("text/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = SoftGrey),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.height(56.dp).weight(1f)
                    ) {
                        Text(if (selectedFileUri == null) "Select File" else "Change")
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
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            shape = RoundedCornerShape(28.dp),
                            modifier = Modifier.height(56.dp).weight(1f)
                        ) {
                            Text("Type File")
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun PremiumBottomBar(selectedTab: Int, onNavigateToAssistant: () -> Unit, onTabSelected: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp, start = 12.dp, end = 12.dp)
            .height(64.dp)
            .background(Graphite, RoundedCornerShape(32.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(32.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                Icons.Default.Keyboard to 0,
                Icons.Default.Mouse to 1,
                Icons.Default.Send to 2,
                Icons.Default.Folder to 3,
                Icons.Default.Tune to 4
            )
            
            tabs.forEach { (icon, index) ->
                IconButton(onClick = { onTabSelected(index) }) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (selectedTab == index) AccentBlue else Silver.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            VerticalDivider(modifier = Modifier.height(24.dp), thickness = 1.dp, color = BorderColor)
            
            IconButton(onClick = onNavigateToAssistant) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = "AI Assistant",
                    tint = AccentGold,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun PremiumHeader(connectionState: HidDeviceManager.ConnectionState, onNavigateToSettings: () -> Unit, onDisconnect: () -> Unit) {
    val deviceName = (connectionState as? HidDeviceManager.ConnectionState.Connected)?.deviceName ?: "OFFLINE"
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "RABIT", 
                color = Platinum, 
                fontSize = 24.sp, 
                fontWeight = FontWeight.Light, 
                letterSpacing = 4.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).background(if (deviceName != "OFFLINE") SuccessGreen else ErrorRed, CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text(deviceName.uppercase(), color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .size(44.dp)
                    .background(SoftGrey, CircleShape)
                    .border(1.dp, BorderColor, CircleShape)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = Platinum, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(
                onClick = onDisconnect,
                modifier = Modifier
                    .size(44.dp)
                    .background(SoftGrey, CircleShape)
                    .border(1.dp, BorderColor, CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Platinum, modifier = Modifier.size(16.dp))
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
            listOf("CUSTOM", "SYSTEM").forEach { label ->
                val active = (label == "SYSTEM" && isSystemMode) || (label == "CUSTOM" && !isSystemMode)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (active) Graphite else Color.Transparent)
                        .clickable { isSystemMode = label == "SYSTEM" },
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

        Spacer(modifier = Modifier.height(32.dp))

        if (isSystemMode) {
            MinimalSystemInput(viewModel)
        } else {
            val activeModifiers by viewModel.activeModifiers.collectAsState()
            
            PremiumKeyboardLayout(
                activeModifiers = activeModifiers,
                onModifierClick = { mod ->
                    viewModel.toggleModifier(mod)
                },
                onKeyPress = { code -> 
                    viewModel.sendKey(code)
                }
            )
        }
    }
}

@Composable
fun MinimalSystemInput(viewModel: MainViewModel) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    
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
            modifier = Modifier.fillMaxWidth().height(140.dp),
            placeholder = { Text("Native Keyboard Input...", color = Silver.copy(alpha = 0.3f)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = BorderColor,
                focusedTextColor = Platinum
            ),
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )
        
        TextButton(onClick = { textFieldValue = TextFieldValue("") }, modifier = Modifier.padding(16.dp)) {
            Text("RESET", color = Silver, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
    }
}

@Composable
fun AdvancedTab(viewModel: MainViewModel, onNavigateToSettings: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Graphite,
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("MEDIA CONTROLS", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    IconButton(onClick = onNavigateToSettings, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = Silver, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MediaIcon(Icons.Default.VolumeDown) { viewModel.sendConsumerKey(HidKeyCodes.MEDIA_VOL_DOWN) }
                    MediaIcon(Icons.Default.PlayArrow) { viewModel.sendConsumerKey(HidKeyCodes.MEDIA_PLAY_PAUSE) }
                    MediaIcon(Icons.Default.VolumeUp) { viewModel.sendConsumerKey(HidKeyCodes.MEDIA_VOL_UP) }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SmallActionCard(
                modifier = Modifier.weight(1f),
                title = "UNLOCK",
                icon = Icons.Default.LockOpen,
                accent = AccentBlue
            ) { viewModel.unlockMac() }

            SmallActionCard(
                modifier = Modifier.weight(1f),
                title = "SETTINGS",
                icon = Icons.Default.Settings,
                accent = SuccessGreen
            ) { onNavigateToSettings() }
        }
        
        SmallActionCard(
            modifier = Modifier.fillMaxWidth(),
            title = "DISCONNECT",
            icon = Icons.Default.Refresh,
            accent = ErrorRed
        ) { viewModel.disconnect() }
    }
}

@Composable
fun MediaIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(56.dp).background(SoftGrey, CircleShape).border(1.dp, BorderColor, CircleShape)
    ) {
        Icon(icon, contentDescription = null, tint = Platinum, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun SmallActionCard(modifier: Modifier, title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(110.dp).clickable { onClick() },
        color = Graphite,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, color = Platinum, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun RemoteTextTab(viewModel: MainViewModel) {
    var textInput by remember { mutableStateOf("") }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("BATCH SENDER", color = Silver, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            modifier = Modifier.fillMaxWidth().height(280.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = BorderColor,
                focusedTextColor = Platinum
            ),
            shape = RoundedCornerShape(24.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { viewModel.sendText(textInput); textInput = "" },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text("SEND", fontWeight = FontWeight.Bold, color = Obsidian, letterSpacing = 2.sp)
        }
    }
}

@Composable
fun PremiumKeyboardLayout(
    activeModifiers: Byte,
    onModifierClick: (Byte) -> Unit,
    onKeyPress: (Byte) -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
        listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
        listOf("Z", "X", "C", "V", "B", "N", "M", "Bksp"),
        listOf("Ctrl", "Opt", "Cmd", "Space", "Enter")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
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
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPressed) SoftGrey else KeyBackground)
            .border(1.dp, if (isPressed) accent.copy(alpha = 0.5f) else BorderColor, RoundedCornerShape(12.dp))
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
