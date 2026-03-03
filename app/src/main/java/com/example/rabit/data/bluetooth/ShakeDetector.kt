package com.example.rabit.data.bluetooth

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(private val onShake: (ShakeType) -> Unit) : SensorEventListener {

    enum class ShakeType {
        HORIZONTAL, VERTICAL, GENERAL
    }

    private var lastUpdate: Long = 0
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var lastZ: Float = 0f
    private val threshold = 12.0f // Sensitivity threshold

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val curTime = System.currentTimeMillis()
        // Only allow one shake event every 500ms
        if ((curTime - lastUpdate) > 100) {
            val diffTime = curTime - lastUpdate
            lastUpdate = curTime

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val speed = sqrt(((x - lastX) * (x - lastX) + (y - lastY) * (y - lastY) + (z - lastZ) * (z - lastZ)).toDouble()) / diffTime * 10000

            if (speed > 800) { // Shake detected
                val absX = Math.abs(x - lastX)
                val absY = Math.abs(y - lastY)
                
                if (absX > absY && absX > 15) {
                    onShake(ShakeType.HORIZONTAL)
                } else if (absY > absX && absY > 15) {
                    onShake(ShakeType.VERTICAL)
                } else {
                    onShake(ShakeType.GENERAL)
                }
            }

            lastX = x
            lastY = y
            lastZ = z
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
