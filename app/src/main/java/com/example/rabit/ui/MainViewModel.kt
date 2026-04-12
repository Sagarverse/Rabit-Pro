package com.example.rabit.ui

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import com.example.rabit.data.secure.SecureStorage
import android.bluetooth.BluetoothManager
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
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import java.io.OutputStreamWriter
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.Properties
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
    private val secureStorage = SecureStorage(application)
    private val prefs = application.getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)

    val connectionState: StateFlow<HidDeviceManager.ConnectionState> = repository.connectionState
    val scannedDevices: StateFlow<Set<BluetoothDevice>> = repository.scannedDevices
    val isScanning: StateFlow<Boolean> = repository.isScanning
    val isPushPaused: StateFlow<Boolean> = repository.isPushPaused
    val isTextPushing = repository.isTextPushing
    val knownWorkstations = repository.knownWorkstations

    private val _precisionModeEnabled = MutableStateFlow(false)
    val precisionModeEnabled = _precisionModeEnabled.asStateFlow()

    private val _unlockPassword = MutableStateFlow(secureStorage.getUnlockPassword() ?: "")
    val unlockPassword = _unlockPassword.asStateFlow()
    private val _hasUnlockPassword = MutableStateFlow(_unlockPassword.value.isNotBlank())
    val hasUnlockPassword = _hasUnlockPassword.asStateFlow()
    private val _macPassword = MutableStateFlow(secureStorage.getMacPassword() ?: "")
    val macPassword = _macPassword.asStateFlow()

    private val _autoReconnectEnabled = MutableStateFlow(prefs.getBoolean("auto_reconnect_enabled", true))
    val autoReconnectEnabled = _autoReconnectEnabled.asStateFlow()

    private val _proximityAutoUnlockEnabled = MutableStateFlow(prefs.getBoolean("proximity_auto_unlock_enabled", false))
    val proximityAutoUnlockEnabled = _proximityAutoUnlockEnabled.asStateFlow()
    private val _proximityNearRssi = MutableStateFlow(prefs.getInt("proximity_near_rssi", -62))
    val proximityNearRssi = _proximityNearRssi.asStateFlow()
    private val _proximityFarRssi = MutableStateFlow(prefs.getInt("proximity_far_rssi", -80))
    val proximityFarRssi = _proximityFarRssi.asStateFlow()
    private val _proximityCooldownSec = MutableStateFlow(prefs.getInt("proximity_cooldown_sec", 12))
    val proximityCooldownSec = _proximityCooldownSec.asStateFlow()
    private val _proximityRequirePhoneUnlock = MutableStateFlow(prefs.getBoolean("proximity_require_phone_unlock", true))
    val proximityRequirePhoneUnlock = _proximityRequirePhoneUnlock.asStateFlow()
    private val _proximityTargetAddress = MutableStateFlow(prefs.getString("proximity_target_address", "") ?: "")
    val proximityTargetAddress = _proximityTargetAddress.asStateFlow()

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

    // Feature visibility toggles (non-technical friendly app simplification)
    private val _featureWebBridgeVisible = MutableStateFlow(prefs.getBoolean("feature_web_bridge_visible", true))
    val featureWebBridgeVisible = _featureWebBridgeVisible.asStateFlow()
    private val _featureAutomationVisible = MutableStateFlow(prefs.getBoolean("feature_automation_visible", true))
    val featureAutomationVisible = _featureAutomationVisible.asStateFlow()
    private val _featureAssistantVisible = MutableStateFlow(prefs.getBoolean("feature_assistant_visible", true))
    val featureAssistantVisible = _featureAssistantVisible.asStateFlow()
    private val _featureSnippetsVisible = MutableStateFlow(prefs.getBoolean("feature_snippets_visible", true))
    val featureSnippetsVisible = _featureSnippetsVisible.asStateFlow()
    private val _featureShortcutsVisible = MutableStateFlow(prefs.getBoolean("feature_shortcuts_visible", true))
    val featureShortcutsVisible = _featureShortcutsVisible.asStateFlow()
    private val _featureWakeOnLanVisible = MutableStateFlow(prefs.getBoolean("feature_wake_on_lan_visible", true))
    val featureWakeOnLanVisible = _featureWakeOnLanVisible.asStateFlow()
    private val _featureSshTerminalVisible = MutableStateFlow(prefs.getBoolean("feature_ssh_terminal_visible", true))
    val featureSshTerminalVisible = _featureSshTerminalVisible.asStateFlow()

    private val _webBridgeRunning = MutableStateFlow(RabitNetworkServer.isRunning)
    val isWebBridgeRunning: StateFlow<Boolean> = _webBridgeRunning.asStateFlow()

    private val _webBridgePin = MutableStateFlow(RabitNetworkServer.currentPin)
    val webBridgePin: StateFlow<String> = _webBridgePin.asStateFlow()

    // Vibration toggle & Presets
    private val _vibrationEnabled = MutableStateFlow(prefs.getBoolean("vibration_enabled", true))
    val vibrationEnabled = _vibrationEnabled.asStateFlow()

    private val _hapticPreset = MutableStateFlow(prefs.getString("haptic_preset", "Mechanical") ?: "Mechanical")
    val hapticPreset = _hapticPreset.asStateFlow()

    fun setHapticPreset(preset: String) {
        _hapticPreset.value = preset
        prefs.edit().putString("haptic_preset", preset).apply()
        performHapticFeedback(preset)
    }

    private fun performHapticFeedback(preset: String) {
        if (!_vibrationEnabled.value) return
        viewModelScope.launch {
            val vibrator = getVibratorCompat()
            if (vibrator.hasVibrator()) {
                when (preset) {
                    "Soft" -> vibrator.vibrate(VibrationEffect.createOneShot(10, 50))
                    "Mechanical" -> vibrator.vibrate(VibrationEffect.createOneShot(25, 180))
                    "Sharp" -> vibrator.vibrate(VibrationEffect.createOneShot(40, 255))
                }
            }
        }
    }

    private fun getVibratorCompat(): Vibrator {
        val app = getApplication<Application>()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = app.getSystemService(VibratorManager::class.java)
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            app.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

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
    private val _hostProfilePreset = MutableStateFlow(
        HostProfilePreset.valueOf(prefs.getString("host_profile_preset", HostProfilePreset.AUTO.name) ?: HostProfilePreset.AUTO.name)
    )
    val hostProfilePreset = _hostProfilePreset.asStateFlow()

    // Voice & Speech Engine
    private val _ttsPitch = MutableStateFlow(prefs.getFloat("tts_pitch", 1.0f))
    val ttsPitch = _ttsPitch.asStateFlow()

    private val _ttsSpeechRate = MutableStateFlow(prefs.getFloat("tts_speech_rate", 1.0f))
    val ttsSpeechRate = _ttsSpeechRate.asStateFlow()

    fun setTtsPitch(value: Float) {
        _ttsPitch.value = value
        prefs.edit().putFloat("tts_pitch", value).apply()
    }

    fun setTtsSpeechRate(value: Float) {
        _ttsSpeechRate.value = value
        prefs.edit().putFloat("tts_speech_rate", value).apply()
    }

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

    private val _activeApp = MutableStateFlow<String?>(null)
    val activeApp = _activeApp.asStateFlow()

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
                if (type == "ACTIVE_APP") {
                    _activeApp.value = data as String
                }
            }
        }
    }

    // Shared Files for Hub (Phone -> Mac)
    private val _sharedFiles = MutableStateFlow<List<android.net.Uri>>(emptyList())
    val sharedFiles = _sharedFiles.asStateFlow()
    private val _sharedTransferQueue = MutableStateFlow<List<SharedTransferItem>>(emptyList())
    val sharedTransferQueue = _sharedTransferQueue.asStateFlow()

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
        _sharedFiles.value = (_sharedFiles.value + uri).distinct()
        val metadata = resolveSharedFileMetadata(uri)
        val item = SharedTransferItem(
            id = uri.toString().hashCode().toString(),
            uri = uri,
            name = metadata.first,
            sizeBytes = metadata.second,
            status = TransferQueueStatus.Ready,
            progress = 100,
            addedAt = System.currentTimeMillis()
        )
        _sharedTransferQueue.value = (_sharedTransferQueue.value.filterNot { it.id == item.id } + item)
            .sortedByDescending { it.addedAt }
    }

    fun removeSharedFile(uri: android.net.Uri) {
        _sharedFiles.value = _sharedFiles.value - uri
        val id = uri.toString().hashCode().toString()
        _sharedTransferQueue.value = _sharedTransferQueue.value.filterNot { it.id == id }
    }

    fun clearSharedFiles() {
        _sharedFiles.value = emptyList()
        _sharedTransferQueue.value = emptyList()
    }

    private val _deviceIp = MutableStateFlow("0.0.0.0")
    val deviceIp = _deviceIp.asStateFlow()

    // Wake-on-LAN
    private val _wolMacAddress = MutableStateFlow(prefs.getString("wol_mac_address", "") ?: "")
    val wolMacAddress = _wolMacAddress.asStateFlow()
    private val _wolBroadcastIp = MutableStateFlow(prefs.getString("wol_broadcast_ip", "255.255.255.255") ?: "255.255.255.255")
    val wolBroadcastIp = _wolBroadcastIp.asStateFlow()
    private val _wolPort = MutableStateFlow(prefs.getInt("wol_port", 9))
    val wolPort = _wolPort.asStateFlow()
    private val _wolStatus = MutableStateFlow("Idle")
    val wolStatus = _wolStatus.asStateFlow()

    // Native SSH Terminal
    private val _sshHost = MutableStateFlow(prefs.getString("ssh_host", "") ?: "")
    val sshHost = _sshHost.asStateFlow()
    private val _sshPort = MutableStateFlow(prefs.getInt("ssh_port", 22))
    val sshPort = _sshPort.asStateFlow()
    private val _sshUser = MutableStateFlow(prefs.getString("ssh_user", "") ?: "")
    val sshUser = _sshUser.asStateFlow()
    private val _sshPassword = MutableStateFlow(prefs.getString("ssh_password", "") ?: "")
    val sshPassword = _sshPassword.asStateFlow()
    private val _sshConnected = MutableStateFlow(false)
    val sshConnected = _sshConnected.asStateFlow()
    private val _sshTerminalLines = MutableStateFlow<List<String>>(listOf("Rabit SSH terminal ready."))
    val sshTerminalLines = _sshTerminalLines.asStateFlow()
    private val _sshStatus = MutableStateFlow("Disconnected")
    val sshStatus = _sshStatus.asStateFlow()

    private var sshSession: Session? = null
    private var sshChannel: ChannelShell? = null
    private var sshWriter: OutputStreamWriter? = null
    private var sshReaderJob: Job? = null
    private var sshCommandJob: Job? = null

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

    fun setWolMacAddress(value: String) {
        _wolMacAddress.value = value
        prefs.edit().putString("wol_mac_address", value).apply()
    }

    fun setWolBroadcastIp(value: String) {
        _wolBroadcastIp.value = value
        prefs.edit().putString("wol_broadcast_ip", value).apply()
    }

    fun setWolPort(value: Int) {
        val clamped = value.coerceIn(1, 65535)
        _wolPort.value = clamped
        prefs.edit().putInt("wol_port", clamped).apply()
    }

    fun sendWakeOnLan() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mac = parseMacAddress(_wolMacAddress.value)
                val packetBytes = ByteArray(6 + 16 * mac.size)
                for (i in 0 until 6) packetBytes[i] = 0xFF.toByte()
                for (i in 6 until packetBytes.size step mac.size) {
                    mac.copyInto(packetBytes, i)
                }

                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    val address = InetAddress.getByName(_wolBroadcastIp.value.ifBlank { "255.255.255.255" })
                    val packet = DatagramPacket(packetBytes, packetBytes.size, address, _wolPort.value)
                    socket.send(packet)
                }

                _wolStatus.value = "Magic packet sent to ${_wolBroadcastIp.value}:${_wolPort.value}"
            } catch (e: Exception) {
                _wolStatus.value = "Failed: ${e.message ?: "invalid MAC or network"}"
            }
        }
    }

    fun setSshHost(value: String) {
        _sshHost.value = value
        prefs.edit().putString("ssh_host", value).apply()
    }

    fun setSshPort(value: Int) {
        val clamped = value.coerceIn(1, 65535)
        _sshPort.value = clamped
        prefs.edit().putInt("ssh_port", clamped).apply()
    }

    fun setSshUser(value: String) {
        _sshUser.value = value
        prefs.edit().putString("ssh_user", value).apply()
    }

    fun setSshPassword(value: String) {
        _sshPassword.value = value
        prefs.edit().putString("ssh_password", value).apply()
    }

    fun connectSsh() {
        if (_sshConnected.value) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                appendTerminalLine("Connecting to ${_sshHost.value}:${_sshPort.value} ...")
                _sshStatus.value = "Connecting"
                val jsch = JSch()
                val session = jsch.getSession(_sshUser.value, _sshHost.value, _sshPort.value)
                session.setPassword(_sshPassword.value)
                val config = Properties().apply { put("StrictHostKeyChecking", "no") }
                session.setConfig(config)
                session.connect(10_000)

                val channel = session.openChannel("shell") as ChannelShell
                channel.setPty(true)
                val input = channel.inputStream
                val writer = OutputStreamWriter(channel.outputStream)
                channel.connect(8_000)

                sshSession = session
                sshChannel = channel
                sshWriter = writer
                _sshConnected.value = true
                _sshStatus.value = "Connected"
                appendTerminalLine("Connected. Type commands below.")

                sshReaderJob?.cancel()
                sshReaderJob = viewModelScope.launch(Dispatchers.IO) {
                    val buffer = ByteArray(1024)
                    while (channel.isConnected) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        val chunk = String(buffer, 0, read)
                        chunk.lines().filter { it.isNotBlank() }.forEach { appendTerminalLine(it) }
                    }
                }
            } catch (e: Exception) {
                _sshStatus.value = "Connection failed"
                appendTerminalLine("SSH error: ${e.message}")
                disconnectSsh()
            }
        }
    }

    fun sendSshCommand(command: String) {
        if (command.isBlank()) return
        if (!_sshConnected.value) {
            appendTerminalLine("Not connected. Connect first.")
            return
        }
        sshCommandJob?.cancel()
        sshCommandJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                appendTerminalLine("$ $command")
                val session = sshSession
                if (session == null || !session.isConnected) {
                    appendTerminalLine("Session dropped. Reconnect SSH.")
                    _sshConnected.value = false
                    _sshStatus.value = "Disconnected"
                    return@launch
                }

                val exec = session.openChannel("exec") as ChannelExec
                exec.setCommand(command)
                exec.setPty(true)
                exec.inputStream = null
                val stderrBuffer = ByteArrayOutputStream()
                exec.setErrStream(stderrBuffer)
                val stdout = exec.inputStream
                exec.connect(8_000)

                val outText = stdout.readBytes().toString(Charsets.UTF_8)
                val errText = stderrBuffer.toString(Charsets.UTF_8.name())
                if (outText.isNotBlank()) {
                    outText.lines().filter { it.isNotBlank() }.forEach { appendTerminalLine(it) }
                }
                if (errText.isNotBlank()) {
                    errText.lines().filter { it.isNotBlank() }.forEach { appendTerminalLine("ERR: $it") }
                }
                appendTerminalLine("[exit ${exec.exitStatus}]")
                exec.disconnect()
            } catch (e: Exception) {
                appendTerminalLine("Send failed: ${e.message}")
            }
        }
    }

    fun disconnectSsh() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                sshWriter?.apply {
                    write("exit\n")
                    flush()
                }
            }
            sshReaderJob?.cancel()
            sshReaderJob = null
            sshCommandJob?.cancel()
            sshCommandJob = null
            runCatching { sshChannel?.disconnect() }
            runCatching { sshSession?.disconnect() }
            sshChannel = null
            sshSession = null
            sshWriter = null
            _sshConnected.value = false
            _sshStatus.value = "Disconnected"
        }
    }

    fun clearSshTerminal() {
        _sshTerminalLines.value = listOf("Rabit SSH terminal cleared.")
    }

    private fun appendTerminalLine(line: String) {
        val clean = line.trimEnd()
        if (clean.isBlank()) return
        val newList = (_sshTerminalLines.value + clean).takeLast(500)
        _sshTerminalLines.value = newList
    }

    private fun parseMacAddress(raw: String): ByteArray {
        val hex = raw.replace(":", "").replace("-", "").trim()
        require(hex.length == 12) { "MAC must be 12 hex characters" }
        return ByteArray(6) { idx ->
            hex.substring(idx * 2, idx * 2 + 2).toInt(16).toByte()
        }
    }

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            "auto_push_enabled" -> _autoPushEnabled.value = sharedPreferences.getBoolean(key, false)
            "auto_reconnect_enabled" -> _autoReconnectEnabled.value = sharedPreferences.getBoolean(key, true)
            "notification_sync_enabled" -> _notificationSyncEnabled.value = sharedPreferences.getBoolean(key, false)
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
                    if (_hostProfilePreset.value == HostProfilePreset.AUTO) {
                        applyHostProfilePreset(guessPresetForDevice(state.deviceName), persist = false)
                    }
                }
            }
        }

        // Auto-reconnect on startup if enabled
        if (_autoReconnectEnabled.value) {
            viewModelScope.launch {
                delay(1000) // Give service time to start
                _savedDevices.value.firstOrNull()?.let { device ->
                    val bluetoothManager = getApplication<Application>().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                    val bluetoothAdapter = bluetoothManager.adapter
                    val bondedDevice = try {
                        bluetoothAdapter?.bondedDevices?.find { it.name == device.name }
                    } catch (e: Exception) { null }
                    
                    if (bondedDevice != null && connectionState.value is HidDeviceManager.ConnectionState.Disconnected) {
                        repository.connectWithRetry(bondedDevice)
                    }
                }
            }
        }

        if (_p2pEnabled.value) {
            viewModelScope.launch {
                delay(800)
                startP2PHosting()
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
                            _localIp.value = addr.hostAddress ?: "0.0.0.0"
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
        if (_p2pEnabled.value && webRtcManager.peerId.value != null) return
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
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
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
        val bluetoothManager = getApplication<Application>().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        val device = adapter?.getRemoteDevice(workstation.address)
        if (device != null) {
            repository.connectWithRetry(device, maxRetries = 3)
        }
    }

    fun sendKey(keyCode: Byte) {
        performHapticFeedback(_hapticPreset.value)
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
        val pass = _macPassword.value.ifBlank { _unlockPassword.value }
        repository.unlockMac(pass)
    }

    fun sendMacro(macro: String) {
        executeMacro2Script(macro)
    }

    fun launchMacApp(appName: String) {
        if (appName.isBlank()) return
        viewModelScope.launch {
            sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_SPACE))
            delay(120)
            sendText(appName)
            delay(120)
            sendKey(HidKeyCodes.KEY_ENTER)
        }
    }

    fun sendSystemShortcut(shortcut: SystemShortcut) {
        when (shortcut) {
            SystemShortcut.MUTE -> repository.sendConsumerKey(HidKeyCodes.MEDIA_MUTE)
            SystemShortcut.VOLUME_UP -> repository.sendConsumerKey(HidKeyCodes.MEDIA_VOL_UP)
            SystemShortcut.VOLUME_DOWN -> repository.sendConsumerKey(HidKeyCodes.MEDIA_VOL_DOWN)
            SystemShortcut.PLAY_PAUSE -> repository.sendConsumerKey(HidKeyCodes.MEDIA_PLAY_PAUSE)
            SystemShortcut.BRIGHTNESS_UP -> repository.sendConsumerKey(HidKeyCodes.BRIGHTNESS_UP)
            SystemShortcut.BRIGHTNESS_DOWN -> repository.sendConsumerKey(HidKeyCodes.BRIGHTNESS_DOWN)
            SystemShortcut.LOCK_SCREEN -> sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_CTRL, HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_Q))
        }
    }

    fun runCustomMacro(macro: CustomMacro) {
        if (!macro.onlyWhenApp.isNullOrBlank()) {
            val active = _activeApp.value.orEmpty()
            if (!active.contains(macro.onlyWhenApp, ignoreCase = true)) return
        }
        executeMacro2Script(macro.command, macro.cooldownMs)
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
                put("category", it.category)
                put("tags", JSONArray(it.tags))
                put("cooldownMs", it.cooldownMs)
                put("onlyWhenApp", it.onlyWhenApp)
            })
        }
        return array.toString(2)
    }

    fun importMacrosJson(json: String): Boolean {
        return try {
            val array = JSONArray(json)
            val imported = (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                CustomMacro(
                    name = obj.getString("name"),
                    command = obj.getString("command"),
                    category = obj.optString("category", "General"),
                    tags = obj.optJSONArray("tags")?.let { tagsArray ->
                        (0 until tagsArray.length()).map { idx -> tagsArray.optString(idx) }
                    } ?: emptyList(),
                    cooldownMs = obj.optLong("cooldownMs", 0L),
                    onlyWhenApp = obj.optString("onlyWhenApp").ifBlank { null }
                )
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
                put("category", it.category)
                put("tags", JSONArray(it.tags))
                put("cooldownMs", it.cooldownMs)
                put("onlyWhenApp", it.onlyWhenApp)
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
                list.add(
                    CustomMacro(
                        name = obj.getString("name"),
                        command = obj.getString("command"),
                        category = obj.optString("category", "General"),
                        tags = obj.optJSONArray("tags")?.let { tagsArray ->
                            (0 until tagsArray.length()).map { idx -> tagsArray.optString(idx) }
                        } ?: emptyList(),
                        cooldownMs = obj.optLong("cooldownMs", 0L),
                        onlyWhenApp = obj.optString("onlyWhenApp").ifBlank { null }
                    )
                )
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
        secureStorage.saveUnlockPassword(password)
        _unlockPassword.value = password
        _hasUnlockPassword.value = password.isNotBlank()
    }

    fun setMacPassword(password: String) {
        secureStorage.saveMacPassword(password)
        _macPassword.value = password
    }

    fun clearMacPassword() {
        secureStorage.saveMacPassword("")
        _macPassword.value = ""
    }

    fun clearUnlockPassword() {
        secureStorage.saveUnlockPassword("")
        _unlockPassword.value = ""
        _hasUnlockPassword.value = false
    }

    fun setAutoReconnectEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_reconnect_enabled", enabled).apply()
        _autoReconnectEnabled.value = enabled
    }

    fun setProximityAutoUnlockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("proximity_auto_unlock_enabled", enabled).apply()
        _proximityAutoUnlockEnabled.value = enabled
        val intent = Intent(getApplication<Application>(), HidService::class.java).apply {
            action = HidService.ACTION_UPDATE_PROXIMITY_SMART_LOCK
            putExtra("enabled", enabled)
        }
        getApplication<Application>().startService(intent)
    }

    fun setProximityNearRssi(value: Int) {
        val clamped = value.coerceIn(-90, -40)
        prefs.edit().putInt("proximity_near_rssi", clamped).apply()
        _proximityNearRssi.value = clamped
    }

    fun setProximityFarRssi(value: Int) {
        val clamped = value.coerceIn(-100, -50)
        prefs.edit().putInt("proximity_far_rssi", clamped).apply()
        _proximityFarRssi.value = clamped
    }

    fun setProximityCooldownSec(value: Int) {
        val clamped = value.coerceIn(3, 60)
        prefs.edit().putInt("proximity_cooldown_sec", clamped).apply()
        _proximityCooldownSec.value = clamped
    }

    fun setProximityRequirePhoneUnlock(enabled: Boolean) {
        prefs.edit().putBoolean("proximity_require_phone_unlock", enabled).apply()
        _proximityRequirePhoneUnlock.value = enabled
    }

    fun setProximityTargetAddress(address: String) {
        prefs.edit().putString("proximity_target_address", address).apply()
        _proximityTargetAddress.value = address
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

    fun setFeatureWebBridgeVisible(visible: Boolean) {
        prefs.edit().putBoolean("feature_web_bridge_visible", visible).apply()
        _featureWebBridgeVisible.value = visible
    }

    fun setFeatureAutomationVisible(visible: Boolean) {
        prefs.edit().putBoolean("feature_automation_visible", visible).apply()
        _featureAutomationVisible.value = visible
    }

    fun setFeatureAssistantVisible(visible: Boolean) {
        prefs.edit().putBoolean("feature_assistant_visible", visible).apply()
        _featureAssistantVisible.value = visible
    }

    fun setFeatureSnippetsVisible(visible: Boolean) {
        prefs.edit().putBoolean("feature_snippets_visible", visible).apply()
        _featureSnippetsVisible.value = visible
    }

    fun setFeatureShortcutsVisible(visible: Boolean) {
        prefs.edit().putBoolean("feature_shortcuts_visible", visible).apply()
        _featureShortcutsVisible.value = visible
    }

    fun setFeatureWakeOnLanVisible(visible: Boolean) {
        prefs.edit().putBoolean("feature_wake_on_lan_visible", visible).apply()
        _featureWakeOnLanVisible.value = visible
    }

    fun setFeatureSshTerminalVisible(visible: Boolean) {
        prefs.edit().putBoolean("feature_ssh_terminal_visible", visible).apply()
        _featureSshTerminalVisible.value = visible
    }

    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("vibration_enabled", enabled).apply()
        _vibrationEnabled.value = enabled
    }

    fun setTrackpadSensitivity(sensitivity: Float) {
        prefs.edit().putFloat("trackpad_sensitivity", sensitivity).apply()
        _trackpadSensitivity.value = sensitivity
    }

    fun applyHostProfilePreset(preset: HostProfilePreset, persist: Boolean = true) {
        _hostProfilePreset.value = preset
        when (preset) {
            HostProfilePreset.AUTO -> Unit
            HostProfilePreset.MAC -> {
                setTypingSpeed("Fast")
                setTrackpadSensitivity(1.4f)
                setAirMouseSensitivity(18f)
            }
            HostProfilePreset.WINDOWS -> {
                setTypingSpeed("Normal")
                setTrackpadSensitivity(1.8f)
                setAirMouseSensitivity(20f)
            }
            HostProfilePreset.LINUX -> {
                setTypingSpeed("Fast")
                setTrackpadSensitivity(1.6f)
                setAirMouseSensitivity(19f)
            }
        }
        if (persist) {
            prefs.edit().putString("host_profile_preset", preset.name).apply()
        }
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

    private fun executeMacro2Script(script: String, cooldownMs: Long = 0L) {
        val commands = script
            .split("\n", "&&")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        viewModelScope.launch {
            for (cmd in commands) {
                when {
                    cmd.startsWith("WAIT(", ignoreCase = true) && cmd.endsWith(")") -> {
                        val ms = cmd.removePrefix("WAIT(").removeSuffix(")").trim().toLongOrNull() ?: 120L
                        delay(ms.coerceIn(0L, 60_000L))
                    }
                    cmd.startsWith("TEXT(", ignoreCase = true) && cmd.endsWith(")") -> {
                        repository.sendText(cmd.removePrefix("TEXT(").removeSuffix(")"))
                    }
                    cmd.startsWith("KEY(", ignoreCase = true) && cmd.endsWith(")") -> {
                        executeKeyCombo(cmd.removePrefix("KEY(").removeSuffix(")"))
                    }
                    cmd.startsWith("MEDIA(", ignoreCase = true) && cmd.endsWith(")") -> {
                        executeSpecialKey(cmd.removePrefix("MEDIA(").removeSuffix(")"))
                    }
                    else -> {
                        repository.sendText(cmd)
                        repository.sendKey(HidKeyCodes.KEY_ENTER)
                    }
                }
                delay(120)
            }
            if (cooldownMs > 0) delay(cooldownMs)
        }
    }

    private fun guessPresetForDevice(deviceName: String): HostProfilePreset {
        val lower = deviceName.lowercase()
        return when {
            lower.contains("mac") || lower.contains("apple") -> HostProfilePreset.MAC
            lower.contains("windows") || lower.contains("surface") || lower.contains("dell") || lower.contains("hp") || lower.contains("lenovo") -> HostProfilePreset.WINDOWS
            lower.contains("ubuntu") || lower.contains("linux") || lower.contains("fedora") || lower.contains("debian") -> HostProfilePreset.LINUX
            else -> HostProfilePreset.WINDOWS
        }
    }

    private fun resolveSharedFileMetadata(uri: Uri): Pair<String, Long> {
        return try {
            var name = "File"
            var size = 0L
            getApplication<Application>().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex >= 0) name = cursor.getString(nameIndex)
                    if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
                }
            }
            name to size
        } catch (e: Exception) {
            "File" to 0L
        }
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

    enum class SystemShortcut {
        MUTE,
        VOLUME_UP,
        VOLUME_DOWN,
        PLAY_PAUSE,
        BRIGHTNESS_UP,
        BRIGHTNESS_DOWN,
        LOCK_SCREEN
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
        disconnectSsh()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        super.onCleared()
    }
}

data class CustomMacro(
    val name: String,
    val command: String,
    val category: String = "General",
    val tags: List<String> = emptyList(),
    val cooldownMs: Long = 0L,
    val onlyWhenApp: String? = null
)
data class SavedDevice(val name: String, val address: String, val lastConnected: Long)
enum class HostProfilePreset { AUTO, MAC, WINDOWS, LINUX }
enum class TransferQueueStatus { Queued, Ready, Failed }
data class SharedTransferItem(
    val id: String,
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val status: TransferQueueStatus,
    val progress: Int,
    val addedAt: Long
)
