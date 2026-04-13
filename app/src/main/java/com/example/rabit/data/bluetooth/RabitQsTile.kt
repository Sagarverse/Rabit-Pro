package com.example.rabit.data.bluetooth

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.example.rabit.MainActivity
import kotlinx.coroutines.*
import org.json.JSONArray

/**
 * RabitQsTile — Quick Settings Tile for one-tap Bluetooth connect/disconnect.
 *
 * Lives in the Android notification shade. Tapping it:
 *   - If connected → disconnects
 *   - If disconnected → connects to the most recently saved device
 *   - If no saved device → launches the app to the pairing screen
 *
 * The tile label shows "Rabit Pro" and the subtitle shows the connection state.
 */
@SuppressLint("MissingPermission")
class RabitQsTile : TileService() {

    private val TAG = "RabitQsTile"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var observerJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()

        // Observe connection state changes while tile is visible
        observerJob?.cancel()
        observerJob = scope.launch {
            try {
                HidDeviceManager.getInstance(applicationContext).connectionState.collect {
                    updateTileState()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing connection state", e)
            }
        }
    }

    override fun onStopListening() {
        observerJob?.cancel()
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()

        val hidManager = HidDeviceManager.getInstance(applicationContext)
        val currentState = hidManager.connectionState.value

        when (currentState) {
            is HidDeviceManager.ConnectionState.Connected -> {
                // Already connected — disconnect
                Log.d(TAG, "Tile tapped: disconnecting")
                hidManager.disconnect()
            }
            else -> {
                // Try to connect to the last saved device
                val connected = connectToSavedDevice()
                if (!connected) {
                    // No saved device — launch the app to pairing screen
                    Log.d(TAG, "No saved device found, launching app")
                    launchApp()
                }
            }
        }
    }

    /**
     * Attempt to connect to the most recently saved Bluetooth device.
     * Returns true if a connection attempt was initiated, false if no device was found.
     */
    private fun connectToSavedDevice(): Boolean {
        val prefs = getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("saved_devices_json", null) ?: return false

        return try {
            val array = JSONArray(json)
            if (array.length() == 0) return false

            val firstDevice = array.getJSONObject(0)
            val address = firstDevice.optString("address", "")
            val name = firstDevice.optString("name", "Unknown")

            if (address.isBlank()) {
                Log.w(TAG, "Saved device '$name' has no MAC address, launching app")
                return false
            }

            val adapter = applicationContext.getSystemService(BluetoothManager::class.java)?.adapter ?: return false
            if (!adapter.isEnabled) {
                Log.w(TAG, "Bluetooth is off")
                return false
            }

            val device = adapter.getRemoteDevice(address)
            Log.d(TAG, "Connecting to saved device: $name ($address)")

            // Ensure the HID service is running
            val serviceIntent = Intent(applicationContext, HidService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(serviceIntent)
            } else {
                applicationContext.startService(serviceIntent)
            }

            // Small delay to let the service initialize profile if needed
            scope.launch {
                delay(500)
                HidDeviceManager.getInstance(applicationContext).connect(device)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to saved device", e)
            false
        }
    }

    private fun launchApp() {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val hidManager = HidDeviceManager.getInstance(applicationContext)
        val state = hidManager.connectionState.value

        when (state) {
            is HidDeviceManager.ConnectionState.Connected -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Rabit Pro"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = state.deviceName
                }
            }
            is HidDeviceManager.ConnectionState.Connecting -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Rabit Pro"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Connecting..."
                }
            }
            is HidDeviceManager.ConnectionState.Disconnected -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "Rabit Pro"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Tap to connect"
                }
            }
        }
        tile.updateTile()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
