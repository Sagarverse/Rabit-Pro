package com.example.rabit.ui.automation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.theme.AccentBlue
import com.example.rabit.ui.theme.BorderColor
import com.example.rabit.ui.theme.Graphite
import com.example.rabit.ui.theme.Platinum
import com.example.rabit.ui.theme.Silver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SshTerminalScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val host by viewModel.sshHost.collectAsState()
    val port by viewModel.sshPort.collectAsState()
    val user by viewModel.sshUser.collectAsState()
    val pass by viewModel.sshPassword.collectAsState()
    val connected by viewModel.sshConnected.collectAsState()
    val status by viewModel.sshStatus.collectAsState()
    val lines by viewModel.sshTerminalLines.collectAsState()

    var hostInput by remember(host) { mutableStateOf(host) }
    var portInput by remember(port) { mutableStateOf(port.toString()) }
    var userInput by remember(user) { mutableStateOf(user) }
    var passInput by remember(pass) { mutableStateOf(pass) }
    var commandInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        contentVisible = true
    }

    AnimatedVisibility(
        visible = contentVisible,
        enter = fadeIn(animationSpec = tween(320)) + slideInVertically(initialOffsetY = { it / 14 }, animationSpec = tween(320))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Graphite)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF162031)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("SSH Terminal Setup (Step by Step)", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("1. Mac: System Settings > General > Sharing > enable Remote Login.", color = Silver, fontSize = 12.sp)
                Text("2. Note your Mac username and local IP (for example 192.168.1.20).", color = Silver, fontSize = 12.sp)
                Text("3. Enter Host/IP, Port (usually 22), Username, and Password below.", color = Silver, fontSize = 12.sp)
                Text("4. Tap Connect. If it fails, verify both devices are on the same Wi-Fi network.", color = Silver, fontSize = 12.sp)
                Text("5. If using a firewall, allow incoming SSH (TCP 22) on your Mac.", color = Silver, fontSize = 12.sp)
                Text("6. After connected, run a simple command first: whoami or pwd.", color = Silver, fontSize = 12.sp)
                Text("7. For better security, use a dedicated low-privilege user account for remote control.", color = Silver, fontSize = 12.sp)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Graphite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Native SSH Terminal", color = Platinum, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                Text("Status: $status", color = Silver, fontSize = 12.sp)

                OutlinedTextField(
                    value = hostInput,
                    onValueChange = {
                        hostInput = it
                        viewModel.setSshHost(it)
                    },
                    label = { Text("Host / IP") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = Platinum
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = portInput,
                        onValueChange = {
                            portInput = it
                            it.toIntOrNull()?.let(viewModel::setSshPort)
                        },
                        label = { Text("Port") },
                        singleLine = true,
                        modifier = Modifier.weight(0.35f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Platinum
                        )
                    )

                    OutlinedTextField(
                        value = userInput,
                        onValueChange = {
                            userInput = it
                            viewModel.setSshUser(it)
                        },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.weight(0.65f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Platinum
                        )
                    )
                }

                OutlinedTextField(
                    value = passInput,
                    onValueChange = {
                        passInput = it
                        viewModel.setSshPassword(it)
                    },
                    label = { Text("Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = Platinum
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { if (connected) viewModel.disconnectSsh() else viewModel.connectSsh() },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(if (connected) Icons.Default.LinkOff else Icons.Default.Link, contentDescription = null)
                        Text(if (connected) " Disconnect" else " Connect")
                    }

                    IconButton(onClick = { viewModel.clearSshTerminal() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = Silver)
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF061306)),
            modifier = Modifier
                .heightIn(min = 220.dp, max = 360.dp)
                .fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .background(Color(0xFF061306), RoundedCornerShape(10.dp))
            ) {
                items(lines) { line ->
                    Text(
                        line,
                        color = Color(0xFF7CFF9B),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                label = { Text("Type command (ls, top, pwd, etc.)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (commandInput.isNotBlank() && connected) {
                            viewModel.sendSshCommand(commandInput)
                            commandInput = ""
                            keyboardController?.hide()
                        }
                    }
                ),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = Platinum
                )
            )
            Button(
                onClick = {
                    viewModel.sendSshCommand(commandInput)
                    commandInput = ""
                },
                enabled = connected,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text("Run")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("pwd", "whoami", "uname -a").forEach { cmd ->
                AssistChip(
                    onClick = { if (connected) viewModel.sendSshCommand(cmd) },
                    label = { Text(cmd) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Graphite,
                        labelColor = Platinum
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Enable macOS Remote Login: System Settings > General > Sharing > Remote Login.",
            color = Silver,
            fontSize = 11.sp
        )
        }
    }
}
