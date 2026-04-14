package com.example.rabit.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rabit.data.airplay.AirPlayStateBus
import com.example.rabit.data.airplay.AlacFrameDecoder
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.data.bluetooth.HidService
import com.example.rabit.data.gemini.LocalLlmManager
import com.example.rabit.data.network.RabitNetworkServer
import com.example.rabit.data.network.WebRtcManager
import com.example.rabit.data.repository.KeyboardRepositoryImpl
import com.example.rabit.data.secure.SecureStorage
import com.example.rabit.data.sensors.GyroscopeAirMouse
import com.example.rabit.data.sensors.SpatialPointerManager
import com.example.rabit.data.voice.VoiceAssistantManager
import com.example.rabit.data.voice.VoiceState
import com.example.rabit.domain.model.HidKeyCodes
import com.example.rabit.domain.model.RemoteFile
import com.example.rabit.domain.model.Workstation
import com.example.rabit.domain.repository.KeyboardRepository
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Properties
import java.util.UUID
import kotlin.experimental.or
import kotlin.math.abs
import kotlin.math.log10
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
    private val wifiAudioSink = com.example.rabit.data.airplay.AudioTrackPcmSink()
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
    private val _passwordVaultEntries = MutableStateFlow(loadPasswordVaultEntries())
    val passwordVaultEntries = _passwordVaultEntries.asStateFlow()

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
    private val _proximityLiveRssi = MutableStateFlow(prefs.getInt("proximity_live_rssi", -120))
    val proximityLiveRssi = _proximityLiveRssi.asStateFlow()
    private val _proximityLiveDistanceMeters = MutableStateFlow(prefs.getFloat("proximity_live_distance_m", -1f))
    val proximityLiveDistanceMeters = _proximityLiveDistanceMeters.asStateFlow()
    private val _proximityLiveLastSeenMs = MutableStateFlow(prefs.getLong("proximity_live_last_seen_ms", 0L))
    val proximityLiveLastSeenMs = _proximityLiveLastSeenMs.asStateFlow()
    private val _proximityUnlockArmed = MutableStateFlow(prefs.getBoolean("proximity_unlock_armed", true))
    val proximityUnlockArmed = _proximityUnlockArmed.asStateFlow()
    private val _proximityMacLockStateGuess = MutableStateFlow(prefs.getString("proximity_mac_lock_state_guess", "UNKNOWN") ?: "UNKNOWN")
    val proximityMacLockStateGuess = _proximityMacLockStateGuess.asStateFlow()

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

    // Media Deck / Now Playing
    private val _nowPlayingTitle = MutableStateFlow("No track")
    val nowPlayingTitle = _nowPlayingTitle.asStateFlow()
    private val _nowPlayingArtist = MutableStateFlow("Connect companion for metadata")
    val nowPlayingArtist = _nowPlayingArtist.asStateFlow()
    private val _nowPlayingAlbum = MutableStateFlow("")
    val nowPlayingAlbum = _nowPlayingAlbum.asStateFlow()
    private val _nowPlayingArtworkBase64 = MutableStateFlow<String?>(null)
    val nowPlayingArtworkBase64 = _nowPlayingArtworkBase64.asStateFlow()
    private val _nowPlayingTimestamp = MutableStateFlow(0L)
    val nowPlayingTimestamp = _nowPlayingTimestamp.asStateFlow()

    // AirPlay Receiver (experimental RAOP service discovery + service lifecycle)
    private val _airPlayReceiverEnabled = MutableStateFlow(prefs.getBoolean("airplay_receiver_enabled", false))
    val airPlayReceiverEnabled = _airPlayReceiverEnabled.asStateFlow()
    private val _airPlayStatus = MutableStateFlow("Idle")
    val airPlayStatus = _airPlayStatus.asStateFlow()
    private val _airPlayStatusLog = MutableStateFlow<List<String>>(listOf("Idle"))
    val airPlayStatusLog = _airPlayStatusLog.asStateFlow()
    private val _airPlayNativeReadiness = MutableStateFlow("LOW")
    val airPlayNativeReadiness = _airPlayNativeReadiness.asStateFlow()
    private val _airPlayAutoRecoveryEnabled = MutableStateFlow(prefs.getBoolean("airplay_auto_recovery_enabled", true))
    val airPlayAutoRecoveryEnabled = _airPlayAutoRecoveryEnabled.asStateFlow()
    private val _airPlayAutoFallbackEnabled = MutableStateFlow(prefs.getBoolean("airplay_auto_fallback_enabled", true))
    val airPlayAutoFallbackEnabled = _airPlayAutoFallbackEnabled.asStateFlow()
    private val _airPlayHandshakeStage = MutableStateFlow("IDLE")
    val airPlayHandshakeStage = _airPlayHandshakeStage.asStateFlow()
    private val _airPlayLastRtspMethod = MutableStateFlow("-")
    val airPlayLastRtspMethod = _airPlayLastRtspMethod.asStateFlow()
    private val _airPlayServerPorts = MutableStateFlow("a=- c=- t=-")
    val airPlayServerPorts = _airPlayServerPorts.asStateFlow()
    private val _airPlayClientPorts = MutableStateFlow("a=- c=- t=-")
    val airPlayClientPorts = _airPlayClientPorts.asStateFlow()
    private val _airPlayPacketStats = MutableStateFlow("delivered=0 reorder=0 drop=0")
    val airPlayPacketStats = _airPlayPacketStats.asStateFlow()
    private val _airPlayAlacCapability = MutableStateFlow("UNKNOWN")
    val airPlayAlacCapability = _airPlayAlacCapability.asStateFlow()
    private var airPlayRecoveryInProgress = false
    private var lastAirPlayRecoveryAtMs = 0L
    private var airPlayFallbackInProgress = false
    private var lastAirPlayFallbackAtMs = 0L
    private val _wifiAudioStatus = MutableStateFlow("Idle")
    val wifiAudioStatus = _wifiAudioStatus.asStateFlow()
    private val _wifiAudioStreamActive = MutableStateFlow(false)
    val wifiAudioStreamActive = _wifiAudioStreamActive.asStateFlow()

    private val _webBridgeRunning = MutableStateFlow(RabitNetworkServer.isRunning)
    val isWebBridgeRunning: StateFlow<Boolean> = _webBridgeRunning.asStateFlow()

    private val _webBridgePin = MutableStateFlow(RabitNetworkServer.currentPin)
    val webBridgePin: StateFlow<String> = _webBridgePin.asStateFlow()
    private val _webBridgeSelfTestStatus = MutableStateFlow("Not tested")
    val webBridgeSelfTestStatus: StateFlow<String> = _webBridgeSelfTestStatus.asStateFlow()
    private val _webBridgeSelfTestInProgress = MutableStateFlow(false)
    val webBridgeSelfTestInProgress: StateFlow<Boolean> = _webBridgeSelfTestInProgress.asStateFlow()

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

    // Mouse Jiggler (Caffeine)
    private val _isMouseJigglerEnabled = MutableStateFlow(prefs.getBoolean("mouse_jiggler_enabled", false))
    val isMouseJigglerEnabled = _isMouseJigglerEnabled.asStateFlow()
    private var mouseJigglerJob: Job? = null
    private var proximityTelemetryJob: Job? = null

    fun setMouseJigglerEnabled(enabled: Boolean) {
        _isMouseJigglerEnabled.value = enabled
        prefs.edit().putBoolean("mouse_jiggler_enabled", enabled).apply()
        updateMouseJiggler()
    }

    private fun updateMouseJiggler() {
        val isEnabled = _isMouseJigglerEnabled.value
        val isConnected = connectionState.value is HidDeviceManager.ConnectionState.Connected
        if (isEnabled && isConnected) {
            if (mouseJigglerJob == null || mouseJigglerJob?.isActive != true) {
                mouseJigglerJob = viewModelScope.launch(Dispatchers.IO) {
                    var right = true
                    while (isActive) {
                        try {
                            repository.sendMouseMove(if (right) 1f else -1f, 0f)
                            right = !right
                        } catch (e: Exception) {
                            Log.w("MainViewModel", "Mouse jiggler send failed", e)
                        }
                        delay(30_000)
                    }
                }
            }
        } else {
            mouseJigglerJob?.cancel()
            mouseJigglerJob = null
        }
    }

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

    private data class IncomingUploadSession(
        val transferId: String,
        val fileName: String,
        val mimeType: String,
        val sizeBytes: Long,
        val tempFile: File,
        val output: FileOutputStream,
        var bytesReceived: Long = 0L
    )

    private val incomingUploadSessions = mutableMapOf<String, IncomingUploadSession>()

    private val _isRemoteLoading = MutableStateFlow(false)
    val isRemoteLoading = _isRemoteLoading.asStateFlow()

    private val _currentRemotePath = MutableStateFlow("/")
    val currentRemotePath = _currentRemotePath.asStateFlow()

    // Phase 10: Advanced Customization & Biometric states
    private val _biometricLockEnabled = MutableStateFlow(prefs.getBoolean("biometric_lock_enabled", false))
    val biometricLockEnabled = _biometricLockEnabled.asStateFlow()
    private val _biometricMacAutofillEnabled = MutableStateFlow(prefs.getBoolean("biometric_mac_autofill_enabled", true))
    val biometricMacAutofillEnabled = _biometricMacAutofillEnabled.asStateFlow()
    private val _macAutofillPreEnter = MutableStateFlow(prefs.getBoolean("mac_autofill_pre_enter", true))
    val macAutofillPreEnter = _macAutofillPreEnter.asStateFlow()
    private val _macAutofillPostEnter = MutableStateFlow(prefs.getBoolean("mac_autofill_post_enter", true))
    val macAutofillPostEnter = _macAutofillPostEnter.asStateFlow()

    private val _shakeToDisconnectEnabled = MutableStateFlow(prefs.getBoolean("shake_to_disconnect_enabled", false))
    val shakeToDisconnectEnabled = _shakeToDisconnectEnabled.asStateFlow()

    private val _stealthModeEnabled = MutableStateFlow(prefs.getBoolean("stealth_mode_enabled", false))
    val stealthModeEnabled = _stealthModeEnabled.asStateFlow()

    private val _activeApp = MutableStateFlow<String?>(null)
    val activeApp = _activeApp.asStateFlow()

    init {
        RabitNetworkServer.nowPlayingReceiver = { payload ->
            setNowPlayingPreview(
                title = payload.title,
                artist = payload.artist,
                album = payload.album,
                artworkBase64 = payload.artworkBase64
            )
        }
        RabitNetworkServer.audioStreamStartReceiver = { payload ->
            wifiAudioSink.configure(
                sampleRate = payload.sampleRate.coerceIn(8_000, 96_000),
                channels = payload.channels.coerceIn(1, 2)
            )
            _wifiAudioStreamActive.value = true
            _wifiAudioStatus.value = "Streaming from ${payload.source} (${payload.sampleRate} Hz)"
        }
        RabitNetworkServer.audioStreamChunkReceiver = { payload ->
            try {
                val pcm = Base64.decode(payload.pcm16leBase64, Base64.DEFAULT)
                if (pcm.isNotEmpty()) {
                    wifiAudioSink.writePcm16le(pcm)
                    _wifiAudioStreamActive.value = true
                }
            } catch (_: Exception) {
                _wifiAudioStatus.value = "Audio chunk decode error"
            }
        }
        RabitNetworkServer.audioStreamStopReceiver = { payload ->
            wifiAudioSink.stop()
            _wifiAudioStreamActive.value = false
            _wifiAudioStatus.value = "Stopped (${payload.reason})"
        }

        spatialPointerManager.onPointerUpdate = { dx, dy ->
            if (_airMouseEnabled.value) {
                repository.sendMouseMove(dx, dy)
            }
        }

        gyroAirMouse.onShakeDetected = {
            if (_shakeToDisconnectEnabled.value) {
                when (connectionState.value) {
                    is HidDeviceManager.ConnectionState.Connected -> repository.disconnect()
                    is HidDeviceManager.ConnectionState.Disconnected -> {
                        if (_autoReconnectEnabled.value) reconnectLastSavedDevice()
                    }
                    else -> Unit
                }
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

        viewModelScope.launch {
            AirPlayStateBus.status.collect { status ->
                _airPlayStatus.value = status
                val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(System.currentTimeMillis())
                _airPlayStatusLog.value = (listOf("[$ts] $status") + _airPlayStatusLog.value).distinct().take(24)
                parseAirPlayDiagnostics(status)
                _airPlayHandshakeStage.value = mapAirPlayStage(status)

                maybeAutoRecoverAirPlay(status)
                maybeAutoEnableAirPlayFallback(status)

                _airPlayNativeReadiness.value = when {
                    status.contains("ALAC decode active", ignoreCase = true) -> "MEDIUM"
                    status.contains("Unsupported RAOP transport", ignoreCase = true) -> "LOW"
                    status.contains("Unsupported RAOP codec", ignoreCase = true) -> "LOW"
                    status.contains("AirPlay fallback required", ignoreCase = true) -> "LOW"
                    status.contains("Auto fallback:", ignoreCase = true) -> "LOW-MEDIUM"
                    status.contains("Compatibility mode: RAOP-only", ignoreCase = true) -> "LOW-MEDIUM"
                    status.contains("ALAC stream detected", ignoreCase = true) -> "LOW"
                    status.contains("RTP stream stalled", ignoreCase = true) -> "LOW-MEDIUM"
                    status.contains("Waiting for RTP packets", ignoreCase = true) -> "LOW-MEDIUM"
                    status.contains("RTP packets delivered", ignoreCase = true) -> "MEDIUM"
                    status.contains("RAOP RECORD started", ignoreCase = true) -> "MEDIUM"
                    status.contains("RAOP SETUP", ignoreCase = true) -> "LOW-MEDIUM"
                    status.contains("RAOP ANNOUNCE", ignoreCase = true) -> "LOW-MEDIUM"
                    status.contains("AirPlay ready on port", ignoreCase = true) -> "LOW"
                    status.contains("Idle", ignoreCase = true) -> "LOW"
                    else -> _airPlayNativeReadiness.value
                }
            }
        }

        refreshAirPlayDecoderCapability()
    }

    fun refreshAirPlayDecoderCapability() {
        _airPlayAlacCapability.value = AlacFrameDecoder.capabilityLabel()
    }

    private fun maybeAutoRecoverAirPlay(status: String) {
        if (!_airPlayAutoRecoveryEnabled.value) return
        if (!_airPlayReceiverEnabled.value) return
        if (!status.contains("stalled", ignoreCase = true)) return
        if (airPlayRecoveryInProgress) return

        val now = System.currentTimeMillis()
        if (now - lastAirPlayRecoveryAtMs < 15_000) return

        airPlayRecoveryInProgress = true
        lastAirPlayRecoveryAtMs = now
        viewModelScope.launch {
            val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(System.currentTimeMillis())
            _airPlayStatusLog.value = (listOf("[$ts] Auto-recovery: restarting receiver") + _airPlayStatusLog.value).take(24)
            stopAirPlayReceiver()
            delay(900)
            startAirPlayReceiver()
            delay(1200)
            airPlayRecoveryInProgress = false
        }
    }

    private fun maybeAutoEnableAirPlayFallback(status: String) {
        if (!_airPlayAutoFallbackEnabled.value) return
        if (status.contains("ALAC decode active", ignoreCase = true)) return
        val hasNativeAlacDecoder = _airPlayAlacCapability.value.startsWith("AVAILABLE")
        val needsFallback =
            status.contains("Unsupported RAOP codec", ignoreCase = true) ||
                status.contains("AirPlay fallback required", ignoreCase = true) ||
                status.contains("ALAC decode unavailable", ignoreCase = true) ||
                (status.contains("ALAC stream detected", ignoreCase = true) && !hasNativeAlacDecoder)
        if (!needsFallback) return
        if (airPlayFallbackInProgress) return
        if (_webBridgeRunning.value) return

        val now = System.currentTimeMillis()
        if (now - lastAirPlayFallbackAtMs < 20_000) return

        airPlayFallbackInProgress = true
        lastAirPlayFallbackAtMs = now
        viewModelScope.launch {
            val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(System.currentTimeMillis())
            _airPlayStatusLog.value = (
                listOf("[$ts] Auto fallback: starting Web Bridge for phone speaker playback") +
                    _airPlayStatusLog.value
                ).take(24)
            startWebBridge()
            delay(900)
            _webBridgeRunning.value = RabitNetworkServer.isRunning
            val doneTs = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(System.currentTimeMillis())
            _airPlayStatusLog.value = (
                listOf("[$doneTs] Auto fallback: Web Bridge ${if (_webBridgeRunning.value) "running" else "failed to start"}") +
                    _airPlayStatusLog.value
                ).take(24)
            airPlayFallbackInProgress = false
        }
    }

    fun setAirPlayAutoRecoveryEnabled(enabled: Boolean) {
        _airPlayAutoRecoveryEnabled.value = enabled
        prefs.edit().putBoolean("airplay_auto_recovery_enabled", enabled).apply()
    }

    fun setAirPlayAutoFallbackEnabled(enabled: Boolean) {
        _airPlayAutoFallbackEnabled.value = enabled
        prefs.edit().putBoolean("airplay_auto_fallback_enabled", enabled).apply()
    }

    private fun mapAirPlayStage(status: String): String {
        return when {
            status.contains("Unsupported RAOP transport", ignoreCase = true) -> "FAILED"
            status.contains("Unsupported RAOP codec", ignoreCase = true) -> "FAILED"
            status.contains("AirPlay fallback required", ignoreCase = true) -> "FAILED"
            status.contains("AirPlay start failed", ignoreCase = true) -> "FAILED"
            status.contains("Idle", ignoreCase = true) -> "IDLE"
            status.contains("RTP stream stalled", ignoreCase = true) -> "STALLED"
            status.contains("RTP packets delivered", ignoreCase = true) -> "STREAMING"
            status.contains("Waiting for RTP packets", ignoreCase = true) -> "WAITING_RTP"
            status.contains("RAOP RECORD started", ignoreCase = true) -> "RECORDING"
            status.contains("RAOP SETUP", ignoreCase = true) -> "SETUP"
            status.contains("RAOP ANNOUNCE", ignoreCase = true) -> "ANNOUNCED"
            status.contains("Client connected", ignoreCase = true) -> "CONNECTED"
            status.contains("RAOP advertised", ignoreCase = true) ||
                status.contains("AirPlay service advertised", ignoreCase = true) ||
                status.contains("AirPlay ready on port", ignoreCase = true) -> "ADVERTISED"
            else -> _airPlayHandshakeStage.value
        }
    }

    private fun parseAirPlayDiagnostics(status: String) {
        Regex("RTSP\\s+([A-Z_]+)\\s+from", RegexOption.IGNORE_CASE).find(status)?.let { match ->
            _airPlayLastRtspMethod.value = match.groupValues[1].uppercase(Locale.getDefault())
        }

        Regex("RAOP UDP ready a=(\\d+) c=(\\d+) t=(\\d+)", RegexOption.IGNORE_CASE).find(status)?.let { match ->
            _airPlayServerPorts.value = "a=${match.groupValues[1]} c=${match.groupValues[2]} t=${match.groupValues[3]}"
        }

        Regex("RAOP client ports a=(-?\\d+) c=(-?\\d+) t=(-?\\d+)", RegexOption.IGNORE_CASE).find(status)?.let { match ->
            _airPlayClientPorts.value = "a=${match.groupValues[1]} c=${match.groupValues[2]} t=${match.groupValues[3]}"
        }

        Regex("RTP packets delivered=(\\d+) reorder=(\\d+) drop=(\\d+)", RegexOption.IGNORE_CASE).find(status)?.let { match ->
            _airPlayPacketStats.value = "delivered=${match.groupValues[1]} reorder=${match.groupValues[2]} drop=${match.groupValues[3]}"
        }

        if (status.contains("RAOP FLUSH", ignoreCase = true) || status.contains("Idle", ignoreCase = true)) {
            _airPlayPacketStats.value = "delivered=0 reorder=0 drop=0"
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

    fun setBiometricMacAutofillEnabled(enabled: Boolean) {
        _biometricMacAutofillEnabled.value = enabled
        prefs.edit().putBoolean("biometric_mac_autofill_enabled", enabled).apply()
    }

    fun setMacAutofillPreEnter(enabled: Boolean) {
        _macAutofillPreEnter.value = enabled
        prefs.edit().putBoolean("mac_autofill_pre_enter", enabled).apply()
    }

    fun setMacAutofillPostEnter(enabled: Boolean) {
        _macAutofillPostEnter.value = enabled
        prefs.edit().putBoolean("mac_autofill_post_enter", enabled).apply()
    }

    fun setShakeToDisconnectEnabled(enabled: Boolean) {
        _shakeToDisconnectEnabled.value = enabled
        prefs.edit().putBoolean("shake_to_disconnect_enabled", enabled).apply()
    }

    fun setStealthModeEnabled(enabled: Boolean) {
        _stealthModeEnabled.value = enabled
        prefs.edit().putBoolean("stealth_mode_enabled", enabled).apply()
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
    private val _sshTerminalLines = MutableStateFlow<List<String>>(listOf("Hackie SSH terminal ready."))
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
                _wolStatus.value = "Sending magic packet..."
                val mac = parseMacAddress(_wolMacAddress.value)
                val packetBytes = ByteArray(6 + 16 * mac.size)
                for (i in 0 until 6) packetBytes[i] = 0xFF.toByte()
                for (i in 6 until packetBytes.size step mac.size) {
                    mac.copyInto(packetBytes, i)
                }

                val targets = linkedSetOf<InetAddress>()
                val preferredBroadcast = _wolBroadcastIp.value.ifBlank { "255.255.255.255" }
                runCatching { InetAddress.getByName(preferredBroadcast) }
                    .onSuccess { targets.add(it) }

                val interfaces = runCatching { java.net.NetworkInterface.getNetworkInterfaces() }.getOrNull()
                if (interfaces != null) {
                    while (interfaces.hasMoreElements()) {
                        val iface = interfaces.nextElement() ?: continue
                        if (!iface.isUp || iface.isLoopback) continue
                        iface.interfaceAddresses
                            .mapNotNull { it.broadcast }
                            .forEach { targets.add(it) }
                    }
                }

                if (targets.isEmpty()) {
                    targets.add(InetAddress.getByName("255.255.255.255"))
                }

                val failures = mutableListOf<String>()
                var successCount = 0
                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    socket.reuseAddress = true
                    targets.forEach { address ->
                        try {
                            socket.send(DatagramPacket(packetBytes, packetBytes.size, address, _wolPort.value))
                            successCount += 1
                        } catch (sendError: Exception) {
                            failures += "${address.hostAddress}: ${sendError.message ?: "send failed"}"
                        }
                    }
                }

                if (successCount > 0) {
                    val sentAt = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(System.currentTimeMillis())
                    _wolStatus.value = "Success: Sent $successCount packet route(s) at $sentAt on port ${_wolPort.value}."
                } else {
                    val details = failures.take(2).joinToString(" | ").ifBlank { "No reachable broadcast route" }
                    _wolStatus.value = "Failed: $details"
                }
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
        _sshTerminalLines.value = listOf("Hackie SSH terminal cleared.")
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
        startProximityTelemetryRefresh()
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
                updateMouseJiggler()
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
                    val bondedDevice = findBondedDeviceByName(bluetoothAdapter, device.name)
                    
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

    private fun startProximityTelemetryRefresh() {
        proximityTelemetryJob?.cancel()
        proximityTelemetryJob = viewModelScope.launch {
            while (isActive) {
                _proximityLiveRssi.value = prefs.getInt("proximity_live_rssi", -120)
                _proximityLiveDistanceMeters.value = prefs.getFloat("proximity_live_distance_m", -1f)
                _proximityLiveLastSeenMs.value = prefs.getLong("proximity_live_last_seen_ms", 0L)
                _proximityUnlockArmed.value = prefs.getBoolean("proximity_unlock_armed", true)
                _proximityMacLockStateGuess.value = prefs.getString("proximity_mac_lock_state_guess", "UNKNOWN") ?: "UNKNOWN"
                delay(1500)
            }
        }
    }

    private fun rssiToDistanceMeters(rssi: Int): Float {
        val txPowerAt1m = -59.0
        val pathLossExponent = 2.0
        val distance = 10.0.pow((txPowerAt1m - rssi) / (10.0 * pathLossExponent))
        return distance.toFloat().coerceIn(0.1f, 20f)
    }

    private fun distanceMetersToRssi(distanceMeters: Float): Int {
        val txPowerAt1m = -59.0
        val pathLossExponent = 2.0
        val safeDistance = distanceMeters.coerceAtLeast(0.1f).toDouble()
        val rssi = txPowerAt1m - (10.0 * pathLossExponent * log10(safeDistance))
        return rssi.toInt().coerceIn(-90, -40)
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
            _webBridgeSelfTestStatus.value = "Not tested"
        }
    }

    fun stopWebBridge() {
        val intent = Intent(getApplication<Application>(), HidService::class.java).apply {
            action = HidService.ACTION_STOP_WEB_BRIDGE
        }
        getApplication<Application>().startService(intent)
        _webBridgeEnabled.value = false
        _webBridgeRunning.value = false
        _webBridgeSelfTestStatus.value = "Bridge stopped"
        clearSharedFiles() // Clear on stop for security
        stopP2PHosting() // Also stop P2P when bridge stops
    }

    fun runWebBridgeConnectivitySelfTest() {
        if (_webBridgeSelfTestInProgress.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _webBridgeSelfTestInProgress.value = true
            try {
                _webBridgeRunning.value = RabitNetworkServer.isRunning
                if (!_webBridgeRunning.value) {
                    _webBridgeSelfTestStatus.value = "FAIL: Web Bridge is not running"
                    return@launch
                }

                refreshLocalIp()
                delay(200)
                val host = _localIp.value
                if (host == "0.0.0.0") {
                    _webBridgeSelfTestStatus.value = "FAIL: Could not resolve phone Wi-Fi IP"
                    return@launch
                }

                val rootUrl = "http://$host:8080/"
                val rootCode = runCatching {
                    val conn = (URL(rootUrl).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 2500
                        readTimeout = 2500
                    }
                    conn.responseCode.also { conn.disconnect() }
                }.getOrElse {
                    _webBridgeSelfTestStatus.value = "FAIL: Port 8080 unreachable (${it.message ?: "network error"})"
                    return@launch
                }

                if (rootCode !in 200..399) {
                    _webBridgeSelfTestStatus.value = "FAIL: Unexpected bridge root response ($rootCode)"
                    return@launch
                }

                val pin = _webBridgePin.value.ifBlank { RabitNetworkServer.currentPin }
                val authCode = runCatching {
                    val conn = (URL("http://$host:8080/auth").openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        connectTimeout = 3000
                        readTimeout = 3000
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json")
                        setRequestProperty("X-Device-Id", "rabit-self-test")
                    }
                    val payload = "{\"pin\":\"$pin\"}".toByteArray(Charsets.UTF_8)
                    conn.outputStream.use { it.write(payload) }
                    conn.responseCode.also { conn.disconnect() }
                }.getOrElse {
                    _webBridgeSelfTestStatus.value = "FAIL: /auth probe failed (${it.message ?: "network error"})"
                    return@launch
                }

                val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(System.currentTimeMillis())
                _webBridgeSelfTestStatus.value = if (authCode == 200) {
                    "PASS @ $ts: Reachable + auth OK for helper"
                } else {
                    "FAIL @ $ts: /auth returned $authCode (refresh PIN and retry)"
                }
            } finally {
                _webBridgeSelfTestInProgress.value = false
            }
        }
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

    fun sendMediaPlayPause() = repository.sendConsumerKey(HidKeyCodes.MEDIA_PLAY_PAUSE)
    fun sendMediaNextTrack() = repository.sendConsumerKey(HidKeyCodes.MEDIA_NEXT)
    fun sendMediaPreviousTrack() = repository.sendConsumerKey(HidKeyCodes.MEDIA_PREVIOUS)
    fun sendMediaVolumeUp() = repository.sendConsumerKey(HidKeyCodes.MEDIA_VOL_UP)
    fun sendMediaVolumeDown() = repository.sendConsumerKey(HidKeyCodes.MEDIA_VOL_DOWN)

    fun requestNowPlayingFromHost() {
        val request = JSONObject().apply {
            put("type", "NOW_PLAYING_REQUEST")
            put("source", "android_media_deck")
            put("ts", System.currentTimeMillis())
        }
        webRtcManager.sendData(request.toString())
    }

    fun setNowPlayingPreview(title: String, artist: String, album: String = "", artworkBase64: String? = null) {
        _nowPlayingTitle.value = title.ifBlank { "No track" }
        _nowPlayingArtist.value = artist.ifBlank { "Unknown artist" }
        _nowPlayingAlbum.value = album
        _nowPlayingArtworkBase64.value = artworkBase64
        _nowPlayingTimestamp.value = System.currentTimeMillis()
    }

    fun startAirPlayReceiver() {
        refreshAirPlayDecoderCapability()
        val intent = Intent(getApplication<Application>(), com.example.rabit.data.airplay.AirPlayReceiverService::class.java).apply {
            action = com.example.rabit.data.airplay.AirPlayReceiverService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
        _airPlayReceiverEnabled.value = true
        _airPlayStatus.value = "Discovery preview: advertising _raop._tcp on local Wi-Fi"
        val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(System.currentTimeMillis())
        _airPlayStatusLog.value = (listOf("[$ts] Discovery preview start requested") + _airPlayStatusLog.value).take(24)
        prefs.edit().putBoolean("airplay_receiver_enabled", true).apply()
    }

    fun stopAirPlayReceiver() {
        val intent = Intent(getApplication<Application>(), com.example.rabit.data.airplay.AirPlayReceiverService::class.java).apply {
            action = com.example.rabit.data.airplay.AirPlayReceiverService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
        _airPlayReceiverEnabled.value = false
        _airPlayStatus.value = "Idle"
        val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(System.currentTimeMillis())
        _airPlayStatusLog.value = (listOf("[$ts] Discovery preview stop requested") + _airPlayStatusLog.value).take(24)
        prefs.edit().putBoolean("airplay_receiver_enabled", false).apply()
    }

    fun restartAirPlayReceiver() {
        stopAirPlayReceiver()
        viewModelScope.launch {
            delay(500)
            startAirPlayReceiver()
        }
    }

    fun clearAirPlayStatusLog() {
        _airPlayStatusLog.value = listOf("Idle")
    }

    fun playAirPlayTestTone() {
        val intent = Intent(getApplication<Application>(), com.example.rabit.data.airplay.AirPlayReceiverService::class.java).apply {
            action = com.example.rabit.data.airplay.AirPlayReceiverService.ACTION_TEST_TONE
        }
        getApplication<Application>().startService(intent)
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
            if (!hasBluetoothConnectPermission(context)) return
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(enableBtIntent)
            } catch (_: SecurityException) {
            }
        }
    }

    fun connect(device: BluetoothDevice) = repository.connect(device)
    fun connectWithRetry(device: BluetoothDevice) = repository.connectWithRetry(device)

    private fun reconnectLastSavedDevice() {
        if (!_autoReconnectEnabled.value) return
        val target = _savedDevices.value.firstOrNull() ?: return
        val bluetoothManager = getApplication<Application>().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        val bonded = findBondedDeviceByNameOrAddress(bluetoothAdapter, target.name, target.address)
        if (bonded != null) {
            repository.connectWithRetry(bonded)
        }
    }

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
        macroJob?.cancel()
        macroJob = viewModelScope.launch {
            _genieState.value = GenieState.Thinking
            try {
                val script = generateMacroScript(intent)
                val commands = parseMacroCommands(script)
                if (commands.isEmpty()) {
                    _genieState.value = GenieState.Error("Could not generate a macro for this intent.")
                    return@launch
                }

                executeAdvancedMacro(commands)
                _genieState.value = GenieState.Success(intent)
                delay(1500)
                _genieState.value = GenieState.Idle
            } catch (_: CancellationException) {
                _genieState.value = GenieState.Idle
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

    private data class MacroCommand(val type: String, val value: String)

    private fun parseMacroCommands(script: String): List<MacroCommand> {
        val commandRegex = Regex("\\[(K|T|W|S):([^\\]]+)\\]")
        return commandRegex.findAll(script).map { match ->
            MacroCommand(type = match.groupValues[1], value = match.groupValues[2])
        }.toList()
    }

    private suspend fun generateMacroScript(intent: String): String {
        val ggufPath = prefs.getString("gguf_path", null)
        if (!ggufPath.isNullOrBlank()) {
            val initialized = localLlmManager.initialize(ggufPath)
            if (initialized) {
                val prompt = """
                    Convert the user intent into only command tags.
                    Allowed tags: [K:...], [T:...], [W:...], [S:...]
                    User Intent: "$intent"
                    Output only tags with no explanation.
                """.trimIndent()
                val response = localLlmManager.generateResponse(prompt).trim()
                if (response.contains("[")) {
                    return response
                }
            }
        }
        return buildFallbackMacroScript(intent)
    }

    private fun buildFallbackMacroScript(intent: String): String {
        val lower = intent.lowercase(Locale.getDefault())
        return when {
            "mute" in lower && "lock" in lower -> "[S:MUTE][W:120][K:CTRL+GUI+Q]"
            "mute" in lower -> "[S:MUTE]"
            "volume up" in lower || "increase volume" in lower -> "[S:VOL_UP]"
            "volume down" in lower || "decrease volume" in lower -> "[S:VOL_DOWN]"
            "next" in lower && "song" in lower -> "[S:PLAY]"
            "play" in lower || "pause" in lower -> "[S:PLAY]"
            "lock" in lower && "screen" in lower -> "[K:CTRL+GUI+Q]"
            "spotlight" in lower -> "[K:GUI+SPACE]"
            "open" in lower -> {
                val appName = intent.substringAfter("open", "").trim().ifBlank { "Safari" }
                "[K:GUI+SPACE][W:200][T:$appName][W:250][K:ENTER]"
            }
            else -> "[T:$intent]"
        }
    }

    private suspend fun executeAdvancedMacro(commands: List<MacroCommand>) {
        commands.forEachIndexed { index, cmd ->
            val progress = (index + 1).toFloat() / commands.size

            when (cmd.type) {
                "K" -> {
                    _genieState.value = GenieState.Executing("Keys: ${cmd.value}", progress)
                    executeKeyCombo(cmd.value)
                }
                "T" -> {
                    _genieState.value = GenieState.Executing("Typing...", progress)
                    repository.sendText(cmd.value)
                }
                "W" -> {
                    val ms = cmd.value.toLongOrNull()?.coerceIn(0L, 5_000L) ?: 120L
                    _genieState.value = GenieState.Executing("Waiting $ms ms", progress)
                    delay(ms)
                }
                "S" -> {
                    _genieState.value = GenieState.Executing("Consumer: ${cmd.value}", progress)
                    executeSpecialKey(cmd.value)
                }
            }
            delay(120)
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
        repository.unlockMac(
            password = pass,
            pressEnterBefore = _macAutofillPreEnter.value,
            pressEnterAfter = _macAutofillPostEnter.value
        )
    }

    fun sendStoredMacPasswordToHost(): String? {
        if (connectionState.value !is HidDeviceManager.ConnectionState.Connected) {
            return "Connect to your Mac first."
        }

        val stored = secureStorage.getMacPassword().orEmpty().ifBlank { secureStorage.getUnlockPassword().orEmpty() }
        if (stored.isBlank()) {
            return "Set your Mac password in Settings first."
        }

        repository.unlockMac(
            password = stored,
            pressEnterBefore = _macAutofillPreEnter.value,
            pressEnterAfter = _macAutofillPostEnter.value
        )
        return null
    }

    fun addOrUpdateVaultEntry(appName: String, username: String, password: String, notes: String = "") {
        val normalizedApp = appName.trim()
        val normalizedPassword = password.trim()
        if (normalizedApp.isBlank() || normalizedPassword.isBlank()) return

        val now = System.currentTimeMillis()
        val existing = _passwordVaultEntries.value.firstOrNull {
            it.appName.equals(normalizedApp, ignoreCase = true)
        }

        val entry = VaultEntry(
            id = existing?.id ?: UUID.randomUUID().toString(),
            appName = normalizedApp,
            username = username.trim(),
            password = normalizedPassword,
            notes = notes.trim(),
            updatedAtMs = now
        )

        val updated = _passwordVaultEntries.value
            .filterNot { it.id == entry.id }
            .toMutableList()
            .apply { add(0, entry) }

        _passwordVaultEntries.value = updated
        savePasswordVaultEntries(updated)
    }

    fun deleteVaultEntry(id: String) {
        val updated = _passwordVaultEntries.value.filterNot { it.id == id }
        _passwordVaultEntries.value = updated
        savePasswordVaultEntries(updated)
    }

    fun sendVaultPasswordToHost(entryId: String): String? {
        if (connectionState.value !is HidDeviceManager.ConnectionState.Connected) {
            return "Connect to your Mac first."
        }

        val entry = _passwordVaultEntries.value.firstOrNull { it.id == entryId }
            ?: return "Password entry not found."

        if (entry.password.isBlank()) {
            return "Selected entry has no password."
        }

        repository.unlockMac(
            password = entry.password,
            pressEnterBefore = _macAutofillPreEnter.value,
            pressEnterAfter = _macAutofillPostEnter.value
        )
        return null
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

    fun runEmergencyAction(action: EmergencyAction) {
        when (action) {
            EmergencyAction.LOCK_MACHINE -> {
                sendSystemShortcut(SystemShortcut.LOCK_SCREEN)
                _emergencyStatus.value = "Lock command sent."
            }

            EmergencyAction.STOP_AUDIO -> {
                sendSystemShortcut(SystemShortcut.MUTE)
                sendMediaPlayPause()
                stopAirPlayReceiver()
                _emergencyStatus.value = "Audio emergency stop sent."
            }

            EmergencyAction.CLEAR_CLIPBOARD -> {
                val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("rabit-clear", ""))

                viewModelScope.launch(Dispatchers.IO) {
                    val ok = runEmergencySshCommand("pbcopy < /dev/null")
                    _emergencyStatus.value = if (ok) {
                        "Phone and Mac clipboard cleared."
                    } else {
                        "Phone clipboard cleared. Connect SSH to clear Mac clipboard too."
                    }
                }
            }

            EmergencyAction.KILL_INTERNET_ADAPTER -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val cmd = """
                                                IFACE=${'$'}(networksetup -listallhardwareports | awk '/Wi-Fi|AirPort/{getline; print ${'$'}2; exit}')
                                                if [ -n "${'$'}IFACE" ]; then
                                                    networksetup -setairportpower "${'$'}IFACE" off
                        else
                          ifconfig en0 down || ifconfig en1 down
                        fi
                    """.trimIndent()
                    val ok = runEmergencySshCommand(cmd)
                    _emergencyStatus.value = if (ok) {
                        "Mac network adapter disabled."
                    } else {
                        "SSH not connected or command failed. Open SSH Terminal and connect host first."
                    }
                }
            }

            EmergencyAction.CLOSE_SENSITIVE_APPS -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val cmd = "osascript -e 'tell application \"Safari\" to quit' -e 'tell application \"Google Chrome\" to quit' -e 'tell application \"Arc\" to quit' -e 'tell application \"Slack\" to quit' -e 'tell application \"Discord\" to quit' -e 'tell application \"Messages\" to quit' -e 'tell application \"Mail\" to quit' -e 'tell application \"Notes\" to quit'"
                    val ok = runEmergencySshCommand(cmd)
                    if (ok) {
                        _emergencyStatus.value = "Sensitive apps closed on Mac."
                    } else {
                        sendKeyCombination(listOf(HidKeyCodes.MODIFIER_LEFT_GUI, HidKeyCodes.KEY_Q))
                        _emergencyStatus.value = "SSH unavailable. Sent Cmd+Q to close the current active app."
                    }
                }
            }
        }
    }

    private fun runEmergencySshCommand(command: String): Boolean {
        return try {
            val session = sshSession
            if (session == null || !session.isConnected) return false

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

            val exitCode = exec.exitStatus
            exec.disconnect()
            exitCode == 0
        } catch (e: Exception) {
            appendTerminalLine("Emergency action failed: ${e.message}")
            false
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

    private fun loadPasswordVaultEntries(): List<VaultEntry> {
        return try {
            val raw = secureStorage.getPasswordVaultJson()
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        VaultEntry(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            appName = obj.optString("appName"),
                            username = obj.optString("username"),
                            password = obj.optString("password"),
                            notes = obj.optString("notes"),
                            updatedAtMs = obj.optLong("updatedAtMs", 0L)
                        )
                    )
                }
            }.sortedByDescending { it.updatedAtMs }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun savePasswordVaultEntries(entries: List<VaultEntry>) {
        val arr = JSONArray()
        entries.forEach { entry ->
            arr.put(
                JSONObject().apply {
                    put("id", entry.id)
                    put("appName", entry.appName)
                    put("username", entry.username)
                    put("password", entry.password)
                    put("notes", entry.notes)
                    put("updatedAtMs", entry.updatedAtMs)
                }
            )
        }
        secureStorage.savePasswordVaultJson(arr.toString())
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

    fun setProximityUnlockDistanceMeters(distanceMeters: Float) {
        val clampedDistance = distanceMeters.coerceIn(0.5f, 8.0f)
        val nearRssi = distanceMetersToRssi(clampedDistance)
        val farRssi = (nearRssi - 18).coerceIn(-100, -50)
        prefs.edit()
            .putInt("proximity_near_rssi", nearRssi)
            .putInt("proximity_far_rssi", farRssi)
            .apply()
        _proximityNearRssi.value = nearRssi
        _proximityFarRssi.value = farRssi
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

    fun proximityNearDistanceMeters(): Float {
        return rssiToDistanceMeters(_proximityNearRssi.value)
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
                "LIST_FILES" -> {
                    sendCurrentFileListToPeer()
                }
                "DOWNLOAD_FILE" -> {
                    val path = json.optString("path", "")
                    val requestId = json.optString("requestId", "")
                    if (path.isBlank()) {
                        webRtcManager.sendData(
                            JSONObject().apply {
                                put("type", "FILE_DOWNLOAD_ACK")
                                put("requestId", requestId)
                                put("accepted", false)
                                put("message", "Download path missing.")
                            }.toString()
                        )
                        webRtcManager.sendData(
                            JSONObject().apply {
                                put("type", "ERROR")
                                put("message", "Download path missing.")
                            }.toString()
                        )
                    } else {
                        val targetUri = _sharedFiles.value.firstOrNull { it.toString() == path } ?: runCatching { Uri.parse(path) }.getOrNull()
                        if (targetUri == null) {
                            webRtcManager.sendData(
                                JSONObject().apply {
                                    put("type", "FILE_DOWNLOAD_ACK")
                                    put("requestId", requestId)
                                    put("accepted", false)
                                    put("message", "Requested file is not available on phone.")
                                }.toString()
                            )
                            webRtcManager.sendData(
                                JSONObject().apply {
                                    put("type", "ERROR")
                                    put("message", "Requested file is not available on phone.")
                                }.toString()
                            )
                        } else {
                            webRtcManager.sendData(
                                JSONObject().apply {
                                    put("type", "FILE_DOWNLOAD_ACK")
                                    put("requestId", requestId)
                                    put("accepted", true)
                                }.toString()
                            )
                            viewModelScope.launch(Dispatchers.IO) {
                                streamSharedFileOverP2p(targetUri)
                            }
                        }
                    }
                }
                "UPLOAD_START" -> {
                    val transferId = json.optString("transferId", "")
                    val fileName = json.optString("fileName", "incoming_file")
                    val mimeType = json.optString("mimeType", "application/octet-stream")
                    val sizeBytes = json.optLong("sizeBytes", 0L)

                    if (transferId.isBlank()) {
                        webRtcManager.sendData(
                            JSONObject().apply {
                                put("type", "ERROR")
                                put("message", "Upload rejected: missing transfer id.")
                            }.toString()
                        )
                        return
                    }

                    val safeName = fileName.replace(Regex("[^a-zA-Z0-9._ -]"), "_")
                    val temp = File(getApplication<Application>().cacheDir, "p2p_upload_${transferId}")
                    runCatching { if (temp.exists()) temp.delete() }

                    try {
                        val stream = FileOutputStream(temp)
                        incomingUploadSessions[transferId] = IncomingUploadSession(
                            transferId = transferId,
                            fileName = safeName,
                            mimeType = mimeType,
                            sizeBytes = sizeBytes,
                            tempFile = temp,
                            output = stream
                        )

                        webRtcManager.sendData(
                            JSONObject().apply {
                                put("type", "UPLOAD_ACK")
                                put("transferId", transferId)
                                put("fileName", safeName)
                            }.toString()
                        )
                    } catch (e: Exception) {
                        webRtcManager.sendData(
                            JSONObject().apply {
                                put("type", "ERROR")
                                put("message", "Upload start failed: ${e.message ?: "unknown"}")
                            }.toString()
                        )
                    }
                }
                "UPLOAD_CHUNK" -> {
                    val transferId = json.optString("transferId", "")
                    val session = incomingUploadSessions[transferId]
                    if (session == null) {
                        webRtcManager.sendData(
                            JSONObject().apply {
                                put("type", "ERROR")
                                put("message", "Upload chunk rejected: unknown transfer.")
                            }.toString()
                        )
                        return
                    }

                    val b64 = json.optString("dataBase64", "")
                    if (b64.isEmpty()) return

                    try {
                        val chunk = Base64.decode(b64, Base64.DEFAULT)
                        session.output.write(chunk)
                        session.bytesReceived += chunk.size

                        if (session.bytesReceived % (64 * 1024) < chunk.size) {
                            webRtcManager.sendData(
                                JSONObject().apply {
                                    put("type", "UPLOAD_PROGRESS")
                                    put("transferId", transferId)
                                    put("fileName", session.fileName)
                                    put("bytesReceived", session.bytesReceived)
                                    put("sizeBytes", session.sizeBytes)
                                }.toString()
                            )
                        }
                    } catch (e: Exception) {
                        cleanupIncomingUpload(transferId)
                        webRtcManager.sendData(
                            JSONObject().apply {
                                put("type", "ERROR")
                                put("message", "Upload chunk failed: ${e.message ?: "unknown"}")
                            }.toString()
                        )
                    }
                }
                "UPLOAD_END" -> {
                    val transferId = json.optString("transferId", "")
                    val session = incomingUploadSessions[transferId]
                    if (session == null) {
                        webRtcManager.sendData(
                            JSONObject().apply {
                                put("type", "ERROR")
                                put("message", "Upload end rejected: unknown transfer.")
                            }.toString()
                        )
                        return
                    }

                    try {
                        session.output.flush()
                        session.output.close()

                        val savedUri = saveIncomingUploadToDownloads(session)
                        addSharedFile(savedUri)

                        webRtcManager.sendData(
                            JSONObject().apply {
                                put("type", "UPLOAD_DONE")
                                put("transferId", transferId)
                                put("fileName", session.fileName)
                                put("sizeBytes", session.bytesReceived)
                            }.toString()
                        )

                        // Push latest list immediately so web UI reflects new file without waiting.
                        sendCurrentFileListToPeer()
                    } catch (e: Exception) {
                        webRtcManager.sendData(
                            JSONObject().apply {
                                put("type", "ERROR")
                                put("message", "Upload finalize failed: ${e.message ?: "unknown"}")
                            }.toString()
                        )
                    } finally {
                        cleanupIncomingUpload(transferId)
                    }
                }
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
                "NOW_PLAYING" -> {
                    val title = json.optString("title", "No track")
                    val artist = json.optString("artist", "Unknown artist")
                    val album = json.optString("album", "")
                    val artwork = json.optString("artworkBase64", "").takeIf { it.isNotBlank() }
                    _nowPlayingTitle.value = title.ifBlank { "No track" }
                    _nowPlayingArtist.value = artist.ifBlank { "Unknown artist" }
                    _nowPlayingAlbum.value = album
                    _nowPlayingArtworkBase64.value = artwork
                    _nowPlayingTimestamp.value = System.currentTimeMillis()
                }
            }
        } catch (e: Exception) {
            _isRemoteLoading.value = false
        }
    }

    private fun cleanupIncomingUpload(transferId: String) {
        val session = incomingUploadSessions.remove(transferId) ?: return
        runCatching { session.output.close() }
        runCatching { if (session.tempFile.exists()) session.tempFile.delete() }
    }

    private fun saveIncomingUploadToDownloads(session: IncomingUploadSession): Uri {
        val app = getApplication<Application>()
        val resolver = app.contentResolver
        val now = System.currentTimeMillis()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val rabitDir = File(downloadsDir, "Hackie")
            if (!rabitDir.exists()) rabitDir.mkdirs()
            MediaStore.Files.getContentUri("external")
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, session.fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, session.mimeType)
            put(MediaStore.MediaColumns.DATE_ADDED, now / 1000)
            put(MediaStore.MediaColumns.DATE_MODIFIED, now / 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Hackie")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            } else {
                @Suppress("DEPRECATION")
                val target = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "Hackie/${session.fileName}"
                )
                put(MediaStore.MediaColumns.DATA, target.absolutePath)
            }
        }

        val itemUri = resolver.insert(collection, values)
            ?: throw IllegalStateException("Unable to create destination in Downloads")

        resolver.openOutputStream(itemUri)?.use { out ->
            session.tempFile.inputStream().use { input ->
                input.copyTo(out)
            }
        } ?: throw IllegalStateException("Unable to open destination output stream")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val doneValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            resolver.update(itemUri, doneValues, null, null)
        }

        return itemUri
    }

    private suspend fun streamSharedFileOverP2p(uri: Uri) {
        try {
            val resolver = getApplication<Application>().contentResolver
            val (name, size) = resolveSharedFileMetadata(uri)
            val mime = resolver.getType(uri) ?: "application/octet-stream"
            val transferId = UUID.randomUUID().toString()

            webRtcManager.sendData(
                JSONObject().apply {
                    put("type", "FILE_DOWNLOAD_START")
                    put("transferId", transferId)
                    put("name", name)
                    put("sizeBytes", size)
                    put("mimeType", mime)
                }.toString()
            )

            val stream = resolver.openInputStream(uri)
            if (stream == null) {
                webRtcManager.sendData(
                    JSONObject().apply {
                        put("type", "ERROR")
                        put("message", "Unable to open file for transfer.")
                    }.toString()
                )
                return
            }

            var sent = 0L
            // Keep chunks very small to avoid DataChannel backpressure/stalls in browsers.
            val buffer = ByteArray(4 * 1024)

            stream.use { input ->
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break

                    val chunk = buffer.copyOf(read)
                    webRtcManager.sendData(
                        JSONObject().apply {
                            put("type", "FILE_DOWNLOAD_CHUNK")
                            put("transferId", transferId)
                            put("dataBase64", Base64.encodeToString(chunk, Base64.NO_WRAP))
                        }.toString()
                    )
                    sent += read

                    if (sent % (64 * 1024) < read) {
                        webRtcManager.sendData(
                            JSONObject().apply {
                                put("type", "FILE_DOWNLOAD_PROGRESS")
                                put("transferId", transferId)
                                put("bytesSent", sent)
                                put("sizeBytes", size)
                            }.toString()
                        )
                    }

                    delay(2)
                }
            }

            webRtcManager.sendData(
                JSONObject().apply {
                    put("type", "FILE_DOWNLOAD_END")
                    put("transferId", transferId)
                    put("bytesSent", sent)
                    put("sizeBytes", size)
                }.toString()
            )
        } catch (e: Exception) {
            webRtcManager.sendData(
                JSONObject().apply {
                    put("type", "ERROR")
                    put("message", "File transfer failed: ${e.message ?: "unknown error"}")
                }.toString()
            )
        }
    }

    private fun sendCurrentFileListToPeer() {
        val files = JSONArray()
        _sharedFiles.value.forEach { uri ->
            val (name, size) = resolveSharedFileMetadata(uri)
            val ext = name.substringAfterLast('.', "").takeIf { it != name } ?: ""
            files.put(
                JSONObject().apply {
                    put("name", name)
                    put("path", uri.toString())
                    put("size", size)
                    put("isFolder", false)
                    put("extension", ext)
                    put("modifiedTime", System.currentTimeMillis())
                }
            )
        }

        webRtcManager.sendData(
            JSONObject().apply {
                put("type", "FILE_LIST")
                put("files", files)
            }.toString()
        )
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

    fun executeDuckyScript(script: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val lines = script.lines().map { it.trim() }.filter { it.isNotBlank() && !it.uppercase().startsWith("REM ") }
            var defaultDelay = 80L
            for (line in lines) {
                val parts = line.split(" ", limit = 2)
                val cmd = parts[0].uppercase()
                val arg = if (parts.size > 1) parts[1] else ""

                when (cmd) {
                    "DEFAULTDELAY" -> defaultDelay = arg.toLongOrNull() ?: defaultDelay
                    "DELAY" -> delay(arg.toLongOrNull() ?: defaultDelay)
                    "STRING" -> repository.sendText(arg)
                    "ENTER" -> repository.sendKey(HidKeyCodes.KEY_ENTER)
                    "TAB" -> repository.sendKey(HidKeyCodes.KEY_TAB)
                    "SPACE" -> repository.sendKey(HidKeyCodes.KEY_SPACE)
                    "UP", "UPARROW" -> repository.sendKey(HidKeyCodes.KEY_UP)
                    "DOWN", "DOWNARROW" -> repository.sendKey(HidKeyCodes.KEY_DOWN)
                    "GUI", "WINDOWS", "COMMAND" -> {
                        val uArg = arg.uppercase()
                        val key = if (uArg == "SPACE") HidKeyCodes.KEY_SPACE 
                        else if (uArg == "ENTER") HidKeyCodes.KEY_ENTER 
                        else if (uArg == "TAB") HidKeyCodes.KEY_TAB
                        else if (uArg.length == 1) {
                            try {
                                val field = HidKeyCodes::class.java.getDeclaredField("KEY_${uArg}")
                                field.get(null) as Byte
                            } catch (e: Exception) { HidKeyCodes.KEY_NONE }
                        } else HidKeyCodes.KEY_NONE
                        
                        repository.sendKey(key, HidKeyCodes.MODIFIER_LEFT_GUI)
                    }
                    "CTRL", "CONTROL" -> {
                        val uArg = arg.uppercase()
                        val key = if (uArg == "ALT") HidKeyCodes.KEY_NONE
                        else if (uArg.length == 1) {
                            try {
                                val field = HidKeyCodes::class.java.getDeclaredField("KEY_${uArg}")
                                field.get(null) as Byte
                            } catch (e: Exception) { HidKeyCodes.KEY_NONE }
                        } else HidKeyCodes.KEY_NONE
                        
                        repository.sendKey(key, HidKeyCodes.MODIFIER_LEFT_CTRL)
                    }
                    "ALT" -> {
                        val uArg = arg.uppercase()
                        val key = if (uArg == "TAB") HidKeyCodes.KEY_TAB 
                        else if (uArg.length == 1) {
                            try {
                                val field = HidKeyCodes::class.java.getDeclaredField("KEY_${uArg}")
                                field.get(null) as Byte
                            } catch (e: Exception) { HidKeyCodes.KEY_NONE }
                        } else HidKeyCodes.KEY_NONE
                        
                        repository.sendKey(key, HidKeyCodes.MODIFIER_LEFT_ALT)
                    }
                    "MAC_STEALTH" -> {
                        // Run command in Terminal with safer timing, then hide Terminal.
                        repository.sendKey(HidKeyCodes.KEY_SPACE, HidKeyCodes.MODIFIER_LEFT_GUI)
                        delay(420)
                        repository.sendText("Terminal")
                        delay(320)
                        repository.sendKey(HidKeyCodes.KEY_ENTER)
                        delay(1100)
                        repository.sendText(arg)
                        repository.sendKey(HidKeyCodes.KEY_ENTER)
                        delay(350)
                        repository.sendKey(HidKeyCodes.KEY_M, HidKeyCodes.MODIFIER_LEFT_GUI)
                    }
                    "SHIFT" -> {
                        val keyMap = mapOf("ENTER" to HidKeyCodes.KEY_ENTER, "TAB" to HidKeyCodes.KEY_TAB)
                        val key = keyMap[arg.uppercase()] ?: HidKeyCodes.KEY_NONE
                        repository.sendKey(key, HidKeyCodes.MODIFIER_LEFT_SHIFT)
                    }
                    else -> {
                        // Fallback: If it's a raw string, type it out and hit enter (simplified terminal entry)
                        repository.sendText(line)
                        repository.sendKey(HidKeyCodes.KEY_ENTER)
                    }
                }
                delay(defaultDelay)
            }
        }
    }

    private fun executeMacro2Script(script: String, cooldownMs: Long = 0L) {
        val commands = script
            .split("\n", "&&")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        fun extractArg(cmd: String, keyword: String): String? {
            val normalized = cmd.trim()
            val prefix = "$keyword("
            if (!normalized.startsWith(prefix, ignoreCase = true) || !normalized.endsWith(")")) return null
            return normalized.substring(prefix.length, normalized.length - 1)
        }

        viewModelScope.launch {
            for (cmd in commands) {
                when {
                    extractArg(cmd, "WAIT") != null -> {
                        val ms = extractArg(cmd, "WAIT")?.trim()?.toLongOrNull() ?: 120L
                        delay(ms.coerceIn(0L, 60_000L))
                    }
                    extractArg(cmd, "TEXT") != null -> {
                        repository.sendText(extractArg(cmd, "TEXT") ?: "")
                    }
                    extractArg(cmd, "KEY") != null -> {
                        executeKeyCombo(extractArg(cmd, "KEY") ?: "")
                    }
                    extractArg(cmd, "MEDIA") != null -> {
                        executeSpecialKey(extractArg(cmd, "MEDIA") ?: "")
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

    enum class EmergencyAction {
        LOCK_MACHINE,
        KILL_INTERNET_ADAPTER,
        STOP_AUDIO,
        CLEAR_CLIPBOARD,
        CLOSE_SENSITIVE_APPS
    }

    private val _activeProfile = MutableStateFlow(MacroProfile.GENERAL)
    val activeProfile = _activeProfile.asStateFlow()
    private val _emergencyStatus = MutableStateFlow("Idle")
    val emergencyStatus = _emergencyStatus.asStateFlow()

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
        RabitNetworkServer.audioStreamStartReceiver = null
        RabitNetworkServer.audioStreamChunkReceiver = null
        RabitNetworkServer.audioStreamStopReceiver = null
        proximityTelemetryJob?.cancel()
        wifiAudioSink.release()
        disconnectSsh()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        super.onCleared()
    }

    private fun hasBluetoothConnectPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    private fun findBondedDeviceByName(bluetoothAdapter: BluetoothAdapter?, name: String): BluetoothDevice? {
        if (!hasBluetoothConnectPermission(getApplication())) return null
        return try {
            bluetoothAdapter?.bondedDevices?.find { it.name == name }
        } catch (_: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    private fun findBondedDeviceByNameOrAddress(bluetoothAdapter: BluetoothAdapter?, name: String, address: String): BluetoothDevice? {
        if (!hasBluetoothConnectPermission(getApplication())) return null
        return try {
            bluetoothAdapter?.bondedDevices?.find { it.name == name || (address.isNotBlank() && it.address == address) }
        } catch (_: Exception) {
            null
        }
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
data class VaultEntry(
    val id: String,
    val appName: String,
    val username: String,
    val password: String,
    val notes: String,
    val updatedAtMs: Long
)
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
