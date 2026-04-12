package com.example.rabit.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

class SpatialPointerManager(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    
    var onPointerUpdate: ((dx: Float, dy: Float) -> Unit)? = null
    
    private var isRunning = false
    var sensitivity = 1.5f
    
    // EMA Filter Alpha (Smoothing)
    private val filterAlpha = 0.85f
    private var filteredDx = 0f
    private var filteredDy = 0f

    fun start() {
        if (isRunning) return
        gyroSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            isRunning = true
        }
    }

    fun stop() {
        if (!isRunning) return
        sensorManager.unregisterListener(this)
        isRunning = false
        filteredDx = 0f
        filteredDy = 0f
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_GYROSCOPE) {
            // event.values[0] -> X rotation (pitch) -> vertical mouse movement (dy)
            // event.values[1] -> Y rotation (yaw) -> horizontal mouse movement (dx)
            // event.values[2] -> Z rotation (roll)
            
            val rawDx = -event.values[1] * 100f * sensitivity
            val rawDy = -event.values[0] * 100f * sensitivity
            
            // Apply EMA Filter
            filteredDx = filterAlpha * filteredDx + (1 - filterAlpha) * rawDx
            filteredDy = filterAlpha * filteredDy + (1 - filterAlpha) * rawDy
            
            // Deadzone to prevent drift
            val finalDx = if (abs(filteredDx) < 0.2f) 0f else filteredDx
            val finalDy = if (abs(filteredDy) < 0.2f) 0f else filteredDy
            
            if (finalDx != 0f || finalDy != 0f) {
                onPointerUpdate?.invoke(finalDx, finalDy)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
