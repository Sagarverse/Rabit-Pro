package com.example.rabit.ui.assistant

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptLibraryModal(
    onDismiss: () -> Unit,
    onSelectPrompt: (String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("prompt_library", Context.MODE_PRIVATE) }
    
    var savedPrompts by remember {
        mutableStateOf(prefs.getStringSet("prompts", emptySet())?.toList() ?: emptyList())
    }
    
    var showAddDialog by remember { mutableStateOf(false) }

    fun refreshPrompts() {
        savedPrompts = prefs.getStringSet("prompts", emptySet())?.toList() ?: emptyList()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ChatSurface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Silver.copy(alpha = 0.2f))
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 20.dp, end = 20.dp)
                .heightIn(max = 500.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Prompt Library",
                        color = Platinum,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${savedPrompts.size} saved prompts",
                        color = Silver.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
                Row {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = AccentBlue)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Silver)
                    }
                }
            }

            if (savedPrompts.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Graphite.copy(alpha = 0.45f),
                    border = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.32f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No saved prompts yet.", color = Silver.copy(alpha = 0.55f))
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(savedPrompts.sortedBy { it.lowercase() }) { prompt ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Graphite.copy(alpha = 0.5f),
                            border = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.34f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectPrompt(prompt) }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    prompt,
                                    color = Platinum,
                                    fontSize = 13.sp,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                IconButton(
                                    onClick = {
                                        val curSet = prefs.getStringSet("prompts", emptySet())?.toMutableSet()
                                        curSet?.remove(prompt)
                                        prefs.edit().putStringSet("prompts", curSet).apply()
                                        refreshPrompts()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Silver.copy(alpha = 0.75f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var newPrompt by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Graphite,
            title = { Text("Save New Prompt", color = Platinum) },
            text = {
                OutlinedTextField(
                    value = newPrompt,
                    onValueChange = { newPrompt = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPrompt.isNotBlank()) {
                        val set = prefs.getStringSet("prompts", emptySet())?.toMutableSet() ?: mutableSetOf()
                        set.add(newPrompt.trim())
                        prefs.edit().putStringSet("prompts", set).apply()
                        refreshPrompts()
                    }
                    showAddDialog = false
                }) {
                    Text("Save", color = AccentBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = Silver)
                }
            }
        )
    }
}
