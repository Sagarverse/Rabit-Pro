package com.example.rabit.domain.repository

import android.bluetooth.BluetoothDevice
import com.example.rabit.data.bluetooth.HidDeviceManager
import kotlinx.coroutines.flow.StateFlow

interface KeyboardRepository {
    val connectionState: StateFlow<HidDeviceManager.ConnectionState>
    val scannedDevices: StateFlow<Set<BluetoothDevice>>
    val isScanning: StateFlow<Boolean>
    val isPushPaused: StateFlow<Boolean>
    
    fun startScanning()
    fun stopScanning()
    fun requestDiscoverable()
    fun connect(device: BluetoothDevice)
    fun disconnect()
    fun sendKey(keyCode: Byte, modifier: Byte = 0)
    fun setModifier(modifier: Byte, active: Boolean)
    fun sendConsumerKey(usageId: Short)
    fun sendText(text: String)
    fun sendMouseMove(dx: Float, dy: Float, buttons: Int = 0, wheel: Int = 0)
    fun stopTextPush()
    fun pauseTextPush()
    fun resumeTextPush()
    fun unlockMac(password: String)
}
