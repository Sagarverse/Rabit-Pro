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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.OpenableColumns
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import com.sagar.rabit.R
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
    val p2pEnabled by viewModel.p2pEnabled.collectAsState("false".toBoolean())
    val peerId by viewModel.p2pPeerId.collectAsState(null)
    val p2pStatus by viewModel.p2pStatus.collectAsState("Disconnected")
    val cloudUnavailable = p2pStatus.contains("Safe Mode") || p2pStatus.contains("Offline")
    val localUrl = if (localIp.isNotEmpty() && localIp != "0.0.0.0")
        "http://$localIp:${RabitNetworkServer.PORT}" else "Identifying network..."
    val gatewayBaseUrl = "https://hackie-260414-01.web.app"
    val p2pUrl = if (!peerId.isNullOrEmpty() && !cloudUnavailable) "$gatewayBaseUrl/?peer=$peerId" else gatewayBaseUrl

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

        // Universal Clipboard Integration
        val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        
        RabitNetworkServer.clipboardProvider = {
            clipboardManager.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
        }

        RabitNetworkServer.clipboardReceiver = { text ->
            if (text.isNotEmpty()) {
                val clip = android.content.ClipData.newPlainText("Hackie Universal", text)
                clipboardManager.setPrimaryClip(clip)
            }
        }
    }

    Scaffold(
        containerColor = Obsidian
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
                            if (isRunning) "Hackie Bridge is live on your network" else "Hackie Bridge is currently offline",
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
                                if (isRunning) "STOP HACKIE BRIDGE" else "START HACKIE BRIDGE", 
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }

                        if (isRunning) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (localIp.isNotEmpty() && localIp != "0.0.0.0") {
                                            clipboardManager.setText(AnnotatedString(localUrl))
                                            android.widget.Toast.makeText(context, "Local Link Copied!", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue),
                                    border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("COPY LINK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Platinum),
                                    border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ADD FILES", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            if (isRunning) {
                // Status indicator for P2P state
                if (cloudUnavailable) {
                    Surface(
                        color = ErrorRed.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, tint = ErrorRed)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Cloud mode unavailable", color = Platinum, fontWeight = FontWeight.Bold)
                                Text("Bridge is in local-only mode until cloud signaling is available.", color = Silver, fontSize = 11.sp)
                            }
                            Button(
                                onClick = { viewModel.startP2PHosting() },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                            ) {
                                Text("SETUP CLOUD")
                            }
                        }
                    }
                }
                
                // Instructions Card
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    color = AccentBlue.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("How to connect", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("1. Connect Mac/PC to the same Wi-Fi network.", color = Silver, fontSize = 12.sp)
                        Text("2. Type the link below into your web browser.", color = Silver, fontSize = 12.sp)
                        Text("3. Enter the 4-digit Security Passcode above to login.", color = Silver, fontSize = 12.sp)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    color = SoftGrey.copy(alpha = 0.36f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Bridge Links", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                        BridgeLinkRow(
                            title = "Local LAN (same Wi-Fi)",
                            url = localUrl,
                            hint = "Fastest and most reliable on home/office network.",
                            onCopy = {
                                if (localIp.isNotEmpty() && localIp != "0.0.0.0") {
                                    clipboardManager.setText(AnnotatedString(localUrl))
                                    android.widget.Toast.makeText(context, "Local link copied", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = localIp.isNotEmpty() && localIp != "0.0.0.0"
                        )

                        BridgeLinkRow(
                            title = "Public Cloud (internet)",
                            url = p2pUrl,
                            hint = if (cloudUnavailable) {
                                "Cloud signaling is unavailable now. Use local LAN link above."
                            } else {
                                "Use this when your Mac/PC is on a different network."
                            },
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(p2pUrl))
                                android.widget.Toast.makeText(context, "Public link copied", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            enabled = !cloudUnavailable
                        )
                    }
                }
                
                // Passcode Card (Premium Glass)
                Text("SECURITY PASSCODE", color = Silver, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Graphite,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Silver.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 40.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            currentPin,
                            color = Platinum,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 8.sp
                        )
                        TextButton(onClick = { viewModel.regenerateWebBridgePin() }) {
                            Text("REGENERATE SECURE KEY", color = Silver, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                            val qrBitmap = remember(localUrl) {
                                QrCodeGenerator.generateQrCode(localUrl, 512)?.asImageBitmap()
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
                
                Spacer(modifier = Modifier.height(24.dp))

                // Internet P2P Hosting (Premium Card)

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
                                Text("Hackie Global Bridge", color = Platinum, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                Text("Secure access via hackie-260414-01.web.app", color = Silver, fontSize = 11.sp)
                            }
                            Switch(
                                checked = p2pEnabled,
                                onCheckedChange = { if (it) viewModel.startP2PHosting() else viewModel.stopP2PHosting() },
                                enabled = !cloudUnavailable,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Platinum,
                                    checkedTrackColor = AccentBlue,
                                    uncheckedTrackColor = Graphite
                                )
                            )
                        }

                        if (cloudUnavailable) {
                            Text(
                                "P2P switch disabled while cloud is unavailable",
                                color = Silver.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        if (p2pEnabled) {
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            if (peerId == null) {
                                // Loading state for Peer ID synchronization
                                Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Synchronizing Secure Signaling...", color = Silver, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            } else {
                                // Internet Gateway Link Card
                                Surface(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(p2pUrl))
                                        android.widget.Toast.makeText(context, "Internet Gateway Copied!", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    color = Obsidian,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Silver.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Language, null, tint = Platinum, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("INTERNET GATEWAY URL", color = Silver, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            p2pUrl, 
                                            color = Platinum, 
                                            fontSize = 13.sp, 
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("TAP TO COPY PERSISTENT HUB LINK", color = Silver.copy(alpha = 0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = SoftGrey.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("DIAGNOSTICS", color = Silver, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        DiagnosticRow("Bluetooth", if (cloudUnavailable) "Cloud fallback ready" else "Ready")
                                        DiagnosticRow("Local IP", if (localIp == "0.0.0.0") "Unavailable" else localIp)
                                        DiagnosticRow("P2P", p2pStatus)
                                        DiagnosticRow("Permissions", "Granted for bridge features")
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Surface(color = Obsidian, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text("BRIDGE PEER ID", color = Silver, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                                        Box(
                                            modifier = Modifier
                                                .background(if (p2pStatus == "P2P Connected") SuccessGreen.copy(alpha = 0.1f) else Silver.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(p2pStatus.uppercase(), color = if (p2pStatus == "P2P Connected") SuccessGreen else Silver, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                    Text(peerId ?: "GENERATING...", color = Platinum, fontSize = 28.sp, fontWeight = FontWeight.Black)
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
                            IconButton(onClick = { filePickerLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.background(AccentBlue.copy(alpha = 0.1f), CircleShape)) {
                                Icon(Icons.Default.Add, null, tint = AccentBlue)
                            }
                        }

                        if (sharedFiles.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            sharedFiles.forEach { uri ->
                                var fileName = "Encrypted Resource"
                                var fileSize = 0L
                                context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                    val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
                                    if (c.moveToFirst()) {
                                        if (idx != -1) fileName = c.getString(idx)
                                        if (sizeIdx != -1) fileSize = c.getLong(sizeIdx)
                                    }
                                }
                                
                                Surface(
                                    modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
                                    color = Graphite.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.AutoMirrored.Filled.InsertDriveFile, null, tint = Silver, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(fileName, color = Platinum, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            if (fileSize > 0L) {
                                                Text(
                                                    String.format("%.1f KB", fileSize / 1024f),
                                                    color = Silver.copy(alpha = 0.65f),
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                        IconButton(onClick = { viewModel.removeSharedFile(uri) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Delete, null, tint = ErrorRed.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                color = Graphite.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("No files added yet", color = Silver, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Tap + to add files and make them instantly available to your browser.",
                                        color = Silver.copy(alpha = 0.7f),
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Silver, fontSize = 11.sp)
        Text(value, color = Platinum, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BridgeLinkRow(
    title: String,
    url: String,
    hint: String,
    onCopy: () -> Unit,
    enabled: Boolean
) {
    Surface(
        color = Graphite.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = Platinum, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onCopy, enabled = enabled) {
                    Text("Copy", color = if (enabled) AccentBlue else Silver)
                }
            }
            Text(
                url,
                color = if (enabled) Silver else Silver.copy(alpha = 0.7f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(hint, color = Silver.copy(alpha = 0.72f), fontSize = 10.sp)
        }
    }
}

