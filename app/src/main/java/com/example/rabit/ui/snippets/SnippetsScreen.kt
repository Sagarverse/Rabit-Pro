package com.example.rabit.ui.snippets

import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*
import com.example.rabit.ui.components.*
import org.json.JSONArray
import org.json.JSONObject

data class TextSnippet(val name: String, val content: String, val category: String = "General")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE) }

    var snippets by remember { mutableStateOf(loadSnippets(prefs)) }
    var showAddDialog by remember { mutableStateOf(false) }
    var expandedSnippet by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val normalizedQuery = searchQuery.trim().lowercase()
    val filteredSnippets = remember(snippets, normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            snippets
        } else {
            snippets.filter {
                it.name.lowercase().contains(normalizedQuery) ||
                    it.content.lowercase().contains(normalizedQuery) ||
                    it.category.lowercase().contains(normalizedQuery)
            }
        }
    }

    Scaffold(
        containerColor = Obsidian,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AccentGold,
                contentColor = Obsidian
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Snippet")
            }
        }
    ) { padding ->
        if (snippets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.AutoMirrored.Filled.TextSnippet,
                        contentDescription = null,
                        tint = Silver.copy(alpha = 0.2f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No snippets yet", color = Silver.copy(alpha = 0.5f), fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Tap + to save frequently used text", color = Silver.copy(alpha = 0.3f), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(24.dp))

                    // Suggestion chips
                    Text("SUGGESTIONS", color = Silver.copy(alpha = 0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    val suggestions = listOf(
                        "Email Signature" to "Best regards,\n[Your Name]\n[Your Title]",
                        "Git Commit" to "git add . && git commit -m \"\" && git push",
                        "Meeting Response" to "Thank you for the invite. I'll be there.",
                    )
                    suggestions.forEach { (name, content) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .padding(vertical = 4.dp)
                                .clickable {
                                    snippets = snippets + TextSnippet(name, content)
                                    saveSnippets(prefs, snippets)
                                },
                            color = AccentBlue.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, AccentBlue.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(name, color = AccentBlue, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                item("snippets_stats") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = AccentGold.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, AccentGold.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Saved snippets", color = Silver, fontSize = 12.sp)
                            Text(snippets.size.toString(), color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
                item("snippets_search") {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        label = { Text("Search snippet name or content") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Platinum,
                            unfocusedTextColor = Platinum
                        )
                    )
                }
                if (searchQuery.isNotBlank()) {
                    item("snippets_results") {
                        Text(
                            "${filteredSnippets.size} result(s) for '$searchQuery'",
                            color = Silver,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                if (filteredSnippets.isEmpty()) {
                    item("snippets_empty_filtered") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Graphite.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                if (searchQuery.isBlank()) "No snippets available" else "No snippets found for '$searchQuery'",
                                color = Silver,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
                items(filteredSnippets) { snippet ->
                    SnippetCard(
                        snippet = snippet,
                        isExpanded = expandedSnippet == snippet.name,
                        onExpandToggle = {
                            expandedSnippet = if (expandedSnippet == snippet.name) null else snippet.name
                        },
                        onPush = { viewModel.sendText(snippet.content) },
                        onDelete = {
                            snippets = snippets.filterNot { it.name == snippet.name }
                            saveSnippets(prefs, snippets)
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Graphite,
            title = { Text("New Snippet", color = Platinum) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Snippet Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Platinum
                        )
                    )
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Content") },
                        minLines = 3,
                        maxLines = 8,
                        placeholder = { Text("Paste or type your snippet text…") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Platinum
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && content.isNotBlank()) {
                            snippets = snippets + TextSnippet(name, content)
                            saveSnippets(prefs, snippets)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                ) { Text("Save", color = Obsidian, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = Silver) }
            }
        )
    }
}

@Composable
fun SnippetCard(
    snippet: TextSnippet,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onPush: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { onExpandToggle() },
        color = Graphite,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        Icons.AutoMirrored.Filled.TextSnippet,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(snippet.name, color = Platinum, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        if (!isExpanded) {
                            Text(
                                snippet.content.take(60).replace("\n", " "),
                                color = Silver,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Silver.copy(alpha = 0.5f)
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Obsidian,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        snippet.content,
                        color = Platinum.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = ErrorRed, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onPush,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Push to Mac", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun loadSnippets(prefs: android.content.SharedPreferences): List<TextSnippet> {
    val json = prefs.getString("text_snippets_json", null) ?: return emptyList()
    return try {
        val array = JSONArray(json)
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            TextSnippet(obj.getString("name"), obj.getString("content"), obj.optString("category", "General"))
        }
    } catch (e: Exception) { emptyList() }
}

private fun saveSnippets(prefs: android.content.SharedPreferences, snippets: List<TextSnippet>) {
    val array = JSONArray()
    snippets.forEach { s ->
        array.put(JSONObject().apply {
            put("name", s.name)
            put("content", s.content)
            put("category", s.category)
        })
    }
    prefs.edit().putString("text_snippets_json", array.toString()).apply()
}
