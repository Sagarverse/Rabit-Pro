package com.example.rabit.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.math.abs
import kotlin.math.sign

/**
 * GyroscopeAirMouse — Overhauled for "Pro" precision and Laser-Pointer style control.
 *
 * This version eliminates inertia and jitter, ensuring the cursor follows the phone's 
 * physical pointing direction with pixel-perfect accuracy.
 */
class GyroscopeAirMouse(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _deltaChannel = Channel<Pair<Float, Float>>(Channel.CONFLATED)
    val deltaFlow: Flow<Pair<Float, Float>> = _deltaChannel.receiveAsFlow()

    // Shake detection
    var onShakeDetected: (() -> Unit)? = null
    private var lastShakeTime = 0L
    private var strongShakeSpikeCount = 0
    private var lastStrongSpikeTime = 0L
    private val SHAKE_THRESHOLD = 22.5f // Require strong movement to avoid accidental disconnects.

    // Calibration state
    private var hasCalibration = false
    private val calibrationOrientation = FloatArray(3)
    private var lastYaw = 0f
    private var lastPitch = 0f
    private var isFirstEvent = true

    // Precision Calibration (Bias removal)
    private var isCalibrating = false
    private var calibrationStartTime = 0L
    private var driftSamplesYaw = mutableListOf<Float>()
    private var driftSamplesPitch = mutableListOf<Float>()
    private var biasYaw = 0f
    private var biasPitch = 0f

    var onCalibrationStatusChanged: ((Boolean) -> Unit)? = null

    // Pro Tuning
    var sensitivity: Float = 22f        // Base sensitivity
    var deadZone: Float = 0.001f        // Ultra-fine deadzone (radians)
    private val snapThreshold = 0.003f  // Threshold to hard-stop movement

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    // Low-pass EMA filter (Proprietary Pro Alpha)
    private var smoothYaw = 0f
    private var smoothPitch = 0f
    private val emaAlpha = 0.85f  // High alpha = Zero inertia (Pro sync)

    fun start() {
        if (rotationSensor == null) return
        isFirstEvent = true
        hasCalibration = false
        smoothYaw = 0f
        smoothPitch = 0f
        sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_FASTEST)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        isFirstEvent = true
        isCalibrating = false
    }

    fun calibrate() {
        hasCalibration = false
    }

    fun calibratePrecision() {
        biasYaw = 0f
        biasPitch = 0f
        driftSamplesYaw.clear()
        driftSamplesPitch.clear()
        calibrationStartTime = System.currentTimeMillis()
        isCalibrating = true
        onCalibrationStatusChanged?.invoke(true)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            handleShake(event)
            return
        }
        if (event.sensor.type != Sensor.TYPE_GAME_ROTATION_VECTOR) return

        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        val yaw = orientationAngles[0]    // Azimuth (Z)
        val pitch = orientationAngles[1]  // Pitch (X)

        if (!hasCalibration || isFirstEvent) {
            lastYaw = yaw
            lastPitch = pitch
            hasCalibration = true
            isFirstEvent = false
            return
        }

        // Compute deltas
        var deltaYaw = yaw - lastYaw
        var deltaPitch = pitch - lastPitch

        // Wraparound support
        if (deltaYaw > Math.PI) deltaYaw -= (2 * Math.PI).toFloat()
        if (deltaYaw < -Math.PI) deltaYaw += (2 * Math.PI).toFloat()

        lastYaw = yaw
        lastPitch = pitch

        if (isCalibrating) {
            processCalibration(deltaYaw, deltaPitch)
            return
        }

        // Apply Bias & Snap-to-Zero logic
        deltaYaw -= biasYaw
        deltaPitch -= biasPitch

        if (abs(deltaYaw) < deadZone) deltaYaw = 0f
        if (abs(deltaPitch) < deadZone) deltaPitch = 0f

        // EMA Smoothing (High alpha for Pro responsiveness)
        smoothYaw = emaAlpha * deltaYaw + (1f - emaAlpha) * smoothYaw
        smoothPitch = emaAlpha * deltaPitch + (1f - emaAlpha) * smoothPitch

        // Snap to zero if movement is negligible to prevent "sensor crawl"
        if (abs(smoothYaw) < snapThreshold && abs(deltaYaw) == 0f) smoothYaw = 0f
        if (abs(smoothPitch) < snapThreshold && abs(deltaPitch) == 0f) smoothPitch = 0f

        if (smoothYaw != 0f || smoothPitch != 0f) {
            // Mapping for "Point to Move" (Top of phone control)
            // Yaw -> Horizontal cursor X
            // Pitch -> Vertical cursor Y (Negated for natural top-up movement)
            val dx = smoothYaw * sensitivity * 120f
            val dy = -smoothPitch * sensitivity * 120f

            _deltaChannel.trySend(dx to dy)
        }
    }

    private fun handleShake(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val acceleration = kotlin.math.sqrt(x*x + y*y + z*z) - SensorManager.GRAVITY_EARTH
        if (acceleration > SHAKE_THRESHOLD) {
            val now = System.currentTimeMillis()
            if (now - lastStrongSpikeTime > 900) {
                strongShakeSpikeCount = 0
            }
            strongShakeSpikeCount += 1
            lastStrongSpikeTime = now

            // Require two strong spikes close together so light shakes do not trigger.
            if (strongShakeSpikeCount >= 2 && now - lastShakeTime > 1500) {
                lastShakeTime = now
                strongShakeSpikeCount = 0
                onShakeDetected?.invoke()
            }
        }
    }

    private fun processCalibration(deltaYaw: Float, deltaPitch: Float) {
        val now = System.currentTimeMillis()
        if (now - calibrationStartTime < 1000) {
            driftSamplesYaw.add(deltaYaw)
            driftSamplesPitch.add(deltaPitch)
        } else {
            if (driftSamplesYaw.isNotEmpty()) {
                biasYaw = driftSamplesYaw.average().toFloat()
                biasPitch = driftSamplesPitch.average().toFloat()
            }
            isCalibrating = false
            onCalibrationStatusChanged?.invoke(false)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
