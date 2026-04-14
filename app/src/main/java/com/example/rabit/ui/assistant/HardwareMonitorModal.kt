package com.example.rabit.ui.assistant

import android.app.ActivityManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HardwareMonitorModal(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var memInfo by remember { mutableStateOf(getMemoryInfo(context)) }
    
    // Auto-update memory info every 2 seconds
    LaunchedEffect(Unit) {
        while (true) {
            memInfo = getMemoryInfo(context)
            delay(2000)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ChatSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Silver.copy(alpha = 0.2f)) },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 20.dp, end = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                        modifier = Modifier.size(36.dp).background(AccentBlue.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = AccentBlue)
                }
                Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Hardware Monitor", color = Platinum, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Live device stats", color = Silver.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Silver)
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Graphite.copy(alpha = 0.5f),
                border = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("SYSTEM MEMORY (RAM)", color = Silver.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val usagePercent = (memInfo.usedMem.toFloat() / memInfo.totalMem.toFloat())
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${memInfo.usedMem} MB Used", color = Platinum, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("${memInfo.totalMem} MB Total", color = Silver, fontSize = 14.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { usagePercent },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = AccentBlue,
                        trackColor = Color.Black.copy(alpha = 0.3f)
                    )
                    
                    if (usagePercent > 0.85f) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "High memory usage. Local models may run slowly or crash.",
                            color = Silver,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Additional basic specs could go here
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Graphite.copy(alpha = 0.5f),
                border = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = Silver, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("CPU Architecture", color = Silver.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(System.getProperty("os.arch") ?: "Unknown", color = Platinum, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text("CPU Cores", color = Silver.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("${Runtime.getRuntime().availableProcessors()}", color = Platinum, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

private data class MemInfo(val totalMem: Long, val availMem: Long, val usedMem: Long)

private fun getMemoryInfo(context: Context): MemInfo {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)
    
    val totalMb = memoryInfo.totalMem / (1024 * 1024)
    val availMb = memoryInfo.availMem / (1024 * 1024)
    val usedMb = totalMb - availMb
    
    return MemInfo(totalMb, availMb, usedMb)
}
