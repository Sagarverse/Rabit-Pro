package com.example.rabit

import android.app.Application

class RabitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Service start removed from here to prevent crash on Android 14+
        // It will be started from MainActivity after permissions are granted.
    }
}
