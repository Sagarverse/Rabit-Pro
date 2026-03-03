package com.example.rabit.ui.assistant

import android.content.Context
import com.example.rabit.data.bluetooth.HidDeviceManager

/**
 * Provides a singleton instance of HidDeviceManager for Bluetooth HID operations.
 */
object BluetoothHidServiceProvider {
    private var instance: HidDeviceManager? = null

    fun getInstance(context: Context): HidDeviceManager {
        if (instance == null) {
            instance = HidDeviceManager.getInstance(context.applicationContext)
        }
        return instance!!
    }
}
