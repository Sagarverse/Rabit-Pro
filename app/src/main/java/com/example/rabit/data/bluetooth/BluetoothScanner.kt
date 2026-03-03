package com.example.rabit.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@SuppressLint("MissingPermission")
class BluetoothScanner(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = try {
        BluetoothAdapter.getDefaultAdapter()
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
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        if (it.name != null) {
                            _scannedDevices.value = _scannedDevices.value + it
                        }
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
                    if (device.name != null) {
                        _scannedDevices.value = _scannedDevices.value + device
                    }
                } catch (e: SecurityException) { }
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            super.onBatchScanResults(results)
            val devices = results.map { it.device }.filter { 
                try { it.name != null } catch (e: SecurityException) { false } 
            }
            _scannedDevices.value = _scannedDevices.value + devices
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            _isScanning.value = false
        }
    }

    fun startScanning() {
        if (_isScanning.value) return
        
        val adapter = bluetoothAdapter ?: return
        
        if (!adapter.isEnabled) {
            try {
                adapter.enable()
                handler.postDelayed({ startScanning() }, 1000)
                return
            } catch (e: Exception) {
                Log.e("BluetoothScanner", "Failed to enable Bluetooth", e)
            }
        }

        // Start Classic Discovery
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(bluetoothReceiver, filter)
        adapter.startDiscovery()

        // Start LE Scan
        val leScanner = try { adapter.bluetoothLeScanner } catch (e: Exception) { null }
        if (leScanner != null) {
            try {
                _scannedDevices.value = adapter.bondedDevices ?: emptySet()
                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()
                leScanner.startScan(null, settings, scanCallback)
            } catch (e: Exception) {
                Log.e("BluetoothScanner", "LE scan start failed", e)
            }
        }

        _isScanning.value = true
        handler.postDelayed(scanStopRunnable, 15000)
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
}
