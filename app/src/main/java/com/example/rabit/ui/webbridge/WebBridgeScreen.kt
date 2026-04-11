package com.example.rabit.ui.webbridge

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.OpenableColumns
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import com.example.rabit.R
import com.example.rabit.data.network.RabitNetworkServer
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.components.QrCodeGenerator
import com.example.rabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebBridgeScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val isRunning by viewModel.isWebBridgeRunning.collectAsState(initial = RabitNetworkServer.isRunning)
    val currentPin by viewModel.webBridgePin.collectAsState(initial = RabitNetworkServer.currentPin)
    val localIp by viewModel.localIp.collectAsState(initial = "0.0.0.0")
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val sharedFiles by viewModel.sharedFiles.collectAsState()
    val isMono = AppThemeMode.isMonochrome

    // ActivityResultLauncher for picking files to share
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { viewModel.addSharedFile(it) }
    }

    // Connect Server Providers
    SideEffect {
        RabitNetworkServer.sharedFilesProvider = {
            sharedFiles.map { uri ->
                var name = "unknown"
                var size = 0L
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx != -1) name = cursor.getString(nameIdx)
                        if (sizeIdx != -1) size = cursor.getLong(sizeIdx)
                    }
                }
                RabitNetworkServer.SharedFile(
                    id = uri.toString().hashCode().toString(),
                    name = name,
                    size = size,
                    type = context.contentResolver.getType(uri) ?: "application/octet-stream"
                )
            }
        }
        
        RabitNetworkServer.fileDownloadProvider = { id ->
            sharedFiles.find { it.toString().hashCode().toString() == id }
        }
    }

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.rabit_logo),
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Web Bridge", color = Platinum, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            Text("Pro Hub", color = if(isMono) Silver else AccentBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Platinum)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Obsidian),
                actions = {
                    // Monochrome Mode Toggle
                    IconButton(onClick = { AppThemeMode.isMonochrome = !AppThemeMode.isMonochrome }) {
                        Icon(
                            if (isMono) Icons.Default.InvertColorsOff else Icons.Default.InvertColors,
                            contentDescription = "Toggle B&W Mode",
                            tint = if (isMono) Silver else AccentBlue
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Glassmorphism Status Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                color = Color.Transparent,
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(1.dp, Brush.verticalGradient(listOf(Platinum.copy(alpha = 0.2f), Color.Transparent)))
            ) {
                Box(modifier = Modifier.background(DarkGlassGradient).padding(24.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isRunning) {
                                Surface(
                                    modifier = Modifier.size(64.dp),
                                    color = SuccessGreen.copy(alpha = 0.1f),
                                    shape = CircleShape,
                                    border = BorderStroke(2.dp, SuccessGreen.copy(alpha = 0.5f))
                                ) {}
                            }
                            Icon(
                                if (isRunning) Icons.Default.WifiTethering else Icons.Default.WifiTetheringOff,
                                contentDescription = null,
                                tint = if (isRunning) SuccessGreen else Silver,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            if (isRunning) "HUB ACTIVE" else "HUB STANDBY",
                            color = Platinum,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        
                        Text(
                            if (isRunning) "Serving control interface locally" else "Bridge is currently offline",
                            color = Silver,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(28.dp))
                        
                        Button(
                            onClick = { if (isRunning) viewModel.stopWebBridge() else viewModel.startWebBridge() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRunning) ErrorRed.copy(alpha = 0.8f) else SuccessGreen.copy(alpha = 0.8f)
                            ),
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            shape = RoundedCornerShape(20.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Text(
                                if (isRunning) "TERMINATE BRIDGE" else "IGNITE BRIDGE", 
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            if (isRunning) {
                // Connection Info Section
                val serverUrl = if (localIp.isNotEmpty() && localIp != "0.0.0.0") 
                    "http://$localIp:8765" else "Identifying network..."
                
                // Passcode Card (Premium Glass)
                Text("SECURITY PASSCODE", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Graphite,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, if(isMono) Silver.copy(alpha = 0.3f) else AccentBlue.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 40.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            currentPin,
                            color = if(isMono) Platinum else AccentBlue,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 8.sp
                        )
                        TextButton(onClick = { viewModel.regenerateWebBridgePin() }) {
                            Text("REGENERATE SECURE KEY", color = if(isMono) Silver else AccentBlue.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // QR Code Section (Tactile Card)
                Surface(
                    modifier = Modifier.size(240.dp),
                    color = Color.White,
                    shape = RoundedCornerShape(32.dp),
                    border = BorderStroke(8.dp, Graphite)
                ) {
                    Box(modifier = Modifier.padding(24.dp)) {
                        if (localIp.isNotEmpty() && localIp != "0.0.0.0") {
                            val qrBitmap = remember(serverUrl) {
                                QrCodeGenerator.generateQrCode(serverUrl, 512)?.asImageBitmap()
                            }
                            if (qrBitmap != null) {
                                Image(bitmap = qrBitmap, contentDescription = "QR Code", modifier = Modifier.fillMaxSize())
                            }
                        } else {
                            CircularProgressIndicator(color = AccentBlue, modifier = Modifier.align(Alignment.Center))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    onClick = {
                        if (localIp.isNotEmpty() && localIp != "0.0.0.0") {
                            clipboardManager.setText(AnnotatedString(serverUrl))
                            android.widget.Toast.makeText(context, "Link Copied!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    color = SoftGrey,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Link, null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(serverUrl, color = Platinum, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Internet P2P Hosting (Premium Card)
                val p2pEnabled by viewModel.p2pEnabled.collectAsState("false".toBoolean())
                val peerId by viewModel.p2pPeerId.collectAsState(null)
                val p2pStatus by viewModel.p2pStatus.collectAsState("Disconnected")

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Transparent,
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, if (p2pEnabled) AccentBlue.copy(alpha = 0.3f) else BorderColor)
                ) {
                    Column(modifier = Modifier.background(if(p2pEnabled) AccentBlue.copy(alpha = 0.03f) else Color.Transparent).padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Public, null, tint = if (p2pEnabled) AccentBlue else Silver)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("P2P Global Bridge", color = Platinum, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                Text("Secure access via internet", color = Silver, fontSize = 11.sp)
                            }
                            Switch(
                                checked = p2pEnabled,
                                onCheckedChange = { if (it) viewModel.startP2PHosting() else viewModel.stopP2PHosting() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Platinum,
                                    checkedTrackColor = AccentBlue,
                                    uncheckedTrackColor = Graphite
                                )
                            )
                        }

                        if (p2pEnabled) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Surface(color = Obsidian, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text("PEER ID", color = Silver, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                                        Box(
                                            modifier = Modifier
                                                .background(if (p2pStatus == "P2P Connected") SuccessGreen.copy(alpha = 0.1f) else Silver.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(p2pStatus.uppercase(), color = if (p2pStatus == "P2P Connected") SuccessGreen else Silver, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                    Text(peerId ?: "GENERATING...", color = if(isMono) Platinum else AccentBlue, fontSize = 28.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                // Phone Sync Hub (Modern List)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SoftGrey.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudSync, null, tint = AccentBlue)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Phone Sync Hub", color = Platinum, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                    Text("${sharedFiles.size} items ready for Mac", color = Silver, fontSize = 11.sp)
                                }
                            }
                            IconButton(
                                onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                                modifier = Modifier.background(AccentBlue.copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(Icons.Default.Add, null, tint = AccentBlue)
                            }
                        }

                        if (sharedFiles.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            sharedFiles.forEach { uri ->
                                var fileName = "Encrypted Resource"
                                context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                    if (c.moveToFirst() && idx != -1) fileName = c.getString(idx)
                                }
                                
                                Surface(
                                    modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
                                    color = Graphite.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.InsertDriveFile, null, tint = Silver, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(fileName, color = Platinum, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        IconButton(onClick = { viewModel.removeSharedFile(uri) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Delete, null, tint = ErrorRed.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
            
            // Branding Footer
            Image(
                painter = painterResource(id = R.drawable.rabit_logo),
                contentDescription = null,
                modifier = Modifier.size(48.dp).alpha(0.3f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Rabit Pro File Sharing Hub\nEnd-to-End Local Infrastructure",
                color = Silver.copy(alpha = 0.6f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

