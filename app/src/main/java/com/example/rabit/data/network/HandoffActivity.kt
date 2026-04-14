package com.example.rabit.data.network

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * HandoffActivity - Feature 3: Screen Handoff
 *
 * This transparent Activity registers as a Share target for URLs.
 * When the user taps "Share > Hackie" in Chrome or any other browser,
 * Android invokes this Activity with the URL as an Intent extra.
 *
 * The Activity then:
 *  1. Discovers the Mac's companion server on the local network via NSD (mDNS)
 *  2. POSTs the URL to the Mac's companion server endpoint via OkHttp
 *  3. Finishes immediately (invisible to the user)
 */
class HandoffActivity : Activity() {

    @Serializable
    data class HandoffPayload(val url: String)

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = when {
            intent?.action == Intent.ACTION_SEND && intent.type == "text/plain" ->
                intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        }

        if (url.isNullOrBlank()) {
            Toast.makeText(this, "Hackie: Nothing to hand off.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d("HandoffActivity", "Handing off URL: $url")
        discoverAndSend(url)
    }

    /**
    * Use Android NSD (Network Service Discovery) to find the Mac's Hackie companion
     * server on the local network, then POST the URL to it.
     */
    private fun discoverAndSend(url: String) {
        val nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager
        var resolved = false

        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e("HandoffActivity", "NSD resolve failed: $errorCode")
                if (!resolved) {
                    resolved = true
                    runOnUiThread {
                        Toast.makeText(this@HandoffActivity, "Hackie: Could not reach Mac server.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                resolved = true
                val host = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    serviceInfo.hostAddresses.firstOrNull()?.hostAddress
                } else {
                    @Suppress("DEPRECATION")
                    serviceInfo.host?.hostAddress
                }
                val port = serviceInfo.port
                Log.d("HandoffActivity", "Resolved Mac at $host:$port")
                sendToMac(url, host ?: "", port)
            }
        }

        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d("HandoffActivity", "NSD discovery started for $serviceType")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d("HandoffActivity", "NSD service found: ${serviceInfo.serviceName}")
                if (serviceInfo.serviceName.contains("rabit", ignoreCase = true)) {
                    try {
                        @Suppress("DEPRECATION")
                        nsdManager.resolveService(serviceInfo, resolveListener)
                    } catch (e: Exception) {
                        Log.e("HandoffActivity", "Resolve error", e)
                    }
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("HandoffActivity", "NSD start failed: $errorCode")
                runOnUiThread {
                    Toast.makeText(this@HandoffActivity, "Hackie: Network discovery failed.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }

        // Start NSD discovery
        try {
            nsdManager.discoverServices("_rabit._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e("HandoffActivity", "Failed to start NSD", e)
            Toast.makeText(this, "Hackie: Discovery error.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Timeout: if not resolved within 4 seconds, give up
        scope.launch {
            delay(4000)
            if (!resolved) {
                resolved = true
                try {
                    nsdManager.stopServiceDiscovery(discoveryListener)
                } catch (e: Exception) {
                    Log.w("HandoffActivity", "Failed stopping NSD discovery on timeout", e)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@HandoffActivity, "Hackie: Mac not found on network. Ensure Hackie Mac companion is running.", Toast.LENGTH_LONG).show()
                    finish()
                }
            } else {
                try {
                    nsdManager.stopServiceDiscovery(discoveryListener)
                } catch (e: Exception) {
                    Log.w("HandoffActivity", "Failed stopping NSD discovery after resolve", e)
                }
            }
        }
    }

    private fun sendToMac(url: String, host: String, port: Int) {
        if (host.isBlank()) {
            runOnUiThread {
                Toast.makeText(this, "Hackie: Invalid Mac address.", Toast.LENGTH_SHORT).show()
                finish()
            }
            return
        }

        val endpoint = "http://$host:$port/handoff"
        scope.launch {
            try {
                val json = Json.encodeToString(HandoffPayload(url))
                val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder().url(endpoint).post(body).build()

                withContext(Dispatchers.IO) { client.newCall(request).execute() }
                Toast.makeText(this@HandoffActivity, "✅ Opened on Mac!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("HandoffActivity", "Handoff failed: ${e.message}")
                Toast.makeText(this@HandoffActivity, "Hackie: Could not reach Mac.", Toast.LENGTH_SHORT).show()
            } finally {
                finish()
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
