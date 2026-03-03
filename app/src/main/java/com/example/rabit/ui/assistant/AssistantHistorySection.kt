package com.example.rabit.ui.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.theme.*

@Composable
fun AssistantHistorySection() {
    val history = AssistantHistoryStore.history
    if (history.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Graphite, shape = MaterialTheme.shapes.medium)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, contentDescription = "History", tint = AccentBlue)
            Spacer(modifier = Modifier.width(8.dp))
            Text("History", color = Silver, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { AssistantHistoryStore.clear() }) {
                Text("Clear", color = ErrorRed)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
            items(history) { (prompt, response) ->
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Text("You: $prompt", color = Platinum, fontSize = 14.sp)
                    Text("Gemini: $response", color = Silver, fontSize = 13.sp)
                }
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            }
        }
    }
}
