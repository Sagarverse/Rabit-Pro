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
import androidx.compose.ui.graphics.Color
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
                Text("Clear", color = Silver)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        LazyColumn(
            modifier = Modifier.heightIn(max = 240.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(history) { (prompt, response) ->
                // User Bubble (Right)
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    Surface(
                        color = AccentBlue,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                            topStart = 16.dp, topEnd = 16.dp,
                            bottomStart = 16.dp, bottomEnd = 4.dp
                        ),
                        modifier = Modifier.padding(start = 40.dp)
                    ) {
                        Text(
                            text = prompt,
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))

                // Gemini Bubble (Left)
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    Surface(
                        color = Obsidian,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                            topStart = 16.dp, topEnd = 16.dp,
                            bottomStart = 4.dp, bottomEnd = 16.dp
                        ),
                        modifier = Modifier.padding(end = 20.dp)
                    ) {
                        com.example.rabit.ui.components.MarkdownText(
                            text = response,
                            color = Platinum,
                            fontSize = 14f,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}
