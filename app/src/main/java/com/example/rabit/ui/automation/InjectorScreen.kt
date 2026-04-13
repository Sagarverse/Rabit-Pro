package com.example.rabit.ui.automation

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InjectorScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var payload by remember { mutableStateOf("DELAY 500\nGUI SPACE\nDELAY 200\nSTRING terminal\nDELAY 200\nENTER\nDELAY 1000\nSTRING echo 'Hello from Rabit INJECTOR!'\nENTER\n") }
    var isInjecting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Obsidian,
        topBar = {
            TopAppBar(
                title = { Text("Payload Injector", color = Platinum, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Platinum)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                color = AccentPurple.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, AccentPurple.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = AccentPurple)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("DuckyScript Engine", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Fast raw keystroke injection", color = Silver, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("How to use:", color = Platinum, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("1. Connect to Mac/PC via Bluetooth\n2. Write commands (one per line)\n3. Tap Inject", color = Silver, fontSize = 11.sp, lineHeight = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Supported Commands:", color = Platinum, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("• STRING <text> | DELAY <ms>\n• ENTER, TAB, SPACE, UP, DOWN\n• GUI (Cmd), CTRL, ALT, SHIFT\n• MAC_STEALTH <cmd> (Hidden execution)", color = Silver, fontSize = 11.sp, lineHeight = 16.sp)
                }
            }

            OutlinedTextField(
                value = payload,
                onValueChange = { payload = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = Platinum
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Graphite,
                    focusedContainerColor = Graphite,
                    unfocusedBorderColor = BorderColor,
                    focusedBorderColor = AccentPurple
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (payload.isNotBlank()) {
                        isInjecting = true
                        viewModel.executeDuckyScript(payload)
                        scope.launch {
                            kotlinx.coroutines.delay(1000) // Brief UI indicator
                            isInjecting = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
            ) {
                Icon(if (isInjecting) Icons.Default.Sync else Icons.Default.ElectricBolt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isInjecting) "INJECTING..." else "INJECT PAYLOAD", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            }
        }
    }
}