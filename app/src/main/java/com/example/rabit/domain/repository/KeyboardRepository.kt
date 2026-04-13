package com.example.rabit.domain.repository

import android.bluetooth.BluetoothDevice
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.domain.model.Workstation
import kotlinx.coroutines.flow.StateFlow

interface KeyboardRepository {
    val connectionState: StateFlow<HidDeviceManager.ConnectionState>
    val scannedDevices: StateFlow<Set<BluetoothDevice>>
    val isScanning: StateFlow<Boolean>
    val isPushPaused: StateFlow<Boolean>
    val isTextPushing: StateFlow<Boolean>
    val knownWorkstations: StateFlow<List<Workstation>>
    var onShakeDetected: (() -> Unit)?
    
    fun startScanning()
    fun stopScanning()
    fun removeWorkstation(address: String)
    fun updateWorkstationNickname(address: String, nickname: String)
    fun requestDiscoverable()
    fun connect(device: BluetoothDevice)
    fun connectWithRetry(device: BluetoothDevice, maxRetries: Int = 3, retryDelayMs: Long = 1500)
    fun disconnect()
    fun sendKey(keyCode: Byte, modifier: Byte = 0)
    fun setModifier(modifier: Byte, active: Boolean)
    fun sendConsumerKey(usageId: Short)
    fun sendText(text: String)
    fun sendMouseMove(dx: Float, dy: Float, buttons: Int = 0, wheel: Int = 0)
    fun sendDigitizerInput(x: Int, y: Int, isPressed: Boolean, inRange: Boolean)
    fun resetMouseAccumulator()
    fun stopTextPush()
    fun pauseTextPush()
    fun resumeTextPush()
    fun unlockMac(
        password: String,
        pressEnterBefore: Boolean = true,
        pressEnterAfter: Boolean = true,
        preTypeDelayMs: Long = 1500L,
        postTypeDelayMs: Long = 800L
    )
}
