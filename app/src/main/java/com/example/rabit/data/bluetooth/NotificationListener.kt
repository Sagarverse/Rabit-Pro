package com.example.rabit.data.bluetooth

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.content.Intent
import android.os.IBinder

class RabitNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        // Avoid self-notification loops
        if (packageName == "com.example.rabit") return

        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: "Notification"
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        Log.d("NotificationListener", "Notification from $packageName: $title - $text")
        
        // Send to Mac via HID (as text)
        val serviceIntent = Intent(this, HidService::class.java).apply {
            action = "SEND_NOTIFICATION"
            putExtra("content", "[$title]: $text")
        }
        try {
            startService(serviceIntent)
        } catch (e: Exception) {
            Log.e("NotificationListener", "Failed to start HidService", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) { }
}
