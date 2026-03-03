package com.example.rabit.ui

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rabit.data.repository.KeyboardRepositoryImpl
import com.example.rabit.domain.repository.KeyboardRepository
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.data.bluetooth.HidService
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
    private val clipboard = application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    val connectionState: StateFlow<HidDeviceManager.ConnectionState> = repository.connectionState
    val scannedDevices: StateFlow<Set<BluetoothDevice>> = repository.scannedDevices
    val isScanning: StateFlow<Boolean> = repository.isScanning
    val isPushPaused: StateFlow<Boolean> = repository.isPushPaused

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

    private var lastClipboardText: String? = null

    // Custom Macros State
    private val _customMacros = MutableStateFlow<List<CustomMacro>>(loadCustomMacros())
    val customMacros = _customMacros.asStateFlow()

    // Trackpad optimization
    private var lastMoveTime = 0L
    private val moveThreshold = 0.5f 

    init {
        updateRepositorySpeed(_typingSpeed.value)
        startClipboardObserver()
    }

    private fun startClipboardObserver() {
        viewModelScope.launch {
            while (true) {
                try {
                    val primaryClip = clipboard.primaryClip
                    if (primaryClip != null && primaryClip.itemCount > 0) {
                        val text = primaryClip.getItemAt(0).text?.toString()
                        if (text != lastClipboardText && !text.isNullOrBlank()) {
                            lastClipboardText = text
                            showClipboardNotification(text)
                        }
                    }
                } catch (e: Exception) { }
                delay(3000)
            }
        }
    }

    private fun showClipboardNotification(text: String) {
        val intent = Intent(getApplication(), HidService::class.java).apply {
            action = "SHOW_CLIPBOARD_NOTIFICATION"
            putExtra("text", text)
        }
        getApplication<Application>().startService(intent)
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
        if (abs(dx) < moveThreshold && abs(dy) < moveThreshold) return
        val now = System.currentTimeMillis()
        val dt = (now - lastMoveTime).coerceAtLeast(1L)
        lastMoveTime = now
        val sensitivity = 1.1f 
        val accelFactor = 1.4f 
        val finalDx = (sign(dx) * abs(dx).pow(accelFactor) * sensitivity)
        val finalDy = (sign(dy) * abs(dy).pow(accelFactor) * sensitivity)
        repository.sendMouseMove(finalDx, finalDy, buttons, wheel)
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
            macro.split("&&").forEach { part ->
                repository.sendText(part.trim())
                delay(100)
                repository.sendKey(HidKeyCodes.KEY_ENTER)
                delay(300)
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

    // Custom Macros Logic
    fun addCustomMacro(name: String, command: String) {
        val newList = _customMacros.value + CustomMacro(name, command)
        _customMacros.value = newList
        saveCustomMacros(newList)
    }

    fun deleteCustomMacro(macro: CustomMacro) {
        val newList = _customMacros.value - macro
        _customMacros.value = newList
        saveCustomMacros(newList)
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
        val json = prefs.getString("custom_macros_json", null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<CustomMacro>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(CustomMacro(obj.getString("name"), obj.getString("command")))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

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
}

data class CustomMacro(val name: String, val command: String)
