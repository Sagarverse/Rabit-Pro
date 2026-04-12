package com.example.rabit.data.bluetooth

import android.annotation.SuppressLint
import android.content.Context
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.rabit.domain.model.HidKeyCodes
import kotlinx.coroutines.*

/**
 * LockMacTile - Quick Settings Tile for 'Lock Workstation'
 */
@SuppressLint("MissingPermission")
class LockMacTile : TileService() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var observerJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        updateState()
        observerJob?.cancel()
        observerJob = scope.launch {
            HidDeviceManager.getInstance(applicationContext).connectionState.collect { updateState() }
        }
    }

    override fun onStopListening() {
        observerJob?.cancel()
        super.onStopListening()
    }

    override fun onClick() {
        val hidManager = HidDeviceManager.getInstance(applicationContext)
        if (hidManager.connectionState.value is HidDeviceManager.ConnectionState.Connected) {
            // CMD + CTRL + Q
            hidManager.sendKeyCombination(listOf(
                HidKeyCodes.MODIFIER_LEFT_GUI,
                HidKeyCodes.MODIFIER_LEFT_CTRL,
                HidKeyCodes.KEY_Q
            ))
        }
        updateState()
    }

    private fun updateState() {
        val tile = qsTile ?: return
        val connected = HidDeviceManager.getInstance(applicationContext).connectionState.value is HidDeviceManager.ConnectionState.Connected
        tile.state = if (connected) Tile.STATE_ACTIVE else Tile.STATE_UNAVAILABLE
        tile.label = "Lock Mac"
        tile.updateTile()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

/**
 * SleepMacTile - Quick Settings Tile for 'Sleep Mac'
 */
@SuppressLint("MissingPermission")
class SleepMacTile : TileService() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var observerJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        updateState()
        observerJob?.cancel()
        observerJob = scope.launch {
            HidDeviceManager.getInstance(applicationContext).connectionState.collect { updateState() }
        }
    }

    override fun onStopListening() {
        observerJob?.cancel()
        super.onStopListening()
    }

    override fun onClick() {
        val hidManager = HidDeviceManager.getInstance(applicationContext)
        if (hidManager.connectionState.value is HidDeviceManager.ConnectionState.Connected) {
            // CMD + OPT + POWER
            hidManager.sendKeyCombination(listOf(
                HidKeyCodes.MODIFIER_LEFT_GUI,
                HidKeyCodes.MODIFIER_LEFT_ALT,
                HidKeyCodes.KEY_POWER
            ))
        }
        updateState()
    }

    private fun updateState() {
        val tile = qsTile ?: return
        val connected = HidDeviceManager.getInstance(applicationContext).connectionState.value is HidDeviceManager.ConnectionState.Connected
        tile.state = if (connected) Tile.STATE_ACTIVE else Tile.STATE_UNAVAILABLE
        tile.label = "Sleep Mac"
        tile.updateTile()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
