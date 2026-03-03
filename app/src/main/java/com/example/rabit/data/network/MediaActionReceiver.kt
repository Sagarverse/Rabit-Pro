package com.example.rabit.data.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.domain.model.HidKeyCodes

/**
 * MediaActionReceiver - Feature 2
 * Receives button taps from the Now Playing notification and fires
 * the appropriate Bluetooth HID Consumer Control report to the Mac.
 */
class MediaActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.example.rabit.MEDIA_ACTION") return
        val action = intent.getStringExtra("action") ?: return
        Log.d("MediaActionReceiver", "Action received: $action")

        val hidManager = HidDeviceManager.getInstance(context)
        when (action) {
            "PREV" -> hidManager.sendConsumerKey(HidKeyCodes.MEDIA_PREVIOUS)
            "PLAY" -> hidManager.sendConsumerKey(HidKeyCodes.MEDIA_PLAY_PAUSE)
            "NEXT" -> hidManager.sendConsumerKey(HidKeyCodes.MEDIA_NEXT)
        }
    }
}
