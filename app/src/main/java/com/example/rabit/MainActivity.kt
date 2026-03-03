package com.example.rabit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.rabit.data.bluetooth.HidService
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.assistant.AssistantScreen
import com.example.rabit.ui.assistant.AssistantViewModel
import com.example.rabit.ui.keyboard.KeyboardScreen
import com.example.rabit.ui.pairing.PairingScreen
import com.example.rabit.ui.settings.SettingsScreen
import com.example.rabit.ui.theme.RabitTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val assistantViewModel: AssistantViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RabitTheme {
                BluetoothPermissions {
                    LaunchedEffect(Unit) {
                        val intent = Intent(this@MainActivity, HidService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(intent)
                        } else {
                            startService(intent)
                        }
                    }
                    AppNavigation(viewModel, assistantViewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: MainViewModel, assistantViewModel: AssistantViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "pairing") {
        composable("pairing") {
            PairingScreen(
                viewModel = viewModel, 
                onConnected = { navController.navigate("keyboard") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("keyboard") {
            KeyboardScreen(
                viewModel = viewModel, 
                onDisconnect = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToAssistant = { navController.navigate("assistant") }
            )
        }
        composable("assistant") {
            AssistantScreen(
                viewModel = assistantViewModel,
                mainViewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToKeyboard = { 
                    navController.navigate("keyboard") {
                        popUpTo("keyboard") { inclusive = true }
                    }
                }
            )
        }
        composable("settings") {
            SettingsScreen(viewModel, onBack = { navController.popBackStack() })
        }
    }
}

@Composable
fun BluetoothPermissions(content: @Composable () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val permissions = mutableListOf<String>().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            add(Manifest.permission.BLUETOOTH)
            add(Manifest.permission.BLUETOOTH_ADMIN)
        }
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.VIBRATE)
        add(Manifest.permission.READ_PHONE_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var permissionsGranted by remember {
        mutableStateOf(permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionsGranted = result.values.all { it }
    }

    LaunchedEffect(Unit) {
        if (!permissionsGranted) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    if (permissionsGranted) {
        content()
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Please grant required permissions to use Rabit.")
        }
    }
}
