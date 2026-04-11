package com.example.rabit.ui.assistant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object AssistantNotifier {
    private const val CHANNEL_ID = "gemini_response_channel"
    private const val NOTIFICATION_ID = 1001

    fun showNotification(context: Context, message: String) {
        createChannel(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("AI Response")
            .setContentText(message.take(60))
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) { /* permission not granted */ }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AI Responses",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for AI replies"
            }
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    fun vibrate(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(VibratorManager::class.java)
            vm?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(
                VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            @Suppress("DEPRECATION")
            vibrator?.vibrate(80)
        }
    }
}
