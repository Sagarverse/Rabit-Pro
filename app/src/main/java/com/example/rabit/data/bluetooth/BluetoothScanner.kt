package com.example.rabit.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@SuppressLint("MissingPermission")
class BluetoothScanner(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = try {
        context.getSystemService(BluetoothManager::class.java)?.adapter
    } catch (e: Exception) {
        null
    }

    private val _scannedDevices = MutableStateFlow<Set<BluetoothDevice>>(emptySet())
    val scannedDevices: StateFlow<Set<BluetoothDevice>> = _scannedDevices

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val handler = Handler(Looper.getMainLooper())
    private val scanStopRunnable = Runnable { stopScanning() }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.parcelableExtraCompat(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        _scannedDevices.value = _scannedDevices.value + it
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    // Classic discovery finished, LE might still be running or vice versa
                }
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            result.device?.let { device ->
                try {
                    _scannedDevices.value = _scannedDevices.value + device
                } catch (e: SecurityException) { }
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            super.onBatchScanResults(results)
            val devices = results.map { it.device }
            _scannedDevices.value = _scannedDevices.value + devices
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            _isScanning.value = false
        }
    }

    /**
     * Clears previously scanned devices for a fresh scan.
     */
    fun clearDevices() {
        _scannedDevices.value = emptySet()
    }

    fun startScanning() {
        if (_isScanning.value) return
        
        val adapter = bluetoothAdapter ?: return
        
        if (!adapter.isEnabled) {
            // Cannot programmatically enable BT on Android 13+.
            // PairingScreen will direct user to system Bluetooth settings.
            return
        }

        // We no longer pre-populate with bonded devices here.
        // The UI (PairingScreen) will handle Bonded vs Nearby separation.

        // Start Classic Discovery
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(bluetoothReceiver, filter)
        adapter.startDiscovery()

        // Start LE Scan
        val leScanner = try { adapter.bluetoothLeScanner } catch (e: Exception) { null }
        if (leScanner != null) {
            try {
                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()
                leScanner.startScan(null, settings, scanCallback)
            } catch (e: Exception) {
                Log.e("BluetoothScanner", "LE scan start failed", e)
            }
        }

        _isScanning.value = true
        // Reduced from 15s to 8s for faster turnaround
        handler.postDelayed(scanStopRunnable, 8000)
    }

    fun stopScanning() {
        if (!_isScanning.value) return
        
        val adapter = bluetoothAdapter ?: return
        
        // Stop Classic Discovery
        try {
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
            context.unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) { }

        // Stop LE Scan
        val leScanner = try { adapter.bluetoothLeScanner } catch (e: Exception) { null }
        try {
            leScanner?.stopScan(scanCallback)
        } catch (e: Exception) { }

        _isScanning.value = false
        handler.removeCallbacks(scanStopRunnable)
    }

    private inline fun <reified T : Parcelable> Intent.parcelableExtraCompat(key: String): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(key) as? T
        }
    }
}
