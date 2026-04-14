package com.example.rabit.ui.automation

import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class InjectorSavedPayload(
    val name: String,
    val script: String,
    val updatedAtMs: Long
)

data class InjectorScriptAnalysis(
    val commandLines: Int,
    val warningLines: Int,
    val estimatedDurationMs: Long,
    val warnings: List<String>
)

data class InjectorRunEntry(
    val ts: Long,
    val title: String,
    val status: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InjectorScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val prefs = remember { context.getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE) }
    val connectionState by viewModel.connectionState.collectAsState()
    val isConnected = connectionState is HidDeviceManager.ConnectionState.Connected

    val defaultPayload = "DELAY 500\nGUI SPACE\nDELAY 200\nSTRING terminal\nDELAY 200\nENTER\nDELAY 1000\nSTRING echo 'Hello from Hackie INJECTOR!'\nENTER\n"
    var payload by remember { mutableStateOf(defaultPayload) }
    var isInjecting by remember { mutableStateOf(false) }
    var showToolsSheet by remember { mutableStateOf(false) }
    var saveName by remember { mutableStateOf("") }
    var savedPayloads by remember { mutableStateOf(loadInjectorSavedPayloads(prefs)) }
    var runHistory by remember { mutableStateOf(loadInjectorRunHistory(prefs)) }
    var analysis by remember(payload) { mutableStateOf(analyzeInjectorScript(payload)) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val templateLibrary = remember {
        listOf(
            "Launch Safari" to "GUI SPACE\nDELAY 200\nSTRING safari\nENTER\n",
            "Open Terminal + whoami" to "GUI SPACE\nDELAY 200\nSTRING terminal\nENTER\nDELAY 600\nSTRING whoami\nENTER\n",
            "Mute Zoom" to "GUI SPACE\nDELAY 200\nSTRING zoom\nENTER\nDELAY 1000\nALT A\n",
            "System Lock" to "CTRL COMMAND Q\n",
            "Stealth Command" to "MAC_STEALTH echo 'rabit stealth'\n"
        )
    }

    Scaffold(
        containerColor = Obsidian,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {}
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                color = AccentPurple.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, AccentPurple.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = AccentPurple)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Script Engine", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Templates, validation, and staged injection", color = Silver, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Commands: DEFAULTDELAY, DELAY, STRING, ENTER, TAB, SPACE, UP, DOWN, GUI, CTRL, ALT, SHIFT, MAC_STEALTH",
                        color = Silver,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                color = Graphite.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Lines: ${analysis.commandLines}", color = Silver, fontSize = 12.sp)
                    Text("Warnings: ${analysis.warningLines}", color = if (analysis.warningLines > 0) WarningYellow else SuccessGreen, fontSize = 12.sp)
                    Text("Est. ${analysis.estimatedDurationMs / 1000.0}s", color = AccentBlue, fontSize = 12.sp)
                }
            }

            OutlinedTextField(
                value = payload,
                onValueChange = {
                    payload = it
                    analysis = analyzeInjectorScript(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp, max = 420.dp),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = Platinum
                ),
                placeholder = { Text("Write DuckyScript payload...", color = Silver.copy(alpha = 0.5f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Graphite,
                    focusedContainerColor = Graphite,
                    unfocusedBorderColor = BorderColor,
                    focusedBorderColor = AccentPurple
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { showToolsSheet = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = AccentGold.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tools")
                }
                OutlinedButton(
                    onClick = {
                        analysis = analyzeInjectorScript(payload)
                        scope.launch {
                            if (analysis.warningLines == 0) snackbarHostState.showSnackbar("Script validated: no warnings")
                            else snackbarHostState.showSnackbar("Validated with ${analysis.warningLines} warning(s)")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Validate")
                }
                OutlinedButton(
                    onClick = { payload = defaultPayload },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    if (!isConnected) {
                        val updated = listOf(
                            InjectorRunEntry(System.currentTimeMillis(), "Inject aborted", "No host connected")
                        ) + runHistory
                        runHistory = updated.take(12)
                        saveInjectorRunHistory(prefs, runHistory)
                        scope.launch { snackbarHostState.showSnackbar("Connect to host before injecting") }
                        return@Button
                    }
                    if (payload.isBlank()) return@Button
                    isInjecting = true
                    viewModel.executeDuckyScript(payload)
                    val updated = listOf(
                        InjectorRunEntry(
                            ts = System.currentTimeMillis(),
                            title = payload.lineSequence().firstOrNull()?.take(36)?.ifBlank { "Payload" } ?: "Payload",
                            status = "Dispatched"
                        )
                    ) + runHistory
                    runHistory = updated.take(12)
                    saveInjectorRunHistory(prefs, runHistory)
                    scope.launch {
                        snackbarHostState.showSnackbar("Payload dispatched")
                        kotlinx.coroutines.delay(1000)
                        isInjecting = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = payload.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
            ) {
                Icon(if (isInjecting) Icons.Default.Sync else Icons.Default.ElectricBolt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isInjecting) "INJECTING..." else "INJECT PAYLOAD", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
        }
    }

    if (showToolsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showToolsSheet = false },
            containerColor = Obsidian,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Silver.copy(alpha = 0.45f)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Injector Tools", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 18.sp)

                Text("Template Library", color = Silver, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                templateLibrary.forEach { (name, script) ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Graphite.copy(alpha = 0.45f),
                        border = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.35f)),
                        onClick = {
                            payload = script
                            analysis = analyzeInjectorScript(script)
                            showToolsSheet = false
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name, color = Platinum, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Icon(Icons.Default.NorthWest, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                HorizontalDivider(color = BorderColor.copy(alpha = 0.45f))

                Text("Command Palette", color = Silver, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                val commandPalette = listOf(
                    "DEFAULTDELAY 100",
                    "DELAY 500",
                    "STRING hello world",
                    "ENTER",
                    "GUI SPACE",
                    "CTRL C",
                    "ALT TAB",
                    "MAC_STEALTH whoami"
                )
                commandPalette.chunked(2).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        rowItems.forEach { cmd ->
                            AssistChip(
                                onClick = {
                                    payload = if (payload.endsWith("\n") || payload.isBlank()) "$payload$cmd\n" else "$payload\n$cmd\n"
                                    analysis = analyzeInjectorScript(payload)
                                },
                                label = { Text(cmd, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = AccentBlue.copy(alpha = 0.14f),
                                    labelColor = Platinum
                                )
                            )
                        }
                        if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }

                HorizontalDivider(color = BorderColor.copy(alpha = 0.45f))

                Text("Save Current Payload", color = Silver, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = saveName,
                    onValueChange = { saveName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Payload name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGold,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = Platinum
                    )
                )
                Button(
                    onClick = {
                        val normalized = saveName.trim()
                        if (normalized.isBlank() || payload.isBlank()) return@Button
                        val now = System.currentTimeMillis()
                        val updated = savedPayloads
                            .filterNot { it.name.equals(normalized, ignoreCase = true) }
                            .toMutableList()
                            .apply { add(0, InjectorSavedPayload(normalized, payload, now)) }
                        savedPayloads = updated
                        saveInjectorSavedPayloads(prefs, updated)
                        saveName = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Payload", color = Obsidian, fontWeight = FontWeight.Bold)
                }

                Text("Saved Payloads (${savedPayloads.size})", color = Silver, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                if (savedPayloads.isEmpty()) {
                    Text("No saved payloads yet.", color = Silver.copy(alpha = 0.7f), fontSize = 12.sp)
                } else {
                    savedPayloads.forEach { item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Graphite.copy(alpha = 0.4f),
                            border = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, color = Platinum, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("${item.script.lines().size} lines", color = Silver.copy(alpha = 0.8f), fontSize = 11.sp)
                                }
                                TextButton(onClick = {
                                    payload = item.script
                                    analysis = analyzeInjectorScript(item.script)
                                    showToolsSheet = false
                                }) { Text("Load") }
                                IconButton(onClick = {
                                    val updated = savedPayloads.filterNot { it.name == item.name }
                                    savedPayloads = updated
                                    saveInjectorSavedPayloads(prefs, updated)
                                }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = ErrorRed)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderColor.copy(alpha = 0.45f))

                Text("Backup", color = Silver, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            val exportJson = JSONArray().apply {
                                savedPayloads.forEach { item ->
                                    put(
                                        JSONObject().apply {
                                            put("name", item.name)
                                            put("script", item.script)
                                            put("updatedAtMs", item.updatedAtMs)
                                        }
                                    )
                                }
                            }
                            clipboard.setText(AnnotatedString(exportJson.toString()))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export")
                    }
                    OutlinedButton(
                        onClick = {
                            val raw = clipboard.getText()?.text.orEmpty()
                            val imported = parseImportedInjectorPayloads(raw)
                            if (imported.isNotEmpty()) {
                                val merged = (imported + savedPayloads)
                                    .distinctBy { it.name.lowercase() }
                                    .sortedByDescending { it.updatedAtMs }
                                savedPayloads = merged
                                saveInjectorSavedPayloads(prefs, merged)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.DownloadForOffline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import")
                    }
                }

                HorizontalDivider(color = BorderColor.copy(alpha = 0.45f))

                Text("Clipboard", color = Silver, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { clipboard.setText(AnnotatedString(payload)) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy")
                    }
                    OutlinedButton(
                        onClick = {
                            val clip = clipboard.getText()?.text.orEmpty()
                            if (clip.isNotBlank()) {
                                payload = clip
                                analysis = analyzeInjectorScript(clip)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paste")
                    }
                }

                if (analysis.warningLines > 0) {
                    HorizontalDivider(color = BorderColor.copy(alpha = 0.45f))
                    Text("Validation Warnings", color = WarningYellow, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    analysis.warnings.take(6).forEach {
                        Text("- $it", color = Silver, fontSize = 11.sp)
                    }
                }

                HorizontalDivider(color = BorderColor.copy(alpha = 0.45f))

                Text("Execution History", color = Silver, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                if (runHistory.isEmpty()) {
                    Text("No recent runs", color = Silver.copy(alpha = 0.7f), fontSize = 11.sp)
                } else {
                    runHistory.take(8).forEach { entry ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Graphite.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.title, color = Platinum, fontSize = 12.sp, maxLines = 1)
                                    Text(entry.status, color = Silver, fontSize = 10.sp)
                                }
                                IconButton(onClick = {
                                    payload = payloadFromHistoryTitle(entry.title, savedPayloads, payload)
                                    analysis = analyzeInjectorScript(payload)
                                }) {
                                    Icon(Icons.Default.History, contentDescription = "Reuse", tint = AccentBlue, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun analyzeInjectorScript(script: String): InjectorScriptAnalysis {
    val knownCommands = setOf(
        "DEFAULTDELAY", "DELAY", "STRING", "ENTER", "TAB", "SPACE",
        "UP", "UPARROW", "DOWN", "DOWNARROW", "GUI", "WINDOWS", "COMMAND",
        "CTRL", "CONTROL", "ALT", "SHIFT", "MAC_STEALTH"
    )

    var defaultDelay = 10L
    var estimatedMs = 0L
    var commandLines = 0
    val warnings = mutableListOf<String>()

    script.lines().forEachIndexed { index, raw ->
        val line = raw.trim()
        if (line.isBlank() || line.uppercase().startsWith("REM ")) return@forEachIndexed

        commandLines += 1
        val parts = line.split(" ", limit = 2)
        val cmd = parts[0].uppercase()
        val arg = if (parts.size > 1) parts[1] else ""

        if (!knownCommands.contains(cmd)) {
            warnings += "Line ${index + 1}: unknown command '$cmd' (typed as raw text)"
        }

        when (cmd) {
            "DEFAULTDELAY" -> defaultDelay = arg.toLongOrNull() ?: defaultDelay
            "DELAY" -> estimatedMs += (arg.toLongOrNull() ?: defaultDelay).coerceAtLeast(0L)
            "MAC_STEALTH" -> estimatedMs += 1200L
            else -> estimatedMs += defaultDelay
        }
    }

    return InjectorScriptAnalysis(
        commandLines = commandLines,
        warningLines = warnings.size,
        estimatedDurationMs = estimatedMs,
        warnings = warnings
    )
}

private fun loadInjectorSavedPayloads(prefs: android.content.SharedPreferences): List<InjectorSavedPayload> {
    val json = prefs.getString("injector_saved_payloads", null) ?: return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { idx ->
            val obj = arr.getJSONObject(idx)
            InjectorSavedPayload(
                name = obj.optString("name", "Payload ${idx + 1}"),
                script = obj.optString("script", ""),
                updatedAtMs = obj.optLong("updatedAtMs", 0L)
            )
        }.sortedByDescending { it.updatedAtMs }
    } catch (e: Exception) {
        Log.w("InjectorScreen", "Failed to parse saved payloads JSON", e)
        emptyList()
    }
}

private fun saveInjectorSavedPayloads(
    prefs: android.content.SharedPreferences,
    payloads: List<InjectorSavedPayload>
) {
    val arr = JSONArray()
    payloads.forEach { payload ->
        arr.put(
            JSONObject().apply {
                put("name", payload.name)
                put("script", payload.script)
                put("updatedAtMs", payload.updatedAtMs)
            }
        )
    }
    prefs.edit().putString("injector_saved_payloads", arr.toString()).apply()
}

private fun loadInjectorRunHistory(prefs: android.content.SharedPreferences): List<InjectorRunEntry> {
    val json = prefs.getString("injector_run_history", null) ?: return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { idx ->
            val obj = arr.getJSONObject(idx)
            InjectorRunEntry(
                ts = obj.optLong("ts", 0L),
                title = obj.optString("title", "Payload"),
                status = obj.optString("status", "Unknown")
            )
        }.sortedByDescending { it.ts }
    } catch (e: Exception) {
        Log.w("InjectorScreen", "Failed to parse run history JSON", e)
        emptyList()
    }
}

private fun saveInjectorRunHistory(
    prefs: android.content.SharedPreferences,
    entries: List<InjectorRunEntry>
) {
    val arr = JSONArray()
    entries.forEach { entry ->
        arr.put(
            JSONObject().apply {
                put("ts", entry.ts)
                put("title", entry.title)
                put("status", entry.status)
            }
        )
    }
    prefs.edit().putString("injector_run_history", arr.toString()).apply()
}

private fun parseImportedInjectorPayloads(raw: String): List<InjectorSavedPayload> {
    if (raw.isBlank()) return emptyList()
    return try {
        val arr = JSONArray(raw)
        (0 until arr.length()).mapNotNull { idx ->
            val obj = arr.optJSONObject(idx) ?: return@mapNotNull null
            val name = obj.optString("name").trim()
            val script = obj.optString("script").trim()
            if (name.isBlank() || script.isBlank()) return@mapNotNull null
            InjectorSavedPayload(
                name = name,
                script = script,
                updatedAtMs = obj.optLong("updatedAtMs", System.currentTimeMillis())
            )
        }
    } catch (e: Exception) {
        Log.w("InjectorScreen", "Failed to parse imported payload JSON", e)
        emptyList()
    }
}

private fun payloadFromHistoryTitle(
    title: String,
    saved: List<InjectorSavedPayload>,
    currentPayload: String
): String {
    return saved.firstOrNull { it.name.equals(title, ignoreCase = true) }?.script ?: currentPayload
}
