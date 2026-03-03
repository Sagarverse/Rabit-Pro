package com.example.rabit.data.automation

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * AutomationManager - Feature 4: Smart PC-Aware Automation
 *
 * Automatically adjusts phone behavior when the Bluetooth HID
 * connection to the Mac is established or dropped.
 *
 * Routines triggered ON CONNECT:
 *  - Enables Do Not Disturb (silences phone notifications while you work at your Mac)
 *  - Acquires a WakeLock to prevent screen timeout (keeps phone screen alive)
 *
 * Routines triggered ON DISCONNECT:
 *  - Restores previous DND state
 *  - Releases WakeLock (phone screen sleeps normally again)
 *
 * Each routine is individually togglable via SharedPreferences.
 */
class AutomationManager(private val context: Context) {

    private val TAG = "AutomationManager"
    private val prefs = context.getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    private var wakeLock: PowerManager.WakeLock? = null
    private var previousDndFilter: Int = NotificationManager.INTERRUPTION_FILTER_ALL

    // ───── Settings Keys ─────
    val isDndOnConnectEnabled: Boolean get() = prefs.getBoolean("auto_dnd_on_connect", false)
    val isWakeLockOnConnectEnabled: Boolean get() = prefs.getBoolean("auto_wake_lock_on_connect", false)

    fun setDndOnConnectEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_dnd_on_connect", enabled).apply()
    }

    fun setWakeLockOnConnectEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_wake_lock_on_connect", enabled).apply()
    }

    // ───── Called when Mac connects ─────
    fun onConnected() {
        scope.launch {
            Log.d(TAG, "Device connected — applying automation routines")

            if (isDndOnConnectEnabled) {
                enableDoNotDisturb()
            }

            if (isWakeLockOnConnectEnabled) {
                acquireWakeLock()
            }
        }
    }

    // ───── Called when Mac disconnects ─────
    fun onDisconnected() {
        scope.launch {
            Log.d(TAG, "Device disconnected — restoring phone state")

            if (isDndOnConnectEnabled) {
                restoreDoNotDisturb()
            }

            releaseWakeLock()
        }
    }

    // ───── DND Logic ─────

    private fun enableDoNotDisturb() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        if (!notifManager.isNotificationPolicyAccessGranted) {
            Log.w(TAG, "DND permission not granted. Skipping DND automation.")
            return
        }
        try {
            previousDndFilter = notifManager.currentInterruptionFilter
            notifManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            Log.d(TAG, "DND enabled (saved previous filter: $previousDndFilter)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable DND", e)
        }
    }

    private fun restoreDoNotDisturb() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        if (!notifManager.isNotificationPolicyAccessGranted) return
        try {
            notifManager.setInterruptionFilter(previousDndFilter)
            Log.d(TAG, "DND restored to filter: $previousDndFilter")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore DND", e)
        }
    }

    // ───── Wake Lock Logic ─────

    @Suppress("DEPRECATION")
    private fun acquireWakeLock() {
        wakeLock?.release()
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "rabit:connected_wake_lock"
        ).also {
            it.acquire(4 * 60 * 60 * 1000L) // Max 4 hours
            Log.d(TAG, "WakeLock acquired")
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "WakeLock released")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wake lock", e)
        } finally {
            wakeLock = null
        }
    }

    fun destroy() {
        releaseWakeLock()
    }
}
