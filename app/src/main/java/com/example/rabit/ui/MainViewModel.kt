package com.example.rabit.ui

import android.app.Application
import android.bluetooth.BluetoothDevice
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
import com.example.rabit.data.network.RabitNetworkServer
import com.example.rabit.domain.model.HidKeyCodes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val prefs = application.getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)

    val connectionState: StateFlow<HidDeviceManager.ConnectionState> = repository.connectionState
    val scannedDevices: StateFlow<Set<BluetoothDevice>> = repository.scannedDevices
    val isScanning: StateFlow<Boolean> = repository.isScanning
    val isPushPaused: StateFlow<Boolean> = repository.isPushPaused
    val isTextPushing: StateFlow<Boolean> = repository.isTextPushing

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

    // Vibration toggle
    private val _vibrationEnabled = MutableStateFlow(prefs.getBoolean("vibration_enabled", true))
    val vibrationEnabled = _vibrationEnabled.asStateFlow()

    // Trackpad sensitivity (0.5f to 3.0f)
    private val _trackpadSensitivity = MutableStateFlow(prefs.getFloat("trackpad_sensitivity", 1.5f))
    val trackpadSensitivity = _trackpadSensitivity.asStateFlow()

    // Text push progress
    private val _pushProgress = MutableStateFlow(0f)
    val pushProgress = _pushProgress.asStateFlow()

    // Saved devices
    private val _savedDevices = MutableStateFlow<List<SavedDevice>>(emptyList())
    val savedDevices = _savedDevices.asStateFlow()

    // Onboarding completed
    val onboardingCompleted: Boolean
        get() = prefs.getBoolean("onboarding_completed", false)

    fun markOnboardingCompleted() {
        prefs.edit().putBoolean("onboarding_completed", true).apply()
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

    // Media Sync State
    private val _currentMedia = MutableStateFlow<RabitNetworkServer.MediaMetadata?>(null)
    val currentMedia = _currentMedia.asStateFlow()

    // Custom Macros State (cached for performance)
    private val _customMacros = MutableStateFlow<List<CustomMacro>>(emptyList())
    val customMacros = _customMacros.asStateFlow()
    private var macrosCache: List<CustomMacro>? = null

    // Trackpad optimization
    private var lastMoveTime = 0L
    private val moveThreshold = 0.2f
    private var lastDx = 0f
    private var lastDy = 0f

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        updateRepositorySpeed(_typingSpeed.value)
        setupMediaListener()
        _customMacros.value = loadCustomMacros()
        macrosCache = _customMacros.value
        _savedDevices.value = loadSavedDevices()
        
        val serviceIntent = Intent(application, HidService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            application.startForegroundService(serviceIntent)
        } else {
            application.startService(serviceIntent)
        }

        // Save device when connected
        viewModelScope.launch {
            connectionState.collect { state ->
                if (state is HidDeviceManager.ConnectionState.Connected) {
                    saveDevice(state.deviceName, "")
                }
            }
        }
    }

    private fun setupMediaListener() {
        RabitNetworkServer.onMediaMetadataReceived = { metadata ->
            _currentMedia.value = metadata
        }
    }

    fun startScanning() = repository.startScanning()
    fun stopScanning() = repository.stopScanning()
    fun requestDiscoverable() = repository.requestDiscoverable()
    fun connect(device: BluetoothDevice) = repository.connect(device)
    fun disconnect() = repository.disconnect()
    
    fun sendKey(keyCode: Byte) {
        repository.sendKey(keyCode, _activeModifiers.value)
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
    
    fun sendMouseMove(dx: Float, dy: Float, buttons: Int = 0, wheel: Int = 0) {
        if (buttons != 0 || wheel != 0) {
            repository.sendMouseMove(dx, dy, buttons, wheel)
            return
        }
        
        // Remove recursive dx filtering to avoid momentum drift
        val sensitivity = _trackpadSensitivity.value
        val accelFactor = 1.15f
        
        // Precise low-speed translation, accelerated high-speed translation
        val finalDx = sign(dx) * abs(dx).pow(accelFactor) * sensitivity
        val finalDy = sign(dy) * abs(dy).pow(accelFactor) * sensitivity
        
        repository.sendMouseMove(finalDx, finalDy, buttons, wheel)
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

    override fun onCleared() {
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        super.onCleared()
    }
}

data class CustomMacro(val name: String, val command: String)
data class SavedDevice(val name: String, val address: String, val lastConnected: Long)
