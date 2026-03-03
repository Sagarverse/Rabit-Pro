package com.example.rabit.data.bluetooth

import android.app.*
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
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
    private lateinit var mediaNotificationManager: MediaNotificationManager
    private lateinit var automationManager: AutomationManager
    private lateinit var encryptionManager: com.example.rabit.data.secure.EncryptionManager

    private lateinit var sensorManager: SensorManager
    private var shakeDetector: ShakeDetector? = null
    private var isShakeEnabled = false

    inner class LocalBinder : Binder() {
        fun getService(): HidService = this@HidService
    }

    override fun onCreate() {
        super.onCreate()
        hidDeviceManager = HidDeviceManager.getInstance(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Feature 2: Set up media notification manager
        mediaNotificationManager = MediaNotificationManager(this)

        // Feature 4: Set up automation manager
        automationManager = com.example.rabit.data.automation.AutomationManager(this)

        // Feature 5: Set up encryption manager
        encryptionManager = com.example.rabit.data.secure.EncryptionManager(this)

        // Start the Ktor HTTP server and wire callbacks
        RabitNetworkServer.onMediaMetadataReceived = { metadata ->
            serviceScope.launch { mediaNotificationManager.updateMedia(metadata) }
        }
        RabitNetworkServer.start(this, encryptionManager)

        createNotificationChannel()
        startForeground(1, buildNotification("Disconnected", "Bluetooth HID connection is inactive."))
        observeConnectionState()
        setupShakeDetector()
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
                        // Feature 4: trigger automations on connect
                        automationManager.onConnected()
                        "Connected" to "Active connection to ${state.deviceName}"
                    }
                    is HidDeviceManager.ConnectionState.Connecting ->
                        "Connecting" to "Attempting to connect..."
                    else -> {
                        // Feature 4: restore phone on disconnect
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
        }
        return START_NOT_STICKY
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Rabit Background Service", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Bluetooth connection active while app is in use."
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
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
