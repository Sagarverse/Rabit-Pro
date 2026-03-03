package com.example.rabit.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

@SuppressLint("MissingPermission")
class BleBeaconManager {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var advertiser: BluetoothLeAdvertiser? = null

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            Log.d("BleBeaconManager", "Advertising started successfully")
            _isAdvertising.value = true
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.e("BleBeaconManager", "Advertising failed with error code: $errorCode")
            _isAdvertising.value = false
        }
    }

    fun startAdvertising(uuidString: String, major: Int, minor: Int) {
        val adapter = bluetoothAdapter ?: return
        if (!adapter.isEnabled) return

        advertiser = adapter.bluetoothLeAdvertiser
        if (advertiser == null) {
            Log.e("BleBeaconManager", "Device does not support BLE advertising")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        val uuid = try {
            UUID.fromString(uuidString)
        } catch (e: Exception) {
            UUID.randomUUID()
        }

        // iBeacon format: 0x02, 0x15, UUID (16 bytes), Major (2 bytes), Minor (2 bytes), TxPower (1 byte)
        val manufacturerData = ByteArray(23)
        manufacturerData[0] = 0x02
        manufacturerData[1] = 0x15
        
        val uuidBytes = uuidToBytes(uuid)
        System.arraycopy(uuidBytes, 0, manufacturerData, 2, 16)
        
        manufacturerData[18] = ((major shr 8) and 0xFF).toByte()
        manufacturerData[19] = (major and 0xFF).toByte()
        
        manufacturerData[20] = ((minor shr 8) and 0xFF).toByte()
        manufacturerData[21] = (minor and 0xFF).toByte()
        
        manufacturerData[22] = 0xC5.toByte() // Measured Power at 1m

        val data = AdvertiseData.Builder()
            .addManufacturerData(0x004C, manufacturerData) // 0x004C is Apple's Company ID
            .build()

        advertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    fun stopAdvertising() {
        advertiser?.stopAdvertising(advertiseCallback)
        _isAdvertising.value = false
    }

    private fun uuidToBytes(uuid: UUID): ByteArray {
        val out = ByteArray(16)
        val msb = uuid.mostSignificantBits
        val lsb = uuid.leastSignificantBits
        for (i in 0..7) out[i] = (msb shr (8 * (7 - i)) and 0xFF).toByte()
        for (i in 8..15) out[i] = (lsb shr (8 * (15 - i)) and 0xFF).toByte()
        return out
    }
}
