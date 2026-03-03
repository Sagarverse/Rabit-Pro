package com.example.rabit.data.repository

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.example.rabit.data.bluetooth.BluetoothScanner
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.domain.repository.KeyboardRepository
import kotlinx.coroutines.flow.StateFlow

class KeyboardRepositoryImpl(context: Context) : KeyboardRepository {
    private val hidDeviceManager = HidDeviceManager.getInstance(context)
    private val bluetoothScanner = BluetoothScanner(context)

    override val connectionState: StateFlow<HidDeviceManager.ConnectionState> = hidDeviceManager.connectionState
    override val scannedDevices: StateFlow<Set<BluetoothDevice>> = bluetoothScanner.scannedDevices
    override val isScanning: StateFlow<Boolean> = bluetoothScanner.isScanning
    override val isPushPaused: StateFlow<Boolean> = hidDeviceManager.isPushPaused

    override fun startScanning() {
        bluetoothScanner.startScanning()
    }

    override fun stopScanning() {
        bluetoothScanner.stopScanning()
    }

    override fun requestDiscoverable() {
        hidDeviceManager.requestDiscoverable()
    }

    override fun connect(device: BluetoothDevice) {
        hidDeviceManager.connect(device)
    }

    override fun disconnect() {
        hidDeviceManager.disconnect()
    }

    override fun sendKey(keyCode: Byte, modifier: Byte) {
        hidDeviceManager.sendKeyPress(keyCode, modifier)
    }

    override fun setModifier(modifier: Byte, active: Boolean) {
        hidDeviceManager.setModifier(modifier, active)
    }

    override fun sendConsumerKey(usageId: Short) {
        hidDeviceManager.sendConsumerKey(usageId)
    }

    override fun sendText(text: String) {
        hidDeviceManager.sendText(text)
    }

    override fun sendMouseMove(dx: Float, dy: Float, buttons: Int, wheel: Int) {
        hidDeviceManager.sendMouseMove(dx, dy, buttons, wheel)
    }

    override fun stopTextPush() {
        hidDeviceManager.stopTextPush()
    }

    override fun pauseTextPush() {
        hidDeviceManager.pauseTextPush()
    }

    override fun resumeTextPush() {
        hidDeviceManager.resumeTextPush()
    }

    override fun unlockMac(password: String) {
        hidDeviceManager.unlockMac(password)
    }
}
