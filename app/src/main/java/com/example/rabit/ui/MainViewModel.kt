package com.example.rabit.ui

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rabit.data.repository.KeyboardRepositoryImpl
import com.example.rabit.domain.repository.KeyboardRepository
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.data.bluetooth.HidService
import com.example.rabit.data.sensors.GyroscopeAirMouse
import com.example.rabit.data.network.RabitNetworkServer
import com.example.rabit.data.network.WebRtcManager
import com.example.rabit.data.gemini.LocalLlmManager
import com.example.rabit.data.sensors.SpatialPointerManager
import com.example.rabit.data.voice.VoiceAssistantManager
import com.example.rabit.data.voice.VoiceState
import com.example.rabit.domain.model.HidKeyCodes
import com.example.rabit.domain.model.Workstation
import com.example.rabit.domain.model.RemoteFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.experimental.or
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: KeyboardRepository = KeyboardRepositoryImpl(application)
    private val localLlmManager = LocalLlmManager(application)
    private val spatialPointerManager = SpatialPointerManager(application)
    private val gyroAirMouse = GyroscopeAirMouse(application)
    private val voiceAssistantManager = VoiceAssistantManager(application)
    private val webRtcManager = WebRtcManager(application)
    private val prefs = application.getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)

    val connectionState: StateFlow<HidDeviceManager.ConnectionState> = repository.connectionState
    val scannedDevices: StateFlow<Set<BluetoothDevice>> = repository.scannedDevices
    val isScanning: StateFlow<Boolean> = repository.isScanning
    val isPushPaused: StateFlow<Boolean> = repository.isPushPaused
    val isTextPushing = repository.isTextPushing
    val knownWorkstations = repository.knownWorkstations

    private val _precisionModeEnabled = MutableStateFlow(false)
    val precisionModeEnabled = _precisionModeEnabled.asStateFlow()

    private val _unlockPassword = MutableStateFlow(prefs.getString("unlock_password", "6202") ?: "6202")
    val unlockPassword = _unlockPassword.asStateFlow()

    private val _autoReconnectEnabled = MutableStateFlow(prefs.getBoolean("auto_reconnect_enabled", true))
    val autoReconnectEnabled = _autoReconnectEnabled.asStateFlow()

    private val _typingSpeed = MutableStateFlow(prefs.getString("typing_speed", "Normal") ?: "Normal")
    val typingSpeed = _typingSpeed.asStateFlow()

    private val _activeModifiers = MutableStateFlow<Byte>(0)
    val activeModifiers = _activeModifiers.asStateFlow()

    private val _notificationSyncEnabled = MutableStateFlow(prefs.getBoolean("notification_sync_enabled", false))
    val notificationSyncEnabled = _notificationSyncEnabled.asStateFlow()

    private val _autoPushEnabled = MutableStateFlow(prefs.getBoolean("auto_push_enabled", false))
    val autoPushEnabled = _autoPushEnabled.asStateFlow()

    private val _webBridgeEnabled = MutableStateFlow(prefs.getBoolean("web_bridge_enabled", false))
    val webBridgeEnabled = _webBridgeEnabled.asStateFlow()

    private val _webBridgeRunning = MutableStateFlow(RabitNetworkServer.isRunning)
    val isWebBridgeRunning: StateFlow<Boolean> = _webBridgeRunning.asStateFlow()

    private val _webBridgePin = MutableStateFlow(RabitNetworkServer.currentPin)
    val webBridgePin: StateFlow<String> = _webBridgePin.asStateFlow()

    // Vibration toggle
    private val _vibrationEnabled = MutableStateFlow(prefs.getBoolean("vibration_enabled", true))
    val vibrationEnabled = _vibrationEnabled.asStateFlow()

    // Trackpad sensitivity (0.5f to 3.0f)
    private val _trackpadSensitivity = MutableStateFlow(prefs.getFloat("trackpad_sensitivity", 1.5f))
    val trackpadSensitivity = _trackpadSensitivity.asStateFlow()

    // Air Mouse
    private val _airMouseEnabled = MutableStateFlow(false)
    val airMouseEnabled = _airMouseEnabled.asStateFlow()

    private val _airMouseSensitivity = MutableStateFlow(prefs.getFloat("air_mouse_sensitivity", 18f))
    val airMouseSensitivity = _airMouseSensitivity.asStateFlow()

    // Text push progress
    private val _pushProgress = MutableStateFlow(0f)
    val pushProgress = _pushProgress.asStateFlow()

    private val _savedDevices = MutableStateFlow<List<SavedDevice>>(emptyList())
    val savedDevices = _savedDevices.asStateFlow()

    // Voice
    val voiceState = voiceAssistantManager.state
    val voiceResult = voiceAssistantManager.result

    private val _remoteFiles = MutableStateFlow<List<RemoteFile>>(emptyList())
    val remoteFiles = _remoteFiles.asStateFlow()

    private val _isRemoteLoading = MutableStateFlow(false)
    val isRemoteLoading = _isRemoteLoading.asStateFlow()

    private val _currentRemotePath = MutableStateFlow("/")
    val currentRemotePath = _currentRemotePath.asStateFlow()

    // Phase 10: Advanced Customization & Biometric states
    private val _biometricLockEnabled = MutableStateFlow(prefs.getBoolean("biometric_lock_enabled", false))
    val biometricLockEnabled = _biometricLockEnabled.asStateFlow()

    private val _shakeToDisconnectEnabled = MutableStateFlow(prefs.getBoolean("shake_to_disconnect_enabled", false))
    val shakeToDisconnectEnabled = _shakeToDisconnectEnabled.asStateFlow()

    private val _stealthModeEnabled = MutableStateFlow(prefs.getBoolean("stealth_mode_enabled", false))
    val stealthModeEnabled = _stealthModeEnabled.asStateFlow()

    private val _dynamicThemeEnabled = MutableStateFlow(prefs.getBoolean("dynamic_theme_enabled", true))
    val dynamicThemeEnabled = _dynamicThemeEnabled.asStateFlow()

    init {
        spatialPointerManager.onPointerUpdate = { dx, dy ->
            if (_airMouseEnabled.value) {
                repository.sendMouseMove(dx, dy)
            }
        }

        gyroAirMouse.onShakeDetected = {
            if (_shakeToDisconnectEnabled.value && connectionState.value is HidDeviceManager.ConnectionState.Connected) {
                repository.disconnect()
            }
        }

        gyroAirMouse.onCalibrationStatusChanged = { isCalibrating ->
            // Use for UI feedback if needed
        }

        // Start sensors
        gyroAirMouse.start()

        // Feature: Shake-to-Disconnect (Linked via gyroAirMouse callback above)
        viewModelScope.launch {
            webRtcManager.incomingDataFlow.collect { (type, data) ->
                if (type == "METADATA") {
                    handleRemoteMetadata(data as String)
                }
            }
        }
    }

    // Shared Files for Hub (Phone -> Mac)
    private val _sharedFiles = MutableStateFlow<List<android.net.Uri>>(emptyList())
    val sharedFiles = _sharedFiles.asStateFlow()

    // Onboarding completed
    val onboardingCompleted: Boolean
        get() = prefs.getBoolean("onboarding_completed", false)

    fun markOnboardingCompleted() {
        prefs.edit().putBoolean("onboarding_completed", true).apply()
    }

    // Phase 10 customization setters
    fun setBiometricLockEnabled(enabled: Boolean) {
        _biometricLockEnabled.value = enabled
        prefs.edit().putBoolean("biometric_lock_enabled", enabled).apply()
    }

    fun setShakeToDisconnectEnabled(enabled: Boolean) {
        _shakeToDisconnectEnabled.value = enabled
        prefs.edit().putBoolean("shake_to_disconnect_enabled", enabled).apply()
    }

    fun setStealthModeEnabled(enabled: Boolean) {
        _stealthModeEnabled.value = enabled
        prefs.edit().putBoolean("stealth_mode_enabled", enabled).apply()
    }

    fun setDynamicThemeEnabled(enabled: Boolean) {
        _dynamicThemeEnabled.value = enabled
        prefs.edit().putBoolean("dynamic_theme_enabled", enabled).apply()
        // Update global theme state if necessary
        com.example.rabit.ui.theme.AppThemeMode.isMonochrome = !enabled
    }

    fun addSharedFile(uri: android.net.Uri) {
        _sharedFiles.value = _sharedFiles.value + uri
    }

    fun removeSharedFile(uri: android.net.Uri) {
        _sharedFiles.value = _sharedFiles.value - uri
    }

    fun clearSharedFiles() {
        _sharedFiles.value = emptyList()
    }

    private val _deviceIp = MutableStateFlow("0.0.0.0")
    val deviceIp = _deviceIp.asStateFlow()

    // P2P Hosting
    val p2pPeerId = webRtcManager.peerId
    val p2pStatus = webRtcManager.connectionStatus

    private val _p2pEnabled = MutableStateFlow(prefs.getBoolean("p2p_enabled", false))
    val p2pEnabled = _p2pEnabled.asStateFlow()

    private fun generateRandomPin(): String = (1000..9999).random().toString()

    fun regenerateWebBridgePin() {
        val newPin = generateRandomPin()
        _webBridgePin.value = newPin
        RabitNetworkServer.currentPin = newPin
    }

    fun setDeviceIp(ip: String) {
        _deviceIp.value = ip
    }

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            "auto_push_enabled" -> _autoPushEnabled.value = sharedPreferences.getBoolean(key, false)
            "auto_reconnect_enabled" -> _autoReconnectEnabled.value = sharedPreferences.getBoolean(key, true)
            "notification_sync_enabled" -> _notificationSyncEnabled.value = sharedPreferences.getBoolean(key, false)
            "unlock_password" -> _unlockPassword.value = sharedPreferences.getString(key, "6202") ?: "6202"
            "typing_speed" -> {
                val speed = sharedPreferences.getString(key, "Normal") ?: "Normal"
                _typingSpeed.value = speed
                updateRepositorySpeed(speed)
            }
        }
    }


    // Custom Macros State (cached for performance)
    private val _customMacros = MutableStateFlow<List<CustomMacro>>(emptyList())
    val customMacros = _customMacros.asStateFlow()
    private var macrosCache: List<CustomMacro>? = null

    // Trackpad optimization — EMA smoothing state
    private var lastMoveTime = 0L
    private val moveThreshold = 0.2f
    private var lastDx = 0f
    private var lastDy = 0f
    private var smoothDx = 0f
    private var smoothDy = 0f
    private val emaAlpha = 0.45f // Smoothing factor: lower = smoother, higher = more responsive

    private val _localIp = MutableStateFlow("0.0.0.0")
    val localIp = _localIp.asStateFlow()

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        updateRepositorySpeed(_typingSpeed.value)
        setupNetworkListeners()
        refreshLocalIp()
        _customMacros.value = loadCustomMacros()
        macrosCache = _customMacros.value
        _savedDevices.value = loadSavedDevices()
        
        val serviceIntent = Intent(getApplication<Application>(), HidService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(serviceIntent)
        } else {
            getApplication<Application>().startService(serviceIntent)
        }

        // Save device when connected
        viewModelScope.launch {
            connectionState.collect { state ->
                if (state is HidDeviceManager.ConnectionState.Connected) {
                    saveDevice(state.deviceName, "")
                }
            }
        }

        // Auto-reconnect on startup if enabled
        if (_autoReconnectEnabled.value) {
            viewModelScope.launch {
                delay(1000) // Give service time to start
                _savedDevices.value.firstOrNull()?.let { device ->
                    val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
                    val bondedDevice = try {
                        bluetoothAdapter?.bondedDevices?.find { it.name == device.name }
                    } catch (e: Exception) { null }
                    
                    if (bondedDevice != null && connectionState.value is HidDeviceManager.ConnectionState.Disconnected) {
                        repository.connectWithRetry(bondedDevice)
                    }
                }
            }
        }
    }

    private fun setupNetworkListeners() {
        // Legacy listeners removed for File Hub focus
        // RabitNetworkServer now purely manages bidirectional file sharing
    }

    fun refreshLocalIp() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val iface = interfaces.nextElement()
                    if (iface.isLoopback || !iface.isUp) continue
                    val addresses = iface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val addr = addresses.nextElement()
                        if (addr is java.net.Inet4Address) {
                            _localIp.value = addr.hostAddress
                            return@launch
                        }
                    }
                }
            } catch (e: Exception) {
                _localIp.value = "0.0.0.0"
            }
        }
    }

    fun startWebBridge() {
        // Generate fresh PIN
        val pin = (1000..9999).random().toString()
        RabitNetworkServer.currentPin = pin
        
        refreshLocalIp()
        
        val intent = Intent(getApplication<Application>(), HidService::class.java).apply {
            action = HidService.ACTION_START_WEB_BRIDGE
        }
        getApplication<Application>().startService(intent)
        _webBridgeEnabled.value = true
        // Polling status for UI feedback
        viewModelScope.launch {
            delay(500)
            _webBridgeRunning.value = RabitNetworkServer.isRunning
            _webBridgePin.value = pin
        }
    }

    fun stopWebBridge() {
        val intent = Intent(getApplication<Application>(), HidService::class.java).apply {
            action = HidService.ACTION_STOP_WEB_BRIDGE
        }
        getApplication<Application>().startService(intent)
        _webBridgeEnabled.value = false
        _webBridgeRunning.value = false
        clearSharedFiles() // Clear on stop for security
        stopP2PHosting() // Also stop P2P when bridge stops
    }

    fun startP2PHosting() {
        webRtcManager.start()
        _p2pEnabled.value = true
        prefs.edit().putBoolean("p2p_enabled", true).apply()
    }

    fun stopP2PHosting() {
        webRtcManager.stop()
        _p2pEnabled.value = false
        prefs.edit().putBoolean("p2p_enabled", false).apply()
    }

    fun startScanning() = repository.startScanning()
    fun stopScanning() = repository.stopScanning()
    fun requestDiscoverable() {
        repository.requestDiscoverable()
    }

    fun requestEnableBluetooth(context: Context) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(enableBtIntent)
        }
    }

    fun connect(device: BluetoothDevice) = repository.connect(device)
    fun connectWithRetry(device: BluetoothDevice) = repository.connectWithRetry(device)
    fun disconnectKeyboard() {
        repository.disconnect()
    }

    fun removeWorkstation(address: String) {
        repository.removeWorkstation(address)
    }

    fun connectToWorkstation(workstation: com.example.rabit.domain.model.Workstation) {
        // Find device in current scans or create a bounded placeholder
        val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        val device = adapter.getRemoteDevice(workstation.address)
        repository.connectWithRetry(device, maxRetries = 3)
    }
    fun sendKey(keyCode: Byte) {
        repository.sendKey(keyCode, _activeModifiers.value)
    }

    fun sendKey(keyCode: Byte, modifier: Byte) {
        repository.sendKey(keyCode, modifier)
    }

    fun toggleModifier(modifier: Byte) {
        val current = _activeModifiers.value
        val newState = if ((current.toInt() and modifier.toInt()) != 0) {
            current.toInt() and modifier.toInt().inv()
        } else {
            current.toInt() or modifier.toInt()
        }.toByte()
        
        _activeModifiers.value = newState
        repository.setModifier(modifier, (newState.toInt() and modifier.toInt()) != 0)
    }

    fun sendConsumerKey(usageId: Short) = repository.sendConsumerKey(usageId)
    fun sendText(text: String) = repository.sendText(text)

    // ── Macro Genie ──
    private val _genieState = MutableStateFlow<GenieState>(GenieState.Idle)
    val genieState = _genieState.asStateFlow()

    sealed class GenieState {
        object Idle : GenieState()
        object Thinking : GenieState()
        data class Executing(val currentStep: String, val progress: Float) : GenieState()
        data class Success(val macroName: String) : GenieState()
        data class Error(val message: String) : GenieState()
    }

    fun generateSmartMacro(intent: String) {
        if (intent.isBlank()) return
        viewModelScope.launch {
            _genieState.value = GenieState.Thinking
            try {
                val ggufPath = prefs.getString("gguf_path", null)
                if (ggufPath == null) {
                    _genieState.value = GenieState.Error("Offline model not configured in Settings.")
                    return@launch
                }

                val initialized = localLlmManager.initialize(ggufPath)
                if (!initialized) {
                    _genieState.value = GenieState.Error("Failed to initialize AI.")
                    return@launch
                }

                val prompt = """
                    You are Infrastructure AI Genie, a professional HID automation engine. 
                    Convert user intent into a sequence of Control Command Tags.
                    
                    TAGS:
                    - [K:MOD+KEY] -> Key combo (Mods: GUI/CMD, SHIFT, ALT/OPT, CTRL. Keys: A-Z, 0-9, SPACE, ENTER, TAB, ESC)
                    - [T:TEXT] -> Type literal text string
                    - [W:MS] -> Wait/Delay in milliseconds
                    - [S:KEY] -> Special key (MUTE, VOL_UP, VOL_DOWN, PLAY, BRIGHT_UP)
                    
                    EXAMPLES:
                    - "Open Chrome and search for Rabit" -> [K:GUI+SPACE][W:200][T:Chrome][K:ENTER][W:800][K:GUI+L][T:google.com][K:ENTER][W:500][T:Rabit Pro][K:ENTER]
                    - "Mute and lock" -> [S:MUTE][W:100][K:GUI+CTRL+Q]
                    - "Next song" -> [S:PLAY]
                    
                    User Intent: "$intent"
                    Output ONLY the Tag sequence. Do not explain.
                """.trimIndent()

                val response = localLlmManager.generateResponse(prompt).trim()
                if (response.isNotEmpty()) {
                    executeAdvancedMacro(response)
                    _genieState.value = GenieState.Success(intent)
                    delay(3000)
                    _genieState.value = GenieState.Idle
                } else {
                    _genieState.value = GenieState.Error("AI returned empty sequence.")
                }
            } catch (e: Exception) {
                _genieState.value = GenieState.Error(e.message ?: "Genie failed")
            }
        }
    }

    private var macroJob: Job? = null

    fun cancelMacro() {
        macroJob?.cancel()
        _genieState.value = GenieState.Idle
    }

    private suspend fun executeAdvancedMacro(script: String) {
        macroJob = CoroutineScope(Dispatchers.Main).launch {
            val commandRegex = Regex("\\[(K|T|W|S):([^\\]]+)\\]")
            val matches = commandRegex.findAll(script).toList()
            
            matches.forEachIndexed { index, matchResult ->
                val type = matchResult.groupValues[1]
                val value = matchResult.groupValues[2]
                val progress = (index + 1).toFloat() / matches.size
                
                when (type) {
                    "K" -> {
                        _genieState.value = GenieState.Executing("Keys: $value", progress)
                        executeKeyCombo(value)
                    }
                    "T" -> {
                        _genieState.value = GenieState.Executing("Typing...", progress)
                        repository.sendText(value)
                    }
                    "W" -> {
                        val ms = value.toLongOrNull() ?: 100L
                        _genieState.value = GenieState.Executing("Waiting $ms ms", progress)
                        delay(ms)
                    }
                    "S" -> {
                        _genieState.value = GenieState.Executing("Consumer: $value", progress)
                        executeSpecialKey(value)
                    }
                }
                delay(120) // Pro delay for OS stability
            }
            delay(1000)
            _genieState.value = GenieState.Idle
        }
    }

    private fun executeKeyCombo(combo: String) {
        val keys = combo.split("+").map { it.trim().uppercase() }
        var modifiers = 0.toByte()
        var mainKey = HidKeyCodes.KEY_NONE

        keys.forEach { key ->
            when (key) {
                "GUI", "CMD", "WIN" -> modifiers = modifiers or HidKeyCodes.MODIFIER_LEFT_GUI
                "SHIFT" -> modifiers = modifiers or HidKeyCodes.MODIFIER_LEFT_SHIFT
                "ALT", "OPTION" -> modifiers = modifiers or HidKeyCodes.MODIFIER_LEFT_ALT
                "CTRL" -> modifiers = modifiers or HidKeyCodes.MODIFIER_LEFT_CTRL
                "SPACE" -> mainKey = HidKeyCodes.KEY_SPACE
                "ENTER" -> mainKey = HidKeyCodes.KEY_ENTER
                "ESC" -> mainKey = HidKeyCodes.KEY_ESC
                "TAB" -> mainKey = HidKeyCodes.KEY_TAB
                "BACKSPACE" -> mainKey = HidKeyCodes.KEY_BACKSPACE
                else -> {
                    if (key.length == 1 && key[0] in 'A'..'Z') {
                        mainKey = (HidKeyCodes.KEY_A + (key[0] - 'A')).toByte()
                    } else if (key.length == 1 && key[0] in '0'..'9') {
                        mainKey = if (key[0] == '0') HidKeyCodes.KEY_0 else (HidKeyCodes.KEY_1 + (key[0] - '1')).toByte()
                    }
                }
            }
        }
        if (mainKey != HidKeyCodes.KEY_NONE || modifiers != 0.toByte()) {
            repository.sendKey(mainKey, modifiers)
        }
    }

    private fun executeSpecialKey(key: String) {
        val usageId = when (key.uppercase()) {
            "MUTE" -> HidKeyCodes.MEDIA_MUTE
            "VOL_UP" -> HidKeyCodes.MEDIA_VOL_UP
            "VOL_DOWN" -> HidKeyCodes.MEDIA_VOL_DOWN
            "PLAY", "PAUSE" -> HidKeyCodes.MEDIA_PLAY_PAUSE
            "BRIGHT_UP" -> HidKeyCodes.BRIGHTNESS_UP
            "BRIGHT_DOWN" -> HidKeyCodes.BRIGHTNESS_DOWN
            else -> 0.toShort()
        }
        if (usageId != 0.toShort()) {
            repository.sendConsumerKey(usageId)
        }
    }

    private fun executeMacroSequence(sequence: String) {
        // Obsolete - Replaced by Advanced Macro DSL
    }
    
    fun sendMouseMove(dx: Float, dy: Float, buttons: Int = 0, wheel: Int = 0) {
        if (buttons != 0 || wheel != 0) {
            repository.sendMouseMove(dx, dy, buttons, wheel)
            return
        }

        val sensitivity = _trackpadSensitivity.value
        
        // Use raw dx/dy with dual-zone acceleration:
        // Zone 1 (Precision): |delta| < 3px — Linear 1:1 for pixel-perfect placement
        // Zone 2 (Speed):     |delta| >= 3px — Quadratic acceleration for fast traversal
        val precisionThreshold = 3f
        
        val finalDx = if (abs(dx) < precisionThreshold) {
            dx * sensitivity
        } else {
            val excess = abs(dx) - precisionThreshold
            val accelerated = precisionThreshold + excess * (1f + excess * 0.08f)
            sign(dx) * accelerated * sensitivity
        }

        val finalDy = if (abs(dy) < precisionThreshold) {
            dy * sensitivity
        } else {
            val excess = abs(dy) - precisionThreshold
            val accelerated = precisionThreshold + excess * (1f + excess * 0.08f)
            sign(dy) * accelerated * sensitivity
        }
        
        repository.sendMouseMove(finalDx, finalDy, buttons, wheel)
    }

    fun sendPrecisionPoint(normalizedX: Float, normalizedY: Float, isPressed: Boolean) {
        val hidX = (normalizedX * 32767).toInt().coerceIn(0, 32767)
        val hidY = (normalizedY * 32767).toInt().coerceIn(0, 32767)
        repository.sendDigitizerInput(hidX, hidY, isPressed, inRange = true)
    }

    fun resetMouse() {
        repository.resetMouseAccumulator()
    }
    
    fun pauseTextPush() = repository.pauseTextPush()
    fun resumeTextPush() = repository.resumeTextPush()
    fun stopTextPush() = repository.stopTextPush()

    fun onVoiceResult(text: String) {
        if (text.isNotBlank()) {
            repository.sendText(text + " ")
        }
    }

    fun unlockMac() {
        repository.unlockMac(_unlockPassword.value)
    }

    fun sendMacro(macro: String) {
        viewModelScope.launch {
            val parts = macro.split("&&").map { it.trim() }.filter { it.isNotEmpty() }
            for (part in parts) {
                repository.sendText(part)
                repository.sendKey(HidKeyCodes.KEY_ENTER)
                delay(120)
            }
        }
    }

    fun sendKeyCombination(codes: List<Byte>) {
        viewModelScope.launch {
            val modifiers = codes.filter { it in listOf(
                HidKeyCodes.MODIFIER_LEFT_CTRL,
                HidKeyCodes.MODIFIER_LEFT_SHIFT,
                HidKeyCodes.MODIFIER_LEFT_ALT,
                HidKeyCodes.MODIFIER_LEFT_GUI
            ) }
            val mainKey = codes.firstOrNull { it !in modifiers } ?: HidKeyCodes.KEY_NONE
            var combinedMod: Byte = 0
            modifiers.forEach { combinedMod = combinedMod or it }
            repository.sendKey(mainKey, combinedMod)
        }
    }

    // ── Custom Macros ──

    fun addCustomMacro(name: String, command: String) {
        val newList = (_customMacros.value + CustomMacro(name, command)).distinctBy { it.name }
        _customMacros.value = newList
        macrosCache = newList
        saveCustomMacros(newList)
    }

    fun deleteCustomMacro(macro: CustomMacro) {
        val newList = _customMacros.value.filterNot { it.name == macro.name && it.command == macro.command }
        _customMacros.value = newList
        macrosCache = newList
        saveCustomMacros(newList)
    }

    fun exportMacrosJson(): String {
        val array = JSONArray()
        _customMacros.value.forEach {
            array.put(JSONObject().apply {
                put("name", it.name)
                put("command", it.command)
            })
        }
        return array.toString(2)
    }

    fun importMacrosJson(json: String): Boolean {
        return try {
            val array = JSONArray(json)
            val imported = (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                CustomMacro(obj.getString("name"), obj.getString("command"))
            }
            val merged = (_customMacros.value + imported).distinctBy { it.name }
            _customMacros.value = merged
            macrosCache = merged
            saveCustomMacros(merged)
            true
        } catch (e: Exception) { false }
    }

    private fun saveCustomMacros(macros: List<CustomMacro>) {
        val array = JSONArray()
        macros.forEach {
            val obj = JSONObject().apply {
                put("name", it.name)
                put("command", it.command)
            }
            array.put(obj)
        }
        prefs.edit().putString("custom_macros_json", array.toString()).apply()
    }

    private fun loadCustomMacros(): List<CustomMacro> {
        macrosCache?.let { return it }
        val json = prefs.getString("custom_macros_json", null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<CustomMacro>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(CustomMacro(obj.getString("name"), obj.getString("command")))
            }
            macrosCache = list
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── Saved Devices ──

    fun saveDevice(name: String, address: String) {
        val existing = _savedDevices.value.toMutableList()
        existing.removeAll { it.name == name }
        existing.add(0, SavedDevice(name, address, System.currentTimeMillis()))
        if (existing.size > 10) existing.removeAt(existing.size - 1) // Keep max 10
        _savedDevices.value = existing
        saveSavedDevices(existing)
    }

    fun removeSavedDevice(device: SavedDevice) {
        val list = _savedDevices.value.filterNot { it.name == device.name }
        _savedDevices.value = list
        saveSavedDevices(list)
    }

    private fun loadSavedDevices(): List<SavedDevice> {
        val json = prefs.getString("saved_devices_json", null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                SavedDevice(
                    obj.getString("name"),
                    obj.optString("address", ""),
                    obj.optLong("lastConnected", 0L)
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun saveSavedDevices(devices: List<SavedDevice>) {
        val array = JSONArray()
        devices.forEach { d ->
            array.put(JSONObject().apply {
                put("name", d.name)
                put("address", d.address)
                put("lastConnected", d.lastConnected)
            })
        }
        prefs.edit().putString("saved_devices_json", array.toString()).apply()
    }

    // ── Settings ──

    fun setUnlockPassword(password: String) {
        prefs.edit().putString("unlock_password", password).apply()
        _unlockPassword.value = password
    }

    fun setAutoReconnectEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_reconnect_enabled", enabled).apply()
        _autoReconnectEnabled.value = enabled
    }

    fun setTypingSpeed(speed: String) {
        prefs.edit().putString("typing_speed", speed).apply()
        _typingSpeed.value = speed
        updateRepositorySpeed(speed)
    }

    fun setNotificationSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("notification_sync_enabled", enabled).apply()
        _notificationSyncEnabled.value = enabled
    }

    fun setAutoPushEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_push_enabled", enabled).apply()
        _autoPushEnabled.value = enabled
    }

    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("vibration_enabled", enabled).apply()
        _vibrationEnabled.value = enabled
    }

    fun setTrackpadSensitivity(sensitivity: Float) {
        prefs.edit().putFloat("trackpad_sensitivity", sensitivity).apply()
        _trackpadSensitivity.value = sensitivity
    }

    fun setPrecisionModeEnabled(enabled: Boolean) {
        _precisionModeEnabled.value = enabled
    }

    fun setAirMouseEnabled(enabled: Boolean) {
        _airMouseEnabled.value = enabled
        if (enabled) {
            spatialPointerManager.start()
            gyroAirMouse.start()
        } else {
            spatialPointerManager.stop()
            // Keep gyro running for shake detection ONLY if shake is enabled
            if (!_shakeToDisconnectEnabled.value) {
                gyroAirMouse.stop()
            }
        }
    }

    fun setAirMouseSensitivity(value: Float) {
        _airMouseSensitivity.value = value
        spatialPointerManager.sensitivity = value / 10f
        gyroAirMouse.sensitivity = value
        prefs.edit().putFloat("air_mouse_sensitivity", value).apply()
    }

    fun fetchRemoteFiles(path: String = "/") {
        _isRemoteLoading.value = true
        _currentRemotePath.value = path
        val request = JSONObject().apply {
            put("type", "LIST_FILES")
            put("path", path)
        }
        webRtcManager.sendData(request.toString())
    }

    private fun handleRemoteMetadata(jsonStr: String) {
        try {
            val json = JSONObject(jsonStr)
            when (json.optString("type")) {
                "FILE_LIST" -> {
                    val filesArray = json.getJSONArray("files")
                    val list = mutableListOf<RemoteFile>()
                    for (i in 0 until filesArray.length()) {
                        val f = filesArray.getJSONObject(i)
                        list.add(RemoteFile(
                            name = f.getString("name"),
                            path = f.getString("path"),
                            size = f.getLong("size"),
                            isFolder = f.getBoolean("isFolder"),
                            extension = f.optString("extension", ""),
                            modifiedTime = f.optLong("modifiedTime", 0L)
                        ))
                    }
                    _remoteFiles.value = list
                    _isRemoteLoading.value = false
                }
            }
        } catch (e: Exception) {
            _isRemoteLoading.value = false
        }
    }

    private fun updateRepositorySpeed(speed: String) {
        val delay = when(speed) {
            "Too Slow" -> 250L
            "Slow" -> 180L
            "Normal" -> 120L
            "Fast" -> 60L
            "Super Fast" -> 20L
            else -> 120L
        }
        HidDeviceManager.getInstance(getApplication()).typingDelay = delay
    }

    // Air Mouse Calibration State
    private val _isAirMouseCalibrating = MutableStateFlow(false)
    val isAirMouseCalibrating = _isAirMouseCalibrating.asStateFlow()

    fun setAirMouseCalibrating(calibrating: Boolean) {
        _isAirMouseCalibrating.value = calibrating
    }

    // ── Macro Profiles ──
    enum class MacroProfile(val label: String, val icon: ImageVector) {
        GENERAL("General", Icons.Default.Apps),
        BROWSER("Web", Icons.Default.Language),
        DEV("Dev", Icons.Default.Code),
        EDIT("Edit", Icons.Default.Edit)
    }

    private val _activeProfile = MutableStateFlow(MacroProfile.GENERAL)
    val activeProfile = _activeProfile.asStateFlow()

    fun setMacroProfile(profile: MacroProfile) {
        _activeProfile.value = profile
    }

    fun startVoiceRecognition() {
        voiceAssistantManager.startListening()
    }

    fun stopVoiceRecognition() {
        voiceAssistantManager.stopListening()
    }

    fun resetVoiceState() {
        voiceAssistantManager.reset()
    }

    override fun onCleared() {
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        super.onCleared()
    }
}

data class CustomMacro(val name: String, val command: String)
data class SavedDevice(val name: String, val address: String, val lastConnected: Long)
