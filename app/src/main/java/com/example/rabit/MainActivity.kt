package com.example.rabit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.rabit.data.bluetooth.HidService
import com.example.rabit.ui.MainViewModel
import com.example.rabit.ui.assistant.AssistantScreen
import com.example.rabit.ui.assistant.AssistantViewModel
import com.example.rabit.ui.keyboard.KeyboardScreen
import com.example.rabit.ui.onboarding.OnboardingScreen
import com.example.rabit.ui.pairing.PairingScreen
import com.example.rabit.ui.settings.PasswordManagerScreen
import com.example.rabit.ui.settings.SettingsScreen
import com.example.rabit.ui.snippets.SnippetsScreen
import com.example.rabit.ui.profile.ProfileScreen
import com.example.rabit.ui.automation.AutomationDashboardScreen
import com.example.rabit.ui.search.GlobalSearchScreen
import com.example.rabit.ui.theme.RabitTheme

class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val assistantViewModel: AssistantViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)

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
                    
                    val biometricEnabled by viewModel.biometricLockEnabled.collectAsState()
                    com.example.rabit.ui.components.BiometricGuard(isEnabled = biometricEnabled) {
                        AppNavigation(viewModel, assistantViewModel)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri = intent.parcelableExtraCompat<android.net.Uri>(Intent.EXTRA_STREAM)
                uri?.let { viewModel.addSharedFile(it) }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = intent.parcelableArrayListExtraCompat<android.net.Uri>(Intent.EXTRA_STREAM)
                uris?.forEach { viewModel.addSharedFile(it) }
            }
        }
    }

    private inline fun <reified T : Parcelable> Intent.parcelableExtraCompat(key: String): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(key) as? T
        }
    }

    private inline fun <reified T : Parcelable> Intent.parcelableArrayListExtraCompat(key: String): ArrayList<T>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableArrayListExtra(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableArrayListExtra(key)
        }
    }
}

@Composable
fun AppNavigation(viewModel: MainViewModel, assistantViewModel: AssistantViewModel) {
    val navController = rememberNavController()
    val startDest = if (viewModel.onboardingCompleted) "pairing" else "onboarding"
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: startDest
    val featureWebBridgeVisible by viewModel.featureWebBridgeVisible.collectAsState()
    val featureAutomationVisible by viewModel.featureAutomationVisible.collectAsState()
    val featureAssistantVisible by viewModel.featureAssistantVisible.collectAsState()
    val featureSnippetsVisible by viewModel.featureSnippetsVisible.collectAsState()
    val featureShortcutsVisible by viewModel.featureShortcutsVisible.collectAsState()
    val featureWakeOnLanVisible by viewModel.featureWakeOnLanVisible.collectAsState()
    val featureSshTerminalVisible by viewModel.featureSshTerminalVisible.collectAsState()
    val webBridgeEnabled by viewModel.webBridgeEnabled.collectAsState()

    // Routes that should NOT show the professional drawer (Onboarding & Initial Pairing)
    val noDrawerRoutes = listOf("onboarding", "pairing", "onboarding_splash", "assistant")
    val showDrawer = currentRoute.split("?").first() !in noDrawerRoutes

    fun routeAllowed(route: String): Boolean {
        return when (route) {
            "web_bridge" -> featureWebBridgeVisible
            "automation" -> featureAutomationVisible
            "shortcuts" -> featureAutomationVisible
            "assistant" -> featureAssistantVisible
            "snippets" -> featureSnippetsVisible
            "wake_on_lan" -> featureWakeOnLanVisible
            "ssh_terminal" -> featureSshTerminalVisible
            "global_search" -> true
            else -> true
        }
    }

    LaunchedEffect(
        currentRoute,
        featureWebBridgeVisible,
        featureAutomationVisible,
        featureAssistantVisible,
        featureSnippetsVisible,
        featureWakeOnLanVisible,
        featureSshTerminalVisible
    ) {
        val current = currentRoute.split("?").first()
        if (!routeAllowed(current)) {
            navController.navigate("keyboard") {
                popUpTo(current) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val navHost = @Composable { padding: androidx.compose.foundation.layout.PaddingValues ->
        Box(modifier = Modifier.padding(padding)) {
            NavHost(navController = navController, startDestination = startDest) {
                composable("onboarding") {
                    OnboardingScreen(
                        onComplete = {
                            viewModel.markOnboardingCompleted()
                            navController.navigate("pairing") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        }
                    )
                }
                composable("pairing") {
                    PairingScreen(
                        viewModel = viewModel,
                        onConnected = { navController.navigate("keyboard") },
                        onNavigateToKeyboard = { navController.navigate("keyboard") },
                        onNavigateToSettings = { navController.navigate("settings") },
                        onNavigateToAssistant = { if (featureAssistantVisible) navController.navigate("assistant") },
                        onNavigateToWebBridge = { if (featureWebBridgeVisible) navController.navigate("web_bridge") },
                        onNavigateToInjector = { navController.navigate("injector") },
                        onNavigateToMediaDeck = { navController.navigate("media_deck") },
                        onNavigateToAirPlayReceiver = { navController.navigate("airplay_receiver") },
                        onNavigateToWakeOnLan = { if (featureWakeOnLanVisible) navController.navigate("wake_on_lan") },
                        onNavigateToSshTerminal = { if (featureSshTerminalVisible) navController.navigate("ssh_terminal") },
                        onNavigateToGlobalSearch = { navController.navigate("global_search") },
                        onNavigateToSnippets = { if (featureSnippetsVisible) navController.navigate("snippets") },
                        onNavigateToAutomation = { if (featureAutomationVisible) navController.navigate("automation") },
                        onNavigateToProfile = { navController.navigate("profile") },
                        onNavigateToCustomization = { navController.navigate("customization") },
                        onNavigateToPasswordManager = { navController.navigate("password_manager") }
                    )
                }
                composable("keyboard") {
                    KeyboardScreen(
                        viewModel = viewModel,
                        onDisconnect = { 
                            viewModel.disconnectKeyboard()
                            navController.navigate("pairing") { popUpTo(0) } 
                        },
                        onNavigateToSettings = { navController.navigate("settings") },
                        onNavigateToAssistant = { if (featureAssistantVisible) navController.navigate("assistant") },
                        onNavigateToSnippets = { if (featureSnippetsVisible) navController.navigate("snippets") },
                        onNavigateToAutomation = { if (featureAutomationVisible) navController.navigate("automation") },
                        onNavigateToWebBridge = { if (featureWebBridgeVisible) navController.navigate("web_bridge") }
                    )
                }
                composable("web_bridge") {
                    com.example.rabit.ui.webbridge.WebBridgeScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("automation") {
                    AutomationDashboardScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onNavigateToWakeOnLan = { if (featureWakeOnLanVisible) navController.navigate("wake_on_lan") },
                        onNavigateToSshTerminal = { if (featureSshTerminalVisible) navController.navigate("ssh_terminal") }
                    )
                }
                composable("media_deck") {
                    com.example.rabit.ui.media.MediaControlDeckScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("airplay_receiver") {
                    com.example.rabit.ui.airplay.AirPlayReceiverScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("wake_on_lan") {
                    com.example.rabit.ui.automation.WakeOnLanScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("ssh_terminal") {
                    com.example.rabit.ui.automation.SshTerminalScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("assistant") {
                    AssistantScreen(
                        viewModel = assistantViewModel,
                        mainViewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onNavigateToSettings = {
                            navController.navigate("settings") {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToKeyboard = { navController.navigate("keyboard") },
                        onNavigate = { route ->
                            navController.navigate(route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable("injector") {
                    com.example.rabit.ui.automation.InjectorScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        viewModel,
                        onBack = { navController.popBackStack() },
                        onNavigateToProfile = { navController.navigate("profile") },
                        onNavigateToCustomization = { navController.navigate("customization") },
                        onNavigateToPasswordManager = { navController.navigate("password_manager") }
                    )
                }
                composable("customization") {
                    com.example.rabit.ui.settings.CustomizationScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("password_manager") {
                    PasswordManagerScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("profile") {
                    ProfileScreen(onBack = { navController.popBackStack() })
                }
                composable("snippets") {
                    SnippetsScreen(viewModel, onBack = { navController.popBackStack() })
                }
                composable("shortcuts") {
                    AutomationDashboardScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onNavigateToWakeOnLan = { if (featureWakeOnLanVisible) navController.navigate("wake_on_lan") },
                        onNavigateToSshTerminal = { if (featureSshTerminalVisible) navController.navigate("ssh_terminal") }
                    )
                }
                composable("global_search") {
                    val available = buildSet {
                        add("keyboard")
                        add("injector")
                        add("media_deck")
                        add("airplay_receiver")
                        add("settings")
                        add("customization")
                        add("password_manager")
                        add("profile")
                        if (featureWebBridgeVisible) add("web_bridge")
                        if (featureAutomationVisible) add("automation")
                        if (featureAssistantVisible) add("assistant")
                        if (featureSnippetsVisible) add("snippets")
                        if (featureWakeOnLanVisible) add("wake_on_lan")
                        if (featureSshTerminalVisible) add("ssh_terminal")
                    }
                    val availableActions = buildSet {
                        add("action_unlock_mac")
                        add("action_lock_screen")
                        add("action_media_play_pause")
                        add("action_media_vol_up")
                        add("action_media_vol_down")
                        add("action_now_playing")
                        add("action_disconnect_keyboard")
                        if (featureWakeOnLanVisible) add("action_wol_send")
                        if (featureWebBridgeVisible) add("action_web_bridge_toggle")
                    }
                    GlobalSearchScreen(
                        currentRoute = currentRoute.split("?").first(),
                        availableRoutes = available,
                        availableActionIds = availableActions,
                        onBack = { navController.popBackStack() },
                        onNavigate = { route ->
                            if (!routeAllowed(route)) return@GlobalSearchScreen
                            navController.navigate(route) {
                                launchSingleTop = true
                            }
                        },
                        onExecuteAction = { actionId ->
                            when (actionId) {
                                "action_unlock_mac" -> viewModel.unlockMac()
                                "action_lock_screen" -> viewModel.sendSystemShortcut(MainViewModel.SystemShortcut.LOCK_SCREEN)
                                "action_media_play_pause" -> viewModel.sendMediaPlayPause()
                                "action_media_vol_up" -> viewModel.sendMediaVolumeUp()
                                "action_media_vol_down" -> viewModel.sendMediaVolumeDown()
                                "action_now_playing" -> viewModel.requestNowPlayingFromHost()
                                "action_wol_send" -> if (featureWakeOnLanVisible) viewModel.sendWakeOnLan()
                                "action_disconnect_keyboard" -> viewModel.disconnectKeyboard()
                                "action_web_bridge_toggle" -> {
                                    if (featureWebBridgeVisible) {
                                        if (webBridgeEnabled) viewModel.stopWebBridge() else viewModel.startWebBridge()
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    val activeApp by viewModel.activeApp.collectAsState()

    if (showDrawer) {
        com.example.rabit.ui.components.RabitAppScaffold(
            currentRoute = if (currentRoute == "keyboard") "main" else currentRoute,
            onNavigate = { route ->
                val target = if (route == "main") "keyboard" else route
                if (!routeAllowed(target)) return@RabitAppScaffold
                navController.navigate(target) {
                    popUpTo("keyboard") { saveState = true }
                    launchSingleTop = true
                    restoreState = target != "assistant"
                }
            },
            featureWebBridgeVisible = featureWebBridgeVisible,
            featureAutomationVisible = featureAutomationVisible,
            featureAssistantVisible = featureAssistantVisible,
            featureSnippetsVisible = featureSnippetsVisible,
            featureShortcutsVisible = featureShortcutsVisible,
            featureWakeOnLanVisible = featureWakeOnLanVisible,
            featureSshTerminalVisible = featureSshTerminalVisible,
            activeApp = activeApp,
            onBack = { navController.popBackStack() },
            topBarActions = {
                IconButton(onClick = {
                    if (currentRoute.split("?").first() != "global_search") {
                        navController.navigate("global_search") { launchSingleTop = true }
                    }
                }) {
                    Icon(Icons.Default.Search, contentDescription = "Open global search")
                }
            }
        ) { padding ->
            navHost(padding)
        }
    } else {
        navHost(androidx.compose.foundation.layout.PaddingValues(0.dp))
    }
}

@Composable
fun BluetoothPermissions(content: @Composable () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // Only require critical Bluetooth permissions to show app content
    val criticalPermissions = mutableListOf<String>().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            add(Manifest.permission.BLUETOOTH)
            add(Manifest.permission.BLUETOOTH_ADMIN)
        }
    }

    // Optional permissions (requested but not blocking)
    val optionalPermissions = mutableListOf<String>().apply {
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.VIBRATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_IMAGES)
            add(Manifest.permission.READ_MEDIA_VIDEO)
            add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    val allPermissions = (criticalPermissions + optionalPermissions).toTypedArray()

    var criticalGranted by remember {
        mutableStateOf(criticalPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        criticalGranted = criticalPermissions.all { result[it] == true }
    }

    LaunchedEffect(Unit) {
        val anyMissing = allPermissions.any {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (anyMissing) {
            permissionLauncher.launch(allPermissions)
        }
    }

    if (criticalGranted) {
        content()
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Default.Bluetooth,
                    contentDescription = "Bluetooth permission icon",
                    tint = Color(0xFF0A84FF),
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    "Bluetooth Permission Required",
                    color = Color(0xFFF2F2F7),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Rabit needs Bluetooth access to connect to your Mac.",
                    color = Color(0xFF8E8E93),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = { permissionLauncher.launch(allPermissions) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0A84FF)
                    )
                ) {
                    Text("Grant Permission", color = Color.White)
                }
            }
        }
    }
}
