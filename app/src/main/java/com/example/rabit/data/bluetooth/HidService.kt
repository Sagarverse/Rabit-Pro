package com.example.rabit.data.bluetooth

import android.app.*
import android.bluetooth.BluetoothDevice
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.rabit.MainActivity
import com.example.rabit.data.automation.AutomationManager
import com.example.rabit.data.network.MediaNotificationManager
import com.example.rabit.data.network.RabitNetworkServer
import com.example.rabit.domain.model.HidKeyCodes
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow

class HidService : Service() {

    private lateinit var hidDeviceManager: HidDeviceManager
    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val channelId = "hid_service_channel"
    private val clipboardChannelId = "clipboard_channel"
    private lateinit var mediaNotificationManager: MediaNotificationManager
    private lateinit var automationManager: AutomationManager
    private lateinit var encryptionManager: com.example.rabit.data.secure.EncryptionManager

    private lateinit var sensorManager: SensorManager
    private var shakeDetector: ShakeDetector? = null
    private var isShakeEnabled = false

    private lateinit var clipboard: ClipboardManager
    private var lastClipboardText: String? = null
    private var clipboardJob: Job? = null

    inner class LocalBinder : Binder() {
        fun getService(): HidService = this@HidService
    }

    override fun onCreate() {
        super.onCreate()
        hidDeviceManager = HidDeviceManager.getInstance(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        mediaNotificationManager = MediaNotificationManager(this)
        automationManager = com.example.rabit.data.automation.AutomationManager(this)
        encryptionManager = com.example.rabit.data.secure.EncryptionManager(this)

        RabitNetworkServer.onMediaMetadataReceived = { metadata ->
            serviceScope.launch { mediaNotificationManager.updateMedia(metadata) }
        }
        RabitNetworkServer.start(this, encryptionManager)

        createNotificationChannels()
        startForeground(1, buildNotification("Disconnected", "Bluetooth HID connection is inactive."))
        observeConnectionState()
        setupShakeDetector()
        startClipboardObserver()
    }

    private fun startClipboardObserver() {
        clipboardJob?.cancel()
        
        try {
            val currentClip = clipboard.primaryClip
            if (currentClip != null && currentClip.itemCount > 0) {
                lastClipboardText = currentClip.getItemAt(0).text?.toString()
            }
        } catch (e: Exception) { }

        clipboardJob = serviceScope.launch {
            while (isActive) {
                try {
                    val primaryClip = clipboard.primaryClip
                    if (primaryClip != null && primaryClip.itemCount > 0) {
                        val text = primaryClip.getItemAt(0).text?.toString()
                        if (text != lastClipboardText && !text.isNullOrBlank()) {
                            lastClipboardText = text
                            val prefs = getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)
                            val isAutoPush = prefs.getBoolean("auto_push_enabled", false)
                            
                            if (isAutoPush) {
                                sendText(text)
                            } else {
                                showClipboardNotification(text)
                            }
                        }
                    }
                } catch (e: Exception) { }
                delay(3000)
            }
        }
    }

    private fun setupShakeDetector() {
        val prefs = getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)
        isShakeEnabled = prefs.getBoolean("shake_to_control_calls", false)
        
        if (isShakeEnabled) {
            registerShakeDetector()
        }
    }

    private fun registerShakeDetector() {
        if (shakeDetector == null) {
            shakeDetector = ShakeDetector { type ->
                if (hidDeviceManager.connectionState.value is HidDeviceManager.ConnectionState.Connected) {
                    when (type) {
                        ShakeDetector.ShakeType.VERTICAL -> {
                            Log.d("HidService", "Vertical shake: Answering call via HID")
                            sendConsumerKey(HidKeyCodes.CALL_ANSWER)
                        }
                        ShakeDetector.ShakeType.HORIZONTAL -> {
                            Log.d("HidService", "Horizontal shake: Rejecting call via HID")
                            sendConsumerKey(HidKeyCodes.CALL_REJECT)
                        }
                        else -> {}
                    }
                }
            }
        }
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    private fun unregisterShakeDetector() {
        shakeDetector?.let { sensorManager.unregisterListener(it) }
    }

    fun setShakeEnabled(enabled: Boolean) {
        if (isShakeEnabled == enabled) return
        isShakeEnabled = enabled
        if (enabled) registerShakeDetector() else unregisterShakeDetector()
    }

    private fun observeConnectionState() {
        serviceScope.launch {
            hidDeviceManager.connectionState.collect { state ->
                val (title, text) = when (state) {
                    is HidDeviceManager.ConnectionState.Connected -> {
                        automationManager.onConnected()
                        "Connected" to "Active connection to ${state.deviceName}"
                    }
                    is HidDeviceManager.ConnectionState.Connecting ->
                        "Connecting" to "Attempting to connect..."
                    else -> {
                        automationManager.onDisconnected()
                        "Disconnected" to "Bluetooth HID connection is inactive."
                    }
                }
                updateNotification(title, text)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "SEND_NOTIFICATION" -> {
                val content = intent.getStringExtra("content") ?: ""
                sendText(content + "\n")
            }
            "UPDATE_SHAKE_SETTINGS" -> {
                val enabled = intent.getBooleanExtra("enabled", false)
                setShakeEnabled(enabled)
            }
            "PUSH_CLIPBOARD" -> {
                val content = intent.getStringExtra("text") ?: ""
                sendText(content)
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.cancel(2)
            }
            "TOGGLE_AUTO_PUSH" -> {
                val prefs = getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)
                val current = prefs.getBoolean("auto_push_enabled", false)
                prefs.edit().putBoolean("auto_push_enabled", !current).apply()
                val status = if (!current) "Enabled" else "Disabled"
                Toast.makeText(this, "Auto-Push $status", Toast.LENGTH_SHORT).show()
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.cancel(2)
            }
            "DISMISS_CLIPBOARD" -> {
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.cancel(2)
            }
            "SHOW_CLIPBOARD_NOTIFICATION" -> {
                val text = intent.getStringExtra("text") ?: ""
                showClipboardNotification(text)
            }
        }
        return START_STICKY
    }

    private fun showClipboardNotification(text: String) {
        val prefs = getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)
        val isAutoPush = prefs.getBoolean("auto_push_enabled", false)

        val pushIntent = Intent(this, HidService::class.java).apply {
            action = "PUSH_CLIPBOARD"
            putExtra("text", text)
        }
        val pushPendingIntent = PendingIntent.getService(this, 0, pushIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val toggleAutoPushIntent = Intent(this, HidService::class.java).apply {
            action = "TOGGLE_AUTO_PUSH"
        }
        val toggleAutoPushPendingIntent = PendingIntent.getService(this, 2, toggleAutoPushIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val dismissIntent = Intent(this, HidService::class.java).apply {
            action = "DISMISS_CLIPBOARD"
        }
        val dismissPendingIntent = PendingIntent.getService(this, 1, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, clipboardChannelId)
            .setSmallIcon(android.R.drawable.ic_menu_set_as)
            .setContentTitle("Clipboard Detected")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_send, "Push to Mac", pushPendingIntent)
            .addAction(
                if (isAutoPush) android.R.drawable.checkbox_on_background else android.R.drawable.checkbox_off_background,
                if (isAutoPush) "Auto-Push: ON" else "Auto-Push: OFF",
                toggleAutoPushPendingIntent
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(2, notification)
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        // Keep service running even if app is swiped away
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            
            val serviceChannel = NotificationChannel(
                channelId, "Rabit Background Service", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Bluetooth connection active while app is in use."
            }
            manager.createNotificationChannel(serviceChannel)

            val clipboardChannel = NotificationChannel(
                clipboardChannelId, "Clipboard Sync", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for clipboard sync features."
            }
            manager.createNotificationChannel(clipboardChannel)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Rabit: $title")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(title: String, text: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(1, buildNotification(title, text))
    }

    val connectionState: StateFlow<HidDeviceManager.ConnectionState> get() = hidDeviceManager.connectionState

    fun connect(device: BluetoothDevice) = hidDeviceManager.connect(device)
    fun disconnect() = hidDeviceManager.disconnect()
    fun sendKey(keyCode: Byte, modifier: Byte) = hidDeviceManager.sendKeyPress(keyCode, modifier)
    fun sendConsumerKey(usageId: Short) = hidDeviceManager.sendConsumerKey(usageId)
    fun sendText(text: String) = hidDeviceManager.sendText(text)
    fun unlockMac(password: String) = hidDeviceManager.unlockMac(password)

    override fun onDestroy() {
        RabitNetworkServer.stop()
        mediaNotificationManager.clearNotification()
        automationManager.destroy()
        serviceScope.cancel()
        unregisterShakeDetector()
        hidDeviceManager.unregister()
        super.onDestroy()
    }
}
