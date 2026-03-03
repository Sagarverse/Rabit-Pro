package com.example.rabit.data.network

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.rabit.data.bluetooth.HidDeviceManager
import com.example.rabit.domain.model.HidKeyCodes

/**
 * MediaNotificationManager - Feature 2
 * Shows a persistent, interactive media player notification on Android
 * when media is playing on the connected Mac.
 *
 * Tapping a control fires a real Bluetooth HID Consumer Key report to the Mac,
 * giving hardware-level media control with zero additional setup.
 *
 * Uses the platform android.media.session.MediaSession (API 21+, no extra deps).
 */
class MediaNotificationManager(private val context: Context) {

    private val channelId = "rabit_media_channel"
    private val notifId = 2001
    private val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var mediaSession: MediaSession? = null

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                channelId,
                "Rabit Media Controls",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Now Playing from your Mac" }
            notifManager.createNotificationChannel(ch)
        }
    }

    fun updateMedia(metadata: RabitNetworkServer.MediaMetadata) {
        // Release previous session
        mediaSession?.release()

        val session = MediaSession(context, "RabitMedia")
        session.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, metadata.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, metadata.artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, metadata.album)
                .build()
        )
        session.setPlaybackState(
            PlaybackState.Builder()
                .setState(PlaybackState.STATE_PLAYING, 0, 1f)
                .setActions(
                    PlaybackState.ACTION_PLAY_PAUSE or
                    PlaybackState.ACTION_SKIP_TO_NEXT or
                    PlaybackState.ACTION_SKIP_TO_PREVIOUS
                )
                .build()
        )
        session.setCallback(object : MediaSession.Callback() {
            override fun onPlay() { sendHidKey(HidKeyCodes.MEDIA_PLAY_PAUSE) }
            override fun onPause() { sendHidKey(HidKeyCodes.MEDIA_PLAY_PAUSE) }
            override fun onSkipToNext() { sendHidKey(HidKeyCodes.MEDIA_NEXT) }
            override fun onSkipToPrevious() { sendHidKey(HidKeyCodes.MEDIA_PREVIOUS) }
            override fun onStop() { clearNotification() }
        })
        session.isActive = true
        mediaSession = session

        val token = session.sessionToken
        val notification = buildNotification(metadata, token)
        notifManager.notify(notifId, notification)
    }

    private fun buildNotification(
        meta: RabitNetworkServer.MediaMetadata,
        token: MediaSession.Token
    ): Notification {
        val prevIntent  = buildAction("PREV", android.R.drawable.ic_media_previous)
        val ppIntent    = buildAction("PLAY", android.R.drawable.ic_media_play)
        val nextIntent  = buildAction("NEXT", android.R.drawable.ic_media_next)

        val style = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.MediaStyle().setMediaSession(token)
                .setShowActionsInCompactView(0, 1, 2)
        } else null

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, channelId)
                .setContentTitle(meta.title.ifBlank { "Now Playing" })
                .setContentText(buildSubtitle(meta))
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .setStyle(style)
                .addAction(buildNativeAction("PREV", android.R.drawable.ic_media_previous))
                .addAction(buildNativeAction("PLAY", android.R.drawable.ic_media_play))
                .addAction(buildNativeAction("NEXT", android.R.drawable.ic_media_next))
                .build()
        } else {
            // Fallback for < Oreo using NotificationCompat
            NotificationCompat.Builder(context, channelId)
                .setContentTitle(meta.title.ifBlank { "Now Playing" })
                .setContentText(buildSubtitle(meta))
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .addAction(NotificationCompat.Action(android.R.drawable.ic_media_previous, "Previous", prevIntent))
                .addAction(NotificationCompat.Action(android.R.drawable.ic_media_play, "Play/Pause", ppIntent))
                .addAction(NotificationCompat.Action(android.R.drawable.ic_media_next, "Next", nextIntent))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        }
    }

    private fun buildSubtitle(meta: RabitNetworkServer.MediaMetadata): String =
        "${meta.artist}${if (meta.album.isNotBlank()) " • ${meta.album}" else ""}"

    private fun buildAction(action: String, icon: Int): PendingIntent {
        val intent = Intent("com.example.rabit.MEDIA_ACTION").apply {
            setPackage(context.packageName)
            putExtra("action", action)
        }
        return PendingIntent.getBroadcast(
            context, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildNativeAction(action: String, icon: Int): Notification.Action {
        return Notification.Action.Builder(
            android.graphics.drawable.Icon.createWithResource(context, icon),
            action,
            buildAction(action, icon)
        ).build()
    }

    private fun sendHidKey(key: Short) {
        HidDeviceManager.getInstance(context).sendConsumerKey(key)
    }

    fun clearNotification() {
        notifManager.cancel(notifId)
        mediaSession?.release()
        mediaSession = null
    }
}
