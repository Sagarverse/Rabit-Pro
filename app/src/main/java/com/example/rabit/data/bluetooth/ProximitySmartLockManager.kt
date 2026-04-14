package com.example.rabit.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.app.KeyguardManager
import android.os.Build
import android.os.Parcelable
import android.util.Log
import com.example.rabit.data.secure.SecureStorage
import com.example.rabit.domain.model.HidKeyCodes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import kotlin.math.pow

@SuppressLint("MissingPermission")
class ProximitySmartLockManager(
    private val context: Context,
    private val hidDeviceManager: HidDeviceManager,
    private val connectionState: StateFlow<HidDeviceManager.ConnectionState>,
    private val scope: CoroutineScope
) {
    companion object {
        private const val PREF_LIVE_RSSI = "proximity_live_rssi"
        private const val PREF_LIVE_DISTANCE_M = "proximity_live_distance_m"
        private const val PREF_LIVE_LAST_SEEN_MS = "proximity_live_last_seen_ms"
        private const val PREF_UNLOCK_ARMED = "proximity_unlock_armed"
        private const val PREF_MAC_LOCK_STATE_GUESS = "proximity_mac_lock_state_guess"

        private const val LOCK_STATE_UNKNOWN = "UNKNOWN"
        private const val LOCK_STATE_LIKELY_LOCKED = "LIKELY_LOCKED"
        private const val LOCK_STATE_LIKELY_UNLOCKED = "LIKELY_UNLOCKED"
    }

    private val bluetoothAdapter: BluetoothAdapter? = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val secureStorage = SecureStorage(context)

    private var enabled = false
    private var scanLoopJob: Job? = null
    private var lastSeenAddress: String? = null
    private var lastSeenRssi = -120
    private var emaRssi = -120.0
    private var lastNearUnlockMs = 0L
    private var lastAwayLockMs = 0L
    private val prefs = context.getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)

    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.parcelableExtraCompat<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                    if (!isTrackedHost(device)) return

                    lastSeenAddress = device.address
                    lastSeenRssi = rssi
                    emaRssi = if (emaRssi <= -110) rssi.toDouble() else (emaRssi * 0.7 + rssi * 0.3)
                    val estimatedDistance = estimateDistanceMeters(emaRssi)
                    prefs.edit()
                        .putInt(PREF_LIVE_RSSI, emaRssi.toInt())
                        .putFloat(PREF_LIVE_DISTANCE_M, estimatedDistance.toFloat())
                        .putLong(PREF_LIVE_LAST_SEEN_MS, System.currentTimeMillis())
                        .apply()
                    evaluateProximity(device)
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    if (enabled && bluetoothAdapter?.isEnabled == true) {
                        scope.launch {
                            delay(1200)
                            safeStartDiscovery()
                        }
                    }
                }
            }
        }
    }

    fun start() {
        if (enabled) return
        enabled = true
        prefs.edit().putString(PREF_MAC_LOCK_STATE_GUESS, LOCK_STATE_UNKNOWN).apply()

        try {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            context.registerReceiver(discoveryReceiver, filter)
        } catch (e: Exception) {
            Log.e("ProximitySmartLock", "Failed to register receiver", e)
        }

        scanLoopJob?.cancel()
        scanLoopJob = scope.launch(Dispatchers.IO) {
            while (isActive && enabled) {
                safeStartDiscovery()
                delay(15_000)
            }
        }
    }

    fun stop() {
        enabled = false
        scanLoopJob?.cancel()
        try {
            context.unregisterReceiver(discoveryReceiver)
        } catch (_: Exception) {
        }
        try {
            if (bluetoothAdapter?.isDiscovering == true) bluetoothAdapter.cancelDiscovery()
        } catch (_: Exception) {
        }
        prefs.edit().putString(PREF_MAC_LOCK_STATE_GUESS, LOCK_STATE_UNKNOWN).apply()
    }

    private fun safeStartDiscovery() {
        try {
            val adapter = bluetoothAdapter ?: return
            if (!adapter.isEnabled) return
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
            adapter.startDiscovery()
        } catch (e: Exception) {
            Log.e("ProximitySmartLock", "Discovery start failed", e)
        }
    }

    private fun evaluateProximity(device: BluetoothDevice) {
        val now = System.currentTimeMillis()
        val connected = connectionState.value is HidDeviceManager.ConnectionState.Connected
        val nearThreshold = prefs.getInt("proximity_near_rssi", -62)
        val farThreshold = prefs.getInt("proximity_far_rssi", -80)
        val triggerCooldownMs = prefs.getInt("proximity_cooldown_sec", 12).toLong() * 1000L
        val requirePhoneUnlock = prefs.getBoolean("proximity_require_phone_unlock", true)
        val unlockArmed = prefs.getBoolean(PREF_UNLOCK_ARMED, true)

        if (emaRssi >= nearThreshold && unlockArmed && now - lastNearUnlockMs > triggerCooldownMs) {
            lastNearUnlockMs = now
            scope.launch(Dispatchers.IO) {
                if (!connected) {
                    hidDeviceManager.connectWithRetry(device, maxRetries = 3, retryDelayMs = 1200)
                    delay(1700)
                }
                if (connectionState.value is HidDeviceManager.ConnectionState.Connected) {
                    // Wake screen first, then type password from secure storage.
                    hidDeviceManager.sendKeyPress(HidKeyCodes.KEY_SPACE, useSticky = false)
                    delay(900)
                    if (requirePhoneUnlock && isPhoneLocked()) {
                        return@launch
                    }
                    val macPassword = secureStorage.getMacPassword().orEmpty()
                    if (macPassword.isNotBlank()) {
                        hidDeviceManager.unlockMac(macPassword)
                        prefs.edit()
                            .putBoolean(PREF_UNLOCK_ARMED, false)
                            .putString(PREF_MAC_LOCK_STATE_GUESS, LOCK_STATE_LIKELY_UNLOCKED)
                            .apply()
                    }
                }
            }
            return
        }

        if (emaRssi <= farThreshold && connected && now - lastAwayLockMs > triggerCooldownMs) {
            lastAwayLockMs = now
            scope.launch(Dispatchers.IO) {
                val modifier = (HidKeyCodes.MODIFIER_LEFT_CTRL.toInt() or HidKeyCodes.MODIFIER_LEFT_GUI.toInt()).toByte()
                hidDeviceManager.sendKeyPress(HidKeyCodes.KEY_Q, modifier = modifier, useSticky = false)
                delay(400)
                prefs.edit()
                    .putBoolean(PREF_UNLOCK_ARMED, true)
                    .putString(PREF_MAC_LOCK_STATE_GUESS, LOCK_STATE_LIKELY_LOCKED)
                    .apply()
            }
        }
    }

    private fun estimateDistanceMeters(rssi: Double): Double {
        // Approximate estimate; BLE distance depends on walls/interference.
        val txPowerAt1m = -59.0
        val pathLossExponent = 2.0
        return 10.0.pow((txPowerAt1m - rssi) / (10.0 * pathLossExponent)).coerceIn(0.1, 20.0)
    }

    private fun isTrackedHost(device: BluetoothDevice): Boolean {
        val trackedAddress = resolveTrackedAddress()
        if (!trackedAddress.isNullOrBlank()) {
            return device.address == trackedAddress
        }

        val name = runCatching { device.name.orEmpty() }.getOrDefault("")
        if (name.isBlank()) return false
        val lower = name.lowercase()
        return lower.contains("mac") || lower.contains("macbook") || lower.contains("imac") || lower.contains("apple")
    }

    private fun resolveTrackedAddress(): String? {
        val explicit = prefs.getString("proximity_target_address", null)
        if (!explicit.isNullOrBlank()) return explicit

        val savedJson = prefs.getString("saved_devices_json", null) ?: return null
        return runCatching {
            val array = JSONArray(savedJson)
            if (array.length() == 0) return@runCatching null
            array.getJSONObject(0).optString("address").ifBlank { null }
        }.getOrNull()
    }

    private inline fun <reified T : Parcelable> Intent.parcelableExtraCompat(key: String): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(key) as? T
        }
    }

    private fun isPhoneLocked(): Boolean {
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        return keyguard?.isDeviceLocked == true
    }
}
