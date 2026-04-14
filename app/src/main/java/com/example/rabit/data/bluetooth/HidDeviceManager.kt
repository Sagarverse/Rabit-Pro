package com.example.rabit.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Parcelable
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

@SuppressLint("MissingPermission")
class HidDeviceManager private constructor(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = context.getSystemService(BluetoothManager::class.java)?.adapter
    private var hidDevice: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null
    private var deviceRepository: com.example.rabit.domain.repository.DeviceRepository? = null
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val executor = Executors.newSingleThreadExecutor()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var reconnectJob: Job? = null
    private var connectionTimeoutJob: Job? = null
    private var isManuallyDisconnected = false
    private var pendingBondAddress: String? = null
    private var pendingConnectRetries: Int = 3
    private var pendingRetryDelayMs: Long = 1500

    private data class ReportRequest(val id: Int, val data: ByteArray)
    private val reportChannel = Channel<ReportRequest>(Channel.UNLIMITED)

    var typingDelay = 120L 
    private var currentModifiers: Byte = 0
    private var textPushJob: Job? = null
    private val consumerKeyLock = Any()
    private var lastConsumerUsageId: Short = 0
    private var lastConsumerSentAtMs: Long = 0L

    private val _isPushPaused = MutableStateFlow(false)
    val isPushPaused: StateFlow<Boolean> = _isPushPaused.asStateFlow()

    private val _isTextPushing = MutableStateFlow(false)
    val isTextPushing: StateFlow<Boolean> = _isTextPushing.asStateFlow()

    private var mouseAccumX = 0f
    private var mouseAccumY = 0f

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
                    handleBluetoothOff()
                } else if (state == BluetoothAdapter.STATE_ON) {
                    initProfiles()
                }
            }
        }
    }

    private val bondStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BluetoothDevice.ACTION_BOND_STATE_CHANGED) return

            val device = intent.parcelableExtraCompat<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return
            if (pendingBondAddress != device.address) return

            val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
            val previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_NONE)

            when (bondState) {
                BluetoothDevice.BOND_BONDED -> {
                    pendingBondAddress = null
                    connectWithRetry(device, pendingConnectRetries, pendingRetryDelayMs)
                }
                BluetoothDevice.BOND_NONE -> {
                    if (previousBondState == BluetoothDevice.BOND_BONDING) {
                        pendingBondAddress = null
                        _connectionState.value = ConnectionState.Disconnected
                    }
                }
            }
        }
    }

    private val profileServiceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                Log.d("HidDeviceManager", "HID profile connected")
                hidDevice = proxy as BluetoothHidDevice
                registerApp()
                checkCurrentConnections()
            }
        }
        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = null
                _connectionState.value = ConnectionState.Disconnected
            }
        }
    }

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            super.onConnectionStateChanged(device, state)
            Log.d("HidDeviceManager", "Connection state changed: $state for device ${device?.name}")
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    reconnectJob?.cancel()
                    connectionTimeoutJob?.cancel()
                    connectedDevice = device
                    _connectionState.value = ConnectionState.Connected(device?.name ?: "Unknown")
                    
                    // Save to known workstations
                    device?.let { 
                        deviceRepository?.saveWorkstation(it.address, it.name ?: "Unknown Workstation")
                    }
                    
                    isManuallyDisconnected = false
                    scope.launch { 
                        delay(300) // Reduced from 1000ms for faster initialization
                        sendReportInternal(1, ByteArray(8)) 
                    } 
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectedDevice = null
                    _connectionState.value = ConnectionState.Disconnected
                }
            }
        }
    }

    init {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(bluetoothStateReceiver, filter)
        val bondFilter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        context.registerReceiver(bondStateReceiver, bondFilter)
        initProfiles()
        
        deviceRepository = com.example.rabit.data.repository.DeviceRepositoryImpl(context)
        
        scope.launch {
            for (request in reportChannel) {
                sendReportInternal(request.id, request.data)
            }
        }
    }

    private fun handleBluetoothOff() {
        reconnectJob?.cancel()
        connectedDevice = null
        _connectionState.value = ConnectionState.Disconnected
        hidDevice = null
    }

    fun initProfiles() {
        if (bluetoothAdapter?.isEnabled == true) {
            bluetoothAdapter.getProfileProxy(context, profileServiceListener, BluetoothProfile.HID_DEVICE)
        }
    }

    private fun registerApp() {
        scope.launch(Dispatchers.IO) {
            if (bluetoothAdapter?.name != "Rabit Keyboard & Mouse") {
                bluetoothAdapter?.name = "Rabit Keyboard & Mouse"
            }
            
            val sdpSettings = BluetoothHidDeviceAppSdpSettings(
                "Rabit Keyboard & Mouse", "Keyboard + Mouse", "Rabit",
                0xC0.toByte(), // 0xC0 = Keyboard (0x40) | Mouse (0x80)
                HID_REPORT_DESCRIPTOR
            )
            hidDevice?.registerApp(sdpSettings, null, null, executor, callback)
        }
    }

    fun requestDiscoverable() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun connect(device: BluetoothDevice) {
        if (bluetoothAdapter?.isEnabled != true) return
        isManuallyDisconnected = false
        _connectionState.value = ConnectionState.Connecting
        
        scope.launch(Dispatchers.IO) {
            ensureHidProfileReady()
            if (device.bondState != BluetoothDevice.BOND_BONDED) {
                pendingBondAddress = device.address
                pendingConnectRetries = 3
                pendingRetryDelayMs = 1500
                try {
                    val startedBonding = device.createBond()
                    if (!startedBonding && device.bondState != BluetoothDevice.BOND_BONDING) {
                        _connectionState.value = ConnectionState.Disconnected
                    }
                } catch (e: Exception) {
                    _connectionState.value = ConnectionState.Disconnected
                }
                return@launch
            }

            hidDevice?.connect(device)
            
            // Start connection timeout within the background scope
            connectionTimeoutJob?.cancel()
            connectionTimeoutJob = launch {
                delay(8000)
                if (_connectionState.value is ConnectionState.Connecting) {
                    _connectionState.value = ConnectionState.Disconnected
                }
            }
        }
    }

    /**
     * Attempts to connect up to [maxRetries] times with [retryDelayMs] gaps.
     * Useful for Windows devices that often need multiple connection attempts.
     */
    fun connectWithRetry(device: BluetoothDevice, maxRetries: Int = 3, retryDelayMs: Long = 1500) {
        if (bluetoothAdapter?.isEnabled != true) return
        isManuallyDisconnected = false
        _connectionState.value = ConnectionState.Connecting
        
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            ensureHidProfileReady()
            if (device.bondState != BluetoothDevice.BOND_BONDED) {
                pendingBondAddress = device.address
                pendingConnectRetries = maxRetries
                pendingRetryDelayMs = retryDelayMs
                try {
                    val startedBonding = device.createBond()
                    if (!startedBonding && device.bondState != BluetoothDevice.BOND_BONDING) {
                        _connectionState.value = ConnectionState.Disconnected
                    }
                } catch (e: Exception) {
                    _connectionState.value = ConnectionState.Disconnected
                }
                return@launch
            }

            repeat(maxRetries) { attempt ->
                if (_connectionState.value is ConnectionState.Connected) return@launch
                Log.d("HidDeviceManager", "Connection attempt ${attempt + 1}/$maxRetries for ${device.name}")
                hidDevice?.connect(device)
                delay(retryDelayMs)
            }
            // After all retries, if still not connected, emit Disconnected
            delay(3000) // Give the last attempt a chance
            if (_connectionState.value !is ConnectionState.Connected) {
                _connectionState.value = ConnectionState.Disconnected
            }
        }
    }

    fun disconnect() {
        isManuallyDisconnected = true
        reconnectJob?.cancel()
        connectionTimeoutJob?.cancel()
        textPushJob?.cancel()
        connectedDevice?.let { hidDevice?.disconnect(it) }
        _connectionState.value = ConnectionState.Disconnected
    }

    private fun sendReportInternal(id: Int, data: ByteArray) {
        val device = connectedDevice ?: return
        if (bluetoothAdapter?.isEnabled != true) return
        try {
            val success = hidDevice?.sendReport(device, id, data) ?: false
            if (!success) Log.e("HidDeviceManager", "Failed to send report $id")
        } catch (e: Exception) {
            Log.e("HidDeviceManager", "Error sending report", e)
        }
    }

    fun setModifier(modifier: Byte, active: Boolean) {
        currentModifiers = if (active) {
            currentModifiers or modifier
        } else {
            currentModifiers and modifier.inv()
        }
        val report = ByteArray(8).apply { this[0] = currentModifiers }
        reportChannel.trySend(ReportRequest(1, report))
    }

    fun sendKeyPress(keyCode: Byte, modifier: Byte = 0, useSticky: Boolean = true) {
        val effectiveModifier = if (modifier != 0.toByte()) {
            modifier or if (useSticky) currentModifiers else 0
        } else if (useSticky) {
            currentModifiers
        } else {
            0.toByte()
        }
        
        val pressReport = ByteArray(8).apply { 
            this[0] = effectiveModifier
            this[2] = keyCode 
        }
        reportChannel.trySend(ReportRequest(1, pressReport))
        
        scope.launch { 
            delay(50)
            val releaseReport = ByteArray(8).apply { 
                this[0] = if (useSticky) currentModifiers else 0 
            }
            reportChannel.trySend(ReportRequest(1, releaseReport)) 
        }
    }

    fun sendKeyCombination(codes: List<Byte>) {
        scope.launch {
            val modifiersSet = listOf(
                com.example.rabit.domain.model.HidKeyCodes.MODIFIER_LEFT_CTRL,
                com.example.rabit.domain.model.HidKeyCodes.MODIFIER_LEFT_SHIFT,
                com.example.rabit.domain.model.HidKeyCodes.MODIFIER_LEFT_ALT,
                com.example.rabit.domain.model.HidKeyCodes.MODIFIER_LEFT_GUI
            )
            val modifiers = codes.filter { it in modifiersSet }
            val mainKey = codes.firstOrNull { it !in modifiersSet } ?: (0x00).toByte()
            
            var combinedMod: Byte = 0
            modifiers.forEach { combinedMod = combinedMod or it }
            
            sendKeyPress(mainKey, combinedMod, useSticky = false)
        }
    }

    fun sendConsumerKey(usageId: Short) {
        val now = SystemClock.elapsedRealtime()
        val shouldDrop = synchronized(consumerKeyLock) {
            val duplicateTooFast = usageId == lastConsumerUsageId && (now - lastConsumerSentAtMs) < 180L
            if (!duplicateTooFast) {
                lastConsumerUsageId = usageId
                lastConsumerSentAtMs = now
            }
            duplicateTooFast
        }
        if (shouldDrop) return

        val pressReport = ByteArray(2).apply {
            this[0] = (usageId.toInt() and 0xFF).toByte()
            this[1] = ((usageId.toInt() shr 8) and 0xFF).toByte()
        }

        // Defensive sequence for host stability: release -> press -> release.
        reportChannel.trySend(ReportRequest(2, ByteArray(2)))
        scope.launch {
            delay(8)
            reportChannel.trySend(ReportRequest(2, pressReport))
            delay(22)
            reportChannel.trySend(ReportRequest(2, ByteArray(2)))
        }
    }

    fun resetMouseAccumulator() {
        mouseAccumX = 0f
        mouseAccumY = 0f
    }

    fun sendMouseMove(dx: Float, dy: Float, buttons: Int = 0, wheel: Int = 0) {
        mouseAccumX += dx
        mouseAccumY += dy
        
        val outX = mouseAccumX.toInt()
        val outY = mouseAccumY.toInt()
        
        if (outX != 0 || outY != 0 || buttons != 0 || wheel != 0) {
            mouseAccumX -= outX
            mouseAccumY -= outY
            
            val report = ByteArray(4).apply {
                this[0] = buttons.toByte()
                this[1] = outX.coerceIn(-127, 127).toByte()
                this[2] = outY.coerceIn(-127, 127).toByte()
                this[3] = wheel.coerceIn(-127, 127).toByte()
            }
            reportChannel.trySend(ReportRequest(3, report))
        }
    }

    fun sendText(text: String) {
        textPushJob?.cancel()
        _isPushPaused.value = false
        _isTextPushing.value = true
        textPushJob = scope.launch {
            try {
                text.forEach { char ->
                    while (_isPushPaused.value) {
                        delay(100)
                    }
                    val model = com.example.rabit.domain.model.HidKeyCodes.getHidCode(char)
                    if (model.keyCode != 0.toByte() || model.modifier != 0.toByte()) {
                        sendKeyPress(model.keyCode, model.modifier, useSticky = false)
                        delay(typingDelay) 
                    }
                }
            } finally {
                _isTextPushing.value = false
            }
        }
    }

    fun pauseTextPush() {
        _isPushPaused.value = true
    }

    fun resumeTextPush() {
        _isPushPaused.value = false
    }

    fun stopTextPush() {
        textPushJob?.cancel()
        _isPushPaused.value = false
        _isTextPushing.value = false
        reportChannel.trySend(ReportRequest(1, ByteArray(8)))
    }

    fun sendDigitizerInput(x: Int, y: Int, isPressed: Boolean, inRange: Boolean) {
        val report = ByteArray(5).apply {
            var flags = 0
            if (isPressed) flags = flags or 0x01
            if (inRange) flags = flags or 0x02
            this[0] = flags.toByte()
            this[1] = (x and 0xFF).toByte()
            this[2] = ((x shr 8) and 0xFF).toByte()
            this[3] = (y and 0xFF).toByte()
            this[4] = ((y shr 8) and 0xFF).toByte()
        }
        reportChannel.trySend(ReportRequest(4, report))
    }

    fun unlockMac(
        password: String,
        pressEnterBefore: Boolean = true,
        pressEnterAfter: Boolean = true,
        preTypeDelayMs: Long = 1500L,
        postTypeDelayMs: Long = 800L
    ) {
        scope.launch {
            if (pressEnterBefore) {
                sendKeyPress(0x28, useSticky = false) // Enter
            }
            delay(preTypeDelayMs.coerceIn(0L, 10_000L))
            sendText(password)
            delay(postTypeDelayMs.coerceIn(0L, 10_000L))
            if (pressEnterAfter) {
                sendKeyPress(0x28, useSticky = false) // Enter
            }
        }
    }

    private fun checkCurrentConnections() {
        val devices = hidDevice?.getDevicesMatchingConnectionStates(intArrayOf(BluetoothProfile.STATE_CONNECTED))
        if (!devices.isNullOrEmpty()) {
            connectedDevice = devices[0]
            _connectionState.value = ConnectionState.Connected(connectedDevice?.name ?: "Unknown")
        }
    }

    fun unregister() {
        try { context.unregisterReceiver(bluetoothStateReceiver) } catch (e: Exception) { }
        try { context.unregisterReceiver(bondStateReceiver) } catch (e: Exception) { }
        hidDevice?.unregisterApp()
        bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
    }

    private suspend fun ensureHidProfileReady(timeoutMs: Long = 2500) {
        if (hidDevice != null) return
        initProfiles()
        val deadline = System.currentTimeMillis() + timeoutMs
        while (hidDevice == null && System.currentTimeMillis() < deadline) {
            delay(120)
        }
    }

    private inline fun <reified T : Parcelable> Intent.parcelableExtraCompat(key: String): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(key) as? T
        }
    }

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        data class Connected(val deviceName: String) : ConnectionState()
    }

    companion object {
        @Volatile
        private var INSTANCE: HidDeviceManager? = null

        fun getInstance(context: Context): HidDeviceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HidDeviceManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        private val HID_REPORT_DESCRIPTOR = byteArrayOf(
            // Keyboard (ID 1)
            0x05.toByte(), 0x01.toByte(), // USAGE_PAGE (Generic Desktop)
            0x09.toByte(), 0x06.toByte(), // USAGE (Keyboard)
            0xA1.toByte(), 0x01.toByte(), // COLLECTION (Application)
            0x85.toByte(), 0x01.toByte(), //   REPORT_ID (1)
            0x05.toByte(), 0x07.toByte(), //   USAGE_PAGE (Keyboard)
            0x19.toByte(), 0xE0.toByte(), //   USAGE_MINIMUM (Keyboard LeftControl)
            0x29.toByte(), 0xE7.toByte(), //   USAGE_MAXIMUM (Keyboard Right GUI)
            0x15.toByte(), 0x00.toByte(), //   LOGICAL_MINIMUM (0)
            0x25.toByte(), 0x01.toByte(), //   LOGICAL_MAXIMUM (1)
            0x75.toByte(), 0x01.toByte(), //   REPORT_SIZE (1)
            0x95.toByte(), 0x08.toByte(), //   REPORT_COUNT (8)
            0x81.toByte(), 0x02.toByte(), //   INPUT (Data,Var,Abs)
            0x95.toByte(), 0x01.toByte(), //   REPORT_COUNT (1)
            0x75.toByte(), 0x08.toByte(), //   REPORT_SIZE (8)
            0x81.toByte(), 0x03.toByte(), //   INPUT (Cnst,Var,Abs)
            0x95.toByte(), 0x06.toByte(), //   REPORT_COUNT (6)
            0x75.toByte(), 0x08.toByte(), //   REPORT_SIZE (8)
            0x15.toByte(), 0x00.toByte(), //   LOGICAL_MINIMUM (0)
            0x25.toByte(), 0x65.toByte(), //   LOGICAL_MAXIMUM (101)
            0x05.toByte(), 0x07.toByte(), //   USAGE_PAGE (Keyboard)
            0x19.toByte(), 0x00.toByte(), //   USAGE_MINIMUM (Reserved)
            0x29.toByte(), 0x65.toByte(), //   USAGE_MAXIMUM (Keyboard Application)
            0x81.toByte(), 0x00.toByte(), //   INPUT (Data,Ary,Abs)
            0xC0.toByte(),               // END_COLLECTION
            
            // Consumer Control (ID 2)
            0x05.toByte(), 0x0C.toByte(), // USAGE_PAGE (Consumer Devices)
            0x09.toByte(), 0x01.toByte(), // USAGE (Consumer Control)
            0xA1.toByte(), 0x01.toByte(), // COLLECTION (Application)
            0x85.toByte(), 0x02.toByte(), //   REPORT_ID (2)
            0x15.toByte(), 0x00.toByte(), //   LOGICAL_MINIMUM (0)
            0x26.toByte(), 0xFF.toByte(), 0x03.toByte(), // LOGICAL_MAXIMUM (1023)
            0x19.toByte(), 0x00.toByte(), //   USAGE_MINIMUM (0)
            0x2A.toByte(), 0xFF.toByte(), 0x03.toByte(), // USAGE_MAXIMUM (1023)
            0x75.toByte(), 0x10.toByte(), //   REPORT_SIZE (16)
            0x95.toByte(), 0x01.toByte(), //   REPORT_COUNT (1)
            0x81.toByte(), 0x00.toByte(), //   INPUT (Data,Ary,Abs)
            0xC0.toByte(),                // END_COLLECTION

            // Mouse (ID 3)
            0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop)
            0x09.toByte(), 0x02.toByte(), // Usage (Mouse)
            0xA1.toByte(), 0x01.toByte(), // Collection (Application)
            0x85.toByte(), 0x03.toByte(), //   Report ID (3)
            0x09.toByte(), 0x01.toByte(), //   Usage (Pointer)
            0xA1.toByte(), 0x00.toByte(), //   Collection (Physical)
            0x05.toByte(), 0x09.toByte(), //     Usage Page (Button)
            0x19.toByte(), 0x01.toByte(), //     Usage Minimum (1)
            0x29.toByte(), 0x03.toByte(), //     Usage Maximum (3)
            0x15.toByte(), 0x00.toByte(), //     Logical Minimum (0)
            0x25.toByte(), 0x01.toByte(), //     Logical Maximum (1)
            0x95.toByte(), 0x03.toByte(), //     Report Count (3)
            0x75.toByte(), 0x01.toByte(), //     Report Size (1)
            0x81.toByte(), 0x02.toByte(), //     Input (Data,Var,Abs)
            0x95.toByte(), 0x01.toByte(), //     Report Count (1)
            0x75.toByte(), 0x05.toByte(), //     Report Size (5)
            0x81.toByte(), 0x03.toByte(), //     Input (Cnst,Var,Abs)
            0x05.toByte(), 0x01.toByte(), //     Usage Page (Generic Desktop)
            0x09.toByte(), 0x30.toByte(), //     Usage (X)
            0x09.toByte(), 0x31.toByte(), //     Usage (Y)
            0x09.toByte(), 0x38.toByte(), //     Usage (Wheel)
            0x15.toByte(), 0x81.toByte(), //     Logical Minimum (-127)
            0x25.toByte(), 0x7F.toByte(), //     Logical Maximum (127)
            0x75.toByte(), 0x08.toByte(), //     Report Size (8)
            0x95.toByte(), 0x03.toByte(), //     Report Count (3)
            0x81.toByte(), 0x06.toByte(), //     Input (Data,Var,Rel)
            0xC0.toByte(),               //   End Collection
            0xC0.toByte(),               // End Collection

            // Digitizer / Pen (ID 4)
            0x05.toByte(), 0x0D.toByte(), // Usage Page (Digitizer)
            0x09.toByte(), 0x02.toByte(), // Usage (Pen)
            0xA1.toByte(), 0x01.toByte(), // Collection (Application)
            0x85.toByte(), 0x04.toByte(), //   Report ID (4)
            0x09.toByte(), 0x20.toByte(), //   Usage (Stylus)
            0xA1.toByte(), 0x00.toByte(), //   Collection (Physical)
            0x09.toByte(), 0x42.toByte(), //     Usage (Tip Switch)
            0x09.toByte(), 0x32.toByte(), //     Usage (In Range)
            0x15.toByte(), 0x00.toByte(), //     Logical Minimum (0)
            0x25.toByte(), 0x01.toByte(), //     Logical Maximum (1)
            0x75.toByte(), 0x01.toByte(), //     Report Size (1)
            0x95.toByte(), 0x02.toByte(), //     Report Count (2)
            0x81.toByte(), 0x02.toByte(), //     Input (Data,Var,Abs)
            0x95.toByte(), 0x01.toByte(), //     Report Count (1)
            0x75.toByte(), 0x06.toByte(), //     Report Size (6)
            0x81.toByte(), 0x03.toByte(), //     Input (Cnst,Var,Abs) - Padding
            0x05.toByte(), 0x01.toByte(), //     Usage Page (Generic Desktop)
            0x09.toByte(), 0x30.toByte(), //     Usage (X)
            0x09.toByte(), 0x31.toByte(), //     Usage (Y)
            0x16.toByte(), 0x00.toByte(), 0x00.toByte(), // Logical Minimum (0)
            0x26.toByte(), 0xFF.toByte(), 0x7F.toByte(), // Logical Maximum (32767)
            0x75.toByte(), 0x10.toByte(), //     Report Size (16)
            0x95.toByte(), 0x02.toByte(), //     Report Count (2)
            0x81.toByte(), 0x02.toByte(), //     Input (Data,Var,Abs)
            0xC0.toByte(),                //   End Collection
            0xC0.toByte()                 // End Collection
        )
    }
}
