package com.example.rabit.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*

/**
 * FileHubSection - P2P file management utility.
 * 
 * Allows users to browse and interact with files on a connected workstation 
 * through a secure binary protocol.
 */
@Composable
fun FileHubSection(
    viewModel: MainViewModel,
    onNavigateToSnippets: () -> Unit,
    onNavigateToAutomation: () -> Unit,
    onNavigateToWebBridge: () -> Unit
) {
    val context = LocalContext.current
    val remoteFiles by viewModel.remoteFiles.collectAsState(initial = emptyList())
    val isRemoteLoading by viewModel.isRemoteLoading.collectAsState(initial = false)
    val currentRemotePath by viewModel.currentRemotePath.collectAsState(initial = "/")
    val p2pStatus by viewModel.p2pStatus.collectAsState(initial = "Disconnected")

    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp, max = 500.dp),
        color = SoftGrey.copy(alpha = 0.5f),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Graphite.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text("How to connect Hub", color = Platinum, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("1. Open Web Bridge and enable P2P.", color = Silver, fontSize = 11.sp)
                    Text("2. Connect your computer to the bridge page and authenticate.", color = Silver, fontSize = 11.sp)
                    Text("3. Come back here and tap refresh to browse files.", color = Silver, fontSize = 11.sp)
                    if (!p2pStatus.contains("Connected")) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onNavigateToWebBridge) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Web Bridge Setup", color = AccentBlue)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Header & Breadcrumbs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("REMOTE FILE HUB", color = Silver, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Text(currentRemotePath, color = Platinum.copy(alpha=0.5f), fontSize = 11.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (currentRemotePath != "/") {
                        IconButton(
                            onClick = { 
                                val parent = currentRemotePath.substringBeforeLast("/").ifEmpty { "/" }
                                viewModel.fetchRemoteFiles(parent) 
                            },
                            modifier = Modifier.size(32.dp).background(Obsidian.copy(alpha=0.5f), CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Silver, modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(
                        onClick = { viewModel.fetchRemoteFiles(currentRemotePath) },
                        modifier = Modifier.size(32.dp).background(Obsidian.copy(alpha=0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = AccentBlue, modifier = Modifier.size(16.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            if (isRemoteLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                }
            } else if (!p2pStatus.contains("Connected")) {
                 Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Connect P2P to browse workstation", color = Silver.copy(alpha=0.4f), fontSize = 13.sp)
                }
            } else if (remoteFiles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { viewModel.fetchRemoteFiles("/") }) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Silver.copy(alpha=0.2f), modifier = Modifier.size(48.dp))
                        }
                        Text("No files found or Hub idle", color = Silver.copy(alpha=0.4f), fontSize = 13.sp)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(remoteFiles.size) { index ->
                        val file = remoteFiles[index]
                        RemoteFileCard(file = file) {
                            if (file.isFolder) {
                                viewModel.fetchRemoteFiles(file.path)
                            } else {
                                android.widget.Toast.makeText(context, "Ready to stream: ${file.name}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Quick Links Row (Relocated for better accessibility)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallActionCard(modifier = Modifier.weight(1f), title = "SNIPPETS", icon = Icons.AutoMirrored.Filled.TextSnippet, accent = AccentGold) { onNavigateToSnippets() }
                SmallActionCard(modifier = Modifier.weight(1f), title = "AUTOMATION", icon = Icons.Default.Bolt, accent = AccentPurple) { onNavigateToAutomation() }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Web Bridge Quick Action
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToWebBridge() },
                color = SoftGrey.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(32.dp).background(AccentBlue.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("WEB SHARE", color = Platinum, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Manage file transfers & streaming", color = Silver, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
